package com.sequenceiq.environment.environment.service;

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

import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentViewDto;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.sync.EnvironmentJobService;

public class EnvironmentDeletionServiceTest {

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
    @ValueSource(strings = {"true", "false"})
    public void deleteByNameAndAccountId(String valueString) {
        boolean cascading = Boolean.parseBoolean(valueString);

        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService
                .getByNameAndAccountId(eq(EnvironmentTestData.ENVIRONMENT_NAME), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(environmentView);
        when(environmentDtoConverter.environmentViewToViewDto(any(EnvironmentView.class))).thenReturn(environmentViewDto);
        assertEquals(environmentViewDto, environmentDeletionServiceWired
                .deleteByNameAndAccountId(EnvironmentTestData.ENVIRONMENT_NAME, TestConstants.ACCOUNT_ID, TestConstants.USER, cascading, true));
        verify(environmentDeletionServiceWired).delete(eq(environmentView), eq(TestConstants.USER), anyBoolean(), anyBoolean());
        if (cascading) {
            verify(reactorFlowManager).triggerCascadingDeleteFlow(eq(environmentView), eq(TestConstants.USER), eq(true));
            verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any(), anyBoolean());
        } else {
            verify(reactorFlowManager).triggerDeleteFlow(eq(environmentView), eq(TestConstants.USER), eq(true));
            verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(), any(), anyBoolean());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void deleteByCrnAndAccountId(String valueString) {
        boolean cascading = Boolean.parseBoolean(valueString);

        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService.getByCrnAndAccountId(eq(TestConstants.CRN), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(environmentView);
        when(environmentDtoConverter.environmentViewToViewDto(any(EnvironmentView.class))).thenReturn(environmentViewDto);
        assertEquals(environmentViewDto, environmentDeletionServiceWired
                .deleteByCrnAndAccountId(TestConstants.CRN, TestConstants.ACCOUNT_ID, TestConstants.USER, cascading, true));
        verify(environmentDeletionServiceWired).delete(eq(environmentView), eq(TestConstants.USER), anyBoolean(), anyBoolean());
        verify(environmentJobService).unschedule(environmentView.getId());
        if (cascading) {
            verify(reactorFlowManager).triggerCascadingDeleteFlow(eq(environmentView), eq(TestConstants.USER), eq(true));
            verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any(), anyBoolean());
        } else {
            verify(reactorFlowManager).triggerDeleteFlow(eq(environmentView), eq(TestConstants.USER), eq(true));
            verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(), any(), anyBoolean());
        }
    }

    @Test
    public void delete() {
        assertEquals(environmentView, environmentDeletionService.delete(environmentView, TestConstants.USER, false, false));
        verify(reactorFlowManager).triggerDeleteFlow(eq(environmentView), eq(TestConstants.USER), eq(false));
    }

    @Test
    public void deleteFailedDistroXesAreAttached() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(eq(environmentView))).thenReturn(Set.of("notempty"));
        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, TestConstants.USER, false, false));
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environmentView), eq(TestConstants.USER), eq(false));
    }

    @Test
    public void deleteFailedDataLakesAreAttached() {
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(eq(environmentView))).thenReturn(Set.of("notempty"));
        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, TestConstants.USER, false, false));
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environmentView), eq(TestConstants.USER), eq(false));
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
                .findByNamesInAccount(eq(names), eq(TestConstants.ACCOUNT_ID))).thenReturn(envs);
        assertEquals(expected, environmentDeletionServiceWired
                .deleteMultipleByNames(names, TestConstants.ACCOUNT_ID, TestConstants.USER, false, false).size());
        verify(environmentDeletionServiceWired, times(expected)).delete(any(), eq(TestConstants.USER), anyBoolean(), anyBoolean());
    }

    @Test
    public void deleteMultipleByCrns() {
        environmentDeletionService
                .deleteMultipleByCrns(Set.of("crn1", "crn2"), TestConstants.ACCOUNT_ID, TestConstants.USER, false, false);

        Set<String> names = Set.of("crn1", "crn2");
        EnvironmentView e1 = new EnvironmentView();
        e1.setId(0L);
        EnvironmentView e2 = new EnvironmentView();
        e2.setId(1L);
        Set<EnvironmentView> envs = Set.of(e1, e2);
        int expected = envs.size();
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);

        when(environmentService.findByResourceCrnsInAccount(eq(names), eq(TestConstants.ACCOUNT_ID))).thenReturn(envs);
        assertEquals(expected, environmentDeletionServiceWired.deleteMultipleByCrns(names, TestConstants.ACCOUNT_ID, TestConstants.USER,
                false, false).size());
        verify(environmentDeletionServiceWired, times(expected)).delete(any(), eq(TestConstants.USER), anyBoolean(), anyBoolean());
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

        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environmentView, TestConstants.USER, false, false));
    }
}
