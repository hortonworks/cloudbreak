package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.EnvironmentNames;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.KubernetesV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.requests.KubernetesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Responses;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.service.KubernetesConfigService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(KubernetesConfig.class)
public class KubernetesV4Controller extends NotificationController implements KubernetesV4Endpoint {

    @Inject
    private KubernetesConfigService kubernetesConfigService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    public KubernetesV4Responses list(Long workspaceId, String environment, Boolean attachGlobal) {
        Set<KubernetesConfig> allInWorkspaceAndEnvironment = kubernetesConfigService
                .findAllInWorkspaceAndEnvironment(workspaceId, environment, attachGlobal);
        return new KubernetesV4Responses(converterUtil.convertAllAsSet(allInWorkspaceAndEnvironment, KubernetesV4Response.class));
    }

    @Override
    public KubernetesV4Response get(Long workspaceId, String name) {
        KubernetesConfig kubernetesConfig = kubernetesConfigService.getByNameForWorkspaceId(name, workspaceId);
        return converterUtil.convert(kubernetesConfig, KubernetesV4Response.class);
    }

    @Override
    public KubernetesV4Response post(Long workspaceId, KubernetesV4Request request) {
        KubernetesConfig kubernetesConfig = converterUtil.convert(request, KubernetesConfig.class);
        kubernetesConfig = kubernetesConfigService.createInEnvironment(kubernetesConfig, request.getEnvironments(), workspaceId);
        notify(ResourceEvent.KUBERNETES_CONFIG_CREATED);
        return converterUtil.convert(kubernetesConfig, KubernetesV4Response.class);
    }

    @Override
    public KubernetesV4Response put(Long workspaceId, KubernetesV4Request request) {
        KubernetesConfig kubernetesConfig = converterUtil.convert(request, KubernetesConfig.class);
        kubernetesConfig = kubernetesConfigService.updateByWorkspaceId(workspaceId, kubernetesConfig);
        notify(ResourceEvent.KUBERNETES_CONFIG_MODIFIED);
        return converterUtil.convert(kubernetesConfig, KubernetesV4Response.class);
    }

    @Override
    public KubernetesV4Response delete(Long workspaceId, String name) {
        KubernetesConfig deleted = kubernetesConfigService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.KUBERNETES_CONFIG_DELETED);
        return converterUtil.convert(deleted, KubernetesV4Response.class);
    }

    @Override
    public KubernetesV4Responses deleteMultiple(Long workspaceId, Set<String> names) {
        Set<KubernetesConfig> deleted = kubernetesConfigService.deleteMultipleByNameFromWorkspace(names, workspaceId);
        notify(ResourceEvent.KUBERNETES_CONFIG_DELETED);
        return new KubernetesV4Responses(converterUtil.convertAllAsSet(deleted, KubernetesV4Response.class));
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
