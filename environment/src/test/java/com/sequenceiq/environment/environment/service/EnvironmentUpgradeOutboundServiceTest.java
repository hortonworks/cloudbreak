package com.sequenceiq.environment.environment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackOutboundTypeValidationV4Response;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.common.api.type.OutboundType;
import com.sequenceiq.environment.api.v1.environment.model.response.OutboundTypeValidationResponse;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;

@ExtendWith(MockitoExtension.class)
class EnvironmentUpgradeOutboundServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:tenantId:environment:environmentId";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:tenantId:user:userId";

    private static final String VALIDATION_MESSAGE = "Validation message";

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private StackV4Endpoint stackV4Endpoint;

    @InjectMocks
    private EnvironmentUpgradeOutboundService underTest;

    @Test
    void validateOutboundTypesShouldReturnValidResponseWhenCalled() {
        // GIVEN
        StackOutboundTypeValidationV4Response stackResponse = new StackOutboundTypeValidationV4Response();
        stackResponse.setStackOutboundTypeMap(Map.of("cluster1", OutboundType.PUBLIC_IP));
        stackResponse.setMessage(VALIDATION_MESSAGE);

        try (MockedStatic<ThreadBasedUserCrnProvider> userCrnProviderMock = mockStatic(ThreadBasedUserCrnProvider.class)) {
            userCrnProviderMock.when(ThreadBasedUserCrnProvider::getUserCrn).thenReturn(USER_CRN);
            userCrnProviderMock.when(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(Supplier.class)))
                    .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());

            when(freeIpaService.getNetworkOutbound(ENVIRONMENT_CRN)).thenReturn(OutboundType.PUBLIC_IP);
            when(stackV4Endpoint.validateStackOutboundTypes(anyLong(), anyString())).thenReturn(stackResponse);

            // WHEN
            OutboundTypeValidationResponse result = underTest.validateOutboundTypes(ENVIRONMENT_CRN);

            // THEN
            assertEquals(VALIDATION_MESSAGE, result.getMessage());
            assertEquals(OutboundType.PUBLIC_IP, result.getIpaOutboundType());
            assertEquals(Map.of("cluster1", OutboundType.PUBLIC_IP), result.getStackOutboundTypeMap());
        }
    }

    @Test
    void validateOutboundTypesShouldHandleNullValuesInResponse() {
        // GIVEN
        StackOutboundTypeValidationV4Response stackResponse = new StackOutboundTypeValidationV4Response();
        stackResponse.setStackOutboundTypeMap(null);
        stackResponse.setMessage(null);

        try (MockedStatic<ThreadBasedUserCrnProvider> userCrnProviderMock = mockStatic(ThreadBasedUserCrnProvider.class)) {
            userCrnProviderMock.when(ThreadBasedUserCrnProvider::getUserCrn).thenReturn(USER_CRN);
            userCrnProviderMock.when(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(Supplier.class)))
                    .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());

            when(freeIpaService.getNetworkOutbound(ENVIRONMENT_CRN)).thenReturn(null);
            when(stackV4Endpoint.validateStackOutboundTypes(anyLong(), anyString())).thenReturn(stackResponse);

            // WHEN
            OutboundTypeValidationResponse result = underTest.validateOutboundTypes(ENVIRONMENT_CRN);

            // THEN
            assertNull(result.getMessage());
            assertNull(result.getIpaOutboundType());
            assertNull(result.getStackOutboundTypeMap());
        }
    }

    @Test
    void validateOutboundTypesShouldHandleEmptyMapInResponse() {
        // GIVEN
        StackOutboundTypeValidationV4Response stackResponse = new StackOutboundTypeValidationV4Response();
        stackResponse.setStackOutboundTypeMap(Map.of());
        stackResponse.setMessage(VALIDATION_MESSAGE);

        try (MockedStatic<ThreadBasedUserCrnProvider> userCrnProviderMock = mockStatic(ThreadBasedUserCrnProvider.class)) {
            userCrnProviderMock.when(ThreadBasedUserCrnProvider::getUserCrn).thenReturn(USER_CRN);
            userCrnProviderMock.when(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(Supplier.class)))
                    .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(0)).get());

            when(freeIpaService.getNetworkOutbound(ENVIRONMENT_CRN)).thenReturn(OutboundType.DEFAULT);
            when(stackV4Endpoint.validateStackOutboundTypes(anyLong(), anyString())).thenReturn(stackResponse);

            // WHEN
            OutboundTypeValidationResponse result = underTest.validateOutboundTypes(ENVIRONMENT_CRN);

            // THEN
            assertEquals(VALIDATION_MESSAGE, result.getMessage());
            assertEquals(OutboundType.DEFAULT, result.getIpaOutboundType());
            assertEquals(Map.of(), result.getStackOutboundTypeMap());
        }
    }
}
