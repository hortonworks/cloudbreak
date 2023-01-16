package com.sequenceiq.cloudbreak.auth.altus.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Provider;
import java.security.Security;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.usermanagement.UserManagementProto;
import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.altus.model.AltusCredential;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;

@ExtendWith(MockitoExtension.class)
class AltusIAMServiceTest {

    @Mock
    private GrpcUmsClient umsClient;

    @Mock
    private SharedAltusCredentialProvider sharedAltusCredentialProvider;

    @Mock
    private RoleCrnGenerator roleCrnGenerator;

    @Mock
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @InjectMocks
    private AltusIAMService underTest;

    @Test
    void testGenerateMachineUserWithAccessKeyForLegacyCm() {
        when(umsClient.createMachineUserAndGenerateKeys(anyString(), anyString(), anyString(), anyString(), anyMap(), any(), any()))
                .thenReturn(new AltusCredential("accessKey", "privateKey".toCharArray()));
        when(roleCrnGenerator.getBuiltInDatabusRoleCrn(eq("accountId"))).thenReturn("roleCrn");
        AltusCredential altusCredential = underTest.generateMachineUserWithAccessKeyForLegacyCm("machineUserName", "actorCrn", "accountId",
                Map.of("key", "value"));
        assertEquals("accessKey", altusCredential.getAccessKey());
        assertEquals("privateKey", String.valueOf(altusCredential.getPrivateKey()));
        verify(umsClient, times(1)).createMachineUserAndGenerateKeys(eq("machineUserName"), eq("actorCrn"), eq("accountId"),
                eq("roleCrn"), eq(Map.of("key", "value")), eq(UserManagementProto.AccessKeyType.Value.RSA), eq(regionAwareInternalCrnGeneratorFactory));
    }

    @Test
    void testGenerateMachineUserWithAccessKeyForLegacyCmWhenFipsEnabled() {
        try (MockedStatic<Security> mockedStatic = Mockito.mockStatic(Security.class)) {
            Provider provider = mock(Provider.class);
            mockedStatic.when(() -> Security.getProvider(anyString())).thenReturn(provider);
            when(umsClient.createMachineUserAndGenerateKeys(anyString(), anyString(), anyString(), anyString(), anyMap(), any(), any()))
                    .thenReturn(new AltusCredential("accessKey", "privateKey".toCharArray()));
            when(roleCrnGenerator.getBuiltInDatabusRoleCrn(eq("accountId"))).thenReturn("roleCrn");
            AltusCredential altusCredential = underTest.generateMachineUserWithAccessKeyForLegacyCm("machineUserName", "actorCrn", "accountId",
                    Map.of("key", "value"));
            assertEquals("accessKey", altusCredential.getAccessKey());
            assertEquals("privateKey", String.valueOf(altusCredential.getPrivateKey()));
            verify(umsClient, times(1)).createMachineUserAndGenerateKeys(eq("machineUserName"), eq("actorCrn"), eq("accountId"),
                    eq("roleCrn"), eq(Map.of("key", "value")), eq(UserManagementProto.AccessKeyType.Value.ED25519), eq(regionAwareInternalCrnGeneratorFactory));
        }
    }
}