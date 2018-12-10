package com.sequenceiq.cloudbreak.converter;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.KubernetesConfigRequest;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;

@Component
public class KubernetesConfigRequestToKubernetesConfigConverter extends AbstractConversionServiceAwareConverter<KubernetesConfigRequest, KubernetesConfig> {

    @Override
    public KubernetesConfig convert(KubernetesConfigRequest source) {
        KubernetesConfig kubernetesConfig = new KubernetesConfig();
        kubernetesConfig.setName(source.getName());
        kubernetesConfig.setDescription(source.getDescription());
        kubernetesConfig.setConfiguration(source.getConfig());
        return kubernetesConfig;
    }
}