package com.sequenceiq.environment.experience.liftie;

import static com.sequenceiq.environment.experience.api.CommonExperienceApi.TAG_KEY_QUERY_PARAM;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.exception.ExperienceOperationFailedException;

class LiftieConnectorServiceDeleteTagsTest extends LiftieConnectorServiceTestBase {

    private static final String LIFTIE_TAGS_ENDPOINT_PATH = "someTagsPath";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:account:environment:envId";

    private static final Set<String> TAG_KEYS = Set.of("key1", "key2");

    @SuppressWarnings("unchecked")
    private void setupMocks() {
        when(getMockLiftiePathProvider().getPathToEnvironmentTagsEndpoint()).thenReturn(LIFTIE_TAGS_ENDPOINT_PATH);
        when(getMockClient().target(LIFTIE_TAGS_ENDPOINT_PATH)).thenReturn(getMockWebTarget());
        when(getMockInvocationBuilderProvider().createInvocationBuilderForInternalActor(getMockWebTarget())).thenReturn(getMockInvocationBuilder());
        when(getMockRetryableWebTarget().delete(getMockInvocationBuilder())).thenReturn(getMockResponse());
        when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), Void.class))
                .thenReturn((Optional) Optional.of("placeholder"));
    }

    @Test
    @DisplayName("When environment tags are deleted on Liftie, then the tags endpoint is obtained from the path provider")
    void testWhenEnvironmentTagsAreDeletedThenTagsEndpointPathIsObtainedFromProvider() {
        setupMocks();

        getUnderTest().deleteEnvironmentTags(null, ENV_CRN, TAG_KEYS);

        verify(getMockLiftiePathProvider(), times(ONCE)).getPathToEnvironmentTagsEndpoint();
    }

    @Test
    @DisplayName("When environment tags are deleted on Liftie, then each tag key is added as a repeated query parameter")
    void testWhenEnvironmentTagsAreDeletedThenTagKeysAreAddedAsQueryParameters() {
        setupMocks();

        getUnderTest().deleteEnvironmentTags(null, ENV_CRN, TAG_KEYS);

        verify(getMockWebTarget(), times(ONCE)).queryParam(TAG_KEY_QUERY_PARAM, "key1");
        verify(getMockWebTarget(), times(ONCE)).queryParam(TAG_KEY_QUERY_PARAM, "key2");
    }

    @Test
    @DisplayName("When environment tags are deleted on Liftie, then an internal actor invocation builder is used")
    void testWhenEnvironmentTagsAreDeletedThenInternalActorInvocationBuilderIsUsed() {
        setupMocks();

        getUnderTest().deleteEnvironmentTags(null, ENV_CRN, TAG_KEYS);

        verify(getMockInvocationBuilderProvider(), times(ONCE)).createInvocationBuilderForInternalActor(getMockWebTarget());
    }

    @Test
    @DisplayName("When environment tags are deleted on Liftie, then a body-less retryable DELETE call is executed")
    void testWhenEnvironmentTagsAreDeletedThenBodilessDeleteIsExecuted() {
        setupMocks();

        getUnderTest().deleteEnvironmentTags(null, ENV_CRN, TAG_KEYS);

        verify(getMockRetryableWebTarget(), times(ONCE)).delete(getMockInvocationBuilder());
    }

    @Test
    @DisplayName("When the Liftie response cannot be resolved, then an operation failure exception is thrown")
    void testWhenResponseReaderReturnsEmptyThenExperienceOperationFailedExceptionIsThrown() {
        when(getMockLiftiePathProvider().getPathToEnvironmentTagsEndpoint()).thenReturn(LIFTIE_TAGS_ENDPOINT_PATH);
        when(getMockClient().target(LIFTIE_TAGS_ENDPOINT_PATH)).thenReturn(getMockWebTarget());
        when(getMockInvocationBuilderProvider().createInvocationBuilderForInternalActor(getMockWebTarget())).thenReturn(getMockInvocationBuilder());
        when(getMockRetryableWebTarget().delete(getMockInvocationBuilder())).thenReturn(getMockResponse());
        when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), Void.class)).thenReturn(Optional.empty());

        assertThrows(ExperienceOperationFailedException.class,
                () -> getUnderTest().deleteEnvironmentTags(null, ENV_CRN, TAG_KEYS));
    }

    @Test
    @DisplayName("When the Liftie tag deletion call fails, then its error is wrapped in an operation failure exception")
    void testWhenEnvironmentTagDeletionCallThrowsThenExperienceOperationFailedExceptionIsThrown() {
        when(getMockLiftiePathProvider().getPathToEnvironmentTagsEndpoint()).thenReturn(LIFTIE_TAGS_ENDPOINT_PATH);
        when(getMockClient().target(LIFTIE_TAGS_ENDPOINT_PATH)).thenReturn(getMockWebTarget());
        when(getMockInvocationBuilderProvider().createInvocationBuilderForInternalActor(getMockWebTarget())).thenReturn(getMockInvocationBuilder());
        when(getMockRetryableWebTarget().delete(getMockInvocationBuilder())).thenThrow(new RuntimeException("connection refused"));

        assertThrows(ExperienceOperationFailedException.class,
                () -> getUnderTest().deleteEnvironmentTags(null, ENV_CRN, TAG_KEYS));
    }

    @Test
    @DisplayName("When Liftie tag deletion succeeds, then the operation completes without an exception")
    void testWhenEnvironmentTagDeletionSucceedsThenCallCompletesWithoutException() {
        setupMocks();

        assertDoesNotThrow(() -> getUnderTest().deleteEnvironmentTags(null, ENV_CRN, TAG_KEYS));
    }

    @Test
    @DisplayName("When environment tags propagation is not configured, then tag deletion is skipped without calling the endpoint")
    void testWhenEnvironmentTagsPropagationIsDisabledThenDeletionIsSkipped() {
        when(getMockLiftiePathProvider().isEnvironmentTagsPropagationDisabled()).thenReturn(true);

        assertDoesNotThrow(() -> getUnderTest().deleteEnvironmentTags(null, ENV_CRN, TAG_KEYS));

        verify(getMockLiftiePathProvider(), never()).getPathToEnvironmentTagsEndpoint();
        verifyNoInteractions(getMockClient(), getMockRetryableWebTarget());
    }

    @Test
    @DisplayName("When no tag keys are given, then tag deletion is skipped without calling the endpoint")
    void testWhenNoTagKeysAreGivenThenDeletionIsSkipped() {
        assertDoesNotThrow(() -> getUnderTest().deleteEnvironmentTags(null, ENV_CRN, Set.of()));

        verify(getMockLiftiePathProvider(), never()).getPathToEnvironmentTagsEndpoint();
        verifyNoInteractions(getMockClient(), getMockRetryableWebTarget());
    }
}
