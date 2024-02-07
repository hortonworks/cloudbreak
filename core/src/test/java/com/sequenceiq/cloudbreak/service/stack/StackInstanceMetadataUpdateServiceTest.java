package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_OPTIONAL;
import static com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateProperties;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeMetadata;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeProperty;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
public class StackInstanceMetadataUpdateServiceTest {

    @Mock
    private StackDtoService stackService;

    @Mock
    private ReactorFlowManager flowManager;

    @Mock
    private InstanceMetadataUpdateProperties instanceMetadataUpdateProperties;

    @InjectMocks
    private StackInstanceMetadataUpdateService underTest;

    @BeforeEach
    void setup() {
        InstanceMetadataUpdateTypeMetadata metadataV2 = new InstanceMetadataUpdateTypeMetadata("v2");
        InstanceMetadataUpdateTypeProperty propertyV2 = new InstanceMetadataUpdateTypeProperty("AWS", Map.of(AWS, metadataV2));
        InstanceMetadataUpdateTypeMetadata metadataV1 = new InstanceMetadataUpdateTypeMetadata("v1");
        InstanceMetadataUpdateTypeProperty propertyV1 = new InstanceMetadataUpdateTypeProperty("AWS", Map.of(AWS, metadataV1));
        when(instanceMetadataUpdateProperties.getTypes()).thenReturn(Map.of(IMDS_HTTP_TOKEN_REQUIRED, propertyV2, IMDS_HTTP_TOKEN_OPTIONAL, propertyV1));
    }

    @Test
    void testTrigger() {
        StackDto stack = mock(StackDto.class);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getSupportedImdsVersion()).thenReturn("v1");
        when(stackService.getByCrn(any())).thenReturn(stack);
        when(flowManager.triggerInstanceMetadataUpdate(any(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "1"));

        underTest.updateInstanceMetadata("env", IMDS_HTTP_TOKEN_REQUIRED);

        verify(flowManager).triggerInstanceMetadataUpdate(any(), eq(IMDS_HTTP_TOKEN_REQUIRED));
    }

    @Test
    void testTriggerWhenImdsOptionalAndAlreadySet() {
        StackDto stack = mock(StackDto.class);
        when(stack.getCloudPlatform()).thenReturn("AWS");
        when(stack.getSupportedImdsVersion()).thenReturn("v1");
        when(stackService.getByCrn(any())).thenReturn(stack);

        assertThrows(BadRequestException.class, () -> underTest.updateInstanceMetadata("env", IMDS_HTTP_TOKEN_OPTIONAL));

        verify(flowManager, times(0)).triggerInstanceMetadataUpdate(any(), eq(IMDS_HTTP_TOKEN_OPTIONAL));
    }
}
