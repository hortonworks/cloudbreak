package com.sequenceiq.environment.experience.liftie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.policy.response.ExperiencePolicyResponse;

class LiftieConnectorServiceFetchPolicyTest extends LiftieConnectorServiceTestBase {

    private static final String LIFTIE_RESPONSE_RESOLVE_EXCEPTION_MSG = "Unable to find the Kubernetes dependencies of this environment due to internal error.";

    private static final String TEST_CLOUD_PLATFORM = "AWS";

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        when(getMockClient().target(LIFTIE_CLUSTER_ENDPOINT_PATH)).thenReturn(getMockWebTarget());
        when(getMockLiftiePathProvider().getPathToPolicyEndpoint(TEST_CLOUD_PLATFORM)).thenReturn(LIFTIE_CLUSTER_ENDPOINT_PATH);
    }

    @Test
    void testInvocationBuilderProviderShouldCreateCallForExecutionBasedOnTheWebTarget() {
        when(getMockInvocationBuilderProvider().createInvocationBuilderForInternalActor(getMockWebTarget())).thenReturn(getMockInvocationBuilder());
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(getMockResponse());
        when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), ExperiencePolicyResponse.class))
                .thenReturn(Optional.of(new ExperiencePolicyResponse()));

        getUnderTest().getPolicy(TEST_CLOUD_PLATFORM);

        verify(getMockInvocationBuilderProvider(), times(ONCE)).createInvocationBuilderForInternalActor(getMockWebTarget());
    }

    @Test
    void testWhenCallExecutionReturnsNullThenNoResponseReadingHappens() {
        when(getMockInvocationBuilderProvider().createInvocationBuilderForInternalActor(getMockWebTarget())).thenReturn(getMockInvocationBuilder());
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(null);

        assertThrows(ExperienceOperationFailedException.class, () -> getUnderTest().getPolicy(TEST_CLOUD_PLATFORM));

        verify(getMockRetryableWebTarget(), times(ONCE)).get(any());
        verify(getMockRetryableWebTarget(), times(ONCE)).get(getMockInvocationBuilder());
        verify(getMockResponseReader(), times(ONCE)).read(LIFTIE_CLUSTER_ENDPOINT_PATH, null, ExperiencePolicyResponse.class);
    }

    @Test
    void testWhenCallExecutionReturnsNullThenThenIllegalStateExceptionShouldInvoke() {
        when(getMockInvocationBuilderProvider().createInvocationBuilderForInternalActor(getMockWebTarget())).thenReturn(getMockInvocationBuilder());
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(null);

        ExperienceOperationFailedException expectedException = assertThrows(
                ExperienceOperationFailedException.class,
                () -> getUnderTest().getPolicy(TEST_CLOUD_PLATFORM));

        assertEquals(LIFTIE_RESPONSE_RESOLVE_EXCEPTION_MSG, expectedException.getMessage());
    }

    @Test
    void testWhenCallExecutionReturnsResponseButItThrowsRuntimeExceptionThenIllegalStateExceptionShouldInvoke() {
        when(getMockInvocationBuilderProvider().createInvocationBuilderForInternalActor(getMockWebTarget())).thenReturn(getMockInvocationBuilder());
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(getMockResponse());
        doThrow(RuntimeException.class).when(getMockResponseReader()).read(any(), any(), any());

        ExperienceOperationFailedException expectedException = assertThrows(
                ExperienceOperationFailedException.class,
                () -> getUnderTest().getPolicy(TEST_CLOUD_PLATFORM));

        assertEquals(LIFTIE_RESPONSE_RESOLVE_EXCEPTION_MSG, expectedException.getMessage());
    }

    @Test
    void testWhenResponseReaderReturnsEmptyResultWhichThrowsIllegalStateExceptionThenIllegalStateExceptionShouldInvoke() {
        when(getMockInvocationBuilderProvider().createInvocationBuilderForInternalActor(getMockWebTarget())).thenReturn(getMockInvocationBuilder());
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(getMockResponse());
        when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), ExperiencePolicyResponse.class)).thenReturn(Optional.empty());

        ExperienceOperationFailedException expectedException = assertThrows(
                ExperienceOperationFailedException.class,
                () -> getUnderTest().getPolicy(TEST_CLOUD_PLATFORM));

        assertEquals(LIFTIE_RESPONSE_RESOLVE_EXCEPTION_MSG, expectedException.getMessage());
    }

    @Test
    void testWhenResponseReaderReturnsNonEmptyResultThenThatShouldReturn() {
        ExperiencePolicyResponse expected = new ExperiencePolicyResponse();

        when(getMockInvocationBuilderProvider().createInvocationBuilderForInternalActor(getMockWebTarget())).thenReturn(getMockInvocationBuilder());
        when(getMockWebTarget().getUri()).thenReturn(URI.create(LIFTIE_CLUSTER_ENDPOINT_PATH));
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(getMockResponse());
        when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), ExperiencePolicyResponse.class)).thenReturn(Optional.of(expected));

        ExperiencePolicyResponse result = getUnderTest().getPolicy(TEST_CLOUD_PLATFORM);

        assertEquals(expected, result);
    }

}
