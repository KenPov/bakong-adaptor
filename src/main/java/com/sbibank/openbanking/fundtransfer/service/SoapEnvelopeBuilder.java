package com.sbibank.openbanking.fundtransfer.service;

import com.sbibank.openbanking.common.xml.XmlSecurity;
import com.sbibank.openbanking.config.CiftpProperties;
import com.sbibank.openbanking.fundtransfer.dto.FullFundTransferRequest;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Wraps the pain.001 message (built by {@link Pain001MessageBuilder}) in the SOAP 1.1
 * {@code web:makeFullFundTransfer} envelope expected by the CIFTP {@code BakongWebService} adapter,
 * matching {@code bakong-full-fund-transfer.xml}. The pain.001 text is set as the text content of
 * {@code web:iso_message}, which the serializer XML-escapes - the same effect the legacy Synapse template
 * achieved with a literal CDATA section.
 */
@Component
public class SoapEnvelopeBuilder {

    private static final String SOAP_NS = "http://schemas.xmlsoap.org/soap/envelope/";
    private static final String WEB_NS = "http://webservice.nbc.org.kh/";

    public String buildMakeFullFundTransfer(FullFundTransferRequest request, CiftpProperties ciftp, String isoMessage) {
        try {
            DocumentBuilderFactory factory = XmlSecurity.newSecuredDocumentBuilderFactory();
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            Document doc = documentBuilder.newDocument();

            Element envelope = doc.createElementNS(SOAP_NS, "soapenv:Envelope");
            envelope.setAttribute("xmlns:soapenv", SOAP_NS);
            envelope.setAttribute("xmlns:web", WEB_NS);
            doc.appendChild(envelope);

            envelope.appendChild(doc.createElementNS(SOAP_NS, "soapenv:Header"));

            Element body = doc.createElementNS(SOAP_NS, "soapenv:Body");
            envelope.appendChild(body);

            Element operation = doc.createElementNS(WEB_NS, "web:makeFullFundTransfer");
            body.appendChild(operation);

            appendText(doc, operation, "web:cm_user_name", ciftp.apiUser());
            appendText(doc, operation, "web:cm_password", ciftp.apiPassword());
            appendText(doc, operation, "web:ext_ref", request.extRefId());
            appendText(doc, operation, "web:iso_message", isoMessage);

            return serialize(doc);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build makeFullFundTransfer SOAP envelope", ex);
        }
    }

    private void appendText(Document doc, Element parent, String qualifiedName, String text) {
        Element element = doc.createElementNS(WEB_NS, qualifiedName);
        element.setTextContent(text);
        parent.appendChild(element);
    }

    private String serialize(Document doc) throws Exception {
        TransformerFactory transformerFactory = XmlSecurity.newSecuredTransformerFactory();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(out));
        return out.toString(StandardCharsets.UTF_8);
    }
}
