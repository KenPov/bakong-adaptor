package com.sbibank.openbanking.fundtransfer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sbibank.openbanking.fundtransfer.dto.FullFundTransferApiResponse;
import com.sbibank.openbanking.fundtransfer.dto.FullFundTransferData;
import com.sbibank.openbanking.fundtransfer.service.FullFundTransferService;
import com.sbibank.openbanking.fundtransfer.web.FullFundTransferController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FullFundTransferController.class)
class FullFundTransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FullFundTransferService service;

    @Test
    void missingRequiredFieldsReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/bakong-full-fund-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
    }

    @Test
    void malformedJsonBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/bakong-full-fund-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation_failed"));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/v1/bakong-full-fund-transfer"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("method_not_allowed"));
    }

    @Test
    void successPassesThroughServiceResponse() throws Exception {
        when(service.transfer(any()))
                .thenReturn(FullFundTransferApiResponse.success(new FullFundTransferData("MSG1", "PMT1", "hash-value")));

        mockMvc.perform(post("/api/v1/bakong-full-fund-transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequestJson())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value(0))
                .andExpect(jsonPath("$.data.requestMsgId").value("MSG1"));
    }

    private java.util.Map<String, Object> validRequestJson() {
        return java.util.Map.ofEntries(
                java.util.Map.entry("createDateTime", "2026-09-15T10:00:00"),
                java.util.Map.entry("requestExactDate", "2026-09-15"),
                java.util.Map.entry("requestMsgId", "MSG123456"),
                java.util.Map.entry("txnAmount", 150.50),
                java.util.Map.entry("receiverBankCode", "OTHRKHPPXXX"),
                java.util.Map.entry("debitAccountNo", "000111222333"),
                java.util.Map.entry("txnCurrency", "USD"),
                java.util.Map.entry("creditAccountName", "CREDITOR NAME"),
                java.util.Map.entry("paymentEndToEndId", "e2e-id"),
                java.util.Map.entry("creditAccountNo", "000999888777"),
                java.util.Map.entry("debitAccountName", "DEBTOR NAME"),
                java.util.Map.entry("extRefId", "EXTREF001"),
                java.util.Map.entry("paymentInstrId", "instr-id"),
                java.util.Map.entry("paymentInfoId", "PMTINFO001"),
                java.util.Map.entry("txnDescription", "invoice payment"));
    }
}
