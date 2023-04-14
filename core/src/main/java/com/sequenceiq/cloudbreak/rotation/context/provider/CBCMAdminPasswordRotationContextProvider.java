package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretLocationType;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Component
public class CBCMAdminPasswordRotationContextProvider implements RotationContextProvider {

    @Inject
    private StackDtoService stackService;

    @Override
    public Map<SecretLocationType, RotationContext> getContexts(String resource) {
        Map<SecretLocationType, RotationContext> result = Maps.newHashMap();
        StackDto stack = stackService.getByCrn(resource);
        ClusterView cluster = stack.getCluster();

        RotationContext rotationContext = RotationContext.contextBuilder()
                .withUserPasswordSecrets(Map.of(
                        cluster.getCloudbreakAmbariUserSecret().getSecret(),
                        cluster.getCloudbreakAmbariPasswordSecret().getSecret()))
                .withClientUserSecretSupplier(() -> cluster.getDpClusterManagerUserSecret().getSecret())
                .withClientPasswordSecretSupplier(() -> cluster.getDpClusterManagerPasswordSecret().getSecret())
                .withResourceCrn(stack.getResourceCrn())
                .build();
        result.put(SecretLocationType.VAULT, rotationContext);
        result.put(SecretLocationType.CM_USER, rotationContext);
        return result;
    }

    @Override
    public SecretType getSecret() {
        return SecretType.CLOUDBREAK_CM_ADMIN_PASSWORD;
    }
}
