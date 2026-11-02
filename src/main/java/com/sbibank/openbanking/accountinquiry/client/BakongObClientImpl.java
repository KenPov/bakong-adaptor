package com.sbibank.openbanking.accountinquiry.client;

import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryUpstreamRequest;
import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryUpstreamResponse;
import com.sbibank.openbanking.accountinquiry.dto.AuthenticateRequest;
import com.sbibank.openbanking.accountinquiry.dto.AuthenticateResponse;
import com.sbibank.openbanking.common.error.UpstreamCallException;
import com.sbibank.openbanking.config.BakongObClientProperties;
import com.sbibank.openbanking.config.OurBankProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class BakongObClientImpl implements BakongObClient {

    private final RestClient restClient;
    private final BakongObClientProperties properties;
    private final OurBankProperties ourBank;

    public BakongObClientImpl(RestClient bakongObClientRestClient, BakongObClientProperties properties, OurBankProperties ourBank) {
        this.restClient = bakongObClientRestClient;
        this.properties = properties;
        this.ourBank = ourBank;
    }

    @Override
    public String authenticate() {
        AuthenticateResponse response = restClient
                .post()
                .uri(properties.authUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AuthenticateRequest(properties.username(), properties.password()))
                .retrieve()
                .body(AuthenticateResponse.class);

        if (response == null || response.data() == null || response.data().token() == null) {
            throw new UpstreamCallException("Bakong OB hub authenticate response did not contain a token");
        }
        return response.data().token();
    }

    @Override
    public AccountInquiryUpstreamResponse inquireAccount(String accountNumber, String participantCode, String bearerToken) {
        AccountInquiryUpstreamResponse response = restClient
                .post()
                .uri(properties.accountInquiryUrl())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken)
                .header("Local-Part-Code", participantCode)
                .header("App-Name", ourBank.appName())
                .header("Type", "LOCAL")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new AccountInquiryUpstreamRequest(accountNumber))
                .retrieve()
                .body(AccountInquiryUpstreamResponse.class);

        if (response == null) {
            throw new UpstreamCallException("Bakong OB hub account inquiry returned an empty response");
        }
        return response;
    }
}
