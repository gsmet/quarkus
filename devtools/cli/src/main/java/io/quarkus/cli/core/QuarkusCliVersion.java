package io.quarkus.cli.core;

import java.io.IOException;
import java.util.Properties;

import picocli.CommandLine;

public class QuarkusCliVersion implements CommandLine.IVersionProvider {

    private static String version;

    public static String version() {
        if (version != null) {
            return version;
        }
        final Properties props = new Properties();
        try {
            props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("quarkus.properties"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load quarkus.properties", e);
        }
        version = props.getProperty("quarkus-core-version");
        if (version == null) {
            throw new RuntimeException("Failed to locate quarkus-core-version property in the bundled quarkus.properties");
        }
        return version;
    }

    @Override
    public String[] getVersion() throws Exception {
        return new String[] { version() };
    }

}
