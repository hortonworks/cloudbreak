package com.sequenceiq.environment.environment.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.FreeIpaResponse;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;

public class FreeIpaConverterTest {

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
    }

    @Test
    public void testConvertWithTwoInstancesAndOnlySpotInstances() {
        // GIVEN
        FreeIpaCreationDto request = FreeIpaCreationDto.builder()
                .withInstanceCountByGroup(2)
                .withAws(FreeIpaCreationAwsParametersDto.builder()
                        .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                                .withPercentage(100)
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
    }

}