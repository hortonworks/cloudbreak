package com.sequenceiq.freeipa.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.RootCert;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.cert.root.RootCertService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class RootCertRegisterServiceTest {

    private static final String EXPECTED_CERT = "-----BEGIN CERTIFICATE-----\n"
            + "MIID0DCCArigAwIBAgIBATANBgkqhkiG9w0BAQsFADBNMSswKQYDVQQKDCJNTU9M\n"
            + "TkFSLlhDVTItOFk4WC5XTC5DTE9VREVSQS5TSVRFMR4wHAYDVQQDDBVDZXJ0aWZp\n"
            + "Y2F0ZSBBdXRob3JpdHkwHhcNMjAxMTI0MTY0NjIyWhcNNDAxMTI0MTY0NjIyWjBN\n"
            + "MSswKQYDVQQKDCJNTU9MTkFSLlhDVTItOFk4WC5XTC5DTE9VREVSQS5TSVRFMR4w\n"
            + "HAYDVQQDDBVDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwggEiMA0GCSqGSIb3DQEBAQUA\n"
            + "A4IBDwAwggEKAoIBAQCs+bHe1JUW09+H1wjt5kQRnAXfXKixKrpMERBLuy1oQCFr\n"
            + "cKvNoj9DCHOMbv1866KA8DomIf7wRcQ+3wNF5g+9jbLbRS+ecaz/I0YQ/6vMP6CZ\n"
            + "2r95eov7eOWGKWHXKV9W+oh7z/SJomGJa0Vsg4r+KhZ50qAVVY800TDFA0zR4EMk\n"
            + "K18OhNaznSBnwwm3soJliDOVvdYHLPPHNmr4s9UkjOIFaq5LUWEEvKKTfbYKPoNz\n"
            + "z7B8l6YIZpoLd3cnmk2rYDnupDY2XLzCL4V7uODiMxQVp1fnznRTv/7/bM3KDnvm\n"
            + "fxT/qsc7ScRdHV06frJQHxym1WaKsS/zSeWscoHTAgMBAAGjgbowgbcwHwYDVR0j\n"
            + "BBgwFoAU+l2j7nzYVRwLnUpO7JpgDJUdlGYwDwYDVR0TAQH/BAUwAwEB/zAOBgNV\n"
            + "HQ8BAf8EBAMCAcYwHQYDVR0OBBYEFPpdo+582FUcC51KTuyaYAyVHZRmMFQGCCsG\n"
            + "AQUFBwEBBEgwRjBEBggrBgEFBQcwAYY4aHR0cDovL2lwYS1jYS5tbW9sbmFyLnhj\n"
            + "dTItOHk4eC53bC5jbG91ZGVyYS5zaXRlL2NhL29jc3AwDQYJKoZIhvcNAQELBQAD\n"
            + "ggEBACnSvXDRE1LK7smj+eqsX9GjN5a0mJs6i1Q4WYP1/Ov/w+6Q85Jy65fWplE1\n"
            + "daiT642In/hyge9lj6HZ3N1+O5hCu0POBtkoWI9wlbulhB4UNSr6qXk2SgSNGs92\n"
            + "u+TccMuzaVy/TJnGnJmXvKhLv9w0+6EFz5FD+zz6dfInaYU5UguzNYw/1JB4gohQ\n"
            + "7Ko98xTeYzN9K69PZQALmZqtUhKWUkBvzeGnAcFR/KfxJeZ5l/jK292PRxyksCrX\n"
            + "VQVOdUfPDN5FeHnS0/ixoOtNQG7YdmIp+ZaCmerpJnAFOGEgs+vj51eX8omH0jhn\n"
            + "cSQj9z09/bLeGI/9RUiMvEJ5Bvg=\n"
            + "-----END CERTIFICATE-----";

    private static final String INPUT_CERT = "MIID0DCCArigAwIBAgIBATANBgkqhkiG9w0BAQsFADBNMSswKQYDVQQKDCJNTU9M"
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

    @Mock
    private FreeIpaClientFactory clientFactory;

    @Mock
    private StackService stackService;

    @Mock
    private RootCertService rootCertService;

    @InjectMocks
    private RootCertRegisterService underTest;

    @Test
    public void testDeleteNotFoundHandled() {
        Stack stack = new Stack();
        doThrow(new NotFoundException("asdgf")).when(rootCertService).deleteByStack(stack);

        underTest.delete(stack);
    }

    @Test
    public void testRegisterByStackId() throws FreeIpaClientException {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("ENV");
        when(stackService.getStackById(1L)).thenReturn(stack);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(clientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.getRootCertificate()).thenReturn(INPUT_CERT);
        ArgumentCaptor<RootCert> certArgumentCaptor = ArgumentCaptor.forClass(RootCert.class);
        when(rootCertService.save(certArgumentCaptor.capture())).thenReturn(new RootCert());

        underTest.register(1L);

        RootCert result = certArgumentCaptor.getValue();
        assertEquals(stack.getEnvironmentCrn(), result.getEnvironmentCrn());
        assertEquals(stack, result.getStack());
        assertEquals(EXPECTED_CERT, result.getCert());
    }

    @Test
    public void testRegisterByStack() throws FreeIpaClientException {
        Stack stack = new Stack();
        stack.setEnvironmentCrn("ENV");
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(clientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.getRootCertificate()).thenReturn(INPUT_CERT);

        when(rootCertService.save(any(RootCert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RootCert result = underTest.register(stack);

        assertEquals(stack.getEnvironmentCrn(), result.getEnvironmentCrn());
        assertEquals(stack, result.getStack());
        assertEquals(EXPECTED_CERT, result.getCert());
    }

}