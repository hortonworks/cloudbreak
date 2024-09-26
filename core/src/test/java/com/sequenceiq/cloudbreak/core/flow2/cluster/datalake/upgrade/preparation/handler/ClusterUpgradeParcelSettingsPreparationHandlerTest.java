package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.handler;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT;
import static com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.ClusterUpgradePreparationStateSelectors.START_CLUSTER_UPGRADE_CM_PACKAGE_DOWNLOAD_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade.preparation.event.ClusterUpgradeParcelSettingsPreparationEvent;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.ImageChangeDto;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ClusterUpgradeParcelSettingsPreparationHandlerTest {

    private static final long STACK_ID = 1L;

    private static final long WORKSPACE_ID = 2L;

    private static final String IMAGE_ID = "image-id";

    private static final String IMAGE_CATALOG_NAME = "image-catalog-name";

    private static final String IMAGE_CATALOG_URL = "image-catalog-url";

    @InjectMocks
    private ClusterUpgradeParcelSettingsPreparationHandler underTest;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ParcelService parcelService;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private Image image;

    @Mock
    private StackDto stackDto;

    @Test
    void testDoAcceptShouldCallClusterApiToUpdateParcelSettings() throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException,
            CloudbreakException {
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL);
        Set<ClouderaManagerProduct> requiredProducts = Collections.singleton(new ClouderaManagerProduct());
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDto.getWorkspace()).thenReturn(createWorkspace());
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID)).thenReturn(
                StatedImage.statedImage(image, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME));
        when(parcelService.getRequiredProductsFromImage(stackDto, image)).thenReturn(requiredProducts);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent(imageChangeDto));

        assertEquals(START_CLUSTER_UPGRADE_CM_PACKAGE_DOWNLOAD_EVENT.name(), nextFlowStepSelector.selector());
        verify(stackDtoService).getById(STACK_ID);
        verify(imageCatalogService).getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID);
        verify(parcelService).getRequiredProductsFromImage(stackDto, image);
        verify(clusterApiConnectors).getConnector(stackDto);
        verify(clusterApi).updateParcelSettings(requiredProducts);
    }

    @Test
    void testDoAcceptShouldReturnPreparationFailureEventWhenClusterApiReturnsWithAnException()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException,
            CloudbreakException {
        ImageChangeDto imageChangeDto = new ImageChangeDto(STACK_ID, IMAGE_ID, IMAGE_CATALOG_NAME, IMAGE_CATALOG_URL);
        Set<ClouderaManagerProduct> requiredProducts = Collections.singleton(new ClouderaManagerProduct());
        when(stackDtoService.getById(STACK_ID)).thenReturn(stackDto);
        when(stackDto.getWorkspace()).thenReturn(createWorkspace());
        when(imageCatalogService.getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID)).thenReturn(
                StatedImage.statedImage(image, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME));
        when(parcelService.getRequiredProductsFromImage(stackDto, image)).thenReturn(requiredProducts);
        when(clusterApiConnectors.getConnector(stackDto)).thenReturn(clusterApi);
        doThrow(new CloudbreakException("Failed to update parcel settings")).when(clusterApi).updateParcelSettings(requiredProducts);

        Selectable nextFlowStepSelector = underTest.doAccept(createEvent(imageChangeDto));

        assertEquals(FAILED_CLUSTER_UPGRADE_PREPARATION_EVENT.name(), nextFlowStepSelector.selector());
        verify(stackDtoService).getById(STACK_ID);
        verify(imageCatalogService).getImage(WORKSPACE_ID, IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME, IMAGE_ID);
        verify(parcelService).getRequiredProductsFromImage(stackDto, image);
        verify(clusterApiConnectors).getConnector(stackDto);
        verify(clusterApi).updateParcelSettings(requiredProducts);
    }

    private HandlerEvent<ClusterUpgradeParcelSettingsPreparationEvent> createEvent(ImageChangeDto imageChangeDto) {
        return new HandlerEvent<>(new Event<>(new ClusterUpgradeParcelSettingsPreparationEvent(STACK_ID, imageChangeDto)));
    }

    private Workspace createWorkspace() {
        Workspace workspace = new Workspace();
        workspace.setId(WORKSPACE_ID);
        return workspace;
    }
}