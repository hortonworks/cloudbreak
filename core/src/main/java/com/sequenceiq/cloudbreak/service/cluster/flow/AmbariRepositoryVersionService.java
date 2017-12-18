package com.sequenceiq.cloudbreak.service.cluster.flow;


import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.CUSTOM_VDF_REPO_KEY;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.VDF_REPO_KEY_PREFIX;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.sequenceiq.ambari.client.services.StackService;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.OrchestratorTypeResolver;
import com.sequenceiq.cloudbreak.domain.Orchestrator;
import com.sequenceiq.cloudbreak.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.ComponentConfigProvider;
import com.sequenceiq.cloudbreak.service.cluster.AmbariServiceException;
import com.sequenceiq.cloudbreak.util.AmbariClientExceptionUtil;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariRepositoryVersionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariRepositoryVersionService.class);

    private static final String VERSION_PATTERN = "^\\d+\\.\\d+\\.\\d+.\\d+";

    private static final long AMBARI_VERSION_OF_NEW_API = 2600L;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private OrchestratorTypeResolver orchestratorTypeResolver;

    @Inject
    private ComponentConfigProvider componentConfigProvider;

    public String getRepositoryVersion(long clusterId, Orchestrator orchestrator) throws CloudbreakException {
        StackRepoDetails stackRepoDetails = getStackRepoDetails(clusterId, orchestrator);
        AmbariRepo ambariRepoDetails = clusterComponentConfigProvider.getAmbariRepo(clusterId);
        long ambariVersion = extractAmbariVersion(ambariRepoDetails);
        String result = "";
        if (stackRepoDetails != null && ambariVersion >= AMBARI_VERSION_OF_NEW_API) {
            Optional<String> repositoryVersion = Optional.ofNullable(stackRepoDetails.getStack().get(StackRepoDetails.REPOSITORY_VERSION));
            result = repositoryVersion.orElse("");
        }
        return result;
    }

    public void setBaseRepoURL(Long stackId, long clusterId, Orchestrator orchestrator, StackService ambariClient) throws CloudbreakException {
        StackRepoDetails stackRepoDetails = getStackRepoDetails(clusterId, orchestrator);
        if (stackRepoDetails != null) {
            try {
                LOGGER.info("Use specific Ambari repository: {}", stackRepoDetails);
                AmbariRepo ambariRepoDetails = clusterComponentConfigProvider.getAmbariRepo(clusterId);
                long ambariVersion = extractAmbariVersion(ambariRepoDetails);
                if (ambariVersion < AMBARI_VERSION_OF_NEW_API) {
                    setRepositoryVersionOnApi(ambariClient, stackRepoDetails);
                } else {
                    addVersionDefinitionFileToAmbari(stackId, ambariClient, stackRepoDetails);
                }
            } catch (HttpResponseException e) {
                String exceptionErrorMsg = AmbariClientExceptionUtil.getErrorMessage(e);
                String msg = String.format("Cannot use the specified Ambari stack: %s. Error: %s", stackRepoDetails.toString(), exceptionErrorMsg);
                throw new AmbariServiceException(msg, e);
            }
        } else {
            LOGGER.info("Using latest HDP repository");
        }
    }

    private StackRepoDetails getStackRepoDetails(long clusterId, Orchestrator orchestrator) throws CloudbreakException {
        StackRepoDetails stackRepoDetails = null;
        if (!orchestratorTypeResolver.resolveType(orchestrator).containerOrchestrator() || "YARN".equals(orchestrator.getType())) {
            stackRepoDetails = clusterComponentConfigProvider.getHDPRepo(clusterId);
        }
        return stackRepoDetails;
    }

    private long extractAmbariVersion(AmbariRepo ambariRepo) {
        String ambariVersion = ambariRepo.getVersion();
        Matcher matcher = Pattern.compile(VERSION_PATTERN).matcher(ambariVersion);
        if (matcher.find()) {
            ambariVersion = matcher.group(0).replace(".", "");
            return Long.parseLong(ambariVersion);
        } else {
            String message = String.format("Ambari version could not be extracted from '%s'.", ambariVersion);
            LOGGER.error(message);
            throw new AmbariServiceException(message);
        }
    }

    private void setRepositoryVersionOnApi(StackService ambariClient, StackRepoDetails stackRepoDetails) throws HttpResponseException {
        LOGGER.info("Set repository versions in Ambari via old API calls.");
        Map<String, String> stackRepo = stackRepoDetails.getStack();
        Map<String, String> utilRepo = stackRepoDetails.getUtil();
        String stackRepoId = stackRepo.remove(StackRepoDetails.REPO_ID_TAG);
        String utilRepoId = utilRepo.remove(StackRepoDetails.REPO_ID_TAG);
        stackRepo = removeVDFRelatedRepoDetails(stackRepo);
        stackRepo.remove(StackRepoDetails.MPACK_TAG);
        String[] typeVersion = stackRepoId.split("-");
        String stackType = typeVersion[0];
        String version = "";
        if (typeVersion.length > 1) {
            version = typeVersion[1];
        }
        for (Map.Entry<String, String> entry : stackRepo.entrySet()) {
            addRepository(ambariClient, stackType, version, entry.getKey(), stackRepoId, entry.getValue(), stackRepoDetails.isVerify());
        }
        for (Map.Entry<String, String> entry : utilRepo.entrySet()) {
            addRepository(ambariClient, stackType, version, entry.getKey(), utilRepoId, entry.getValue(), stackRepoDetails.isVerify());
        }
    }

    private Map<String, String> removeVDFRelatedRepoDetails(Map<String, String> stackRepoDetails) {
        stackRepoDetails.remove(StackRepoDetails.REPOSITORY_VERSION);
        return stackRepoDetails.entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith(VDF_REPO_KEY_PREFIX))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void addRepository(StackService client, String stack, String version, String os,
            String repoId, String repoUrl, boolean verify) throws HttpResponseException {
        client.addStackRepository(stack, version, os, repoId, repoUrl, verify);
    }

    private void addVersionDefinitionFileToAmbari(Long stackId, StackService ambariClient, StackRepoDetails stackRepoDetails) {
        Optional<String> vdfUrl = Optional.ofNullable(stackRepoDetails.getStack().get(CUSTOM_VDF_REPO_KEY));
        if (!vdfUrl.isPresent()) {
            vdfUrl = getVDFUrlByOsType(stackId, stackRepoDetails);
        }

        if (vdfUrl.isPresent()) {
            LOGGER.info("VDF request has been sent to Ambari with VDF url: '{}'.", vdfUrl.get());
            String vdf = ambariClient.createVersionDefinition(vdfUrl.get());
            LOGGER.info("VDF request has been sent to Ambari: '{}'.", JsonUtil.minify(vdf));
        } else {
            LOGGER.error("Couldn't determine any VDF file, let Ambari to start with defaults");
        }
    }

    private Optional<String> getVDFUrlByOsType(Long stackId, StackRepoDetails stackRepoDetails) {
        String vdfStackRepoKeyFilter = VDF_REPO_KEY_PREFIX;
        try {
            Image image = componentConfigProvider.getImage(stackId);
            if (!StringUtils.isEmpty(image.getOsType())) {
                vdfStackRepoKeyFilter += image.getOsType();
            }
        } catch (CloudbreakImageNotFoundException e) {
            LOGGER.error(String.format("Could not get Image Component for stack: '%s'.", stackId), e);
        }

        final String filter = vdfStackRepoKeyFilter;
        return stackRepoDetails.getStack().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(filter))
                .map(Map.Entry::getValue)
                .findFirst();
    }
}
