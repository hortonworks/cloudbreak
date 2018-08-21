package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

@Component
public class PlatformResourceRequestJsonToPlatformResourceRequest extends
        AbstractConversionServiceAwareConverter<PlatformResourceRequestJson, PlatformResourceRequest> {

    @Inject
    private CredentialService credentialService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public PlatformResourceRequest convert(PlatformResourceRequestJson source) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();
        if (!Strings.isNullOrEmpty(source.getCredentialName())) {
            Long orgId = restRequestThreadLocalService.getRequestedOrgId();
            if (orgId == null) {
                orgId = organizationService.getDefaultOrganizationForCurrentUser().getId();
            }
            platformResourceRequest.setCredential(credentialService.getByNameForOrganizationId(source.getCredentialName(), orgId));
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
        platformResourceRequest.setCloudPlatform(platformResourceRequest.getCredential().cloudPlatform());
        if (!Strings.isNullOrEmpty(source.getAvailabilityZone())) {
            platformResourceRequest.setAvailabilityZone(source.getAvailabilityZone());
        }
        return platformResourceRequest;
    }
}
