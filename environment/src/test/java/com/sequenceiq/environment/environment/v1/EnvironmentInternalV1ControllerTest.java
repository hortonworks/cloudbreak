package com.sequenceiq.environment.environment.v1;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.v1.converter.EnvironmentResponseConverter;
import com.sequenceiq.environment.parameters.service.ParametersService;
import com.sequenceiq.notification.domain.DistributionList;
import com.sequenceiq.notification.domain.DistributionListActionType;
import com.sequenceiq.notification.domain.NotificationGroupType;
import com.sequenceiq.notification.domain.NotificationSeverity;
import com.sequenceiq.notification.sender.DistributionListManagementService;

@ExtendWith(MockitoExtension.class)
class EnvironmentInternalV1ControllerTest {

    private static final Long ENV_ID = 42L;

    private static final String ENV_NAME = "test-environment";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:env-uuid";

    private static final String TARGET_RESOURCE_CRN = "crn:cdp:datalake:us-west-1:tenant:datalake:dl-uuid";

    private static final String TARGET_RESOURCE_NAME = "test-datalake";

    @Mock
    private CredentialService credentialService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EnvironmentResponseConverter environmentResponseConverter;

    @Mock
    private DistributionListManagementService distributionListManagementService;

    @Mock
    private ParametersService parametersService;

    @InjectMocks
    private EnvironmentInternalV1Controller underTest;

    private EnvironmentDto environmentDto;

    @BeforeEach
    void setUp() {
        environmentDto = EnvironmentDto.builder()
                .withId(ENV_ID)
                .withName(ENV_NAME)
                .withResourceCrn(ENV_CRN)
                .build();
        when(environmentService.internalGetByCrn(ENV_CRN)).thenReturn(environmentDto);
    }

    @Test
    void createOrUpdateDistributionListWhenTargetCrnAndNameProvidedThenUsesThemAsIs() {
        DistributionList distributionList = DistributionList.builder()
                .externalDistributionListId("dl-external-id")
                .resourceCrn(TARGET_RESOURCE_CRN)
                .build();
        when(distributionListManagementService.createOrUpdateList(
                eq(ENV_CRN),
                eq(ENV_NAME),
                eq(TARGET_RESOURCE_NAME),
                eq(TARGET_RESOURCE_CRN),
                eq(NotificationGroupType.DATALAKE),
                eq(NotificationSeverity.ERROR),
                eq(DistributionListActionType.REGISTRATION)
        )).thenReturn(Optional.of(distributionList));

        underTest.createOrUpdateDistributionListByEnvironmentCrn(
                ENV_CRN,
                TARGET_RESOURCE_CRN,
                TARGET_RESOURCE_NAME,
                "ERROR",
                "REGISTRATION"
        );

        verify(distributionListManagementService).createOrUpdateList(
                eq(ENV_CRN),
                eq(ENV_NAME),
                eq(TARGET_RESOURCE_NAME),
                eq(TARGET_RESOURCE_CRN),
                eq(NotificationGroupType.DATALAKE),
                eq(NotificationSeverity.ERROR),
                eq(DistributionListActionType.REGISTRATION)
        );
        verify(parametersService).updateDistributionListDetails(ENV_ID, distributionList);
    }

    @Test
    void createOrUpdateDistributionListWhenTargetCrnAndNameEmptyThenFallsBackToEnvironmentCrnAndName() {
        DistributionList distributionList = DistributionList.builder()
                .externalDistributionListId("dl-external-id")
                .resourceCrn(ENV_CRN)
                .build();
        when(distributionListManagementService.createOrUpdateList(
                eq(ENV_CRN),
                eq(ENV_NAME),
                eq(ENV_NAME),
                eq(ENV_CRN),
                eq(NotificationGroupType.ENVIRONMENT),
                eq(NotificationSeverity.ERROR),
                eq(DistributionListActionType.REGISTRATION)
        )).thenReturn(Optional.of(distributionList));

        underTest.createOrUpdateDistributionListByEnvironmentCrn(
                ENV_CRN,
                "",
                "",
                "ERROR",
                "REGISTRATION"
        );

        verify(distributionListManagementService).createOrUpdateList(
                eq(ENV_CRN),
                eq(ENV_NAME),
                eq(ENV_NAME),
                eq(ENV_CRN),
                eq(NotificationGroupType.ENVIRONMENT),
                eq(NotificationSeverity.ERROR),
                eq(DistributionListActionType.REGISTRATION)
        );
        verify(parametersService).updateDistributionListDetails(ENV_ID, distributionList);
    }

