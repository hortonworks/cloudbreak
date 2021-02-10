package com.sequenceiq.environment.experience.liftie;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.net.URI;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.experience.InvocationBuilderProvider;
import com.sequenceiq.environment.experience.RetryableWebTarget;

@ExtendWith(MockitoExtension.class)
class LiftieConnectorServiceTestBase {

    protected static final int ONCE = 1;

    protected static final String LIFTIE_CLUSTER_ENDPOINT_PATH = "somewhereOverTheRainbow";

    @InjectMocks
    private LiftieConnectorService underTest;

    @Mock
    private InvocationBuilderProvider mockInvocationBuilderProvider;

    @Mock
    private RetryableWebTarget mockRetryableWebTarget;

    @Mock
    private LiftiePathProvider mockLiftiePathProvider;

    @Mock
    private Invocation.Builder mockInvocationBuilder;

    @Mock
    private LiftieResponseReader mockResponseReader;

    @Mock
    private WebTarget mockWebTarget;

    @Mock
    private Response mockResponse;

    @Mock
    private Client mockClient;

    @BeforeEach
    void setUp() {
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
        lenient().when(mockWebTarget.queryParam(anyString(), anyString())).thenReturn(getMockWebTarget());
        lenient().when(mockWebTarget.getUri()).thenReturn(URI.create(LIFTIE_CLUSTER_ENDPOINT_PATH));

    }

    protected LiftieConnectorService getUnderTest() {
        return underTest;
    }

    public InvocationBuilderProvider getMockInvocationBuilderProvider() {
        return mockInvocationBuilderProvider;
    }

    public RetryableWebTarget getMockRetryableWebTarget() {
        return mockRetryableWebTarget;
    }

    public LiftiePathProvider getMockLiftiePathProvider() {
        return mockLiftiePathProvider;
    }

    public Invocation.Builder getMockInvocationBuilder() {
        return mockInvocationBuilder;
    }

    public LiftieResponseReader getMockResponseReader() {
        return mockResponseReader;
    }

    public WebTarget getMockWebTarget() {
        return mockWebTarget;
    }

    public Response getMockResponse() {
        return mockResponse;
    }

    public Client getMockClient() {
        return mockClient;
    }

}
