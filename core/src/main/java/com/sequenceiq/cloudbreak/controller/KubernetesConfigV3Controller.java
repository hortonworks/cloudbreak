package com.sequenceiq.cloudbreak.controller;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.constraints.NotEmpty;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.KubernetesConfigV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.KubernetesConfigRequest;
import com.sequenceiq.cloudbreak.api.model.KubernetesConfigResponse;
import com.sequenceiq.cloudbreak.common.type.ResourceEvent;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.domain.workspace.User;
import com.sequenceiq.cloudbreak.service.KubernetesConfigService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.util.WorkspaceEntityType;

@Controller
@Transactional(TxType.NEVER)
@WorkspaceEntityType(KubernetesConfig.class)
public class KubernetesConfigV3Controller extends NotificationController implements KubernetesConfigV3Endpoint {

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
    public Set<KubernetesConfigResponse> listByWorkspace(Long workspaceId, String environment, Boolean attachGlobal) {
        return kubernetesConfigService.findAllInWorkspaceAndEnvironment(workspaceId, environment, attachGlobal).stream()
                .map(kubernetesConfig -> conversionService.convert(kubernetesConfig, KubernetesConfigResponse.class))
                .collect(Collectors.toSet());
    }

    @Override
    public KubernetesConfigResponse getByNameInWorkspace(Long workspaceId, String name) {
        KubernetesConfig kubernetesConfig = kubernetesConfigService.getByNameForWorkspaceId(name, workspaceId);
        return conversionService.convert(kubernetesConfig, KubernetesConfigResponse.class);
    }

    @Override
    public KubernetesConfigResponse createInWorkspace(Long workspaceId, KubernetesConfigRequest request) {
        KubernetesConfig kubernetesConfig = conversionService.convert(request, KubernetesConfig.class);
        kubernetesConfig = kubernetesConfigService.createInEnvironment(kubernetesConfig, request.getEnvironments(), workspaceId);
        notify(ResourceEvent.KUBERNETES_CONFIG_CREATED);
        return conversionService.convert(kubernetesConfig, KubernetesConfigResponse.class);
    }

    @Override
    public KubernetesConfigResponse putInWorkspace(Long workspaceId, KubernetesConfigRequest request) {
        KubernetesConfig kubernetesConfig = conversionService.convert(request, KubernetesConfig.class);
        User user = userService.getOrCreate(restRequestThreadLocalService.getCloudbreakUser());
        kubernetesConfig = kubernetesConfigService.updateByWorkspaceId(workspaceId, kubernetesConfig, user);
        notify(ResourceEvent.KUBERNETES_CONFIG_MODIFIED);
        return conversionService.convert(kubernetesConfig, KubernetesConfigResponse.class);
    }

    @Override
    public KubernetesConfigResponse deleteInWorkspace(Long workspaceId, String name) {
        KubernetesConfig deleted = kubernetesConfigService.deleteByNameFromWorkspace(name, workspaceId);
        notify(ResourceEvent.KUBERNETES_CONFIG_DELETED);
        return conversionService.convert(deleted, KubernetesConfigResponse.class);
    }

    @Override
    public KubernetesConfigResponse attachToEnvironments(Long workspaceId, String name, @NotEmpty Set<String> environmentNames) {
        return kubernetesConfigService.attachToEnvironmentsAndConvert(name, environmentNames, workspaceId, KubernetesConfigResponse.class);
    }

    @Override
    public KubernetesConfigResponse detachFromEnvironments(Long workspaceId, String name, @NotEmpty Set<String> environmentNames) {
        return kubernetesConfigService.detachFromEnvironmentsAndConvert(name, environmentNames, workspaceId, KubernetesConfigResponse.class);
    }
}