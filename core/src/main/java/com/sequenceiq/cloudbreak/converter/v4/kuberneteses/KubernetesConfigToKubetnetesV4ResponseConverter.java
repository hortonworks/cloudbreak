package com.sequenceiq.cloudbreak.converter.v4.kuberneteses;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.responses.KubernetesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.workspace.responses.WorkspaceResourceV4Response;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;

@Component
public class KubernetesConfigToKubetnetesV4ResponseConverter extends AbstractConversionServiceAwareConverter<KubernetesConfig, KubernetesV4Response> {

    @Override
    public KubernetesV4Response convert(KubernetesConfig source) {
        KubernetesV4Response json = new KubernetesV4Response();
        json.setId(source.getId());
        json.setName(source.getName());
        json.setDescription(source.getDescription());
        json.setContent(getConversionService().convert(source.getConfigurationSecret(), SecretResponse.class));
        json.setWorkspace(getConversionService().convert(source.getWorkspace(), WorkspaceResourceV4Response.class));
        return json;
    }
}
