package com.sequenceiq.environment.credential.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionRequest;
import com.sequenceiq.cloudbreak.cloud.event.platform.ResourceDefinitionResult;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.service.OperationException;
import com.sequenceiq.flow.reactor.ErrorHandlerAwareReactorEventFactory;

@ExtendWith(SpringExtension.class)
public class ResourceDefinitionServiceTest {

    public static final String SAMPLE_RESULT = "SAMPLE_RESULT";

    public static final String CLOUD_PLATFORM = "CLOUD";

    public static final String RESOURCE = "RESOURCE";

    @MockBean
    private EventBus eventBus;

    @MockBean
    private ErrorHandlerAwareReactorEventFactory eventFactory;

    @MockBean
    private RequestProvider requestProvider;

    @Mock
    private ResourceDefinitionRequest resourceDefinitionRequest;

    @Mock
    private ResourceDefinitionResult resourceDefinitionResult;

    @Inject
    private ResourceDefinitionService resourceDefinitionServiceUnderTest;

    @Test
    public void testResourceDefinitionServiceAnswered() throws InterruptedException {
        when(requestProvider.getResourceDefinitionRequest(any(), anyString())).thenReturn(resourceDefinitionRequest);
        when(resourceDefinitionRequest.await()).thenReturn(resourceDefinitionResult);
        when(resourceDefinitionResult.getDefinition()).thenReturn(SAMPLE_RESULT);
        assertEquals(
                SAMPLE_RESULT,
                resourceDefinitionServiceUnderTest.getResourceDefinition(CLOUD_PLATFORM, RESOURCE)
        );
    }

    @Test
    public void testResourceDefinitionServiceUnanswered() throws InterruptedException {
        when(requestProvider.getResourceDefinitionRequest(any(), anyString())).thenReturn(resourceDefinitionRequest);
        when(resourceDefinitionRequest.await()).thenThrow(InterruptedException.class);
        assertThrows(OperationException.class, () ->
                resourceDefinitionServiceUnderTest.getResourceDefinition(CLOUD_PLATFORM, RESOURCE));
    }

    @Configuration
    @Import(ResourceDefinitionService.class)
    static class Config {
    }
}
