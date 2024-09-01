package io.quarkus.maven.config.doc.generator;

import io.quarkus.annotation.processor.documentation.config.merger.JavadocRepository;
import io.quarkus.annotation.processor.documentation.config.model.ConfigProperty;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements;

final class AsciidocFormatter extends AbstractFormatter {

    private static final String TOOLTIP_MACRO = "tooltip:%s[%s]";
    private static final String MORE_INFO_ABOUT_TYPE_FORMAT = "link:#%s[icon:question-circle[title=More information about the %s format]]";

    AsciidocFormatter(JavadocRepository javadocRepository, boolean enableEnumTooltips) {
        super(javadocRepository, enableEnumTooltips);
    }

    @Override
    public String formatDescription(ConfigProperty configProperty) {
        String description = super.formatDescription(configProperty);
        return description == null ? null : description + "\n\n";
    }

    protected String moreInformationAboutType(String anchorRoot, String type) {
        return String.format(MORE_INFO_ABOUT_TYPE_FORMAT, anchorRoot + "-{summaryTableId}", type);
    }

    protected String tooltip(String value, String javadocDescription) {
        return String.format(TOOLTIP_MACRO, value, cleanTooltipContent(javadocDescription));
    }

    /**
     * Note that this is extremely brittle. Apparently, colons breaks the tooltips but if escaped with \, the \ appears in the
     * output.
     * <p>
     * We should probably have some warnings/errors as to what is accepted in enum Javadoc.
     */
    private String cleanTooltipContent(String tooltipContent) {
        return tooltipContent.replace("<p>", "").replace("</p>", "").replace("\n+\n", " ").replace("\n", " ")
                .replace(":", "\\:").replace("[", "\\]").replace("]", "\\]");
    }

    protected String link(String href, String description) {
        return String.format("link:%s[%s]", href, description);
    }

    @Override
    protected String javadoc(JavadocElements.JavadocElement javadocElement) {
        return javadocElement.description();
    }
}
