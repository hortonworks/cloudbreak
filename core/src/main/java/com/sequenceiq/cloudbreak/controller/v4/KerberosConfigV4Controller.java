package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.validation.Valid;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.EnvironmentNames;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.filter.ListV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.responses.GeneralSetV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.KerberosConfigV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.requests.KerberosV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kerberos.responses.KerberosViewV4Response;
import com.sequenceiq.cloudbreak.controller.common.NotificationController;
import com.sequenceiq.cloudbreak.domain.KerberosConfig;
import com.sequenceiq.cloudbreak.service.kerberos.KerberosService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(Transactional.TxType.NEVER)
@WorkspaceEntityType(KerberosConfig.class)
public class KerberosConfigV4Controller extends NotificationController implements KerberosConfigV4Endpoint {

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private KerberosService kerberosService;

    @Override
    public GeneralSetV4Response<KerberosViewV4Response> list(Long workspaceId, ListV4Filter listV4Filter) {
        Set<KerberosViewV4Response> kerberosV4ViewRespons = kerberosService.findAllInWorkspaceAndEnvironment(workspaceId,
                    listV4Filter.getEnvironment(), listV4Filter.getAttachGlobal()).stream()
                .map(kerberosConfig -> conversionService.convert(kerberosConfig, KerberosViewV4Response.class))
                .collect(Collectors.toSet());
        return GeneralSetV4Response.propagateResponses(kerberosV4ViewRespons);
    }

    @Override
    public KerberosV4Response get(Long workspaceId, String name) {
        KerberosConfig kerberosConfig = kerberosService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(kerberosConfig, KerberosV4Response.class);
    }

    @Override
    public KerberosV4Response create(Long workspaceId, @Valid KerberosV4Request request) {
        KerberosConfig newKerberosConfig = conversionService.convert(request, KerberosConfig.class);
        KerberosConfig createdKerberosConfig = kerberosService.createInEnvironment(newKerberosConfig, request.getEnvironments(), workspaceId);
        return conversionService.convert(createdKerberosConfig, KerberosV4Response.class);
    }

    @Override
    public KerberosV4Response delete(Long workspaceId, String name) {
        KerberosConfig deleted = kerberosService.deleteByNameFromWorkspace(name, workspaceId);
        return conversionService.convert(deleted, KerberosV4Response.class);
    }

    @Override
    public KerberosV4Response attach(Long workspaceId, String name, EnvironmentNames environmentNames) {
        KerberosConfig attached = kerberosService.attachToEnvironments(name, environmentNames.getEnvironmentNames(), workspaceId);
        return conversionService.convert(attached, KerberosV4Response.class);
    }

    @Override
    public KerberosV4Response detach(Long workspaceId, String name, EnvironmentNames environmentNames) {
        KerberosConfig detached = kerberosService.detachFromEnvironments(name, environmentNames.getEnvironmentNames(), workspaceId);
        return conversionService.convert(detached, KerberosV4Response.class);
    }

    @Override
    public KerberosV4Request getRequest(Long workspaceId, String name) {
        KerberosConfig kerberosConfig = kerberosService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(kerberosConfig, KerberosV4Request.class);
    }
}