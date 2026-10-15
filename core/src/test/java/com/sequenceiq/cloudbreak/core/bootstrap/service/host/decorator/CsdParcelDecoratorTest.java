package com.sequenceiq.cloudbreak.core.bootstrap.service.host.decorator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
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
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.BlueprintHybridOption;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
class CsdParcelDecoratorTest {

    private static final List<String> CSD_LIST = List.of("csd-1, csd-2, csd-3");

    private static final String ACCOUNT_ID = "accountId";

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:" + ACCOUNT_ID + ":cluster:f3b8ed82-e712-4f89-bda7-be07183720d3";

    @Mock
    private ParcelService parcelService;

    @Mock
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    @Mock
    private EntitlementService entitlementService;

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
        assertEquals(false, csdUrls.get("upgrade-preparation"));

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
    void testDecoratePillarWithCsdParcelsShouldNotAddTheParcelsToPillarWhenTheStackTypeIsDataLakeWithoutEntitlement() {
        StackDto stack = createStack(StackType.DATALAKE);
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        underTest.decoratePillarWithCsdParcels(stack, servicePillar);

        assertNull(servicePillar.get("csd-downloader"));
        verifyNoInteractions(parcelService, centralCDHVersionCoordinator);
    }

    @Test
    void testDecoratePillarWithCsdParcelsShouldNotAddTheParcelsToPillarWhenTheStackTypeIsDataLakeWithEntitlementButNoHybridOption() {
        StackDto stack = createStack(StackType.DATALAKE);
        setUpOnPremEntitledCluster(stack);
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        underTest.decoratePillarWithCsdParcels(stack, servicePillar);

        assertNull(servicePillar.get("csd-downloader"));
        verifyNoInteractions(parcelService, centralCDHVersionCoordinator);
    }

    @Test
    void testDecoratePillarWithCsdParcelsShouldNotAddTheParcelsToPillarWhenTheStackTypeIsDataLakeWithEntitlementButNoBlueprint() {
        StackDto stack = createStack(StackType.DATALAKE);
        when(entitlementService.isOnPremBaseDlShapeEnabled(ACCOUNT_ID)).thenReturn(true);
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        underTest.decoratePillarWithCsdParcels(stack, servicePillar);

        assertNull(servicePillar.get("csd-downloader"));
        verifyNoInteractions(parcelService, centralCDHVersionCoordinator);
    }

    @Test
    void testDecoratePillarWithCsdParcelsShouldNotAddTheParcelsToPillarWhenTheStackTypeIsDataLakeWithEntitlementAndHybridOption() {
        StackDto stack = createStack(StackType.DATALAKE);
        Set<ClusterComponentView> componentsByBlueprint = Collections.emptySet();
        Blueprint blueprint = setUpOnPremEntitledCluster(stack);
        when(blueprint.getHybridOption()).thenReturn(BlueprintHybridOption.ON_PREM_BASE);
        when(parcelService.getParcelComponentsByBlueprint(stack)).thenReturn(componentsByBlueprint);
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(componentsByBlueprint)).thenReturn(createProducts());
        Map<String, SaltPillarProperties> servicePillar = new HashMap<>();

        underTest.decoratePillarWithCsdParcels(stack, servicePillar);

        SaltPillarProperties pillarProperties = servicePillar.get("csd-downloader");
        assertEquals("/cloudera-manager/csd.sls", pillarProperties.getPath());
        Map<String, List<String>> csdUrls = (Map<String, List<String>>) pillarProperties.getProperties().get("cloudera-manager");
        assertTrue(csdUrls.get("csd-urls").containsAll(CSD_LIST));
        assertEquals(false, csdUrls.get("upgrade-preparation"));

        verify(parcelService).getParcelComponentsByBlueprint(stack);
        verify(centralCDHVersionCoordinator).getClouderaManagerProductsFromComponents(componentsByBlueprint);
    }

    @Test
    void testAddCsdParcelsToServicePillarShouldSetUpgradePreparationTrue() {
        Map<String, SaltPillarProperties> result =
                underTest.addCsdParcelsToServicePillar(createProducts(), true);
        SaltPillarProperties pillarProperties = result.get("csd-downloader");
        Map<String, Object> csdUrls =
                (Map<String, Object>) pillarProperties.getProperties().get("cloudera-manager");

        assertEquals(true, csdUrls.get("upgrade-preparation"));
    }

    private StackDto createStack(StackType stackType) {
        StackDto stackDto = spy(StackDto.class);
        Stack stack = new Stack();
        stack.setType(stackType);
        stack.setId(1L);
        Workspace workspace = new Workspace();
        workspace.setId(2L);
        stack.setWorkspace(workspace);
        stack.setResourceCrn(RESOURCE_CRN);
        when(stackDto.getStack()).thenReturn(stack);
        return stackDto;
    }

    private Blueprint setUpOnPremEntitledCluster(StackDto stackDto) {
        Blueprint blueprint = mock();
        when(stackDto.getBlueprint()).thenReturn(blueprint);
        when(entitlementService.isOnPremBaseDlShapeEnabled(ACCOUNT_ID)).thenReturn(true);
        return blueprint;
    }

    private Set<ClouderaManagerProduct> createProducts() {
        ClouderaManagerProduct product = new ClouderaManagerProduct();
        product.setCsd(CSD_LIST);
        return Collections.singleton(product);
    }

}