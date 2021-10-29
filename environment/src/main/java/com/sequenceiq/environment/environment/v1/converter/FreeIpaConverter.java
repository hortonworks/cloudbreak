package com.sequenceiq.environment.environment.v1.converter;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.FreeIpaImageRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaSpotParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.FreeIpaImageResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.FreeIpaResponse;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;

@Component
public class FreeIpaConverter {

    @Inject
    private EntitlementService entitlementService;

    public FreeIpaResponse convert(FreeIpaCreationDto freeIpaCreation) {
        if (freeIpaCreation == null) {
            return null;
        } else {
            FreeIpaResponse response = new FreeIpaResponse();
            response.setInstanceCountByGroup(freeIpaCreation.getInstanceCountByGroup());
            Optional.ofNullable(freeIpaCreation.getAws())
                    .map(this::convertAws)
                    .ifPresent(response::setAws);
            response.setImage(convertImage(freeIpaCreation.getImageCatalog(), freeIpaCreation.getImageId()));
            response.setEnableMultiAz(freeIpaCreation.isEnableMultiAz());
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
        result.setMaxPrice(spot.getMaxPrice());
        return result;
    }

    private FreeIpaImageResponse convertImage(String catalog, String id) {
        FreeIpaImageResponse result = null;
        if (!Strings.isNullOrEmpty(catalog) && !Strings.isNullOrEmpty(id)) {
            result = new FreeIpaImageResponse();
            result.setCatalog(catalog);
            result.setId(id);
        }
        return result;
    }

    public FreeIpaCreationDto convert(AttachedFreeIpaRequest request, String accountId, String cloudPlatform) {
        FreeIpaCreationDto.Builder builder = FreeIpaCreationDto.builder();
        if (request != null) {
            builder.withCreate(request.getCreate());
            builder.withEnableMultiAz(request.isEnableMultiAz());
            if (request.isEnableMultiAz()) {
                if (!cloudPlatform.equalsIgnoreCase(CloudConstants.AWS)) {
                    throw new BadRequestException("You need to provision AWS environment to use Multi Availability Zone.");
                }
                if (!entitlementService.awsNativeFreeIpaEnabled(accountId)) {
                    throw new BadRequestException("You need to be entitled for CDP_CB_AWS_NATIVE_FREEIPA to provision FreeIPA in Multi Availability Zone.");
                }
            }
            Optional.ofNullable(request.getInstanceCountByGroup())
                    .ifPresent(builder::withInstanceCountByGroup);
            Optional.ofNullable(request.getAws())
                    .map(AwsFreeIpaParameters::getSpot)
                    .ifPresent(spotParameters -> builder.withAws(FreeIpaCreationAwsParametersDto.builder()
                            .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                                    .withPercentage(spotParameters.getPercentage())
                                    .withMaxPrice(spotParameters.getMaxPrice())
                                    .build())
                            .build()));
            FreeIpaImageRequest image = request.getImage();
            if (image != null) {
                builder.withImageCatalog(image.getCatalog());
                builder.withImageId(image.getId());
            }
        }
        return builder.build();
    }
}
