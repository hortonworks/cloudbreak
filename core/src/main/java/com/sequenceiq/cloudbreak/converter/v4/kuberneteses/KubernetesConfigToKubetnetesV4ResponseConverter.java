package com.sequenceiq.cloudbreak.converter.v4.kuberneteses;

import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.cloudbreak.api.model.SecretResponse;
import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResourceResponse;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.domain.view.CompactView;

@Component
public class KubernetesConfigToKubetnetesV4ResponseConverter extends AbstractConversionServiceAwareConverter<KubernetesConfig, KubernetesV4Response> {

    @Inject
    private ConversionService conversionService;

    @Override
    public KubernetesV4Response convert(KubernetesConfig source) {
        KubernetesV4Response json = new KubernetesV4Response();
        json.setId(source.getId());
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setContent(conversionService.convert(source.getConfigurationSecret(), SecretResponse.class));
        json.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceResponse.class));
        json.setEnvironments(source.getEnvironments().stream().map(CompactView::getName).collect(Collectors.toSet()));
        return json;
    }
}
