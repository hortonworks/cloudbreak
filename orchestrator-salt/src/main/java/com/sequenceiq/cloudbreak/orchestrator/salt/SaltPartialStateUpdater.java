package com.sequenceiq.cloudbreak.orchestrator.salt;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import com.google.common.base.Joiner;
import com.sequenceiq.cloudbreak.orchestrator.OrchestratorBootstrap;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.PartialStateUpdater;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.SaltUploadWithPermission;
import com.sequenceiq.cloudbreak.orchestrator.salt.runner.SaltRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStateService;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.Retry;

@Component
public class SaltPartialStateUpdater implements PartialStateUpdater {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaltPartialStateUpdater.class);

    private static final String LOCAL_SALT_RESOURCES_LOCATION = "salt";

    private static final String SALT_STATE_UPDATER_SCRIPT = "salt-state-updater.sh";

    private static final String REMOTE_SCRIPTS_LOCATION = "/opt/salt/scripts";

    private static final String REMOTE_TMP_FOLDER = "/tmp/";

    private static final String READ_WRITE_PERMISSION = "0600";

    private static final String EXECUTE_PERMISSION = "0700";

    @Inject
    private SaltService saltService;

    @Inject
    private SaltRunner saltRunner;

    @Inject
    private ExitCriteria exitCriteria;

    @Inject
    private SaltStateService saltStateService;

    @Inject
    private Retry retry;

    @Override
    public void updatePartialSaltDefinition(byte[] partialSaltState, List<String> components, List<GatewayConfig> gatewayConfigs,
            ExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException {
        LOGGER.debug("Start partial salt update for components: {}.", components);
        GatewayConfig primaryGateway = saltService.getPrimaryGatewayConfig(gatewayConfigs);
        Set<String> gatewayTargets = getGatewayPrivateIps(gatewayConfigs);
        Set<String> gatewayHostnames = getGatewayHostnames(gatewayConfigs);
        Target<String> targets = new HostList(gatewayHostnames);
        try (SaltConnector sc = saltService.createSaltConnector(primaryGateway)) {
            uploadScripts(sc, gatewayTargets, exitModel, LOCAL_SALT_RESOURCES_LOCATION, SALT_STATE_UPDATER_SCRIPT);
            String stateZip = "partial_salt_states.zip";
            uploadFileToTargetsWithContentAndPermission(sc, gatewayTargets, exitModel, partialSaltState,
                    REMOTE_TMP_FOLDER, stateZip, READ_WRITE_PERMISSION);
            updateSaltStateComponentDefinition(sc, targets, stateZip, Joiner.on(',').join(components));
            LOGGER.debug("Partial salt update has been successfully finished with components {}", components);
        }
    }

    protected void uploadAndUpdateSaltStateComponent(String component, byte[] saltState, SaltConnector sc, Set<String> gatewayTargets,
            Set<String> gatewayHostnames, ExitCriteriaModel exitModel) throws CloudbreakOrchestratorFailedException {
        Target<String> targets = new HostList(gatewayHostnames);
        uploadScripts(sc, gatewayTargets, exitModel, LOCAL_SALT_RESOURCES_LOCATION, SALT_STATE_UPDATER_SCRIPT);
        String stateZip = String.format("%s.zip", component);
        uploadFileToTargetsWithContentAndPermission(sc, gatewayTargets, exitModel, saltState,
                REMOTE_TMP_FOLDER, stateZip, READ_WRITE_PERMISSION);
        updateSaltStateComponentDefinition(sc, targets, stateZip, component);
    }

    protected void uploadScripts(SaltConnector saltConnector, Set<String> targets, ExitCriteriaModel exitCriteriaModel,
            String localFolder, String... fileNames)
            throws CloudbreakOrchestratorFailedException {
        for (String fileName : fileNames) {
            uploadFileToTargetsWithPermission(saltConnector, targets, exitCriteriaModel, localFolder, fileName);
        }
    }

    protected void uploadFileToTargetsWithContentAndPermission(SaltConnector saltConnector, Set<String> targets, ExitCriteriaModel exitCriteriaModel,
            byte[] content, String remoteFolder, String fileName, String permission) throws CloudbreakOrchestratorFailedException {
        try {
            OrchestratorBootstrap saltUpload = new SaltUploadWithPermission(saltConnector, targets, remoteFolder,
                    fileName, permission, content);
            Callable<Boolean> saltUploadRunner = saltRunner.runnerWithConfiguredErrorCount(saltUpload, exitCriteria, exitCriteriaModel);
            saltUploadRunner.call();
        } catch (Exception e) {
            LOGGER.info("Error occurred during file distribute to gateway nodes", e);
            throw new CloudbreakOrchestratorFailedException(e.getMessage(), e);
        }
    }

    protected void updateSaltStateComponentDefinition(SaltConnector sc, Target<String> targets, String zipFileName, String component) {
        String command = String.format("%s/%s -f %s%s -s %s",
                REMOTE_SCRIPTS_LOCATION, SALT_STATE_UPDATER_SCRIPT, REMOTE_TMP_FOLDER, zipFileName, component);
        Map<String, String> result = saltStateService.runCommandOnHosts(sc, targets, command);
        LOGGER.debug("Result of partial salt state ({}) upgrade: {}", component, result);
    }

    private void uploadFileToTargetsWithPermission(SaltConnector saltConnector, Set<String> targets, ExitCriteriaModel exitCriteriaModel,
            String localFolderPath, String fileName) throws CloudbreakOrchestratorFailedException {
        ClassPathResource scriptResource = new ClassPathResource(Path.of(localFolderPath, fileName).toString(), getClass().getClassLoader());
        byte[] content = asString(scriptResource).getBytes(StandardCharsets.UTF_8);
        uploadFileToTargetsWithContentAndPermission(saltConnector, targets, exitCriteriaModel, content,
                REMOTE_SCRIPTS_LOCATION, fileName, EXECUTE_PERMISSION);
    }

    private Set<String> getGatewayPrivateIps(Collection<GatewayConfig> allGatewayConfigs) {
        return allGatewayConfigs.stream().filter(GatewayConfig::isPrimary).map(GatewayConfig::getPrivateAddress).collect(Collectors.toSet());
    }

    private Set<String> getGatewayHostnames(Collection<GatewayConfig> allGatewayConfigs) {
        return allGatewayConfigs.stream().filter(GatewayConfig::isPrimary).map(GatewayConfig::getHostname).collect(Collectors.toSet());
    }

    private String asString(Resource resource) {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
