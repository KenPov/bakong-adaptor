package com.sbibank.openbanking.fundtransfer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.xmlunit.matchers.EvaluateXPathMatcher.hasXPath;

import com.sbibank.openbanking.config.OurBankProperties;
import com.sbibank.openbanking.fundtransfer.dto.FullFundTransferRequest;
import com.sbibank.openbanking.fundtransfer.service.Pain001MessageBuilder;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Pain001MessageBuilderTest {

    private static final String NS = "urn:iso:std:iso:20022:tech:xsd:pain.001.001.05";

    private final Pain001MessageBuilder builder = new Pain001MessageBuilder();
    private OurBankProperties ourBank;
    private String xml;

    @BeforeEach
    void setUp() {
        ourBank = new OurBankProperties(
                "SBHRKHPPXXX",
                "BANK SBHRKHPPXXX",
                "SBI LY HOUR Bank PLC.",
                new OurBankProperties.PostalAddress("St. 128 corner no 169", "219", "Phnom Penh", "KH"));

        FullFundTransferRequest request = new FullFundTransferRequest(
                "2026-09-15T10:00:00",
                "2026-09-15",
                "MSG123456",
                new BigDecimal("150.50"),
                "OTHRKHPPXXX",
                "000111222333",
                "USD",
                "CREDITOR NAME",
                "unused-e2e-id",
                "000999888777",
                "DEBTOR NAME",
                "EXTREF001",
                "unused-instr-id",
                "PMTINFO001",
                "invoice payment");

        xml = builder.build(request, ourBank);
    }

    @Test
    void mapsAllFieldsToTheExpectedXPaths() {
        Map<String, String> ns = Map.of("p", NS);

        assertThat(xml, hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:GrpHdr/p:MsgId", equalTo("MSG123456")).withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:GrpHdr/p:CreDtTm", equalTo("2026-09-15T10:00:00"))
                        .withNamespaceContext(ns));
        assertThat(xml, hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:GrpHdr/p:NbOfTxs", equalTo("1")).withNamespaceContext(ns));
        assertThat(xml, hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:GrpHdr/p:CtrlSum", equalTo("150.50")).withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:GrpHdr/p:InitgPty/p:Nm", equalTo("SBI LY HOUR Bank PLC."))
                        .withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:GrpHdr/p:InitgPty/p:PstlAdr/p:StrtNm", equalTo("St. 128 corner no 169"))
                        .withNamespaceContext(ns));

        assertThat(xml, hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:PmtInfId", equalTo("PMTINFO001")).withNamespaceContext(ns));
        assertThat(xml, hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:PmtMtd", equalTo("TRF")).withNamespaceContext(ns));
        assertThat(xml, hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:BtchBookg", equalTo("false")).withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:ReqdExctnDt", equalTo("2026-09-15")).withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:Dbtr/p:Nm", equalTo("DEBTOR NAME")).withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:DbtrAcct/p:Id/p:Othr/p:Id", equalTo("000111222333"))
                        .withNamespaceContext(ns));
        assertThat(
                xml, hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:DbtrAcct/p:Ccy", equalTo("USD")).withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:DbtrAgt/p:FinInstnId/p:BICFI", equalTo("SBHRKHPPXXX"))
                        .withNamespaceContext(ns));

        // InstrId/EndToEndId are derived independently of paymentInstrId/paymentEndToEndId, matching legacy.
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:CdtTrfTxInf/p:PmtId/p:InstrId", equalTo("SBHRKHPPXXX/MSG123456"))
                        .withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath(
                                "//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:CdtTrfTxInf/p:PmtId/p:EndToEndId",
                                equalTo("000111222333-000999888777"))
                        .withNamespaceContext(ns));

        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:CdtTrfTxInf/p:Amt/p:InstdAmt", equalTo("150.50"))
                        .withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:CdtTrfTxInf/p:Amt/p:InstdAmt/@Ccy", equalTo("USD"))
                        .withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:CdtTrfTxInf/p:ChrgBr", equalTo("CRED")).withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:CdtTrfTxInf/p:CdtrAgt/p:FinInstnId/p:BICFI", equalTo("OTHRKHPPXXX"))
                        .withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:CdtTrfTxInf/p:Cdtr/p:Nm", equalTo("CREDITOR NAME"))
                        .withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:CdtTrfTxInf/p:CdtrAcct/p:Id/p:Othr/p:Id", equalTo("000999888777"))
                        .withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:CdtTrfTxInf/p:Purp/p:Prtry", equalTo("CRED"))
                        .withNamespaceContext(ns));
        assertThat(
                xml,
                hasXPath("//p:Document/p:CstmrCdtTrfInitn/p:PmtInf/p:CdtTrfTxInf/p:RmtInf/p:Ustrd", equalTo("invoice payment"))
                        .withNamespaceContext(ns));
    }
}
