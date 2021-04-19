package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.mockito.Mockito.when;

import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;

@ExtendWith(MockitoExtension.class)
class EnvironmentDetailsToCDPFreeIPADetailsConverterTest {

    private EnvironmentDetailsToCDPFreeIPADetailsConverter underTest;

    @Mock
    private EnvironmentDetails environmentDetails;

    @Mock
    private FreeIpaCreationDto freeIpaCreationDto;

    @Mock
    private FreeIpaCreationAwsParametersDto aws;

    @Mock
    private FreeIpaCreationAwsSpotParametersDto spot;

    @BeforeEach()
    public void setUp() {
        underTest = new EnvironmentDetailsToCDPFreeIPADetailsConverter();
    }

    @Test
    public void testNull() {
        UsageProto.CDPFreeIPADetails freeIPADetails = underTest.convert(null);

        Assertions.assertFalse(freeIPADetails.getSpot());
        Assertions.assertEquals(-1, freeIPADetails.getNodes());
    }

    @Test
    public void testNullFreeIpaCreationDto() {
        when(environmentDetails.getFreeIpaCreation()).thenReturn(null);

        UsageProto.CDPFreeIPADetails freeIPADetails = underTest.convert(environmentDetails);

        Assertions.assertFalse(freeIPADetails.getSpot());
        Assertions.assertEquals(-1, freeIPADetails.getNodes());
    }

    @Test
    public void testEmpty() {
        when(environmentDetails.getFreeIpaCreation()).thenReturn(freeIpaCreationDto);

        UsageProto.CDPFreeIPADetails freeIPADetails = underTest.convert(environmentDetails);

        Assert.assertEquals(false, freeIPADetails.getSpot());
        Assert.assertEquals(0, freeIPADetails.getNodes());
    }

    @Test
    public void testFreeIPAHA() {
        when(freeIpaCreationDto.getInstanceCountByGroup()).thenReturn(3);
        when(environmentDetails.getFreeIpaCreation()).thenReturn(freeIpaCreationDto);

        UsageProto.CDPFreeIPADetails freeIPADetails = underTest.convert(environmentDetails);

        Assert.assertEquals(false, freeIPADetails.getSpot());
        Assert.assertEquals(3, freeIPADetails.getNodes());
    }

    @Test
    public void testFreeIPASpot() {
        when(spot.getPercentage()).thenReturn(1);
        when(aws.getSpot()).thenReturn(spot);
        when(freeIpaCreationDto.getAws()).thenReturn(aws);
        when(freeIpaCreationDto.getInstanceCountByGroup()).thenReturn(1);
        when(environmentDetails.getFreeIpaCreation()).thenReturn(freeIpaCreationDto);

        UsageProto.CDPFreeIPADetails freeIPADetails = underTest.convert(environmentDetails);

        Assert.assertEquals(true, freeIPADetails.getSpot());
        Assert.assertEquals(1, freeIPADetails.getNodes());
    }
}