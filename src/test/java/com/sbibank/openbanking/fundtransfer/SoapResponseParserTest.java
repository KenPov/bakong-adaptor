package com.sbibank.openbanking.fundtransfer;

import static org.assertj.core.api.Assertions.assertThat;

import com.sbibank.openbanking.fundtransfer.dto.FullFundTransferApiResponse;
import com.sbibank.openbanking.fundtransfer.service.SoapResponseParser;
import org.junit.jupiter.api.Test;

class SoapResponseParserTest {

    private final SoapResponseParser parser = new SoapResponseParser();

    @Test
    void parsesSuccessfulResponse() {
        String innerXml = "&lt;OrgnlMsgId&gt;MSG123456&lt;/OrgnlMsgId&gt;"
                + "&lt;OrgnlPmtInfId&gt;PMTINFO001&lt;/OrgnlPmtInfId&gt;"
                + "&lt;Ustrd&gt;invoice payment&lt;/Ustrd&gt;";
        String soapResponse =
                """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:web="http://webservice.nbc.org.kh/">
                  <soapenv:Body>
                    <web:makeFullFundTransferResponse>
                      <web:return>%s</web:return>
                    </web:makeFullFundTransferResponse>
                  </soapenv:Body>
                </soapenv:Envelope>
                """
                        .formatted(innerXml);

        FullFundTransferApiResponse response = parser.parse(soapResponse);

        assertThat(response.status().code()).isEqualTo(0);
        assertThat(response.data().requestMsgId()).isEqualTo("MSG123456");
        assertThat(response.data().paymentInfoId()).isEqualTo("PMTINFO001");
        assertThat(response.data().hash()).isEqualTo("invoice payment");
    }

    @Test
    void parsesSoapFault() {
        String soapResponse =
                """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <soapenv:Fault>
                      <faultcode>soapenv:Server</faultcode>
                      <faultstring>Duplicate ext_ref</faultstring>
                    </soapenv:Fault>
                  </soapenv:Body>
                </soapenv:Envelope>
                """;

        FullFundTransferApiResponse response = parser.parse(soapResponse);

        assertThat(response.status().code()).isEqualTo(1);
        assertThat(response.status().errorMessage()).isEqualTo("Duplicate ext_ref");
        assertThat(response.data()).isNull();
    }

    @Test
    void gracefullyHandlesMalformedResponse() {
        FullFundTransferApiResponse response = parser.parse("not xml at all");

        assertThat(response.status().code()).isEqualTo(1);
        assertThat(response.data()).isNull();
    }
}
