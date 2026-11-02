package com.sbibank.openbanking.fundtransfer.client;

import com.sbibank.openbanking.common.error.UpstreamCallException;
import com.sbibank.openbanking.config.CiftpProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class CiftpSoapClientImpl implements CiftpSoapClient {

    private final RestClient restClient;
    private final CiftpProperties properties;

    public CiftpSoapClientImpl(RestClient ciftpRestClient, CiftpProperties properties) {
        this.restClient = ciftpRestClient;
        this.properties = properties;
    }

    @Override
    public String makeFullFundTransfer(String soapEnvelopeXml) {
        // Use exchange(), not retrieve(), because SOAP faults commonly arrive on a non-2xx HTTP status and
        // the fault body must still be read (retrieve() would throw before the body could be inspected).
        String body = restClient
                .post()
                .uri(properties.soapEndpoint())
                .header("Content-Type", MediaType.TEXT_XML_VALUE + "; charset=UTF-8")
                .header("SOAPAction", "")
                .body(soapEnvelopeXml)
                .exchange((request, response) -> {
                    byte[] bytes = response.getBody().readAllBytes();
                    return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                });

        if (body == null || body.isBlank()) {
            throw new UpstreamCallException("CIFTP makeFullFundTransfer returned an empty response body");
        }
        return body;
    }
}
