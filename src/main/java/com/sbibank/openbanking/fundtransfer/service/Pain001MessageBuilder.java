package com.sbibank.openbanking.fundtransfer.service;

import com.sbibank.openbanking.common.xml.XmlSecurity;
import com.sbibank.openbanking.config.OurBankProperties;
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
 * Builds the ISO 20022 pain.001.001.05 {@code CstmrCdtTrfInitn} XML document sent as the
 * {@code iso_message} of the CIFTP {@code makeFullFundTransfer} SOAP call, field-for-field identical to
 * the template in {@code bakong-full-fund-transfer.xml}.
 *
 * <p>Built via DOM rather than string concatenation so that all user-supplied text fields (names,
 * description, account numbers) are XML-escaped automatically by the serializer instead of relying on
 * manual escaping.
 */
@Component
public class Pain001MessageBuilder {

    private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pain.001.001.05";

    public String build(FullFundTransferRequest request, OurBankProperties ourBank) {
        try {
            DocumentBuilderFactory factory = XmlSecurity.newSecuredDocumentBuilderFactory();
            DocumentBuilder documentBuilder = factory.newDocumentBuilder();
            Document doc = documentBuilder.newDocument();

            Element document = doc.createElementNS(NS, "Document");
            // Declared explicitly (rather than relying on the serializer to infer/fix up missing
            // namespace declarations for createElementNS-created, prefix-less elements) so the output is
            // correct regardless of the JAXP implementation's namespace-fixup behavior.
            document.setAttribute("xmlns", NS);
            document.setAttribute("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
            document.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            doc.appendChild(document);

            Element cstmrCdtTrfInitn = append(doc, document, "CstmrCdtTrfInitn");
            appendGrpHdr(doc, cstmrCdtTrfInitn, request, ourBank);
            appendPmtInf(doc, cstmrCdtTrfInitn, request, ourBank);

            return serialize(doc);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to build pain.001 fund transfer message", ex);
        }
    }

    private void appendGrpHdr(Document doc, Element parent, FullFundTransferRequest request, OurBankProperties ourBank) {
        Element grpHdr = append(doc, parent, "GrpHdr");
        appendText(doc, grpHdr, "MsgId", request.requestMsgId());
        appendText(doc, grpHdr, "CreDtTm", request.createDateTime());
        appendText(doc, grpHdr, "NbOfTxs", "1");
        appendText(doc, grpHdr, "CtrlSum", request.txnAmount().toPlainString());

        Element initgPty = append(doc, grpHdr, "InitgPty");
        appendText(doc, initgPty, "Nm", ourBank.name());
        Element pstlAdr = append(doc, initgPty, "PstlAdr");
        OurBankProperties.PostalAddress address = ourBank.postalAddress();
        appendText(doc, pstlAdr, "StrtNm", address.streetName());
        appendText(doc, pstlAdr, "BldgNb", address.buildingNumber());
        appendText(doc, pstlAdr, "TwnNm", address.townName());
        appendText(doc, pstlAdr, "Ctry", address.country());
    }

    private void appendPmtInf(Document doc, Element parent, FullFundTransferRequest request, OurBankProperties ourBank) {
        Element pmtInf = append(doc, parent, "PmtInf");
        appendText(doc, pmtInf, "PmtInfId", request.paymentInfoId());
        appendText(doc, pmtInf, "PmtMtd", "TRF");
        appendText(doc, pmtInf, "BtchBookg", "false");
        appendText(doc, pmtInf, "ReqdExctnDt", request.requestExactDate());

        Element dbtr = append(doc, pmtInf, "Dbtr");
        appendText(doc, dbtr, "Nm", request.debitAccountName());

        Element dbtrAcct = append(doc, pmtInf, "DbtrAcct");
        appendText(doc, append(doc, append(doc, dbtrAcct, "Id"), "Othr"), "Id", request.debitAccountNo());
        appendText(doc, dbtrAcct, "Ccy", request.txnCurrency());

        Element dbtrAgt = append(doc, pmtInf, "DbtrAgt");
        appendText(doc, append(doc, dbtrAgt, "FinInstnId"), "BICFI", ourBank.bic());

        appendCdtTrfTxInf(doc, pmtInf, request, ourBank);
    }

    private void appendCdtTrfTxInf(Document doc, Element parent, FullFundTransferRequest request, OurBankProperties ourBank) {
        Element cdtTrfTxInf = append(doc, parent, "CdtTrfTxInf");

        Element pmtId = append(doc, cdtTrfTxInf, "PmtId");
        appendText(doc, pmtId, "InstrId", ourBank.bic() + "/" + request.requestMsgId());
        appendText(doc, pmtId, "EndToEndId", request.debitAccountNo() + "-" + request.creditAccountNo());

        Element amt = append(doc, cdtTrfTxInf, "Amt");
        Element instdAmt = appendText(doc, amt, "InstdAmt", request.txnAmount().toPlainString());
        instdAmt.setAttribute("Ccy", request.txnCurrency());

        appendText(doc, cdtTrfTxInf, "ChrgBr", "CRED");

        Element cdtrAgt = append(doc, cdtTrfTxInf, "CdtrAgt");
        appendText(doc, append(doc, cdtrAgt, "FinInstnId"), "BICFI", request.receiverBankCode());

        Element cdtr = append(doc, cdtTrfTxInf, "Cdtr");
        appendText(doc, cdtr, "Nm", request.creditAccountName());

        Element cdtrAcct = append(doc, cdtTrfTxInf, "CdtrAcct");
        appendText(doc, append(doc, append(doc, cdtrAcct, "Id"), "Othr"), "Id", request.creditAccountNo());

        Element purp = append(doc, cdtTrfTxInf, "Purp");
        appendText(doc, purp, "Prtry", "CRED");

        Element rmtInf = append(doc, cdtTrfTxInf, "RmtInf");
        appendText(doc, rmtInf, "Ustrd", request.txnDescription());
    }

    private Element append(Document doc, Element parent, String localName) {
        Element element = doc.createElementNS(NS, localName);
        parent.appendChild(element);
        return element;
    }

    private Element appendText(Document doc, Element parent, String localName, String text) {
        Element element = append(doc, parent, localName);
        element.setTextContent(text);
        return element;
    }

    private String serialize(Document doc) throws Exception {
        TransformerFactory transformerFactory = XmlSecurity.newSecuredTransformerFactory();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(doc), new StreamResult(out));
        return out.toString(StandardCharsets.UTF_8);
    }
}
