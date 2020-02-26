package io.quarkus.agroal.runtime;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import org.jboss.logging.Logger;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionPoolConfiguration.ConnectionValidator;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import io.agroal.api.transaction.TransactionIntegration;
import io.agroal.narayana.NarayanaTransactionIntegration;
import io.quarkus.agroal.runtime.DataSourcesJdbcBuildTimeConfig.DataSourceJdbcOuterNamedBuildTimeConfig;
import io.quarkus.agroal.runtime.DataSourcesJdbcRuntimeConfig.DataSourceJdbcOuterNamedRuntimeConfig;
import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.datasource.common.runtime.DataSourceUtil;
import io.quarkus.datasource.runtime.DataSourceBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourceRuntimeConfig;
import io.quarkus.datasource.runtime.DataSourcesBuildTimeConfig;
import io.quarkus.datasource.runtime.DataSourcesRuntimeConfig;
import io.quarkus.vault.CredentialsProvider;

public abstract class AbstractDataSourceProducer {

    private static final Logger log = Logger.getLogger(AbstractDataSourceProducer.class.getName());

    private DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig;
    private DataSourcesRuntimeConfig dataSourcesRuntimeConfig;
    private DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig;
    private DataSourcesJdbcRuntimeConfig dataSourcesJdbcRuntimeConfig;
    private boolean disableSslSupport = false;

    private List<AgroalDataSource> dataSources = new ArrayList<>();

    @Inject
    public TransactionManager transactionManager;

    @Inject
    public TransactionSynchronizationRegistry transactionSynchronizationRegistry;

    public void configureDataSources(DataSourcesBuildTimeConfig dataSourcesBuildTimeConfig,
            DataSourcesJdbcBuildTimeConfig dataSourcesJdbcBuildTimeConfig,
            DataSourcesRuntimeConfig dataSourcesRuntimeConfig,
            DataSourcesJdbcRuntimeConfig dataSourcesJdbcRuntimeConfig,
            boolean disableSslSupport) {
        this.dataSourcesBuildTimeConfig = dataSourcesBuildTimeConfig;
        this.dataSourcesJdbcBuildTimeConfig = dataSourcesJdbcBuildTimeConfig;
        this.dataSourcesRuntimeConfig = dataSourcesRuntimeConfig;
        this.dataSourcesJdbcRuntimeConfig = dataSourcesJdbcRuntimeConfig;
        this.disableSslSupport = disableSslSupport;
    }

