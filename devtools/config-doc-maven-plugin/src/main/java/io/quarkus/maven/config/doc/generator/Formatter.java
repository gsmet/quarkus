package io.quarkus.maven.config.doc.generator;

import io.quarkus.annotation.processor.documentation.config.merger.JavadocRepository;
import io.quarkus.annotation.processor.documentation.config.model.ConfigProperty;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;
import io.quarkus.annotation.processor.documentation.config.model.Extension;

public interface Formatter {

    String formatDescription(ConfigProperty configProperty);

    String formatTypeDescription(ConfigProperty configProperty);

    String formatDefaultValue(ConfigProperty configProperty);

    String escapeCellContent(String value);

    String toAnchor(String value);

    String formatSectionTitle(ConfigSection configSection);

    String formatName(Extension extension);

    static Formatter getFormatter(JavadocRepository javadocRepository, boolean enableEnumTooltips, Format format) {
        switch (format) {
            case asciidoc:
                return new AsciidocFormatter(javadocRepository, enableEnumTooltips);
            case markdown:
                return new MarkdownFormatter(javadocRepository, enableEnumTooltips);
            default:
                throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }
}