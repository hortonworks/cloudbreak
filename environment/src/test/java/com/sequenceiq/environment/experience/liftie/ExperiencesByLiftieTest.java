package com.sequenceiq.environment.experience.liftie;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.environment.environment.dto.EnvironmentExperienceDto;
import com.sequenceiq.environment.experience.ExperienceSource;
import com.sequenceiq.environment.experience.api.LiftieApi;
import com.sequenceiq.environment.experience.liftie.responses.ClusterView;
import com.sequenceiq.environment.experience.liftie.responses.ListClustersResponse;
import com.sequenceiq.environment.experience.liftie.responses.PageStats;
import com.sequenceiq.environment.experience.liftie.responses.StatusMessage;

class ExperiencesByLiftieTest {

    private static final String DTO_ARG_NULL_EXCEPTION_MESSAGE = EnvironmentExperienceDto.class.getSimpleName() + " cannot be null!";

    private static final String AVAILABLE_STATUS = "AVAILABLE";

    private static final String TEST_WORKLOAD = "mon-platform";

    private static final String DELETED_STATUS = "DELETED";

    private static final String TEST_TENANT = "someTenant";

    private static final String TEST_ENV_NAME = "someEnv";

    private static final String TEST_ENV_CRN = "someCrn";

    private static final int TWICE = 2;

    private static final int ONCE = 1;

    @Mock
    private LiftieApi mockLiftieApi;

    @Mock
    private ListClustersResponseValidator mockListClustersResponseValidator;

    @Mock
    private ExperienceIndependentLiftieClusterWorkloadProvider mockWorkloadProvider;

