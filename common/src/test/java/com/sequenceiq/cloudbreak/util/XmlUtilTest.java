package com.sequenceiq.cloudbreak.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

class XmlUtilTest {

    @Test
    void parseDoesNotResolveExternalGeneralEntity() throws Exception {
        File secret = File.createTempFile("xxe-entity", ".txt");
        secret.deleteOnExit();
        Files.writeString(secret.toPath(), "XXE_LEAKED_SECRET");

        String xml = String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE root [
                    <!ENTITY xxe SYSTEM "file://%s">
                ]>
                <root>&xxe;</root>
                """, secret.getAbsolutePath());

        Document document = XmlUtil.parse(toStream(xml));

        assertThat(document.getDocumentElement().getTextContent())
                .as("External general entities must not leak file contents")
                .doesNotContain("XXE_LEAKED_SECRET");
    }

    @Test
    void parseDoesNotLoadExternalDtd() throws Exception {
        File dtd = File.createTempFile("xxe-external", ".dtd");
        dtd.deleteOnExit();
        Files.writeString(dtd.toPath(), "<!ELEMENT root ANY>");

        String xml = String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE root SYSTEM "file://%s">
                <root>value</root>
                """, dtd.getAbsolutePath());

        Document document = XmlUtil.parse(toStream(xml));

        assertThat(document.getDocumentElement().getTextContent()).isEqualTo("value");
    }

    @Test
    void parseRejectsParameterEntityExfiltration() throws Exception {
        File secret = File.createTempFile("xxe-param", ".txt");
        secret.deleteOnExit();
        Files.writeString(secret.toPath(), "XXE_PARAM_LEAK");

        String xml = String.format("""
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE root [
                    <!ENTITY %% file SYSTEM "file://%s">
                    <!ENTITY %% eval "<!ENTITY exfil '%%file;'>">
                    %%eval;
                ]>
                <root>&exfil;</root>
                """, secret.getAbsolutePath());

        assertThatThrownBy(() -> XmlUtil.parse(toStream(xml)))
                .as("External parameter entities must be disabled")
                .isInstanceOf(SAXException.class);
    }

    @Test
    void parseBlocksBillionLaughs() {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <!DOCTYPE root [
                    <!ENTITY a0 "lol">
                    <!ENTITY a1 "&a0;&a0;&a0;&a0;&a0;&a0;&a0;&a0;&a0;&a0;">
                    <!ENTITY a2 "&a1;&a1;&a1;&a1;&a1;&a1;&a1;&a1;&a1;&a1;">
                    <!ENTITY a3 "&a2;&a2;&a2;&a2;&a2;&a2;&a2;&a2;&a2;&a2;">
                    <!ENTITY a4 "&a3;&a3;&a3;&a3;&a3;&a3;&a3;&a3;&a3;&a3;">
                    <!ENTITY a5 "&a4;&a4;&a4;&a4;&a4;&a4;&a4;&a4;&a4;&a4;">
                    <!ENTITY a6 "&a5;&a5;&a5;&a5;&a5;&a5;&a5;&a5;&a5;&a5;">
                    <!ENTITY a7 "&a6;&a6;&a6;&a6;&a6;&a6;&a6;&a6;&a6;&a6;">
                    <!ENTITY a8 "&a7;&a7;&a7;&a7;&a7;&a7;&a7;&a7;&a7;&a7;">
                ]>
                <root>&a8;</root>
                """;

        assertThatThrownBy(() -> XmlUtil.parse(toStream(xml)))
                .as("Secure processing must cap entity expansion")
                .isInstanceOf(SAXException.class);
    }

    @Test
    void parseAcceptsWellFormedXmlWithoutDoctype() throws Exception {
        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <configuration>
                    <property>
                        <name>dfs.nameservices</name>
                        <value>ns1</value>
                    </property>
                </configuration>
                """;

        Document document = XmlUtil.parse(toStream(xml));

        assertThat(document.getDocumentElement().getTagName()).isEqualTo("configuration");
    }

    private static ByteArrayInputStream toStream(String xml) {
        return new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
    }
}
