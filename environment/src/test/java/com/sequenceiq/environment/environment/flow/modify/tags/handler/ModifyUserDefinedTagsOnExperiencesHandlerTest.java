package com.sequenceiq.environment.environment.flow.modify.tags.handler;

import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationHandlerSelectors.MODIFY_USER_DEFINED_TAGS_ON_EXPERIENCES_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FAILED_MODIFY_USER_DEFINED_TAGS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationStateSelectors.FINISH_MODIFY_USER_DEFINED_TAGS_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.tags.event.EnvTagsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.experience.common.CommonExperience;
import com.sequenceiq.environment.experience.common.CommonExperienceConnectorService;
import com.sequenceiq.environment.experience.common.CommonExperiencePathCreator;
import com.sequenceiq.environment.experience.config.ExperienceServicesConfig;
import com.sequenceiq.environment.experience.liftie.LiftieConnectorService;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@ExtendWith(MockitoExtension.class)
class ModifyUserDefinedTagsOnExperiencesHandlerTest {

    private static final long ENV_ID = 1L;

    private static final String ENV_NAME = "envName";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:account:environment:envId";

    private static final String ACCOUNT_ID = "account";

    private static final Map<String, String> USER_DEFINED_TAGS = Map.of("custom", "value");

    private static final String EXPERIENCE_PATH = "http://experience:8080/api/v1/environments";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private LiftieConnectorService liftieConnectorService;

    @Mock
    private CommonExperienceConnectorService commonExperienceConnectorService;

    @Mock
    private ExperienceServicesConfig experienceServicesConfig;

    @Mock
    private CommonExperiencePathCreator commonExperiencePathCreator;

    @InjectMocks
    private ModifyUserDefinedTagsOnExperiencesHandler underTest;

