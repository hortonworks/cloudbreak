package com.sequenceiq.cloudbreak.converter.v4.kuberneteses;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.kubernetes.requests.KubernetesV4Request;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.KubernetesConfig;

@Component
public class KubernetesV4RequestToKubernetesConfigConverter extends AbstractConversionServiceAwareConverter<KubernetesV4Request, KubernetesConfig> {

    @Override
    public KubernetesConfig convert(KubernetesV4Request source) {
        KubernetesConfig kubernetesConfig = new KubernetesConfig();
        kubernetesConfig.setName(source.getName());
        kubernetesConfig.setDescription(source.getDescription());
        kubernetesConfig.setConfiguration(source.getContent());
        return kubernetesConfig;
    }
}