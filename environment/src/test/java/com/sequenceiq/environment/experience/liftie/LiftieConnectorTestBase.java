package com.sequenceiq.environment.experience.liftie;

import static org.mockito.Mockito.when;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.environment.experience.InvocationBuilderProvider;
import com.sequenceiq.environment.experience.RetryableWebTarget;

class LiftieConnectorTestBase {

    protected static final int ONCE = 1;

    private LiftieConnector underTest;

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
        MockitoAnnotations.openMocks(this);
        underTest = new LiftieConnector(mockLiftiePathProvider, mockResponseReader, mockRetryableWebTarget, mockClient, mockInvocationBuilderProvider);
        when(mockInvocationBuilderProvider.createInvocationBuilder(mockWebTarget)).thenReturn(mockInvocationBuilder);
    }

    protected LiftieConnector getUnderTest() {
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