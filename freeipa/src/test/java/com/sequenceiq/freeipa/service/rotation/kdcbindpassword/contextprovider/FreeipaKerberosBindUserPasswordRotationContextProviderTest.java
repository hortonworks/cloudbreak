package com.sequenceiq.freeipa.service.rotation.kdcbindpassword.contextprovider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.service.secret.service.UncachedSecretServiceForRotation;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.kerberos.KerberosConfig;
import com.sequenceiq.freeipa.kerberos.v1.KerberosConfigV1Service;

@ExtendWith(MockitoExtension.class)
public class FreeipaKerberosBindUserPasswordRotationContextProviderTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:tenant:user:5678";

    @Mock
    private KerberosConfigV1Service kerberosConfigV1Service;

    @Mock
    private UncachedSecretServiceForRotation uncachedSecretServiceForRotation;

    @InjectMocks
    private FreeipaKerberosBindUserPasswordRotationContextProvider underTest;

    @Test
    void testGetContexts() throws FreeIpaClientException {
        KerberosConfig kerberosConfig = mock(KerberosConfig.class);
        when(kerberosConfig.getPasswordSecret()).thenReturn("password");
        when(kerberosConfig.getClusterName()).thenReturn("clustername");
        when(kerberosConfigV1Service.getForCluster(any(), any(), any())).thenReturn(kerberosConfig);
        when(uncachedSecretServiceForRotation.get(any())).thenReturn("username");
        Map<SecretRotationStep, ? extends RotationContext> contexts = ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.getContextsWithProperties("crn", Map.of("CLUSTER_NAME", "clustername")));
        assertEquals(2, contexts.size());
    }
}
