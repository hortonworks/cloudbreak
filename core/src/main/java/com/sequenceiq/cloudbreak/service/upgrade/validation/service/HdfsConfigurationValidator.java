package com.sequenceiq.cloudbreak.service.upgrade.validation.service;

import static com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsConfigHelper.HDFS_CLIENT_CONFIG_SAFETY_VALVE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cmtemplate.CmTemplateGeneratorService;
import com.sequenceiq.cloudbreak.cmtemplate.configproviders.hdfs.HdfsRoles;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.common.exception.UpgradeValidationFailedException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;

@Component
public class HdfsConfigurationValidator implements ServiceUpgradeValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(HdfsConfigurationValidator.class);

    private static final String HDFS_REPLACE_DATANODE_ON_FAILURE_POLICY = "dfs.client.block.write.replace-datanode-on-failure.policy";

    private static final String HDFS_DATANODE_ON_FAILURE_ENABLED = "dfs.client.block.write.replace-datanode-on-failure.enable";

    private static final String HDFS_REPLACE_DATANODE_ON_FAILURE_POLICY_PROPERTY =
            "<property><name>dfs.client.block.write.replace-datanode-on-failure.policy</name><value>NEVER</value></property>";

    private static final String HDFS_DATANODE_ON_FAILURE_ENABLED_PROPERTY =
            "<property><name>dfs.client.block.write.replace-datanode-on-failure.enable</name><value>false</value></property>";

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private EntitlementService entitlementService;

    @Inject
    private CmTemplateGeneratorService clusterTemplateGeneratorService;

    @Override
    public void validate(ServiceUpgradeValidationRequest validationRequest) {
        StackDto stack = validationRequest.stack();
        if (validationRequest.rollingUpgradeEnabled() && rollingUpgradeValidationEnabled() && hdfsServicePresentOnTheCluster(stack)) {
            validateHbaseConfigurationForRollingUpgrade(stack);
        } else {
            LOGGER.debug("Skipping HDFS configuration validation because the rolling upgrade is not enabled");
        }
    }

    private boolean hdfsServicePresentOnTheCluster(StackDto stack) {
        Set<SupportedService> services = clusterTemplateGeneratorService.getServicesByBlueprint(stack.getBlueprintJsonText()).getServices();
        if (services.stream().anyMatch(service -> HdfsRoles.HDFS.equalsIgnoreCase(service.getName()))) {
            return true;
        } else {
            LOGGER.debug("The HDFS service is not present on the cluster. Only the following services are available: {}", services);
            return false;
        }
    }

    private void validateHbaseConfigurationForRollingUpgrade(StackDto stack) {
        ClusterApi connector = clusterApiConnectors.getConnector(stack);
        Optional<String> config = connector.getRoleConfigValueByServiceType(stack.getName(), HdfsRoles.GATEWAY, HdfsRoles.HDFS,
                HDFS_CLIENT_CONFIG_SAFETY_VALVE);
        List<String> configurationErrors = new ArrayList<>();
        validateHdfsReplaceDataNodeOnFailurePolicy(config, configurationErrors);
        validateHdfsReplaceDataNodeOnFailureEnabled(config, configurationErrors);
        if (configurationErrors.isEmpty()) {
            LOGGER.debug("Rolling upgrade is permitted for this cluster because all required HDFS configuration is correct.");
        } else {
            String errorMessage = "Rolling upgrade is not permitted because " + String.join(" and ", configurationErrors);
            throw new UpgradeValidationFailedException(errorMessage);
        }

    }

    private void validateHdfsReplaceDataNodeOnFailurePolicy(Optional<String> config, List<String> configurationErrors) {
        if (config.isEmpty() || !config.get().contains(HDFS_REPLACE_DATANODE_ON_FAILURE_POLICY_PROPERTY)) {
            String errorMessage = String.format("the value of the %s configuration is incorrect,"
                    + " please set this value to NEVER to enable the rolling upgrade for this cluster", HDFS_REPLACE_DATANODE_ON_FAILURE_POLICY);
            configurationErrors.add(errorMessage);
        }
    }

    private void validateHdfsReplaceDataNodeOnFailureEnabled(Optional<String> config, List<String> configurationErrors) {
        if (config.isEmpty() || !config.get().contains(HDFS_DATANODE_ON_FAILURE_ENABLED_PROPERTY)) {
            String errorMessage = String.format("the value of the %s configuration is incorrect,"
                    + " please set this value to FALSE to enable the rolling upgrade for this cluster", HDFS_DATANODE_ON_FAILURE_ENABLED);
            configurationErrors.add(errorMessage);
        }
    }

    private boolean rollingUpgradeValidationEnabled() {
        boolean skipRollingUpgradeValidationEnabled = entitlementService.isSkipRollingUpgradeValidationEnabled(ThreadBasedUserCrnProvider.getAccountId());
        if (skipRollingUpgradeValidationEnabled) {
            LOGGER.debug("Skipping HDFS configuration validation because rolling upgrade validation is disabled.");
        }
        return !skipRollingUpgradeValidationEnabled;
    }
}