    public AgroalDataSource createDataSource(String dataSourceName,
            DataSourceBuildTimeConfig dataSourceBuildTimeConfig,
            DataSourceJdbcBuildTimeConfig dataSourceJdbcBuildTimeConfig,
            DataSourceRuntimeConfig dataSourceRuntimeConfig,
            DataSourceJdbcRuntimeConfig dataSourceJdbcRuntimeConfig,
            boolean mpMetricsPresent) {
        checkConfigInjection();

        if (!dataSourceJdbcBuildTimeConfig.enabled) {
            return null;
        }

        if (!dataSourceJdbcRuntimeConfig.url.isPresent()) {
            log.warn("JDBC datasource " + dataSourceName + " not started: driver and/or url are not defined.");
            return null;
        }

        // we first make sure that all available JDBC drivers are loaded in the current TCCL
        loadDriversInTCCL();

        String driverName = dataSourceJdbcBuildTimeConfig.driver.get();
        Class<?> driver;
        try {
            driver = Class.forName(driverName, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Unable to load the dataSource driver", e);
        }

        String url = dataSourceJdbcRuntimeConfig.url.get();

        //TODO should we do such checks at build time only? All these are currently defined at build - but it could change
        //depending on if and how we could do Driver auto-detection.
        final io.quarkus.agroal.runtime.TransactionIntegration transactionIntegration = dataSourceJdbcBuildTimeConfig.transactions;
        if (transactionIntegration == io.quarkus.agroal.runtime.TransactionIntegration.XA) {
            if (!XADataSource.class.isAssignableFrom(driver)) {
                throw new RuntimeException("Driver is not an XA dataSource and XA has been configured");
            }
        } else {
            if (driver != null && !DataSource.class.isAssignableFrom(driver) && !Driver.class.isAssignableFrom(driver)) {
                throw new RuntimeException("Driver is an XA dataSource and XA has not been configured");
            }
        }

        AgroalDataSourceConfigurationSupplier dataSourceConfiguration = new AgroalDataSourceConfigurationSupplier();

        AgroalConnectionPoolConfigurationSupplier poolConfiguration = dataSourceConfiguration.connectionPoolConfiguration();
        AgroalConnectionFactoryConfigurationSupplier agroalConnectionFactoryConfigurationSupplier = poolConfiguration
                .connectionFactoryConfiguration();
        agroalConnectionFactoryConfigurationSupplier.jdbcUrl(url);
        agroalConnectionFactoryConfigurationSupplier.connectionProviderClass(driver);
        agroalConnectionFactoryConfigurationSupplier.trackJdbcResources(dataSourceJdbcRuntimeConfig.detectStatementLeaks);

        if (dataSourceJdbcRuntimeConfig.transactionIsolationLevel.isPresent()) {
            agroalConnectionFactoryConfigurationSupplier
                    .jdbcTransactionIsolation(
                            dataSourceJdbcRuntimeConfig.transactionIsolationLevel.get());
        }

        if (transactionIntegration != io.quarkus.agroal.runtime.TransactionIntegration.DISABLED) {
            TransactionIntegration txIntegration = new NarayanaTransactionIntegration(transactionManager,
                    transactionSynchronizationRegistry);
            poolConfiguration.transactionIntegration(txIntegration);
        }

        // New connection SQL
        if (dataSourceJdbcRuntimeConfig.newConnectionSql.isPresent()) {
            agroalConnectionFactoryConfigurationSupplier.initialSql(dataSourceJdbcRuntimeConfig.newConnectionSql.get());
        }

        // metrics
        if (dataSourceJdbcBuildTimeConfig.enableMetrics.isPresent()) {
            dataSourceConfiguration.metricsEnabled(dataSourceJdbcBuildTimeConfig.enableMetrics.get());
        } else {
            // if the enable-metrics property is unspecified, treat it as true if MP Metrics are being exposed
            dataSourceConfiguration.metricsEnabled(dataSourcesBuildTimeConfig.metricsEnabled && mpMetricsPresent);
        }

        // Authentication
        if (dataSourceRuntimeConfig.username.isPresent()) {
            agroalConnectionFactoryConfigurationSupplier
                    .principal(new NamePrincipal(dataSourceRuntimeConfig.username.get()));
        }
        if (dataSourceRuntimeConfig.password.isPresent()) {
            agroalConnectionFactoryConfigurationSupplier
                    .credential(new SimplePassword(dataSourceRuntimeConfig.password.get()));
        }

        // Vault credentials provider
        if (dataSourceRuntimeConfig.credentialsProvider.isPresent()) {
            ArcContainer container = Arc.container();
            String type = dataSourceRuntimeConfig.credentialsProviderType.orElse(null);
            CredentialsProvider credentialsProvider = type != null
                    ? (CredentialsProvider) container.instance(type).get()
                    : container.instance(CredentialsProvider.class).get();

            if (credentialsProvider == null) {
                throw new RuntimeException("unable to find credentials provider of type " + (type == null ? "default" : type));
            }

            String name = dataSourceRuntimeConfig.credentialsProvider.get();
            agroalConnectionFactoryConfigurationSupplier
                    .credential(new AgroalVaultCredentialsProviderPassword(name, credentialsProvider));
        }

        // Pool size configuration:
        poolConfiguration.minSize(dataSourceJdbcRuntimeConfig.minSize);
        poolConfiguration.maxSize(dataSourceJdbcRuntimeConfig.maxSize);
        if (dataSourceJdbcRuntimeConfig.initialSize.isPresent() && dataSourceJdbcRuntimeConfig.initialSize.get() > 0) {
            poolConfiguration.initialSize(dataSourceJdbcRuntimeConfig.initialSize.get());
        }

        // Connection management
        poolConfiguration.connectionValidator(ConnectionValidator.defaultValidator());
        if (dataSourceJdbcRuntimeConfig.acquisitionTimeout.isPresent()) {
            poolConfiguration.acquisitionTimeout(dataSourceJdbcRuntimeConfig.acquisitionTimeout.get());
        }
        if (dataSourceJdbcRuntimeConfig.backgroundValidationInterval.isPresent()) {
            poolConfiguration.validationTimeout(dataSourceJdbcRuntimeConfig.backgroundValidationInterval.get());
        }
        if (dataSourceJdbcRuntimeConfig.validationQuerySql.isPresent()) {
            String validationQuery = dataSourceJdbcRuntimeConfig.validationQuerySql.get();
            poolConfiguration.connectionValidator(new ConnectionValidator() {

                @Override
                public boolean isValid(Connection connection) {
                    try (Statement stmt = connection.createStatement()) {
                        stmt.execute(validationQuery);
                        return true;
                    } catch (Exception e) {
                        log.warn("Connection validation failed", e);
                    }
                    return false;
                }
            });
        }
        if (dataSourceJdbcRuntimeConfig.idleRemovalInterval.isPresent()) {
            poolConfiguration.reapTimeout(dataSourceJdbcRuntimeConfig.idleRemovalInterval.get());
        }
        if (dataSourceJdbcRuntimeConfig.leakDetectionInterval.isPresent()) {
            poolConfiguration.leakTimeout(dataSourceJdbcRuntimeConfig.leakDetectionInterval.get());
        }
        if (dataSourceJdbcRuntimeConfig.maxLifetime.isPresent()) {
            poolConfiguration.maxLifetime(dataSourceJdbcRuntimeConfig.maxLifetime.get());
        }

        // SSL support: we should push the driver specific code to the driver extensions but it will have to do for now
        if (disableSslSupport) {
            switch (driverName) {
                case "org.postgresql.Driver":
                    agroalConnectionFactoryConfigurationSupplier.jdbcProperty("sslmode", "disable");
                    break;
                case "org.mariadb.jdbc.Driver":
                    agroalConnectionFactoryConfigurationSupplier.jdbcProperty("useSSL", "false");
                    break;
                default:
                    log.warn("Agroal does not support disabling SSL for driver " + driverName);
            }
        }

        // Explicit reference to bypass reflection need of the ServiceLoader used by AgroalDataSource#from
        AgroalDataSource dataSource = new io.agroal.pool.DataSource(dataSourceConfiguration.get(),
                new AgroalEventLoggingListener(dataSourceName));
        log.debugv("Started data source {0} connected to {1}", dataSource, url);

        this.dataSources.add(dataSource);

        return dataSource;
    }

    public DataSourceBuildTimeConfig getDataSourceBuildTimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return dataSourcesBuildTimeConfig.defaultDataSource;
        }

