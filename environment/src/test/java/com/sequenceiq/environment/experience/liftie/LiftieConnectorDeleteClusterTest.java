package com.sequenceiq.environment.experience.liftie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.experience.liftie.responses.DeleteClusterResponse;

class LiftieConnectorDeleteClusterTest extends LiftieConnectorTestBase {

    private static final String LIFTIE_RESPONSE_RESOLVE_EXCEPTION_MSG = "Unable to resolve Liftie response!";

    private static final String LIFTIE_CLUSTER_ENDPOINT_PATH = "somewhereOverTheRainbow";

    private static final String TEST_CLUSTER_ID = "someClusterId";

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        when(getMockClient().target(LIFTIE_CLUSTER_ENDPOINT_PATH)).thenReturn(getMockWebTarget());
        when(getMockWebTarget().queryParam(anyString(), anyString())).thenReturn(getMockWebTarget());
        when(getMockLiftiePathProvider().getPathToClusterEndpoint(TEST_CLUSTER_ID)).thenReturn(LIFTIE_CLUSTER_ENDPOINT_PATH);
    }

    @Test
    void testInvocationBuilderProviderShouldCreateCallForExecutionBasedOnTheWebTarget() {
        when(getMockWebTarget().getUri()).thenReturn(URI.create(LIFTIE_CLUSTER_ENDPOINT_PATH));
        when(getMockRetryableWebTarget().delete(getMockInvocationBuilder())).thenReturn(getMockResponse());
        when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), DeleteClusterResponse.class)).thenReturn(Optional.of(new DeleteClusterResponse()));

        getUnderTest().deleteCluster(TEST_CLUSTER_ID);

        verify(getMockInvocationBuilderProvider(), times(ONCE)).createInvocationBuilder(getMockWebTarget());
    }

    @Test
    void testWhenCallExecutionReturnsNullThenNoResponseReadingHappens() {
        when(getMockRetryableWebTarget().delete(getMockInvocationBuilder())).thenReturn(null);

        Assertions.assertThrows(IllegalStateException.class, () -> getUnderTest().deleteCluster(TEST_CLUSTER_ID));

        verify(getMockRetryableWebTarget(), times(ONCE)).delete(any());
        verify(getMockRetryableWebTarget(), times(ONCE)).delete(getMockInvocationBuilder());
        verify(getMockResponseReader(), never()).read(any(), any(), any());
    }

    @Test
    void testWhenCallExecutionReturnsNullThenThenIllegalStateExceptionShouldInvoke() {
        when(getMockRetryableWebTarget().delete(getMockInvocationBuilder())).thenReturn(null);

        IllegalStateException expectedException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> getUnderTest().deleteCluster(TEST_CLUSTER_ID));

        assertEquals(LIFTIE_RESPONSE_RESOLVE_EXCEPTION_MSG, expectedException.getMessage());
    }

    @Test
    void testWhenCallExecutionReturnsResponseButItThrowsRuntimeExceptionThenIllegalStateExceptionShouldInvoke() {
        when(getMockRetryableWebTarget().delete(getMockInvocationBuilder())).thenReturn(getMockResponse());
        doThrow(RuntimeException.class).when(getMockResponseReader()).read(any(), any(), any());

        IllegalStateException expectedException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> getUnderTest().deleteCluster(TEST_CLUSTER_ID));

        assertEquals(LIFTIE_RESPONSE_RESOLVE_EXCEPTION_MSG, expectedException.getMessage());
    }

    @Test
    void testWhenResponseReaderReturnsEmptyResultWhichThrowsIllegalStateExceptionThenIllegalStateExceptionShouldInvoke() {
        when(getMockRetryableWebTarget().delete(getMockInvocationBuilder())).thenReturn(getMockResponse());
        when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), DeleteClusterResponse.class)).thenReturn(Optional.empty());

        IllegalStateException expectedException = Assertions.assertThrows(
                IllegalStateException.class,
                () -> getUnderTest().deleteCluster(TEST_CLUSTER_ID));

        assertEquals(LIFTIE_RESPONSE_RESOLVE_EXCEPTION_MSG, expectedException.getMessage());
    }

    @Test
    void testWhenResponseReaderReturnsNonEmptyResultThenThatShouldReturn() {
        DeleteClusterResponse expected = new DeleteClusterResponse();

        when(getMockWebTarget().getUri()).thenReturn(URI.create(LIFTIE_CLUSTER_ENDPOINT_PATH));
        when(getMockRetryableWebTarget().delete(getMockInvocationBuilder())).thenReturn(getMockResponse());
        when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), DeleteClusterResponse.class)).thenReturn(Optional.of(expected));

        DeleteClusterResponse result = getUnderTest().deleteCluster(TEST_CLUSTER_ID);

        assertEquals(expected, result);
    }

}