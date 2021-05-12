package com.sequenceiq.environment.experience.liftie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.experience.ExperienceCluster;
import com.sequenceiq.environment.experience.ExperienceSource;
import com.sequenceiq.environment.experience.api.LiftieApi;
import com.sequenceiq.environment.experience.config.LiftieWorkloadsConfig;
import com.sequenceiq.environment.experience.liftie.responses.LiftieClusterView;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;
import com.sequenceiq.environment.experience.liftie.responses.PageStats;
import com.sequenceiq.environment.experience.liftie.responses.StatusMessage;
import com.sequenceiq.environment.experience.policy.response.ExperiencePolicyResponse;
import com.sequenceiq.environment.experience.policy.response.ProviderPolicyResponse;

@ExtendWith(MockitoExtension.class)
class ExperiencesByLiftieTest {

    private static final String DTO_ARG_NULL_EXCEPTION_MESSAGE = EnvironmentExperienceDto.class.getSimpleName() + " cannot be null!";

    private static final String AVAILABLE_STATUS = "AVAILABLE";

    private static final String TEST_CLOUD_PLATFORM = "AWS";

    private static final LiftieWorkload TEST_WORKLOAD = new LiftieWorkload("mon-platform", "Monitoring Clusters");

    private static final String DELETED_STATUS = "DELETED";

    private static final String TEST_TENANT = "someTenant";

    private static final String TEST_ENV_NAME = "someEnv";

    private static final String TEST_ENV_CRN = "someCrn";

    private static final String LIFTIE = "LIFTIE";

    private static final int TWICE = 2;

    private static final int ONCE = 1;

    @Mock
    private LiftieApi mockLiftieApi;

    @Mock
    private LiftieWorkloadsConfig mockWorkloadConfig;

    @Mock
    private ListClustersResponseValidator mockListClustersResponseValidator;

    private ExperiencesByLiftie underTest;

