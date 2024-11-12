package com.sequenceiq.environment.environment.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.environment.environment.domain.CompactView;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentViewDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.scheduled.sync.EnvironmentJobService;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;

@Service
public class EnvironmentDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDeletionService.class);

    private final EnvironmentViewService environmentViewService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentResourceDeletionService environmentResourceDeletionService;

    private final EnvironmentJobService environmentJobService;

    public EnvironmentDeletionService(EnvironmentViewService environmentViewService,
            EnvironmentJobService environmentJobService,
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentReactorFlowManager reactorFlowManager,
            EnvironmentResourceDeletionService environmentResourceDeletionService) {
        this.environmentResourceDeletionService = environmentResourceDeletionService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.environmentJobService = environmentJobService;
        this.environmentViewService = environmentViewService;
        this.reactorFlowManager = reactorFlowManager;
    }

    public EnvironmentViewDto deleteByNameAndAccountId(String environmentName, String accountId, String actualUserCrn,
            boolean cascading, boolean forced) {
        EnvironmentView environment = environmentViewService.getByNameAndAccountId(environmentName, accountId);
        MDCBuilder.buildMdcContext(environment);
        LOGGER.debug("Deleting environment [name: {}]", environment.getName());
        delete(environment, actualUserCrn, cascading, forced);
        return environmentDtoConverter.environmentViewToViewDto(environment);
    }

    public EnvironmentViewDto deleteByCrnAndAccountId(String crn, String accountId, String actualUserCrn,
            boolean cascading, boolean forced) {
        EnvironmentView environment = environmentViewService.getByCrnAndAccountId(crn, accountId);
        MDCBuilder.buildMdcContext(environment);
        LOGGER.debug("Deleting environment [name: {}]", environment.getName());
        delete(environment, actualUserCrn, cascading, forced);
        return environmentDtoConverter.environmentViewToViewDto(environment);
    }

    @VisibleForTesting
    EnvironmentView delete(EnvironmentView environment, String userCrn,
            boolean cascading, boolean forced) {
        LOGGER.debug("Deleting environment [name: {}, cascading={}, forced={}]", environment.getName(), cascading, forced);
        MDCBuilder.buildMdcContext(environment);
        if (cascading) {
            prepareForDeletion(environment, forced);
            reactorFlowManager.triggerCascadingDeleteFlow(environment, userCrn, forced);
        } else {
            validateDeletionWithoutCascadingWhenChildEnvironments(environment);
            checkIsEnvironmentDeletableWithoutCascadingWhenAttachedResources(environment);
            prepareForDeletion(environment, forced);
            reactorFlowManager.triggerDeleteFlow(environment, userCrn, forced);
        }
        return environment;
    }

    private void prepareForDeletion(EnvironmentView environment, boolean forced) {
        updateEnvironmentDeletionType(environment, forced);
        environmentJobService.unschedule(environment.getId());
    }

    private void updateEnvironmentDeletionType(EnvironmentView environment, boolean forced) {
        environmentViewService.editDeletionType(environment, forced);
    }

    public List<EnvironmentViewDto> deleteMultipleByNames(Set<String> environmentNames, String accountId, String actualUserCrn,
            boolean cascading, boolean forced) {
        Collection<EnvironmentView> environmentViews = environmentViewService.findByNamesInAccount(environmentNames, accountId);
        return deleteMultiple(actualUserCrn, cascading, forced, environmentViews);
    }

    public List<EnvironmentViewDto> deleteMultipleByCrns(Set<String> crns, String accountId, String actualUserCrn,
            boolean cascading, boolean forced) {
        Collection<EnvironmentView> environmentViews = environmentViewService.findByResourceCrnsInAccount(crns, accountId);
        return deleteMultiple(actualUserCrn, cascading, forced, environmentViews);
    }

    private List<EnvironmentViewDto> deleteMultiple(String actualUserCrn, boolean cascading, boolean forced, Collection<EnvironmentView> environments) {
        Collection<EnvironmentView> environmentsToDelete = environments;
        if (cascading) {
            environmentsToDelete = removeChildEnvironmentsToAvoidDeletionConflicts(environments);
        }
        return new ArrayList<>(environmentsToDelete).stream()
                .map(environment -> {
                    LOGGER.debug("Starting to delete environment [name: {}, CRN: {}]", environment.getName(), environment.getResourceCrn());
                    delete(environment, actualUserCrn, cascading, forced);
                    return environmentDtoConverter.environmentViewToViewDto(environment);
                })
                .collect(Collectors.toList());
    }

    private Collection<EnvironmentView> removeChildEnvironmentsToAvoidDeletionConflicts(Collection<EnvironmentView> environments) {
        Set<String> environmentNamesToDelete = environments.stream().map(CompactView::getName).collect(Collectors.toSet());
        Set<EnvironmentView> childrenEnvironmentsToRemove = environments.stream().filter(environmentView -> environmentView.getParentEnvironment() != null)
                .filter(environmentView -> environmentNamesToDelete.contains(environmentView.getParentEnvironment().getName()))
                .collect(Collectors.toSet());
        LOGGER.info("Removing child environments [names: {}] to avoid deletion conflicts as their parents are in the deletion list and parent deletion " +
                "deletes children environment.", childrenEnvironmentsToRemove.stream().map(CompactView::getName).collect(Collectors.joining(", ")));
        return environments.stream()
                .filter(env -> !childrenEnvironmentsToRemove.contains(env))
                .collect(Collectors.toSet());
    }

    @VisibleForTesting
    void checkIsEnvironmentDeletableWithoutCascadingWhenAttachedResources(EnvironmentView env) {
        LOGGER.info("Checking if environment [name: {}] is deletable without cascading: inspecting attached clusters & experiences.", env.getName());

        Set<String> distroXClusterNames = environmentResourceDeletionService.getAttachedDistroXClusterNames(env);
        if (!distroXClusterNames.isEmpty()) {
            throw new BadRequestException(String.format("The following Data Hub cluster(s) are attached to the Environment: [%s]. " +
                            getMustUseCascadingDeleteError("they"), String.join(", ", distroXClusterNames)));
        }

        int amountOfConnectedExperiences;
        try {
            amountOfConnectedExperiences = environmentResourceDeletionService.getConnectedExperienceAmount(env);
        } catch (IllegalStateException | IllegalArgumentException | ExperienceOperationFailedException re) {
            LOGGER.error("Something has occurred during checking the connected experiences!", re);
            throw new IllegalStateException("Unable to access all experiences (data services) to check whether the Environment "
                    + "has any connected one(s)! If you would like to bypass the issue, you can use the force deletion option, but please keep in mind that "
                    + "- in some cases - it may leave resources on the cloud provider side that need to be checked and cleaned by hand afterwards. " +
                    "Error reason: " + re.getMessage());
        }
        if (amountOfConnectedExperiences == 1) {
            throw new BadRequestException("The Environment has 1 connected experience (data service). " + getMustUseCascadingDeleteError("this"));
        } else if (amountOfConnectedExperiences > 1) {
            throw new BadRequestException("The Environment has " + amountOfConnectedExperiences + " connected experiences (data services). " +
                    getMustUseCascadingDeleteError("these"));
        }

        Set<String> datalakes = environmentResourceDeletionService.getAttachedSdxClusterCrns(env);
        // If someone creates the clusters via internal cluster API, in this case the SDX service does not know about these clusters,
        // so we need to check against legacy DL API from Core service
        if (datalakes.isEmpty()) {
            datalakes = environmentResourceDeletionService.getDatalakeClusterNames(env);
        }
        if (!datalakes.isEmpty()) {
            throw new BadRequestException(String.format("The following Data Lake cluster(s) are attached to the Environment: [%s]. " +
                    getMustUseCascadingDeleteError("they"), String.join(", ", datalakes)));
        }

        Set<String> computeClusterNames = environmentResourceDeletionService.getComputeClusterNames(env);
        if (!computeClusterNames.isEmpty()) {
            throw new BadRequestException(String.format("The following Compute cluster(s) are attached to the Environment: [%s]. " +
                    getMustUseCascadingDeleteError("they"), String.join(", ", computeClusterNames)));
        }

        LOGGER.info("No attached clusters or experiences have been found for environment [name: {}], deletion without cascading may proceed.",
                env.getName());
    }

    private String getMustUseCascadingDeleteError(String subject) {
        return String.format("Either %s must be terminated before Environment deletion, " +
                "or the cascading delete option (\"I would like to delete all connected resources\") shall be utilized.", subject);
    }

    private void validateDeletionWithoutCascadingWhenChildEnvironments(EnvironmentView environmentView) {
        LOGGER.info("Checking if environment [name: {}] is deletable without cascading: inspecting child environments.", environmentView.getName());

        List<String> childEnvNames =
                environmentViewService.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(environmentView.getAccountId(), environmentView.getId());
        if (!childEnvNames.isEmpty()) {
            throw new BadRequestException(String.format("The following child Environment(s) are attached to the Environment: [%s]. " +
                    getMustUseCascadingDeleteError("these"), String.join(", ", childEnvNames)));
        }

        LOGGER.info("No child environments have been found for environment [name: {}], deletion without cascading may proceed.", environmentView.getName());
    }

}
