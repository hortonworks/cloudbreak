package com.sequenceiq.cloudbreak.converter;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.KubernetesConfigResponse;
import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class KubernetesConfigToKubetnetesConfigResponseConverter extends AbstractConversionServiceAwareConverter<KubernetesConfig, KubernetesConfigResponse> {

    @Inject
    private ConversionService conversionService;

    @Override
    public KubernetesConfigResponse convert(KubernetesConfig source) {
        KubernetesConfigResponse json = new KubernetesConfigResponse();
        json.setId(source.getId());
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setConfig(conversionService.convert(source.getConfigurationSecret(), SecretResponse.class));
        json.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        json.setEnvironments(source.getEnvironments().stream().map(CompactView::getName).collect(Collectors.toSet()));
        return json;
    }
}
