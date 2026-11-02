package com.sbibank.openbanking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Pure context-load smoke test: verifies bean wiring (including that the two {@code RestClient} beans -
 * one per downstream integration - are unambiguous for constructor injection) without making any network
 * calls or requiring real downstream systems.
 */
@SpringBootTest
@TestPropertySource(
        properties = {
            "BAKONG_OB_CLIENT_AUTH_URL=http://localhost:0/v1/authenticate",
            "BAKONG_OB_CLIENT_ACCOUNT_INQUIRY_URL=http://localhost:0/api/v1/account_inquiry/account",
            "BAKONG_OB_CLIENT_USERNAME=test-user",
            "BAKONG_OB_CLIENT_PASSWORD=test-password",
            "CIFTP_API_URL=http://localhost:0",
            "CIFTP_API_USER=test-user",
            "CIFTP_API_PWD=test-password"
        })
class OpenBankingApplicationTests {

    @Test
    void contextLoads() {}
}