    @Test
    void createOrUpdateDistributionListWhenTargetCrnAndNameNullThenFallsBackToEnvironmentCrnAndName() {
        DistributionList distributionList = DistributionList.builder()
                .externalDistributionListId("dl-external-id")
                .resourceCrn(ENV_CRN)
                .build();
        when(distributionListManagementService.createOrUpdateList(
                eq(ENV_CRN),
                eq(ENV_NAME),
                eq(ENV_NAME),
                eq(ENV_CRN),
                eq(NotificationGroupType.ENVIRONMENT),
                eq(NotificationSeverity.ERROR),
                eq(DistributionListActionType.REGISTRATION)
        )).thenReturn(Optional.of(distributionList));

        underTest.createOrUpdateDistributionListByEnvironmentCrn(
                ENV_CRN,
                null,
                null,
                "ERROR",
                "REGISTRATION"
        );

        verify(distributionListManagementService).createOrUpdateList(
                eq(ENV_CRN),
                eq(ENV_NAME),
                eq(ENV_NAME),
                eq(ENV_CRN),
                eq(NotificationGroupType.ENVIRONMENT),
                eq(NotificationSeverity.ERROR),
                eq(DistributionListActionType.REGISTRATION)
        );
        verify(parametersService).updateDistributionListDetails(ENV_ID, distributionList);
    }

    @Test
    void createOrUpdateDistributionListWhenServiceReturnsEmptyThenParametersServiceNotCalled() {
        when(distributionListManagementService.createOrUpdateList(
                eq(ENV_CRN),
                eq(ENV_NAME),
                eq(TARGET_RESOURCE_NAME),
                eq(TARGET_RESOURCE_CRN),
                eq(NotificationGroupType.DATALAKE),
                eq(NotificationSeverity.ERROR),
                eq(DistributionListActionType.REGISTRATION)
        )).thenReturn(Optional.empty());

        underTest.createOrUpdateDistributionListByEnvironmentCrn(
                ENV_CRN,
                TARGET_RESOURCE_CRN,
                TARGET_RESOURCE_NAME,
                "ERROR",
                "REGISTRATION"
        );

        verify(parametersService, never()).updateDistributionListDetails(ENV_ID, null);
    }

    @Test
    void createOrUpdateDistributionListWhenSeverityIsInfoThenPassedCorrectly() {
        when(distributionListManagementService.createOrUpdateList(
                eq(ENV_CRN),
                eq(ENV_NAME),
                eq(TARGET_RESOURCE_NAME),
                eq(TARGET_RESOURCE_CRN),
                eq(NotificationGroupType.DATALAKE),
                eq(NotificationSeverity.INFO),
                eq(DistributionListActionType.REGISTRATION)
        )).thenReturn(Optional.empty());

        underTest.createOrUpdateDistributionListByEnvironmentCrn(
                ENV_CRN,
                TARGET_RESOURCE_CRN,
                TARGET_RESOURCE_NAME,
                "INFO",
                "REGISTRATION"
        );

        verify(distributionListManagementService).createOrUpdateList(
                eq(ENV_CRN),
                eq(ENV_NAME),
                eq(TARGET_RESOURCE_NAME),
                eq(TARGET_RESOURCE_CRN),
                eq(NotificationGroupType.DATALAKE),
                eq(NotificationSeverity.INFO),
                eq(DistributionListActionType.REGISTRATION)
        );
    }

    @Test
    void createOrUpdateDistributionListWhenSeverityIsEmptyThenDefaultsToError() {
        when(distributionListManagementService.createOrUpdateList(
                eq(ENV_CRN),
                eq(ENV_NAME),
                eq(TARGET_RESOURCE_NAME),
                eq(TARGET_RESOURCE_CRN),
                eq(NotificationGroupType.DATALAKE),
                eq(NotificationSeverity.ERROR),
                eq(DistributionListActionType.REGISTRATION)
        )).thenReturn(Optional.empty());

        underTest.createOrUpdateDistributionListByEnvironmentCrn(
                ENV_CRN,
                TARGET_RESOURCE_CRN,
                TARGET_RESOURCE_NAME,
                "",
                "REGISTRATION"
        );

        verify(distributionListManagementService).createOrUpdateList(
                eq(ENV_CRN),
                eq(ENV_NAME),
                eq(TARGET_RESOURCE_NAME),
                eq(TARGET_RESOURCE_CRN),
                eq(NotificationGroupType.DATALAKE),
                eq(NotificationSeverity.ERROR),
                eq(DistributionListActionType.REGISTRATION)
        );
    }
}
