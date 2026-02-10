package com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateHandlerSelectors.DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.DATAHUB_DISK_UPDATE_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.DistroXDiskUpdateStateSelectors.FAILED_DATAHUB_DISK_UPDATE_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskType;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.DiskTypes;
import com.sequenceiq.cloudbreak.cloud.model.VolumeParameterType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.flow2.cluster.verticalscale.diskupdate.event.DistroXDiskUpdateEvent;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.VerticalScalingValidatorService;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class DistroXDiskUpdateValidationHandlerTest {

    private static final Long STACK_ID = 1L;

    private static final String TEST_CLUSTER = "TEST_CLUSTER";

    private static final String ACCOUNT_ID = "ACCOUNT_ID";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private StackService stackService;

    @Mock
    private DiskUpdateService diskUpdateService;

    @Mock
    private VerticalScalingValidatorService verticalScalingValidatorService;

    @InjectMocks
    private DistroXDiskUpdateValidationHandler underTest;

    private Json json;

    @BeforeEach
    void setUp() {
        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume(
                "vol-1",
                "/dev/xvdb",
                50,
                VolumeParameterType.ST1.name().toLowerCase(Locale.ROOT),
                CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("us-west-2a", true, "", List.of(volume),
                512, "standard");
        json = new Json(volumeSetAttributes);
    }

    @Test
    void testDiskUpdateValidationAction() {
        String selector = DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT.event();
        DistroXDiskUpdateEvent event = DistroXDiskUpdateEvent.builder()
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withSize(100)
                .withVolumeType("gp2")
                .withGroup("compute")
                .withSelector(selector)
                .withStackId(STACK_ID)
                .withResourceId(STACK_ID)
                .build();
        StackDto stackDto = mock(StackDto.class);
        Stack stack = mock(Stack.class);
        Resource resource = mock(Resource.class);
        doReturn("A1234").when(resource).getInstanceId();
        doReturn("compute").when(resource).getInstanceGroup();
        doReturn(json).when(resource).getAttributes();
        doReturn(ResourceType.AWS_VOLUMESET).when(resource).getResourceType();
        DiskTypes diskTypes = mock(DiskTypes.class);
        when(diskTypes.diskMapping()).thenReturn(
                Map.of(
                        "ephemeral", VolumeParameterType.EPHEMERAL,
                        "st1", VolumeParameterType.ST1
                )
        );
        when(diskUpdateService.getDiskTypes(any(StackDto.class))).thenReturn(diskTypes);
        Resource resource2 = mock(Resource.class);
        doReturn("B1234").when(resource2).getInstanceId();
        doReturn(Set.of(resource, resource2)).when(stackDto).getResources();
        doReturn(CloudPlatform.AWS.toString()).when(stackDto).getCloudPlatform();
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        ValidationResult validationResult = mock(ValidationResult.class);
        when(validationResult.hasError()).thenReturn(false);
        when(verticalScalingValidatorService.validateAddVolumesRequest(any(), any(), any()))
                .thenReturn(validationResult);
        doReturn(true).when(diskUpdateService).isDiskTypeChangeSupported(CloudPlatform.AWS.toString());
        doReturn(stackDto).when(stackDtoService).getById(anyLong());

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        DistroXDiskUpdateEvent eventCaptured = (DistroXDiskUpdateEvent) selectable;
        assertEquals(DATAHUB_DISK_UPDATE_EVENT.selector(), eventCaptured.getSelector());
        assertEquals(1, eventCaptured.getVolumesToBeUpdated().size());
        assertEquals("vol-1", eventCaptured.getVolumesToBeUpdated().get(0).getId());
    }

    @Test
    void testFailureDiskUpdateValidationAction() throws IOException {
        String selector = DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT.event();
        DistroXDiskUpdateEvent event = DistroXDiskUpdateEvent.builder()
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withGroup("compute")
                .withVolumeType("gp2")
                .withSize(100)
                .withSelector(selector)
                .withStackId(STACK_ID)
                .withResourceId(STACK_ID)
                .build();
        StackDto stackDto = mock(StackDto.class);
        Stack stack = mock(Stack.class);
        when(stackDto.getCloudPlatform()).thenReturn(CloudPlatform.AWS.name());
        Resource resource = mock(Resource.class);
        doReturn("A1234").when(resource).getInstanceId();
        doReturn("compute").when(resource).getInstanceGroup();
        doReturn(ResourceType.AWS_VOLUMESET).when(resource).getResourceType();
        doReturn(json).when(resource).getAttributes();
        doReturn(Set.of(resource)).when(stackDto).getResources();
        doReturn(stackDto).when(stackDtoService).getById(anyLong());
        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));
        assertEquals(FAILED_DATAHUB_DISK_UPDATE_EVENT.selector(), selectable.getSelector());
    }

    @Test
    void testDiskUpdateValidationActionWithDatabaseDiskType() {
        String selector = DATAHUB_DISK_UPDATE_VALIDATION_HANDLER_EVENT.event();
        DistroXDiskUpdateEvent event = DistroXDiskUpdateEvent.builder()
                .withClusterName(TEST_CLUSTER)
                .withAccountId(ACCOUNT_ID)
                .withSize(100)
                .withVolumeType("gp2")
                .withGroup("compute")
                .withDiskType(DiskType.DATABASE_DISK.name())
                .withSelector(selector)
                .withStackId(STACK_ID)
                .withResourceId(STACK_ID)
                .build();

        VolumeSetAttributes.Volume volume = new VolumeSetAttributes.Volume(
                "vol-1",
                "/dev/xvdb",
                50,
                VolumeParameterType.ST1.name().toLowerCase(Locale.ROOT),
                CloudVolumeUsageType.GENERAL);
        VolumeSetAttributes.Volume dbVolume = new VolumeSetAttributes.Volume(
                "db-vol-1",
                "/dev/xvdb",
                50,
                VolumeParameterType.ST1.name().toLowerCase(Locale.ROOT),
                CloudVolumeUsageType.DATABASE);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes("us-west-2a", true, "", List.of(volume, dbVolume),
                512, "standard");
        Json dbJson = new Json(volumeSetAttributes);

        StackDto stackDto = mock(StackDto.class);
        Stack stack = mock(Stack.class);
        Resource resource = mock(Resource.class);
        doReturn("A1234").when(resource).getInstanceId();
        doReturn("compute").when(resource).getInstanceGroup();
        doReturn(dbJson).when(resource).getAttributes();
        doReturn(ResourceType.AWS_VOLUMESET).when(resource).getResourceType();

        DiskTypes diskTypes = mock(DiskTypes.class);
        when(diskTypes.diskMapping()).thenReturn(
                Map.of(
                        "ephemeral", VolumeParameterType.EPHEMERAL,
                        "st1", VolumeParameterType.ST1
                )
        );
        when(diskUpdateService.getDiskTypes(any(StackDto.class))).thenReturn(diskTypes);
        doReturn(Set.of(resource)).when(stackDto).getResources();
        doReturn(CloudPlatform.AWS.toString()).when(stackDto).getCloudPlatform();
        when(stackService.getByIdWithLists(anyLong())).thenReturn(stack);
        ValidationResult validationResult = mock(ValidationResult.class);
        when(validationResult.hasError()).thenReturn(false);
        when(verticalScalingValidatorService.validateAddVolumesRequest(any(), any(), any()))
                .thenReturn(validationResult);
        doReturn(true).when(diskUpdateService).isDiskTypeChangeSupported(CloudPlatform.AWS.toString());
        doReturn(stackDto).when(stackDtoService).getById(anyLong());

        Selectable selectable = underTest.doAccept(new HandlerEvent<>(new Event<>(event)));

        DistroXDiskUpdateEvent eventCaptured = (DistroXDiskUpdateEvent) selectable;
        assertEquals(DATAHUB_DISK_UPDATE_EVENT.selector(), eventCaptured.getSelector());
        assertEquals(1, eventCaptured.getVolumesToBeUpdated().size());
        assertEquals("db-vol-1", eventCaptured.getVolumesToBeUpdated().get(0).getId());
        assertEquals(CloudVolumeUsageType.DATABASE, eventCaptured.getVolumesToBeUpdated().get(0).getVolumeUsageType());
    }

}