package com.sequenceiq.cloudbreak.service.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

@ExtendWith(MockitoExtension.class)
public class PreWarmParcelParserTest {

    private static final String PARCEL_URL = "http://s3.amazonaws.com/dev.hortonworks.com/CSP/centos7/3.x/BUILDS/3.0.0.0-9/tars/parcel";

    private static final String PARCEL_VERSION = "SCHEMAREGISTRY-0.8.0.3.0.0.0-9-el7.parcel";

    private static final String CSD_1 = "https://csd1";

    private static final String CSD_2 = "https://csd2";

    private static final String PARCEL_NAME = "SCHEMAREGISTRY";

    @InjectMocks
    private PreWarmParcelParser preWarmParcelParser;

    @Mock
    private CsdParcelNameMatcher csdParcelNameMatcher;

    @Test
    public void testParseProductFromParcelShouldReturnValidParcelWhenThereIsNoCsd() {
        List<String> parcel = List.of(PARCEL_VERSION, PARCEL_URL);

        Optional<ClouderaManagerProduct> result = preWarmParcelParser.parseProductFromParcel(parcel, Collections.emptyList());

        assertTrue(result.isPresent());
        ClouderaManagerProduct clouderaManagerProduct = result.get();
        assertEquals("SCHEMAREGISTRY", clouderaManagerProduct.getName());
        assertEquals(PARCEL_VERSION, clouderaManagerProduct.getDisplayName());
        assertEquals(PARCEL_URL, clouderaManagerProduct.getParcel());
        assertEquals(PARCEL_URL.concat("/").concat(PARCEL_VERSION), clouderaManagerProduct.getParcelFileUrl());
        assertEquals("0.8.0.3.0.0.0-9", clouderaManagerProduct.getVersion());
        assertTrue(clouderaManagerProduct.getCsd().isEmpty());
        verifyNoInteractions(csdParcelNameMatcher);
    }

    @Test
    public void testParseProductFromParcelShouldReturnValidParcelThereAreExtraDotAtTheEndOfTheUrl() {
        String parcelUrlWithExtraDot = PARCEL_URL + ".";
        List<String> parcel = List.of(PARCEL_VERSION, parcelUrlWithExtraDot);

        Optional<ClouderaManagerProduct> result = preWarmParcelParser.parseProductFromParcel(parcel, Collections.emptyList());

        assertTrue(result.isPresent());
        ClouderaManagerProduct clouderaManagerProduct = result.get();
        assertEquals("SCHEMAREGISTRY", clouderaManagerProduct.getName());
        assertEquals(PARCEL_VERSION, clouderaManagerProduct.getDisplayName());
        assertEquals(parcelUrlWithExtraDot, clouderaManagerProduct.getParcel());
        assertEquals(PARCEL_URL.concat("/").concat(PARCEL_VERSION), clouderaManagerProduct.getParcelFileUrl());
        assertEquals("0.8.0.3.0.0.0-9", clouderaManagerProduct.getVersion());
        assertTrue(clouderaManagerProduct.getCsd().isEmpty());
        verifyNoInteractions(csdParcelNameMatcher);
    }

    @Test
    public void testParseProductFromParcelShouldReturnValidParcelThereAreExtraSlashAtTheEndOfTheUrl() {
        String parcelUrlWithExtraSlash = PARCEL_URL + ".";
        List<String> parcel = List.of(PARCEL_VERSION, parcelUrlWithExtraSlash);

        Optional<ClouderaManagerProduct> result = preWarmParcelParser.parseProductFromParcel(parcel, Collections.emptyList());

        assertTrue(result.isPresent());
        ClouderaManagerProduct clouderaManagerProduct = result.get();
        assertEquals("SCHEMAREGISTRY", clouderaManagerProduct.getName());
        assertEquals(PARCEL_VERSION, clouderaManagerProduct.getDisplayName());
        assertEquals(parcelUrlWithExtraSlash, clouderaManagerProduct.getParcel());
        assertEquals(PARCEL_URL.concat("/").concat(PARCEL_VERSION), clouderaManagerProduct.getParcelFileUrl());
        assertEquals("0.8.0.3.0.0.0-9", clouderaManagerProduct.getVersion());
        assertTrue(clouderaManagerProduct.getCsd().isEmpty());
        verifyNoInteractions(csdParcelNameMatcher);
    }

    @Test
    public void testParseProductFromParcelShouldReturnValidParcelWhenThereAreMultipleCsdParcels() {
        List<String> parcel = List.of(PARCEL_VERSION, PARCEL_URL);
        List<String> cdsParcels = List.of(CSD_1, CSD_2);
        when(csdParcelNameMatcher.matching(CSD_1, PARCEL_NAME)).thenReturn(false);
        when(csdParcelNameMatcher.matching(CSD_2, PARCEL_NAME)).thenReturn(true);

        Optional<ClouderaManagerProduct> actual = preWarmParcelParser.parseProductFromParcel(parcel, cdsParcels);

        assertTrue(actual.isPresent());
        ClouderaManagerProduct clouderaManagerProduct = actual.get();
        assertEquals(PARCEL_NAME, clouderaManagerProduct.getName());
        assertEquals(PARCEL_VERSION, clouderaManagerProduct.getDisplayName());
        assertEquals(PARCEL_URL, clouderaManagerProduct.getParcel());
        assertEquals(PARCEL_URL.concat("/").concat(PARCEL_VERSION), clouderaManagerProduct.getParcelFileUrl());
        assertEquals("0.8.0.3.0.0.0-9", clouderaManagerProduct.getVersion());
        assertTrue(clouderaManagerProduct.getCsd().contains(CSD_2));
        assertEquals(1, clouderaManagerProduct.getCsd().size());
        verify(csdParcelNameMatcher).matching(CSD_1, PARCEL_NAME);
        verify(csdParcelNameMatcher).matching(CSD_2, PARCEL_NAME);
    }

    @Test
    public void testParseProductFromParcelShouldReturnOptionalEmptyWhenTheParcelNamePartIsMissing() {
        assertTrue(preWarmParcelParser.parseProductFromParcel(List.of(PARCEL_VERSION), Collections.emptyList()).isEmpty());
    }

    @Test
    public void testParseProductFromParcelShouldReturnOptionalEmptyWhenTheParcelVersionPartIsMissing() {
        assertTrue(preWarmParcelParser.parseProductFromParcel(List.of(PARCEL_URL), Collections.emptyList()).isEmpty());
    }
}