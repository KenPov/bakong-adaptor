package com.sbibank.openbanking.accountinquiry;

import static org.assertj.core.api.Assertions.assertThat;

import com.sbibank.openbanking.accountinquiry.client.BakongObClientImpl;
import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryApiResponse;
import com.sbibank.openbanking.accountinquiry.service.AccountInquiryResponseMapper;
import com.sbibank.openbanking.accountinquiry.service.AccountInquiryService;
import com.sbibank.openbanking.config.BakongObClientProperties;
import com.sbibank.openbanking.config.OurBankProperties;
import java.io.IOException;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class AccountInquiryServiceTest {

    private MockWebServer server;
    private AccountInquiryService service;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        String base = server.url("/").toString();
        BakongObClientProperties properties = new BakongObClientProperties(
                base + "v1/authenticate",
                base + "api/v1/account_inquiry/account",
                "sit-user",
                "sit-password",
                Duration.ofSeconds(5),
                Duration.ofSeconds(5));
        OurBankProperties ourBank = new OurBankProperties(
                "SBHRKHPPXXX",
                "BANK SBHRKHPPXXX",
                "SBI LY HOUR Bank PLC.",
                new OurBankProperties.PostalAddress("St. 128 corner no 169", "219", "Phnom Penh", "KH"));

        RestClient restClient = RestClient.create();
        BakongObClientImpl client = new BakongObClientImpl(restClient, properties, ourBank);
        service = new AccountInquiryService(client, new AccountInquiryResponseMapper());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void happyPathAuthenticatesThenInquiresAndMapsResponse() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"data\":{\"token\":\"tok-123\"}}"));
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(
                        """
                        {
                          "responseCode": 0,
                          "errorCode": null,
                          "responseMessage": "OK",
                          "data": {
                            "accountInfo": {
                              "name": "JANE DOE",
                              "accountNumber": "000999888777",
                              "bankCode": "ABCDKHPPXXX",
                              "ccy": ["USD"],
                              "type": "CURRENT",
                              "kycStatus": "FULL_KYC",
                              "publicOperations": ["RETAIL"]
                            }
                          }
                        }
                        """));

        AccountInquiryApiResponse response = service.inquire("000999888777", "ABCD");

        assertThat(response.status().code()).isEqualTo(0);
        assertThat(response.data().accountData().accountName()).isEqualTo("JANE DOE");
        assertThat(response.data().accountData().accountType()).isEqualTo("02");
        assertThat(response.data().accountData().kycStatus()).isEqualTo("1");

        RecordedRequest authRequest = server.takeRequest();
        assertThat(authRequest.getPath()).contains("/v1/authenticate");

        RecordedRequest inquiryRequest = server.takeRequest();
        assertThat(inquiryRequest.getPath()).contains("/api/v1/account_inquiry/account");
        assertThat(inquiryRequest.getHeader("Authorization")).isEqualTo("Bearer tok-123");
        assertThat(inquiryRequest.getHeader("Local-Part-Code")).isEqualTo("ABCD");
        assertThat(inquiryRequest.getHeader("Type")).isEqualTo("LOCAL");
    }

    @Test
    void authenticateFailureReturnsInternalErrorEnvelopeWithoutThrowing() {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

        AccountInquiryApiResponse response = service.inquire("000999888777", "ABCD");

        assertThat(response.status().code()).isEqualTo(1);
        assertThat(response.status().errorCode()).isEqualTo("999");
        assertThat(response.status().error()).isEqualTo("Internal Server Error");
        assertThat(response.data()).isNull();
    }
}
