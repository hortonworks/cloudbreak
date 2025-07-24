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
import java.util.concurrent.atomic.AtomicBoolean;
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
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

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

    private final Map<String, Map<Class<? extends MicroserviceClient>, MicroserviceClient>> clients = new HashMap<>();

    private final MultiValueMap<String, String> deletedResources = new LinkedMultiValueMap<>();

    @Value("${integrationtest.outputdir:.}")
    private String outputDirectory;

    @Value("${integrationtest.cleanup.afterAbort:false}")
    private boolean cleanupAfterAbort;

    @Inject
    private CleanupWaitUtil waitUtil;

    public void cleanupAllResources() {
        if (resourceFilesArePresent() && !cleanupAfterAbort) {
            cleanupDistroxes();
            cleanupSdxes();
            cleanupEnvironments();
            cleanupCredentials();
        } else {
            EnvironmentClient environmentClient = createEnvironmentClient();
            List<String> foundChildEnvironmentNames = new ArrayList<>(getChildEnvironments(environmentClient).values());
            List<String> foundEnvironmentNames = new ArrayList<>(getEnvironments(environmentClient).values());
            List<String> foundCredentialNames = getCredentials(environmentClient);

            if (!foundChildEnvironmentNames.isEmpty()) {
                LOG.info("Found child environments: '{}'", foundChildEnvironmentNames);
                deleteEnvironments(environmentClient, foundChildEnvironmentNames);
            }
            if (!foundEnvironmentNames.isEmpty()) {
                LOG.info("Found environments: '{}'", foundEnvironmentNames);
                deleteEnvironments(environmentClient, foundEnvironmentNames);
            } else {
                LOG.info("Cannot find any environment!");
            }
            if (!foundCredentialNames.isEmpty()) {
                LOG.info("Found credentials: '{}'", foundCredentialNames);
                deleteCredentials(environmentClient, foundCredentialNames);
            } else {
                LOG.info("Cannot find any credential!");
            }
        }
    }

    public void cleanupDistroxes() {
        CloudbreakClient cloudbreakClient = createCloudbreakClient();
        EnvironmentClient environmentClient = createEnvironmentClient();
        List<String> foundDistroxNames = getDistroxes(environmentClient, cloudbreakClient);

        setCloudbreakClient(cloudbreakClient);
        LOG.info("Found distroxes: '{}'", foundDistroxNames);
        if (!foundDistroxNames.isEmpty()) {
            deleteResources(foundDistroxNames, "distroxName");
        } else {
            LOG.info("Cannot find any distrox");
        }
    }

    public void cleanupSdxes() {
        SdxClient sdxClient = createSdxClient();
        EnvironmentClient environmentClient = createEnvironmentClient();
        List<String> foundSdxNames = getSdxes(environmentClient, sdxClient);

        setSdxClient(sdxClient);
        LOG.info("Found data lakes (sdxes): '{}'", foundSdxNames);
        if (!foundSdxNames.isEmpty()) {
            deleteResources(foundSdxNames, "sdxName");
        } else {
            LOG.info("Cannot find any sdx");
        }
    }

    public void cleanupEnvironments() {
        EnvironmentClient environmentClient = createEnvironmentClient();
        List<String> foundChildEnvironmentNames = new ArrayList<>(getChildEnvironments(environmentClient).values());
        List<String> foundEnvironmentNames = new ArrayList<>(getEnvironments(environmentClient).values());

        setEnvironmentClient(environmentClient);
        if (!foundChildEnvironmentNames.isEmpty()) {
            LOG.info("Found child environments: '{}'", foundChildEnvironmentNames);
            deleteResources(foundChildEnvironmentNames, "environmentName");
        }
        if (!foundEnvironmentNames.isEmpty()) {
            LOG.info("Found environments: '{}'", foundEnvironmentNames);
            deleteResources(foundEnvironmentNames, "environmentName");
        } else {
            LOG.info("Cannot find any environment!");
        }
    }

    public void cleanupCredentials() {
        EnvironmentClient environmentClient = createEnvironmentClient();
        List<String> foundCredentialNames = getCredentials(environmentClient);

        setEnvironmentClient(environmentClient);
        LOG.info("Found credentials: '{}'", foundCredentialNames);
        if (!foundCredentialNames.isEmpty()) {
            deleteResources(foundCredentialNames, "credentialName");
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

    private void deleteResources(List<String> foundResources, String resourceNameType) {
        List<Path> fileList = new ArrayList<>();
        AtomicBoolean e2eCleanupFailed = new AtomicBoolean(false);

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
        fileList.forEach(filePath -> {
            LOG.info("Processing resource file: '{}'", filePath.getFileName());
            List<String> resourcesName = Optional.ofNullable(getResourcesFromFile(resourceNameType, filePath))
                    .orElse(List.of());
            resourcesName.forEach(resourceName -> {
                if (foundResources.contains(resourceName)) {
                    LOG.info("{}:{} will be deleted!", resourceNameType, foundResources.stream()
                            .filter(resourceName::equals).findAny().orElse(null));
                    switch (resourceNameType) {
                        case "distroxName":
                        case "stackName":
                            deleteDistrox(getCloudbreakClient(), resourceName);
                            deletedResources.add(resourceNameType, resourceName);
                            break;
                        case "sdxName":
                            deleteSdx(getSdxClient(), resourceName);
                            deletedResources.add(resourceNameType, resourceName);
                            break;
                        case "credentialName":
                            deleteCredential(getEnvironmentClient(), resourceName);
                            deletedResources.add(resourceNameType, resourceName);
                            e2eCleanupFailed.set(true);
                            break;
                        default:
                            deleteEnvironment(getEnvironmentClient(), resourceName);
                            deletedResources.add(resourceNameType, resourceName);
                            break;
                    }
                } else {
                    LOG.info("Cannot find '{}:{}'! So End To End cleanup have been done successfully.", resourceNameType, resourceName);
                }
            });
        });
        validateE2ECleanup(e2eCleanupFailed, deletedResources);
    }

    private void deleteEnvironments(EnvironmentClient environmentClient, List<String> environmentNames) {
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
        waitUtil.waitForEnvironmentsCleanup(environmentClient);
        try {
            credentialNames.forEach(credentialName -> LOG.info("Credential with name: {} will be deleted!", credentialName));
            environmentClient.credentialV1Endpoint().deleteMultiple(new HashSet<>(credentialNames));
        } catch (Exception e) {
            LOG.error("One or more credential cannot be deleted, because of: {}", e.getMessage(), e);
            throw new RuntimeException(String.format("One or more credential cannot be deleted, because of: %s", e.getMessage()));
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

    private void validateE2ECleanup(AtomicBoolean e2eCleanupFailed, MultiValueMap<String, String> resourceNames) {
        if (e2eCleanupFailed.get()) {
            resourceNames
                    .forEach((type, names) ->
                            LOG.error("End To End cleanup have been failed, because of resource '{}' with name(s) '{}' found left behind!", type, names)
                    );
            throw new RuntimeException(String.format("End To End cleanup have been failed, because of '%d' resource(s) found left behind!",
                    resourceNames.size()));
        } else {
            LOG.info("End To End cleanup have been success, because of cannot found any resource left behind!");
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
