package com.sequenceiq.cloudbreak.service.upgrade;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cloud.model.catalog.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.upgrade.image.ImageFilterParams;

@RunWith(MockitoJUnitRunner.class)
public class ImageFilterParamsFactoryTest {

    @InjectMocks
    private ImageFilterParamsFactory underTest;

    @Mock
    private ParcelService parcelService;

    @Mock
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @Test
    public void testCreateShouldReturnsANewImageFilterParamsInstanceWhenTheStackTypeIsDataLake() {
        Image image = mock(Image.class);
        Stack stack = createStack(StackType.DATALAKE);
        Set<ClusterComponent> clusterComponents = createCdhClusterComponent();
        String cdhName = com.sequenceiq.cloudbreak.cloud.model.component.StackType.CDH.name();
        String cdhVersion = "7.2.0";

        when(parcelService.getParcelComponentsByBlueprint(stack)).thenReturn(clusterComponents);
        when(clouderaManagerProductsProvider.findCdhProduct(clusterComponents)).thenReturn(Optional.of(createCMProduct(cdhName, cdhVersion)));

        ImageFilterParams actual = underTest.create(image, true, stack);

        assertEquals(image, actual.getCurrentImage());
        assertTrue(actual.isLockComponents());
        assertEquals(cdhVersion, actual.getStackRelatedParcels().get(cdhName));
        assertEquals(StackType.DATALAKE, actual.getStackType());
        verify(parcelService).getParcelComponentsByBlueprint(stack);
        verify(clouderaManagerProductsProvider).findCdhProduct(clusterComponents);
    }

    @Test
    public void testCreateShouldReturnsANewImageFilterParamsInstanceWhenTheStackTypeIsDataHub() {
        Image image = mock(Image.class);
        Stack stack = createStack(StackType.WORKLOAD);
        Set<ClusterComponent> cdhClusterComponent = createCdhClusterComponent();
        String sparkName = "Spark";
        String sparkVersion = "123";
        ClouderaManagerProduct spark = createCMProduct(sparkName, sparkVersion);
        String nifiName = "Nifi";
        String nifiVersion = "456";
        ClouderaManagerProduct nifi = createCMProduct(nifiName, nifiVersion);

        when(parcelService.getParcelComponentsByBlueprint(stack)).thenReturn(cdhClusterComponent);
        when(clouderaManagerProductsProvider.getProducts(cdhClusterComponent)).thenReturn(Set.of(spark, nifi));

        ImageFilterParams actual = underTest.create(image, true, stack);

        assertEquals(image, actual.getCurrentImage());
        assertTrue(actual.isLockComponents());
        assertEquals(sparkVersion, actual.getStackRelatedParcels().get(sparkName));
        assertEquals(nifiVersion, actual.getStackRelatedParcels().get(nifiName));
        assertEquals(StackType.WORKLOAD, actual.getStackType());
        verify(parcelService).getParcelComponentsByBlueprint(stack);
        verify(clouderaManagerProductsProvider).getProducts(cdhClusterComponent);
    }

    @Test(expected = NotFoundException.class)
    public void testCreateShouldThrowExceptionThenThereIsNoCdhComponentAvailable() {
        Image image = mock(Image.class);
        Stack stack = createStack(StackType.DATALAKE);
        ClusterComponent clusterComponent = new ClusterComponent();
        clusterComponent.setName("CM");

        when(parcelService.getParcelComponentsByBlueprint(stack)).thenReturn(Collections.singleton(clusterComponent));

        underTest.create(image, true, stack);

        verify(parcelService).getParcelComponentsByBlueprint(stack);
    }

    private Set<ClusterComponent> createCdhClusterComponent() {
        return Collections.singleton(new ClusterComponent());
    }

    private Stack createStack(StackType stackType) {
        Stack stack = new Stack();
        stack.setType(stackType);
        return stack;
    }

    private ClouderaManagerProduct createCMProduct(String name, String version) {
        ClouderaManagerProduct clouderaManagerProduct = new ClouderaManagerProduct();
        clouderaManagerProduct.setName(name);
        clouderaManagerProduct.setVersion(version);
        return clouderaManagerProduct;
    }

}