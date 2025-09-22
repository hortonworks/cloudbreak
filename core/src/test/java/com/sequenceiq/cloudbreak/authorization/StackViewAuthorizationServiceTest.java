package com.sequenceiq.cloudbreak.authorization;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATALAKE;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.authorization.service.CommonPermissionCheckingUtils;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.view.StackView;
import com.sequenceiq.cloudbreak.service.stack.StackViewService;

@ExtendWith(MockitoExtension.class)
public class StackViewAuthorizationServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    @Mock
    private CommonPermissionCheckingUtils commonPermissionCheckingUtils;

    @Mock
    private StackViewService stackViewService;

    @InjectMocks
    private StackViewAuthorizationService underTest;

    @Test
    void testPermissionCheckByCrn() {
        StackView stackView = mock(StackView.class);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(stackView.getResourceCrn()).thenReturn("crn");
        when(stackView.getEnvironmentCrn()).thenReturn("envCrn");
        when(stackViewService.findByCrn(any(), any())).thenReturn(Optional.of(stackView));
        when(commonPermissionCheckingUtils.getPermissionsForUserOnResources(any(), any(), any())).thenReturn(Map.of());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkReadPermissionForStackCrn("crn", 1L));

        verify(stackViewService).findByCrn(any(), any());
    }

    @Test
    void testPermissionCheckByName() {
        StackView stackView = mock(StackView.class);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(stackView.getResourceCrn()).thenReturn("crn");
        when(stackView.getEnvironmentCrn()).thenReturn("envCrn");
        when(stackViewService.findByName(any(), any())).thenReturn(Optional.of(stackView));
        when(commonPermissionCheckingUtils.getPermissionsForUserOnResources(any(), any(), any())).thenReturn(Map.of());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkReadPermissionForStackName("name", 1L));

        verify(stackViewService).findByName(any(), any());
    }

    @Test
    void testPermissionCheckForDatalake() {
        StackView stackView = mock(StackView.class);
        when(stackView.getType()).thenReturn(StackType.DATALAKE);
        when(stackView.getResourceCrn()).thenReturn("crn");
        when(stackView.getEnvironmentCrn()).thenReturn("envCrn");
        when(commonPermissionCheckingUtils.getPermissionsForUserOnResources(any(), any(), any()))
                .thenReturn(Map.of("crn", FALSE, "envCrn", TRUE));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkReadPermissionForStackView(stackView));

        verify(commonPermissionCheckingUtils).getPermissionsForUserOnResources(eq(DESCRIBE_DATALAKE), eq(USER_CRN), eq(List.of("crn", "envCrn")));
    }

    @Test
    void testPermissionCheckForDatahub() {
        StackView stackView = mock(StackView.class);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(stackView.getResourceCrn()).thenReturn("crn");
        when(stackView.getEnvironmentCrn()).thenReturn("envCrn");
        when(commonPermissionCheckingUtils.getPermissionsForUserOnResources(any(), any(), any()))
                .thenReturn(Map.of("crn", FALSE, "envCrn", TRUE));

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkReadPermissionForStackView(stackView));

        verify(commonPermissionCheckingUtils).getPermissionsForUserOnResources(eq(DESCRIBE_DATAHUB), eq(USER_CRN), eq(List.of("crn", "envCrn")));
    }

    @Test
    void testPermissionCheckWhenAuthzFails() {
        StackView stackView = mock(StackView.class);
        when(stackView.getType()).thenReturn(StackType.WORKLOAD);
        when(stackView.getResourceCrn()).thenReturn("crn");
        when(stackView.getEnvironmentCrn()).thenReturn("envCrn");
        when(commonPermissionCheckingUtils.getPermissionsForUserOnResources(any(), any(), any()))
                .thenReturn(Map.of("crn", FALSE, "envCrn", FALSE));
        doThrow(new ForbiddenException()).when(commonPermissionCheckingUtils).throwAccessDeniedIfActionNotAllowed(any(), any());

        assertThrows(ForbiddenException.class, () ->
                ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.checkReadPermissionForStackView(stackView)));

        verify(commonPermissionCheckingUtils).getPermissionsForUserOnResources(eq(DESCRIBE_DATAHUB), eq(USER_CRN), eq(List.of("crn", "envCrn")));
    }
}
