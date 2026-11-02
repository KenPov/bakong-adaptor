package com.sbibank.openbanking.fundtransfer.service;

import com.sbibank.openbanking.common.xml.XmlSecurity;
import com.sbibank.openbanking.fundtransfer.dto.FullFundTransferApiResponse;
import com.sbibank.openbanking.fundtransfer.dto.FullFundTransferData;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Parses the CIFTP {@code makeFullFundTransfer} SOAP response.
 *
 * <p>Replaces the legacy Synapse JS, which scraped {@code <OrgnlMsgId>}/{@code <Ustrd>}/
 * {@code <OrgnlPmtInfId>} out of the {@code return} field with fragile {@code lastIndexOf}/{@code
 * substring} string operations, with proper two-stage DOM parsing: the outer SOAP envelope, then the
 * {@code return} field's own text content, which is itself a well-formed XML fragment.
 */
@Component
public class SoapResponseParser {

    public FullFundTransferApiResponse parse(String rawSoapResponse) {
        Document soapDoc = parseXml(rawSoapResponse);
        if (soapDoc == null) {
            return FullFundTransferApiResponse.error("Unable to parse CIFTP SOAP response");
        }

        Optional<Element> responseElement = findByLocalName(soapDoc.getDocumentElement(), "makeFullFundTransferResponse");
        if (responseElement.isPresent()) {
            return parseSuccess(responseElement.get());
        }

        Optional<Element> faultElement = findByLocalName(soapDoc.getDocumentElement(), "Fault");
        String faultString = faultElement
                .flatMap(fault -> findByLocalName(fault, "faultstring"))
                .map(this::textOf)
                .orElse("Unknown SOAP fault from CIFTP");
        return FullFundTransferApiResponse.error(faultString);
    }

    private FullFundTransferApiResponse parseSuccess(Element responseElement) {
        String returnText =
                findByLocalName(responseElement, "return").map(this::textOf).orElse(null);
        if (returnText == null || returnText.isBlank()) {
            return FullFundTransferApiResponse.error("CIFTP response missing 'return' field");
        }

        String cleaned = returnText.trim().replaceFirst("^<\\?xml[^>]*\\?>", "").trim();
        Document inner = parseXml("<root>" + cleaned + "</root>");
        if (inner == null) {
            return FullFundTransferApiResponse.error("Unable to parse CIFTP 'return' XML fragment");
        }

        String msgId = findByLocalName(inner.getDocumentElement(), "OrgnlMsgId").map(this::textOf).orElse(null);
        String pmtInfId = findByLocalName(inner.getDocumentElement(), "OrgnlPmtInfId").map(this::textOf).orElse(null);
        String ustrd = findByLocalName(inner.getDocumentElement(), "Ustrd").map(this::textOf).orElse(null);

        return FullFundTransferApiResponse.success(new FullFundTransferData(msgId, pmtInfId, ustrd));
    }

    private Document parseXml(String xml) {
        try {
            DocumentBuilderFactory factory = XmlSecurity.newSecuredDocumentBuilderFactory();
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            return null;
        }
    }

    /** Namespace-agnostic descendant lookup, mirroring the legacy XPath's {@code local-name()='...'} usage. */
    private Optional<Element> findByLocalName(Node root, String localName) {
        if (root == null) {
            return Optional.empty();
        }
        if (root instanceof Element element && localName.equals(element.getLocalName())) {
            return Optional.of(element);
        }
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Optional<Element> found = findByLocalName(children.item(i), localName);
            if (found.isPresent()) {
                return found;
            }
        }
        return Optional.empty();
    }

    private String textOf(Element element) {
        return element.getTextContent();
    }
}
