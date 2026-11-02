package com.sbibank.openbanking.fundtransfer.dto;

public record FullFundTransferStatus(int code, String errorCode, String errorMessage) {

    public static FullFundTransferStatus success() {
        return new FullFundTransferStatus(0, null, null);
    }

    public static FullFundTransferStatus error(String errorMessage) {
        return new FullFundTransferStatus(1, null, errorMessage);
    }
}
