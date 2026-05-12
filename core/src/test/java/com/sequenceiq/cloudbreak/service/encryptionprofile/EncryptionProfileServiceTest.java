package com.sequenceiq.cloudbreak.service.encryptionprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:user@cloudera.com";

    @Mock
    private EncryptionProfileEndpoint encryptionProfileEndpoint;

    @Mock
    private ClusterService clusterService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private EncryptionProfileService underTest;

    @Test
    void testGetEncryptionProfileWhenClusterEPIsNullEnvironmentFallback() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setEncryptionProfileCrn("environmentEp");
        ClusterView cluster = mock(ClusterView.class);
        StackDto mockStack = mock(StackDto.class);
        when(mockStack.getCluster()).thenReturn(cluster);
        when(cluster.getEncryptionProfileCrn()).thenReturn(null);

        String response = underTest.getEncryptionProfileByCrnOrDefault(environment, mockStack);

        assertEquals("environmentEp", response);
    }

    @Test
    void testGetEncryptionProfileByCrnWhenEncryptionProfileIsNotNull() {
        ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () -> underTest.getEncryptionProfileByCrnOrDefault("clusterEpCrn"));

        verify(encryptionProfileEndpoint, only()).getByCrn(eq("clusterEpCrn"));
        verify(encryptionProfileEndpoint, never()).getDefaultEncryptionProfile();
    }

    @Test
    void testWhenEncryptionProfileIsNullThenDefaultShouldBeUsed() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setEncryptionProfileCrn(null);
        StackDto stackDto = mock(StackDto.class);
        ClusterView cluster = mock(ClusterView.class);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getEncryptionProfileByCrnOrDefault(null));

        verify(encryptionProfileEndpoint, never()).getByCrn(any());
        verify(encryptionProfileEndpoint, times(1)).getDefaultEncryptionProfile();
    }

    @Test
    void testGetEncryptionProfileByNameOrCrnWhenEntitlementIsNotGrantedResponseShouldBeNull() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(false);

        EncryptionProfileResponse response = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getEncryptionProfileByNameOrCrn("epName", null));

        assertThat(response).isNull();
    }

    @Test
    void testGetEncryptionProfileByNameOrCrnWhenProfileNameIsUsed() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.getEncryptionProfileByNameOrCrn("epName", null));

        verify(encryptionProfileEndpoint, times(1)).getByName("epName");
        verify(encryptionProfileEndpoint, never()).getByCrn(anyString());
    }

    @Test
    void testGetEncryptionProfileByNameOrCrnWhenProfileCrnIsUsed() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getEncryptionProfileByNameOrCrn(
                "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-123", null));

        verify(encryptionProfileEndpoint, times(1))
                .getByCrn("crn:cdp:environments:us-west-1:cloudera:encryptionProfile:custom-123");
        verify(encryptionProfileEndpoint, never()).getByName(anyString());
    }

    @Test
    void testSetEncryptionProfile() {
        String encryptionProfileCrn = "encryptionProfileCrn";
        Stack stack = mock(Stack.class);
        Cluster  cluster = mock(Cluster.class);
        EncryptionProfileResponse encryptionProfileResponse = new EncryptionProfileResponse();
        encryptionProfileResponse.setCrn(encryptionProfileCrn);

        when(stack.getCluster()).thenReturn(cluster);
        when(encryptionProfileEndpoint.getByCrn(encryptionProfileCrn)).thenReturn(encryptionProfileResponse);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.setEncryptionProfile(encryptionProfileCrn, stack));

        verify(clusterService, times(1)).save(cluster);
    }

    @Test
    void testSetEncryptionProfileShouldNotFailWhenEncryptionProfileIsNull() {
        Stack stack = mock(Stack.class);

        assertDoesNotThrow(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.setEncryptionProfile(null, stack)));

        verify(clusterService, never()).save(any());
    }

    @Test
    void testGetEncryptionProfileOrThrowExceptionByCrn() {
        String encryptionProfileCrn = "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:a645ac1b-14b6-45a7-88ef-b920ad9b32b4";

        when(encryptionProfileEndpoint.getByCrn(encryptionProfileCrn)).thenReturn(mock(EncryptionProfileResponse.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getEncryptionProfileOrThrowException(encryptionProfileCrn));

        verify(encryptionProfileEndpoint, never()).getByName(any());
        verify(encryptionProfileEndpoint, times(1)).getByCrn(encryptionProfileCrn);
    }

    @Test
    void testGetEncryptionProfileOrThrowExceptionByName() {
        String encryptionProfileName = "epName";

        when(encryptionProfileEndpoint.getByName(encryptionProfileName)).thenReturn(mock(EncryptionProfileResponse.class));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.getEncryptionProfileOrThrowException(encryptionProfileName));

        verify(encryptionProfileEndpoint, never()).getByCrn(any());
        verify(encryptionProfileEndpoint, times(1)).getByName(encryptionProfileName);
    }

    @Test
    void testGetEncryptionProfileOrThrowExceptionWhenEncryptionProfileIsNotFound() {
        String encryptionProfileCrn = "crn:cdp:environments:us-west-1:cloudera:encryptionProfile:a645ac1b-14b6-45a7-88ef-b920ad9b32b4";

        when(encryptionProfileEndpoint.getByCrn(encryptionProfileCrn)).thenReturn(null);

        NotFoundException ex = assertThrows(NotFoundException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                underTest.getEncryptionProfileOrThrowException(encryptionProfileCrn)));

        verify(encryptionProfileEndpoint, never()).getByName(any());
        verify(encryptionProfileEndpoint, times(1)).getByCrn(encryptionProfileCrn);
        assertEquals("Encryption profile not found: crn:cdp:environments:us-west-1:cloudera:encryptionProfile:a645ac1b-14b6-45a7-88ef-b920ad9b32b4",
                ex.getMessage());
    }
}
