package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.BlueprintV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.BlueprintRequest;
import com.sequenceiq.cloudbreak.api.model.BlueprintResponse;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Controller
@Transactional(TxType.NEVER)
public class BlueprintV3Controller extends NotificationController implements BlueprintV3Endpoint {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private UserService userService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private OrganizationService organizationService;

    @Override
    public Set<BlueprintResponse> listByOrganization(Long organizationId) {
        IdentityUser identityUser = restRequestThreadLocalService.getIdentityUser();
        User user = userService.getOrCreate(identityUser);
        Organization organization = organizationService.get(organizationId, user);
        return blueprintService.getAllAvailableInOrganization(organization).stream()
                .map(blueprint -> conversionService.convert(blueprint, BlueprintResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public BlueprintResponse getByNameInOrganization(Long organizationId, String name) {
        Blueprint blueprint = blueprintService.getByNameForOrganizationId(name, organizationId);
        return conversionService.convert(blueprint, BlueprintResponse.class);
    }

    @Override
    public BlueprintResponse createInOrganization(Long organizationId, BlueprintRequest request) {
        Blueprint blueprint = conversionService.convert(request, Blueprint.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getIdentityUser());
        blueprint = blueprintService.create(blueprint, organizationId, user);
        notify(ResourceEvent.BLUEPRINT_CREATED);
        return conversionService.convert(blueprint, BlueprintResponse.class);
    }

    @Override
    public BlueprintResponse deleteInOrganization(Long organizationId, String name) {
        Blueprint deleted = blueprintService.deleteByNameFromOrganization(name, organizationId);
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return conversionService.convert(deleted, BlueprintResponse.class);
    }

    @Override
    public BlueprintRequest getRequestFromName(Long organizationId, String name) {
        Blueprint blueprint = blueprintService.getByNameForOrganizationId(name, organizationId);
        return conversionService.convert(blueprint, BlueprintRequest.class);
    }
}
