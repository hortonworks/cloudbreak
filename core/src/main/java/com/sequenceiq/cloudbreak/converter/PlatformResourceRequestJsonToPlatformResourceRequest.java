package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;

@Component
public class PlatformResourceRequestJsonToPlatformResourceRequest extends
        AbstractConversionServiceAwareConverter<PlatformResourceRequestJson, PlatformResourceRequest> {

    @Inject
    private CredentialService credentialService;

    @Override
    public PlatformResourceRequest convert(PlatformResourceRequestJson source) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        if (!Strings.isNullOrEmpty(source.getCredentialName())) {
            platformResourceRequest.setCredential(credentialService.get(source.getCredentialName(), source.getAccount()));
        } else if (source.getCredentialId() != null) {
            platformResourceRequest.setCredential(credentialService.get(source.getCredentialId()));
        } else {
            throw new BadRequestException("The credentialId or the credentialName must be specified in the request");
        }
        if (!Strings.isNullOrEmpty(source.getPlatformVariant())) {
            platformResourceRequest.setCloudPlatform(platformResourceRequest.getCredential().cloudPlatform());
        } else {
            platformResourceRequest.setPlatformVariant(
                    Strings.isNullOrEmpty(source.getPlatformVariant()) ? platformResourceRequest.getCredential().cloudPlatform() : source.getPlatformVariant());
        }
        platformResourceRequest.setFilters(source.getFilters());
        platformResourceRequest.setRegion(source.getRegion());
        return platformResourceRequest;
    }
}
