package com.sbibank.openbanking.accountinquiry;

import static org.assertj.core.api.Assertions.assertThat;

import com.sbibank.openbanking.accountinquiry.dto.AccountInfo;
import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryApiResponse;
import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryUpstreamResponse;
import com.sbibank.openbanking.accountinquiry.service.AccountInquiryResponseMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AccountInquiryResponseMapperTest {

    private final AccountInquiryResponseMapper mapper = new AccountInquiryResponseMapper();

    @Test
    void mapsSuccessResponseWithLocalPartCode() {
        AccountInfo accountInfo = new AccountInfo(
                "JOHN DOE",
                "000123456789",
                "ABCDKHPPXXX",
                null,
                List.of("USD"),
                "SAVINGS",
                "2020-01-01",
                "ACTIVE",
                "FULL_KYC",
                List.of("RETAIL"),
                "OTHRKHPPXXX");
        AccountInquiryUpstreamResponse upstream =
                new AccountInquiryUpstreamResponse(0, null, null, new AccountInquiryUpstreamResponse.UpstreamData(accountInfo));

        AccountInquiryApiResponse response = mapper.toApiResponse(upstream);

        assertThat(response.status().code()).isEqualTo(0);
        assertThat(response.data().result()).isEqualTo("success");
        assertThat(response.data().productCode()).isEqualTo("BAKONG");
        assertThat(response.data().accountData().account()).isEqualTo("000123456789");
        assertThat(response.data().accountData().accountName()).isEqualTo("JOHN DOE");
        assertThat(response.data().accountData().accountCurrency()).isEqualTo("USD");
        assertThat(response.data().accountData().kycStatus()).isEqualTo("1");
        assertThat(response.data().accountData().accountType()).isEqualTo("01");
        // localPartCode preferred over bankCode, trailing XXX stripped
        assertThat(response.data().accountData().bankCode()).isEqualTo("OTHRKHPP");
        assertThat(response.data().publicOperations()).isEqualTo(List.of("RETAIL"));
    }

    @Test
    void fallsBackToBankCodeWhenLocalPartCodeAbsent() {
        AccountInfo accountInfo = new AccountInfo(
                "JOHN DOE",
                "000123456789",
                "ABCDKHPPXXX",
                null,
                List.of("KHR"),
                "CURRENT",
                "2020-01-01",
                "ACTIVE",
                "PARTIAL_KYC",
                List.of(),
                null);
        AccountInquiryUpstreamResponse upstream =
                new AccountInquiryUpstreamResponse(0, null, null, new AccountInquiryUpstreamResponse.UpstreamData(accountInfo));

        AccountInquiryApiResponse response = mapper.toApiResponse(upstream);

        assertThat(response.data().accountData().bankCode()).isEqualTo("ABCDKHPP");
        assertThat(response.data().accountData().kycStatus()).isEqualTo("2");
        assertThat(response.data().accountData().accountType()).isEqualTo("02");
    }

    @ParameterizedTest
    @CsvSource({"WALLET,03", "LOAN,04", "UNKNOWN_TYPE,01"})
    void mapsAllAccountTypes(String type, String expectedCode) {
        AccountInfo accountInfo =
                new AccountInfo("N", "A", "B", null, List.of("USD"), type, "D", "S", "FULL_KYC", List.of(), null);
        AccountInquiryUpstreamResponse upstream =
                new AccountInquiryUpstreamResponse(0, null, null, new AccountInquiryUpstreamResponse.UpstreamData(accountInfo));

        assertThat(mapper.toApiResponse(upstream).data().accountData().accountType()).isEqualTo(expectedCode);
    }

    @ParameterizedTest
    @CsvSource({"015", "15"})
    void mapsAccountLocalNumberNotFound(String errorCode) {
        AccountInquiryUpstreamResponse upstream = new AccountInquiryUpstreamResponse(1, errorCode, "irrelevant", null);

        AccountInquiryApiResponse response = mapper.toApiResponse(upstream);

        assertThat(response.status().code()).isEqualTo(1);
        assertThat(response.status().errorCode()).isEqualTo(65);
        assertThat(response.status().error()).isEqualTo("The Account Local Number is not found.");
        assertThat(response.data()).isNull();
    }

    @Test
    void passesThroughGenericUpstreamError() {
        AccountInquiryUpstreamResponse upstream = new AccountInquiryUpstreamResponse(1, "042", "Account frozen", null);

        AccountInquiryApiResponse response = mapper.toApiResponse(upstream);

        assertThat(response.status().code()).isEqualTo(1);
        assertThat(response.status().errorCode()).isEqualTo("042");
        assertThat(response.status().error()).isEqualTo("Account frozen");
    }
}
