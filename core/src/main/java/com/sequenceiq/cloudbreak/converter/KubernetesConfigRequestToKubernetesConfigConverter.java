package com.sequenceiq.cloudbreak.converter;

import org.apache.commons.codec.binary.Base64;
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
        kubernetesConfig.setConfiguration(new String(Base64.decodeBase64(source.getConfig())));
        return kubernetesConfig;
    }
}