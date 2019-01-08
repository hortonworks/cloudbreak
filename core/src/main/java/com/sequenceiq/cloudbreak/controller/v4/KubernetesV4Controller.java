package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Responses.kubernetesV4Responses;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.EnvironmentNames;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.filter.ListV4Filter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.KubernetesV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.requests.KubernetesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Responses;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.controller.NotificationController;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.KubernetesConfigService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(KubernetesConfig.class)
public class KubernetesV4Controller extends NotificationController implements KubernetesV4Endpoint {

    @Inject
    @Named("conversionService")
    private ConversionService conversionService;

    @Inject
    private KubernetesConfigService kubernetesConfigService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private UserService userService;

    @Override
    public KubernetesV4Responses list(Long workspaceId, ListV4Filter listV4Filter) {
        Set<KubernetesV4Response> kubernetesV4Responses = kubernetesConfigService
                .findAllInWorkspaceAndEnvironment(workspaceId, listV4Filter.getEnvironment(), listV4Filter.getAttachGlobal())
                .stream()
                .map(kubernetesConfig -> conversionService.convert(kubernetesConfig, KubernetesV4Response.class))
                .collect(Collectors.toSet());
        return kubernetesV4Responses(kubernetesV4Responses);
    }

    @Override
    public KubernetesV4Response get(Long workspaceId, String name) {
        KubernetesConfig kubernetesConfig = kubernetesConfigService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(kubernetesConfig, KubernetesV4Response.class);
    }

    @Override
    public KubernetesV4Response post(Long workspaceId, KubernetesV4Request request) {
        KubernetesConfig kubernetesConfig = conversionService.convert(request, KubernetesConfig.class);
        kubernetesConfig = kubernetesConfigService.createInEnvironment(kubernetesConfig, request.getEnvironments(), workspaceId);
        notify(ResourceEvent.KUBERNETES_CONFIG_CREATED);
        return conversionService.convert(kubernetesConfig, KubernetesV4Response.class);
    }

    @Override
    public KubernetesV4Response put(Long workspaceId, KubernetesV4Request request) {
        KubernetesConfig kubernetesConfig = conversionService.convert(request, KubernetesConfig.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        kubernetesConfig = kubernetesConfigService.updateByWorkspaceId(workspaceId, kubernetesConfig, user);
        notify(ResourceEvent.KUBERNETES_CONFIG_MODIFIED);
        return conversionService.convert(kubernetesConfig, KubernetesV4Response.class);
    }

    @Override
    public KubernetesV4Response delete(Long workspaceId, String name) {
        KubernetesConfig deleted = kubernetesConfigService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.KUBERNETES_CONFIG_DELETED);
        return conversionService.convert(deleted, KubernetesV4Response.class);
    }

    @Override
    public KubernetesV4Response attach(Long workspaceId, String name, EnvironmentNames environmentNames) {
        return kubernetesConfigService.attachToEnvironmentsAndConvert(name, environmentNames.getEnvironmentNames(), workspaceId, KubernetesV4Response.class);
    }

    @Override
    public KubernetesV4Response detach(Long workspaceId, String name, EnvironmentNames environmentNames) {
        return kubernetesConfigService.detachFromEnvironmentsAndConvert(name, environmentNames.getEnvironmentNames(), workspaceId, KubernetesV4Response.class);
    }
}