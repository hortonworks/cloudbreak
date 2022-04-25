package com.sequenceiq.environment.experience.liftie;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;

class LiftieConnectorServiceListClustersTest extends LiftieConnectorServiceTestBase {

    private static final String LIFTIE_CLUSTER_ENDPOINT_PATH = "somewhereOverTheRainbow";

    private static final ListClustersResponse RESPONSE = new ListClustersResponse();

    private static final String TENANT_QUERY_PARAM_KEY = "tenant";

    private static final String PAGE_QUERY_PARAM_KEY = "page";

    private static final String ENV_QUERY_PARAM_KEY = "env";

    private static final String TEST_TENANT = "someTenant";

    private static final String TEST_ENV_NAME = "someEnv";

    private static final int MAX_RETRY_COUNT = 5;

    @Override
    @BeforeEach
    void setUp() {
        super.setUp();
        when(getMockLiftiePathProvider().getPathToClustersEndpoint()).thenReturn(LIFTIE_CLUSTER_ENDPOINT_PATH);
        when(getMockClient().target(LIFTIE_CLUSTER_ENDPOINT_PATH)).thenReturn(getMockWebTarget());
        when(getMockWebTarget().queryParam(anyString(), anyString())).thenReturn(getMockWebTarget());
        lenient().when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), ListClustersResponse.class))
                .thenReturn(Optional.of(RESPONSE));
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(getMockResponse());
    }

    @Test
    void testSettingNecessaryQueryParamsOnWebTarget() {
        int pageNumber = 1;

        getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null, pageNumber);

        verify(getMockWebTarget(), times(ONCE)).queryParam(ENV_QUERY_PARAM_KEY, TEST_ENV_NAME);
        verify(getMockWebTarget(), times(ONCE)).queryParam(TENANT_QUERY_PARAM_KEY, TEST_TENANT);
        verify(getMockWebTarget(), times(ONCE)).queryParam(PAGE_QUERY_PARAM_KEY, String.valueOf(pageNumber));
        verify(getMockWebTarget(), times(3)).queryParam(anyString(), anyString());
    }

    @Test
    void testWhenPageIsNullThenItShouldNotBeSetAsQueryParam() {
        getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null, null);

        verify(getMockWebTarget(), never()).queryParam(eq(PAGE_QUERY_PARAM_KEY), any());
    }

    @Test
    void testInvocationBuilderProviderShouldCreateCallForExecutionBasedOnTheWebTarget() {
        getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null, null);

        verify(getMockInvocationBuilderProvider(), times(ONCE)).createInvocationBuilder(getMockWebTarget());
    }

    @Test
    void testWhenCallExecutionReturnsEmptyResponseReadingHappens() {
        getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null, null);

        verify(getMockRetryableWebTarget(), times(ONCE)).get(any());
        verify(getMockRetryableWebTarget(), times(ONCE)).get(getMockInvocationBuilder());
        verify(getMockResponseReader(), times(ONCE)).read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), ListClustersResponse.class);
    }

    @Test
    void testWhenCallExecutionReturnsResponseButItThrowsRuntimeExceptionThenIllegalStateExceptionIsThrown() {
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(getMockResponse());
        doThrow(RuntimeException.class).when(getMockResponseReader()).read(any(), any(), any());

        assertThatThrownBy(() -> getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null, null))
                .isExactlyInstanceOf(ExperienceOperationFailedException.class);
    }

    @Test
    void testWhenResponseReaderReturnsEmptyResultWhichThrowsIllegalStateExceptionThenIllegalStateExceptionIsThrown() {
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(getMockResponse());
        when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), ListClustersResponse.class)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null, null))
                .isExactlyInstanceOf(ExperienceOperationFailedException.class);
    }

    @Test
    void testWhenResponseReaderReturnsValidResultButOnTheSixthAttemptThenItThrowsIllegalStateException() {
        when(getMockRetryableWebTarget().get(getMockInvocationBuilder())).thenReturn(getMockResponse());
        when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), ListClustersResponse.class))
                .thenReturn(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.of(RESPONSE));

        assertThatThrownBy(() -> getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null, null))
                .isExactlyInstanceOf(ExperienceOperationFailedException.class);
    }

    @Test
    void testWhenResponseReaderReturnsNonEmptyResultThenThatShouldReturn() {
        when(getMockWebTarget().getUri()).thenReturn(URI.create(LIFTIE_CLUSTER_ENDPOINT_PATH));

        ListClustersResponse result = getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null, null);

        assertEquals(RESPONSE, result);
    }

    @Test
    void testWhenResponseReaderReturnsNonEmptyResultButNotOnTheFirstTryThenThatShouldReturn() {
        for (int i = 2; i < MAX_RETRY_COUNT; i++) {
            Optional<ListClustersResponse>[] inputs = new Optional[i];
            inputs[inputs.length - 1] = Optional.of(RESPONSE);
            when(getMockWebTarget().getUri()).thenReturn(URI.create(LIFTIE_CLUSTER_ENDPOINT_PATH));
            when(getMockResponseReader().read(LIFTIE_CLUSTER_ENDPOINT_PATH, getMockResponse(), ListClustersResponse.class))
                    .thenReturn(Optional.empty(), inputs);

            ListClustersResponse result = getUnderTest().listClusters(TEST_ENV_NAME, TEST_TENANT, null, null);

            assertEquals(RESPONSE, result, "Not the expected result has arrived when the first " + (i - 1) + " should be is empty.");
        }
    }

}
