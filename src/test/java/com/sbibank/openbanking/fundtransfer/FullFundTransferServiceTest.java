package com.sbibank.openbanking.fundtransfer;

import static org.assertj.core.api.Assertions.assertThat;

import com.sbibank.openbanking.config.CiftpProperties;
import com.sbibank.openbanking.config.OurBankProperties;
import com.sbibank.openbanking.fundtransfer.client.CiftpSoapClientImpl;
import com.sbibank.openbanking.fundtransfer.dto.FullFundTransferApiResponse;
import com.sbibank.openbanking.fundtransfer.dto.FullFundTransferRequest;
import com.sbibank.openbanking.fundtransfer.service.FullFundTransferService;
import com.sbibank.openbanking.fundtransfer.service.Pain001MessageBuilder;
import com.sbibank.openbanking.fundtransfer.service.SoapEnvelopeBuilder;
import com.sbibank.openbanking.fundtransfer.service.SoapResponseParser;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class FullFundTransferServiceTest {

    private MockWebServer server;
    private FullFundTransferService service;
    private FullFundTransferRequest request;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        CiftpProperties ciftpProperties =
                new CiftpProperties(server.url("/").toString(), "/ciftp-payment-service/cb-adapter/BakongWebService", "sit-user", "sit-pwd", Duration.ofSeconds(5), Duration.ofSeconds(5));
        OurBankProperties ourBank = new OurBankProperties(
                "SBHRKHPPXXX",
                "BANK SBHRKHPPXXX",
                "SBI LY HOUR Bank PLC.",
                new OurBankProperties.PostalAddress("St. 128 corner no 169", "219", "Phnom Penh", "KH"));

        CiftpSoapClientImpl client = new CiftpSoapClientImpl(RestClient.create(), ciftpProperties);
        service = new FullFundTransferService(
                new Pain001MessageBuilder(), new SoapEnvelopeBuilder(), new SoapResponseParser(), client, ciftpProperties, ourBank);

        request = new FullFundTransferRequest(
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
    }

    @AfterEach
    void tearDown() {
        try {
            server.shutdown();
        } catch (IOException | IllegalStateException ignored) {
            // already shut down by a test simulating a downstream outage
        }
    }

    @Test
    void successfulTransferExtractsFieldsFromSoapResponse() throws InterruptedException {
        String innerXml = "&lt;OrgnlMsgId&gt;MSG123456&lt;/OrgnlMsgId&gt;"
                + "&lt;OrgnlPmtInfId&gt;PMTINFO001&lt;/OrgnlPmtInfId&gt;"
                + "&lt;Ustrd&gt;invoice payment&lt;/Ustrd&gt;";
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "text/xml")
                .setBody(
                        """
                        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:web="http://webservice.nbc.org.kh/">
                          <soapenv:Body>
                            <web:makeFullFundTransferResponse>
                              <web:return>%s</web:return>
                            </web:makeFullFundTransferResponse>
                          </soapenv:Body>
                        </soapenv:Envelope>
                        """
                                .formatted(innerXml)));

        FullFundTransferApiResponse response = service.transfer(request);

        assertThat(response.status().code()).isEqualTo(0);
        assertThat(response.data().requestMsgId()).isEqualTo("MSG123456");
        assertThat(response.data().paymentInfoId()).isEqualTo("PMTINFO001");
        assertThat(response.data().hash()).isEqualTo("invoice payment");

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/ciftp-payment-service/cb-adapter/BakongWebService");
        String sentBody = recorded.getBody().readUtf8();
        assertThat(sentBody).contains("<web:cm_user_name>sit-user</web:cm_user_name>");
        assertThat(sentBody).contains("<web:ext_ref>EXTREF001</web:ext_ref>");
        assertThat(sentBody).contains("MSG123456");
    }

    @Test
    void soapFaultOnNon2xxStatusIsStillParsed() {
        server.enqueue(new MockResponse()
                .setResponseCode(500)
                .addHeader("Content-Type", "text/xml")
                .setBody(
                        """
                        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                          <soapenv:Body>
                            <soapenv:Fault>
                              <faultstring>Duplicate ext_ref</faultstring>
                            </soapenv:Fault>
                          </soapenv:Body>
                        </soapenv:Envelope>
                        """));

        FullFundTransferApiResponse response = service.transfer(request);

        assertThat(response.status().code()).isEqualTo(1);
        assertThat(response.status().errorMessage()).isEqualTo("Duplicate ext_ref");
    }

    @Test
    void downstreamOutageReturnsInternalErrorEnvelopeWithoutThrowing() throws IOException {
        server.shutdown(); // nothing is listening -> connection failure

        FullFundTransferApiResponse response = service.transfer(request);

        assertThat(response.status().code()).isEqualTo(1);
        assertThat(response.status().errorMessage()).isEqualTo("Internal Server Error");
    }
}
