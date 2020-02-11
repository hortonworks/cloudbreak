package com.sequenceiq.environment.environment.service;

import static java.util.Collections.emptySet;
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

public class EnvironmentDeletionServiceTest {

    private final EnvironmentService environmentService = Mockito.mock(EnvironmentService.class);

    private final EnvironmentDtoConverter environmentDtoConverter = Mockito.mock(EnvironmentDtoConverter.class);

    private final EnvironmentReactorFlowManager reactorFlowManager = Mockito.mock(EnvironmentReactorFlowManager.class);

    private final EnvironmentResourceDeletionService environmentResourceDeletionService = Mockito.mock(EnvironmentResourceDeletionService.class);

    private final EnvironmentDeletionService environmentDeletionService
            = new EnvironmentDeletionService(environmentService, environmentDtoConverter, reactorFlowManager, environmentResourceDeletionService);

    private Environment environment;

    private EnvironmentDto environmentDto;

    @BeforeEach
    public void setup() {
        environment = new Environment();
        environmentDto = new EnvironmentDto();
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void deleteByNameAndAccountIdNotFound(String valueString) {
        boolean forced = Boolean.parseBoolean(valueString);

        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(EnvironmentTestData.ENVIRONMENT_NAME), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> environmentDeletionServiceWired
                        .deleteByNameAndAccountId(EnvironmentTestData.ENVIRONMENT_NAME, TestConstants.ACCOUNT_ID, TestConstants.USER,
                                forced));
        verify(environmentDeletionServiceWired, never()).delete(eq(environment), eq(TestConstants.USER), anyBoolean());
        verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any());
        verify(reactorFlowManager, never()).triggerForcedDeleteFlow(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void deleteByNameAndAccountId(String valueString) {
        boolean forced = Boolean.parseBoolean(valueString);

        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService
                .findByNameAndAccountIdAndArchivedIsFalse(eq(EnvironmentTestData.ENVIRONMENT_NAME), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.of(environment));
        when(environmentDtoConverter.environmentToDto(any())).thenReturn(environmentDto);
        assertEquals(environmentDto, environmentDeletionServiceWired
                .deleteByNameAndAccountId(EnvironmentTestData.ENVIRONMENT_NAME, TestConstants.ACCOUNT_ID, TestConstants.USER, forced));
        verify(environmentDeletionServiceWired).delete(eq(environment), eq(TestConstants.USER), anyBoolean());
        if (forced) {
            verify(reactorFlowManager).triggerForcedDeleteFlow(eq(environment), eq(TestConstants.USER));
            verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any());
        } else {
            verify(reactorFlowManager).triggerDeleteFlow(eq(environment), eq(TestConstants.USER));
            verify(reactorFlowManager, never()).triggerForcedDeleteFlow(any(), any());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void deleteByCrnAndAccountIdNotFound(String valueString) {
        boolean forced = Boolean.parseBoolean(valueString);

        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TestConstants.CRN), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> environmentDeletionServiceWired.deleteByCrnAndAccountId(
                        TestConstants.CRN, TestConstants.ACCOUNT_ID, TestConstants.USER, forced));
        verify(environmentDeletionServiceWired, never()).delete(eq(environment), eq(TestConstants.USER), anyBoolean());
        verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any());
        verify(reactorFlowManager, never()).triggerForcedDeleteFlow(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"true", "false"})
    public void deleteByCrnAndAccountId(String valueString) {
        boolean forced = Boolean.parseBoolean(valueString);

        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);
        when(environmentService
                .findByResourceCrnAndAccountIdAndArchivedIsFalse(eq(TestConstants.CRN), eq(TestConstants.ACCOUNT_ID)))
                .thenReturn(Optional.of(environment));
        when(environmentDtoConverter.environmentToDto(any())).thenReturn(environmentDto);
        assertEquals(environmentDto, environmentDeletionServiceWired
                .deleteByCrnAndAccountId(TestConstants.CRN, TestConstants.ACCOUNT_ID, TestConstants.USER, forced));
        verify(environmentDeletionServiceWired).delete(eq(environment), eq(TestConstants.USER), anyBoolean());
        if (forced) {
            verify(reactorFlowManager).triggerForcedDeleteFlow(eq(environment), eq(TestConstants.USER));
            verify(reactorFlowManager, never()).triggerDeleteFlow(any(), any());
        } else {
            verify(reactorFlowManager).triggerDeleteFlow(eq(environment), eq(TestConstants.USER));
            verify(reactorFlowManager, never()).triggerForcedDeleteFlow(any(), any());
        }
    }

    @Test
    public void delete() {
        assertEquals(environment, environmentDeletionService.delete(environment, TestConstants.USER, false));
        verify(reactorFlowManager).triggerDeleteFlow(eq(environment), eq(TestConstants.USER));
    }

    @Test
    public void deleteFailedDistroXesAreAttached() {
        when(environmentResourceDeletionService.getAttachedDistroXClusterNames(eq(environment))).thenReturn(Set.of("notempty"));
        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environment, TestConstants.USER, false));
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environment), eq(TestConstants.USER));
    }

    @Test
    public void deleteFailedDataLakesAreAttached() {
        when(environmentResourceDeletionService.getAttachedSdxClusterCrns(eq(environment))).thenReturn(Set.of("notempty"));
        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environment, TestConstants.USER, false));
        verify(reactorFlowManager, never()).triggerDeleteFlow(eq(environment), eq(TestConstants.USER));
    }

    @Test
    public void deleteMultipleByNames() {
        Set<String> names = Set.of("name1", "name2");
        Set<Environment> envs = Set.of(new Environment(), new Environment());
        int expected = envs.size();
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);

        when(environmentService
                .findByNameInAndAccountIdAndArchivedIsFalse(eq(names), eq(TestConstants.ACCOUNT_ID))).thenReturn(envs);
        assertEquals(expected, environmentDeletionServiceWired
                .deleteMultipleByNames(names, TestConstants.ACCOUNT_ID, TestConstants.USER, false).size());
        verify(environmentDeletionServiceWired, times(expected)).delete(any(), eq(TestConstants.USER), anyBoolean());
    }

    @Test
    public void deleteMultipleByCrns() {
        environmentDeletionService
                .deleteMultipleByCrns(Set.of("crn1", "crn2"), TestConstants.ACCOUNT_ID, TestConstants.USER, false);

        Set<String> names = Set.of("crn1", "crn2");
        Set<Environment> envs = Set.of(new Environment(), new Environment());
        int expected = envs.size();
        EnvironmentDeletionService environmentDeletionServiceWired = spy(environmentDeletionService);

        when(environmentService
                .findByResourceCrnInAndAccountIdAndArchivedIsFalse(eq(names), eq(TestConstants.ACCOUNT_ID))).thenReturn(envs);
        assertEquals(expected, environmentDeletionServiceWired.deleteMultipleByCrns(names, TestConstants.ACCOUNT_ID, TestConstants.USER,
                false).size());
        verify(environmentDeletionServiceWired, times(expected)).delete(any(), eq(TestConstants.USER), anyBoolean());
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
        when(environmentService.existsWithAccountIdAndParentEnvIdAndArchivedIsFalse(environment.getAccountId(), environment.getId())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> environmentDeletionService.delete(environment, TestConstants.USER, false));
    }
}
