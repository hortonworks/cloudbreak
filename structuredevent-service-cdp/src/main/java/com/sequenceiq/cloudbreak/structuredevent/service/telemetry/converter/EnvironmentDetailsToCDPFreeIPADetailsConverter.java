package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPFreeIPADetails;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.environment.EnvironmentDetails;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;

@Component
public class EnvironmentDetailsToCDPFreeIPADetailsConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDetailsToCDPFreeIPADetailsConverter.class);

    private static final int DEFAULT_INTEGER_VALUE = -1;

    public CDPFreeIPADetails convert(EnvironmentDetails environmentDetails) {
        CDPFreeIPADetails.Builder cdpFreeIPADetails = CDPFreeIPADetails.newBuilder();
        cdpFreeIPADetails.setNodes(DEFAULT_INTEGER_VALUE);

        if (environmentDetails != null) {
            FreeIpaCreationDto freeIpaCreationDto = environmentDetails.getFreeIpaCreation();

            if (freeIpaCreationDto != null) {
                cdpFreeIPADetails.setNodes(freeIpaCreationDto.getInstanceCountByGroup());
                cdpFreeIPADetails.setMultiAz(freeIpaCreationDto.isEnableMultiAz());
                cdpFreeIPADetails.setSeLinux(freeIpaCreationDto.getSeLinux() == null ?
                        SeLinux.PERMISSIVE.name() : freeIpaCreationDto.getSeLinux().name());

                FreeIpaCreationAwsParametersDto aws = freeIpaCreationDto.getAws();

                if (aws != null) {
                    FreeIpaCreationAwsSpotParametersDto spot = aws.getSpot();
                    if (spot != null && spot.getPercentage() != null) {
                        cdpFreeIPADetails.setSpot(spot.getPercentage() > 0);
                    }
                }
            }
        }

        CDPFreeIPADetails ret = cdpFreeIPADetails.build();
        LOGGER.debug("Converted CDPFreeIPADetails: {}", ret);
        return ret;
    }
}
