package com.sequenceiq.cloudbreak.service.upgrade;

import static com.sequenceiq.common.model.ImageCatalogPlatform.imageCatalogPlatform;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.InternalUpgradeSettings;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.PlatformStringTransformer;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;

@RunWith(MockitoJUnitRunner.class)
public class ImageFilterParamsFactoryTest {

    private static final Long STACK_ID = 1L;

    private static final String CLOUD_PLATFORM = "AWS";

    @InjectMocks
    private ImageFilterParamsFactory underTest;

    @Mock
    private ParcelService parcelService;

    @Mock
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @Mock
    private Blueprint blueprint;

    @Mock
    private PlatformStringTransformer platformStringTransformer;

    @Test
    public void testCreateShouldReturnsANewImageFilterParamsInstanceWhenTheStackTypeIsDataLake() {
        Image image = mock(Image.class);
        Stack stack = createStack(StackType.DATALAKE);
        Set<ClusterComponentView> clusterComponents = createCdhClusterComponent();
        String cdhName = com.sequenceiq.cloudbreak.cloud.model.component.StackType.CDH.name();
        String cdhVersion = "7.2.0";
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform(CLOUD_PLATFORM));
        when(parcelService.getParcelComponentsByBlueprint(stack)).thenReturn(clusterComponents);
        when(clouderaManagerProductsProvider.findCdhProduct(clusterComponents)).thenReturn(Optional.of(createCMProduct(cdhName, cdhVersion)));

        ImageFilterParams actual = underTest.create(image, true, stack, new InternalUpgradeSettings(false, true, true), false);

        assertEquals(image, actual.getCurrentImage());
        assertTrue(actual.isLockComponents());
        assertEquals(cdhVersion, actual.getStackRelatedParcels().get(cdhName));
        assertEquals(StackType.DATALAKE, actual.getStackType());
        assertEquals(blueprint, actual.getBlueprint());
        assertEquals(STACK_ID, actual.getStackId());
        assertEquals(CLOUD_PLATFORM, actual.getImageCatalogPlatform().nameToUpperCase());
        assertFalse(actual.isGetAllImages());
        verify(parcelService).getParcelComponentsByBlueprint(stack);
        verify(clouderaManagerProductsProvider).findCdhProduct(clusterComponents);
    }

    @Test
    public void testCreateShouldReturnsANewImageFilterParamsInstanceWhenTheStackTypeIsDataHub() {
        Image image = mock(Image.class);
        Stack stack = createStack(StackType.WORKLOAD);
        Set<ClusterComponentView> cdhClusterComponent = createCdhClusterComponent();
        String sparkName = "Spark";
        String sparkVersion = "123";
        ClouderaManagerProduct spark = createCMProduct(sparkName, sparkVersion);
        String nifiName = "Nifi";
        String nifiVersion = "456";
        ClouderaManagerProduct nifi = createCMProduct(nifiName, nifiVersion);
        when(platformStringTransformer.getPlatformStringForImageCatalog(anyString(), anyString())).thenReturn(imageCatalogPlatform(CLOUD_PLATFORM));
        when(parcelService.getParcelComponentsByBlueprint(stack)).thenReturn(cdhClusterComponent);
        when(clouderaManagerProductsProvider.getProducts(cdhClusterComponent)).thenReturn(Set.of(spark, nifi));

        ImageFilterParams actual = underTest.create(image, true, stack, new InternalUpgradeSettings(true, true, true), false);

        assertEquals(image, actual.getCurrentImage());
        assertTrue(actual.isLockComponents());
        assertTrue(actual.isSkipValidations());
        assertEquals(sparkVersion, actual.getStackRelatedParcels().get(sparkName));
        assertEquals(nifiVersion, actual.getStackRelatedParcels().get(nifiName));
        assertEquals(StackType.WORKLOAD, actual.getStackType());
        assertEquals(blueprint, actual.getBlueprint());
        assertEquals(STACK_ID, actual.getStackId());
        assertEquals(CLOUD_PLATFORM, actual.getImageCatalogPlatform().nameToUpperCase());
        assertFalse(actual.isGetAllImages());
        verify(parcelService).getParcelComponentsByBlueprint(stack);
        verify(clouderaManagerProductsProvider).getProducts(cdhClusterComponent);
    }

    @Test(expected = NotFoundException.class)
    public void testCreateShouldThrowExceptionThenThereIsNoCdhComponentAvailable() {
        Image image = mock(Image.class);
        Stack stack = createStack(StackType.DATALAKE);
        ClusterComponentView clusterComponent = new ClusterComponentView();
        clusterComponent.setName("CM");

        when(parcelService.getParcelComponentsByBlueprint(stack)).thenReturn(Collections.singleton(clusterComponent));

        underTest.create(image, true, stack, new InternalUpgradeSettings(false, true, true), false);

        verify(parcelService).getParcelComponentsByBlueprint(stack);
    }

    private Set<ClusterComponentView> createCdhClusterComponent() {
        return Collections.singleton(new ClusterComponentView());
    }

    private Stack createStack(StackType stackType) {
        Stack stack = new Stack();
        stack.setType(stackType);
        stack.setCluster(createCluster());
        stack.setId(STACK_ID);
        stack.setCloudPlatform(CLOUD_PLATFORM);
        stack.setPlatformVariant(CLOUD_PLATFORM);
        return stack;
    }

    private Cluster createCluster() {
        Cluster cluster = new Cluster();
        cluster.setBlueprint(blueprint);
        return cluster;
    }

    private ClouderaManagerProduct createCMProduct(String name, String version) {
        ClouderaManagerProduct clouderaManagerProduct = new ClouderaManagerProduct();
        clouderaManagerProduct.setName(name);
        clouderaManagerProduct.setVersion(version);
        return clouderaManagerProduct;
    }

}