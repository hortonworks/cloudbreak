package com.sequenceiq.environment.credential.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialExperiencePolicyRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialExperiencePolicyResult;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialPrerequisitesRequest;
import com.sequenceiq.cloudbreak.cloud.event.credential.CredentialPrerequisitesResult;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.response.AwsCredentialPrerequisites;
import com.sequenceiq.cloudbreak.cloud.response.CredentialPrerequisitesResponse;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.common.model.CredentialType;
import com.sequenceiq.environment.user.UserPreferencesService;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(MockitoExtension.class)
class CredentialPrerequisiteServiceTest {

    private static final String AWS_CLOUD_PLATFORM = CloudPlatform.AWS.name();

    private static final boolean INTERNAL_CALL = true;

    private static final boolean NOT_INTERNAL_CALL = false;

    private static final boolean NOT_GOV_CLOUD = false;

    private static final String TEST_DEPLOYMENT_ADDRESS = "testDeploymentAddress";

    @Mock
    private CredentialPrerequisitesRequest mockCredentialPrerequisitesRequest;

    @Mock
    private CredentialPrerequisitesResult mockCredentialPrerequisitesResult;

    @Mock
    private CredentialPrerequisitesResponse mockCredentialPrerequisitesResponse;

    @Mock
    private AwsCredentialPrerequisites mockAwsCredentialPrerequisites;

    @Mock
    private CredentialExperiencePolicyRequest mockExperiencePrerequisitesRequest;

    @Mock
    private CredentialExperiencePolicyResult mockExperiencePrerequisitesResult;

    @Mock
    private EventBus mockEventBus;

    @Mock
    private UserPreferencesService mockUserPreferencesService;

    @Mock
    private ErrorHandlerAwareReactorEventFactory mockEventFactory;

    @Mock
    private EntitlementService mockEntitlementService;

    @Mock
    private CloudPlatformRequestProvider mockCloudPlatformRequestProvider;

    private CredentialPrerequisiteService underTest;

    @BeforeEach
    void setUp() throws InterruptedException {
        underTest = new CredentialPrerequisiteService(mockEventBus, mockUserPreferencesService, mockEventFactory, mockEntitlementService,
                mockCloudPlatformRequestProvider);
        mockCredentialPrerequisitesRequestToOk();
        mockExperiencePrerequisitesRequestToOk();
    }

    @Test
    @DisplayName("Test for that in case of an internal request for credential prerequisites no entitlement check happens but both the Cloudbreak and " +
            "the experience calls happens.")
    void testAwsInternal() throws InterruptedException {
        underTest.getPrerequisites(AWS_CLOUD_PLATFORM, NOT_GOV_CLOUD, TEST_DEPLOYMENT_ADDRESS, CredentialType.ENVIRONMENT, INTERNAL_CALL);

        verifyNoInteractions(mockEntitlementService);
        validateMocksWhenAwsRestrictedPolicyIsEntitled();
    }

