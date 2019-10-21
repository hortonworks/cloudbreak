package com.sequenceiq.environment.environment.experience.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.environment.environment.experience.resolve.Experience;

class XServiceTest {

    private static final String PATH_POSTFIX = "/environment/{crn}";

    private static final String ENVIRONMENT_CRN = "somecrn";

    private static final String EXPERIENCE_PROTOCOL = "https";

    @Mock
    private ExperienceConnectorService experienceConnectorService;

    @Mock
    private ExperienceValidator experienceValidator;

    private XService underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    void testWhenEnvironmentCrnIsNullThenIllegalArgumentExceptionComes() {
        underTest = new XService(EXPERIENCE_PROTOCOL, PATH_POSTFIX, experienceConnectorService, new XPServices(), experienceValidator);

        Assertions.assertThrows(IllegalArgumentException.class, () -> underTest.environmentHasActiveExperience(null));
    }

    @Test
    void testWhenEnvironmentCrnIsEmptyThenIllegalArgumentExceptionComes() {
        underTest = new XService(EXPERIENCE_PROTOCOL, PATH_POSTFIX, experienceConnectorService, new XPServices(), experienceValidator);

        Assertions.assertThrows(IllegalArgumentException.class, () -> underTest.environmentHasActiveExperience(""));
    }

    @Test
    void testIfExperiencesNotSpecifiedThenEmptySetShouldReturnWithoutRemoteCallAttempt() {
        underTest = new XService(EXPERIENCE_PROTOCOL, PATH_POSTFIX, experienceConnectorService, new XPServices(), experienceValidator);

        Set<String> result = underTest.environmentHasActiveExperience(ENVIRONMENT_CRN);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());

        verify(experienceConnectorService, times(0)).getWorkspaceNamesConnectedToEnv(anyString(), anyString());
    }

    @Test
    void testIfExperienceIsGivenAndItHasNoAttachedWorkspaceToTheGivenEnvironmentThenEmptySetShouldReturn() {
        when(experienceValidator.isExperienceFilled(any())).thenReturn(true);
        String experienceName = "SomeGreatExperience";
        XPServices xp = createXPServices(experienceName);
        underTest = new XService(EXPERIENCE_PROTOCOL, PATH_POSTFIX, experienceConnectorService, xp, experienceValidator);
        when(experienceConnectorService.getWorkspaceNamesConnectedToEnv("https://" + xp.getExperiences().get(experienceName).getPathPrefix() +
                xp.getExperiences().get(experienceName).getPathInfix() + PATH_POSTFIX, ENVIRONMENT_CRN)).thenReturn(Collections.emptySet());

        Set<String> result = underTest.environmentHasActiveExperience(ENVIRONMENT_CRN);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(0L, result.size());

        verify(experienceConnectorService, times(1)).getWorkspaceNamesConnectedToEnv(anyString(), anyString());
    }

    @Test
    void testIfExperienceIsGivenAndItHasAttachedWorkspaceToTheGivenEnvironmentThenSetShouldReturnWithTheNameOfTheGivenExperience() {
        String experienceNameConnectedToEnv = "SomeGreatExperience";
        String experienceNameWithoutEnv = "SomeGreatExperience";
        XPServices xp = createXPServices(experienceNameConnectedToEnv, experienceNameWithoutEnv);
        when(experienceValidator.isExperienceFilled(any())).thenReturn(true);
        underTest = new XService(EXPERIENCE_PROTOCOL, PATH_POSTFIX, experienceConnectorService, xp, experienceValidator);
        String pathToResponse = EXPERIENCE_PROTOCOL + "://" + xp.getExperiences().get(experienceNameConnectedToEnv).getPathPrefix() + ":" + xp.getExperiences()
                .get(experienceNameConnectedToEnv).getPort() + xp.getExperiences().get(experienceNameConnectedToEnv).getPathInfix() + PATH_POSTFIX;
        when(experienceConnectorService.getWorkspaceNamesConnectedToEnv(pathToResponse, ENVIRONMENT_CRN)).thenReturn(Set.of(experienceNameConnectedToEnv));

        Set<String> result = underTest.environmentHasActiveExperience(ENVIRONMENT_CRN);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1L, result.size());
        Assertions.assertTrue(new ArrayList<>(result).get(0).equalsIgnoreCase(experienceNameConnectedToEnv));

        verify(experienceConnectorService, times(1)).getWorkspaceNamesConnectedToEnv(anyString(), anyString());
    }

    @Test
    void testIfOneOfTheExperiencesHasNotFilledProperlyThenItShouldNotBeProcessed() {
        String experienceName = "SomeGreatExperience";
        XPServices xp = createXPServices(experienceName);
        when(experienceValidator.isExperienceFilled(any())).thenReturn(false);
        when(experienceConnectorService.getWorkspaceNamesConnectedToEnv(anyString(), eq(ENVIRONMENT_CRN))).thenReturn(Set.of("something"));
        underTest = new XService(EXPERIENCE_PROTOCOL, PATH_POSTFIX, experienceConnectorService, xp, experienceValidator);

        Set<String> result = underTest.environmentHasActiveExperience(ENVIRONMENT_CRN);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(0L, result.size());

        verify(experienceConnectorService, times(0)).getWorkspaceNamesConnectedToEnv(anyString(), anyString());
    }

    private XPServices createXPServices(String... names) {
        XPServices xpServices = new XPServices();
        xpServices.setExperiences(createExperienceWithName(names));
        return xpServices;
    }

    private Map<String, Experience> createExperienceWithName(String... names) {
        Map<String, Experience> experiences = new LinkedHashMap<>(names.length);
        int lastDigit = 1;
        for (String name : names) {
            Experience xp = new Experience();
            xp.setPathInfix("/ml-" + name);
            xp.setPathPrefix("127.0.0." + lastDigit);
            lastDigit++;
            xp.setPort("1234");
            experiences.put(name, xp);
        }
        return experiences;
    }

}