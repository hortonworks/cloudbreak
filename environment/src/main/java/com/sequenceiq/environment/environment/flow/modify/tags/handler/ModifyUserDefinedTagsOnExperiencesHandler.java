package com.sequenceiq.environment.environment.flow.modify.tags.handler;

import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_EXPERIENCES_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_EVENT;

import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.experience.common.CommonExperienceConnectorService;
import com.sequenceiq.environment.experience.common.CommonExperiencePathCreator;
import com.sequenceiq.environment.experience.config.ExperienceServicesConfig;
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

    public ModifyUserDefinedTagsOnExperiencesHandler(EntitlementService entitlementService, EnvironmentService environmentService,
            LiftieConnectorService liftieConnectorService, CommonExperienceConnectorService commonExperienceConnectorService,
            ExperienceServicesConfig experienceServicesConfig, CommonExperiencePathCreator commonExperiencePathCreator) {
        this.entitlementService = entitlementService;
        this.environmentService = environmentService;
        this.liftieConnectorService = liftieConnectorService;
        this.commonExperienceConnectorService = commonExperienceConnectorService;
        this.experienceServicesConfig = experienceServicesConfig;
        this.commonExperiencePathCreator = commonExperiencePathCreator;
    }

    @Override
    public String selector() {
        return MODIFY_USER_DEFINED_TAGS_ON_EXPERIENCES_EVENT.selector();
    }

    @Override
    protected Selectable doAccept(HandlerEvent<EnvTagsModificationEvent> event) {
        Long resourceId = event.getData().getResourceId();
        String resourceName = event.getData().getResourceName();
        String resourceCrn = event.getData().getResourceCrn();
        Map<String, String> userDefinedTags = event.getData().getUserDefinedTags();

        if (experienceScanEnabled) {
            Optional<Environment> environmentOpt = environmentService.findEnvironmentById(resourceId);
            if (environmentOpt.isEmpty()) {
                LOGGER.warn("Environment not found with id: {}, skipping experience tag distribution.", resourceId);
            } else {
                String accountId = environmentOpt.get().getAccountId();
                if (entitlementService.isExperienceDeletionEnabled(accountId)) {
                    distributeTagsToLiftie(resourceCrn, userDefinedTags);
                    distributeTagsToCommonExperiences(resourceCrn, userDefinedTags);
                } else {
                    LOGGER.debug("Experience tag distribution skipped: entitlement not enabled for account {}.", accountId);
                }
            }
        } else {
            LOGGER.debug("Experience tag distribution skipped: environment.experience.scan.enabled is false.");
        }

        return EnvTagsModificationEvent.builder()
                .withSelector(FINISH_MODIFY_USER_DEFINED_TAGS_EVENT.name())
                .withResourceId(resourceId)
                .withResourceName(resourceName)
                .withResourceCrn(resourceCrn)
                .withUserDefinedTags(userDefinedTags)
                .build();
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<EnvTagsModificationEvent> event) {
        LOGGER.error("Unexpected failure in experience tag distribution handler.", e);
        String resourceName = event.getData().getResourceName();
        String resourceCrn = event.getData().getResourceCrn();
        return new EnvTagsModificationFailureEvent(resourceId, resourceName, resourceCrn,
                EnvironmentStatus.USER_DEFINED_TAGS_MODIFICATION_FAILED, e);
    }

    private void distributeTagsToLiftie(String environmentCrn, Map<String, String> tags) {
        try {
            liftieConnectorService.distributeEnvironmentTags(null, environmentCrn, tags);
            LOGGER.debug("Successfully distributed tags to Liftie for environment [crn: {}].", environmentCrn);
        } catch (Exception e) {
            LOGGER.warn("Failed to distribute tags to Liftie for environment [crn: {}]. Skipping.", environmentCrn, e);
        }
    }

    private void distributeTagsToCommonExperiences(String environmentCrn, Map<String, String> tags) {
        experienceServicesConfig.getConfigs().forEach(xp -> {
            if (!xp.hasEnvironmentTagsDistribution()) {
                LOGGER.debug("Experience '{}' has no environmentTagsEndpoint configured, skipping tag distribution.", xp.getName());
                return;
            }
            try {
                String path = commonExperiencePathCreator.createPathToEnvironmentTags(xp);
                commonExperienceConnectorService.distributeEnvironmentTags(path, environmentCrn, tags);
                LOGGER.debug("Successfully distributed tags to experience '{}' for environment [crn: {}].", xp.getName(), environmentCrn);
            } catch (Exception e) {
                LOGGER.warn("Failed to distribute tags to experience '{}' for environment [crn: {}]. Skipping.", xp.getName(), environmentCrn, e);
            }
        });
    }
}