    @Test
    @DisplayName("Test for that in case of a directly non-internal request for credential prerequisites entitlement check happens and if user is entitled " +
            "both the Cloudbreak and the experience calls happens.")
    void testAwsDirectlyNotInternalEntitled() throws InterruptedException {
        when(mockEntitlementService.awsRestrictedPolicy(any())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs("crn:altus:iam:us-west-1:123:user:456", () -> {
            underTest.getPrerequisites(AWS_CLOUD_PLATFORM, NOT_GOV_CLOUD, TEST_DEPLOYMENT_ADDRESS, CredentialType.ENVIRONMENT, NOT_INTERNAL_CALL);
        });

        validateMocksWhenAwsRestrictedPolicyIsEntitled();
    }

    @Test
    @DisplayName("Test for that in case of a directly non-internal request for credential prerequisites entitlement check happens and if user is not " +
            "entitled, only the Cloudbreak call should happen and the experience not.")
    void testAwsDirectlyNotInternalNotEntitled() throws InterruptedException {
        when(mockEntitlementService.awsRestrictedPolicy(any())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs("crn:altus:iam:us-west-1:123:user:456", () -> {
            underTest.getPrerequisites(AWS_CLOUD_PLATFORM, NOT_GOV_CLOUD, TEST_DEPLOYMENT_ADDRESS, CredentialType.ENVIRONMENT, NOT_INTERNAL_CALL);
        });

        validateMocksWhenAwsRestrictedPolicyIsNotEntitled();
    }

    @Test
    @DisplayName("Test for that in case of a non-internal request for credential prerequisites entitlement check happens and if user is entitled " +
            "both the Cloudbreak and the experience calls happens.")
    void testAwsNotInternalEntitled() throws InterruptedException {
        when(mockEntitlementService.awsRestrictedPolicy(any())).thenReturn(true);

        ThreadBasedUserCrnProvider.doAs("crn:altus:iam:us-west-1:123:user:456", () -> {
            underTest.getPrerequisites(AWS_CLOUD_PLATFORM, NOT_GOV_CLOUD, TEST_DEPLOYMENT_ADDRESS, CredentialType.ENVIRONMENT);
        });

        validateMocksWhenAwsRestrictedPolicyIsEntitled();
    }

    @Test
    @DisplayName("Test for that in case of a non-internal request for credential prerequisites entitlement check happens and if user is not " +
            "entitled, only the Cloudbreak call should happen and the experience not.")
    void testAwsNotInternalNotEntitled() throws InterruptedException {
        when(mockEntitlementService.awsRestrictedPolicy(any())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs("crn:altus:iam:us-west-1:123:user:456", () -> {
            underTest.getPrerequisites(AWS_CLOUD_PLATFORM, NOT_GOV_CLOUD, TEST_DEPLOYMENT_ADDRESS, CredentialType.ENVIRONMENT);
        });

        validateMocksWhenAwsRestrictedPolicyIsNotEntitled();
    }

    private void mockCredentialPrerequisitesRequestToOk() throws InterruptedException {
        lenient().when(mockCloudPlatformRequestProvider.getCredentialPrerequisitesRequest(any(), any(), any(), any(), any()))
                .thenReturn(mockCredentialPrerequisitesRequest);
        lenient().when(mockCredentialPrerequisitesRequest.await()).thenReturn(mockCredentialPrerequisitesResult);
        lenient().when(mockCredentialPrerequisitesResult.getStatus()).thenReturn(EventStatus.OK);
        lenient().when(mockCredentialPrerequisitesResult.getCredentialPrerequisitesResponse()).thenReturn(mockCredentialPrerequisitesResponse);
        lenient().when(mockCredentialPrerequisitesResponse.getAws()).thenReturn(mockAwsCredentialPrerequisites);
        lenient().when(mockAwsCredentialPrerequisites.getPolicies()).thenReturn(new HashMap<>());
    }

    private void mockExperiencePrerequisitesRequestToOk() throws InterruptedException {
        lenient().when(mockCloudPlatformRequestProvider.getCredentialExperiencePolicyRequest(any())).thenReturn(mockExperiencePrerequisitesRequest);
        lenient().when(mockExperiencePrerequisitesRequest.await()).thenReturn(mockExperiencePrerequisitesResult);
        lenient().when(mockExperiencePrerequisitesResult.getStatus()).thenReturn(EventStatus.OK);
        lenient().when(mockExperiencePrerequisitesResult.getPolicies()).thenReturn(new HashMap<>());
    }

    private void validateMocksWhenAwsRestrictedPolicyIsNotEntitled() throws InterruptedException {
        verify(mockCloudPlatformRequestProvider, times(1)).getCredentialPrerequisitesRequest(any(), any(), any(), any(), any());
        verify(mockCredentialPrerequisitesRequest, times(1)).selector();
        verify(mockCredentialPrerequisitesRequest, times(1)).await();
        verify(mockCloudPlatformRequestProvider, never()).getCredentialExperiencePolicyRequest(any());
        verifyNoMoreInteractions(mockCloudPlatformRequestProvider, mockExperiencePrerequisitesRequest);
    }

    private void validateMocksWhenAwsRestrictedPolicyIsEntitled() throws InterruptedException {
        verify(mockCloudPlatformRequestProvider, times(1)).getCredentialPrerequisitesRequest(any(), any(), any(), any(), any());
        verify(mockCredentialPrerequisitesRequest, times(1)).selector();
        verify(mockCredentialPrerequisitesRequest, times(1)).await();
        verify(mockCloudPlatformRequestProvider, times(1)).getCredentialExperiencePolicyRequest(any());
        verify(mockExperiencePrerequisitesRequest, times(1)).selector();
        verify(mockExperiencePrerequisitesRequest, times(1)).await();
        verifyNoMoreInteractions(mockCloudPlatformRequestProvider, mockExperiencePrerequisitesRequest);
    }

}