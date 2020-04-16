package com.sequenceiq.environment.environment.v1.converter;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaSpotParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.FreeIpaResponse;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;

@Component
public class FreeIpaConverter {

    public FreeIpaResponse convert(FreeIpaCreationDto freeIpaCreation) {
        if (freeIpaCreation == null) {
            return null;
        } else {
            FreeIpaResponse response = new FreeIpaResponse();
            response.setInstanceCountByGroup(freeIpaCreation.getInstanceCountByGroup());
            Optional.ofNullable(freeIpaCreation.getAws())
                    .map(this::convertAws)
                    .ifPresent(response::setAws);
            return response;
        }
    }

    private AwsFreeIpaParameters convertAws(FreeIpaCreationAwsParametersDto aws) {
        AwsFreeIpaParameters result = new AwsFreeIpaParameters();
        result.setSpot(convertAwsSpot(aws.getSpot()));
        return result;
    }

    private AwsFreeIpaSpotParameters convertAwsSpot(FreeIpaCreationAwsSpotParametersDto spot) {
        AwsFreeIpaSpotParameters result = new AwsFreeIpaSpotParameters();
        result.setPercentage(spot.getPercentage());
        return result;
    }

    public FreeIpaCreationDto convert(AttachedFreeIpaRequest request) {
        FreeIpaCreationDto.Builder builder = FreeIpaCreationDto.builder();
        if (request != null) {
            builder.withCreate(request.getCreate());
            Optional.ofNullable(request.getInstanceCountByGroup())
                    .ifPresent(builder::withInstanceCountByGroup);
            Optional.ofNullable(request.getAws())
                    .map(AwsFreeIpaParameters::getSpot)
                    .map(AwsFreeIpaSpotParameters::getPercentage)
                    .ifPresent(percentage -> builder.withAws(FreeIpaCreationAwsParametersDto.builder()
                            .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                                    .withPercentage(percentage)
                                    .build())
                            .build()));
        }
        return builder.build();
    }
}
