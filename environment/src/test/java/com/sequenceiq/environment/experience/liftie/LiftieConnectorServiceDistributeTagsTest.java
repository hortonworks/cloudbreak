package com.sequenceiq.environment.experience.liftie;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.exception.ExperienceOperationFailedException;

class LiftieConnectorServiceDistributeTagsTest extends LiftieConnectorServiceTestBase {

    private static final String LIFTIE_TAGS_ENDPOINT_PATH = "someTagsPath";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:account:environment:envId";

    private static final Map<String, String> TAGS = Map.of("key1", "value1", "key2", "value2");

    private void setupMocks() {
        when(getMockLiftiePathProvider().getPathToEnvironmentTagsEndpoint(ENV_CRN)).thenReturn(LIFTIE_TAGS_ENDPOINT_PATH);
        when(getMockClient().target(LIFTIE_TAGS_ENDPOINT_PATH)).thenReturn(getMockWebTarget());
        when(getMockInvocationBuilderProvider().createInvocationBuilderForInternalActor(getMockWebTarget())).thenReturn(getMockInvocationBuilder());
        when(getMockRetryableWebTarget().put(getMockInvocationBuilder(), TAGS)).thenReturn(getMockResponse());
        when(getMockResponse().getStatusInfo()).thenReturn(Response.Status.NO_CONTENT);
    }

    @Test
    @DisplayName("When environment tags are distributed to Liftie, then the tags endpoint is obtained from the path provider")
    void testWhenEnvironmentTagsAreDistributedThenTagsEndpointPathIsObtainedFromProvider() {
        setupMocks();

        getUnderTest().distributeEnvironmentTags(null, ENV_CRN, TAGS);

        verify(getMockLiftiePathProvider(), times(ONCE)).getPathToEnvironmentTagsEndpoint(ENV_CRN);
    }

    @Test
    @DisplayName("When environment tags are distributed to Liftie, then an internal actor invocation builder is used")
    void testWhenEnvironmentTagsAreDistributedThenInternalActorInvocationBuilderIsUsed() {
        setupMocks();

        getUnderTest().distributeEnvironmentTags(null, ENV_CRN, TAGS);

        verify(getMockInvocationBuilderProvider(), times(ONCE)).createInvocationBuilderForInternalActor(getMockWebTarget());
    }

    @Test
    @DisplayName("When environment tags are distributed to Liftie, then the retryable PUT call receives the tags as its payload")
    void testWhenEnvironmentTagsAreDistributedThenPutIsExecutedWithTagsAsPayload() {
        setupMocks();

        getUnderTest().distributeEnvironmentTags(null, ENV_CRN, TAGS);

        verify(getMockRetryableWebTarget(), times(ONCE)).put(getMockInvocationBuilder(), TAGS);
    }

    @Test
    @DisplayName("When Liftie responds with an unsuccessful status, then an operation failure exception is thrown")
    void testWhenResponseIsUnsuccessfulThenExperienceOperationFailedExceptionIsThrown() {
        when(getMockLiftiePathProvider().getPathToEnvironmentTagsEndpoint(ENV_CRN)).thenReturn(LIFTIE_TAGS_ENDPOINT_PATH);
        when(getMockClient().target(LIFTIE_TAGS_ENDPOINT_PATH)).thenReturn(getMockWebTarget());
        when(getMockInvocationBuilderProvider().createInvocationBuilderForInternalActor(getMockWebTarget())).thenReturn(getMockInvocationBuilder());
        when(getMockRetryableWebTarget().put(getMockInvocationBuilder(), TAGS)).thenReturn(getMockResponse());
        when(getMockResponse().getStatusInfo()).thenReturn(Response.Status.BAD_REQUEST);

        assertThrows(ExperienceOperationFailedException.class,
                () -> getUnderTest().distributeEnvironmentTags(null, ENV_CRN, TAGS));
    }

    @Test
    @DisplayName("When the Liftie tag distribution call fails, then its error is wrapped in an operation failure exception")
    void testWhenEnvironmentTagDistributionCallThrowsThenExperienceOperationFailedExceptionIsThrown() {
        when(getMockLiftiePathProvider().getPathToEnvironmentTagsEndpoint(ENV_CRN)).thenReturn(LIFTIE_TAGS_ENDPOINT_PATH);
        when(getMockClient().target(LIFTIE_TAGS_ENDPOINT_PATH)).thenReturn(getMockWebTarget());
        when(getMockInvocationBuilderProvider().createInvocationBuilderForInternalActor(getMockWebTarget())).thenReturn(getMockInvocationBuilder());
        when(getMockRetryableWebTarget().put(getMockInvocationBuilder(), TAGS)).thenThrow(new RuntimeException("connection refused"));

        assertThrows(ExperienceOperationFailedException.class,
                () -> getUnderTest().distributeEnvironmentTags(null, ENV_CRN, TAGS));
    }

    @Test
    @DisplayName("When Liftie tag distribution succeeds, then the operation completes without an exception")
    void testWhenEnvironmentTagDistributionSucceedsThenCallCompletesWithoutException() {
        setupMocks();

        assertDoesNotThrow(() -> getUnderTest().distributeEnvironmentTags(null, ENV_CRN, TAGS));
    }

    @Test
    @DisplayName("When an experience base path is provided, then the Liftie path provider endpoint is still used")
    void testWhenExperienceBasePathIsProvidedThenLiftiePathProviderEndpointIsUsed() {
        setupMocks();

        getUnderTest().distributeEnvironmentTags("http://some-other-base-path:8080", ENV_CRN, TAGS);

        verify(getMockClient(), times(ONCE)).target(LIFTIE_TAGS_ENDPOINT_PATH);
    }

    @Test
    @DisplayName("When environment tags propagation is not configured, then tag distribution is skipped without calling the endpoint")
    void testWhenEnvironmentTagsPropagationIsDisabledThenDistributionIsSkipped() {
        when(getMockLiftiePathProvider().isEnvironmentTagsPropagationDisabled()).thenReturn(true);

        assertDoesNotThrow(() -> getUnderTest().distributeEnvironmentTags(null, ENV_CRN, TAGS));

        verify(getMockLiftiePathProvider(), never()).getPathToEnvironmentTagsEndpoint(any());
        verifyNoInteractions(getMockClient(), getMockRetryableWebTarget());
    }
}
