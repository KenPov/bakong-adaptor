package com.sbibank.openbanking.common;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sbibank.openbanking.common.xml.XmlSecurity;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.TransformerFactory;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

class XmlSecurityTest {

    @Test
    void securedFactoryParsesNormalXml() throws Exception {
        DocumentBuilderFactory factory = XmlSecurity.newSecuredDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        String xml = "<root><child>value</child></root>";
        var doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        assertNotNull(doc);
    }

    @Test
    void securedFactoryRejectsDoctypeDeclaration() throws Exception {
        DocumentBuilderFactory factory = XmlSecurity.newSecuredDocumentBuilderFactory();
        DocumentBuilder builder = factory.newDocumentBuilder();
        String xxePayload = "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"http://127.0.0.1/evil\">]><root>&xxe;</root>";

        assertThrows(SAXParseException.class, () -> {
            builder.parse(new ByteArrayInputStream(xxePayload.getBytes(StandardCharsets.UTF_8)));
        });
    }

    @Test
    void securedTransformerFactoryNotNull() throws Exception {
        TransformerFactory factory = XmlSecurity.newSecuredTransformerFactory();
        assertNotNull(factory);
    }
}
