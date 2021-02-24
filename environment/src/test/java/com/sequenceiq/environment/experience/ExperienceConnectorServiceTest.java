package com.sequenceiq.environment.experience;

import static com.sequenceiq.environment.experience.ExperienceSource.BASIC;
import static com.sequenceiq.environment.experience.ExperienceSource.LIFTIE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;

@ExtendWith(MockitoExtension.class)
class ExperienceConnectorServiceTest {

    private static final int ONCE = 1;

    private static final String TEST_ENV_CRN = "someTestEnvCrn";

    private static final String TEST_ENV_NAME = "someTestEnvName";

    private static final String TEST_ACCOUNT_ID = "someTestAccountId";

    private static final String NULL_ENV_DTO_EXCEPTION_MSG = "environment should not be null!";

    private List<Experience> mockExperiences = List.of(createMockExperience(LIFTIE), createMockExperience(BASIC));

    private ExperienceConnectorService underTest;

    @Mock
    private EntitlementService entitlementServiceMock;

    @BeforeEach
    void setUp() {
        underTest = new ExperienceConnectorService(mockExperiences, entitlementServiceMock);
    }

    @Test
    void testWhenScanIsNotEnabledThenNoExperienceCallHappensAndZeroShouldReturn() {
        EnvironmentExperienceDto dto = createEnvironmentExperienceDto();

        when(entitlementServiceMock.isExperienceDeletionEnabled(dto.getAccountId())).thenReturn(false);

        int result = underTest.getConnectedExperienceCount(dto);

        assertEquals(0, result);
        mockExperiences.forEach(experience -> verify(experience, never()).getConnectedClustersForEnvironment(any(EnvironmentExperienceDto.class)));
    }

    @Test
    void testWhenNoExperienceHasConfiguredThenZeroShouldReturn() {
        EnvironmentExperienceDto dto = createEnvironmentExperienceDto();
        underTest = new ExperienceConnectorService(new ArrayList<>(), entitlementServiceMock);

        when(entitlementServiceMock.isExperienceDeletionEnabled(dto.getAccountId())).thenReturn(true);

        int result = underTest.getConnectedExperienceCount(dto);

        assertEquals(0, result);
        mockExperiences.forEach(xp -> verify(xp, never()).getConnectedClustersForEnvironment(any(EnvironmentExperienceDto.class)));
    }

    @Test
    void testWhenExperienceCheckingIsEnabledAndEachExperienceHasClusterForTheGivenEnvThenUnifiedSetShouldReturn() {
        int connectedClusterQuantity = 1;
        EnvironmentExperienceDto dto = createEnvironmentExperienceDto();

        mockExperiences.forEach(xp -> {
            Set<ExperienceCluster> cluster = Set.of(ExperienceCluster.builder()
                    .withExperienceName(xp.getSource().name())
                    .withName("Workload")
                    .withStatus("AVAILABLE")
                    .build());
            when(xp.getConnectedClustersForEnvironment(dto)).thenReturn(cluster);
        });

        when(entitlementServiceMock.isExperienceDeletionEnabled(dto.getAccountId())).thenReturn(true);

        int result = underTest.getConnectedExperienceCount(dto);

        assertEquals(mockExperiences.size() * connectedClusterQuantity, result);
    }

    @Test
    void testGetConnectedExperienceCountWhenProvidedDtoIsNullThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(IllegalArgumentException.class, () -> underTest.getConnectedExperienceCount(null));

        assertNotNull(expectedException);
        assertEquals(NULL_ENV_DTO_EXCEPTION_MSG, expectedException.getMessage());
    }

    @Test
    void testDeleteConnectedExperiencesWhenProvidedDtoIsNullThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(IllegalArgumentException.class, () -> underTest.deleteConnectedExperiences(null));

        assertNotNull(expectedException);
        assertEquals(NULL_ENV_DTO_EXCEPTION_MSG, expectedException.getMessage());
    }

    @Test
    void testDeleteConnectedExperiencesWhenInputIsValidThenAllExperienceShouldHaveAcceptDeletion() {
        EnvironmentExperienceDto dto = createEnvironmentExperienceDto();

        underTest.deleteConnectedExperiences(dto);

        mockExperiences.forEach(xp -> {
            verify(xp, times(ONCE)).deleteConnectedExperiences(any(EnvironmentExperienceDto.class));
            verify(xp, times(ONCE)).deleteConnectedExperiences(dto);
        });
    }

    private EnvironmentExperienceDto createEnvironmentExperienceDto() {
        return new EnvironmentExperienceDto.Builder()
                .withAccountId(TEST_ACCOUNT_ID)
                .withName(TEST_ENV_NAME)
                .withCrn(TEST_ENV_CRN)
                .build();
    }

    private Experience createMockExperience(ExperienceSource type) {
        Experience xp = mock(Experience.class);
        when(xp.getSource()).thenReturn(type);
        return xp;
    }

}
