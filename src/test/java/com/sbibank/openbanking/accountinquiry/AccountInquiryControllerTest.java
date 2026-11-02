package com.sbibank.openbanking.accountinquiry;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryApiResponse;
import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryStatus;
import com.sbibank.openbanking.accountinquiry.service.AccountInquiryService;
import com.sbibank.openbanking.accountinquiry.web.AccountInquiryController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountInquiryController.class)
class AccountInquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AccountInquiryService service;

    @Test
    void missingParticipantCodeReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/account-inquiry-to-other-bank").param("accountNumber", "000123456789"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
    }

    @Test
    void successPassesThroughServiceResponse() throws Exception {
        when(service.inquire(anyString(), anyString()))
                .thenReturn(AccountInquiryApiResponse.error(AccountInquiryStatus.error(65, "The Account Local Number is not found.")));

        mockMvc.perform(get("/api/v1/account-inquiry-to-other-bank")
                        .param("accountNumber", "000123456789")
                        .param("participantCode", "ABCD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(1))
                .andExpect(jsonPath("$.status.errorCode").value(65));
    }
}
