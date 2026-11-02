package com.sbibank.openbanking.fundtransfer.web;

import com.sbibank.openbanking.fundtransfer.dto.FullFundTransferApiResponse;
import com.sbibank.openbanking.fundtransfer.dto.FullFundTransferRequest;
import com.sbibank.openbanking.fundtransfer.service.FullFundTransferService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Migrated from {@code bakong-full-fund-transfer.xml}: a single-shot interbank transfer built as an ISO
 * 20022 pain.001 message and sent over SOAP to the CIFTP core-banking adapter.
 *
 * <p>Always responds {@code 200 OK} - success/failure is signalled purely via {@code status.code} in the
 * body, matching the legacy contract that existing consumers already rely on.
 */
@RestController
@RequestMapping("/api/v1/bakong-full-fund-transfer")
public class FullFundTransferController {

    private final FullFundTransferService service;

    public FullFundTransferController(FullFundTransferService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<FullFundTransferApiResponse> transfer(@Valid @RequestBody FullFundTransferRequest request) {
        return ResponseEntity.ok(service.transfer(request));
    }
}
