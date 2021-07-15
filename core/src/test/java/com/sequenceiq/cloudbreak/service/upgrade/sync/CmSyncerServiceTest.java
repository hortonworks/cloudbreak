package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.type.ComponentType;
import com.sequenceiq.cloudbreak.domain.stack.Component;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterComponentUpdater;
import com.sequenceiq.cloudbreak.service.upgrade.StackComponentUpdater;

@ExtendWith(MockitoExtension.class)
public class CmSyncerServiceTest {

    @Mock
    private StackComponentUpdater stackComponentUpdater;

    @Mock
    private ClusterComponentUpdater clusterComponentUpdater;

    @Mock
    private CmInstalledComponentFinderService cmInstalledComponentFinderService;

    @InjectMocks
    private CmSyncerService underTest;

    @Mock
    private Stack stack;

    private Component cdhComponent = new Component(ComponentType.CDH_PRODUCT_DETAILS, "CDH", new Json("{}"), new Stack());

    private Component cmRepoComponent = new Component(ComponentType.CM_REPO_DETAILS, "CmRepoDetails", new Json("{}"), new Stack());

    @Test
    void testSyncFromCmToDb() {
        Set<StatedImage> candidateImages = Set.of();
        doAnswer(addParcelToResult(cdhComponent)).when(cmInstalledComponentFinderService).findParcelComponents(eq(stack), eq(candidateImages), any(Set.class));
        doAnswer(addParcelToResult(cmRepoComponent)).when(cmInstalledComponentFinderService).findCmRepoComponent(eq(stack), any(), any());

        underTest.syncFromCmToDb(stack, candidateImages);

        verify(cmInstalledComponentFinderService).findCmRepoComponent(eq(stack), eq(candidateImages), any());
        verify(cmInstalledComponentFinderService).findParcelComponents(eq(stack), eq(candidateImages), any());

        ArgumentCaptor<Set<Component>> componentArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(stackComponentUpdater).updateComponentsByStackId(eq(stack), componentArgumentCaptor.capture(), anyBoolean());
        Set<Component> componentsToPersist = componentArgumentCaptor.getValue();
        assertThat(componentsToPersist, hasSize(2));
        assertThat(componentsToPersist, containsInAnyOrder(
                hasProperty("componentType", is(ComponentType.CDH_PRODUCT_DETAILS)),
                hasProperty("componentType", is(ComponentType.CM_REPO_DETAILS))
        ));

        ArgumentCaptor<Set<Component>> clusterComponentArgumentCaptor = ArgumentCaptor.forClass(Set.class);
        verify(clusterComponentUpdater).updateClusterComponentsByStackId(eq(stack), clusterComponentArgumentCaptor.capture(), anyBoolean());
        Set<Component> clusterComponentsToPersist = clusterComponentArgumentCaptor.getValue();
        assertThat(clusterComponentsToPersist, hasSize(2));
        assertThat(componentsToPersist, containsInAnyOrder(
                hasProperty("componentType", is(ComponentType.CDH_PRODUCT_DETAILS)),
                hasProperty("componentType", is(ComponentType.CM_REPO_DETAILS))
        ));
    }

    private Answer addParcelToResult(Component comp) {
        return new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                Set<Component> syncedFromServer = invocation.getArgument(2);
                syncedFromServer.add(comp);
                return null;
            }
        };
    }

    private Image getImage(String imageName) {
        Image image = mock(Image.class);
        when(image.getUuid()).thenReturn(imageName);
        return image;
    }

    private void assertComponentProperties(Component component, ComponentType expectedComponentType, String expectedComponentName) {
        assertEquals(expectedComponentType, component.getComponentType());
        assertEquals(expectedComponentName, component.getName());
    }

