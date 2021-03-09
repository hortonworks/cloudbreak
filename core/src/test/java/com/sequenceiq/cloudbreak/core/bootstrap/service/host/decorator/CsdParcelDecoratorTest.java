package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.service.ClouderaManagerProductsProvider;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterComponent;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;

@RunWith(MockitoJUnitRunner.class)
public class CsdParcelDecoratorTest {

    private static final List<String> CSD_LIST = List.of("csd-1, csd-2, csd-3");

    @Mock
    private ParcelService parcelService;

    @Mock
    private ClouderaManagerProductsProvider clouderaManagerProductsProvider;

    @InjectMocks
    private CsdParcelDecorator underTest;

    @Test
    public void testDecoratePillarWithCsdParcelsShouldAddTheParcelsToPillarWhenTheStackTypeIsWorkload() {
        Stack stack = createStack(StackType.WORKLOAD);
        Set<ClusterComponent> componentsByBlueprint = Collections.emptySet();
        Set<ClouderaManagerProduct> products = createProducts();
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        when(parcelService.getParcelComponentsByBlueprint(stack)).thenReturn(componentsByBlueprint);
        when(clouderaManagerProductsProvider.getProducts(componentsByBlueprint)).thenReturn(products);

        underTest.decoratePillarWithCsdParcels(stack, servicePillar);

        SaltPillarProperties pillarProperties = servicePillar.get("csd-downloader");
        assertEquals("/cloudera-manager/csd.sls", pillarProperties.getPath());
        Map<String, List<String>> csdUrls = (Map<String, List<String>>) pillarProperties.getProperties().get("cloudera-manager");
        assertTrue(csdUrls.get("csd-urls").containsAll(CSD_LIST));
        verify(parcelService).getParcelComponentsByBlueprint(stack);
        verify(clouderaManagerProductsProvider).getProducts(componentsByBlueprint);
    }

    @Test
    public void testDecoratePillarWithCsdParcelsShouldNotAddTheParcelsToPillarWhenThereAreNoCsdAvailableAndTheStackTypeIsWorkload() {
        Stack stack = createStack(StackType.WORKLOAD);
        Set<ClusterComponent> componentsByBlueprint = Collections.emptySet();
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        when(parcelService.getParcelComponentsByBlueprint(stack)).thenReturn(componentsByBlueprint);
        when(clouderaManagerProductsProvider.getProducts(componentsByBlueprint)).thenReturn(Collections.emptySet());

        underTest.decoratePillarWithCsdParcels(stack, servicePillar);

        assertNull(servicePillar.get("csd-downloader"));
        verify(parcelService).getParcelComponentsByBlueprint(stack);
        verify(clouderaManagerProductsProvider).getProducts(Collections.emptySet());
    }

    @Test
    public void testDecoratePillarWithCsdParcelsShouldNotAddTheParcelsToPillarWhenTheStackTypeIsDataLake() {
        Stack stack = createStack(StackType.DATALAKE);
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        underTest.decoratePillarWithCsdParcels(stack, servicePillar);

        assertNull(servicePillar.get("csd-downloader"));
        verifyNoInteractions(parcelService, clouderaManagerProductsProvider);
    }

    private Stack createStack(StackType stackType) {
        Stack stack = new Stack();
        stack.setType(stackType);
        return stack;
    }

    private Set<ClouderaManagerProduct> createProducts() {
        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.setCsd(CSD_LIST);
        return Collections.singleton(product);
    }

}