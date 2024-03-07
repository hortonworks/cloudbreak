package com.sequenceiq.freeipa.service.stack.instance;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.flow.freeipa.upscale.event.UpscaleStackResult;
import com.sequenceiq.freeipa.flow.stack.StackContext;

@ExtendWith(MockitoExtension.class)
class InstanceValidationServiceTest {

    @Mock
    private MetadataSetupService metadataSetupService;

    @InjectMocks
    private InstanceValidationService underTest;

    @Test
    void testPayloadHasErrors() {
        assertThrows(CloudbreakException.class,
                () -> underTest.finishAddInstances(mock(StackContext.class), new UpscaleStackResult("afd", new CloudbreakException("dfa"), 3L)));
        verifyNoInteractions(metadataSetupService);
    }

    @Test
    void testTemplateFailed() {
        assertThrows(CloudbreakException.class,
                () -> underTest.finishAddInstances(mock(StackContext.class), new UpscaleStackResult(3L, ResourceStatus.CREATED, List.of(
                        new CloudResourceStatus(CloudResource.builder()
                                .withType(ResourceType.CLOUDFORMATION_STACK)
                                .withStatus(CommonStatus.FAILED)
                                .withName("asdf")
                                .withParameters(Map.of())
                                .build(), ResourceStatus.FAILED)))));
        verifyNoInteractions(metadataSetupService);
    }

    @Test
    void testEveryResourceFailed() {
        assertThrows(CloudbreakException.class,
                () -> underTest.finishAddInstances(mock(StackContext.class), new UpscaleStackResult(3L, ResourceStatus.CREATED, List.of(
                        new CloudResourceStatus(CloudResource.builder()
                                .withType(ResourceType.AWS_INSTANCE)
                                .withStatus(CommonStatus.FAILED)
                                .withName("asdf")
                                .withParameters(Map.of())
                                .build(), ResourceStatus.FAILED)))));
        verify(metadataSetupService).cleanupRequestedInstances(any());
    }

    @Test
    void testSingleFailure() throws CloudbreakException {
        underTest.finishAddInstances(mock(StackContext.class), new UpscaleStackResult(3L, ResourceStatus.CREATED, List.of(
                        new CloudResourceStatus(CloudResource.builder()
                                .withType(ResourceType.AWS_INSTANCE)
                                .withStatus(CommonStatus.FAILED)
                                .withName("asdf")
                                .withParameters(Map.of())
                                .build(), ResourceStatus.FAILED),
                        new CloudResourceStatus(CloudResource.builder()
                                .withType(ResourceType.AWS_INSTANCE)
                                .withStatus(CommonStatus.CREATED)
                                .withName("asdf")
                                .withParameters(Map.of())
                                .build(), ResourceStatus.CREATED))));
        verifyNoInteractions(metadataSetupService);
    }
}