package com.sequenceiq.cloudbreak.rotation.context.provider;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.meteringv2.MeteringV2ServiceRoles.METERINGV2_DATABUS_ACCESS_KEY_ID;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.meteringv2.MeteringV2ServiceRoles.METERINGV2_DATABUS_ACCESS_SECRET_KEY;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.meteringv2.MeteringV2ServiceRoles.METERINGV2_DATABUS_ACCESS_SECRET_KEY_ALGO;
import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.meteringv2.MeteringV2ServiceRoles.METERINGV2_SERVICE;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.CM_SERVICE;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretRotationStep.UMS_DATABUS_CREDENTIAL;
import static com.sequenceiq.cloudbreak.rotation.CloudbreakSecretType.DBUS_UMS_ACCESS_KEY;
import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.cluster.ClusterBuilderService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.SecretRotationSaltService;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.context.CMServiceConfigRotationContext;
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
public class DbusUmsAccessKeyRotationContextProvider implements RotationContextProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbusUmsAccessKeyRotationContextProvider.class);

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

    @Inject
    private ClusterBuilderService clusterBuilderService;

    @Override
    public Map<SecretRotationStep, ? extends RotationContext> getContexts(String resourceCrn) {
        StackDto stackDto = stackService.getByCrn(resourceCrn);
        if (stackDto.getCluster().getDatabusCredential() == null) {
            throw new SecretRotationException("No databus credential present for cluster, rotation is not needed!");
        }
        CMServiceConfigRotationContext cmServiceConfigRotationContext = getCMServiceConfigRotationContext(stackDto);
        RotationContext customJobRotationContext = CustomJobRotationContext.builder()
                .withResourceCrn(resourceCrn)
                .withRotationJob(() -> updateClusterWithDatabusCredentials(stackDto))
                .withRollbackJob(() -> updateClusterWithDatabusCredentials(stackDto))
                .build();
        return Map.of(UMS_DATABUS_CREDENTIAL, new RotationContext(resourceCrn),
                CUSTOM_JOB, customJobRotationContext,
                CM_SERVICE, cmServiceConfigRotationContext);
    }

    private CMServiceConfigRotationContext getCMServiceConfigRotationContext(StackDto stackDto) {
        Table<String, String, String> cmConfigTable = HashBasedTable.create();
        String credentials = stackDto.getCluster().getDatabusCredential();
        if (StringUtils.isNotBlank(credentials)) {
            try {
                DataBusCredential dataBusCredential = new Json(credentials).get(DataBusCredential.class);
                cmConfigTable.put(METERINGV2_SERVICE, METERINGV2_DATABUS_ACCESS_KEY_ID, dataBusCredential.getAccessKey());
                cmConfigTable.put(METERINGV2_SERVICE, METERINGV2_DATABUS_ACCESS_SECRET_KEY, dataBusCredential.getPrivateKey());
                cmConfigTable.put(METERINGV2_SERVICE, METERINGV2_DATABUS_ACCESS_SECRET_KEY_ALGO, dataBusCredential.getAccessKeyType());
            } catch (IOException e) {
                LOGGER.error("Cannot read DataBusCredential from cluster entity. Continue without value.", e);
            }
        }
        return new CMServiceConfigRotationContext(stackDto.getResourceCrn(), cmConfigTable);
    }

    private void updateClusterWithDatabusCredentials(StackDto stackDto) {
        clusterBuilderService.configureManagementServices(stackDto.getId());
        refreshDatabusPillars(stackDto);
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

    private void refreshDatabusPillars(StackDto stackDto) {
        try {
            DatabusConfigView databusConfigView = getDatabusConfigView(stackDto);
            SaltPillarProperties saltPillarProperties = new SaltPillarProperties("/" + DATABUS_KEY + "/init.sls",
                    Map.of(DATABUS_KEY, databusConfigView.toMap()));
            saltService.updateSaltPillar(stackDto, Map.of(DATABUS_KEY, saltPillarProperties));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new SecretRotationException("Failed to refresh Databus relevant salt pillars.", e);
        }
    }

    private DatabusConfigView getDatabusConfigView(StackDto stackDto) {
        try {
            Telemetry telemetry = componentConfigProviderService.getTelemetry(stackDto.getId());
            boolean useDbusCnameEndpoint = entitlementService.useDataBusCNameEndpointEnabled(ThreadBasedUserCrnProvider.getAccountId());
            String databusEndpoint = dataBusEndpointProvider.getDataBusEndpoint(telemetry.getDatabusEndpoint(), useDbusCnameEndpoint);
            DataBusCredential dataBusCredential = JsonUtil.readValue(stackDto.getCluster().getDatabusCredential(), DataBusCredential.class);
            String accessKeySecretAlgorithm = StringUtils.defaultIfBlank(dataBusCredential.getAccessKeyType(), DEFAULT_ACCESS_KEY_TYPE);
            return new DatabusConfigView.Builder()
                    .withEnabled()
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
        return DBUS_UMS_ACCESS_KEY;
    }

    @Override
    public Set<String> getVaultSecretsForRollback(String resourceCrn) {
        StackDto stackDto = stackService.getByCrn(resourceCrn);
        return Set.of(stackDto.getCluster().getDatabusCredentialSecret().getSecret());
    }
}
