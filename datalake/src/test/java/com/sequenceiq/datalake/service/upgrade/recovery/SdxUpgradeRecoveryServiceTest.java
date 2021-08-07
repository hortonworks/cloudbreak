package com.sequenceiq.datalake.service.upgrade.recovery;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.message.CloudbreakMessagesService;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.flow.SdxReactorFlowManager;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.datalake.service.sdx.status.SdxStatusService;
import com.sequenceiq.sdx.api.model.SdxRecoveryRequest;
import com.sequenceiq.sdx.api.model.SdxRecoveryType;

@ExtendWith(MockitoExtension.class)
public class SdxUpgradeRecoveryServiceTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:cloudera:user:bob@cloudera.com";

    private static final String CLUSTER_NAME = "dummyCluster";

    @Mock
    private SdxService sdxService;

    @Mock
    private SdxStatusService sdxStatusService;

    @Mock
    private SdxReactorFlowManager sdxReactorFlowManager;

    @Mock
    private CloudbreakMessagesService messagesService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SdxUpgradeRecoveryService underTest;

    @Mock
    private SdxCluster cluster;

    private SdxRecoveryRequest request;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(underTest, "recoverableStatuses", List.of("DATALAKE_UPGRADE_FAILED", "RUNNING"));
        request = new SdxRecoveryRequest();
        request.setType(SdxRecoveryType.RECOVER_WITHOUT_DATA);
        when(cluster.getClusterName()).thenReturn(CLUSTER_NAME);
        when(sdxService.getByNameOrCrn(USER_CRN, NameOrCrn.ofName(CLUSTER_NAME))).thenReturn(cluster);
    }

    @Test
    public void validateDatalakeStatusNotAccessible() {
        when(sdxStatusService.getActualStatusForSdx(any())).thenReturn(null);
        String expeectedException = "Datalake cluster status with name dummyCluster could not be determined.";

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.triggerRecovery(USER_CRN, NameOrCrn.ofName(CLUSTER_NAME), request));
        assertEquals(expeectedException, exception.getMessage());
    }

    @Test
    public void validateDatalakeStatusRecoverable() {
        SdxStatusEntity statusEntity = new SdxStatusEntity();
        statusEntity.setStatus(DatalakeStatusEnum.DATALAKE_UPGRADE_FAILED);
        when(sdxStatusService.getActualStatusForSdx(any())).thenReturn(statusEntity);

        assertDoesNotThrow(() -> underTest.triggerRecovery(USER_CRN, NameOrCrn.ofName(CLUSTER_NAME), request));
    }

    @Test
    public void validateDatalakeStatusNotRecoverable() {
        SdxStatusEntity statusEntity = new SdxStatusEntity();
        statusEntity.setStatus(DatalakeStatusEnum.STOPPED);

        when(sdxStatusService.getActualStatusForSdx(any())).thenReturn(statusEntity);
        when(sdxStatusService.findLastStatusByIdAndStatuses(any(), any())).thenReturn(Optional.empty());

        String expectedException = "Current datalake cluster status is STOPPED, it should have been in either of"
                + " [DATALAKE_UPGRADE_FAILED, RUNNING] statuses now or previously to be able to start the recovery.";

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> underTest.triggerRecovery(USER_CRN, NameOrCrn.ofName(CLUSTER_NAME), request));
        assertEquals(expectedException, exception.getMessage());
    }

    @Test
    public void validateDatalakeStatusHistoryHasRecoverableStatus() {
        SdxStatusEntity statusEntity = new SdxStatusEntity();
        statusEntity.setStatus(DatalakeStatusEnum.RUNNING);
        SdxStatusEntity formerStatusEntity = new SdxStatusEntity();
        formerStatusEntity.setStatus(DatalakeStatusEnum.DATALAKE_UPGRADE_FAILED);

        when(sdxStatusService.getActualStatusForSdx(any())).thenReturn(statusEntity);
        when(sdxStatusService.findLastStatusByIdAndStatuses(any(), any())).thenReturn(Optional.of(formerStatusEntity));

        assertDoesNotThrow(() -> underTest.triggerRecovery(USER_CRN, NameOrCrn.ofName(CLUSTER_NAME), request));
    }
}