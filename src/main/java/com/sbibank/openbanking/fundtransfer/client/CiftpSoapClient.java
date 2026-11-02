package com.sbibank.openbanking.fundtransfer.client;

public interface CiftpSoapClient {

    /**
     * Posts a raw SOAP 1.1 envelope to the CIFTP {@code BakongWebService} adapter and returns the raw
     * response body, regardless of HTTP status - SOAP faults commonly arrive on a non-2xx status and still
     * need their body parsed.
     */
    String makeFullFundTransfer(String soapEnvelopeXml);
}