    private ExperiencesByLiftie underTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        underTest = new ExperiencesByLiftie(mockWorkloadProvider, mockLiftieApi, mockListClustersResponseValidator);
        when(mockWorkloadProvider.getWorkloadsLabels()).thenReturn(Set.of(TEST_WORKLOAD));
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenLiftieApiListClustersReturnsEmptyThenZeroShouldReturn() {
        ListClustersResponse emptyResult = createEmptyListClustersResponse();
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD)).thenReturn(emptyResult);
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(emptyResult)).thenReturn(true);

        int result = underTest.getConnectedClusterCountForEnvironment(createEnvironmentExperienceDto());

        assertEquals(0, result);

        verify(mockLiftieApi, times(ONCE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD);
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(any());
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(emptyResult);
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenInputDtoIsNullThenIllegalArgumentExceptionShouldCome() {
        IllegalArgumentException expectedException = assertThrows(
                IllegalArgumentException.class, () -> underTest.getConnectedClusterCountForEnvironment(null));

        assertNotNull(expectedException);
        assertEquals(DTO_ARG_NULL_EXCEPTION_MESSAGE, expectedException.getMessage());
        verify(mockLiftieApi, never()).listClusters(any(), any(), any(), any());
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenOnlyOneElementReturnsButThatIsDeletedThenZeroShouldReturn() {
        ListClustersResponse first = createEmptyListClustersResponse();
        ClusterView cluster = createClusterViewWithStatus("TestCluster1", DELETED_STATUS);
        first.setClusters(Map.of(cluster.getName(), cluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD)).thenReturn(first);

        int result = underTest.getConnectedClusterCountForEnvironment(createEnvironmentExperienceDto());

        assertEquals(0, result);

        verify(mockLiftieApi, times(ONCE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD);
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(any());
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(first);
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenOnlyOneElementReturnsAndThatIsNotDeletedThenOneShouldReturn() {
        ListClustersResponse first = createEmptyListClustersResponse();
        ClusterView cluster = createClusterViewWithStatus("TestCluster1", AVAILABLE_STATUS);
        first.setClusters(Map.of(cluster.getName(), cluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD)).thenReturn(first);

        int result = underTest.getConnectedClusterCountForEnvironment(createEnvironmentExperienceDto());

        assertEquals(1, result);

        verify(mockLiftieApi, times(ONCE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD);
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(any());
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(first);
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenMoreThanOneElementReturnsAndOneOfThemIsDeletedThenOneLessShouldReturn() {
        int availableClusterQuantity = 1;
        ListClustersResponse first = createEmptyListClustersResponse();
        ClusterView availableCluster = createClusterViewWithStatus("availableCluster1", AVAILABLE_STATUS);
        ClusterView deletedCluster = createClusterViewWithStatus("deletedCluster1", DELETED_STATUS);
        first.setClusters(Map.of(
                availableCluster.getName(), availableCluster,
                deletedCluster.getName(), deletedCluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD)).thenReturn(first);

        int result = underTest.getConnectedClusterCountForEnvironment(createEnvironmentExperienceDto());

        assertEquals(availableClusterQuantity, result);

        verify(mockLiftieApi, times(ONCE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD);
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(any());
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(first);
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenMoreThanOneElementReturnsAndAllOfThemIsAvailableThenTotalNumberShouldReturn() {
        int availableClusterQuantity = 2;
        ListClustersResponse first = createEmptyListClustersResponse();
        ClusterView availableCluster = createClusterViewWithStatus("availableCluster1", AVAILABLE_STATUS);
        ClusterView anotherAvailableCluster = createClusterViewWithStatus("availableCluster2", AVAILABLE_STATUS);
        first.setClusters(Map.of(
                availableCluster.getName(), availableCluster,
                anotherAvailableCluster.getName(), anotherAvailableCluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD)).thenReturn(first);

        int result = underTest.getConnectedClusterCountForEnvironment(createEnvironmentExperienceDto());

        assertEquals(availableClusterQuantity, result);

        verify(mockLiftieApi, times(ONCE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD);
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(any());
        verify(mockListClustersResponseValidator, times(ONCE)).isListClustersResponseEmpty(first);
    }

    @Test
    void testGetConnectedClusterCountForEnvironmentWhenMoreThanOneElementReturnsAndAllOfThemIsDeletedThenZeroShouldReturn() {
        int availableClusterQuantity = 0;
        ListClustersResponse first = createEmptyListClustersResponse();
        ClusterView deletedCluster = createClusterViewWithStatus("deletedCluster1", DELETED_STATUS);
        ClusterView anotherDeletedCluster = createClusterViewWithStatus("deletedCluster2", DELETED_STATUS);
        first.setClusters(Map.of(
                deletedCluster.getName(), deletedCluster,
                anotherDeletedCluster.getName(), anotherDeletedCluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD)).thenReturn(first);

        int result = underTest.getConnectedClusterCountForEnvironment(createEnvironmentExperienceDto());

        assertEquals(availableClusterQuantity, result);

        verify(mockLiftieApi, times(ONCE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD);
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
        ClusterView availableCluster = createClusterViewWithStatus("availableClusterOnPage1", AVAILABLE_STATUS);
        firstResponse.setClusters(Map.of(availableCluster.getName(), availableCluster));

        ListClustersResponse secondResponse = createEmptyListClustersResponse();
        ClusterView availableClusterOnPage2 = createClusterViewWithStatus("availableClusterOnPage2", AVAILABLE_STATUS);
        firstResponse.setClusters(Map.of(availableClusterOnPage2.getName(), availableClusterOnPage2));

        when(mockListClustersResponseValidator.isListClustersResponseEmpty(firstResponse)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD)).thenReturn(firstResponse);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, 2, TEST_WORKLOAD)).thenReturn(secondResponse);

        underTest.getConnectedClusterCountForEnvironment(createEnvironmentExperienceDto());

        verify(mockLiftieApi, times(TWICE)).listClusters(any(), any(), any(), any());
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD);
        verify(mockLiftieApi, times(ONCE)).listClusters(TEST_ENV_NAME, TEST_TENANT, 2, TEST_WORKLOAD);
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
        ClusterView cluster = createClusterViewWithStatus("TestCluster1", AVAILABLE_STATUS);
        first.setClusters(Map.of(cluster.getName(), cluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD)).thenReturn(first);

        underTest.deleteConnectedExperiences(createEnvironmentExperienceDto());

        verify(mockLiftieApi, times(ONCE)).deleteCluster(any());
        verify(mockLiftieApi, times(ONCE)).deleteCluster(cluster.getClusterId());
    }

    @Test
    void testDeleteConnectedExperiencesWhenMoreClusterIsAvailableTheThatExactAmountDeletionShouldHappen() {
        ListClustersResponse first = createEmptyListClustersResponse();
        ClusterView availableCluster = createClusterViewWithStatus("availableCluster1", AVAILABLE_STATUS);
        ClusterView anotherAvailableCluster = createClusterViewWithStatus("availableCluster2", AVAILABLE_STATUS);
        first.setClusters(Map.of(
                availableCluster.getName(), availableCluster,
                anotherAvailableCluster.getName(), anotherAvailableCluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD)).thenReturn(first);

        underTest.deleteConnectedExperiences(createEnvironmentExperienceDto());

        verify(mockLiftieApi, times(TWICE)).deleteCluster(any());
        verify(mockLiftieApi, times(ONCE)).deleteCluster(availableCluster.getClusterId());
        verify(mockLiftieApi, times(ONCE)).deleteCluster(anotherAvailableCluster.getClusterId());
    }

    @Test
    void testDeleteConnectedExperiencesWhenMoreClusterIsComesBackFromListingButOnlyOneIsAvailableThatOneDeletionShouldHappen() {
        ListClustersResponse first = createEmptyListClustersResponse();
        ClusterView availableCluster = createClusterViewWithStatus("availableCluster1", AVAILABLE_STATUS);
        ClusterView notAvailableCluster = createClusterViewWithStatus("notAvailableCluster2", DELETED_STATUS);
        first.setClusters(Map.of(
                availableCluster.getName(), availableCluster,
                notAvailableCluster.getName(), notAvailableCluster));
        when(mockListClustersResponseValidator.isListClustersResponseEmpty(first)).thenReturn(false);
        when(mockLiftieApi.listClusters(TEST_ENV_NAME, TEST_TENANT, null, TEST_WORKLOAD)).thenReturn(first);

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

    private ListClustersResponse createEmptyListClustersResponse() {
        ListClustersResponse empty = new ListClustersResponse();
        PageStats ps = new PageStats();
        ps.setTotalPages(0);
        empty.setPage(ps);
        return empty;
    }

    private EnvironmentExperienceDto createEnvironmentExperienceDto() {
        return new EnvironmentExperienceDto.Builder()
                .withAccountId(TEST_TENANT)
                .withName(TEST_ENV_NAME)
                .withCrn(TEST_ENV_CRN)
                .build();
    }

    private ClusterView createClusterViewWithStatus(String name, String status) {
        ClusterView cv = new ClusterView();
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