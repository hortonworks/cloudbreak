package com.sequenceiq.freeipa.service.freeipa.cert.root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.entity.RootCert;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.config.RootCertRegisterService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaRootCertificateServiceTest {

    private static final String ENV_CRN = "envCrn";

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private StackService stackService;

    @Mock
    private RootCertService rootCertService;

    @Mock
    private RootCertRegisterService rootCertRegisterService;

    @InjectMocks
    private FreeIpaRootCertificateService underTest;

    @Test
    public void testFetchFromDb() throws FreeIpaClientException {
        RootCert rootCert = new RootCert();
        rootCert.setCert("test");
        when(rootCertService.findByEnvironmentCrn(ENV_CRN)).thenReturn(Optional.of(rootCert));

        String result = underTest.getRootCertificate(ENV_CRN, ACCOUNT_ID);

        assertEquals(rootCert.getCert(), result);
        verifyNoInteractions(rootCertRegisterService);
    }

    @Test
    public void testNotInDb() throws FreeIpaClientException {
        RootCert rootCert = new RootCert();
        rootCert.setCert("test");
        when(rootCertService.findByEnvironmentCrn(ENV_CRN)).thenReturn(Optional.empty());
        Stack stack = new Stack();
        when(stackService.getByEnvironmentCrnAndAccountId(ENV_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(rootCertRegisterService.register(stack)).thenReturn(rootCert);

        String result = underTest.getRootCertificate(ENV_CRN, ACCOUNT_ID);

        assertEquals(rootCert.getCert(), result);
    }
}