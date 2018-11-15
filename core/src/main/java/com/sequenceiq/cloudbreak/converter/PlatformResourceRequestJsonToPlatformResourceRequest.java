package com.sequenceiq.cloudbreak.converter;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.model.PlatformResourceRequestJson;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.PlatformResourceRequest;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;

@Component
public class PlatformResourceRequestJsonToPlatformResourceRequest extends
        AbstractConversionServiceAwareConverter<PlatformResourceRequestJson, PlatformResourceRequest> {

    @Inject
    private CredentialService credentialService;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public PlatformResourceRequest convert(PlatformResourceRequestJson source) {
        PlatformResourceRequest platformResourceRequest = new PlatformResourceRequest();

        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        Workspace workspace = workspaceService.get(restRequestThreadLocalService.getRequestedWorkspaceId(), user);
        if (!Strings.isNullOrEmpty(source.getCredentialName())) {
            platformResourceRequest.setCredential(credentialService.getByNameForWorkspaceId(source.getCredentialName(), workspace.getId()));
        } else if (source.getCredentialId() != null) {
            platformResourceRequest.setCredential(credentialService.get(source.getCredentialId(), workspace));
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
