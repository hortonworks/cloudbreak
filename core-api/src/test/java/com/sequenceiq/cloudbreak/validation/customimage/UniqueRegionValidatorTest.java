package com.sequenceiq.cloudbreak.validation.customimage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.customimage.request.CustomImageCatalogV4VmImageRequest;

class UniqueRegionValidatorTest {

    private UniqueRegionValidator victim;

    @BeforeEach
    void setUp() {
        victim = new UniqueRegionValidator();
    }

    @Test
    void testValid() {
        assertTrue(victim.isValid(withRegions("region1", "region2"), null));
    }

    @Test
    void testValidEmpty() {
        assertTrue(victim.isValid(Collections.emptySet(), null));
    }

    @Test
    void testValidNull() {
        assertTrue(victim.isValid(null, null));
    }

    @Test
    void testInvalid() {
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