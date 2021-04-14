package com.sequenceiq.environment.environment.v1.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.sequenceiq.environment.api.v1.environment.model.request.FreeIpaImageRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.FreeIpaResponse;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;

public class FreeIpaConverterTest {

    private static final String IMAGE_CATALOG = "image catalog";

    private static final String IMAGE_ID = "image id";

    private FreeIpaConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new FreeIpaConverter();
    }

    @Test
    public void testConvertWithNull() {
        // GIVEN
        FreeIpaCreationDto request = null;
        // WHEN
        FreeIpaResponse result = underTest.convert(request);
        // THEN
        assertNull(result);
    }

    @Test
    public void testConvertWithDefaults() {
        // GIVEN
        FreeIpaCreationDto request = FreeIpaCreationDto.builder().build();
        // WHEN
        FreeIpaResponse result = underTest.convert(request);
        // THEN
        assertNotNull(result);
        assertNotNull(result.getInstanceCountByGroup());
        assertEquals(1, result.getInstanceCountByGroup());
        assertNull(result.getAws());
        assertNull(result.getImage());
    }

    @Test
    public void testConvertWithFreeIpaImageCatalogAndId() {
        // GIVEN
        FreeIpaCreationDto request = FreeIpaCreationDto.builder()
                .withImageId(IMAGE_ID)
                .withImageCatalog(IMAGE_CATALOG)
                .build();
        // WHEN
        FreeIpaResponse result = underTest.convert(request);
        // THEN
        assertEquals(IMAGE_ID, result.getImage().getId());
        assertEquals(IMAGE_CATALOG, result.getImage().getCatalog());
    }

    @Test
    public void testConvertWithFreeIpaImageCatalogButWithoutImageId() {
        // GIVEN
        FreeIpaCreationDto request = FreeIpaCreationDto.builder()
                .withImageCatalog(IMAGE_CATALOG)
                .build();
        // WHEN
        FreeIpaResponse result = underTest.convert(request);
        // THEN
        assertNull(result.getImage());
    }

    @Test
    public void testConvertWithFreeIpaImageIdButWithoutImageCatalog() {
        // GIVEN
        FreeIpaCreationDto request = FreeIpaCreationDto.builder()
                .withImageId(IMAGE_ID)
                .build();
        // WHEN
        FreeIpaResponse result = underTest.convert(request);
        // THEN
        assertNull(result.getImage());
    }

    @Test
    public void testConvertWithTwoInstancesAndOnlySpotInstances() {
        // GIVEN
        FreeIpaCreationDto request = FreeIpaCreationDto.builder()
                .withInstanceCountByGroup(2)
                .withAws(FreeIpaCreationAwsParametersDto.builder()
                        .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                                .withPercentage(100)
                                .withMaxPrice(0.9)
                                .build())
                        .build())
                .build();
        // WHEN
        FreeIpaResponse result = underTest.convert(request);
        // THEN
        assertNotNull(result);
        assertNotNull(result.getInstanceCountByGroup());
        assertEquals(2, result.getInstanceCountByGroup());
        assertEquals(100, result.getAws().getSpot().getPercentage());
        assertEquals(0.9, result.getAws().getSpot().getMaxPrice());
    }

    @Test
    public void testConvertWithImage() {
        // GIVEN
        AttachedFreeIpaRequest request = new AttachedFreeIpaRequest();
        request.setCreate(true);
        request.setImage(aFreeIpaImage(IMAGE_CATALOG, IMAGE_ID));
        // WHEN
        FreeIpaCreationDto result = underTest.convert(request);
        // THEN
        assertEquals(IMAGE_CATALOG, result.getImageCatalog());
        assertEquals(IMAGE_ID, result.getImageId());
    }

    @Test
    public void testConvertWithoutImage() {
        // GIVEN
        AttachedFreeIpaRequest request = new AttachedFreeIpaRequest();
        request.setCreate(true);
        request.setImage(null);
        // WHEN
        FreeIpaCreationDto result = underTest.convert(request);
        // THEN
        assertNull(result.getImageCatalog());
        assertNull(result.getImageId());
    }

    @Test
    public void testConvertWithoutImageIdAndImageCatalog() {
        // GIVEN
        AttachedFreeIpaRequest request = new AttachedFreeIpaRequest();
        request.setCreate(true);
        request.setImage(aFreeIpaImage(null, null));
        // WHEN
        FreeIpaCreationDto result = underTest.convert(request);
        // THEN
        assertNull(result.getImageCatalog());
        assertNull(result.getImageId());
    }

    private FreeIpaImageRequest aFreeIpaImage(String catalog, String id) {
        FreeIpaImageRequest request = new FreeIpaImageRequest();
        request.setId(id);
        request.setCatalog(catalog);

        return  request;
    }
}