package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;

@ExtendWith(MockitoExtension.class)
class CsdParcelDecoratorTest {

    private static final List<String> CSD_LIST = List.of("csd-1, csd-2, csd-3");

    @Mock
    private ParcelService parcelService;

    @Mock
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    @InjectMocks
    private CsdParcelDecorator underTest;

    @Test
    void testDecoratePillarWithCsdParcelsShouldAddTheParcelsToPillarWhenTheStackTypeIsWorkload() {
        StackDto stack = createStack(StackType.WORKLOAD);
        Set<ClusterComponentView> componentsByBlueprint = Collections.emptySet();
        Set<ClouderaManagerProduct> products = createProducts();
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        when(parcelService.getParcelComponentsByBlueprint(stack)).thenReturn(componentsByBlueprint);
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(componentsByBlueprint)).thenReturn(products);

        underTest.decoratePillarWithCsdParcels(stack, servicePillar);

        SaltPillarProperties pillarProperties = servicePillar.get("csd-downloader");
        assertEquals("/cloudera-manager/csd.sls", pillarProperties.getPath());
        Map<String, List<String>> csdUrls = (Map<String, List<String>>) pillarProperties.getProperties().get("cloudera-manager");
        assertTrue(csdUrls.get("csd-urls").containsAll(CSD_LIST));
        verify(parcelService).getParcelComponentsByBlueprint(stack);
        verify(centralCDHVersionCoordinator).getClouderaManagerProductsFromComponents(componentsByBlueprint);
    }

    @Test
    void testDecoratePillarWithCsdParcelsShouldNotAddTheParcelsToPillarWhenThereAreNoCsdAvailableAndTheStackTypeIsWorkload() {
        StackDto stack = createStack(StackType.WORKLOAD);
        Set<ClusterComponentView> componentsByBlueprint = Collections.emptySet();
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        when(parcelService.getParcelComponentsByBlueprint(stack)).thenReturn(componentsByBlueprint);
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(componentsByBlueprint)).thenReturn(Collections.emptySet());

        underTest.decoratePillarWithCsdParcels(stack, servicePillar);

        assertNull(servicePillar.get("csd-downloader"));
        verify(parcelService).getParcelComponentsByBlueprint(stack);
        verify(centralCDHVersionCoordinator).getClouderaManagerProductsFromComponents(Collections.emptySet());
    }

    @Test
    void testDecoratePillarWithCsdParcelsShouldNotAddTheParcelsToPillarWhenTheStackTypeIsDataLake() {
        StackDto stack = createStack(StackType.DATALAKE);
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        underTest.decoratePillarWithCsdParcels(stack, servicePillar);

        assertNull(servicePillar.get("csd-downloader"));
        verifyNoInteractions(parcelService, centralCDHVersionCoordinator);
    }

    private StackDto createStack(StackType stackType) {
        StackDto stackDto = spy(StackDto.class);
        Stack stack = new Stack();
        stack.setType(stackType);
        when(stackDto.getStack()).thenReturn(stack);
        return stackDto;
    }

    private Set<ClouderaManagerProduct> createProducts() {
        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.setCsd(CSD_LIST);
        return Collections.singleton(product);
    }

}