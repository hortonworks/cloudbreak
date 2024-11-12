package com.sequenceiq.environment.environment.service;

import static com.sequenceiq.cloudbreak.util.TestConstants.ACCOUNT_ID;
import static com.sequenceiq.cloudbreak.util.TestConstants.CRN;
import static com.sequenceiq.cloudbreak.util.TestConstants.USER;
import static com.sequenceiq.environment.environment.service.EnvironmentTestData.ENVIRONMENT_NAME;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.ParentEnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentViewDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.scheduled.sync.EnvironmentJobService;
import com.sequenceiq.environment.exception.ExperienceOperationFailedException;

@ExtendWith(MockitoExtension.class)
public class EnvironmentDeletionServiceTest {

    private static final boolean CASCADE = true;

    private static final boolean NON_CASCADE = false;

    private static final long ENVIRONMENT_ID = 12L;

    @Mock
    private EnvironmentViewService environmentService;

    @Mock
    private EnvironmentDtoConverter environmentDtoConverter;

    @Mock
    private EnvironmentReactorFlowManager reactorFlowManager;

    @Mock
    private EnvironmentResourceDeletionService environmentResourceDeletionService;

    @Mock
    private EnvironmentJobService environmentJobService;

    @InjectMocks
    private EnvironmentDeletionService environmentDeletionService;

    private EnvironmentView environmentView;

    private EnvironmentViewDto environmentViewDto;

    @BeforeEach
    public void setup() {
        environmentView = new EnvironmentView();
        environmentView.setId(ENVIRONMENT_ID);
        environmentView.setAccountId(ACCOUNT_ID);
        environmentViewDto = new EnvironmentViewDto();

        lenient().when(environmentService.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(any(), any())).thenReturn(emptyList());
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {true, false})
    public void deleteByNameAndAccountIdWithCascading(boolean forceDelete) {
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService
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
        when(environmentService.getByCrnAndAccountId(eq(CRN), eq(ACCOUNT_ID)))
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
        when(environmentService.getByNameAndAccountId(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(environmentView);
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
        when(environmentService.getByCrnAndAccountId(eq(CRN), eq(ACCOUNT_ID))).thenReturn(environmentView);
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

        verify(environmentService, never()).findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(anyString(), anyLong());
        verify(environmentResourceDeletionService, never()).getAttachedDistroXClusterNames(any(EnvironmentView.class));
        verify(environmentService).editDeletionType(environmentView, forceDelete);
        verify(environmentJobService).unschedule(ENVIRONMENT_ID);
        verify(reactorFlowManager, never()).triggerDeleteFlow(any(EnvironmentView.class), anyString(), anyBoolean());
        verify(reactorFlowManager).triggerCascadingDeleteFlow(eq(environmentView), eq(USER), eq(forceDelete));
    }

    @ParameterizedTest(name = "forceDelete={0}")
    @ValueSource(booleans = {true, false})
    public void deleteWithoutCascading(boolean forceDelete) {
        assertEquals(environmentView, environmentDeletionService.delete(environmentView, USER, NON_CASCADE, forceDelete));

        verify(environmentService).findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID);
        verify(environmentResourceDeletionService).getAttachedDistroXClusterNames(environmentView);
        verify(environmentResourceDeletionService).getConnectedExperienceAmount(environmentView);
        verify(environmentResourceDeletionService).getAttachedSdxClusterCrns(environmentView);
        verify(environmentResourceDeletionService).getDatalakeClusterNames(environmentView);
        verify(environmentService).editDeletionType(environmentView, forceDelete);
        verify(environmentJobService).unschedule(ENVIRONMENT_ID);
        verify(reactorFlowManager).triggerDeleteFlow(eq(environmentView), eq(USER), eq(forceDelete));
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(EnvironmentView.class), anyString(), anyBoolean());
    }

