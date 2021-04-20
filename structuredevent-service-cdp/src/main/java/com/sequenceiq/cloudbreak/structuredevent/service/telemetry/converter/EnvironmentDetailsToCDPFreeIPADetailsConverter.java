package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;

@Component
public class EnvironmentDetailsToCDPFreeIPADetailsConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDetailsToCDPFreeIPADetailsConverter.class);

    private static final int DEFAULT_INTEGER_VALUE = -1;

    public UsageProto.CDPFreeIPADetails convert(EnvironmentDetails environmentDetails) {
        UsageProto.CDPFreeIPADetails.Builder cdpFreeIPADetails = UsageProto.CDPFreeIPADetails.newBuilder();
        cdpFreeIPADetails.setNodes(DEFAULT_INTEGER_VALUE);

        if (environmentDetails != null) {
            FreeIpaCreationDto freeIpaCreationDto = environmentDetails.getFreeIpaCreation();

            if (freeIpaCreationDto != null) {
                cdpFreeIPADetails.setNodes(freeIpaCreationDto.getInstanceCountByGroup());

                FreeIpaCreationAwsParametersDto aws = freeIpaCreationDto.getAws();

                if (aws != null) {
                    FreeIpaCreationAwsSpotParametersDto spot = aws.getSpot();
                    if (spot != null && spot.getPercentage() != null) {
                        cdpFreeIPADetails.setSpot(spot.getPercentage() > 0);
                    }
                }
            }
        }

        UsageProto.CDPFreeIPADetails ret = cdpFreeIPADetails.build();
        LOGGER.debug("Converted CDPFreeIPADetails: {}", ret);
        return ret;
    }
}
