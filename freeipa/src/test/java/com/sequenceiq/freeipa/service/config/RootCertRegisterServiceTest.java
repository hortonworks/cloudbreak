package com.sequenceiq.freeipa.service.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

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

    private static final String OLD_EXPECTED_CERT = "-----BEGIN CERTIFICATE-----\n"
            + "MIIEowIBAAKCAQEAv/iPGukH2DfwgWOjcxPAra3dKxKs3et1PER+cZgPc4g8B+AW\n"
            + "yewgdmhDXEjJpbbMgKxiu2bfM+qmTB5I5ceSN34TMv4v7GdQvZ17suOoEhULEqMF\n"
            + "NQ0UM238vMXLI+6NduM725Rr03QFAhS4s1LKuBbVfMJE8QB9bsB9Keqa+/Q66Jp9\n"
            + "QddQ41IMCQ3vWrphQ2oIB+2/6qik4rd90DJnpcpi6gT3jnR3DAaDX3CSw2PBQVAc\n"
            + "Wan0qPDb0HCN4ppFElLjJ5k218u1SIxH6Aqd21V+wWpcOdnunmrzfkOfs6PhHVe+\n"
            + "6Y8jA81Tza15bWCgiCJQ14plfU3HqmGM8pkPPwIBJQKCAQEAsGfdcqyfbL3IP4v9\n"
            + "+ws7byolLn/dHvQK3W9fb011+3ZFAFFhDJPKzavq3y4hFNF9pqw/DRJsPYuEDphe\n"
            + "qaKiCXrFiM3m2TxYCC/Zc/PK4C8DQY7iML/pDKpCMO0biM1fZlRE150k0CVsYsbu\n"
            + "6fkNTzeTvsBbBvmIBOg7qfotjZK6UECJa69FLhgL5yQYkwsS/7WK02n/dcDbKEi0\n"
            + "S5rJRWkxYnEn2EktkX8iysUX1fwc0uD+De9mgVriAu2RXNXeYoB96gxYiGeFL/W8\n"
            + "hqAER5tz2x6hRf+bF9D27JscVVTYJRjLRSAnvIUIS6BEpESBuGn1LEHF2kzyxZ+A\n"
            + "KyK7TQKBgQDh5LBLOoQKcbgh/phKG18un2LPRDcbUgLpCz13r+Ufd5Cs858JAe2q\n"
            + "cm7aWF//jPPa4fCIrNhM3TGQurP/AYAj3ziJEGE3zJu3ApAJEM/w5nmjgnI6VnO1\n"
            + "1Hfc39+eAhJ637byCN6zeeLQFtAjqiWKUsAXVWm//mdy6HrnZ0i2FwKBgQDZjnbJ\n"
            + "qLov3Y7OB2Tz6qH4UYVs+8NK1L1F0UkFr+iaU6GWzwjo1dzPt34/ZqOAzZI/eeMz\n"
            + "6PLHTgmRXdglZXyvMVJJiXXa7zfPN3DTkAUCpu7sOe1jU3W1qICgfspEvh57BjL+\n"
            + "uxyAiA5zKFSSl/+YGd/b4QY/IGVqrpr0a25RGQKBgQCShoAwzAKe/afesvr/ow1O\n"
            + "rJMeqMnMiDk9N7krCk9uW2TDNj2k+lT48Efnk0UwJBPMP4dD5by8PHMtci+QpwcC\n"
            + "gvQ9Ow6gvBH6Kyz+9iYECx535ek6mPEb/3BX6ylD5asfRQELsrn34FvFPzrms434\n"
            + "27res/GRSxKrZl2PLj0AfQKBgFJRlLsBko6m3BabA5qQIZw6hYMu1EXT9Jb1PjmA\n"
            + "1I1rwJnteP4naE6YdPVlG0V+N1ZJy5cZ31JU4QaSNhv84xHbT5FySEUAkahaKrDq\n"
            + "YsK7tFliBsujCfG1YRoiIwVBBJ1Anax+JnXSna8IV1oP/9iv2CmvFx7N/NxCER42\n"
            + "fMS9AoGBAOBaoUdij3AYpNXNbu3WgFwkkzxwiPdziwYiV81xv2jVg/JhWn34sUZT\n"
            + "8WltJh0VP1vW/zt3+Ay8TB08/lnOhCHzZGIVfqBvJXti4o4nK2bEylhZJBsvDFHW\n"
            + "phdH5oSG4alEPBxqoKJpeo+3KaApEEVXUjTqQPfFXvpoCebPc36i\n"
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

    private static final long STACK_ID = 1L;

    private static final String ENV_CRN = "env-crn";

    private static final long CERT_ID = 2L;

    @Mock
    private FreeIpaClientFactory clientFactory;

    @Mock
    private StackService stackService;

    @Mock
    private RootCertService rootCertService;

    @InjectMocks
    private RootCertRegisterService underTest;

    private final Stack stack = createStack();

    @Test
    public void testDeleteNotFoundHandled() {
        doThrow(new NotFoundException("asdgf")).when(rootCertService).deleteByStack(stack);

        underTest.delete(stack);
    }

    @Test
    public void testRegisterByStackId() throws FreeIpaClientException {
        when(stackService.getStackById(STACK_ID)).thenReturn(stack);
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(clientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.getRootCertificate()).thenReturn(INPUT_CERT);
        ArgumentCaptor<RootCert> certArgumentCaptor = ArgumentCaptor.forClass(RootCert.class);
        when(rootCertService.findByStackId(STACK_ID)).thenReturn(Optional.empty());
        when(rootCertService.save(certArgumentCaptor.capture())).thenReturn(new RootCert());

        underTest.register(STACK_ID);

        RootCert result = certArgumentCaptor.getValue();
        assertEquals(stack.getEnvironmentCrn(), result.getEnvironmentCrn());
        assertEquals(stack, result.getStack());
        assertEquals(EXPECTED_CERT, result.getCert());
    }

    @Test
    public void testRegisterByStack() throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        when(clientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.getRootCertificate()).thenReturn(INPUT_CERT);
        when(rootCertService.findByStackId(STACK_ID)).thenReturn(Optional.empty());

        when(rootCertService.save(any(RootCert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RootCert result = underTest.register(stack);

        assertEquals(stack.getEnvironmentCrn(), result.getEnvironmentCrn());
        assertEquals(stack, result.getStack());
        assertEquals(EXPECTED_CERT, result.getCert());
    }

    @Test
    public void testRegisterByStackShouldNotSaveTheRootCertWhenTheCurrentCertAvailableWithSameRootCertificate() throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        RootCert currentRootCert = createRootCert(EXPECTED_CERT);
        when(clientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.getRootCertificate()).thenReturn(INPUT_CERT);
        when(rootCertService.findByStackId(STACK_ID)).thenReturn(Optional.of(currentRootCert));

        RootCert result = underTest.register(stack);

        assertEquals(stack.getEnvironmentCrn(), result.getEnvironmentCrn());
        assertEquals(stack, result.getStack());
        assertEquals(EXPECTED_CERT, result.getCert());
        assertEquals(CERT_ID, result.getId());
        verify(clientFactory).getFreeIpaClientForStack(stack);
        verify(rootCertService).findByStackId(STACK_ID);
        verifyNoMoreInteractions(rootCertService);
    }

    @Test
    public void testRegisterByStackShouldSaveTheRootCertWhenTheCurrentCertAvailableWithOldRootCertificate() throws FreeIpaClientException {
        FreeIpaClient freeIpaClient = mock(FreeIpaClient.class);
        RootCert currentRootCert = createRootCert(OLD_EXPECTED_CERT);
        when(clientFactory.getFreeIpaClientForStack(stack)).thenReturn(freeIpaClient);
        when(freeIpaClient.getRootCertificate()).thenReturn(INPUT_CERT);
        when(rootCertService.findByStackId(STACK_ID)).thenReturn(Optional.of(currentRootCert));
        when(rootCertService.save(any(RootCert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RootCert result = underTest.register(stack);

        assertEquals(stack.getEnvironmentCrn(), result.getEnvironmentCrn());
        assertEquals(stack, result.getStack());
        assertEquals(EXPECTED_CERT, result.getCert());
        assertEquals(CERT_ID, result.getId());
        verify(clientFactory).getFreeIpaClientForStack(stack);
        verify(rootCertService).findByStackId(STACK_ID);
        verify(rootCertService).save(result);
    }

    private RootCert createRootCert(String certificate) {
        RootCert rootCert = new RootCert();
        rootCert.setId(CERT_ID);
        rootCert.setStack(stack);
        rootCert.setEnvironmentCrn(ENV_CRN);
        rootCert.setCert(certificate);
        return rootCert;
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setEnvironmentCrn(ENV_CRN);
        return stack;
    }

}