        DataSourceBuildTimeConfig namedConfig = dataSourcesBuildTimeConfig.namedDataSources.get(dataSourceName);

        return namedConfig != null ? namedConfig : new DataSourceBuildTimeConfig();
    }

    public DataSourceJdbcBuildTimeConfig getDataSourceJdbcBuildTimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return dataSourcesJdbcBuildTimeConfig.jdbc;
        }

        DataSourceJdbcOuterNamedBuildTimeConfig namedOuterConfig = dataSourcesJdbcBuildTimeConfig.namedDataSources
                .get(dataSourceName);

        return namedOuterConfig != null ? namedOuterConfig.jdbc : new DataSourceJdbcBuildTimeConfig();
    }

    public DataSourceRuntimeConfig getDataSourceRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return dataSourcesRuntimeConfig.defaultDataSource;
        }

        DataSourceRuntimeConfig namedConfig = dataSourcesRuntimeConfig.namedDataSources.get(dataSourceName);

        return namedConfig != null ? namedConfig : new DataSourceRuntimeConfig();
    }

    public DataSourceJdbcRuntimeConfig getDataSourceJdbcRuntimeConfig(String dataSourceName) {
        if (DataSourceUtil.isDefault(dataSourceName)) {
            return dataSourcesJdbcRuntimeConfig.jdbc;
        }

        DataSourceJdbcOuterNamedRuntimeConfig namedOuterConfig = dataSourcesJdbcRuntimeConfig.namedDataSources
                .get(dataSourceName);

        return namedOuterConfig != null ? namedOuterConfig.jdbc : new DataSourceJdbcRuntimeConfig();
    }

    private void checkConfigInjection() {
        if (dataSourcesBuildTimeConfig == null || dataSourcesJdbcBuildTimeConfig == null || dataSourcesRuntimeConfig == null
                || dataSourcesJdbcRuntimeConfig == null) {
            throw new IllegalStateException(
                    "The datasources are not ready to be consumed: the configuration has not been injected yet");
        }
    }

    /**
     * Uses the {@link ServiceLoader#load(Class) ServiceLoader to load the JDBC drivers} in context
     * of the current {@link Thread#getContextClassLoader() TCCL}
     */
    private static void loadDriversInTCCL() {
        // load JDBC drivers in the current TCCL
        final ServiceLoader<Driver> drivers = ServiceLoader.load(Driver.class);
        final Iterator<Driver> iterator = drivers.iterator();
        while (iterator.hasNext()) {
            try {
                // load the driver
                iterator.next();
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    @PreDestroy
    public void stop() {
        for (AgroalDataSource dataSource : dataSources) {
            if (dataSource != null) {
                dataSource.close();
            }
        }
    }
}