    @BeforeEach
    void setUp() {
        lenient().when(mockWorkloadConfig.getWorkloads()).thenReturn(Set.of(TEST_WORKLOAD));
        underTest = new ExperiencesByLiftie(mockWorkloadConfig, mockLiftieApi, mockListClustersResponseValidator);
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenLiftieApiListClustersReturnsEmptyThenEmptySetShouldReturn() {
        ListClustersResponse emptyResult = createEmptyListClustersResponse();
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null)).thenReturn(emptyResult);
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(emptyResult)).thenReturn(true);

        Set<ExperienceCluster> clusters = underTest.getConnectedClustersForEnvironment(createEnvironmentExperienceDto());

        assertThat(clusters).isEmpty();

        verify(mockLiftieApi, times(ONCE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null);
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(any());
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(emptyResult);
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenInputDtoIsNullThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(
                IllegalArgumentException.class, () -> underTest.getConnectedClustersForEnvironment(null));

        assertNotNull(expectedException);
        assertEquals(DTO_ARG_NULL_EXCEPTION_MESSAGE, expectedException.getMessage());
        verify(mockLiftieApi, never()).listClusters(any(), any(), any(), any());
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenOnlyOneElementReturnsButThatIsDeletedThenEmptySetShouldReturn() {
        ListClustersResponse first = createEmptyListClustersResponse();
        LiftieClusterView cluster = createClusterViewWithStatus("TestCluster1", DELETED_STATUS);
        first.setClusters(Map.of(cluster.getName(), cluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null)).thenReturn(first);

        Set<ExperienceCluster> clusters = underTest.getConnectedClustersForEnvironment(createEnvironmentExperienceDto());

        assertThat(clusters).isEmpty();

        verify(mockLiftieApi, times(ONCE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null);
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(any());
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(first);
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenOnlyOneElementReturnsAndThatIsNotDeletedThenOneItemReturns() {
        ListClustersResponse first = createEmptyListClustersResponse();
        LiftieClusterView cluster = createClusterViewWithStatus("TestCluster1", AVAILABLE_STATUS);
        first.setClusters(Map.of(cluster.getName(), cluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null)).thenReturn(first);

        Set<ExperienceCluster> clusters = underTest.getConnectedClustersForEnvironment(createEnvironmentExperienceDto());

        ExperienceCluster expected = ExperienceCluster.builder()
                .withExperienceName(LIFTIE)
                .withName("TestCluster1")
                .withStatus(AVAILABLE_STATUS)
                .build();
        assertThat(clusters).containsOnly(expected);

        verify(mockLiftieApi, times(ONCE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null);
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(any());
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(first);
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenMoreThanOneElementReturnsAndOneOfThemIsDeletedThenOneLessShouldReturn() {
        ListClustersResponse first = createEmptyListClustersResponse();
        LiftieClusterView availableCluster = createClusterViewWithStatus("availableCluster1", AVAILABLE_STATUS);
        LiftieClusterView deletedCluster = createClusterViewWithStatus("deletedCluster1", DELETED_STATUS);
        first.setClusters(Map.of(
                availableCluster.getName(), availableCluster,
                deletedCluster.getName(), deletedCluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null)).thenReturn(first);

        Set<ExperienceCluster> clusters = underTest.getConnectedClustersForEnvironment(createEnvironmentExperienceDto());

        ExperienceCluster expected = ExperienceCluster.builder()
                .withExperienceName(LIFTIE)
                .withName("availableCluster1")
                .withStatus(AVAILABLE_STATUS)
                .build();
        assertThat(clusters).containsOnly(expected);

        verify(mockLiftieApi, times(ONCE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null);
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(any());
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(first);
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenMoreThanOneElementReturnsAndAllOfThemIsAvailableThenAllShouldReturn() {
        int availableClusterQuantity = 2;
        ListClustersResponse first = createEmptyListClustersResponse();
        LiftieClusterView availableCluster = createClusterViewWithStatus("availableCluster1", AVAILABLE_STATUS);
        LiftieClusterView anotherAvailableCluster = createClusterViewWithStatus("availableCluster2", AVAILABLE_STATUS);
        first.setClusters(Map.of(
                availableCluster.getName(), availableCluster,
                anotherAvailableCluster.getName(), anotherAvailableCluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null)).thenReturn(first);

        Set<ExperienceCluster> clusters = underTest.getConnectedClustersForEnvironment(createEnvironmentExperienceDto());

        ExperienceCluster expected1 = ExperienceCluster.builder()
                .withExperienceName(LIFTIE)
                .withName("availableCluster1")
                .withStatus(AVAILABLE_STATUS)
                .build();
        ExperienceCluster expected2 = ExperienceCluster.builder()
                .withExperienceName(LIFTIE)
                .withName("availableCluster2")
                .withStatus(AVAILABLE_STATUS)
                .build();
        assertThat(clusters).containsOnly(expected1, expected2);

        verify(mockLiftieApi, times(ONCE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null);
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(any());
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(first);
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenMoreThanOneElementReturnsAndAllOfThemIsDeletedThenEmptySetShouldReturn() {
        ListClustersResponse first = createEmptyListClustersResponse();
        LiftieClusterView deletedCluster = createClusterViewWithStatus("deletedCluster1", DELETED_STATUS);
        LiftieClusterView anotherDeletedCluster = createClusterViewWithStatus("deletedCluster2", DELETED_STATUS);
        first.setClusters(Map.of(
                deletedCluster.getName(), deletedCluster,
                anotherDeletedCluster.getName(), anotherDeletedCluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null)).thenReturn(first);

        Set<ExperienceCluster> clusters = underTest.getConnectedClustersForEnvironment(createEnvironmentExperienceDto());

        assertThat(clusters).isEmpty();

        verify(mockLiftieApi, times(ONCE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null);
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(any());
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(first);
    }

    @Test
    @Disabled
    void testGetConnectedClusterCountForEnvironmentWhenLiftieApiReturnsMultiPageResultThenWeCallItAgainWithTheSpecificPageNumber() {
        PageStats ps = new PageStats();
        ps.setTotalPages(2);
        ps.setNumber(1);
        ListClustersResponse firstResponse = createEmptyListClustersResponse();
        firstResponse.setPage(ps);
        LiftieClusterView availableCluster = createClusterViewWithStatus("availableClusterOnPage1", AVAILABLE_STATUS);
        firstResponse.setClusters(Map.of(availableCluster.getName(), availableCluster));

        ListClustersResponse secondResponse = createEmptyListClustersResponse();
        LiftieClusterView availableClusterOnPage2 = createClusterViewWithStatus("availableClusterOnPage2", AVAILABLE_STATUS);
        firstResponse.setClusters(Map.of(availableClusterOnPage2.getName(), availableClusterOnPage2));

        when(mockListClustersResponseValidator.isListClustersResponseEmpty(firstResponse)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null)).thenReturn(firstResponse);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), 2)).thenReturn(secondResponse);

        underTest.getConnectedClustersForEnvironment(createEnvironmentExperienceDto());

        verify(mockLiftieApi, times(TWICE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null);
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), 2);
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(any());
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(firstResponse);
    }

    @Test
    void testDeleteConnectedExperiencesWhenInputDtoIsNullThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(
                IllegalArgumentException.class, () -> underTest.deleteConnectedExperiences(null));

        assertNotNull(expectedException);
        assertEquals(DTO_ARG_NULL_EXCEPTION_MESSAGE, expectedException.getMessage());

        verify(mockLiftieApi, never()).deleteCluster(any());
    }

    @Test
    void testDeleteConnectedExperiencesWhenOneClusterIsAvailableTheOneDeletionShouldHappen() {
        ListClustersResponse first = createEmptyListClustersResponse();
        LiftieClusterView cluster = createClusterViewWithStatus("TestCluster1", AVAILABLE_STATUS);
        first.setClusters(Map.of(cluster.getName(), cluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null)).thenReturn(first);

        underTest.deleteConnectedExperiences(createEnvironmentExperienceDto());

        verify(mockLiftieApi, times(ONCE)).deleteCluster(any());
        verify(mockLiftieApi, times(ONCE)).deleteCluster(cluster.getClusterId());
    }

    @Test
    void testDeleteConnectedExperiencesWhenMoreClusterIsAvailableTheThatExactAmountDeletionShouldHappen() {
        ListClustersResponse first = createEmptyListClustersResponse();
        LiftieClusterView availableCluster = createClusterViewWithStatus("availableCluster1", AVAILABLE_STATUS);
        LiftieClusterView anotherAvailableCluster = createClusterViewWithStatus("availableCluster2", AVAILABLE_STATUS);
        first.setClusters(Map.of(
                availableCluster.getName(), availableCluster,
                anotherAvailableCluster.getName(), anotherAvailableCluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null)).thenReturn(first);

        underTest.deleteConnectedExperiences(createEnvironmentExperienceDto());

        verify(mockLiftieApi, times(TWICE)).deleteCluster(any());
        verify(mockLiftieApi, times(ONCE)).deleteCluster(availableCluster.getClusterId());
        verify(mockLiftieApi, times(ONCE)).deleteCluster(anotherAvailableCluster.getClusterId());
    }

    @Test
    void testDeleteConnectedExperiencesWhenMoreClusterIsComesBackFromListingButOnlyOneIsAvailableThatOneDeletionShouldHappen() {
        ListClustersResponse first = createEmptyListClustersResponse();
        LiftieClusterView availableCluster = createClusterViewWithStatus("availableCluster1", AVAILABLE_STATUS);
        LiftieClusterView notAvailableCluster = createClusterViewWithStatus("notAvailableCluster2", DELETED_STATUS);
        first.setClusters(Map.of(
                availableCluster.getName(), availableCluster,
                notAvailableCluster.getName(), notAvailableCluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, TEST_WORKLOAD.getName(), null)).thenReturn(first);

        underTest.deleteConnectedExperiences(createEnvironmentExperienceDto());

        verify(mockLiftieApi, times(ONCE)).deleteCluster(any());
        verify(mockLiftieApi, times(ONCE)).deleteCluster(availableCluster.getClusterId());
        verify(mockLiftieApi, never()).deleteCluster(notAvailableCluster.getClusterId());
    }

    @Test
    void testGetSourceReturnsLiftieType() {
        ExperienceSource expectedSource = underTest.getSource();

        assertEquals(expectedSource, ExperienceSource.LIFTIE);
    }

    @Test
    void testCollectPolicy() {
        String liftiePolicyKey = "Kubernetes cluster manager";
        ExperiencePolicyResponse fetchedPolicy = new ExperiencePolicyResponse();
        fetchedPolicy.setAws(new ProviderPolicyResponse("somePolicyForAws"));
        EnvironmentExperienceDto environmentExperienceDto = createEnvironmentExperienceDto();
        when(mockLiftieApi.getPolicy(environmentExperienceDto.getCloudPlatform())).thenReturn(fetchedPolicy);

        Map<String, String> result = underTest.collectPolicy(environmentExperienceDto);

        assertNotNull(result);
        assertTrue(result.containsKey(liftiePolicyKey));
        assertEquals(fetchedPolicy.getAws().getPolicy(), result.get(liftiePolicyKey));
    }

    private ListClustersResponse createEmptyListClustersResponse() {
        ListClustersResponse empty = new ListClustersResponse();
        PageStats ps = new PageStats();
        ps.setTotalPages(0);
        empty.setPage(ps);
        return empty;
    }

    private EnvironmentExperienceDto createEnvironmentExperienceDto() {
        return new EnvironmentExperienceDto.Builder()
                .withCloudPlatform(TEST_CLOUD_PLATFORM)
                .withAccountId(TEST_TENANT)
                .withName(TEST_ENV_NAME)
                .withCrn(TEST_ENV_CRN)
                .build();
    }

    private LiftieClusterView createClusterViewWithStatus(String name, String status) {
        LiftieClusterView cv = new LiftieClusterView();
        cv.setClusterId(name + "-clusterId-" + new Date().getTime());
        cv.setName(name);
        cv.setClusterStatus(createStatusMessage(status));
        return cv;
    }

    private StatusMessage createStatusMessage(String status) {
        StatusMessage sm = new StatusMessage();
        sm.setStatus(status);
        return sm;
    }

}
