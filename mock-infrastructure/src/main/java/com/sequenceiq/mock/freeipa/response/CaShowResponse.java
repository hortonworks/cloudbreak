package com.sequenceiq.mock.freeipa.response;

import java.util.List;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.Ca;

@Component
public class CaShowResponse extends AbstractFreeIpaResponse<Ca> {

    private static final String CERT = "MIID0DCCArigAwIBAgIBATANBgkqhkiG9w0BAQsFADBNMSswKQYDVQQKDCJNTU9M"
            + "TkFSLlhDVTItOFk4WC5XTC5DTE9VREVSQS5TSVRFMR4wHAYDVQQDDBVDZXJ0aWZp"
            + "Y2F0ZSBBdXRob3JpdHkwHhcNMjAxMTI0MTY0NjIyWhcNNDAxMTI0MTY0NjIyWjBN"
            + "MSswKQYDVQQKDCJNTU9MTkFSLlhDVTItOFk4WC5XTC5DTE9VREVSQS5TSVRFMR4w"
            + "HAYDVQQDDBVDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwggEiMA0GCSqGSIb3DQEBAQUA"
            + "A4IBDwAwggEKAoIBAQCs+bHe1JUW09+H1wjt5kQRnAXfXKixKrpMERBLuy1oQCFr"
            + "cKvNoj9DCHOMbv1866KA8DomIf7wRcQ+3wNF5g+9jbLbRS+ecaz/I0YQ/6vMP6CZ"
            + "2r95eov7eOWGKWHXKV9W+oh7z/SJomGJa0Vsg4r+KhZ50qAVVY800TDFA0zR4EMk"
            + "K18OhNaznSBnwwm3soJliDOVvdYHLPPHNmr4s9UkjOIFaq5LUWEEvKKTfbYKPoNz"
            + "z7B8l6YIZpoLd3cnmk2rYDnupDY2XLzCL4V7uODiMxQVp1fnznRTv/7/bM3KDnvm"
            + "fxT/qsc7ScRdHV06frJQHxym1WaKsS/zSeWscoHTAgMBAAGjgbowgbcwHwYDVR0j"
            + "BBgwFoAU+l2j7nzYVRwLnUpO7JpgDJUdlGYwDwYDVR0TAQH/BAUwAwEB/zAOBgNV"
            + "HQ8BAf8EBAMCAcYwHQYDVR0OBBYEFPpdo+582FUcC51KTuyaYAyVHZRmMFQGCCsG"
            + "AQUFBwEBBEgwRjBEBggrBgEFBQcwAYY4aHR0cDovL2lwYS1jYS5tbW9sbmFyLnhj"
            + "dTItOHk4eC53bC5jbG91ZGVyYS5zaXRlL2NhL29jc3AwDQYJKoZIhvcNAQELBQAD"
            + "ggEBACnSvXDRE1LK7smj+eqsX9GjN5a0mJs6i1Q4WYP1/Ov/w+6Q85Jy65fWplE1"
            + "daiT642In/hyge9lj6HZ3N1+O5hCu0POBtkoWI9wlbulhB4UNSr6qXk2SgSNGs92"
            + "u+TccMuzaVy/TJnGnJmXvKhLv9w0+6EFz5FD+zz6dfInaYU5UguzNYw/1JB4gohQ"
            + "7Ko98xTeYzN9K69PZQALmZqtUhKWUkBvzeGnAcFR/KfxJeZ5l/jK292PRxyksCrX"
            + "VQVOdUfPDN5FeHnS0/ixoOtNQG7YdmIp+ZaCmerpJnAFOGEgs+vj51eX8omH0jhn"
            + "cSQj9z09/bLeGI/9RUiMvEJ5Bvg=";

    @Override
    public String method() {
        return "ca_show";
    }

    @Override
    protected Ca handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        Ca ca = new Ca();
        ca.setCertificate(CERT);
        return ca;
    }
}
