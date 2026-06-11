package com.sequenceiq.distrox.v1.distrox.service.upgrade.rds;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.StackDatabaseServerResponse;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.database.MajorVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.database.DatabaseDefaultVersionProvider;
import com.sequenceiq.cloudbreak.service.database.DatabaseService;
import com.sequenceiq.cloudbreak.service.database.DbOverrideConfig;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.distrox.api.v1.distrox.model.upgrade.rds.DistroXDatabaseUpgradeStatus;

@ExtendWith(MockitoExtension.class)
class DistroXRdsUpgradeStatusServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:acc:user:user1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:acc:cluster:dh1";

    private static final String DATAHUB_NAME = "test-datahub";

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private DatabaseService databaseService;

    @Mock
    private DatabaseDefaultVersionProvider databaseDefaultVersionProvider;

    @Mock
    private DbOverrideConfig dbOverrideConfig;

    @InjectMocks
    private DistroXRdsUpgradeStatusService underTest;

    // --- helpers ---

    private StackDto mockStackDtoWithExternalDb(String crn) {
        StackDto stackDto = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stackView.getResourceCrn()).thenReturn(crn);
        when(stackView.getStackVersion()).thenReturn("7.2.18");
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDto.getExternalDatabaseCreationType()).thenReturn(DatabaseAvailabilityType.NON_HA);
        return stackDto;
    }

    private StackDto mockStackDtoWithEmbeddedDb(String crn) {
        StackDto stackDto = mock(StackDto.class);
        StackView stackView = mock(StackView.class);
        when(stackView.getResourceCrn()).thenReturn(crn);
        when(stackDto.getStack()).thenReturn(stackView);
        when(stackDto.getExternalDatabaseCreationType()).thenReturn(DatabaseAvailabilityType.NONE);
        return stackDto;
    }

    // --- getUpgradeRequiredByDatahubCrn ---

    @Test
    void testGetUpgradeRequiredByDatahubCrnWhenUpgradeNeeded() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            StackDto stackDto = mockStackDtoWithExternalDb(DATAHUB_CRN);
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(stackDto);
            when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(any(), eq(null))).thenReturn("14");
            StackDatabaseServerResponse dbResponse = new StackDatabaseServerResponse();
            dbResponse.setMajorVersion(MajorVersion.VERSION_11);
            when(databaseService.getDatabaseServer(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(dbResponse);

            DistroXDatabaseUpgradeStatus result = underTest.getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN));

            assertEquals("UPGRADE_REQUIRED", result.getUpgradeStatus());
            assertEquals(DATAHUB_CRN, result.getDatahubCrn());
            assertEquals("14", result.getTargetMajorVersion());
            assertEquals("11", result.getCurrentMajorVersion());
        });
    }

    @Test
    void testGetUpgradeRequiredByDatahubCrnWhenUpgradeNotNeeded() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            StackDto stackDto = mockStackDtoWithExternalDb(DATAHUB_CRN);
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(stackDto);
            when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(any(), eq(null))).thenReturn("14");
            StackDatabaseServerResponse dbResponse = new StackDatabaseServerResponse();
            dbResponse.setMajorVersion(MajorVersion.VERSION_14);
            when(databaseService.getDatabaseServer(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(dbResponse);

            DistroXDatabaseUpgradeStatus result = underTest.getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN));

            assertEquals("UPGRADE_NOT_REQUIRED", result.getUpgradeStatus());
            assertEquals(DATAHUB_CRN, result.getDatahubCrn());
            assertEquals("14", result.getCurrentMajorVersion());
        });
    }

    @Test
    void testGetUpgradeRequiredByDatahubCrnWhenCurrentVersionIsHigherThanTarget() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            StackDto stackDto = mockStackDtoWithExternalDb(DATAHUB_CRN);
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(stackDto);
            when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(any(), eq(null))).thenReturn("14");
            StackDatabaseServerResponse dbResponse = new StackDatabaseServerResponse();
            dbResponse.setMajorVersion(MajorVersion.VERSION_17);
            when(databaseService.getDatabaseServer(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(dbResponse);

            DistroXDatabaseUpgradeStatus result = underTest.getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN));

            assertEquals("UPGRADE_NOT_REQUIRED", result.getUpgradeStatus());
            assertEquals("17", result.getCurrentMajorVersion());
            assertNull(result.getTargetMajorVersion());
        });
    }

    @Test
    void testGetUpgradeRequiredByDatahubCrnWhenEmbeddedDatabase() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            StackDto stackDto = mockStackDtoWithEmbeddedDb(DATAHUB_CRN);
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(stackDto);

            DistroXDatabaseUpgradeStatus result = underTest.getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN));

            assertEquals("UPGRADE_NOT_REQUIRED", result.getUpgradeStatus());
            assertEquals(DATAHUB_CRN, result.getDatahubCrn());
            verifyNoInteractions(databaseService, databaseDefaultVersionProvider);
        });
    }

    @Test
    void testGetUpgradeRequiredByDatahubCrnWhenNotFound() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any()))
                    .thenThrow(new NotFoundException("not found"));

            assertThrows(NotFoundException.class, () -> underTest.getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN)));
        });
    }

    @Test
    void testGetUpgradeRequiredByDatahubCrnWhenDbLookupFails() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            StackDto stackDto = mockStackDtoWithExternalDb(DATAHUB_CRN);
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(stackDto);
            when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(any(), eq(null))).thenReturn("14");
            when(databaseService.getDatabaseServer(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any()))
                    .thenThrow(new RuntimeException("db unavailable"));

            DistroXDatabaseUpgradeStatus result = underTest.getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN));

            assertEquals("UNKNOWN", result.getUpgradeStatus());
            assertEquals(DATAHUB_CRN, result.getDatahubCrn());
        });
    }

    @Test
    void testGetUpgradeRequiredByDatahubCrnNullCurrentVersionOnNoDb() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            StackDto stackDto = mockStackDtoWithExternalDb(DATAHUB_CRN);
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(stackDto);
            when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(any(), eq(null))).thenReturn("14");
            StackDatabaseServerResponse dbResponse = new StackDatabaseServerResponse();
            // majorVersion is null (unknown) — service defaults to "10" for comparison, upgrade required
            when(databaseService.getDatabaseServer(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(dbResponse);

            DistroXDatabaseUpgradeStatus result = underTest.getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN));

            assertEquals("UPGRADE_REQUIRED", result.getUpgradeStatus());
            assertEquals("14", result.getTargetMajorVersion());
            assertNull(result.getCurrentMajorVersion());
        });
    }

    // --- getUpgradeRequiredByDatahubName ---

    @Test
    void testGetUpgradeRequiredByDatahubNameWhenUpgradeNeeded() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            StackDto stackDto = mockStackDtoWithExternalDb(DATAHUB_CRN);
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofName(DATAHUB_NAME)), any())).thenReturn(stackDto);
            when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(any(), eq(null))).thenReturn("14");
            StackDatabaseServerResponse dbResponse = new StackDatabaseServerResponse();
            dbResponse.setMajorVersion(MajorVersion.VERSION_11);
            when(databaseService.getDatabaseServer(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(dbResponse);

            DistroXDatabaseUpgradeStatus result = underTest.getUpgradeRequired(NameOrCrn.ofName(DATAHUB_NAME));

            assertEquals("UPGRADE_REQUIRED", result.getUpgradeStatus());
            assertEquals(DATAHUB_CRN, result.getDatahubCrn());
        });
    }

    @Test
    void testGetUpgradeRequiredByDatahubNameWhenNotFound() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofName(DATAHUB_NAME)), any()))
                    .thenThrow(new NotFoundException("not found"));

            assertThrows(NotFoundException.class, () -> underTest.getUpgradeRequired(NameOrCrn.ofName(DATAHUB_NAME)));
        });
    }

    @Test
    void testGetUpgradeRequiredByDatahubNameWhenUnexpectedError() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofName(DATAHUB_NAME)), any()))
                    .thenThrow(new RuntimeException("unexpected error"));

            DistroXDatabaseUpgradeStatus result = underTest.getUpgradeRequired(NameOrCrn.ofName(DATAHUB_NAME));

            assertEquals("UNKNOWN", result.getUpgradeStatus());
        });
    }

    @Test
    void testGetUpgradeRequiredByDatahubCrnWhenInvalidCrn() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> assertThrows(BadRequestException.class,
                () -> underTest.getUpgradeRequired(NameOrCrn.ofCrn("not-a-crn"))));
    }

    // --- getUpgradeRequiredByDatahubCrns (bulk) ---

    @Test
    void testGetUpgradeRequiredByDatahubCrnsReturnsBulkResultsInOrder() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            String crn1 = "crn:cdp:datahub:us-west-1:acc:cluster:aa";
            String crn2 = "crn:cdp:datahub:us-west-1:acc:cluster:bb";
            String crn3 = "crn:cdp:datahub:us-west-1:acc:cluster:cc";

            StackDto stackDto1 = mockStackDtoWithExternalDb(crn1);
            StackDto stackDto2 = mockStackDtoWithEmbeddedDb(crn2);
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(crn1)), any())).thenReturn(stackDto1);
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(crn2)), any())).thenReturn(stackDto2);
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(crn3)), any()))
                    .thenThrow(new NotFoundException("not found"));

            when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(any(), eq(null))).thenReturn("14");
            StackDatabaseServerResponse dbResponse = new StackDatabaseServerResponse();
            dbResponse.setMajorVersion(MajorVersion.VERSION_11);
            when(databaseService.getDatabaseServer(eq(NameOrCrn.ofCrn(crn1)), any())).thenReturn(dbResponse);

            List<DistroXDatabaseUpgradeStatus> statuses = underTest.getUpgradeRequiredByDatahubCrns(List.of(crn1, crn2, crn3));

            assertEquals(3, statuses.size());
            assertEquals(crn1, statuses.get(0).getDatahubCrn());
            assertEquals("UPGRADE_REQUIRED", statuses.get(0).getUpgradeStatus());
            assertEquals(crn2, statuses.get(1).getDatahubCrn());
            assertEquals("UPGRADE_NOT_REQUIRED", statuses.get(1).getUpgradeStatus());
            assertEquals(crn3, statuses.get(2).getDatahubCrn());
            assertEquals("NO_DATAHUB", statuses.get(2).getUpgradeStatus());
        });
    }

    @Test
    void testGetUpgradeRequiredByDatahubCrnsEmptyListReturnsEmptyResponse() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            List<DistroXDatabaseUpgradeStatus> statuses = underTest.getUpgradeRequiredByDatahubCrns(List.of());

            assertEquals(0, statuses.size());
            verifyNoInteractions(stackDtoService, databaseService, databaseDefaultVersionProvider);
        });
    }

    @Test
    void testGetUpgradeRequiredByDatahubCrnsWhenNullList() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> assertThrows(BadRequestException.class,
                () -> underTest.getUpgradeRequiredByDatahubCrns(null)));
    }

    @Test
    void testGetUpgradeRequiredByDatahubCrnsWhenInvalidCrnInList() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> assertThrows(BadRequestException.class,
                () -> underTest.getUpgradeRequiredByDatahubCrns(List.of(DATAHUB_CRN, "invalid-crn"))));
    }

    @Test
    void testGetUpgradeRequiredByDatahubCrnsWhenExceedsMaxBatchSize() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            List<String> tooManyCrns = Collections.nCopies(51, DATAHUB_CRN);
            assertThrows(BadRequestException.class, () -> underTest.getUpgradeRequiredByDatahubCrns(tooManyCrns));
        });
    }

    @Test
    void testEolDateSetWhenCurrentVersionIsEol() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            StackDto stackDto = mockStackDtoWithExternalDb(DATAHUB_CRN);
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(stackDto);
            when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(any(), eq(null))).thenReturn("14");
            StackDatabaseServerResponse dbResponse = new StackDatabaseServerResponse();
            dbResponse.setMajorVersion(MajorVersion.VERSION_11);
            when(databaseService.getDatabaseServer(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(dbResponse);
            when(dbOverrideConfig.getEolDate("11")).thenReturn(Optional.of(LocalDate.of(2023, 11, 9)));

            DistroXDatabaseUpgradeStatus result = underTest.getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN));

            assertEquals("UPGRADE_REQUIRED", result.getUpgradeStatus());
            assertEquals("2023-11-09", result.getEolDate());
        });
    }

    @Test
    void testEolDateNotSetWhenEolNotReached() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            StackDto stackDto = mockStackDtoWithExternalDb(DATAHUB_CRN);
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(stackDto);
            when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(any(), eq(null))).thenReturn("14");
            StackDatabaseServerResponse dbResponse = new StackDatabaseServerResponse();
            dbResponse.setMajorVersion(MajorVersion.VERSION_11);
            when(databaseService.getDatabaseServer(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(dbResponse);
            when(dbOverrideConfig.getEolDate("11")).thenReturn(Optional.of(LocalDate.of(2099, 12, 31)));

            DistroXDatabaseUpgradeStatus result = underTest.getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN));

            assertEquals("UPGRADE_REQUIRED", result.getUpgradeStatus());
            assertNull(result.getEolDate());
        });
    }

    @Test
    void testEolDateNotSetWhenCurrentVersionIsNull() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> {
            StackDto stackDto = mockStackDtoWithExternalDb(DATAHUB_CRN);
            when(stackDtoService.getByNameOrCrn(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(stackDto);
            when(databaseDefaultVersionProvider.calculateDbVersionBasedOnRuntime(any(), eq(null))).thenReturn("14");
            StackDatabaseServerResponse dbResponse = new StackDatabaseServerResponse();
            when(databaseService.getDatabaseServer(eq(NameOrCrn.ofCrn(DATAHUB_CRN)), any())).thenReturn(dbResponse);

            DistroXDatabaseUpgradeStatus result = underTest.getUpgradeRequired(NameOrCrn.ofCrn(DATAHUB_CRN));

            assertNull(result.getEolDate());
            verifyNoInteractions(dbOverrideConfig);
        });
    }
}
