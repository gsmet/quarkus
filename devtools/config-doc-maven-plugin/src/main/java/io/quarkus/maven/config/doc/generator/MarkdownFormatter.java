package io.quarkus.maven.config.doc.generator;

import io.quarkus.annotation.processor.documentation.config.merger.JavadocRepository;
import io.quarkus.annotation.processor.documentation.config.model.JavadocElements.JavadocElement;

final class MarkdownFormatter extends AbstractFormatter {

    private static final String MORE_INFO_ABOUT_TYPE_FORMAT = "[🛈](%s)";

    MarkdownFormatter(JavadocRepository javadocRepository, boolean enableEnumTooltips) {
        super(javadocRepository, enableEnumTooltips);
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

    @Override
    protected String javadoc(JavadocElement javadocElement) {
        return javadocElement.rawJavadoc();
    }
}
