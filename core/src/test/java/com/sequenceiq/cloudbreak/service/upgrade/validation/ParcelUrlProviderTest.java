package com.sequenceiq.cloudbreak.service.upgrade.validation;

import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.ACTIVATED;
import static com.sequenceiq.cloudbreak.cluster.model.ParcelStatus.DISTRIBUTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.parcel.ParcelService;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradeProperties;
import com.sequenceiq.cloudbreak.service.upgrade.ClusterUpgradePropertiesTestUtils;
import com.sequenceiq.cloudbreak.service.upgrade.sync.component.CmServerQueryService;
import com.sequenceiq.common.model.OsType;

@ExtendWith(MockitoExtension.class)
class ParcelUrlProviderTest {

    private static final String PRE_WARM_CSD = "http://spark3.jar";

    @Mock
    private ParcelService parcelService;

    @Mock
    private CmServerQueryService cmServerQueryService;

    @Mock
    private StackDto stackDto;

    @InjectMocks
    private ParcelUrlProvider underTest;

    @Test
    void testGetRequiredParcelsFromImageShouldReturnRequiredParcelUrlsWhenTheStackTypeIsWorkload() {
        ClouderaManagerProduct cdh = createCmProduct("CDH", "7.2.15", "https://cdh.parcel");
        ClouderaManagerProduct spark3 = createCmProduct("SPARK3", "1.2.4", "https://spark3.parcel").withCsd(Collections.singletonList(PRE_WARM_CSD));
        ClouderaManagerProduct nifi = createCmProduct("NIFI", "4.5.6", "https://nifi.parcel");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.2.15", "base-image", OsType.RHEL8, "x86_64", cdh, Set.of(spark3, nifi), null);
        when(parcelService.getComponentNamesByProducts(stackDto, properties.getAllTargetProducts())).thenReturn(Set.of("CDH", "SPARK3"));
        when(cmServerQueryService.queryAllParcels(stackDto)).thenReturn(
                Set.of(new ParcelInfo("CDH", "7.2.7", ACTIVATED), new ParcelInfo("SPARK3", "1.2.3", ACTIVATED)));

        Set<String> actual = underTest.getRequiredParcelsFromImage(properties, stackDto);

        assertEquals(3, actual.size());
        assertTrue(actual.contains(cdh.getParcelFileUrl()));
        assertTrue(actual.contains(spark3.getParcelFileUrl()));
        assertTrue(actual.contains(PRE_WARM_CSD));
        verify(parcelService).getComponentNamesByProducts(stackDto, properties.getAllTargetProducts());
        verify(cmServerQueryService).queryAllParcels(stackDto);
    }

    @Test
    void testGetRequiredParcelsFromImageShouldReturnRequiredParcelUrlsWhenTheStackTypeIsDataLake() {
        ClouderaManagerProduct cdh = createCmProduct("CDH", "7.2.15", "https://cdh.parcel");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.2.15", "base-image", OsType.RHEL8, "x86_64", cdh, Set.of(), null);
        when(parcelService.getComponentNamesByProducts(stackDto, properties.getAllTargetProducts())).thenReturn(Collections.singleton("CDH"));
        when(cmServerQueryService.queryAllParcels(stackDto)).thenReturn(Set.of(new ParcelInfo("CDH", "7.2.7", ACTIVATED)));

        Set<String> actual = underTest.getRequiredParcelsFromImage(properties, stackDto);

        assertEquals(1, actual.size());
        assertTrue(actual.contains(cdh.getParcelFileUrl()));
        verify(parcelService).getComponentNamesByProducts(stackDto, properties.getAllTargetProducts());
        verify(cmServerQueryService).queryAllParcels(stackDto);
    }

    @Test
    void testGetRequiredParcelsFromImageShouldReturnEmptyListWhenAllRequiredParcelIsAlreadyDistributed() {
        ClouderaManagerProduct cdh = createCmProduct("CDH", "7.2.15", "https://cdh.parcel");
        ClouderaManagerProduct spark3 = createCmProduct("SPARK3", "1.2.4", "https://spark3.parcel").withCsd(Collections.singletonList(PRE_WARM_CSD));
        ClouderaManagerProduct nifi = createCmProduct("NIFI", "4.5.6", "https://nifi.parcel");
        ClusterUpgradeProperties properties = ClusterUpgradePropertiesTestUtils.withTargetProducts(
                "7.2.15", "base-image", OsType.RHEL8, "x86_64", cdh, Set.of(spark3, nifi), null);
        when(parcelService.getComponentNamesByProducts(stackDto, properties.getAllTargetProducts())).thenReturn(Set.of("CDH", "SPARK3"));
        when(cmServerQueryService.queryAllParcels(stackDto)).thenReturn(
                Set.of(new ParcelInfo(cdh.getName(), "7.2.7", ACTIVATED), new ParcelInfo(spark3.getName(), "1.2.3", ACTIVATED),
                        new ParcelInfo(cdh.getName(), cdh.getVersion(), DISTRIBUTED), new ParcelInfo(spark3.getName(), spark3.getVersion(), DISTRIBUTED)));

        Set<String> actual = underTest.getRequiredParcelsFromImage(properties, stackDto);

        assertTrue(actual.isEmpty());
        verify(parcelService).getComponentNamesByProducts(stackDto, properties.getAllTargetProducts());
        verify(cmServerQueryService).queryAllParcels(stackDto);
    }

    private ClouderaManagerProduct createCmProduct(String name, String version, String parcelFileUrl) {
        return new ClouderaManagerProduct().withName(name).withVersion(version).withParcelFileUrl(parcelFileUrl);
    }

}