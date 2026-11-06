package com.sequenceiq.maintenance.authorization;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowScheduleEntitlementServiceTest {

    private static final String ACCOUNT_ID = "acc-1";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private MaintenanceWindowScheduleEntitlementService underTest;

    @Test
    void verifyEntitledAllowsWhenEntitlementPresent() {
        when(entitlementService.isMaintenanceWindowEnabled(ACCOUNT_ID)).thenReturn(true);

        assertThatCode(() -> underTest.verifyEntitled(ACCOUNT_ID)).doesNotThrowAnyException();
    }

    @Test
    void verifyEntitledForbiddenWhenEntitlementMissing() {
        when(entitlementService.isMaintenanceWindowEnabled(ACCOUNT_ID)).thenReturn(false);

        assertThatThrownBy(() -> underTest.verifyEntitled(ACCOUNT_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("CDP_MAINTENANCE_WINDOW")
                .hasMessageContaining(ACCOUNT_ID);
    }
}
