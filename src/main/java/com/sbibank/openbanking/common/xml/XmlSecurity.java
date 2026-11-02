package com.sbibank.openbanking.common.xml;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerConfigurationException;

/**
 * Utility to create hardened XML factories that prevent XML External Entity (XXE) injection,
 * XML entity expansion denial of service (Billion Laughs), and external schema/DTD loading.
 */
public final class XmlSecurity {

    private XmlSecurity() {}

    /**
     * Creates a namespace-aware {@link DocumentBuilderFactory} secured against XXE attacks.
     */
    public static DocumentBuilderFactory newSecuredDocumentBuilderFactory() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException ignored) {
            // Some parsers may not support these attributes directly; secure features above still apply
        }

        return factory;
    }

    /**
     * Creates a {@link TransformerFactory} secured against external stylesheet and DTD resolution.
     */
    public static TransformerFactory newSecuredTransformerFactory() throws TransformerConfigurationException {
        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (IllegalArgumentException ignored) {
            // Attribute might not be supported by all implementations
        }

        return factory;
    }
}
