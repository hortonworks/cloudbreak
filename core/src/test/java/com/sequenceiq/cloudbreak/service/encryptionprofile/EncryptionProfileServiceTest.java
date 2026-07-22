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

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentConfigProvider;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.cloudbreak.view.ClusterView;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
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

    @Mock
    private EnvironmentConfigProvider environmentConfigProvider;

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

    @Test
    void testGetDefaultEncryptionProfileIfRequiredWhenClusterHasProfileCrn() {
        Cluster cluster = new Cluster();
        cluster.setEncryptionProfileCrn("clusterProfileCrn");
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setEncryptionProfileCrn("envProfileCrn");

        Optional<String> result = underTest.getDefaultEncryptionProfileIfRequired(environment, cluster, Optional.of("7.3.2"));

        assertThat(result).contains("clusterProfileCrn");
        verify(encryptionProfileEndpoint, never()).getDefaultEncryptionProfile();
    }

    @Test
    void testGetDefaultEncryptionProfileIfRequiredWhenGovCloudAndVersion732ReturnsEnvProfile() {
        Cluster cluster = new Cluster();
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setEncryptionProfileCrn("envProfileCrn");
        CredentialResponse credential = new CredentialResponse();
        credential.setGovCloud(true);
        environment.setCredential(credential);

        Optional<String> result = underTest.getDefaultEncryptionProfileIfRequired(environment, cluster, Optional.of("7.3.2"));

        assertThat(result).contains("envProfileCrn");
        verify(encryptionProfileEndpoint, never()).getDefaultEncryptionProfile();
    }

    @Test
    void testGetDefaultEncryptionProfileIfRequiredWhenNotGovCloudReturnsEnvCrn() {
        Cluster cluster = new Cluster();
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setEncryptionProfileCrn("envProfileCrn");
        CredentialResponse credential = new CredentialResponse();
        credential.setGovCloud(false);
        environment.setCredential(credential);

        Optional<String> result = underTest.getDefaultEncryptionProfileIfRequired(environment, cluster, Optional.of("7.3.2"));

        assertThat(result).isEmpty();
        verify(encryptionProfileEndpoint, never()).getDefaultEncryptionProfile();
    }

    @Test
    void testGetDefaultEncryptionProfileIfRequiredWhenGovCloudButOlderVersionReturnsEnvCrn() {
        Cluster cluster = new Cluster();
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        environment.setEncryptionProfileCrn("envProfileCrn");
        CredentialResponse credential = new CredentialResponse();
        credential.setGovCloud(true);
        environment.setCredential(credential);

        Optional<String> result = underTest.getDefaultEncryptionProfileIfRequired(environment, cluster, Optional.of("7.3.1"));

        assertThat(result).isEmpty();
        verify(encryptionProfileEndpoint, never()).getDefaultEncryptionProfile();
    }

    @Test
    void testGetDefaultEncryptionProfileIfRequiredWhenNoCrnAnywhereReturnsEmpty() {
        Cluster cluster = new Cluster();
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();

        Optional<String> result = underTest.getDefaultEncryptionProfileIfRequired(environment, cluster, Optional.empty());

        assertThat(result).isEmpty();
        verify(encryptionProfileEndpoint, never()).getDefaultEncryptionProfile();
    }

    @Test
    void testGetEncryptionProfile() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setEncryptionProfileCrn("envProfileCrn");
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getEncryptionProfileCrn()).thenReturn(null);
        EncryptionProfileResponse profile = new EncryptionProfileResponse();
        when(encryptionProfileEndpoint.getByCrn("envProfileCrn")).thenReturn(profile);

        EncryptionProfileResponse result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getEncryptionProfile(stackDto, environmentResponse));

        assertThat(result).isSameAs(profile);
        verify(environmentConfigProvider, never()).getEnvironmentByCrn(any());
    }

    @Test
    void testGetEncryptionProfileWhenEnvIsNullAndUsesClusterEncryptionProfile() {
        DetailedEnvironmentResponse environmentResponse = new DetailedEnvironmentResponse();
        environmentResponse.setEncryptionProfileCrn("envProfileCrn");
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getEnvironmentCrn()).thenReturn("env-crn");
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getEncryptionProfileCrn()).thenReturn("clusterProfileCrn");
        when(environmentConfigProvider.getEnvironmentByCrn("env-crn")).thenReturn(environmentResponse);
        EncryptionProfileResponse profile = new EncryptionProfileResponse();
        when(encryptionProfileEndpoint.getByCrn("clusterProfileCrn")).thenReturn(profile);

        EncryptionProfileResponse result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getEncryptionProfile(stackDto, null));

        assertThat(result).isSameAs(profile);
        verify(encryptionProfileEndpoint, never()).getDefaultEncryptionProfile();
    }

    @Test
    void testGetEncryptionProfileFallsBackToDefaultWhenNoEncryptionProfileIsConfigured() {
        DetailedEnvironmentResponse callerEnvironment = new DetailedEnvironmentResponse();
        callerEnvironment.setEncryptionProfileCrn(null);
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getEncryptionProfileCrn()).thenReturn(null);
        EncryptionProfileResponse defaultProfile = new EncryptionProfileResponse();
        when(encryptionProfileEndpoint.getDefaultEncryptionProfile()).thenReturn(defaultProfile);

        EncryptionProfileResponse result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getEncryptionProfile(stackDto, callerEnvironment));

        assertThat(result).isSameAs(defaultProfile);
        verify(encryptionProfileEndpoint, never()).getByCrn(any());
        verify(environmentConfigProvider, never()).getEnvironmentByCrn(any());
    }

    @Test
    void testGetEncryptionProfileWhenExceptionThrown() {
        DetailedEnvironmentResponse callerEnvironment = new DetailedEnvironmentResponse();
        callerEnvironment.setEncryptionProfileCrn("envProfileCrn");
        StackDto stackDto = mock(StackDto.class);
        ClusterView clusterView = mock(ClusterView.class);
        when(stackDto.getCluster()).thenReturn(clusterView);
        when(clusterView.getEncryptionProfileCrn()).thenReturn(null);
        when(encryptionProfileEndpoint.getByCrn("envProfileCrn")).thenThrow(new RuntimeException("boom"));

        CloudbreakServiceException ex = assertThrows(CloudbreakServiceException.class, () -> ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.getEncryptionProfile(stackDto, callerEnvironment)));

        assertThat(ex.getMessage()).contains("envProfileCrn").contains("boom");
    }

    @Test
    void testGetDefaultEncryptionProfileIfRequiredWhenGovCloudButNoEnvCrnReturnsEmpty() {
        Cluster cluster = new Cluster();
        DetailedEnvironmentResponse environment = new DetailedEnvironmentResponse();
        CredentialResponse credential = new CredentialResponse();
        credential.setGovCloud(true);
        environment.setCredential(credential);

        Optional<String> result = underTest.getDefaultEncryptionProfileIfRequired(environment, cluster, Optional.of("7.3.2"));

        assertThat(result).isEmpty();
        verify(encryptionProfileEndpoint, never()).getDefaultEncryptionProfile();
    }
}
