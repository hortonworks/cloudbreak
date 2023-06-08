package com.sequenceiq.cloudbreak.service.upgrade.sync;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;
import com.sequenceiq.cloudbreak.cluster.model.ParcelInfo;
import com.sequenceiq.cloudbreak.cluster.model.ParcelStatus;

class CustomParcelFilterServiceTest {

    private final CustomParcelFilterService underTest = new CustomParcelFilterService();

    @Test
    void testFilterCustomParcelsShouldReturnAllParcelsWhenThereAreNoCustomParcelFound() {
        Set<ParcelInfo> activeParcels = Set.of(createParcelInfo("CDH"), createParcelInfo("CFM"), createParcelInfo("SPARK"));
        Set<ClouderaManagerProduct> availableProducts =
                Set.of(createCmProduct("CFM"), createCmProduct("CDH"), createCmProduct("SPARK"), createCmProduct("NIFI"));

        Set<ParcelInfo> actual = underTest.filterCustomParcels(activeParcels, availableProducts);

        assertEquals(activeParcels, actual);
    }

    @Test
    void testFilterCustomParcelsShouldReturnParcelsWhenACustomParcelIsPresent() {
        ParcelInfo parcel1 = createParcelInfo("CDH");
        ParcelInfo parcel2 = createParcelInfo("CFM");
        ParcelInfo parcel3 = createParcelInfo("SPARK");
        Set<ParcelInfo> activeParcels = Set.of(parcel1, parcel2, parcel3, createParcelInfo("custom_parcel"));
        Set<ClouderaManagerProduct> availableProducts =
                Set.of(createCmProduct("CFM"), createCmProduct("CDH"), createCmProduct("SPARK"), createCmProduct("NIFI"));

        Set<ParcelInfo> actual = underTest.filterCustomParcels(activeParcels, availableProducts);

        assertEquals(Set.of(parcel1, parcel2, parcel3), actual);
    }

    private ParcelInfo createParcelInfo(String parcelName) {
        return new ParcelInfo(parcelName, "7.2.12", ParcelStatus.ACTIVATED);
    }

    private ClouderaManagerProduct createCmProduct(String productName) {
        return new ClouderaManagerProduct().withName(productName);
    }

}