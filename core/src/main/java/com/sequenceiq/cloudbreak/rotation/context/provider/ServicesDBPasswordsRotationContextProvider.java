package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretLocationType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;

// TODO WIP
@Component
public class ServicesDBPasswordsRotationContextProvider implements RotationContextProvider {

    @Inject
    private StackDtoService stackService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Override
    public Map<SecretLocationType, RotationContext> getContexts(String resource) {
        Map<SecretLocationType, RotationContext> result = Maps.newHashMap();
        StackDto stack = stackService.getByCrn(resource);
        ClusterView cluster = stack.getCluster();

        Set<RDSConfig> rdsConfigs = rdsConfigService.findByClusterId(cluster.getId());

        Map<String, String> userPasswordSecretPairs = rdsConfigs.stream()
                .collect(Collectors.toMap(RDSConfig::getConnectionUserNameSecret, RDSConfig::getConnectionPasswordSecret));

        RotationContext rotationContext = RotationContext.contextBuilder()
                .withUserPasswordSecrets(userPasswordSecretPairs)
                .build();
        result.put(SecretLocationType.VAULT, rotationContext);
        return result;
    }

    @Override
    public SecretType getSecret() {
        return SecretType.SERVICES_DB_PASSWORDS;
    }
}
