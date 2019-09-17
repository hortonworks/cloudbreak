package com.sequenceiq.cloudbreak.service.image;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.Test;

import com.sequenceiq.cloudbreak.cloud.model.ClouderaManagerProduct;

public class PreWarmParcelParserTest {

    private static final String PARCEL_URL = "http://s3.amazonaws.com/dev.hortonworks.com/CSP/centos7/3.x/BUILDS/3.0.0.0-9/tars/parcel";

    private static final String PARCEL_VERSION = "SCHEMAREGISTRY-0.8.0.3.0.0.0-9-el7.parcel";

    private final PreWarmParcelParser preWarmParcelParser = new PreWarmParcelParser();

    @Test
    public void testWithValidParcel() {
        List<String> parcel = List.of(PARCEL_VERSION, PARCEL_URL);

        Optional<ClouderaManagerProduct> result = preWarmParcelParser.parseProductFromParcel(parcel);

        assertTrue(result.isPresent());
        assertEquals("SCHEMAREGISTRY", result.get().getName());
        assertEquals(PARCEL_URL, result.get().getParcel());
        assertEquals("0.8.0.3.0.0.0-9", result.get().getVersion());
    }

    @Test
    public void testWithMissingParcelPart() {
        List<String> parcel = List.of(PARCEL_VERSION);

        Optional<ClouderaManagerProduct> result = preWarmParcelParser.parseProductFromParcel(parcel);

        assertTrue(result.isEmpty());
    }
}