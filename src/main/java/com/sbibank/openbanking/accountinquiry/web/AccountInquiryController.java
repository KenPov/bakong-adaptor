package com.sbibank.openbanking.accountinquiry.web;

import com.sbibank.openbanking.accountinquiry.dto.AccountInquiryApiResponse;
import com.sbibank.openbanking.accountinquiry.service.AccountInquiryService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Migrated from {@code account-inquiry-to-other-bank.xml}: our mobile app checks another bank's account
 * via the Bakong OB hub before initiating a transfer.
 *
 * <p>Always responds {@code 200 OK} - success/failure is signalled purely via {@code status.code} in the
 * body, matching the legacy contract that existing consumers already rely on.
 */
@Validated
@RestController
@RequestMapping("/api/v1/account-inquiry-to-other-bank")
public class AccountInquiryController {

    private final AccountInquiryService service;

    public AccountInquiryController(AccountInquiryService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<AccountInquiryApiResponse> inquire(
            @RequestParam @NotBlank String accountNumber, @RequestParam @NotBlank String participantCode) {
        return ResponseEntity.ok(service.inquire(accountNumber, participantCode));
    }
}
