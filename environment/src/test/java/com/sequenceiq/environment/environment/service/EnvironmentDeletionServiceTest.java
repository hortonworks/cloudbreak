package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.util.TestConstants.CRN;
import static com.sequenceiq.cloudbreak.util.TestConstants.USER;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static com.sequenceiq.environment.network.dao.domain.RegistrationType.CREATE_NEW;
import static com.sequenceiq.environment.network.dao.domain.RegistrationType.EXISTING;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Set.of;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentViewDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.sync.EnvironmentJobService;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;

@ExtendWith(MockitoExtension.class)
public class EnvironmentDeletionServiceTest {

    private static final boolean CASCADE = true;

    private static final boolean NON_CASCADE = false;

    private static final long ENVIRONMENT_ID = 12L;

    @Mock
    private EnvironmentViewService mockEnvironmentViewService;

    @Mock
    private EnvironmentDtoConverter environmentDtoConverter;

    @Mock
    private EnvironmentReactorFlowManager reactorFlowManager;

    @Mock
    private EnvironmentResourceDeletionService environmentResourceDeletionService;

    @Mock
    private EnvironmentJobService environmentJobService;

    @Mock
    private EnvironmentService mockEnvironmentService;

    @Mock
    private BaseNetwork mockBaseNetwork;

    @InjectMocks
    private EnvironmentDeletionService environmentDeletionService;

    private EnvironmentView environmentView;

    private EnvironmentViewDto environmentViewDto;

