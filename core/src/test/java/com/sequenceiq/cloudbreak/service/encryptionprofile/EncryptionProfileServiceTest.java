package com.sequenceiq.cloudbreak.service.encryptionprofile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.environment.api.v1.encryptionprofile.endpoint.EncryptionProfileEndpoint;
import com.sequenceiq.environment.api.v1.encryptionprofile.model.EncryptionProfileResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@ExtendWith(MockitoExtension.class)
class EncryptionProfileServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private EncryptionProfileEndpoint encryptionProfileEndpoint;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private EncryptionProfileService underTest;

    @Test
    void testGetEncryptionProfileCrnWhenClusterEncryptionProfileIsNotNull() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setEncryptionProfileCrn("envEncryptionProfileCrn");
        ClusterView cluster = mock(ClusterView.class);

        when(cluster.getEncryptionProfileCrn()).thenReturn("clusterEpCrn");

        String response = underTest.getEncryptionProfileCrn(environment, cluster);

        assertEquals("clusterEpCrn", response);
    }

    @Test
    void testGetEncryptionProfileCrnWhenClusterEncryptionProfileIsNullEnvironmentEncryptionProfileShouldBeUsed() {
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setEncryptionProfileCrn("environmentEp");
        ClusterView cluster = mock(ClusterView.class);

        when(cluster.getEncryptionProfileCrn()).thenReturn(null);

        String response = underTest.getEncryptionProfileCrn(environment, cluster);

        assertEquals("environmentEp", response);
    }

    @Test
    void testGetEncryptionProfileByCrnWhenEncryptionProfileIsNullDefaultEncryptionProfileShouldBeUsed() {
        ThreadBasedUserCrnProvider.doAs(TestConstants.CRN, () -> underTest.getEncryptionProfileByCrnOrDefault(null));

        verify(encryptionProfileEndpoint, never()).getByCrn(anyString());
        verify(encryptionProfileEndpoint, only()).getDefaultEncryptionProfile();
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
    void testGetEncryptionProfileByNameOrCrnWhenInputIsNullResponseShouldBeNullAndDoesNotThrowException() {
        when(entitlementService.isConfigureEncryptionProfileEnabled(anyString())).thenReturn(true);

        EncryptionProfileResponse response  = assertDoesNotThrow(() ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () ->
                        underTest.getEncryptionProfileByNameOrCrn(null, null)));

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
}
