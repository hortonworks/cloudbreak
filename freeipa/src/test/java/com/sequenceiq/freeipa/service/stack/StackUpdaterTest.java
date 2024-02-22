package com.sequenceiq.freeipa.service.stack;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateProperties;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateType;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeMetadata;
import com.sequenceiq.cloudbreak.common.imdupdate.InstanceMetadataUpdateTypeProperty;
import com.sequenceiq.freeipa.entity.Stack;

@ExtendWith(MockitoExtension.class)
public class StackUpdaterTest {

    @Mock
    private StackService stackService;

    @Mock
    private InstanceMetadataUpdateProperties instanceMetadataUpdateProperties;

    @InjectMocks
    private StackUpdater underTest;

    @Test
    void testUpdateImdsVersionIfMatchingVersion() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setSupportedImdsVersion("v2");
        when(stackService.getStackById(any())).thenReturn(stack);
        mockGetTypes();

        underTest.updateSupportedImdsVersion(1L, InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED);

        verify(stackService, times(0)).save(any());
    }

    @Test
    void testUpdateImdsVersion() {
        Stack stack = new Stack();
        stack.setCloudPlatform("AWS");
        stack.setSupportedImdsVersion("v1");
        when(stackService.getStackById(any())).thenReturn(stack);
        mockGetTypes();
        when(stackService.save(any())).thenReturn(stack);

        underTest.updateSupportedImdsVersion(1L, InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED);

        verify(stackService).save(any());
    }

    private void mockGetTypes() {
        InstanceMetadataUpdateTypeMetadata metadataV2 = new InstanceMetadataUpdateTypeMetadata("v2");
        InstanceMetadataUpdateTypeProperty propertyV2 = new InstanceMetadataUpdateTypeProperty("AWS", Map.of(AWS, metadataV2));
        when(instanceMetadataUpdateProperties.getTypes()).thenReturn(Map.of(InstanceMetadataUpdateType.IMDS_HTTP_TOKEN_REQUIRED, propertyV2));
    }
}
