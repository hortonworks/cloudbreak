package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.validation.Valid;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ManagementPackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackRequest;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.ManagementPack;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.mpack.ManagementPackService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Controller
public class ManagementPackV3Controller extends NotificationController implements ManagementPackV3Endpoint {

    @Inject
    private ManagementPackService mpackService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    public Set<ManagementPackResponse> listByOrganization(Long organizationId) {
        return mpackService.findAllByOrganizationId(organizationId).stream()
                .map(mpack -> conversionService.convert(mpack, ManagementPackResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public ManagementPackResponse getByNameInOrganization(Long organizationId, String name) {
        ManagementPack managementPack = mpackService.getByNameForOrganizationId(name, organizationId);
        return conversionService.convert(managementPack, ManagementPackResponse.class);
    }

    @Override
    public ManagementPackResponse createInOrganization(Long organizationId, @Valid ManagementPackRequest request) {
        ManagementPack managementPack = conversionService.convert(request, ManagementPack.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        managementPack = mpackService.create(managementPack, organizationId, user);
        notify(ResourceEvent.MANAGEMENT_PACK_CREATED);
        return conversionService.convert(managementPack, ManagementPackResponse.class);
    }

    @Override
    public ManagementPackResponse deleteInOrganization(Long organizationId, String name) {
        ManagementPack deleted = mpackService.deleteByNameFromOrganization(name, organizationId);
        notify(ResourceEvent.MANAGEMENT_PACK_DELETED);
        return conversionService.convert(deleted, ManagementPackResponse.class);
    }
}
