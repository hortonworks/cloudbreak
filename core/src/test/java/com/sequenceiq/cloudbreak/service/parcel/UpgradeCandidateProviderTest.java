package com.sequenceiq.cloudbreak.service.parcel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;
import com.sequenceiq.cloudbreak.domain.view.ClusterComponentView;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.stack.CentralCDHVersionCoordinator;

@ExtendWith(MockitoExtension.class)
class UpgradeCandidateProviderTest {

    private static final String STACK_NAME = "stack-name";

    private static final String CDH = "CDH";

    private static final String CFM = "CFM";

    @InjectMocks
    private UpgradeCandidateProvider underTest;

    @Mock
    private CentralCDHVersionCoordinator centralCDHVersionCoordinator;

    @Mock
    private ClusterApi connector;

    @Mock
    private StackDto stackDto;

    private Set<ClusterComponentView> componentsByBlueprint = Set.of(new ClusterComponentView());

    @BeforeEach
    void before() {
        when(stackDto.getName()).thenReturn(STACK_NAME);
    }

    @Test
    void testGetRequiredProductsForUpgradeShouldReturnAllRequiredProduct() {
        Set<ParcelInfo> activeParcels = Set.of(createParcelInfo(CDH, "7.2.15"), createParcelInfo(CFM, "1.2.3"));
        Set<ClouderaManagerProduct> candidateProducts = Set.of(createProduct(CDH, "7.2.16"), createProduct(CFM, "1.2.4"));
        when(connector.gatherInstalledParcels(STACK_NAME)).thenReturn(activeParcels);
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(componentsByBlueprint)).thenReturn(candidateProducts);

        Set<ClouderaManagerProduct> actual = underTest.getRequiredProductsForUpgrade(connector, stackDto, componentsByBlueprint);

        assertEquals(candidateProducts, actual);
    }

    @Test
    void testGetRequiredProductsForUpgradeShouldReturnEmptySetWhenAllRequiredProductIsAlreadyActivated() {
        Set<ParcelInfo> activeParcels = Set.of(createParcelInfo(CDH, "7.2.15"), createParcelInfo(CFM, "1.2.3"));
        Set<ClouderaManagerProduct> candidateProducts = Set.of(createProduct(CDH, "7.2.15"), createProduct(CFM, "1.2.3"));
        when(connector.gatherInstalledParcels(STACK_NAME)).thenReturn(activeParcels);
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(componentsByBlueprint)).thenReturn(candidateProducts);

        Set<ClouderaManagerProduct> actual = underTest.getRequiredProductsForUpgrade(connector, stackDto, componentsByBlueprint);

        assertTrue(actual.isEmpty());
    }

    @Test
    void testGetRequiredProductsForUpgradeShouldReturnOnlyTheCDHParcelWhenTheCFMParcelIsAlreadyActivated() {
        Set<ParcelInfo> activeParcels = Set.of(createParcelInfo(CDH, "7.2.15"), createParcelInfo(CFM, "1.2.3"));
        ClouderaManagerProduct cdhProduct = createProduct(CDH, "7.2.16");
        Set<ClouderaManagerProduct> candidateProducts = Set.of(cdhProduct, createProduct(CFM, "1.2.3"));
        when(connector.gatherInstalledParcels(STACK_NAME)).thenReturn(activeParcels);
        when(centralCDHVersionCoordinator.getClouderaManagerProductsFromComponents(componentsByBlueprint)).thenReturn(candidateProducts);

        Set<ClouderaManagerProduct> actual = underTest.getRequiredProductsForUpgrade(connector, stackDto, componentsByBlueprint);

        assertTrue(actual.contains(cdhProduct));
        assertEquals(1, actual.size());
    }

    private ParcelInfo createParcelInfo(String name, String version) {
        return new ParcelInfo(name, version, ParcelStatus.ACTIVATED);
    }

    private ClouderaManagerProduct createProduct(String name, String version) {
        return new ClouderaManagerProduct().withName(name).withVersion(version);
    }

}