    @BeforeEach
    public void setup() {
        environmentView = new EnvironmentView();
        environmentView.setId(ENVIRONMENT_ID);
        environmentView.setAccountId(ACCOUNT_ID);
        environmentView.setNetwork(mockBaseNetwork);
        environmentViewDto = new EnvironmentViewDto();

        lenient().when(mockEnvironmentViewService.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(any(), any())).thenReturn(emptyList());
        lenient().when(mockEnvironmentService.getEnvironmentsUsingTheSameNetwork(any())).thenReturn(emptySet());
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {true, false})
    public void deleteByNameAndAccountIdWithCascading(boolean forceDelete) {
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(mockEnvironmentViewService
                .getByNameAndAccountId(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID)))
                .thenReturn(environmentView);
        when(environmentDtoConverter.environmentViewToViewDto(any(EnvironmentView.class))).thenReturn(environmentViewDto);
        assertEquals(environmentViewDto, environmentDeletionServiceWired
                .deleteByNameAndAccountId(ENVIRONMENT_NAME, ACCOUNT_ID, USER, CASCADE, forceDelete));
        verify(environmentDeletionServiceWired).delete(eq(environmentView), eq(USER), eq(CASCADE), eq(forceDelete));
        verify(environmentJobService).unschedule(environmentView.getId());
        verify(reactorFlowManager).triggerCascadingDeleteFlow(eq(environmentView), eq(USER), eq(forceDelete));
        verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any(), anyBoolean());
        verify(environmentResourceDeletionService, never()).getConnectedExperienceAmount(environmentView);
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {true, false})
    public void deleteByCrnAndAccountIdWithCascading(boolean forceDelete) {
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(mockEnvironmentViewService.getByCrnAndAccountId(eq(CRN), eq(ACCOUNT_ID)))
                .thenReturn(environmentView);
        when(environmentDtoConverter.environmentViewToViewDto(any(EnvironmentView.class))).thenReturn(environmentViewDto);
        assertEquals(environmentViewDto, environmentDeletionServiceWired
                .deleteByCrnAndAccountId(CRN, ACCOUNT_ID, USER, CASCADE, forceDelete));
        verify(environmentDeletionServiceWired).delete(eq(environmentView), eq(USER), eq(CASCADE), eq(forceDelete));
        verify(environmentJobService).unschedule(environmentView.getId());
        verify(reactorFlowManager).triggerCascadingDeleteFlow(eq(environmentView), eq(USER), eq(forceDelete));
        verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any(), anyBoolean());
        verify(environmentResourceDeletionService, never()).getConnectedExperienceAmount(environmentView);
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {true, false})
    public void deleteByNameAndAccountIdWithoutCascading(boolean forceDelete) {
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(mockEnvironmentViewService.getByNameAndAccountId(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(environmentView);
        when(environmentDtoConverter.environmentViewToViewDto(any(EnvironmentView.class))).thenReturn(environmentViewDto);
        assertEquals(environmentViewDto, environmentDeletionServiceWired.deleteByNameAndAccountId(ENVIRONMENT_NAME, ACCOUNT_ID, USER, NON_CASCADE,
                forceDelete));
        verify(environmentDeletionServiceWired).delete(eq(environmentView), eq(USER), eq(NON_CASCADE), eq(forceDelete));
        verify(environmentJobService).unschedule(environmentView.getId());
        verify(environmentResourceDeletionService, times(1)).getConnectedExperienceAmount(environmentView);
        verify(reactorFlowManager).triggerDeleteFlow(eq(environmentView), eq(USER), eq(forceDelete));
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(), any(), anyBoolean());
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {true, false})
    public void deleteByCrnAndAccountIdWithoutCascading(boolean forceDelete) {
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(mockEnvironmentViewService.getByCrnAndAccountId(eq(CRN), eq(ACCOUNT_ID))).thenReturn(environmentView);
        when(environmentDtoConverter.environmentViewToViewDto(any(EnvironmentView.class))).thenReturn(environmentViewDto);
        assertEquals(environmentViewDto, environmentDeletionServiceWired.deleteByCrnAndAccountId(CRN, ACCOUNT_ID, USER, NON_CASCADE, forceDelete));
        verify(environmentDeletionServiceWired).delete(eq(environmentView), eq(USER), eq(NON_CASCADE), eq(forceDelete));
        verify(environmentJobService).unschedule(environmentView.getId());
        verify(environmentResourceDeletionService, times(1)).getConnectedExperienceAmount(environmentView);
        verify(reactorFlowManager).triggerDeleteFlow(eq(environmentView), eq(USER), eq(forceDelete));
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(), any(), anyBoolean());
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {true, false})
    public void deleteWithCascading(boolean forceDelete) {
        assertEquals(environmentView, environmentDeletionService.delete(environmentView, USER, CASCADE, forceDelete));

        verify(mockEnvironmentViewService, never()).findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(anyString(), anyLong());
        verify(environmentResourceDeletionService, never()).getAttachedDistroXClusterNames(any(EnvironmentView.class));
        verify(mockEnvironmentViewService).editDeletionType(environmentView, forceDelete);
        verify(environmentJobService).unschedule(ENVIRONMENT_ID);
        verify(reactorFlowManager, never()).triggerDeleteFlow(any(EnvironmentView.class), anyString(), anyBoolean());
        verify(reactorFlowManager).triggerCascadingDeleteFlow(eq(environmentView), eq(USER), eq(forceDelete));
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {true, false})
    public void deleteWithoutCascading(boolean forceDelete) {
        assertEquals(environmentView, environmentDeletionService.delete(environmentView, USER, NON_CASCADE, forceDelete));

        verify(mockEnvironmentViewService).findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID);
        verify(environmentResourceDeletionService).getAttachedDistroXClusterNames(environmentView);
        verify(environmentResourceDeletionService).getConnectedExperienceAmount(environmentView);
        verify(environmentResourceDeletionService).getAttachedSdxClusterCrns(environmentView);
        verify(environmentResourceDeletionService).getDatalakeClusterNames(environmentView);
        verify(mockEnvironmentViewService).editDeletionType(environmentView, forceDelete);
        verify(environmentJobService).unschedule(ENVIRONMENT_ID);
        verify(reactorFlowManager).triggerDeleteFlow(eq(environmentView), eq(USER), eq(forceDelete));
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(EnvironmentView.class), anyString(), anyBoolean());
    }

    @Test
    public void deleteFailedChildEnvironmentsAreAttached() {
        when(mockEnvironmentViewService.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID))
                .thenReturn(singletonList("child name"));

        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, USER, NON_CASCADE, false));

        verify(environmentResourceDeletionService, never()).getAttachedDistroXClusterNames(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getConnectedExperienceAmount(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getAttachedSdxClusterCrns(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
        verify(mockEnvironmentViewService, never()).editDeletionType(any(EnvironmentView.class), anyBoolean());
        verify(environmentJobService, never()).unschedule(anyLong());
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environmentView), eq(USER), eq(false));
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(EnvironmentView.class), anyString(), anyBoolean());
    }

    @Test
    public void deleteFailedDistroXesAreAttached() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(eq(environmentView))).thenReturn(of("nonempty"));

        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, USER, NON_CASCADE, false));

        verify(mockEnvironmentViewService).findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID);
        verify(environmentResourceDeletionService, never()).getConnectedExperienceAmount(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getAttachedSdxClusterCrns(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
        verify(mockEnvironmentViewService, never()).editDeletionType(any(EnvironmentView.class), anyBoolean());
        verify(environmentJobService, never()).unschedule(anyLong());
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environmentView), eq(USER), eq(false));
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(EnvironmentView.class), anyString(), anyBoolean());
    }

    @Test
    public void deleteFailedExperiencesAreAttached() {
        when(environmentResourceDeletionService.getConnectedExperienceAmount(eq(environmentView))).thenReturn(1);

        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, USER, NON_CASCADE, false));

        verify(mockEnvironmentViewService).findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID);
        verify(environmentResourceDeletionService).getAttachedDistroXClusterNames(environmentView);
        verify(environmentResourceDeletionService, never()).getAttachedSdxClusterCrns(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
        verify(mockEnvironmentViewService, never()).editDeletionType(any(EnvironmentView.class), anyBoolean());
        verify(environmentJobService, never()).unschedule(anyLong());
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environmentView), eq(USER), eq(false));
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(EnvironmentView.class), anyString(), anyBoolean());
    }

    @Test
    public void deleteFailedDataLakesAreAttached() {
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(eq(environmentView))).thenReturn(of("nonempty"));

        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, USER, NON_CASCADE, false));

        verify(mockEnvironmentViewService).findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID);
        verify(environmentResourceDeletionService).getAttachedDistroXClusterNames(environmentView);
        verify(environmentResourceDeletionService).getConnectedExperienceAmount(environmentView);
        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
        verify(mockEnvironmentViewService, never()).editDeletionType(any(EnvironmentView.class), anyBoolean());
        verify(environmentJobService, never()).unschedule(anyLong());
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environmentView), eq(USER), eq(false));
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(EnvironmentView.class), anyString(), anyBoolean());
    }

    @Test
    public void deleteMultipleByNames() {
        Set<String> names = of("name1", "name2");
        EnvironmentView e1 = new EnvironmentView();
        e1.setId(0L);
        e1.setNetwork(mock(BaseNetwork.class));
        EnvironmentView e2 = new EnvironmentView();
        e2.setId(1L);
        e2.setNetwork(mock(BaseNetwork.class));
        Set<EnvironmentView> envs = of(e1, e2);
        int expected = envs.size();
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);

        when(mockEnvironmentViewService
                .findByNamesInAccount(eq(names), eq(ACCOUNT_ID))).thenReturn(envs);

        assertEquals(expected, environmentDeletionServiceWired
                .deleteMultipleByNames(names, ACCOUNT_ID, USER, NON_CASCADE, false).size());

        verify(environmentDeletionServiceWired, times(expected)).delete(any(), eq(USER), eq(NON_CASCADE), eq(false));
    }

    @Test
    public void deleteMultipleByCrns() {
        environmentDeletionService
                .deleteMultipleByCrns(of("crn1", "crn2"), ACCOUNT_ID, USER, false, false);

        Set<String> names = of("crn1", "crn2");
        EnvironmentView e1 = new EnvironmentView();
        e1.setId(0L);
        e1.setNetwork(mock(BaseNetwork.class));
        EnvironmentView e2 = new EnvironmentView();
        e2.setId(1L);
        e2.setNetwork(mock(BaseNetwork.class));
        Set<EnvironmentView> envs = of(e1, e2);
        int expected = envs.size();
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);

        when(mockEnvironmentViewService.findByResourceCrnsInAccount(eq(names), eq(ACCOUNT_ID))).thenReturn(envs);

        assertEquals(expected, environmentDeletionServiceWired.deleteMultipleByCrns(names, ACCOUNT_ID, USER,
                NON_CASCADE, false).size());

        verify(environmentDeletionServiceWired, times(expected)).delete(any(), eq(USER), eq(NON_CASCADE), eq(false));
    }

    @Test
    public void testCheckIsEnvironmentDeletableWithoutCascadingWhenAttachedResourcesWhenNothingAttached() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(environmentView)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getConnectedExperienceAmount(environmentView)).thenReturn(0);
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environmentView)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getDatalakeClusterNames(environmentView)).thenReturn(emptySet());

        environmentDeletionService.checkIsEnvironmentDeletableWithoutCascadingWhenAttachedResources(environmentView);
    }

    @Test
    public void testCheckIsEnvironmentDeletableWithoutCascadingWhenAttachedResourcesWhenSdxIsEmptyButDatalakeIsNot() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(environmentView)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getConnectedExperienceAmount(environmentView)).thenReturn(0);
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environmentView)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getDatalakeClusterNames(environmentView)).thenReturn(of("name"));

        BadRequestException actual = assertThrows(BadRequestException.class,
                () -> environmentDeletionService.checkIsEnvironmentDeletableWithoutCascadingWhenAttachedResources(environmentView));
        assertEquals("The following Data Lake cluster(s) are attached to the Environment: [name]. " +
                "Either they must be terminated before Environment deletion, " +
                "or the cascading delete option (\"I would like to delete all connected resources\") shall be utilized.", actual.getMessage());
    }

    @Test
    public void testCheckIsEnvironmentDeletableWithoutCascadingWhenAttachedResourcesWhenSdxIsNotEmpty() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(environmentView)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getConnectedExperienceAmount(environmentView)).thenReturn(0);
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environmentView)).thenReturn(of("name"));

        BadRequestException actual = assertThrows(BadRequestException.class,
                () -> environmentDeletionService.checkIsEnvironmentDeletableWithoutCascadingWhenAttachedResources(environmentView));
        assertEquals("The following Data Lake cluster(s) are attached to the Environment: [name]. " +
                "Either they must be terminated before Environment deletion, " +
                "or the cascading delete option (\"I would like to delete all connected resources\") shall be utilized.", actual.getMessage());

        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
    }

    @Test
    public void testCheckIsEnvironmentDeletableWithoutCascadingWhenAttachedResourcesWhenDistroXIsNotEmpty() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(environmentView)).thenReturn(of("name"));

        BadRequestException actual = assertThrows(BadRequestException.class,
                () -> environmentDeletionService.checkIsEnvironmentDeletableWithoutCascadingWhenAttachedResources(environmentView));
        assertEquals("The following Data Hub cluster(s) are attached to the Environment: [name]. " +
                "Either they must be terminated before Environment deletion, " +
                "or the cascading delete option (\"I would like to delete all connected resources\") shall be utilized.", actual.getMessage());

        verify(environmentResourceDeletionService, never()).getConnectedExperienceAmount(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getAttachedSdxClusterCrns(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
    }

    @Test
    public void testCheckIsEnvironmentDeletableWithoutCascadingWhenAttachedResourcesWhenExperienceCheckFails() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(environmentView)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getConnectedExperienceAmount(environmentView))
                .thenThrow(new ExperienceOperationFailedException("Serious error"));

        IllegalStateException actual = assertThrows(IllegalStateException.class,
                () -> environmentDeletionService.checkIsEnvironmentDeletableWithoutCascadingWhenAttachedResources(environmentView));
        assertEquals("Unable to access all experiences (data services) to check whether the Environment "
                + "has any connected one(s)! If you would like to bypass the issue, you can use the force deletion option, but please keep in mind that "
                + "- in some cases - it may leave resources on the cloud provider side that need to be checked and cleaned by hand afterwards. " +
                "Error reason: Serious error", actual.getMessage());

        verify(environmentResourceDeletionService, never()).getAttachedSdxClusterCrns(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
    }

    @Test
    public void testCheckIsEnvironmentDeletableWithoutCascadingWhenAttachedResourcesWhenOneExperience() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(environmentView)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getConnectedExperienceAmount(environmentView)).thenReturn(1);

        BadRequestException actual = assertThrows(BadRequestException.class,
                () -> environmentDeletionService.checkIsEnvironmentDeletableWithoutCascadingWhenAttachedResources(environmentView));
        assertEquals("The Environment has 1 connected experience (data service). " +
                "Either this must be terminated before Environment deletion, " +
                "or the cascading delete option (\"I would like to delete all connected resources\") shall be utilized.", actual.getMessage());

        verify(environmentResourceDeletionService, never()).getAttachedSdxClusterCrns(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
    }

    @Test
    public void testCheckIsEnvironmentDeletableWithoutCascadingWhenAttachedResourcesWhenTwoExperiences() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(environmentView)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getConnectedExperienceAmount(environmentView)).thenReturn(2);

        BadRequestException actual = assertThrows(BadRequestException.class,
                () -> environmentDeletionService.checkIsEnvironmentDeletableWithoutCascadingWhenAttachedResources(environmentView));
        assertEquals("The Environment has 2 connected experiences (data services). " +
                "Either these must be terminated before Environment deletion, " +
                "or the cascading delete option (\"I would like to delete all connected resources\") shall be utilized.", actual.getMessage());

        verify(environmentResourceDeletionService, never()).getAttachedSdxClusterCrns(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
    }

    @Test
    public void canNotDeleteParentEnvironment() {
        when(mockEnvironmentViewService.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID))
                .thenReturn(singletonList("child name"));

        BadRequestException actual = assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, USER,
                NON_CASCADE, false));

        assertEquals("The following child Environment(s) are attached to the Environment: [child name]. " +
                "Either these must be terminated before Environment deletion, " +
                "or the cascading delete option (\"I would like to delete all connected resources\") shall be utilized.", actual.getMessage());
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = "cascade={0}")
    @DisplayName("Environment deletion going to be cancelled since the environment is created with a new network creation option and later on other " +
            "environments started to use the same.")
    void testSingleDeleteNetworkUsedByOthers(boolean cascade) {
        when(mockBaseNetwork.getRegistrationType()).thenReturn(CREATE_NEW);
        environmentView.setNetwork(mockBaseNetwork);
        when(mockEnvironmentService.getEnvironmentsUsingTheSameNetwork(mockBaseNetwork)).thenReturn(of(NameOrCrn.ofBoth("someEnv", "someEnvCrn")));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> environmentDeletionService.delete(environmentView, USER, cascade,
                false));
        assertEquals("Deletion not allowed because there are other environments that are using the same network that was created " +
                "during the creation of this environment. \nPlease remove those before initiating the deletion of this environment or use the force " +
                "option, but if you'd do the latter option, please keep in mind that the termination could break those environments since their " +
                "network related cloud resources will be terminated as well.", exception.getMessage());

        verify(mockEnvironmentService, times(1)).getEnvironmentsUsingTheSameNetwork(mockBaseNetwork);
        verifyNoMoreInteractions(mockEnvironmentService);

        verifyNoInteractions(reactorFlowManager);
        verifyNoInteractions(environmentJobService);
        verifyNoInteractions(mockEnvironmentViewService);
        verifyNoInteractions(environmentResourceDeletionService);
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = "cascade={0}")
    @DisplayName("Environment deletion won't to be cancelled if the environment is created with a new network creation option and no other environments " +
            "are using the same.")
    void testDeleteNetworkNotUsedByOthers(boolean cascade) {
        when(mockBaseNetwork.getRegistrationType()).thenReturn(CREATE_NEW);
        environmentView.setNetwork(mockBaseNetwork);
        when(mockEnvironmentService.getEnvironmentsUsingTheSameNetwork(mockBaseNetwork)).thenReturn(emptySet());

        environmentDeletionService.delete(environmentView, USER, cascade, false);

        verify(mockEnvironmentService, times(1)).getEnvironmentsUsingTheSameNetwork(mockBaseNetwork);
        verifyNoMoreInteractions(mockEnvironmentService);

        verify(environmentJobService, times(1)).unschedule(ENVIRONMENT_ID);
        if (cascade) {
            reactorFlowManager.triggerCascadingDeleteFlow(environmentView, USER, false);
        } else {
            verify(mockEnvironmentViewService, times(1)).findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID);
            verify(environmentResourceDeletionService, times(1)).getConnectedExperienceAmount(environmentView);
            verify(environmentResourceDeletionService, times(1)).getAttachedDistroXClusterNames(environmentView);
            reactorFlowManager.triggerDeleteFlow(environmentView, USER, false);
        }
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = "cascade={0}")
    @DisplayName("Environment deletion won't be cancelled even if the environment is created with a new network creation option and later on other " +
            "environments started to use the same, since the force flag disables this check.")
    void testSingleDeleteNetworkUsedByOthersForced(boolean cascade) {
        lenient().when(mockBaseNetwork.getRegistrationType()).thenReturn(CREATE_NEW);
        environmentView.setNetwork(mockBaseNetwork);
        lenient().when(mockEnvironmentService.getEnvironmentsUsingTheSameNetwork(mockBaseNetwork)).thenReturn(of(NameOrCrn.ofBoth("someEnv", "someEnvCrn")));

        environmentDeletionService.delete(environmentView, USER, cascade, true);


        verifyNoInteractions(mockEnvironmentService);
        verify(environmentJobService, times(1)).unschedule(ENVIRONMENT_ID);
        if (cascade) {
            reactorFlowManager.triggerCascadingDeleteFlow(environmentView, USER, true);
        } else {
            verify(mockEnvironmentViewService, times(1)).findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID);
            verify(environmentResourceDeletionService, times(1)).getConnectedExperienceAmount(environmentView);
            verify(environmentResourceDeletionService, times(1)).getAttachedDistroXClusterNames(environmentView);
            reactorFlowManager.triggerDeleteFlow(environmentView, USER, true);
        }
    }

    @ValueSource(booleans = {true, false})
    @ParameterizedTest(name = "cascade={0}")
    @DisplayName("Environment deletion won't to be cancelled if the environment is created with an existing network option and other environments are using " +
            "the same network.")
    void testDeleteExistingNetworkUsedByOthers(boolean cascade) {
        lenient().when(mockBaseNetwork.getRegistrationType()).thenReturn(EXISTING);
        environmentView.setNetwork(mockBaseNetwork);
        lenient().when(mockEnvironmentService.getEnvironmentsUsingTheSameNetwork(mockBaseNetwork)).thenReturn(of(NameOrCrn.ofBoth("someEnv", "someEnvCrn")));

        environmentDeletionService.delete(environmentView, USER, cascade, false);

        verifyNoInteractions(mockEnvironmentService);
        verify(environmentJobService, times(1)).unschedule(ENVIRONMENT_ID);
        if (cascade) {
            reactorFlowManager.triggerCascadingDeleteFlow(environmentView, USER, true);
        } else {
            verify(mockEnvironmentViewService, times(1)).findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID);
            verify(environmentResourceDeletionService, times(1)).getConnectedExperienceAmount(environmentView);
            verify(environmentResourceDeletionService, times(1)).getAttachedDistroXClusterNames(environmentView);
            reactorFlowManager.triggerDeleteFlow(environmentView, USER, true);
        }
    }

}