    private HandlerEvent<EnvTagsModificationEvent> event;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "experienceScanEnabled", true);
        EnvTagsModificationEvent request = EnvTagsModificationEvent.builder()
                .withSelector(MODIFY_USER_DEFINED_TAGS_ON_EXPERIENCES_EVENT.selector())
                .withResourceId(ENV_ID)
                .withResourceName(ENV_NAME)
                .withResourceCrn(ENV_CRN)
                .withUserDefinedTags(USER_DEFINED_TAGS)
                .build();
        event = new HandlerEvent<>(new Event<>(request));
    }

    @Test
    @DisplayName("When the handler selector is requested, then the experiences tag modification selector is returned")
    void testWhenSelectorIsRequestedThenModifyUserDefinedTagsOnExperiencesEventSelectorIsReturned() {
        assertEquals(MODIFY_USER_DEFINED_TAGS_ON_EXPERIENCES_EVENT.selector(), underTest.selector());
    }

    @Test
    @DisplayName("When experience tag distribution succeeds, then both services receive the tags and a finish event is returned")
    void testWhenExperienceTagDistributionSucceedsThenTagsAreDistributedAndFinishEventIsReturned() {
        Environment environment = new Environment();
        environment.setAccountId(ACCOUNT_ID);
        when(environmentService.findEnvironmentById(ENV_ID)).thenReturn(Optional.of(environment));
        when(entitlementService.isExperienceDeletionEnabled(ACCOUNT_ID)).thenReturn(true);
        CommonExperience xp = new CommonExperience();
        xp.setName("testXp");
        xp.setEnvironmentTagsEndpoint("/env/{environmentCrn}/tags");
        when(experienceServicesConfig.getConfigs()).thenReturn(List.of(xp));
        when(commonExperiencePathCreator.createPathToEnvironmentTags(xp)).thenReturn(EXPERIENCE_PATH);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvTagsModificationEvent.class, result);
        assertEquals(FINISH_MODIFY_USER_DEFINED_TAGS_EVENT.name(), result.getSelector());
        verify(liftieConnectorService).distributeEnvironmentTags(null, ENV_CRN, USER_DEFINED_TAGS);
        verify(commonExperienceConnectorService).distributeEnvironmentTags(EXPERIENCE_PATH, ENV_CRN, USER_DEFINED_TAGS);
    }

    @Test
    @DisplayName("When experience scanning is disabled, then tag distribution is skipped and a finish event is returned")
    void testWhenExperienceScanIsDisabledThenDistributionIsSkippedAndFinishEventIsReturned() {
        ReflectionTestUtils.setField(underTest, "experienceScanEnabled", false);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvTagsModificationEvent.class, result);
        assertEquals(FINISH_MODIFY_USER_DEFINED_TAGS_EVENT.name(), result.getSelector());
        verifyNoInteractions(liftieConnectorService, commonExperienceConnectorService);
    }

    @Test
    @DisplayName("When the experience deletion entitlement is disabled, then tag distribution is skipped and a finish event is returned")
    void testWhenExperienceDeletionEntitlementIsDisabledThenDistributionIsSkippedAndFinishEventIsReturned() {
        Environment environment = new Environment();
        environment.setAccountId(ACCOUNT_ID);
        when(environmentService.findEnvironmentById(ENV_ID)).thenReturn(Optional.of(environment));
        when(entitlementService.isExperienceDeletionEnabled(ACCOUNT_ID)).thenReturn(false);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvTagsModificationEvent.class, result);
        assertEquals(FINISH_MODIFY_USER_DEFINED_TAGS_EVENT.name(), result.getSelector());
        verifyNoInteractions(liftieConnectorService, commonExperienceConnectorService);
    }

    @Test
    @DisplayName("When the environment cannot be found, then tag distribution is skipped and a finish event is returned")
    void testWhenEnvironmentIsNotFoundThenDistributionIsSkippedAndFinishEventIsReturned() {
        when(environmentService.findEnvironmentById(ENV_ID)).thenReturn(Optional.empty());

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvTagsModificationEvent.class, result);
        assertEquals(FINISH_MODIFY_USER_DEFINED_TAGS_EVENT.name(), result.getSelector());
        verifyNoInteractions(liftieConnectorService, commonExperienceConnectorService);
    }

    @Test
    @DisplayName("When Liftie tag distribution fails, then the best-effort operation still returns a finish event")
    void testWhenLiftieTagDistributionFailsThenFinishEventIsReturned() {
        Environment environment = new Environment();
        environment.setAccountId(ACCOUNT_ID);
        when(environmentService.findEnvironmentById(ENV_ID)).thenReturn(Optional.of(environment));
        when(entitlementService.isExperienceDeletionEnabled(ACCOUNT_ID)).thenReturn(true);
        doThrow(new RuntimeException("Liftie unavailable")).when(liftieConnectorService)
                .distributeEnvironmentTags(any(), anyString(), any());
        when(experienceServicesConfig.getConfigs()).thenReturn(List.of());

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvTagsModificationEvent.class, result);
        assertEquals(FINISH_MODIFY_USER_DEFINED_TAGS_EVENT.name(), result.getSelector());
    }

    @Test
    @DisplayName("When common experience tag distribution fails, then the best-effort operation still returns a finish event")
    void testWhenCommonExperienceTagDistributionFailsThenFinishEventIsReturned() {
        Environment environment = new Environment();
        environment.setAccountId(ACCOUNT_ID);
        when(environmentService.findEnvironmentById(ENV_ID)).thenReturn(Optional.of(environment));
        when(entitlementService.isExperienceDeletionEnabled(ACCOUNT_ID)).thenReturn(true);
        CommonExperience xp = new CommonExperience();
        xp.setName("failingXp");
        xp.setEnvironmentTagsEndpoint("/env/{environmentCrn}/tags");
        when(experienceServicesConfig.getConfigs()).thenReturn(List.of(xp));
        when(commonExperiencePathCreator.createPathToEnvironmentTags(xp)).thenReturn(EXPERIENCE_PATH);
        doThrow(new RuntimeException("Connection refused")).when(commonExperienceConnectorService)
                .distributeEnvironmentTags(anyString(), anyString(), any());

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvTagsModificationEvent.class, result);
        assertEquals(FINISH_MODIFY_USER_DEFINED_TAGS_EVENT.name(), result.getSelector());
    }

    @Test
    @DisplayName("When a default failure event is created, then a modification failure event with the failed selector is returned")
    void testWhenDefaultFailureEventIsCreatedThenFailureEventWithFailedSelectorIsReturned() {
        Event<EnvTagsModificationEvent> rawEvent = new Event<>(event.getData());
        Selectable result = underTest.defaultFailureEvent(ENV_ID, new RuntimeException("unexpected"), rawEvent);

        assertInstanceOf(EnvTagsModificationFailureEvent.class, result);
        assertEquals(FAILED_MODIFY_USER_DEFINED_TAGS_EVENT.name(), result.getSelector());
    }
}
