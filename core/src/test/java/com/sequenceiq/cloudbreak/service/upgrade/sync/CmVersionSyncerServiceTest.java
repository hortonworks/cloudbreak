package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.ClouderaManagerProductConverter;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.parcel.ClouderaManagerProductTransformer;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.StackComponentUpdater;

@ExtendWith(MockitoExtension.class)
public class CmVersionSyncerServiceTest {

    private static final String IMAGE_1 = "Image1";

    private static final String IMAGE_2 = "Image2";

    private static final String IMAGE_CATALOG_NAME = "imageCatalogName";

    private static final String IMAGE_CATALOG_URL = "imageCatalogUrl";

    private static final String PARCEL_VERSION_1 = "Version1";

    private static final String PARCEL_VERSION_2 = "Version2";

    private static final String PARCEL_VERSION_3 = "Version3";

    private static final String CDH_PARCEL_NAME = "CDH";

    @Mock
    private ClouderaManagerProductFinderService clouderaManagerProductFinderService;

    @Mock
    private ClouderaManagerProductTransformer clouderaManagerProductTransformer;

    @Mock
    private StackComponentUpdater stackComponentUpdater;

    @Mock
    private ClouderaManagerProductConverter clouderaManagerProductConverter;

    @Mock
    private ClusterComponentUpdater clusterComponentUpdater;

    @Mock
    private CmParcelInfoRetrieverService cmParcelInfoRetriever;

    @InjectMocks
    private CmVersionSyncerService underTest;

    @Mock
    private Stack stack;

    private Set<StatedImage> candidateImages;

    @BeforeEach
    void setup() {
        candidateImages = Set.of(
                StatedImage.statedImage(getImage(IMAGE_1), IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME),
                StatedImage.statedImage(getImage(IMAGE_2), IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME)
        );
        when(clouderaManagerProductTransformer.transform(any(), anyBoolean()))
                .thenAnswer((Answer<Set<ClouderaManagerProduct>>) this::getClouderaManagerProducts);
        when(clouderaManagerProductFinderService.findInstalledProduct(any(), any())).thenCallRealMethod();
        when(clouderaManagerProductConverter.clouderaManagerProductListToComponent(any(), any())).thenCallRealMethod();
    }

    @Test
    void testSyncCmParcelsToDbWhenComponentFoundBasedOnParcel() throws IOException {
        when(cmParcelInfoRetriever.getActiveParcelsFromServer(anyLong())).thenReturn(List.of(new ParcelInfo("CDH", PARCEL_VERSION_1)));

        underTest.syncCmParcelsToDb(stack, candidateImages);

        ArgumentCaptor<Set<Component>> componentArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(stackComponentUpdater).updateComponentsByStackId(eq(stack), componentArgumentCaptor.capture(), anyBoolean());
        Set<Component> componentsToPersist = componentArgumentCaptor.getValue();
        assertThat(componentsToPersist, hasSize(1));
        Component component = componentsToPersist.stream().findFirst().get();
        assertComponentProperties(component, ComponentType.CDH_PRODUCT_DETAILS, ComponentType.CDH_PRODUCT_DETAILS.name(), PARCEL_VERSION_1, CDH_PARCEL_NAME);

        ArgumentCaptor<Set<Component>> clusterComponentArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(clusterComponentUpdater).updateClusterComponentsByStackId(eq(stack), clusterComponentArgumentCaptor.capture(), anyBoolean());
        Set<Component> clusterComponentsToPersist = clusterComponentArgumentCaptor.getValue();
        assertThat(clusterComponentsToPersist, hasSize(1));
        Component clusterComponent = clusterComponentsToPersist.stream().findFirst().get();
        assertComponentProperties(
                clusterComponent, ComponentType.CDH_PRODUCT_DETAILS, ComponentType.CDH_PRODUCT_DETAILS.name(), PARCEL_VERSION_1, CDH_PARCEL_NAME);
    }

    @Test
    void testSyncCmParcelsToDbWhenComponentNotFoundBasedOnParcel() throws IOException {
        when(cmParcelInfoRetriever.getActiveParcelsFromServer(anyLong())).thenReturn(List.of(new ParcelInfo("CDH", PARCEL_VERSION_3)));

        underTest.syncCmParcelsToDb(stack, candidateImages);

        ArgumentCaptor<Set<Component>> componentArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(stackComponentUpdater).updateComponentsByStackId(eq(stack), componentArgumentCaptor.capture(), anyBoolean());
        Set<Component> componentsToPersist = componentArgumentCaptor.getValue();
        assertThat(componentsToPersist, hasSize(0));

        ArgumentCaptor<Set<Component>> clusterComponentArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(clusterComponentUpdater).updateClusterComponentsByStackId(eq(stack), clusterComponentArgumentCaptor.capture(), anyBoolean());
        Set<Component> clusterComponentsToPersist = clusterComponentArgumentCaptor.getValue();
        assertThat(clusterComponentsToPersist, hasSize(0));
    }

    private void assertComponentProperties(Component component, ComponentType expectedComponentType, String expectedComponentName,
            String expectedProductVersion, String expectedProductName) throws IOException {
        assertEquals(expectedComponentType, component.getComponentType());
        assertEquals(expectedComponentName, component.getName());
        ClouderaManagerProduct clouderaManagerProduct = component.getAttributes().get(ClouderaManagerProduct.class);
        assertEquals(expectedProductVersion, clouderaManagerProduct.getVersion());
        assertEquals(expectedProductName, clouderaManagerProduct.getName());
    }

    private Image getImage(String imageName) {
        Image image = mock(Image.class);
        when(image.getUuid()).thenReturn(imageName);
        return image;
    }

    private Set<ClouderaManagerProduct> getClouderaManagerProducts(InvocationOnMock invocationOnMock) {
        Image image = invocationOnMock.getArgument(0);
        if (IMAGE_1.equals(image.getUuid())) {
            return Set.of(new ClouderaManagerProduct().withName(CDH_PARCEL_NAME).withVersion(PARCEL_VERSION_1).withParcel("Parcel1"));
        }
        return Set.of(new ClouderaManagerProduct().withName(CDH_PARCEL_NAME).withVersion(PARCEL_VERSION_2).withParcel("Parcel2"));
    }

}
