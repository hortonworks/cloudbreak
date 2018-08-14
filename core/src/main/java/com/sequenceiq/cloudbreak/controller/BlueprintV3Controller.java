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
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.service.blueprint.BlueprintService;

@Controller
@Transactional(TxType.NEVER)
public class BlueprintV3Controller extends NotificationController implements BlueprintV3Endpoint {

    @Inject
    private BlueprintService blueprintService;

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Override
    public Set<BlueprintResponse> listByOrganization(Long organizationId) {
        return blueprintService.findAllByOrganizationId(organizationId).stream()
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
        blueprint = blueprintService.create(blueprint, organizationId);
        notify(ResourceEvent.BLUEPRINT_CREATED);
        return conversionService.convert(blueprint, BlueprintResponse.class);
    }

    @Override
    public BlueprintResponse deleteInOrganization(Long organizationId, String name) {
        Blueprint deleted = blueprintService.deleteByNameFromOrganization(name, organizationId);
        notify(ResourceEvent.BLUEPRINT_DELETED);
        return conversionService.convert(deleted, BlueprintResponse.class);
    }
}
