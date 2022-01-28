package com.sequenceiq.cloudbreak.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.UpdateStackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.network.NetworkScaleV4Request;
import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.controller.validation.network.MultiAzValidator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.StackScaleV4RequestToUpdateStackV4RequestConverter;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.stack.CloudParameterCache;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;

@ExtendWith(MockitoExtension.class)
class StackCommonServiceTest {

    private static final long WORKSPACE_ID = 1L;

    private static final long STACK_ID = 2L;

    private static final NameOrCrn STACK_NAME = NameOrCrn.ofName("stackName");

    private static final String SUBNET_ID = "aSubnetId";

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private StackService stackService;

    @Mock
    private StackOperationService stackOperationService;

    @Mock
    private UserService userService;

    @Mock
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Mock
    private CloudbreakEventService eventService;

    @Mock
    private MultiAzValidator multiAzValidator;

    @Mock
    private StackScaleV4RequestToUpdateStackV4RequestConverter stackScaleV4RequestToUpdateStackV4RequestConverter;

    @Mock
    private CloudParameterCache cloudParameterCache;

    @Mock
    private NodeCountLimitValidator nodeCountLimitValidator;

    @Mock
    private TransactionService transactionService;

    @InjectMocks
    private StackCommonService underTest;

    @Test
    public void testCreateImageChangeDtoWithCatalog() {
        StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
        stackImageChangeRequest.setImageCatalogName("catalog");
        stackImageChangeRequest.setImageId("imageId");
        ImageCatalog catalog = new ImageCatalog();
        catalog.setName(stackImageChangeRequest.getImageCatalogName());
        catalog.setImageCatalogUrl("catalogUrl");
        when(imageCatalogService.getImageCatalogByName(WORKSPACE_ID, stackImageChangeRequest.getImageCatalogName())).thenReturn(catalog);
        when(stackService.getIdByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(STACK_ID);

        ImageChangeDto result = underTest.createImageChangeDto(STACK_NAME, WORKSPACE_ID, stackImageChangeRequest);

        assertEquals(STACK_ID, result.getStackId());
        assertEquals(stackImageChangeRequest.getImageId(), result.getImageId());
        assertEquals(catalog.getName(), result.getImageCatalogName());
        assertEquals(catalog.getImageCatalogUrl(), result.getImageCatalogUrl());
    }

    @Test
    public void testCreateImageChangeDtoWithoutCatalog() {
        StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
        stackImageChangeRequest.setImageId("imageId");
        when(stackService.getIdByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(STACK_ID);

        ImageChangeDto result = underTest.createImageChangeDto(STACK_NAME, WORKSPACE_ID, stackImageChangeRequest);

        assertEquals(STACK_ID, result.getStackId());
        assertEquals(stackImageChangeRequest.getImageId(), result.getImageId());
        assertNull(result.getImageCatalogName());
        assertNull(result.getImageCatalogUrl());
    }

    @Test
    public void testChangeImageInWorkspace() {
        StackImageChangeV4Request stackImageChangeRequest = new StackImageChangeV4Request();
        when(stackService.getIdByNameOrCrnInWorkspace(STACK_NAME, WORKSPACE_ID)).thenReturn(STACK_ID);
        when(stackOperationService.updateImage(any(ImageChangeDto.class))).thenReturn(new FlowIdentifier(FlowType.FLOW, "id"));

        FlowIdentifier result = underTest.changeImageInWorkspace(STACK_NAME, WORKSPACE_ID, stackImageChangeRequest);

        assertEquals(FlowType.FLOW, result.getType());
        assertEquals("id", result.getPollableId());
    }

    @Test
    public void testThrowsExceptionWhenDeleteInstanceFromDataLake() {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        when(stackService.findStackByNameOrCrnAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.of(stack));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.deleteInstanceInWorkspace(STACK_NAME, WORKSPACE_ID, "node1", true));

        assertEquals("node1 is a node of a data lake cluster, therefore it's not allowed to delete it.", exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    public void testDeleteInstanceFromDataHub() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        when(stackService.findStackByNameOrCrnAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.of(stack));

        underTest.deleteInstanceInWorkspace(STACK_NAME, WORKSPACE_ID, "node1", true);

        verify(stackOperationService).removeInstance(stack, "node1", true);
    }

    @Test
    public void testThrowsExceptionWhenDeleteInstancesFromDataLake() {
        Stack stack = new Stack();
        stack.setType(StackType.DATALAKE);
        when(stackService.findStackByNameOrCrnAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.of(stack));

        Set<String> nodes = new LinkedHashSet<>();
        nodes.add("node1");
        nodes.add("node2");

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.deleteMultipleInstancesInWorkspace(STACK_NAME, WORKSPACE_ID, nodes, true));

        assertEquals("node1, node2 are nodes of a data lake cluster, therefore it's not allowed to delete them.", exception.getMessage());
        verifyNoInteractions(stackOperationService);
    }

    @Test
    public void testDeleteInstancesFromDataHub() {
        Stack stack = new Stack();
        stack.setType(StackType.WORKLOAD);
        when(stackService.findStackByNameOrCrnAndWorkspaceId(STACK_NAME, WORKSPACE_ID)).thenReturn(Optional.of(stack));

        Set<String> nodes = new LinkedHashSet<>();
        nodes.add("node1");
        nodes.add("node2");

        underTest.deleteMultipleInstancesInWorkspace(STACK_NAME, WORKSPACE_ID, nodes, true);

        verify(stackOperationService).removeInstances(stack, nodes, true);
    }

    @Test
    public void testvalidateNetworkScaleRequestWhenVariantIsNotSupportedForMultiAzButAzHasBeenPreferredInTheRequest() {
        Stack stack = new Stack();
        String variant = AwsConstants.AwsVariant.AWS_VARIANT.name();
        stack.setPlatformVariant(variant);
        when(multiAzValidator.supportedVariant(variant)).thenReturn(Boolean.FALSE);
        NetworkScaleV4Request networkScaleV4Request = new NetworkScaleV4Request();
        networkScaleV4Request.setPreferredSubnetIds(List.of(SUBNET_ID));

        Assertions.assertThrows(BadRequestException.class, () -> underTest.validateNetworkScaleRequest(stack, networkScaleV4Request));

        Mockito.verify(multiAzValidator, times(1)).supportedVariant(variant);
        Mockito.verify(multiAzValidator, times(0)).collectSubnetIds(any());
    }

    @Test
    public void testValidateNetworkScaleRequestWhenThereIsPreferredAzAndStackProvisionedToASingleSubnet() {
        Stack stack = new Stack();
        String variant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.name();
        stack.setPlatformVariant(variant);
        when(multiAzValidator.supportedVariant(variant)).thenReturn(Boolean.TRUE);
        when(multiAzValidator.collectSubnetIds(any())).thenReturn(Set.of(SUBNET_ID));
        NetworkScaleV4Request networkScaleV4Request = new NetworkScaleV4Request();
        networkScaleV4Request.setPreferredSubnetIds(List.of(SUBNET_ID));

        Assertions.assertThrows(BadRequestException.class, () -> underTest.validateNetworkScaleRequest(stack, networkScaleV4Request));

        Mockito.verify(multiAzValidator, times(1)).supportedVariant(variant);
        Mockito.verify(multiAzValidator, times(1)).collectSubnetIds(any());
    }

    @Test
    public void testPutScalingInWorkspaceWhenThereIsPreferredAzAndStackProvisionedInMultipleSubnetButScalingIsNotSupportedOnPlatform()
            throws TransactionService.TransactionExecutionException {
        Stack stack = new Stack();
        stack.setCloudPlatform(AwsConstants.AWS_PLATFORM.value());
        String variant = AwsConstants.AwsVariant.AWS_NATIVE_VARIANT.name();
        stack.setPlatformVariant(variant);
        when(transactionService.required(any(Supplier.class))).thenReturn(stack);
        NetworkScaleV4Request networkScaleV4Request = new NetworkScaleV4Request();
        networkScaleV4Request.setPreferredSubnetIds(List.of(SUBNET_ID));
        StackScaleV4Request updateRequest = new StackScaleV4Request();
        updateRequest.setStackNetworkScaleV4Request(networkScaleV4Request);
        UpdateStackV4Request updateStackV4Request = new UpdateStackV4Request();
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustment.setScalingAdjustment(1);
        updateStackV4Request.setInstanceGroupAdjustment(instanceGroupAdjustment);
        when(stackScaleV4RequestToUpdateStackV4RequestConverter.convert(any())).thenReturn(updateStackV4Request);
        when(cloudParameterCache.isUpScalingSupported(anyString())).thenReturn(Boolean.FALSE);

        BadRequestException actual = assertThrows(BadRequestException.class, () -> underTest.putScalingInWorkspace(STACK_NAME, WORKSPACE_ID, updateRequest));
        assertEquals(actual.getMessage(), "Upscaling is not supported on AWS cloudplatform");

    }
}