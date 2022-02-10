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

import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.EnvironmentReactorFlowManager;
import com.sequenceiq.environment.environment.sync.EnvironmentJobService;

public class EnvironmentDeletionServiceTest {

    private final EnvironmentService environmentService = Mockito.mock(EnvironmentService.class);

    private final EnvironmentDtoConverter environmentDtoConverter = Mockito.mock(EnvironmentDtoConverter.class);

    private final EnvironmentReactorFlowManager reactorFlowManager = Mockito.mock(EnvironmentReactorFlowManager.class);

    private final EnvironmentResourceDeletionService environmentResourceDeletionService = Mockito.mock(EnvironmentResourceDeletionService.class);

    private final EnvironmentJobService environmentJobService = Mockito.mock(EnvironmentJobService.class);

    private final EnvironmentDeletionService environmentDeletionService = new EnvironmentDeletionService(environmentService, environmentJobService,
            environmentDtoConverter, reactorFlowManager, environmentResourceDeletionService);

    private Environment environment;

    private EnvironmentDto environmentDto;

    @BeforeEach
    public void setup() {
        environment = new Environment();
        environmentDto = new EnvironmentDto();

        when(environmentService.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(any(), any())).thenReturn(emptyList());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void deleteByNameAndAccountIdNotFound(String valueString) {
        boolean cascading = Boolean.parseBoolean(valueString);

        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(EnvironmentTestData.ENVIRONMENT_NAME), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> environmentDeletionServiceWired
                        .deleteByNameAndAccountId(EnvironmentTestData.ENVIRONMENT_NAME, TestConstants.ACCOUNT_ID, TestConstants.USER,
                                cascading, true));
        verify(environmentDeletionServiceWired, never()).delete(eq(environment), eq(TestConstants.USER), anyBoolean(), anyBoolean());
        verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any(), anyBoolean());
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(), any(), anyBoolean());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void deleteByNameAndAccountId(String valueString) {
        boolean cascading = Boolean.parseBoolean(valueString);

        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService.editDeletionType(environment, true)).thenReturn(environment);
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(EnvironmentTestData.ENVIRONMENT_NAME), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.of(environment));
        when(environmentDtoConverter.environmentToDto(any())).thenReturn(environmentDto);
        assertEquals(environmentDto, environmentDeletionServiceWired
                .deleteByNameAndAccountId(EnvironmentTestData.ENVIRONMENT_NAME, TestConstants.ACCOUNT_ID, TestConstants.USER, cascading, true));
        verify(environmentDeletionServiceWired).delete(eq(environment), eq(TestConstants.USER), anyBoolean(), anyBoolean());
        if (cascading) {
            verify(reactorFlowManager).triggerCascadingDeleteFlow(eq(environment), eq(TestConstants.USER), eq(true));
            verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any(), anyBoolean());
        } else {
            verify(reactorFlowManager).triggerDeleteFlow(eq(environment), eq(TestConstants.USER), eq(true));
            verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(), any(), anyBoolean());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void deleteByCrnAndAccountIdNotFound(String valueString) {
        boolean cascading = Boolean.parseBoolean(valueString);

        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService.editDeletionType(environment, true)).thenReturn(environment);
        when(environmentService
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TestConstants.CRN), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> environmentDeletionServiceWired.deleteByCrnAndAccountId(
                        TestConstants.CRN, TestConstants.ACCOUNT_ID, TestConstants.USER, cascading, true));
        verify(environmentDeletionServiceWired, never()).delete(eq(environment), eq(TestConstants.USER), anyBoolean(), anyBoolean());
        verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any(), anyBoolean());
        verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(), any(), anyBoolean());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void deleteByCrnAndAccountId(String valueString) {
        boolean cascading = Boolean.parseBoolean(valueString);

        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService.editDeletionType(environment, true)).thenReturn(environment);
        when(environmentService
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TestConstants.CRN), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.of(environment));
        when(environmentDtoConverter.environmentToDto(any())).thenReturn(environmentDto);
        assertEquals(environmentDto, environmentDeletionServiceWired
                .deleteByCrnAndAccountId(TestConstants.CRN, TestConstants.ACCOUNT_ID, TestConstants.USER, cascading, true));
        verify(environmentDeletionServiceWired).delete(eq(environment), eq(TestConstants.USER), anyBoolean(), anyBoolean());
        verify(environmentJobService).unschedule(any());
        if (cascading) {
            verify(reactorFlowManager).triggerCascadingDeleteFlow(eq(environment), eq(TestConstants.USER), eq(true));
            verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any(), anyBoolean());
        } else {
            verify(reactorFlowManager).triggerDeleteFlow(eq(environment), eq(TestConstants.USER), eq(true));
            verify(reactorFlowManager, never()).triggerCascadingDeleteFlow(any(), any(), anyBoolean());
        }
    }

    @Test
    public void delete() {
        when(environmentService.editDeletionType(environment, false)).thenReturn(environment);
        assertEquals(environment, environmentDeletionService.delete(environment, TestConstants.USER, false, false));
        verify(reactorFlowManager).triggerDeleteFlow(eq(environment), eq(TestConstants.USER), eq(false));
    }

    @Test
    public void deleteFailedDistroXesAreAttached() {
        when(environmentService.editDeletionType(environment, false)).thenReturn(environment);
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(eq(environment))).thenReturn(Set.of("notempty"));
        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environment, TestConstants.USER, false, false));
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environment), eq(TestConstants.USER), eq(false));
    }

    @Test
    public void deleteFailedDataLakesAreAttached() {
        when(environmentService.editDeletionType(environment, false)).thenReturn(environment);
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(eq(environment))).thenReturn(Set.of("notempty"));
        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environment, TestConstants.USER, false, false));
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environment), eq(TestConstants.USER), eq(false));
    }

    @Test
    public void deleteMultipleByNames() {
        Set<String> names = Set.of("name1", "name2");
        Set<Environment> envs = Set.of(new Environment(), new Environment());
        int expected = envs.size();
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);

        when(environmentService.editDeletionType(any(), anyBoolean())).thenReturn(new Environment());
        when(environmentService
                .findByNameInAndAccountIdAndArchivedIsFalse(eq(names), eq(TestConstants.ACCOUNT_ID))).thenReturn(envs);
        assertEquals(expected, environmentDeletionServiceWired
                .deleteMultipleByNames(names, TestConstants.ACCOUNT_ID, TestConstants.USER, false, false).size());
        verify(environmentDeletionServiceWired, times(expected)).delete(any(), eq(TestConstants.USER), anyBoolean(), anyBoolean());
    }

    @Test
    public void deleteMultipleByCrns() {
        environmentDeletionService
                .deleteMultipleByCrns(Set.of("crn1", "crn2"), TestConstants.ACCOUNT_ID, TestConstants.USER, false, false);

        Set<String> names = Set.of("crn1", "crn2");
        Set<Environment> envs = Set.of(new Environment(), new Environment());
        int expected = envs.size();
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);

        when(environmentService.editDeletionType(any(), anyBoolean())).thenReturn(new Environment());
        when(environmentService
                .findByResourceCrnInAndAccountIdAndArchivedIsFalse(eq(names), eq(TestConstants.ACCOUNT_ID))).thenReturn(envs);
        assertEquals(expected, environmentDeletionServiceWired.deleteMultipleByCrns(names, TestConstants.ACCOUNT_ID, TestConstants.USER,
                false, false).size());
        verify(environmentDeletionServiceWired, times(expected)).delete(any(), eq(TestConstants.USER), anyBoolean(), anyBoolean());
    }

    @Test
    public void testCheckIsEnvironmentDeletableWhenSdxAndDatalakeAreEmpty() {
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environment)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getDatalakeClusterNames(environment)).thenReturn(emptySet());

        environmentDeletionService.checkIsEnvironmentDeletable(environment);
        verify(environmentResourceDeletionService).getAttachedDistroXClusterNames(environment);
    }

    @Test
    public void testCheckIsEnvironmentDeletableWhenSdxIsEmptyButDatalakeIsNot() {
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(environment)).thenReturn(emptySet());
        when(environmentResourceDeletionService.getDatalakeClusterNames(environment)).thenReturn(Set.of("name"));

        BadRequestException actual = assertThrows(BadRequestException.class, () -> environmentDeletionService.checkIsEnvironmentDeletable(environment));
        assertEquals("The following Data Lake cluster(s) must be terminated before Environment deletion [name]", actual.getMessage());
    }

    @Test
    public void canNotDeleteParentEnvironment() {
        when(environmentService.findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(environment.getAccountId(), environment.getId()))
                .thenReturn(singletonList("child name"));

        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environment, TestConstants.USER, false, false));
    }
}