//
//    private static final String PARCEL_VERSION_1 = "ParcelVersion1";
//
//    private static final String PARCEL_VERSION_2 = "ParcelVersion2";
//
//    private static final String PARCEL_VERSION_3 = "ParcelVersion3";
//
//    private static final String CDH_PARCEL_NAME = "CDH";
//    public static final String CM_VERSION_1 = "CmVersion1";
//    public static final String CM_VERSION_2 = "CmVersion2";
//    public static final String CM_VERSION_3 = "CmVersion3";
//
//    @Mock
//    private CmProductFinderService cmProductFinderService;
//
//    @Mock
//    private StackComponentUpdater stackComponentUpdater;
//
//    @Mock
//    private ComponentConverter componentConverter;
//
//    @Mock
//    private ClusterComponentUpdater clusterComponentUpdater;
//
//    @Mock
//    private CmInfoRetrieverService cmInfoRetriever;
//
//    @Mock
//    private CandidateProductPreparerService candidateProductPreparerService;
//
//    @InjectMocks
//    private CmVersionSyncerService underTest;
//
//    @Mock
//    private Stack stack;
//
//    private Set<StatedImage> candidateImages;
//
//    @BeforeEach
//    void setup() {
//        candidateImages = Set.of(
//                StatedImage.statedImage(getImage(IMAGE_1), IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME),
//                StatedImage.statedImage(getImage(IMAGE_2), IMAGE_CATALOG_URL, IMAGE_CATALOG_NAME)
//        );
//        when(candidateProductPreparerService.getCandidateParcels(any(Set.class), any(Stack.class)))
//                .thenAnswer((Answer<Set<ClouderaManagerProduct>>) this::getClouderaManagerProducts);
//        when(candidateProductPreparerService.getCandidateCmRepos(any(Set.class)))
//                .thenAnswer((Answer<Set<ClouderaManagerRepo>>) this::getClouderaManagerRepos);
//        when(cmProductFinderService.findInstalledParcelProduct(any(), any())).thenCallRealMethod();
//        when(componentConverter.fromClouderaManagerProductList(any(), any())).thenCallRealMethod();
//    }
//
//    @Test
//    void testSyncFromCmToDbWhenComponentFoundBasedOnParcel() throws IOException {
//        when(cmInfoRetriever.getActiveParcelsFromServer(any(Stack.class))).thenReturn(Set.of(new ParcelInfo("CDH", PARCEL_VERSION_1)));
//        when(cmInfoRetriever.getCmVersion(any(Stack.class))).thenReturn(CM_VERSION_1);
//
//        underTest.syncFromCmToDb(stack, candidateImages);
//
//        ArgumentCaptor<Set<Component>> componentArgumentCaptor = ArgumentCaptor.forClass(Set.class);
//        verify(stackComponentUpdater).updateComponentsByStackId(eq(stack), componentArgumentCaptor.capture(), anyBoolean());
//        Set<Component> componentsToPersist = componentArgumentCaptor.getValue();
//        assertThat(componentsToPersist, hasSize(1));
//        Component component = componentsToPersist.stream().findFirst().get();
//        assertComponentProperties(component, ComponentType.CDH_PRODUCT_DETAILS, ComponentType.CDH_PRODUCT_DETAILS.name(), PARCEL_VERSION_1, CDH_PARCEL_NAME);
//
//        ArgumentCaptor<Set<Component>> clusterComponentArgumentCaptor = ArgumentCaptor.forClass(Set.class);
//        verify(clusterComponentUpdater).updateClusterComponentsByStackId(eq(stack), clusterComponentArgumentCaptor.capture(), anyBoolean());
//        Set<Component> clusterComponentsToPersist = clusterComponentArgumentCaptor.getValue();
//        assertThat(clusterComponentsToPersist, hasSize(1));
//        Component clusterComponent = clusterComponentsToPersist.stream().findFirst().get();
//        assertComponentProperties(
//                clusterComponent, ComponentType.CDH_PRODUCT_DETAILS, ComponentType.CDH_PRODUCT_DETAILS.name(), PARCEL_VERSION_1, CDH_PARCEL_NAME);
//    }
//
//    @Test
//    void testSyncFromCmToDbWhenComponentNotFoundBasedOnParcel() {
//        when(cmInfoRetriever.getActiveParcelsFromServer(any(Stack.class))).thenReturn(Set.of(new ParcelInfo("CDH", PARCEL_VERSION_3)));
//        when(cmInfoRetriever.getCmVersion(any(Stack.class))).thenReturn(CM_VERSION_3);
//
//        underTest.syncFromCmToDb(stack, candidateImages);
//
//        ArgumentCaptor<Set<Component>> componentArgumentCaptor = ArgumentCaptor.forClass(Set.class);
//        verify(stackComponentUpdater).updateComponentsByStackId(eq(stack), componentArgumentCaptor.capture(), anyBoolean());
//        Set<Component> componentsToPersist = componentArgumentCaptor.getValue();
//        assertThat(componentsToPersist, hasSize(0));
//
//        ArgumentCaptor<Set<Component>> clusterComponentArgumentCaptor = ArgumentCaptor.forClass(Set.class);
//        verify(clusterComponentUpdater).updateClusterComponentsByStackId(eq(stack), clusterComponentArgumentCaptor.capture(), anyBoolean());
//        Set<Component> clusterComponentsToPersist = clusterComponentArgumentCaptor.getValue();
//        assertThat(clusterComponentsToPersist, hasSize(0));
//    }
//
//    private void assertComponentProperties(Component component, ComponentType expectedComponentType, String expectedComponentName,
//            String expectedProductVersion, String expectedProductName) throws IOException {
//        assertEquals(expectedComponentType, component.getComponentType());
//        assertEquals(expectedComponentName, component.getName());
//        ClouderaManagerProduct clouderaManagerProduct = component.getAttributes().get(ClouderaManagerProduct.class);
//        assertEquals(expectedProductVersion, clouderaManagerProduct.getVersion());
//        assertEquals(expectedProductName, clouderaManagerProduct.getName());
//    }
//
//
//    private Set<ClouderaManagerProduct> getClouderaManagerProducts(InvocationOnMock invocationOnMock) {
//        Set<StatedImage> images = invocationOnMock.getArgument(0);
//        Set<ClouderaManagerProduct> returnedProducts = new HashSet<>();
//        images.forEach(im -> {
//            if (IMAGE_1.equals(im.getImage().getUuid())) {
//                returnedProducts.add(new ClouderaManagerProduct().withName(CDH_PARCEL_NAME).withVersion(PARCEL_VERSION_1).withParcel("Parcel1"));
//            }
//            returnedProducts.add(new ClouderaManagerProduct().withName(CDH_PARCEL_NAME).withVersion(PARCEL_VERSION_2).withParcel("Parcel2"));
//        });
//        return returnedProducts;
//    }
//
//    private Set<ClouderaManagerRepo> getClouderaManagerRepos(InvocationOnMock invocationOnMock) {
//        Set<StatedImage> images = invocationOnMock.getArgument(0);
//        Set<ClouderaManagerRepo> returnedCmRepos = new HashSet<>();
//        images.forEach(im -> {
//            if (IMAGE_1.equals(im.getImage().getUuid())) {
//                returnedCmRepos.add(new ClouderaManagerRepo().withVersion(CM_VERSION_1));
//            }
//            returnedCmRepos.add(new ClouderaManagerRepo().withVersion(CM_VERSION_2));
//        });
//        return returnedCmRepos;
//    }
//
}
