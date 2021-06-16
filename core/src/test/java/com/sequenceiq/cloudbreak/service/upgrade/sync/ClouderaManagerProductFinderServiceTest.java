package com.sequenceiq.cloudbreak.service.upgrade.sync;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

public class ClouderaManagerProductFinderServiceTest {

    private static final String PARCEL_VERSION_1 = "Version1";

    private static final String PARCEL_VERSION_2 = "Version2";

    private static final String PARCEL_NAME_1 = "ParcelName1";

    private static final String PARCEL_NAME_2 = "ParcelName2";

    private final ClouderaManagerProductFinderService underTest = new ClouderaManagerProductFinderService();

    @Test
    void findInstalledProductWhenMatchingNameAndVersionThenReturns() {
        List<ParcelInfo> installedParcels = List.of(new ParcelInfo(PARCEL_NAME_1, PARCEL_VERSION_1));
        List<ClouderaManagerProduct> candidateProducts = List.of(new ClouderaManagerProduct().withName(PARCEL_NAME_1).withVersion(PARCEL_VERSION_1));

        List<ClouderaManagerProduct> foundProducts = underTest.findInstalledProduct(installedParcels, candidateProducts);

        assertThat(foundProducts, hasSize(1));
        assertEquals(PARCEL_NAME_1, foundProducts.get(0).getName());
        assertEquals(PARCEL_VERSION_1, foundProducts.get(0).getVersion());
    }

    @Test
    void findInstalledProductWhenMultipleMatchingNameAndVersionThenReturnsAllMatches() {
        List<ParcelInfo> installedParcels = List.of(
                new ParcelInfo(PARCEL_NAME_1, PARCEL_VERSION_1),
                new ParcelInfo(PARCEL_NAME_2, PARCEL_VERSION_2)
        );
        List<ClouderaManagerProduct> candidateProducts = List.of(
                new ClouderaManagerProduct().withName(PARCEL_NAME_1).withVersion(PARCEL_VERSION_1),
                new ClouderaManagerProduct().withName(PARCEL_NAME_2).withVersion(PARCEL_VERSION_2)
        );

        List<ClouderaManagerProduct> foundProducts = underTest.findInstalledProduct(installedParcels, candidateProducts);

        assertThat(foundProducts, hasSize(2));
        assertEquals(PARCEL_NAME_1, foundProducts.get(0).getName());
        assertEquals(PARCEL_VERSION_1, foundProducts.get(0).getVersion());
        assertEquals(PARCEL_NAME_2, foundProducts.get(1).getName());
        assertEquals(PARCEL_VERSION_2, foundProducts.get(1).getVersion());
    }

    @Test
    void findInstalledProductWhenMultipleMatchingNameAndVersionThenReturnsOne() {
        List<ParcelInfo> installedParcels = List.of(new ParcelInfo(PARCEL_NAME_1, PARCEL_VERSION_1));
        List<ClouderaManagerProduct> candidateProducts = List.of(
                new ClouderaManagerProduct().withName(PARCEL_NAME_1).withVersion(PARCEL_VERSION_1),
                new ClouderaManagerProduct().withName(PARCEL_NAME_1).withVersion(PARCEL_VERSION_1)
        );

        List<ClouderaManagerProduct> foundProducts = underTest.findInstalledProduct(installedParcels, candidateProducts);

        assertThat(foundProducts, hasSize(1));
        assertEquals(PARCEL_NAME_1, foundProducts.get(0).getName());
        assertEquals(PARCEL_VERSION_1, foundProducts.get(0).getVersion());
    }

    @Test
    void findInstalledProductWhenMatchingNameButDifferentVersionThenEmptyResult() {
        List<ParcelInfo> installedParcels = List.of(new ParcelInfo(PARCEL_NAME_1, PARCEL_VERSION_2));
        List<ClouderaManagerProduct> candidateProducts = List.of(new ClouderaManagerProduct().withName(PARCEL_NAME_1).withVersion(PARCEL_VERSION_1));

        List<ClouderaManagerProduct> foundProducts = underTest.findInstalledProduct(installedParcels, candidateProducts);

        assertThat(foundProducts, hasSize(0));
    }

    @Test
    void findInstalledProductWhenDifferentNameButSameVersionThenEmptyResult() {
        List<ParcelInfo> installedParcels = List.of(new ParcelInfo(PARCEL_NAME_2, PARCEL_VERSION_1));
        List<ClouderaManagerProduct> candidateProducts = List.of(new ClouderaManagerProduct().withName(PARCEL_NAME_1).withVersion(PARCEL_VERSION_1));

        List<ClouderaManagerProduct> foundProducts = underTest.findInstalledProduct(installedParcels, candidateProducts);

        assertThat(foundProducts, hasSize(0));
    }

}
