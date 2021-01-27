package com.sequenceiq.environment.experience.liftie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

class LiftieConnectorListClustersTest extends LiftieConnectorTestBase {

    private static final String LIFTIE_CLUSTER_ENDPOINT_PATH = "somewhereOverTheRainbow";

    private static final String TENANT_QUERY_PARAM_KEY = "tenant";

    private static final String PAGE_QUERY_PARAM_KEY = "page";

    private static final String ENV_QUERY_PARAM_KEY = "env";

    private static final String TEST_TENANT = "someTenant";

    private static final String TEST_ENV_NAME = "someEnv";

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        when(getMockLiftiePathProvider().getPathToClustersEndpoint()).thenReturn(LIFTIE_CLUSTER_ENDPOINT_PATH);
        when(getMockClient().target(LIFTIE_CLUSTER_ENDPOINT_PATH)).thenReturn(getMockWebTarget());
        when(getMockWebTarget().queryParam(anyString(), anyString())).thenReturn(getMockWebTarget());
    }

    @Test
    void testSettingNecessaryQueryParamsOnWebTarget() {
        int pageNumber = 1;

        getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, pageNumber);

        verify(getMockWebTarget(), times(ONCE)).queryParam(ENV_QUERY_PARAM_KEY, TEST_ENV_NAME);
        verify(getMockWebTarget(), times(ONCE)).queryParam(TENANT_QUERY_PARAM_KEY, TEST_TENANT);
        verify(getMockWebTarget(), times(ONCE)).queryParam(PAGE_QUERY_PARAM_KEY, String.valueOf(pageNumber));
        verify(getMockWebTarget(), times(3)).queryParam(anyString(), anyString());
    }

    @Test
    void testWhenPageIsNullThenItShouldNotBeSetAsQueryParam() {
        getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null);

        verify(getMockWebTarget(), never()).queryParam(eq(PAGE_QUERY_PARAM_KEY), any());
    }

    @Test
    void testInvocationBuilderProviderShouldCreateCallForExecutionBasedOnTheWebTarget() {
        getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null);

        verify(getMockInvocationBuilderProvider(), times(ONCE)).createInvocationBuilder(getMockWebTarget());
    }

    @Test
    void testWhenCallExecutionReturnsNullThenNoResponseReadingHappens() {
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(null);

        getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null);

        verify(getMockRetryableWebTarget(), times(ONCE)).get(any());
        verify(getMockRetryableWebTarget(), times(ONCE)).get(getMockInvocationBuilder());
        verify(getMockResponseReader(), never()).read(any(), any(), any());
    }

    @Test
    void testWhenCallExecutionReturnsNullThenThenEmptyResponseShouldReturn() {
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(null);

        ListClustersResponse result = getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null);

        assertNotNull(result);

        assertNull(result.getClusters());

        assertNotNull(result.getPage());
        assertEquals(0, result.getPage().getTotalPages());
    }

    @Test
    void testWhenCallExecutionReturnsResponseButItThrowsRuntimeExceptionThenEmptyResponseShouldReturn() {
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(getMockResponse());
        doThrow(RuntimeException.class).when(getMockResponseReader()).read(any(), any(), any());

        ListClustersResponse result = getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null);

        assertNotNull(result);

        assertNull(result.getClusters());

        assertNotNull(result.getPage());
        assertEquals(0, result.getPage().getTotalPages());
    }

    @Test
    void testWhenResponseReaderReturnsEmptyResultWhichThrowsIllegalStateExceptionThenEmptyResponseShouldReturn() {
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(getMockResponse());
        when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), ListClustersResponse.class)).thenReturn(Optional.empty());

        ListClustersResponse result = getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null);

        assertNotNull(result);

        assertNull(result.getClusters());

        assertNotNull(result.getPage());
        assertEquals(0, result.getPage().getTotalPages());
    }

    @Test
    void testWhenResponseReaderReturnsNonEmptyResultThenThatShouldReturn() {
        ListClustersResponse expected = new ListClustersResponse();

        when(getMockWebTarget().getUri()).thenReturn(URI.create(LIFTIE_CLUSTER_ENDPOINT_PATH));
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(getMockResponse());
        when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), ListClustersResponse.class)).thenReturn(Optional.of(expected));

        ListClustersResponse result = getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null);

        assertEquals(expected, result);
    }

}