package com.sequenceiq.cloudbreak.rotation.context.provider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.collect.Maps;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.secret.GenericPasswordSecretGenerator;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.secret.SecretGenerator;
import com.sequenceiq.cloudbreak.rotation.secret.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.secret.SecretType;
import com.sequenceiq.cloudbreak.rotation.secret.type.CloudbreakSecretType;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.secret.service.rotation.context.VaultRotationContext;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.ClusterView;

@Component
public class CMDBPasswordRotationContextProvider implements RotationContextProvider {

    private static final String DATETIMEFORMAT = "ddMMyyHHmm";

    @Value("${cb.clouderamanager.service.database.user:clouderamanager}")
    private String defaultUserName;

    @Inject
    private StackDtoService stackService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resource) {
        Map<SecretRotationStep, RotationContext> result = Maps.newHashMap();
        StackDto stack = stackService.getByCrn(resource);
        ClusterView cluster = stack.getCluster();

        Set<RDSConfig> rdsConfigs = rdsConfigService.findByClusterId(cluster.getId())
                .stream()
                .filter(rdsConfig -> DatabaseType.CLOUDERA_MANAGER.name().equals(rdsConfig.getType()))
                .collect(Collectors.toSet());

        String newUser = defaultUserName + new SimpleDateFormat(DATETIMEFORMAT).format(new Date());

        Map<String, Class<? extends SecretGenerator>> secretMap = rdsConfigs.stream()
                .collect(Collectors.toMap(RDSConfig::getConnectionUserNameSecret, rdsConfig -> GenericPasswordSecretGenerator.class));
        secretMap.putAll(rdsConfigs.stream()
                .collect(Collectors.toMap(RDSConfig::getConnectionPasswordSecret, rdsConfig -> GenericPasswordSecretGenerator.class)));

        VaultRotationContext rotationContext = VaultRotationContext.builder()
                .withSecretUpdateSupplierMap(secretMap)
                .withResourceCrn(stack.getResourceCrn())
                .build();
        result.put(SecretRotationStep.VAULT, rotationContext);
        return result;
    }

    @Override
    public SecretType getSecret() {
        return CloudbreakSecretType.CM_DB_PASSWORD;
    }
}