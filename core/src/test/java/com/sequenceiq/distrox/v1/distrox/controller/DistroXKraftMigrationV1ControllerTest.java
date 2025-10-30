package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.doAs;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.flow.api.model.FlowLogResponse;
import com.sequenceiq.flow.service.FlowService;

@ExtendWith(MockitoExtension.class)
class DistroXKraftMigrationV1ControllerTest {

    private static final String CRN = "crn";

    private static final String ACCOUNT_ID = "accountId";

    private static final String TEST_USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:mockuser@cloudera.com";

    @Mock
    private StackOperationService stackOperationService;

    @Mock
    private FlowService flowService;

    @InjectMocks
    private DistroXKraftMigrationV1Controller underTest;

    @Test
    void testMigrateZookeeperToKraftByCrn() {
        doAs(TEST_USER_CRN, () -> {
            try {
                underTest.migrateFromZookeeperToKraftByCrn(CRN);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        verify(stackOperationService).triggerZookeeperToKraftMigration(NameOrCrn.ofCrn(CRN), "accountId");
    }

    @Test
    void testFinalizeMigrationFromZookeeperToKraftByCrn() {
        doAs(TEST_USER_CRN, () -> {
            try {
                underTest.finalizeMigrationFromZookeeperToKraftByCrn(CRN);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        verify(stackOperationService).triggerZookeeperToKraftMigrationFinalization(NameOrCrn.ofCrn(CRN), "accountId");
    }

    @Test
    void testRollbackMigrationFromZookeeperToKraftByCrn() {
        doAs(TEST_USER_CRN, () -> {
            try {
                underTest.rollbackMigrationFromZookeeperToKraftByCrn(CRN);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        verify(stackOperationService).triggerZookeeperToKraftMigrationRollback(NameOrCrn.ofCrn(CRN), "accountId");
    }

    @Test
    void testZookeeperToKraftMigrationStatusByCrn() {
        FlowLogResponse flowLogResponse = mock(FlowLogResponse.class);
        when(flowService.getAllFlowLogsByResourceCrnAndFlowTypes(eq(CRN), anyList())).thenReturn(List.of(flowLogResponse));
        doAs(TEST_USER_CRN, () -> {
            try {
                underTest.zookeeperToKraftMigrationStatusByCrn(CRN);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        verify(stackOperationService).getKraftMigrationStatus(NameOrCrn.ofCrn(CRN), "accountId", List.of(flowLogResponse));
    }
}