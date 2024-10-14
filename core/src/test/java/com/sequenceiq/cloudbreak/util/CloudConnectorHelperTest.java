package com.sequenceiq.cloudbreak.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudConnectResources;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.converter.spi.CloudContextProvider;
import com.sequenceiq.cloudbreak.converter.spi.StackToCloudStackConverter;
import com.sequenceiq.cloudbreak.dto.StackDto;

@ExtendWith(MockitoExtension.class)
class CloudConnectorHelperTest {

    @Mock
    private CloudContextProvider cloudContextProvider;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private StackToCloudStackConverter cloudStackConverter;

    @Mock
    private StackUtil stackUtil;

    @InjectMocks
    private CloudConnectorHelper underTest;

    @Mock
    private CloudStack cloudStack;

    @Mock
    private StackDto stack;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private Authenticator authenticator;

    @Mock
    private CloudContext cloudContext;

    @Test
    void testGetCloudConnector() throws CloudbreakServiceException  {
        doReturn(cloudContext).when(cloudContextProvider).getCloudContext(stack);
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(authenticator).when(cloudConnector).authentication();
        doReturn("test template").when(cloudStack).getTemplate();
        doReturn("test-env-crn").when(stack).getEnvironmentCrn();
        doReturn(cloudStack).when(cloudStackConverter).convert(stack);
        CloudConnectResources result = underTest.getCloudConnectorResources(stack);
        assertEquals(cloudStack, result.getCloudStack());
        assertEquals("test template", result.getCloudStack().getTemplate());
    }
}
