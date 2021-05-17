package com.sequenceiq.cloudbreak.validation.customimage;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4VmImageRequest;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UniqueRegionValidatorTest {

    private UniqueRegionValidator victim;

    @Before
    public void setUp() {
        victim = new UniqueRegionValidator();
    }

    @Test
    public void testValid() {
        assertTrue(victim.isValid(withRegions("region1", "region2"), null));
    }

    @Test
    public void testValidEmpty() {
        assertTrue(victim.isValid(Collections.emptySet(), null));
    }

    @Test
    public void testValidNull() {
        assertTrue(victim.isValid(null, null));
    }

    @Test
    public void testInvalid() {
        assertFalse(victim.isValid(withRegions("region1", "region1"), null));
    }

    private Set<CustomImageCatalogV4VmImageRequest> withRegions(String... regions) {
        return Arrays.stream(regions).map(r -> {
            CustomImageCatalogV4VmImageRequest item = new CustomImageCatalogV4VmImageRequest();
            item.setRegion(r);

            return item;
        }).collect(Collectors.toSet());
    }
}