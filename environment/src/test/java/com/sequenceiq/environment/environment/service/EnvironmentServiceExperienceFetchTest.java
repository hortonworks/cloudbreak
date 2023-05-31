package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static java.util.Collections.emptySet;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.environment.repository.EnvironmentRepository;
import com.sequenceiq.environment.experience.ExperienceCluster;
import com.sequenceiq.environment.experience.ExperienceConnectorService;

@ExtendWith(MockitoExtension.class)
class EnvironmentServiceExperienceFetchTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

    private static final String TEST_ENV_NAME = "testEnvironmentName";

    private static final String TEST_ENV_CRN = "testEnvironmentCrn";

    private static final String LIFTIE_XP_PUBLIC_NAME = "Kubernetes Experience";

    private static final String NON_LIFTIE_XP_PUBLIC_NAME = "some other experience";

    @Mock
    private ExperienceConnectorService mockExperienceConnectorService;

    @Mock
    private Environment mockEnvironment;

    @Mock
    private EnvironmentDto mockEnvironmentDto;

    @Mock
    private EnvironmentRepository mockEnvironmentRepository;

    @Mock
    private EnvironmentDtoConverter mockEnvironmentDtoConverter;

    @InjectMocks
    private EnvironmentService underTest;

    @BeforeEach
    void setUp() {
        when(mockEnvironmentDtoConverter.environmentToDto(mockEnvironment)).thenReturn(mockEnvironmentDto);
    }

    @AfterEach
    void tearDown() {
        verify(mockExperienceConnectorService, times(1)).getConnectedExperiences(any(EnvironmentExperienceDto.class));
        verifyNoMoreInteractions(mockExperienceConnectorService);
    }

    @Test
    void testFetchWithNameWhenNoExperienceReturnsFromConnectorServiceThenEmptyMapShouldReturn() {
        when(mockExperienceConnectorService.getConnectedExperiences(any())).thenReturn(emptySet());
        when(mockEnvironmentRepository.findByNameAndAccountIdAndArchivedIsFalse(eq(TEST_ENV_NAME), any())).thenReturn(Optional.of(mockEnvironment));

        Map<String, Set<String>> result = doAs(USER_CRN, () -> underTest.collectExperiences(NameOrCrn.ofName(TEST_ENV_NAME)));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(mockEnvironmentRepository, times(1)).findByNameAndAccountIdAndArchivedIsFalse(eq(TEST_ENV_NAME), any());
        verifyNoMoreInteractions(mockEnvironmentRepository);
    }

    @Test
    void testWithNameWhenOneExperienceReturnsThenOneEntryHasToBePresentInResultMap() {
        Set<ExperienceCluster> attachedClusters = createClusters(LIFTIE_XP_PUBLIC_NAME, 3);

        when(mockExperienceConnectorService.getConnectedExperiences(any())).thenReturn(attachedClusters);
        when(mockEnvironmentRepository.findByNameAndAccountIdAndArchivedIsFalse(eq(TEST_ENV_NAME), any())).thenReturn(Optional.of(mockEnvironment));

        Map<String, Set<String>> result = doAs(USER_CRN, () -> underTest.collectExperiences(NameOrCrn.ofName(TEST_ENV_NAME)));

        assertNotNull(result);
        assertEquals(1, result.keySet().size());
        result.forEach((xpName, clusters) -> assertEquals(attachedClusters.size(), clusters.size()));

        verify(mockEnvironmentRepository, times(1)).findByNameAndAccountIdAndArchivedIsFalse(eq(TEST_ENV_NAME), any());
        verifyNoMoreInteractions(mockEnvironmentRepository);
    }

    @Test
    void testWithNameWhenOneExperienceReturnsThenMoreEntryHasToBePresentInResultMap() {
        Set<ExperienceCluster> liftieClusters = createClusters(LIFTIE_XP_PUBLIC_NAME, 3);
        Set<ExperienceCluster> nonLiftieClusters = createClusters(NON_LIFTIE_XP_PUBLIC_NAME, 2);
        Set<ExperienceCluster> attachedClusters = new HashSet<>(2);
        attachedClusters.addAll(liftieClusters);
        attachedClusters.addAll(nonLiftieClusters);

        when(mockExperienceConnectorService.getConnectedExperiences(any())).thenReturn(attachedClusters);
        when(mockEnvironmentRepository.findByNameAndAccountIdAndArchivedIsFalse(eq(TEST_ENV_NAME), any())).thenReturn(Optional.of(mockEnvironment));

        Map<String, Set<String>> result = doAs(USER_CRN, () -> underTest.collectExperiences(NameOrCrn.ofName(TEST_ENV_NAME)));

        assertNotNull(result);
        assertEquals(2, result.keySet().size());

        result.forEach((xpName, strings) -> {
            switch (xpName) {
            case LIFTIE_XP_PUBLIC_NAME:
                assertEquals(liftieClusters.size(), strings.size());
                break;
            case NON_LIFTIE_XP_PUBLIC_NAME:
                assertEquals(nonLiftieClusters.size(), strings.size());
                break;
            default:
                throw new IllegalStateException("Invalid case!");
            }
        });

        verify(mockEnvironmentRepository, times(1)).findByNameAndAccountIdAndArchivedIsFalse(eq(TEST_ENV_NAME), any());
        verifyNoMoreInteractions(mockEnvironmentRepository);
    }

    @Test
    void testFetchWithCrnWhenNoExperienceReturnsFromConnectorServiceThenEmptyMapShouldReturn() {
        when(mockExperienceConnectorService.getConnectedExperiences(any())).thenReturn(emptySet());
        when(mockEnvironmentRepository.findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TEST_ENV_CRN), any())).thenReturn(Optional.of(mockEnvironment));

        Map<String, Set<String>> result = doAs(USER_CRN, () -> underTest.collectExperiences(NameOrCrn.ofCrn(TEST_ENV_CRN)));

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(mockEnvironmentRepository, times(1)).findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TEST_ENV_CRN), any());
        verifyNoMoreInteractions(mockEnvironmentRepository);
    }

    @Test
    void testWithCrnWhenOneExperienceReturnsThenOneEntryHasToBePresentInResultMap() {
        Set<ExperienceCluster> attachedClusters = createClusters(LIFTIE_XP_PUBLIC_NAME, 3);

        when(mockExperienceConnectorService.getConnectedExperiences(any())).thenReturn(attachedClusters);
        when(mockEnvironmentRepository.findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TEST_ENV_CRN), any())).thenReturn(Optional.of(mockEnvironment));

        Map<String, Set<String>> result = doAs(USER_CRN, () -> underTest.collectExperiences(NameOrCrn.ofCrn(TEST_ENV_CRN)));

        assertNotNull(result);
        assertEquals(1, result.keySet().size());
        result.forEach((xpName, clusters) -> assertEquals(attachedClusters.size(), clusters.size()));

        verify(mockEnvironmentRepository, times(1)).findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TEST_ENV_CRN), any());
        verifyNoMoreInteractions(mockEnvironmentRepository);
    }

    @Test
    void testWithCrnWhenOneExperienceReturnsThenMoreEntryHasToBePresentInResultMap() {
        Set<ExperienceCluster> liftieClusters = createClusters(LIFTIE_XP_PUBLIC_NAME, 3);
        Set<ExperienceCluster> nonLiftieClusters = createClusters(NON_LIFTIE_XP_PUBLIC_NAME, 2);
        Set<ExperienceCluster> attachedClusters = new HashSet<>(2);
        attachedClusters.addAll(liftieClusters);
        attachedClusters.addAll(nonLiftieClusters);

        when(mockExperienceConnectorService.getConnectedExperiences(any())).thenReturn(attachedClusters);
        when(mockEnvironmentRepository.findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TEST_ENV_CRN), any())).thenReturn(Optional.of(mockEnvironment));

        Map<String, Set<String>> result = doAs(USER_CRN, () -> underTest.collectExperiences(NameOrCrn.ofCrn(TEST_ENV_CRN)));

        assertNotNull(result);
        assertEquals(2, result.keySet().size());

        result.forEach((xpName, strings) -> {
            switch (xpName) {
            case LIFTIE_XP_PUBLIC_NAME:
                assertEquals(liftieClusters.size(), strings.size());
                break;
            case NON_LIFTIE_XP_PUBLIC_NAME:
                assertEquals(nonLiftieClusters.size(), strings.size());
                break;
            default:
                throw new IllegalStateException("Invalid case!");
            }
        });

        verify(mockEnvironmentRepository, times(1)).findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TEST_ENV_CRN), any());
        verifyNoMoreInteractions(mockEnvironmentRepository);
    }

    private Set<ExperienceCluster> createClusters(String publicName, int count) {
        Set<ExperienceCluster> clusters = new HashSet<>(count);
        for (int i = 0; i < count; i++) {
            clusters.add(ExperienceCluster.builder()
                    .withPublicName(publicName)
                    .withName("cluster-" + i + '-' + publicName)
                    .withExperienceName("experience_" + publicName)
                    .withStatus("AVAILABLE")
                    .withStatusReason("BECAUSE")
                    .build());
        }
        return clusters;
    }

}