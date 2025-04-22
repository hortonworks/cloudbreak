package com.sequenceiq.freeipa.service.rotation.dbuscredential.contextprovider;

import static com.sequenceiq.cloudbreak.rotation.CommonSecretRotationStep.CUSTOM_JOB;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretRotationStep.FREEIPA_UMS_DATABUS_CREDENTIAL;
import static com.sequenceiq.freeipa.rotation.FreeIpaSecretType.DBUS_UMS_ACCESS_KEY;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.model.SaltPillarProperties;
import com.sequenceiq.cloudbreak.rotation.SecretRotationStep;
import com.sequenceiq.cloudbreak.rotation.SecretType;
import com.sequenceiq.cloudbreak.rotation.common.RotationContext;
import com.sequenceiq.cloudbreak.rotation.common.RotationContextProvider;
import com.sequenceiq.cloudbreak.rotation.common.SecretRotationException;
import com.sequenceiq.cloudbreak.rotation.secret.custom.CustomJobRotationContext;
import com.sequenceiq.cloudbreak.telemetry.DataBusEndpointProvider;
import com.sequenceiq.cloudbreak.telemetry.UMSSecretKeyFormatter;
import com.sequenceiq.cloudbreak.telemetry.databus.DatabusConfigView;
import com.sequenceiq.common.api.telemetry.model.DataBusCredential;
import com.sequenceiq.common.api.telemetry.model.Telemetry;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.rotation.SecretRotationSaltService;
import com.sequenceiq.freeipa.service.stack.StackService;

@Component
public class FreeIpaDbusUmsAccessKeyRotationContextProvider implements RotationContextProvider {
    private static final String DATABUS_KEY = "databus";

    private static final String DEFAULT_ACCESS_KEY_TYPE = "Ed25519";

    @Inject
    private StackService stackService;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private DataBusEndpointProvider dataBusEndpointProvider;

    @Inject
    private SecretRotationSaltService saltService;

    @Override
    public Map<SecretRotationStep, ? extends RotationContext> getContexts(String environmentCrnAsString) {
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        Stack stack = stackService.getByEnvironmentCrnAndAccountIdWithLists(environmentCrnAsString, environmentCrn.getAccountId());
        if (stack.getDatabusCredential() == null) {
            throw new SecretRotationException("No databus credential present for cluster, rotation is not needed!");
        }
        RotationContext customJobRotationContext = CustomJobRotationContext.builder()
                .withResourceCrn(environmentCrnAsString)
                .withRotationJob(() -> updateClusterWithDatabusCredentials(stack, environmentCrn.getAccountId()))
                .withRollbackJob(() -> updateClusterWithDatabusCredentials(stack, environmentCrn.getAccountId()))
                .build();
        return Map.of(FREEIPA_UMS_DATABUS_CREDENTIAL, new RotationContext(environmentCrnAsString),
                CUSTOM_JOB, customJobRotationContext);
    }

    private void updateClusterWithDatabusCredentials(Stack stack, String accountId) {
        refreshDatabusPillars(stack, accountId);
        executeDbusRelatedSaltStates(stack);
    }

    private void executeDbusRelatedSaltStates(Stack stack) {
        try {
            Set<String> targets = stack.getAllNotDeletedNodes()
                    .stream()
                    .map(Node::getHostname)
                    .collect(Collectors.toSet());
            saltService.executeSaltState(stack, targets, List.of("fluent.init"));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new SecretRotationException("Failed to execute Databus relevant salt states.", e);
        }
    }

    private void refreshDatabusPillars(Stack stack, String accountId) {
        try {
            DatabusConfigView databusConfigView = getDatabusConfigView(stack, accountId);
            SaltPillarProperties saltPillarProperties = new SaltPillarProperties("/" + DATABUS_KEY + "/init.sls",
                    Map.of(DATABUS_KEY, databusConfigView.toMap()));
            saltService.updateSaltPillar(stack, Map.of(DATABUS_KEY, saltPillarProperties));
        } catch (CloudbreakOrchestratorFailedException e) {
            throw new SecretRotationException("Failed to refresh Databus relevant salt pillars.", e);
        }
    }

    private DatabusConfigView getDatabusConfigView(Stack stack, String accountId) {
        try {
            Telemetry telemetry = stack.getTelemetry();
            boolean useDbusCnameEndpoint = entitlementService.useDataBusCNameEndpointEnabled(accountId);
            String databusEndpoint = dataBusEndpointProvider.getDataBusEndpoint(telemetry.getDatabusEndpoint(), useDbusCnameEndpoint);
            DataBusCredential dataBusCredential = JsonUtil.readValue(stack.getDatabusCredential(), DataBusCredential.class);
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
    public Set<String> getVaultSecretsForRollback(String environmentCrnAsString) {
        Crn environmentCrn = Crn.safeFromString(environmentCrnAsString);
        Stack stack = stackService.getByEnvironmentCrnAndAccountId(environmentCrnAsString, environmentCrn.getAccountId());
        if (stack.getDatabusCredential() != null) {
            return Set.of(stack.getDatabusCredentialSecret().getSecret());
        }
        return Set.of();
    }
}
