package com.sequenceiq.environment.environment.flow.modify.tags.handler;

import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_EXPERIENCES_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_EVENT;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.modify.tags.EnvTagsModificationSupport;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.events.EventSenderService;
import com.sequenceiq.environment.experience.common.CommonExperienceConnectorService;
import com.sequenceiq.environment.experience.common.CommonExperiencePathCreator;
import com.sequenceiq.environment.experience.config.ExperienceServicesConfig;
import com.sequenceiq.environment.experience.liftie.ExperiencesByLiftie;
import com.sequenceiq.environment.experience.liftie.LiftieConnectorService;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class ModifyUserDefinedTagsOnExperiencesHandler extends ExceptionCatcherEventHandler<EnvTagsModificationEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyUserDefinedTagsOnExperiencesHandler.class);

    @Value("${environment.experience.scan.enabled}")
    private boolean experienceScanEnabled;

    private final EntitlementService entitlementService;

    private final EnvironmentService environmentService;

    private final LiftieConnectorService liftieConnectorService;

    private final CommonExperienceConnectorService commonExperienceConnectorService;

    private final ExperienceServicesConfig experienceServicesConfig;

    private final CommonExperiencePathCreator commonExperiencePathCreator;

    private final EventSenderService eventSenderService;

    public ModifyUserDefinedTagsOnExperiencesHandler(EntitlementService entitlementService, EnvironmentService environmentService,
            LiftieConnectorService liftieConnectorService, CommonExperienceConnectorService commonExperienceConnectorService,
            ExperienceServicesConfig experienceServicesConfig, CommonExperiencePathCreator commonExperiencePathCreator,
            EventSenderService eventSenderService) {
        this.entitlementService = entitlementService;
        this.environmentService = environmentService;
        this.liftieConnectorService = liftieConnectorService;
        this.commonExperienceConnectorService = commonExperienceConnectorService;
        this.experienceServicesConfig = experienceServicesConfig;
        this.commonExperiencePathCreator = commonExperiencePathCreator;
        this.eventSenderService = eventSenderService;
    }

    @Override
    public String selector() {
        return MODIFY_USER_DEFINED_TAGS_ON_EXPERIENCES_EVENT.selector();
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvTagsModificationEvent> event) {
        EnvTagsModificationEvent data = event.getData();
        propagateTagsToExperiences(data.getResourceId(), data.getResourceCrn(), data.getUserDefinedTags(), data.getTagsToRemove());
        return EnvTagsModificationSupport.nextEvent(data, FINISH_MODIFY_USER_DEFINED_TAGS_EVENT.name());
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvTagsModificationEvent> event) {
        LOGGER.error("Unexpected failure in experience tag distribution handler.", e);
        String resourceName = event.getData().getResourceName();
        String resourceCrn = event.getData().getResourceCrn();
        return new EnvTagsModificationFailureEvent(resourceId, resourceName, resourceCrn,
                EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_FAILED, e);
    }

    private void propagateTagsToExperiences(Long resourceId, String environmentCrn, Map<String, String> userDefinedTags, Set<String> tagsToRemove) {
        if (!experienceScanEnabled) {
            LOGGER.debug("Experience tag propagation skipped: environment.experience.scan.enabled is false.");
            return;
        }
        Optional<Environment> environmentOpt = environmentService.findEnvironmentById(resourceId);
        if (environmentOpt.isEmpty()) {
            LOGGER.warn("Environment not found with ID: {}, skipping experience tag propagation.", resourceId);
            return;
        }
        Environment environment = environmentOpt.get();
        String accountId = environment.getAccountId();
        // Tag add and remove share one rollout gate: the entitlement the distribution path already used since CB-32337.
        if (!entitlementService.isExperienceDeletionEnabled(accountId)) {
            LOGGER.debug("Experience tag propagation skipped: entitlement not enabled for account {}.", accountId);
            return;
        }
        List<String> failedExperiences = new ArrayList<>();
        String operation;
        if (EnvTagsModificationSupport.hasTagsToRemove(tagsToRemove)) {
            operation = "removed tag keys: " + String.join(", ", tagsToRemove);
            failedExperiences.addAll(deleteTagsFromLiftie(environmentCrn, tagsToRemove));
            failedExperiences.addAll(deleteTagsFromCommonExperiences(environmentCrn, tagsToRemove));
        } else {
            operation = "added or updated tag keys: " + String.join(", ", userDefinedTags.keySet());
            failedExperiences.addAll(distributeTagsToLiftie(environmentCrn, userDefinedTags));
            failedExperiences.addAll(distributeTagsToCommonExperiences(environmentCrn, userDefinedTags));
        }
        notifyAboutFailedExperiences(environment, operation, failedExperiences);
    }

    private void notifyAboutFailedExperiences(Environment environment, String operation, List<String> failedExperiences) {
        if (!failedExperiences.isEmpty()) {
            try {
                eventSenderService.sendEventAndNotification(environmentService.getEnvironmentDto(environment), ThreadBasedUserCrnProvider.getUserCrn(),
                        ResourceEvent.ENVIRONMENT_USER_DEFINED_TAGS_EXPERIENCES_PROPAGATION_FAILED, List.of(operation, String.join(", ", failedExperiences)));
            } catch (Exception e) {
                LOGGER.warn("Failed to notify about the failed tag propagation to experiences {} for environment [crn: {}].",
                        failedExperiences, environment.getResourceCrn(), e);
            }
        }
    }

    private List<String> distributeTagsToLiftie(String environmentCrn, Map<String, String> tags) {
        try {
            liftieConnectorService.distributeEnvironmentTags(null, environmentCrn, tags);
            LOGGER.debug("Successfully distributed tags to Liftie for environment [crn: {}].", environmentCrn);
            return List.of();
        } catch (Exception e) {
            LOGGER.warn("Failed to distribute tags to Liftie for environment [crn: {}]. Skipping.", environmentCrn, e);
            return List.of(ExperiencesByLiftie.KUBERNETES_XP);
        }
    }

    private List<String> distributeTagsToCommonExperiences(String environmentCrn, Map<String, String> tags) {
        List<String> failedExperiences = new ArrayList<>();
        experienceServicesConfig.getConfigs().forEach(xp -> {
            if (!xp.hasEnvironmentTagsPropagation()) {
                LOGGER.debug("Experience '{}' has no environmentTagsEndpoint configured, skipping tag distribution.", xp.getName());
                return;
            }
            try {
                String path = commonExperiencePathCreator.createPathToEnvironmentTags(xp);
                commonExperienceConnectorService.distributeEnvironmentTags(path, environmentCrn, tags);
                LOGGER.debug("Successfully distributed tags to experience '{}' for environment [crn: {}].", xp.getName(), environmentCrn);
            } catch (Exception e) {
                LOGGER.warn("Failed to distribute tags to experience '{}' for environment [crn: {}]. Skipping.", xp.getName(), environmentCrn, e);
                failedExperiences.add(xp.getBusinessName());
            }
        });
        return failedExperiences;
    }

    private List<String> deleteTagsFromLiftie(String environmentCrn, Set<String> tagKeys) {
        try {
            liftieConnectorService.deleteEnvironmentTags(null, environmentCrn, tagKeys);
            LOGGER.debug("Successfully removed tags {} from Liftie for environment [crn: {}].", tagKeys, environmentCrn);
            return List.of();
        } catch (Exception e) {
            LOGGER.warn("Failed to remove tags {} from Liftie for environment [crn: {}]. Skipping.", tagKeys, environmentCrn, e);
            return List.of(ExperiencesByLiftie.KUBERNETES_XP);
        }
    }

    private List<String> deleteTagsFromCommonExperiences(String environmentCrn, Set<String> tagKeys) {
        List<String> failedExperiences = new ArrayList<>();
        experienceServicesConfig.getConfigs().forEach(xp -> {
            if (!xp.hasEnvironmentTagsPropagation()) {
                LOGGER.debug("Experience '{}' has no environmentTagsEndpoint configured, skipping tag removal.", xp.getName());
                return;
            }
            try {
                String path = commonExperiencePathCreator.createPathToEnvironmentTags(xp);
                commonExperienceConnectorService.deleteEnvironmentTags(path, environmentCrn, tagKeys);
                LOGGER.debug("Successfully removed tags {} from experience '{}' for environment [crn: {}].", tagKeys, xp.getName(), environmentCrn);
            } catch (Exception e) {
                LOGGER.warn("Failed to remove tags {} from experience '{}' for environment [crn: {}]. Skipping.", tagKeys, xp.getName(), environmentCrn, e);
                failedExperiences.add(xp.getBusinessName());
            }
        });
        return failedExperiences;
    }
}
