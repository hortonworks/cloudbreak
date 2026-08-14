package com.sequenceiq.it.util.cleanup;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentBaseResponse;
import com.sequenceiq.environment.client.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.util.WaitResult;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;
import com.sequenceiq.sdx.client.SdxClient;

@Component
public class CleanupUtil extends CleanupClientUtil {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupUtil.class);

    private static final String TYPE_DISTROX = "distroxName";

    private static final String TYPE_STACK = "stackName";

    private static final String TYPE_SDX = "sdxName";

    private static final String TYPE_CREDENTIAL = "credentialName";

    private static final String TYPE_ENVIRONMENT = "environmentName";

    private final Map<String, Map<Class<? extends MicroserviceClient>, MicroserviceClient>> clients = new HashMap<>();

    @Value("${integrationtest.outputdir:.}")
    private String outputDirectory;

    @Value("${integrationtest.cleanup.afterAbort:false}")
    private boolean cleanupAfterAbort;

    @Inject
    private CleanupWaitUtil waitUtil;

    public void cleanupAllResources() {
        CleanupReport report = new CleanupReport();
        try {
            if (resourceFilesArePresent() && !cleanupAfterAbort) {
                cleanupDistroxes(report);
                cleanupSdxes(report);
                cleanupEnvironments(report);
                cleanupCredentials(report);
            } else {
                EnvironmentClient environmentClient = createEnvironmentClient();
                setEnvironmentClient(environmentClient);
                List<String> foundChildEnvironmentNames = new ArrayList<>(getChildEnvironments(environmentClient).values());
                List<String> foundEnvironmentNames = new ArrayList<>(getEnvironments(environmentClient).values());
                List<String> foundCredentialNames = getCredentials(environmentClient);

                if (!foundChildEnvironmentNames.isEmpty()) {
                    LOG.info("Found child environments: '{}'", foundChildEnvironmentNames);
                    deleteEnvironments(environmentClient, foundChildEnvironmentNames, report);
                }
                if (!foundEnvironmentNames.isEmpty()) {
                    LOG.info("Found environments: '{}'", foundEnvironmentNames);
                    deleteEnvironments(environmentClient, foundEnvironmentNames, report);
                } else {
                    LOG.info("Cannot find any environment!");
                }
                if (!foundCredentialNames.isEmpty()) {
                    LOG.info("Found credentials: '{}'", foundCredentialNames);
                    deleteCredentials(environmentClient, foundCredentialNames, report);
                } else {
                    LOG.info("Cannot find any credential!");
                }
            }
        } finally {
            logCleanupSummary(report);
        }
    }

    public void cleanupDistroxes() {
        cleanupDistroxes(new CleanupReport());
    }

    private void cleanupDistroxes(CleanupReport report) {
        CloudbreakClient cloudbreakClient = createCloudbreakClient();
        EnvironmentClient environmentClient = createEnvironmentClient();
        List<String> foundDistroxNames = getDistroxes(environmentClient, cloudbreakClient);

        setCloudbreakClient(cloudbreakClient);
        setEnvironmentClient(environmentClient);
        LOG.info("Found distroxes: '{}'", foundDistroxNames);
        if (!foundDistroxNames.isEmpty()) {
            deleteResources(foundDistroxNames, TYPE_DISTROX, report);
        } else {
            LOG.info("Cannot find any distrox");
        }
    }

    public void cleanupSdxes() {
        cleanupSdxes(new CleanupReport());
    }

    private void cleanupSdxes(CleanupReport report) {
        SdxClient sdxClient = createSdxClient();
        EnvironmentClient environmentClient = createEnvironmentClient();
        List<String> foundSdxNames = getSdxes(environmentClient, sdxClient);

        setSdxClient(sdxClient);
        setEnvironmentClient(environmentClient);
        LOG.info("Found data lakes (sdxes): '{}'", foundSdxNames);
        if (!foundSdxNames.isEmpty()) {
            deleteResources(foundSdxNames, TYPE_SDX, report);
        } else {
            LOG.info("Cannot find any sdx");
        }
    }

    public void cleanupEnvironments() {
        cleanupEnvironments(new CleanupReport());
    }

    private void cleanupEnvironments(CleanupReport report) {
        EnvironmentClient environmentClient = createEnvironmentClient();
        List<String> foundChildEnvironmentNames = new ArrayList<>(getChildEnvironments(environmentClient).values());
        List<String> foundEnvironmentNames = new ArrayList<>(getEnvironments(environmentClient).values());

        setEnvironmentClient(environmentClient);
        if (!foundChildEnvironmentNames.isEmpty()) {
            LOG.info("Found child environments: '{}'", foundChildEnvironmentNames);
            deleteResources(foundChildEnvironmentNames, TYPE_ENVIRONMENT, report);
        }
        if (!foundEnvironmentNames.isEmpty()) {
            LOG.info("Found environments: '{}'", foundEnvironmentNames);
            deleteResources(foundEnvironmentNames, TYPE_ENVIRONMENT, report);
        } else {
            LOG.info("Cannot find any environment!");
        }
    }

    public void cleanupCredentials() {
        cleanupCredentials(new CleanupReport());
    }

    private void cleanupCredentials(CleanupReport report) {
        EnvironmentClient environmentClient = createEnvironmentClient();
        List<String> foundCredentialNames = getCredentials(environmentClient);

        setEnvironmentClient(environmentClient);
        LOG.info("Found credentials: '{}'", foundCredentialNames);
        if (!foundCredentialNames.isEmpty()) {
            deleteResources(foundCredentialNames, TYPE_CREDENTIAL, report);
        } else {
            LOG.info("Cannot find any credential!");
        }
    }

    public Map<String, String> getAllEnvironments(EnvironmentClient environmentClient) {
        return environmentClient.environmentV1Endpoint().list(null).getResponses().stream()
                .collect(Collectors.toMap(EnvironmentBaseResponse::getCrn, EnvironmentBaseResponse::getName));
    }

    public Map<String, String> getEnvironments(EnvironmentClient environmentClient) {
        Map<String, String> parentEnvironments = environmentClient.environmentV1Endpoint().list(null).getResponses().stream()
                .filter(response -> response.getParentEnvironmentName() == null)
                .collect(Collectors.toMap(EnvironmentBaseResponse::getCrn, EnvironmentBaseResponse::getName));
        parentEnvironments.forEach((crn, name) -> LOG.info("Found deletable environment CRN: {} and NAME: {}", crn, name));
        return parentEnvironments;
    }

    public Map<String, String> getChildEnvironments(EnvironmentClient environmentClient) {
        Map<String, String> childEnvironments = environmentClient.environmentV1Endpoint().list(null).getResponses().stream()
                .filter(response -> response.getParentEnvironmentName() != null)
                .collect(Collectors.toMap(EnvironmentBaseResponse::getCrn, EnvironmentBaseResponse::getName));
        childEnvironments.forEach((crn, name) -> LOG.info("Found deletable child environment CRN: {} and NAME: {}", crn, name));
        return childEnvironments;
    }

    public List<String> getCredentials(EnvironmentClient environment) {
        return environment.credentialV1Endpoint().list().getResponses().stream()
                .map(CredentialResponse::getName)
                .collect(Collectors.toList());
    }

    public List<String> getDistroxes(EnvironmentClient environment, CloudbreakClient cloudbreak) {
        List<String> distroxNames = new ArrayList<>();

        getAllEnvironments(environment).forEach((key, value) -> {
            LOG.info("Collecting available distroxes for environment: {}", value);
            distroxNames.addAll(cloudbreak.distroXV1Endpoint().list(value, key).getResponses().stream()
                    .map(StackViewV4Response::getName)
                    .collect(Collectors.toList()));
        });
        return distroxNames;
    }

    public List<String> getSdxes(EnvironmentClient environment, SdxClient sdx) {
        List<String> sdxNames = new ArrayList<>();

        getAllEnvironments(environment).forEach((key, value) -> {
            LOG.info("Collecting available sdxes for environment: {}", value);
            sdxNames.addAll(sdx.sdxEndpoint().list(value, false).stream()
                    .map(SdxClusterResponse::getName)
                    .collect(Collectors.toList()));
        });
        return sdxNames;
    }

    private List<String> getResourcesFromFile(String resourceNameType, Path filePath) {
        List<String> resourceNames = new ArrayList<>();
        try {
            String resourcesFromFile = Files.readString(filePath);
            JSONObject jsonObject = new JSONObject(resourcesFromFile);
            if (jsonObject.has(resourceNameType)) {
                try {
                    JSONArray resources = jsonObject.getJSONArray(resourceNameType);
                    for (int i = 0; i < resources.length(); i++) {
                        String resource = resources.getString(i);
                        resourceNames.add(resource);
                        LOG.info("Get '{}' JSON array '{}' element from resource file with: '{}'.", resourceNameType, i, resource);
                    }
                } catch (JSONException e) {
                    String resource = jsonObject.getString(resourceNameType);
                    resourceNames.add(resource);
                    LOG.info("Get '{}' JSON object from resource file with: '{}'.", resourceNameType, resource);
                }
            } else {
                LOG.error("Cannot find '{}' in resource file '{}'.", resourceNameType, filePath.getFileName());
            }
            return resourceNames;
        } catch (JSONException e) {
            LOG.warn("Cannot get '{}' key, because of: {}", resourceNameType, e.getMessage(), e);
            return resourceNames;
        } catch (FileNotFoundException e) {
            LOG.warn("'{}' file not found, because of: {}", filePath, e.getMessage(), e);
            return resourceNames;
        } catch (IOException e) {
            LOG.warn("Reading '{}' file throws exception: {}", filePath, e.getMessage(), e);
            return resourceNames;
        }
    }

    private void deleteResources(List<String> foundResources, String resourceNameType, CleanupReport report) {
        List<Path> fileList = new ArrayList<>();
        List<String> attemptedResources = new ArrayList<>();
        Map<String, String> deleteFailures = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(outputDirectory))) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (fileName.startsWith("resource_names") && fileName.endsWith(".json")) {
                    LOG.info("Found resource file: '{}' is going to be added to resource files' list", path.getFileName().toAbsolutePath().normalize());
                    fileList.add(path);
                }
            }
        } catch (Exception e) {
            LOG.error("Cannot find resource file at path: '{}', because of: {}", Paths.get(outputDirectory).toAbsolutePath().normalize(), e.getMessage(), e);
            throw new RuntimeException(String.format("Cannot find resource file at path: '%s', because of: %s",
                    Paths.get(outputDirectory).toAbsolutePath().normalize(), e.getMessage()));
        }
        for (Path filePath : fileList) {
            LOG.info("Processing resource file: '{}'", filePath.getFileName());
            List<String> resourcesName = Optional.ofNullable(getResourcesFromFile(resourceNameType, filePath))
                    .orElse(List.of());
            for (String resourceName : resourcesName) {
                if (!foundResources.contains(resourceName)) {
                    LOG.info("Cannot find '{}:{}' via the API — treating as already cleaned up.", resourceNameType, resourceName);
                    continue;
                }
                LOG.info("{}:{} will be deleted!", resourceNameType, resourceName);
                attemptedResources.add(resourceName);
                try {
                    switch (resourceNameType) {
                        case TYPE_DISTROX:
                        case TYPE_STACK:
                            deleteDistrox(getCloudbreakClient(), resourceName);
                            break;
                        case TYPE_SDX:
                            deleteSdx(getSdxClient(), resourceName);
                            break;
                        case TYPE_CREDENTIAL:
                            deleteCredential(getEnvironmentClient(), resourceName);
                            break;
                        default:
                            deleteEnvironment(getEnvironmentClient(), resourceName);
                            break;
                    }
                } catch (RuntimeException ex) {
                    // Do not abort the whole resource-type pass on a single failure — record it and continue so that
                    // the final report reflects every leftover, not just the first one.
                    LOG.error("Deleting {}:{} failed, continuing with the rest: {}", resourceNameType, resourceName, ex.getMessage(), ex);
                    deleteFailures.put(resourceName, ex.getMessage());
                }
            }
        }
        List<String> leftoverResources = findLeftoverResources(resourceNameType, attemptedResources);

        // Feed the per-run accumulators: everything we attempted and neither errored nor is still
        // present counts as successfully deleted for the summary at the end of cleanupAllResources().
        List<String> successfullyDeleted = attemptedResources.stream()
                .filter(name -> !deleteFailures.containsKey(name))
                .filter(name -> !leftoverResources.contains(name))
                .collect(Collectors.toList());
        report.recordDeleted(resourceNameType, successfullyDeleted);
        report.recordLeftovers(resourceNameType, leftoverResources);
        report.recordDeleteErrors(resourceNameType, deleteFailures);

        validateE2ECleanup(resourceNameType, attemptedResources, deleteFailures, leftoverResources);
    }

    /**
     * Re-query the appropriate API after deletion to determine which of the resources we attempted to delete
     * are actually still present. A resource missing from the response (or a 404 on the per-name endpoint)
     * means the delete succeeded; anything still visible is a genuine leftover. On a re-list failure
     * (transient network / auth), we conservatively treat all attempted resources as leftovers rather than
     * silently declaring success — same principle as CleanupWaitUtil.check*IsAvailable.
     */
    private List<String> findLeftoverResources(String resourceNameType, List<String> attemptedResources) {
        if (attemptedResources.isEmpty()) {
            return List.of();
        }
        try {
            Set<String> stillPresent;
            switch (resourceNameType) {
                case TYPE_DISTROX:
                case TYPE_STACK:
                    stillPresent = new HashSet<>(getDistroxes(getEnvironmentClient(), getCloudbreakClient()));
                    break;
                case TYPE_SDX:
                    stillPresent = new HashSet<>(getSdxes(getEnvironmentClient(), getSdxClient()));
                    break;
                case TYPE_CREDENTIAL:
                    stillPresent = new HashSet<>(getCredentials(getEnvironmentClient()));
                    break;
                default:
                    EnvironmentClient environmentClient = getEnvironmentClient();
                    stillPresent = new HashSet<>();
                    stillPresent.addAll(getChildEnvironments(environmentClient).values());
                    stillPresent.addAll(getEnvironments(environmentClient).values());
                    break;
            }
            return attemptedResources.stream()
                    .filter(stillPresent::contains)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            LOG.error("Could not re-list '{}' resources to verify cleanup, treating all attempted as still present: {}",
                    resourceNameType, e.getMessage(), e);
            return new ArrayList<>(attemptedResources);
        }
    }

    private void deleteEnvironments(EnvironmentClient environmentClient, List<String> environmentNames, CleanupReport report) {
        try {
            environmentNames.forEach(environmentName -> LOG.info("Environment with name: {} will be deleted!", environmentName));
            environmentClient.environmentV1Endpoint().deleteMultipleByNames(new HashSet<>(environmentNames), true, false);
            environmentNames.forEach(environmentName -> {
                WaitResult waitResult = waitUtil.waitForEnvironmentCleanup(environmentClient, environmentName);
                if (waitResult == WaitResult.FAILED) {
                    throw new RuntimeException(String.format("Failed: Deleting %s environment has been failed!", environmentName));
                }
                if (waitResult == WaitResult.TIMEOUT) {
                    throw new RuntimeException(String.format("Timeout: Deleting %s environment has been timed out!", environmentName));
                }
                // Record per-environment success as the wait for that env clears.
                report.recordDeleted(TYPE_ENVIRONMENT, environmentName);
            });
        } catch (Exception e) {
            LOG.error("One or more environment cannot be deleted, because of: {}", e.getMessage(), e);
            throw new RuntimeException(String.format("One or more environment cannot be deleted, because of: %s", e.getMessage()));
        }
    }

    private void deleteEnvironment(EnvironmentClient environmentClient, String environmentName) {
        try {
            environmentClient.environmentV1Endpoint().deleteByName(environmentName, true, false);
            WaitResult waitResult = waitUtil.waitForEnvironmentCleanup(environmentClient, environmentName);
            if (waitResult == WaitResult.FAILED) {
                throw new RuntimeException(String.format("Failed: Deleting %s environment has been failed!", environmentName));
            }
            if (waitResult == WaitResult.TIMEOUT) {
                throw new RuntimeException(String.format("Timeout: Deleting %s environment has been timed out!", environmentName));
            }
        } catch (NotFoundException e) {
            LOG.info("{} environment have already been deleted", environmentName);
        } catch (Exception e) {
            LOG.error("{} environment cannot be deleted, because of: {}", environmentName, e.getMessage(), e);
            throw new RuntimeException(String.format("%s environment cannot be deleted, because of: %s", environmentName, e.getMessage()));
        }
    }

    public void deleteCredentials(EnvironmentClient environmentClient, List<String> credentialNames) {
        deleteCredentials(environmentClient, credentialNames, new CleanupReport());
    }

    private void deleteCredentials(EnvironmentClient environmentClient, List<String> credentialNames, CleanupReport report) {
        waitUtil.waitForEnvironmentsCleanup(environmentClient);
        try {
            credentialNames.forEach(credentialName -> LOG.info("Credential with name: {} will be deleted!", credentialName));
            environmentClient.credentialV1Endpoint().deleteMultiple(new HashSet<>(credentialNames));
        } catch (Exception e) {
            LOG.error("One or more credential cannot be deleted, because of: {}", e.getMessage(), e);
            throw new RuntimeException(String.format("One or more credential cannot be deleted, because of: %s", e.getMessage()));
        }
        // deleteMultiple returning normally means the API accepted the batch — verify per-credential
        // via a re-list so we do not repeat the original false-positive bug on partial failure.
        List<String> stillPresent = findLeftoverResources(TYPE_CREDENTIAL, credentialNames);
        List<String> deleted = credentialNames.stream()
                .filter(name -> !stillPresent.contains(name))
                .collect(Collectors.toList());
        report.recordDeleted(TYPE_CREDENTIAL, deleted);
        report.recordLeftovers(TYPE_CREDENTIAL, stillPresent);
        if (!stillPresent.isEmpty()) {
            LOG.error("End To End cleanup failed: credential(s) '{}' still present after deleteMultiple.", stillPresent);
            throw new RuntimeException(String.format(
                    "End To End cleanup failed for resource type '%s': %d credential(s) still present after delete.",
                    TYPE_CREDENTIAL, stillPresent.size()));
        }
    }

    private void deleteCredential(EnvironmentClient environmentClient, String credentialName) {
        try {
            environmentClient.credentialV1Endpoint().deleteByName(credentialName);
        } catch (NotFoundException e) {
            LOG.info("{} credential have already been deleted", credentialName);
        } catch (Exception e) {
            LOG.error("{} credential cannot be deleted, because of: {}", credentialName, e.getMessage(), e);
            throw new RuntimeException(String.format("%s credential cannot be deleted, because of: %s", credentialName, e.getMessage()));
        }
    }

    private void deleteSdx(SdxClient sdxClient, String sdxName) {
        try {
            sdxClient.sdxEndpoint().delete(sdxName, true);
            WaitResult waitResult = waitUtil.waitForSdxCleanup(sdxClient, sdxName);
            if (waitResult == WaitResult.FAILED) {
                throw new RuntimeException(String.format("Failed: Deleting %s data lake (sdx) has been failed!", sdxName));
            }
            if (waitResult == WaitResult.TIMEOUT) {
                throw new RuntimeException(String.format("Timeout: Deleting %s data lake (sdx) has been timed out!", sdxName));
            }
        } catch (NotFoundException e) {
            LOG.info("{} data lake (sdx) have already been deleted", sdxName);
        } catch (Exception e) {
            LOG.error("{} data lake (sdx) cannot be deleted, because of: {}", sdxName, e.getMessage(), e);
            throw new RuntimeException(String.format("%s data lake (sdx) cannot be deleted, because of: %s", sdxName, e.getMessage()));
        }
    }

    private void deleteDistrox(CloudbreakClient cloudbreakClient, String distroxName) {
        try {
            cloudbreakClient.distroXV1Endpoint().deleteByName(distroxName, true);
            WaitResult waitResult = waitUtil.waitForDistroxCleanup(cloudbreakClient, distroxName);
            if (waitResult == WaitResult.FAILED) {
                throw new RuntimeException(String.format("Failed: Deleting %s data hub (distrox) has been failed!", distroxName));
            }
            if (waitResult == WaitResult.TIMEOUT) {
                throw new RuntimeException(String.format("Timeout: Deleting %s data hub (distrox) has been timed out!", distroxName));
            }
        } catch (NotFoundException e) {
            LOG.info("{} data hub (distrox) have already been deleted", distroxName);
        } catch (Exception e) {
            LOG.error("{} data hub (distrox) cannot be deleted, because of: {}", distroxName, e.getMessage(), e);
            throw new RuntimeException(String.format("%s data hub (distrox) cannot be deleted, because of: %s", distroxName, e.getMessage()));
        }
    }

    /**
     * Reports cleanup outcome for a single resource type. Only fails when either a delete call errored out
     * or the post-delete re-list still shows the resource — a successful delete whose follow-up GET returns
     * 404 is a success, not a leftover.
     */
    private void validateE2ECleanup(String resourceNameType, List<String> attemptedResources,
            Map<String, String> deleteFailures, List<String> leftoverResources) {
        if (attemptedResources.isEmpty()) {
            LOG.info("End To End cleanup for resource type '{}': nothing to delete.", resourceNameType);
            return;
        }
        if (deleteFailures.isEmpty() && leftoverResources.isEmpty()) {
            LOG.info("End To End cleanup for resource type '{}' succeeded — {} resource(s) deleted: {}",
                    resourceNameType, attemptedResources.size(), attemptedResources);
            return;
        }
        if (!deleteFailures.isEmpty()) {
            deleteFailures.forEach((name, reason) ->
                    LOG.error("End To End cleanup failed: delete of '{}:{}' errored — {}", resourceNameType, name, reason));
        }
        if (!leftoverResources.isEmpty()) {
            LOG.error("End To End cleanup failed: resource '{}' with name(s) '{}' still present after delete!",
                    resourceNameType, leftoverResources);
        }
        int problemCount = deleteFailures.size() + leftoverResources.size();
        throw new RuntimeException(String.format(
                "End To End cleanup failed for resource type '%s': %d resource(s) could not be cleaned up (delete errors: %d, still present: %d).",
                resourceNameType, problemCount, deleteFailures.size(), leftoverResources.size()));
    }

    /**
     * Renders a single, consolidated summary block at the end of a cleanup run so operators do not
     * have to scroll through the Jenkins console log. Called from cleanupAllResources()'s finally
     * block so it also prints when the run aborts mid-way.
     */
    private void logCleanupSummary(CleanupReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        sb.append("=========================== CLEANUP SUMMARY ===========================").append(System.lineSeparator());
        sb.append(String.format("Deleted: %d | Still present: %d | Delete errors: %d",
                        report.deletedCount(), report.leftoverCount(), report.errorCount()))
                .append(System.lineSeparator());

        if (report.isEmpty()) {
            sb.append("Nothing to clean up.").append(System.lineSeparator());
        } else {
            Map<String, List<String>> deleted = report.getDeletedByType();
            Map<String, List<String>> leftovers = report.getLeftoversByType();
            Map<String, Map<String, String>> errors = report.getDeleteErrorsByType();
            if (!deleted.isEmpty()) {
                sb.append("--- Deleted successfully ---").append(System.lineSeparator());
                deleted.forEach((type, names) ->
                        sb.append(String.format("  %s (%d): %s", type, names.size(), names)).append(System.lineSeparator()));
            }
            if (!leftovers.isEmpty()) {
                sb.append("--- Still present after delete ---").append(System.lineSeparator());
                leftovers.forEach((type, names) ->
                        sb.append(String.format("  %s (%d): %s", type, names.size(), names)).append(System.lineSeparator()));
            }
            if (!errors.isEmpty()) {
                sb.append("--- Delete errors ---").append(System.lineSeparator());
                errors.forEach((type, byName) -> byName.forEach((name, reason) ->
                        sb.append(String.format("  %s: %s — %s", type, name, reason)).append(System.lineSeparator())));
            }
        }
        sb.append("=======================================================================");

        String summary = sb.toString();
        if (report.leftoverCount() > 0 || report.errorCount() > 0) {
            LOG.error(summary);
        } else {
            LOG.info(summary);
        }
    }

    private boolean resourceFilesArePresent() {
        boolean result = false;
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(outputDirectory))) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                if (fileName.startsWith("resource_names") && fileName.endsWith(".json")) {
                    LOG.info("Found resource file at path: '{}'.", path.getFileName().toAbsolutePath().normalize());
                    result = true;
                }
            }
        } catch (Exception e) {
            LOG.info("Cannot find resource file at path: '{}'.", Paths.get(outputDirectory).toAbsolutePath().normalize());
        }
        return result;
    }
}
