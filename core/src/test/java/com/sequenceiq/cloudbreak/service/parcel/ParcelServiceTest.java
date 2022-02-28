package com.sequenceiq.cloudbreak.service.parcel;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelOperationStatus;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.CloudbreakRuntimeException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.image.ImageService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;

@ExtendWith(MockitoExtension.class)
public class ParcelServiceTest {

    private static final long CLUSTER_ID = 2L;

    private static final long STACK_ID = 1L;

    private static final String PARCEL_NAME = "parcel1";

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClusterComponentUpdater clusterComponentUpdater;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ImageService imageService;

    @Mock
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @InjectMocks
    private ParcelService underTest;

    @Test
    void testRemoveUnusedComponents() throws CloudbreakException, CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack();
        Set<ClusterComponent> clusterComponentsByBlueprint = Collections.emptySet();
        Image currentImage = Mockito.mock(Image.class);
        Set<ClouderaManagerProduct> products = Set.of(new ClouderaManagerProduct().withName(PARCEL_NAME));
        ParcelOperationStatus removalStatus = new ParcelOperationStatus();

        when(imageService.getCurrentImage(STACK_ID)).thenReturn(StatedImage.statedImage(currentImage, null, null));
        when(clouderaManagerProductTransformer.transform(currentImage, false, true)).thenReturn(products);
        when(clusterApiConnectors.getConnector(stack)).thenReturn(clusterApi);
        when(clusterApi.removeUnusedParcels(clusterComponentsByBlueprint, Set.of(PARCEL_NAME))).thenReturn(removalStatus);

        underTest.removeUnusedParcelComponents(stack, clusterComponentsByBlueprint);

        verify(imageService).getCurrentImage(STACK_ID);
        verify(clouderaManagerProductTransformer).transform(currentImage, false, true);
        verify(clusterApiConnectors).getConnector(stack);
        verify(clusterApi).removeUnusedParcels(clusterComponentsByBlueprint, Set.of(PARCEL_NAME));
        verify(clusterComponentUpdater).removeUnusedCdhProductsFromClusterComponents(stack.getCluster().getId(), clusterComponentsByBlueprint, removalStatus);
    }

    @Test
    void testRemoveUnusedComponentsShouldCallParcelRemovalWithEmptyProductListWhenTheCurrentImageIsNotFound()
            throws CloudbreakImageNotFoundException, CloudbreakImageCatalogException {
        Stack stack = createStack();
        Set<ClusterComponent> clusterComponentsByBlueprint = Collections.emptySet();

        when(imageService.getCurrentImage(STACK_ID)).thenThrow(new CloudbreakImageCatalogException("Image not found"));

        assertThrows(CloudbreakRuntimeException.class, () -> underTest.removeUnusedParcelComponents(stack, clusterComponentsByBlueprint));

        verify(imageService).getCurrentImage(STACK_ID);
        verifyNoInteractions(clusterApiConnectors);
        verifyNoInteractions(clusterComponentUpdater);
        verifyNoInteractions(clusterApi);
        verifyNoInteractions(clouderaManagerProductTransformer);
    }

    private Stack createStack() {
        Stack stack = new Stack();
        Cluster cluster = new Cluster();
        cluster.setId(CLUSTER_ID);
        stack.setId(STACK_ID);
        stack.setCluster(cluster);
        return stack;
    }
}
