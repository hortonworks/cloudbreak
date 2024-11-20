package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.INTERNAL_DATALAKE_CM_SERVICE_SHARED_DB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.database.base.DatabaseType;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.rotation.service.SharedDBRotationUtils;
import com.sequenceiq.cloudbreak.service.rdsconfig.RdsConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class DatalakeCMServiceSharedDBRotationContextProvider extends CMServiceDBPasswordRotationContextProvider {

    @Inject
    private StackDtoService stackService;

    @Inject
    private RdsConfigService rdsConfigService;

    @Inject
    private SharedDBRotationUtils sharedDBRotationUtils;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceCrn) {
        Map<SecretRotationStep, RotationContext> contexts = new HashMap<>(super.getContexts(resourceCrn));
        StackDto stackDto = stackService.getByCrn(resourceCrn);
        CustomJobRotationContext customJobRotationContext = CustomJobRotationContext.builder()
                .withPreValidateJob(() -> {
                    String jdbcConnectionUrl = sharedDBRotationUtils.getJdbcConnectionUrl(stackDto.getCluster().getDatabaseServerCrn());
                    Set<RDSConfig> rdsConfigs = rdsConfigService.findAllByConnectionUrlAndTypeWithClusters(jdbcConnectionUrl);
                    boolean clusterHasOwnHmsUser = rdsConfigs.stream().map(RDSConfig::getClusters).anyMatch(clusters -> clusters.size() == 1 &&
                            Objects.equals(clusters.iterator().next().getId(), stackDto.getCluster().getId()));
                    if (!clusterHasOwnHmsUser) {
                        throw new SecretRotationException("Data Lake's HMS user is still shared between Data Hubs, Data Hubs should be rotated first, " +
                                "which operation will create unique HMS user for Data Hub!");
                    }
                })
                .withResourceCrn(resourceCrn)
                .build();
        contexts.put(CUSTOM_JOB, customJobRotationContext);
        return contexts;
    }

    @Override
    protected Predicate<RDSConfig> getRDSConfigTypePredicate() {
        return rdsConfig -> StringUtils.equals(rdsConfig.getType(), DatabaseType.HIVE.name());
    }

    @Override
    public SecretType getSecret() {
        return INTERNAL_DATALAKE_CM_SERVICE_SHARED_DB;
    }
}
