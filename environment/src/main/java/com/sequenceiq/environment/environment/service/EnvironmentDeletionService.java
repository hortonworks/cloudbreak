package com.sequenceiq.environment.environment.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.sync.EnvironmentJobService;

@Service
public class EnvironmentDeletionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnvironmentDeletionService.class);

    private final EnvironmentService environmentService;

    private final EnvironmentDtoConverter environmentDtoConverter;

    private final EnvironmentReactorFlowManager reactorFlowManager;

    private final EnvironmentResourceDeletionService environmentResourceDeletionService;

    private final EnvironmentJobService environmentJobService;

    public EnvironmentDeletionService(EnvironmentService environmentService,
            EnvironmentJobService environmentJobService,
            EnvironmentDtoConverter environmentDtoConverter,
            EnvironmentReactorFlowManager reactorFlowManager,
            EnvironmentResourceDeletionService environmentResourceDeletionService) {
        this.environmentResourceDeletionService = environmentResourceDeletionService;
        this.environmentDtoConverter = environmentDtoConverter;
        this.environmentJobService = environmentJobService;
        this.environmentService = environmentService;
        this.reactorFlowManager = reactorFlowManager;
    }

    public EnvironmentDto deleteByNameAndAccountId(String environmentName, String accountId, String actualUserCrn,
            boolean cascading, boolean forced) {
        Optional<Environment> environment = environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(environmentName, accountId);
        MDCBuilder.buildMdcContext(environment.orElseThrow(()
                -> new NotFoundException(String.format("No environment found with name '%s'", environmentName))));
        LOGGER.debug(String.format("Deleting environment [name: %s]", environment.get().getName()));
        delete(environment.get(), actualUserCrn, cascading, forced);
        return environmentDtoConverter.environmentToDto(environment.get());
    }

    public EnvironmentDto deleteByCrnAndAccountId(String crn, String accountId, String actualUserCrn,
            boolean cascading, boolean forced) {
        Optional<Environment> environment = environmentService
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(crn, accountId);
        MDCBuilder.buildMdcContext(environment.orElseThrow(()
                -> new NotFoundException(String.format("No environment found with crn '%s'", crn))));
        LOGGER.debug(String.format("Deleting  environment [name: %s]", environment.get().getName()));
        delete(environment.get(), actualUserCrn, cascading, forced);
        return environmentDtoConverter.environmentToDto(environment.get());
    }

    public Environment delete(Environment environment, String userCrn,
            boolean cascading, boolean forced) {
        MDCBuilder.buildMdcContext(environment);
        validateDeletion(environment, cascading);
        LOGGER.debug("Deleting environment with name: {}", environment.getName());
        environmentJobService.unschedule(environment);
        if (cascading) {
            reactorFlowManager.triggerCascadingDeleteFlow(environment, userCrn, forced);
        } else {
            checkIsEnvironmentDeletable(environment);
            reactorFlowManager.triggerDeleteFlow(environment, userCrn, forced);
        }
        return environment;
    }

    public List<EnvironmentDto> deleteMultipleByNames(Set<String> environmentNames, String accountId, String actualUserCrn,
            boolean cascading, boolean forced) {
        Collection<Environment> environments = environmentService.findByNameInAndAccountIdAndArchivedIsFalse(environmentNames, accountId);
        return deleteMultiple(actualUserCrn, cascading, forced, environments);
    }

    public List<EnvironmentDto> deleteMultipleByCrns(Set<String> crns, String accountId, String actualUserCrn,
            boolean cascading, boolean forced) {
        Collection<Environment> environments = environmentService.findByResourceCrnInAndAccountIdAndArchivedIsFalse(crns, accountId);
        return deleteMultiple(actualUserCrn, cascading, forced, environments);
    }

    private List<EnvironmentDto> deleteMultiple(String actualUserCrn, boolean cascading, boolean forced, Collection<Environment> environments) {
        return new ArrayList<>(environments).stream()
                .map(environment -> {
                    LOGGER.debug(String.format("Starting to archive environment [name: %s, CRN: %s]", environment.getName(), environment.getResourceCrn()));
                    delete(environment, actualUserCrn, cascading, forced);
                    return environmentDtoConverter.environmentToDto(environment);
                })
                .collect(Collectors.toList());
    }

    @VisibleForTesting
    void checkIsEnvironmentDeletable(Environment env) {
        LOGGER.info("Checking if environment [name: {}] is deletable", env.getName());

        Set<String> datalakes = environmentResourceDeletionService.getAttachedSdxClusterCrns(env);
        // if someone use create the clusters via internal cluster API, in this case the SDX service does not know about these clusters,
        // so we need to check against legacy DL API from Core service
        if (datalakes.isEmpty()) {
            datalakes = environmentResourceDeletionService.getDatalakeClusterNames(env);
        }
        if (!datalakes.isEmpty()) {
            throw new BadRequestException(String.format("The following Data Lake cluster(s) must be terminated before Environment deletion [%s]",
                    String.join(", ", datalakes)));
        }

        Set<String> distroXClusterNames = environmentResourceDeletionService.getAttachedDistroXClusterNames(env);
        if (!distroXClusterNames.isEmpty()) {
            throw new BadRequestException(String.format("The following Data Hub cluster(s) must be terminated before Environment deletion [%s]",
                    String.join(", ", distroXClusterNames)));
        }


        int amountOfConnectedExperiences = environmentResourceDeletionService.getConnectedExperienceAmount(env);
        if (amountOfConnectedExperiences > 0) {
            if (amountOfConnectedExperiences == 1) {
                throw new BadRequestException("The given environment [" + env.getName() + "] has 1 connected experience. " +
                        "This must be terminated before Environment deletion.");
            } else {
                throw new BadRequestException("The given environment [\" + env.getName() + \"] has " + amountOfConnectedExperiences +
                        " connected experiences. " + "These must be terminated before Environment deletion.");
            }
        }
    }

    void validateDeletion(Environment environment, boolean cascading) {
        if (!cascading) {
            List<String> childEnvNames =
                    environmentService.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(environment.getAccountId(), environment.getId());
            if (!childEnvNames.isEmpty()) {
                throw new BadRequestException(String.format("The following Environment(s) must be deleted before Environment deletion [%s]",
                        String.join(", ", childEnvNames)));
            }
        }
    }

}