    @Test
    public void deleteFailedChildEnvironmentsAreAttached() {
        when(environmentService.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID))
                .thenReturn(singletonList("child name"));

        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, USER, NON_CASCADE, false));

        verify(environmentResourceDeletionService, never()).getAttachedDistroXClusterNames(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getConnectedExperienceAmount(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getAttachedSdxClusterCrns(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
        verify(environmentService, never()).editDeletionType(any(EnvironmentView.class), anyBoolean());
        verify(environmentJobService, never()).unschedule(anyLong());
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environmentView), eq(USER), eq(false));
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(EnvironmentView.class), anyString(), anyBoolean());
    }

    @Test
    public void deleteFailedDistroXesAreAttached() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(eq(environmentView))).thenReturn(Set.of("nonempty"));

        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, USER, NON_CASCADE, false));

        verify(environmentService).findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID);
        verify(environmentResourceDeletionService, never()).getConnectedExperienceAmount(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getAttachedSdxClusterCrns(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
        verify(environmentService, never()).editDeletionType(any(EnvironmentView.class), anyBoolean());
        verify(environmentJobService, never()).unschedule(anyLong());
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environmentView), eq(USER), eq(false));
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(EnvironmentView.class), anyString(), anyBoolean());
    }

    @Test
    public void deleteFailedExperiencesAreAttached() {
        when(environmentResourceDeletionService.getConnectedExperienceAmount(eq(environmentView))).thenReturn(1);

        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, USER, NON_CASCADE, false));

        verify(environmentService).findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID);
        verify(environmentResourceDeletionService).getAttachedDistroXClusterNames(environmentView);
        verify(environmentResourceDeletionService, never()).getAttachedSdxClusterCrns(any(EnvironmentView.class));
        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
        verify(environmentService, never()).editDeletionType(any(EnvironmentView.class), anyBoolean());
        verify(environmentJobService, never()).unschedule(anyLong());
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environmentView), eq(USER), eq(false));
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(EnvironmentView.class), anyString(), anyBoolean());
    }

    @Test
    public void deleteFailedDataLakesAreAttached() {
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(eq(environmentView))).thenReturn(Set.of("nonempty"));

        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, USER, NON_CASCADE, false));

        verify(environmentService).findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID);
        verify(environmentResourceDeletionService).getAttachedDistroXClusterNames(environmentView);
        verify(environmentResourceDeletionService).getConnectedExperienceAmount(environmentView);
        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
        verify(environmentService, never()).editDeletionType(any(EnvironmentView.class), anyBoolean());
        verify(environmentJobService, never()).unschedule(anyLong());
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environmentView), eq(USER), eq(false));
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(EnvironmentView.class), anyString(), anyBoolean());
    }

    @Test
    public void deleteMultipleByNames() {
        Set<String> names = Set.of("name1", "name2");
        EnvironmentView e1 = new EnvironmentView();
        e1.setId(0L);
        EnvironmentView e2 = new EnvironmentView();
        e2.setId(1L);
        Set<EnvironmentView> envs = Set.of(e1, e2);
        int expected = envs.size();
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);

        when(environmentService
                .findByNamesInAccount(eq(names), eq(ACCOUNT_ID))).thenReturn(envs);

        assertEquals(expected, environmentDeletionServiceWired
                .deleteMultipleByNames(names, ACCOUNT_ID, USER, NON_CASCADE, false).size());

        verify(environmentDeletionServiceWired, times(expected)).delete(any(), eq(USER), eq(NON_CASCADE), eq(false));
    }

    @Test
    public void deleteMultipleByNamesIfParentAndChildEnvironmentAreInTheSet() {
        Set<String> names = Set.of("name1", "name2");
        EnvironmentView e1 = new EnvironmentView();
        e1.setId(0L);
        e1.setName("name1");
        EnvironmentView e2 = new EnvironmentView();
        e2.setId(1L);
        e2.setName("name2");
        ParentEnvironmentView parentEnvironment = new ParentEnvironmentView();
        parentEnvironment.setName("name1");
        parentEnvironment.setId(0L);
        e2.setParentEnvironment(parentEnvironment);
        Set<EnvironmentView> envs = Set.of(e1, e2);
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);

        when(environmentService
                .findByNamesInAccount(eq(names), eq(ACCOUNT_ID))).thenReturn(envs);

        assertEquals(1, environmentDeletionServiceWired
                .deleteMultipleByNames(names, ACCOUNT_ID, USER, CASCADE, false).size());

        verify(environmentDeletionServiceWired, times(1)).delete(any(), eq(USER), eq(CASCADE), eq(false));
        verify(reactorFlowManager).triggerCascadingDeleteFlow(eq(e1), eq(USER), eq(false));
    }

    @Test
    public void deleteMultipleByCrns() {
        environmentDeletionService
                .deleteMultipleByCrns(Set.of("crn1", "crn2"), ACCOUNT_ID, USER, false, false);

        Set<String> names = Set.of("crn1", "crn2");
        EnvironmentView e1 = new EnvironmentView();
        e1.setId(0L);
        EnvironmentView e2 = new EnvironmentView();
        e2.setId(1L);
        Set<EnvironmentView> envs = Set.of(e1, e2);
        int expected = envs.size();
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);

        when(environmentService.findByResourceCrnsInAccount(eq(names), eq(ACCOUNT_ID))).thenReturn(envs);

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
        when(environmentResourceDeletionService.getDatalakeClusterNames(environmentView)).thenReturn(Set.of("name"));

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
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environmentView)).thenReturn(Set.of("name"));

        BadRequestException actual = assertThrows(BadRequestException.class,
                () -> environmentDeletionService.checkIsEnvironmentDeletableWithoutCascadingWhenAttachedResources(environmentView));
        assertEquals("The following Data Lake cluster(s) are attached to the Environment: [name]. " +
                "Either they must be terminated before Environment deletion, " +
                "or the cascading delete option (\"I would like to delete all connected resources\") shall be utilized.", actual.getMessage());

        verify(environmentResourceDeletionService, never()).getDatalakeClusterNames(any(EnvironmentView.class));
    }

    @Test
    public void testCheckIsEnvironmentDeletableWithoutCascadingWhenAttachedResourcesWhenDistroXIsNotEmpty() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(environmentView)).thenReturn(Set.of("name"));

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
        when(environmentService.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(ACCOUNT_ID, ENVIRONMENT_ID))
                .thenReturn(singletonList("child name"));

        BadRequestException actual = assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, USER,
                NON_CASCADE, false));

        assertEquals("The following child Environment(s) are attached to the Environment: [child name]. " +
                "Either these must be terminated before Environment deletion, " +
                "or the cascading delete option (\"I would like to delete all connected resources\") shall be utilized.", actual.getMessage());
    }

}
