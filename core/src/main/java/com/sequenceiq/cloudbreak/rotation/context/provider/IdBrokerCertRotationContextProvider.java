package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE_ROLE_RESTART;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_PILLAR;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.SALT_STATE_APPLY;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.IDBROKER_CERT;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.VAULT;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cmtemplate.configproviders.knox.KnoxRoles;
import com.sequenceiq.cloudbreak.converter.IdBrokerConverterUtil;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.cluster.IdBroker;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.rotation.ExitCriteriaProvider;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.idbroker.IdBrokerService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;

@Component
public class IdBrokerCertRotationContextProvider extends AbstractKnoxCertRotationProvider {

    @Inject
    private StackDtoService stackService;

    @Inject
    private IdBrokerService idBrokerService;

    @Inject
    private IdBrokerConverterUtil idBrokerConverterUtil;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private GatewayConfigService gatewayConfigService;

    @Inject
    private ExitCriteriaProvider exitCriteriaProvider;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Override
    public Map<SecretRotationStep, RotationContext> getContexts(String resourceId) {
        Map<SecretRotationStep, RotationContext> result = new HashMap<>();
        StackDto stack = stackService.getByCrn(resourceId);

        final IdBroker idBroker = idBrokerService.getByCluster(stack.getCluster().getId());
        final IdBroker newIdBroker = idBrokerConverterUtil.generateIdBrokerSignKeys(stack.getCluster().getId(), stack.getWorkspace());
        //not yet supported in CM - OPSAPS-65182
        newIdBroker.setMasterSecret(idBroker.getMasterSecret());

        result.put(VAULT, getVaultRotationContext(stack.getResourceCrn(), Map.of(idBroker.getSignKeySecret().getSecret(), newIdBroker.getSignKey())));
        result.put(SALT_PILLAR, getSaltPillarRotationContext(stack.getResourceCrn(), clusterHostServiceRunner.getIdBrokerPillarProperties(newIdBroker)));
        result.put(SALT_STATE_APPLY, getSaltStateApplyRotationContext(stack, gatewayConfigService, exitCriteriaProvider.get(stack)));
        result.put(CM_SERVICE_ROLE_RESTART, getCMServiceRoleRestartRotationContext(stack.getResourceCrn()));
        result.put(CUSTOM_JOB, getCustomJobRotationContext(stack.getResourceCrn(), newIdBroker, idBroker));
        return result;
    }

    private CustomJobRotationContext getCustomJobRotationContext(String resourceCrn, IdBroker newIdBroker, IdBroker oldIdBroker) {
        CustomJobRotationContext.CustomJobRotationContextBuilder customJobRotationContextBuilder = CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withRotationJob(() -> idBrokerService.save(updateIdBroker(oldIdBroker, newIdBroker)))
                .withRollbackJob(() -> idBrokerService.save(oldIdBroker));
        return customJobRotationContextBuilder.build();
    }

    private IdBroker updateIdBroker(IdBroker oldIdBroker, IdBroker newIdBroker) {
        IdBroker result = oldIdBroker.copy();
        result.setSignPub(newIdBroker.getSignPub());
        result.setSignCert(newIdBroker.getSignCert());
        return result;
    }

    @Override
    public SecretType getSecret() {
        return IDBROKER_CERT;
    }

    @Override
    protected String getKnoxRole() {
        return KnoxRoles.IDBROKER;
    }

    @Override
    protected String getSettingsStateName() {
        return "idbroker";
    }
}
