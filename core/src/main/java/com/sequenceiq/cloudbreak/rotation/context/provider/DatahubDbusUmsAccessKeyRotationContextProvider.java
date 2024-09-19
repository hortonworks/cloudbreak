package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.UMS_DATABUS_CREDENTIAL;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DATAHUB_DBUS_UMS_ACCESS_KEY;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.UMSSecretKeyFormatter;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigView;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;

@Component
public class DatahubDbusUmsAccessKeyRotationContextProvider implements RotationContextProvider {

    private static final String DATABUS_KEY = "databus";

    private static final String DEFAULT_ACCESS_KEY_TYPE = "Ed25519";

    @Inject
    private StackDtoService stackService;

    @Inject
    private StackUtil stackUtil;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private SecretRotationSaltService saltService;

    @Override
    public Map<SecretRotationStep, ? extends RotationContext> getContexts(String resourceCrn) {
        StackDto stackDto = stackService.getByCrn(resourceCrn);
        RotationContext customJobRotationContext = CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withRotationJob(() -> updateClusterWithDatabusCredentials(stackDto, "rotation"))
                .withRollbackJob(() -> updateClusterWithDatabusCredentials(stackDto, "rollback"))
                .build();
        return Map.of(UMS_DATABUS_CREDENTIAL, new RotationContext(resourceCrn),
                CUSTOM_JOB, customJobRotationContext);
    }

    private void updateClusterWithDatabusCredentials(StackDto stackDto, String state) {
        refreshDatabusPillars(stackDto, state);
        executeDbusRelatedSaltStates(stackDto);
        restartMgmtServicesInCM(stackDto);
    }

    private void restartMgmtServicesInCM(StackDto stackDto) {
        try {
            clusterApiConnectors.getConnector(stackDto).clusterModificationService().restartMgmtServices();
        } catch (Exception e) {
            throw new SecretRotationException("Failed to restart MGMT services to update them with Dbus credential.", e);
        }
    }

    private void executeDbusRelatedSaltStates(StackDto stackDto) {
        try {
            Set<String> targets = stackUtil.collectReachableNodes(stackDto).stream().map(Node::getHostname).collect(Collectors.toSet());
            saltService.executeSaltState(stackDto, targets, List.of("fluent.init"));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new SecretRotationException("Failed to execute Databus relevant salt states.", e);
        }
    }

    private void refreshDatabusPillars(StackDto stackDto, String state) {
        try {
            DatabusConfigView databusConfigView = getDatabusConfigView(stackDto);
            SaltPillarProperties saltPillarProperties = new SaltPillarProperties("/" + DATABUS_KEY + "/init.sls",
                    Map.of(DATABUS_KEY, databusConfigView.toMap()));
            saltService.updateSaltPillar(stackDto, Map.of(DATABUS_KEY, saltPillarProperties), state);
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new SecretRotationException("Failed to refresh Databus relevant salt pillars.", e);
        }
    }

    private DatabusConfigView getDatabusConfigView(StackDto stackDto) {
        try {
            Telemetry telemetry = componentConfigProviderService.getTelemetry(stackDto.getId());
            boolean useDbusCnameEndpoint = entitlementService.useDataBusCNameEndpointEnabled(ThreadBasedUserCrnProvider.getAccountId());
            String databusEndpoint = dataBusEndpointProvider.getDataBusEndpoint(telemetry.getDatabusEndpoint(), useDbusCnameEndpoint);
            boolean enabled = telemetry.isMeteringFeatureEnabled();
            DataBusCredential dataBusCredential = JsonUtil.readValue(stackDto.getCluster().getDatabusCredential(), DataBusCredential.class);
            String accessKeySecretAlgorithm = StringUtils.defaultIfBlank(dataBusCredential.getAccessKeyType(), DEFAULT_ACCESS_KEY_TYPE);
            return new DatabusConfigView.Builder()
                    .withEnabled(enabled)
                    .withEndpoint(databusEndpoint)
                    .withAccessKeyId(dataBusCredential.getAccessKey())
                    .withAccessKeySecret(UMSSecretKeyFormatter.formatSecretKey(accessKeySecretAlgorithm, dataBusCredential.getPrivateKey()).toCharArray())
                    .withAccessKeySecretAlgorithm(accessKeySecretAlgorithm)
                    .build();
        } catch (IOException e) {
            throw new SecretRotationException("Failed to read Databus credential from internal database.", e);
        }
    }

    @Override
    public SecretType getSecret() {
        return DATAHUB_DBUS_UMS_ACCESS_KEY;
    }

    @Override
    public Set<String> getVaultSecretsForRollback(String resourceCrn) {
        StackDto stackDto = stackService.getByCrn(resourceCrn);
        return Set.of(stackDto.getCluster().getDatabusCredentialSecret().getSecret());
    }
}
