package com.sequenceiq.freeipa.service.freeipa.trust.setup;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.environments2api.model.DescribeEnvironmentResponse;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.KerberosInfo;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.cloudera.thunderhead.service.environments2api.model.PvcEnvironmentDetails;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.CrossRealmTrustService;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.freeipa.trust.statusvalidation.TrustStatusValidationService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.remoteenvironment.api.v1.environment.endpoint.RemoteEnvironmentEndpoint;

@ExtendWith(MockitoExtension.class)
class AddTrustServiceTest {

    private static final Long STACK_ID = 1L;

    @Mock
    private StackService stackService;

    @Mock
    private CrossRealmTrustService crossRealmTrustService;

    @Mock
    private FreeIpaClientFactory freeIpaClientFactory;

    @Mock
    private TrustStatusValidationService trustStatusValidationService;

    @Mock
    private RemoteEnvironmentEndpoint remoteEnvironmentEndpoint;

    @InjectMocks
    private AddTrustService underTest;

    @Mock
    private Stack stack;

    @Mock
    private CrossRealmTrust crossRealmTrust;

    @Mock
    private FreeIpaClient client;

    @Mock
    private ValidationResult validationResult;

    @BeforeEach
    void setUp() throws Exception {
        when(stackService.getByIdWithListsInTransaction(STACK_ID)).thenReturn(stack);
        crossRealmTrust = new CrossRealmTrust();
        crossRealmTrust.setTrustSecret("secret");
        crossRealmTrust.setKdcRealm("hybrid.cloudera.org");
        when(crossRealmTrustService.getByStackId(STACK_ID)).thenReturn(crossRealmTrust);
        lenient().doReturn(client).when(freeIpaClientFactory).getFreeIpaClientForStack(stack);
        when(trustStatusValidationService.validateTrustStatus(stack, crossRealmTrust)).thenReturn(validationResult);
        setKdcType("Active Directory");
    }

    @Test
    void successWithActiveDirectoryKdc() throws Exception {
        when(validationResult.hasError()).thenReturn(false);

        underTest.addAndValidateTrust(STACK_ID);

        verify(client).addTrust("secret", "ad", true, "HYBRID.CLOUDERA.ORG");
        verify(trustStatusValidationService).validateTrustStatus(stack, crossRealmTrust);
    }

    @Test
    void successWithMitKdc() throws Exception {
        when(validationResult.hasError()).thenReturn(false);
        setKdcType("MIT KDC");

        underTest.addAndValidateTrust(STACK_ID);

        verifyNoInteractions(client);
        verify(trustStatusValidationService).validateTrustStatus(stack, crossRealmTrust);
    }

    @Test
    void validationFailure() {
        when(validationResult.hasError()).thenReturn(true);
        when(validationResult.getFormattedErrors()).thenReturn("errors");

        assertThatThrownBy(() -> underTest.addAndValidateTrust(STACK_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Failed to validate trust on FreeIPA: errors");
    }

    private void setKdcType(String kdcType) {
        DescribeEnvironmentResponse response = new DescribeEnvironmentResponse();
        Environment environment = new Environment();
        PvcEnvironmentDetails pvcEnvironmentDetails = new PvcEnvironmentDetails();
        PrivateDatalakeDetails privateDatalakeDetails = new PrivateDatalakeDetails();
        KerberosInfo kerberosInfo = new KerberosInfo();
        kerberosInfo.setKdcType(kdcType);
        privateDatalakeDetails.setKerberosInfo(kerberosInfo);
        pvcEnvironmentDetails.setPrivateDatalakeDetails(privateDatalakeDetails);
        environment.setPvcEnvironmentDetails(pvcEnvironmentDetails);
        response.setEnvironment(environment);
        when(remoteEnvironmentEndpoint.getByCrn(any())).thenReturn(response);
    }

}
