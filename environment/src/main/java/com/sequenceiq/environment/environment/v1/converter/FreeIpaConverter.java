package com.sequenceiq.environment.environment.v1.converter;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.altus.model.Entitlement;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.FreeIpaImageRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaSpotParameters;
import com.sequenceiq.environment.api.v1.environment.model.response.FreeIpaImageResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.FreeIpaResponse;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto.Builder;
import com.sequenceiq.environment.environment.flow.creation.handler.freeipa.MultiAzValidator;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaInstanceCountByGroupProvider;

@Component
public class FreeIpaConverter {

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private FreeIpaInstanceCountByGroupProvider ipaInstanceCountByGroupProvider;

    @Inject
    private MultiAzValidator multiAzValidator;

    public FreeIpaResponse convert(FreeIpaCreationDto freeIpaCreation) {
        if (freeIpaCreation == null) {
            return null;
        } else {
            FreeIpaResponse response = new FreeIpaResponse();
            response.setInstanceCountByGroup(freeIpaCreation.getInstanceCountByGroup());
            Optional.ofNullable(freeIpaCreation.getAws())
                    .map(this::convertAws)
                    .ifPresent(response::setAws);
            response.setImage(convertImage(freeIpaCreation.getImageCatalog(), freeIpaCreation.getImageId(), freeIpaCreation.getImageOs()));
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

    private FreeIpaImageResponse convertImage(String catalog, String id, String os) {
        FreeIpaImageResponse result = null;
        if ((!Strings.isNullOrEmpty(catalog) && !Strings.isNullOrEmpty(id)) || !Strings.isNullOrEmpty(os)) {
            result = new FreeIpaImageResponse();
            result.setCatalog(catalog);
            result.setId(id);
            result.setOs(os);
        }
        return result;
    }

    public FreeIpaCreationDto convert(AttachedFreeIpaRequest request, String accountId, String cloudPlatform) {
        Builder builder = FreeIpaCreationDto.builder(ipaInstanceCountByGroupProvider.getInstanceCount(request));
        if (request != null) {
            builder.withCreate(request.getCreate());
            builder.withEnableMultiAz(request.isEnableMultiAz());
            if (request.isEnableMultiAz()) {
                if (!multiAzValidator.suportedMultiAzForEnvironment(cloudPlatform)) {
                    throw new BadRequestException(String.format("Multi Availability Zone is not supported for %s",
                            CloudPlatform.valueOf(cloudPlatform).getDislayName()));
                }
                List<String> entitlements = entitlementService.getEntitlements(accountId);
                Set<Entitlement> requiredEntitlements = multiAzValidator.getMultiAzEntitlements(CloudPlatform.valueOf(cloudPlatform));
                List<String> missingEntitlements = requiredEntitlements.stream()
                        .filter(entitlement -> !entitlements.contains(entitlement.name()))
                        .map(entitlement -> entitlement.name())
                        .collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(missingEntitlements)) {
                    throw new BadRequestException(String.format("You need to be entitled for %s to provision FreeIPA in Multi Availability Zone",
                            StringUtils.join(missingEntitlements, ",")));
                }
            }
            Optional.ofNullable(request.getInstanceType())
                    .ifPresent(builder::withInstanceType);
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
                builder.withImageOs(image.getOs());
            }
            Set<String> recipes = request.getRecipes();
            if (recipes != null) {
                builder.withRecipes(recipes);
            }
        }
        return builder.build();
    }
}
