package com.sequenceiq.cloudbreak.ambari;


import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.CUSTOM_VDF_REPO_KEY;
import static com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails.VDF_REPO_KEY_PREFIX;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sequenceiq.ambari.client.services.ClusterService;
import com.sequenceiq.ambari.client.services.StackService;
import com.sequenceiq.cloudbreak.cloud.VersionComparator;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.Versioned;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.util.JsonUtil;

import groovyx.net.http.HttpResponseException;

@Service
public class AmbariRepositoryVersionService {

    public static final Versioned AMBARI_VERSION_2_6_0_0 = () -> "2.6.0.0";

    public static final Versioned AMBARI_VERSION_2_7_0_0 = () -> "2.7.0.0-0";

    public static final Versioned AMBARI_VERSION_2_7_2_0 = () -> "2.7.2.0";

    public static final Versioned AMBARI_VERSION_2_7_100_0 = () -> "2.7.100.0-0";

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariRepositoryVersionService.class);

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private JsonHelper jsonHelper;

    public String getRepositoryVersion(Long clusterId) {
        StackRepoDetails stackRepoDetails = getStackRepoDetails(clusterId);
        AmbariRepo ambariRepoDetails = clusterComponentConfigProvider.getAmbariRepo(clusterId);
        String result = "";
        if (stackRepoDetails != null && isVersionNewerOrEqualThanLimited(ambariRepoDetails::getVersion, AMBARI_VERSION_2_6_0_0)) {
            Optional<String> repositoryVersion = Optional.ofNullable(stackRepoDetails.getStack().get(StackRepoDetails.REPOSITORY_VERSION));
            result = repositoryVersion.orElse("");
        }
        return result;
    }

    public void setBaseRepoURL(String stackName, Long clusterId, StackService ambariClient) {
        StackRepoDetails stackRepoDetails = getStackRepoDetails(clusterId);
        if (stackRepoDetails != null) {
            try {
                LOGGER.debug("Use specific Ambari repository: {}", stackRepoDetails);
                AmbariRepo ambariRepoDetails = clusterComponentConfigProvider.getAmbariRepo(clusterId);
                if (isVersionNewerOrEqualThanLimited(ambariRepoDetails::getVersion, AMBARI_VERSION_2_6_0_0)) {
                    addVersionDefinitionFileToAmbari(stackName, ambariClient, stackRepoDetails);
                } else {
                    setRepositoryVersionOnApi(ambariClient, stackRepoDetails);
                }
            } catch (HttpResponseException e) {
                String exceptionErrorMsg = AmbariClientExceptionUtil.getErrorMessage(e);
                String msg = String.format("Cannot use the specified Ambari stack: %s. Error: %s", stackRepoDetails.toString(), exceptionErrorMsg);
                throw new AmbariServiceException(msg, e);
            }
        } else {
            LOGGER.debug("Using latest HDP repository");
        }
    }

    public boolean setupLdapAndSsoOnApi(AmbariRepo ambariRepo) {
        return isVersionNewerOrEqualThanLimited(ambariRepo::getVersion, AMBARI_VERSION_2_7_0_0);
    }

    public boolean isVersionNewerOrEqualThanLimited(Versioned currentVersion, Versioned limitedAPIVersion) {
        Comparator<Versioned> versionComparator = new VersionComparator();
        return versionComparator.compare(currentVersion, limitedAPIVersion) > -1;
    }

    public String getOsTypeForStackRepoDetails(StackRepoDetails stackRepoDetails) {
        Map<String, String> stackRepo = stackRepoDetails.getStack();
        stackRepo = removeVDFRelatedRepoDetails(stackRepo);
        String[] unwantedKeys = {StackRepoDetails.REPO_ID_TAG, StackRepoDetails.MPACK_TAG};
        Set<String> keys = stackRepo.keySet();
        keys.removeAll(List.of(unwantedKeys));
        return keys.isEmpty() ? "" : keys.iterator().next();
    }

    private StackRepoDetails getStackRepoDetails(long clusterId) {
        return clusterComponentConfigProvider.getHDPRepo(clusterId);
    }

    private void setRepositoryVersionOnApi(StackService ambariClient, StackRepoDetails stackRepoDetails) throws HttpResponseException {
        LOGGER.debug("Set repository versions in Ambari via old API calls.");
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
        for (Entry<String, String> entry : stackRepo.entrySet()) {
            addRepository(ambariClient, stackType, version, entry.getKey(), stackRepoId, entry.getValue(), stackRepoDetails.isVerify());
        }
        for (Entry<String, String> entry : utilRepo.entrySet()) {
            addRepository(ambariClient, stackType, version, entry.getKey(), utilRepoId, entry.getValue(), stackRepoDetails.isVerify());
        }
    }

    private Map<String, String> removeVDFRelatedRepoDetails(Map<String, String> stackRepoDetails) {
        stackRepoDetails.remove(StackRepoDetails.REPOSITORY_VERSION);
        return stackRepoDetails.entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith(VDF_REPO_KEY_PREFIX))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    private void addRepository(StackService client, String stack, String version, String os,
            String repoId, String repoUrl, boolean verify) throws HttpResponseException {
        client.addStackRepository(stack, version, os, repoId, repoUrl, verify);
    }

    private void addVersionDefinitionFileToAmbari(String stackName, ClusterService ambariClient, StackRepoDetails stackRepoDetails) {
        Optional<String> vdfUrl = Optional.ofNullable(stackRepoDetails.getStack().get(CUSTOM_VDF_REPO_KEY));
        if (!vdfUrl.isPresent()) {
            String message = String.format("Couldn't determine any VDF file for the stack: %s", stackName);
            LOGGER.info(message);
            throw new AmbariOperationFailedException(message);
        }

        String repoId = stackRepoDetails.getStack().get(StackRepoDetails.REPO_ID_TAG);
        int ind = repoId.indexOf('-');
        if (ind != -1) {
            repoId = repoId.substring(0, ind);
        }
        String repoVersion = stackRepoDetails.getStack().get(StackRepoDetails.REPOSITORY_VERSION);
        String versionDefJson = ambariClient.getVersionDefinition(repoId, repoVersion);
        JsonNode versionDefNode = jsonHelper.createJsonFromString(versionDefJson);
        ArrayNode versionDefItems = (ArrayNode) versionDefNode.path("items");
        if (versionDefItems.size() == 0) {
            String vdf = ambariClient.createVersionDefinition(vdfUrl.get());
            LOGGER.debug("VDF request has been sent to Ambari: '{}'.", JsonUtil.minify(vdf));
        } else {
            LOGGER.debug("VDF url is already set for: {} {}.", repoId, repoVersion);
        }
    }
}
