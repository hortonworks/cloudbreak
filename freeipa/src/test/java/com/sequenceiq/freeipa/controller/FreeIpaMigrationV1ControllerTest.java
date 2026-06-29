package com.sequenceiq.freeipa.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.crn.AccountIdService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.util.TestConstants;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.freeipa.api.v1.freeipa.migration.model.FreeIpaMultiAzMigrationV1Request;
import com.sequenceiq.freeipa.api.v1.freeipa.migration.model.FreeIpaMultiAzMigrationV1Response;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.migration.MultiAzMigrationService;
import com.sequenceiq.freeipa.service.migration.MultiAzMigrationValidationService;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaMigrationV1ControllerTest {

    @Mock
    private AccountIdService accountIdService;

    @Mock
    private StackService stackService;

    @Mock
    private MultiAzMigrationValidationService multiAzMigrationValidationService;

    @Mock
    private MultiAzMigrationService multiAzMigrationService;

    @InjectMocks
    private FreeIpaMigrationV1Controller underTest;

    @BeforeEach
    void setUp() {
        when(accountIdService.getAccountIdFromResourceCrn(any())).thenCallRealMethod();
    }

    @Test
    void testMigrateToMultiAz() {
        Stack stack = new Stack();
        FreeIpaMultiAzMigrationV1Response response = new FreeIpaMultiAzMigrationV1Response();
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(TestConstants.ENV_CRN, TestConstants.ACCOUNT_ID)).thenReturn(stack);
        when(multiAzMigrationValidationService.validateMultiAzMigrationRequest(TestConstants.ENV_CRN, TestConstants.ACCOUNT_ID, stack))
                .thenReturn(ValidationResult.empty());
        when(multiAzMigrationService.triggerMultiAzMigration(TestConstants.ENV_CRN, TestConstants.ACCOUNT_ID, stack)).thenReturn(response);

        FreeIpaMultiAzMigrationV1Request request = new FreeIpaMultiAzMigrationV1Request();
        request.setEnvironmentCrn(TestConstants.ENV_CRN);
        FreeIpaMultiAzMigrationV1Response result = underTest.migrateToMultiAz(request);

        assertEquals(response, result);
    }

    @Test
    void testMigrateToMultiAzWhenValidationFails() {
        Stack stack = new Stack();
        when(stackService.getByEnvironmentCrnAndAccountIdWithListsAndMdcContext(TestConstants.ENV_CRN, TestConstants.ACCOUNT_ID)).thenReturn(stack);
        when(multiAzMigrationValidationService.validateMultiAzMigrationRequest(TestConstants.ENV_CRN, TestConstants.ACCOUNT_ID, stack))
                .thenReturn(ValidationResult.ofError("Validation error"));

        FreeIpaMultiAzMigrationV1Request request = new FreeIpaMultiAzMigrationV1Request();
        request.setEnvironmentCrn(TestConstants.ENV_CRN);
        assertThrows(BadRequestException.class, () -> underTest.migrateToMultiAz(request));

        verifyNoInteractions(multiAzMigrationService);
    }

    @NullSource
    @ValueSource(strings = {"invalidCrn"})
    @ParameterizedTest
    void testMigrateToMultiAzWhenInvalidEnvironmentCrn(String environmentCrn) {
        FreeIpaMultiAzMigrationV1Request request = new FreeIpaMultiAzMigrationV1Request();
        request.setEnvironmentCrn(environmentCrn);
        assertThrows(BadRequestException.class, () -> underTest.migrateToMultiAz(request));

        verifyNoInteractions(stackService);
        verifyNoInteractions(multiAzMigrationValidationService);
        verifyNoInteractions(multiAzMigrationService);
    }
}
