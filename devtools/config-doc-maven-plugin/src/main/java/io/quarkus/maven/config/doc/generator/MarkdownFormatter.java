package io.quarkus.maven.config.doc.generator;

import io.quarkus.annotation.processor.documentation.config.merger.JavadocRepository;
import io.quarkus.annotation.processor.documentation.config.model.ConfigSection;

final class MarkdownFormatter extends AbstractFormatter {

    private static final String MORE_INFO_ABOUT_TYPE_FORMAT = "[🛈](#%s)";

    MarkdownFormatter(JavadocRepository javadocRepository, boolean enableEnumTooltips) {
        super(javadocRepository, enableEnumTooltips);
    }

    @Override
    public String formatSectionTitle(ConfigSection configSection) {
        // markdown only has 6 heading levels
        int headingLevel = Math.min(6, 2 + configSection.getLevel());
        return "#".repeat(headingLevel) + " " + super.formatSectionTitle(configSection);
    }

    @Override
    public String escapeCellContent(String value) {
        return super.escapeCellContent(value).replaceAll("\n+", " ");
    }

    @Override
    protected String moreInformationAboutType(String anchorRoot, String type) {
        return MORE_INFO_ABOUT_TYPE_FORMAT.formatted(anchorRoot);
    }

    @Override
    protected String link(String href, String description) {
        return String.format("[%2$s](%1$s)", href, description);
    }

    @Override
    protected String tooltip(String value, String javadocDescription) {
        // we don't have tooltip support in Markdown
        return "`" + value + "`";
    }
}
