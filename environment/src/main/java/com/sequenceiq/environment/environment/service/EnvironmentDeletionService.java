package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.environment.network.dao.domain.RegistrationType.CREATE_NEW;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentViewDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.sync.EnvironmentJobService;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;

@Service
public class EnvironmentDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDeletionService.class);

    private final EnvironmentViewService environmentViewService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentResourceDeletionService environmentResourceDeletionService;

    private final EnvironmentJobService environmentJobService;

    private final EnvironmentService environmentService;

    public EnvironmentDeletionService(EnvironmentViewService environmentViewService,
            EnvironmentService environmentService,
            EnvironmentJobService environmentJobService,
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentReactorFlowManager reactorFlowManager,
            EnvironmentResourceDeletionService environmentResourceDeletionService) {
        this.environmentResourceDeletionService = environmentResourceDeletionService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.environmentJobService = environmentJobService;
        this.environmentViewService = environmentViewService;
        this.reactorFlowManager = reactorFlowManager;
        this.environmentService = environmentService;
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
    EnvironmentView delete(EnvironmentView environment, String userCrn, boolean cascading, boolean forced) {
        LOGGER.debug("Deleting environment [name: {}, cascading={}, forced={}]", environment.getName(), cascading, forced);
        checkIfNetworkIsNotUsedByOtherEnv(environment, forced);
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

    private void checkIfNetworkIsNotUsedByOtherEnv(EnvironmentView environment, boolean skipValidation) {
        if (skipValidation) {
            LOGGER.debug("Force deletion was requested (for environment: {}) therefore network usage verification is skipped.", environment.getResourceCrn());
            return;
        }
        BaseNetwork network = environment.getNetwork();
        if (network.getRegistrationType() == CREATE_NEW) {
            LOGGER.debug("Environment (CRN: {}) was created alongside a new network, therefore its usage is about to be checked before proceeding deletion. " +
                    "[networkName: {}, networkID: {}]", environment.getResourceCrn(), network.getName(), network.getId());
            Set<NameOrCrn> envs = environmentService.getEnvironmentsUsingTheSameNetwork(network);
            if (isNotEmpty(envs)) {
                LOGGER.info("Environment deletion terminated! There are other environments deployed in same network which was created during the creation" +
                                " of this (CRN: {}), therefore we cannot continue the environment deletion to prevent breaking these environments: {}",
                        environment.getResourceCrn(), StringUtils.join(",", envs));
                throw new IllegalStateException("Deletion not allowed because there are other environments that are using the same network that was created " +
                        "during the creation of this environment. \nPlease remove those before initiating the deletion of this environment or use the force " +
                        "option, but if you'd do the latter option, please keep in mind that the termination could break those environments since their " +
                        "network related cloud resources will be terminated as well.");
            }
        }
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
        return new ArrayList<>(environments).stream()
                .map(environment -> {
                    LOGGER.debug("Starting to delete environment [name: {}, CRN: {}]", environment.getName(), environment.getResourceCrn());
                    delete(environment, actualUserCrn, cascading, forced);
                    return environmentDtoConverter.environmentViewToViewDto(environment);
                })
                .collect(Collectors.toList());
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
