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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentViewDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.sync.EnvironmentJobService;

public class EnvironmentDeletionServiceTest {

    private static final boolean CASCADE = true;

    private static final boolean NON_CASCADE = !CASCADE;

    private final EnvironmentViewService environmentService = Mockito.mock(EnvironmentViewService.class);

    private final EnvironmentDtoConverter environmentDtoConverter = Mockito.mock(EnvironmentDtoConverter.class);

    private final EnvironmentReactorFlowManager reactorFlowManager = Mockito.mock(EnvironmentReactorFlowManager.class);

    private final EnvironmentResourceDeletionService environmentResourceDeletionService = Mockito.mock(EnvironmentResourceDeletionService.class);

    private final EnvironmentJobService environmentJobService = Mockito.mock(EnvironmentJobService.class);

    private final EnvironmentDeletionService environmentDeletionService = new EnvironmentDeletionService(environmentService, environmentJobService,
            environmentDtoConverter, reactorFlowManager, environmentResourceDeletionService);

    private EnvironmentView environmentView;

    private EnvironmentViewDto environmentViewDto;

    @BeforeEach
    public void setup() {
        environmentView = new EnvironmentView();
        environmentViewDto = new EnvironmentViewDto();

        when(environmentService.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(any(), any())).thenReturn(emptyList());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void deleteByNameAndAccountId(boolean cascading) {
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService
                .getByNameAndAccountId(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID)))
                .thenReturn(environmentView);
        when(environmentDtoConverter.environmentViewToViewDto(any(EnvironmentView.class))).thenReturn(environmentViewDto);
        assertEquals(environmentViewDto, environmentDeletionServiceWired
                .deleteByNameAndAccountId(ENVIRONMENT_NAME, ACCOUNT_ID, USER, cascading, true));
        verify(environmentDeletionServiceWired).delete(eq(environmentView), eq(USER), anyBoolean(), anyBoolean());
        if (cascading) {
            verify(reactorFlowManager).triggerCascadingDeleteFlow(eq(environmentView), eq(USER), eq(true));
            verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any(), anyBoolean());
        } else {
            verify(reactorFlowManager).triggerDeleteFlow(eq(environmentView), eq(USER), eq(true));
            verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(), any(), anyBoolean());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void deleteByCrnAndAccountId(boolean cascading) {
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService.getByCrnAndAccountId(eq(CRN), eq(ACCOUNT_ID)))
                .thenReturn(environmentView);
        when(environmentDtoConverter.environmentViewToViewDto(any(EnvironmentView.class))).thenReturn(environmentViewDto);
        assertEquals(environmentViewDto, environmentDeletionServiceWired
                .deleteByCrnAndAccountId(CRN, ACCOUNT_ID, USER, cascading, true));
        verify(environmentDeletionServiceWired).delete(eq(environmentView), eq(USER), anyBoolean(), anyBoolean());
        verify(environmentJobService).unschedule(environmentView.getId());
        if (cascading) {
            verify(reactorFlowManager).triggerCascadingDeleteFlow(eq(environmentView), eq(USER), eq(true));
            verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any(), anyBoolean());
        } else {
            verify(reactorFlowManager).triggerDeleteFlow(eq(environmentView), eq(USER), eq(true));
            verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(), any(), anyBoolean());
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void deleteByNameAndAccountIdWithForceDelete(boolean forceDelete) {
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService.getByNameAndAccountId(eq(ENVIRONMENT_NAME), eq(ACCOUNT_ID))).thenReturn(environmentView);
        when(environmentDtoConverter.environmentViewToViewDto(any(EnvironmentView.class))).thenReturn(environmentViewDto);
        assertEquals(environmentViewDto, environmentDeletionServiceWired.deleteByNameAndAccountId(ENVIRONMENT_NAME, ACCOUNT_ID, USER, NON_CASCADE, forceDelete));
        verify(environmentDeletionServiceWired).delete(eq(environmentView), eq(USER), anyBoolean(), anyBoolean());
        if (forceDelete) {
            verify(environmentResourceDeletionService, never()).getConnectedExperienceAmount(any());
            verify(environmentResourceDeletionService, never()).getConnectedExperienceAmount(environmentView);
        } else {
            verify(environmentResourceDeletionService, times(1)).getConnectedExperienceAmount(any());
            verify(environmentResourceDeletionService, times(1)).getConnectedExperienceAmount(environmentView);
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void deleteByCrnAndAccountIdWithForceDelete(boolean forceDelete) {
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService.getByCrnAndAccountId(eq(CRN), eq(ACCOUNT_ID))).thenReturn(environmentView);
        when(environmentDtoConverter.environmentViewToViewDto(any(EnvironmentView.class))).thenReturn(environmentViewDto);
        assertEquals(environmentViewDto, environmentDeletionServiceWired.deleteByCrnAndAccountId(CRN, ACCOUNT_ID, USER, NON_CASCADE, forceDelete));
        verify(environmentDeletionServiceWired).delete(eq(environmentView), eq(USER), anyBoolean(), anyBoolean());
        verify(environmentJobService).unschedule(environmentView.getId());
        if (forceDelete) {
            verify(environmentResourceDeletionService, never()).getConnectedExperienceAmount(any());
            verify(environmentResourceDeletionService, never()).getConnectedExperienceAmount(environmentView);
        } else {
            verify(environmentResourceDeletionService, times(1)).getConnectedExperienceAmount(any());
            verify(environmentResourceDeletionService, times(1)).getConnectedExperienceAmount(environmentView);
        }
    }

    @Test
    public void delete() {
        assertEquals(environmentView, environmentDeletionService.delete(environmentView, USER, false, false));
        verify(reactorFlowManager).triggerDeleteFlow(eq(environmentView), eq(USER), eq(false));
    }

    @Test
    public void deleteFailedDistroXesAreAttached() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(eq(environmentView))).thenReturn(Set.of("notempty"));
        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, USER, false, false));
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environmentView), eq(USER), eq(false));
    }

    @Test
    public void deleteFailedDataLakesAreAttached() {
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(eq(environmentView))).thenReturn(Set.of("notempty"));
        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, USER, false, false));
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environmentView), eq(USER), eq(false));
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
                .deleteMultipleByNames(names, ACCOUNT_ID, USER, false, false).size());
        verify(environmentDeletionServiceWired, times(expected)).delete(any(), eq(USER), anyBoolean(), anyBoolean());
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
                false, false).size());
        verify(environmentDeletionServiceWired, times(expected)).delete(any(), eq(USER), anyBoolean(), anyBoolean());
    }

    @Test
    public void testCheckIsEnvironmentDeletableWhenSdxAndDatalakeAreEmpty() {
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environmentView)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getDatalakeClusterNames(environmentView)).thenReturn(emptySet());

        environmentDeletionService.checkIsEnvironmentDeletable(environmentView);
        verify(environmentResourceDeletionService).getAttachedDistroXClusterNames(environmentView);
    }

    @Test
    public void testCheckIsEnvironmentDeletableWhenSdxIsEmptyButDatalakeIsNot() {
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environmentView)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getDatalakeClusterNames(environmentView)).thenReturn(Set.of("name"));

        BadRequestException actual = assertThrows(BadRequestException.class, () -> environmentDeletionService.checkIsEnvironmentDeletable(environmentView));
        assertEquals("The following Data Lake cluster(s) must be terminated before Environment deletion [name]", actual.getMessage());
    }

    @Test
    public void canNotDeleteParentEnvironment() {
        when(environmentService.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(environmentView.getAccountId(), environmentView.getId()))
                .thenReturn(singletonList("child name"));

        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, USER, false, false));
    }
}
