package com.sequenceiq.cloudbreak.core.flow2.cluster.maintenance;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.AVAILABLE;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.MAINTENANCE_MODE_ENABLED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_FAILED;
import static com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status.UPDATE_IN_PROGRESS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.cloud.event.model.EventStatus;
import com.sequenceiq.cloudbreak.cloud.model.AmbariRepo;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cloud.model.component.StackRepoDetails;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.CloudbreakImageCatalogException;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.flow2.CheckResult;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.core.flow2.stack.image.update.StackImageUpdateService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.json.JsonHelper;
import com.sequenceiq.cloudbreak.message.Msg;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.ComponentConfigProviderService;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;

@Service
public class MaintenanceModeValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaintenanceModeValidationService.class);

    @Inject
    private ComponentConfigProviderService componentConfigProviderService;

    @Inject
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Inject
    private ImageCatalogService imageCatalogService;

    @Inject
    private ClusterService clusterService;

    @Inject
    private StackImageUpdateService stackImageUpdateService;

    @Inject
    private StackUpdater stackUpdater;

    @Inject
    private CloudbreakFlowMessageService flowMessageService;

    @Inject
    private JsonHelper jsonHelper;

    public String fetchStackRepository(Long stackId) {
        String stackRepo = clusterService.getStackRepositoryJson(stackId);
        if (stackRepo == null) {
            LOGGER.debug("Stack repository info cannot be fetched due missing OS type.");
            return null;
        } else if (stackRepo.isEmpty()) {
            throw new CloudbreakServiceException("Stack repository info cannot be validated!");
        }

        LOGGER.debug(String.format("Stack repo fetched: %s", stackRepo));
        return stackRepo;
    }

    public void setUpValidationFlow(Long stackId) {
        clusterService.updateClusterStatusByStackId(stackId, UPDATE_IN_PROGRESS);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.CLUSTER_OPERATION, "Validating repos and images...");
        flowMessageService.fireEventAndLog(stackId, Msg.MAINTENANCE_MODE_VALIDATION_STARTED, UPDATE_IN_PROGRESS.name());
    }

    public List<Warning> validateStackRepository(Long clusterId, String stackRepo) {
        List<Warning> warnings = new ArrayList<>();
        StackRepoDetails repoDetails = clusterComponentConfigProvider.getStackRepoDetails(clusterId);
        String hdpVersion = repoDetails.getHdpVersion();
        if (StringUtils.isEmpty(hdpVersion)) {
            throw new CloudbreakServiceException("HDP version is null in database, validation aborted!");
        }
        Map<String, String> stack = repoDetails.getStack();
        if (Objects.nonNull(stackRepo)) {
            JsonNode stackRepoJson = jsonHelper.createJsonFromString(stackRepo).path("Repositories");
            String baseUrl = stackRepoJson.path("base_url").asText();
            String osType = stackRepoJson.path("os_type").asText();
            String repoId = stackRepoJson.path("repo_id").asText();

            String configuredRepoId = stack.get(StackRepoDetails.REPO_ID_TAG);
            if (!configuredRepoId.contentEquals(repoId)) {
                warnings.add(new Warning(WarningType.STACK_REPO_WARNING,
                        String.format("Incorrect repo id! Configured '%s', but fetched '%s' from Ambari.",
                                configuredRepoId,
                                repoId)));
            }
            String configuredBaseUrl = stack.get(osType);
            if (configuredBaseUrl == null) {
                warnings.add(new Warning(WarningType.STACK_REPO_WARNING,
                        String.format("Incorrect OS type configured, fetched '%s' from Ambari.",
                                osType)));
            } else if (!configuredBaseUrl.contentEquals(baseUrl)) {
                warnings.add(new Warning(WarningType.STACK_REPO_WARNING,
                        String.format("Incorrect repo URL. '%s' configured but '%s' fetched from Ambari.",
                                configuredBaseUrl,
                                baseUrl)));
            }
        }

        stack.remove(StackRepoDetails.REPO_ID_TAG);
        List<Warning> notFound = stack.entrySet().stream().filter(element -> !element.getValue().contains(hdpVersion))
                .peek(element -> LOGGER.warn("Stack repo naming validation warning! {} cannot be found in {}", hdpVersion, element.getValue()))
                .map(element -> new Warning(WarningType.STACK_NAMING_WARNING,
                        String.format("Stack version: '%s' cannot be found in parameter: '%s'.", hdpVersion, element.getValue())))
                .collect(Collectors.toList());
        warnings.addAll(notFound);

        return warnings;
    }

    public List<Warning> validateAmbariRepository(Long clusterId) {
        List<Warning> warnings = new ArrayList<>();
        AmbariRepo repoDetails = clusterComponentConfigProvider.getAmbariRepo(clusterId);
        String baseUrl = repoDetails.getBaseUrl();
        String version = repoDetails.getVersion();
        if (!baseUrl.contains(version)) {
            LOGGER.warn("Ambari repo naming validation warning! {} cannot be found in {}", version, baseUrl);
            warnings.add(new Warning(WarningType.AMBARI_NAMING_WARNING,
                    String.format("Ambari version: '%s' cannot be found in url '%s'.",
                            version,
                            baseUrl)));
        }
        return warnings;
    }

    public List<Warning> validateImageCatalog(Stack stack) {
        List<Warning> warnings = new ArrayList<>();
        try {
            Image image = componentConfigProviderService.getImage(stack.getId());
            StatedImage statedImage = imageCatalogService.getImage(image.getImageCatalogUrl(),
                    image.getImageCatalogName(), image.getImageId());

            if (!image.getPackageVersions().isEmpty()) {
                CheckResult checkResult = stackImageUpdateService.checkPackageVersions(stack, statedImage);
                if (checkResult.getStatus().equals(EventStatus.FAILED)) {
                    warnings.add(new Warning(WarningType.IMAGE_INCOMPATIBILITY_WARNING, checkResult.getMessage()));
                }
            }
        } catch (CloudbreakImageNotFoundException | CloudbreakImageCatalogException e) {
            throw new CloudbreakServiceException("Image info could not be validated!", e);
        }
        return warnings;
    }

    public void handleValidationSuccess(Long stackId, List<Warning> warnings) {
        LOGGER.debug("Maintenance mode validation flow has been finished successfully");
        clusterService.updateClusterStatusByStackId(stackId, MAINTENANCE_MODE_ENABLED);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE, "Validation has been finished");

        try {
            if (!warnings.isEmpty()) {
                String warningJson = new ObjectMapper().writeValueAsString(warnings);
                LOGGER.warn(String.format("Found warnings: {%s}", warningJson));
                flowMessageService.fireEventAndLog(stackId, Msg.MAINTENANCE_MODE_VALIDATION_FINISHED_FOUND_WARNINGS,
                        AVAILABLE.name(), warningJson);
            } else {
                flowMessageService.fireEventAndLog(stackId, Msg.MAINTENANCE_MODE_VALIDATION_FINISHED_NO_WARNINGS,
                        AVAILABLE.name());
            }
        } catch (JsonProcessingException e) {
            throw new CloudbreakServiceException("Validation result could not be serialized!", e);
        }
    }

    public void handleValidationFailure(Long stackId, Exception error) {
        String errorDetailes = error.getMessage();
        LOGGER.warn("Error during Maintenance mode validation flow: ", error);
        clusterService.updateClusterStatusByStackId(stackId, MAINTENANCE_MODE_ENABLED);
        stackUpdater.updateStackStatus(stackId, DetailedStackStatus.AVAILABLE,
                String.format("Validation has been finished with error: %s", error));
        flowMessageService.fireEventAndLog(stackId, Msg.MAINTENANCE_MODE_VALIDATION_FAILED, UPDATE_FAILED.name(),
                errorDetailes);
    }

    protected enum WarningType {
        AMBARI_NAMING_WARNING,
        STACK_REPO_WARNING,
        STACK_NAMING_WARNING,
        IMAGE_INCOMPATIBILITY_WARNING
    }

}