package com.sequenceiq.datalake.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.parameter.stack.AzureStackV4Parameters;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.ha.NodeConfig;
import com.sequenceiq.cloudbreak.structuredevent.event.DatabaseDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.DatalakeDetails;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxStatusRepository;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.sdx.api.model.SdxClusterShape;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@ExtendWith(MockitoExtension.class)
class DatalakeStructuredFlowEventFactoryTest {

    private static final long SDX_ID = 1L;

    private static final long TIMESTAMP = 1L;

    @Mock
    private SdxService mockSdxService;

    @Mock
    private NodeConfig mockNodeConfig;

    @Mock
    private Clock mockClock;

    @Mock
    private SdxStatusRepository mockSdxStatusRepository;

    @Mock
    private SdxStatusEntity mockSdxStatusEntity;

    @Mock
    private FlowDetails mockFlowDetails;

    @InjectMocks
    private DatalakeStructuredFlowEventFactory underTest;

    private SdxCluster cluster;

    @BeforeEach
    void setUp() {
        cluster = createSdxCluster();
        setUpSdxStatusEntity(cluster);
        lenient().when(mockNodeConfig.getId()).thenReturn("nodeId");
        lenient().when(mockSdxService.getById(SDX_ID)).thenReturn(cluster);
        lenient().when(mockClock.getCurrentTimeMillis()).thenReturn(TIMESTAMP);
        lenient().when(mockFlowDetails.getFlowState()).thenReturn(DatalakeStatusEnum.RUNNING.name());
        lenient().when(mockSdxStatusRepository.findFirstByDatalakeIsOrderByIdDesc(eq(cluster))).thenReturn(mockSdxStatusEntity);
    }

    @Test
    void testMakeCdpOperationDetailsWhenExceptionIsNull() {
        CDPStructuredFlowEvent<DatalakeDetails> result = underTest.createStructuredFlowEvent(SDX_ID, mockFlowDetails, null);

        assertEquals(SDX_ID, result.getOperation().getResourceId());
        assertEquals(cluster.getResourceCrn(), result.getOperation().getResourceCrn());
        assertEquals(cluster.getClusterName(), result.getOperation().getResourceName());
        assertEquals(cluster.getAccountId(), result.getOperation().getAccountId());
        assertEquals(cluster.getEnvCrn(), result.getOperation().getEnvironmentCrn());
        assertEquals(TIMESTAMP, result.getOperation().getTimestamp());
        assertTrue(StringUtils.isNotEmpty(result.getOperation().getUuid()));
        assertEquals("datalake", result.getOperation().getResourceType());
        assertEquals(StructuredEventType.FLOW, result.getOperation().getEventType());
        assertEquals("nodeId", result.getOperation().getCloudbreakId());
        assertEquals(mockSdxStatusEntity.getStatus().name(), result.getStatus());
        assertEquals(mockSdxStatusEntity.getStatusReason(), result.getStatusReason());
        assertNull(result.getException());
        DatalakeDetails datalakeDetails = result.getPayload();
        assertNotNull(datalakeDetails);
        assertEquals("AZURE", datalakeDetails.getCloudPlatform());
        assertEquals(mockSdxStatusEntity.getStatus().name(), datalakeDetails.getStatus());
        assertEquals(mockSdxStatusEntity.getStatusReason(), datalakeDetails.getStatusReason());
        DatabaseDetails databaseDetails = datalakeDetails.getDatabaseDetails();
        assertEquals("1", databaseDetails.getEngineVersion());
        assertEquals("HA", databaseDetails.getAvailabilityType());
    }

    @Test
    void testMakeCdpOperationDetailsWhenExceptionIsNotNull() {
        Exception exception = new Exception("someMessage");
        CDPStructuredFlowEvent<DatalakeDetails> result = underTest.createStructuredFlowEvent(SDX_ID, null, exception);

        assertTrue(result.getException().contains(exception.getMessage()));
    }

    private SdxCluster createSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setCreatorClient("cdp-ui");
        sdxCluster.setAccountId("accountId");
        sdxCluster.setResourceCrn("crn:cdp:sdx:us-west-1:1234:sdxcluster:mystack");
        sdxCluster.setClusterName("sdxName");
        sdxCluster.setClusterShape(SdxClusterShape.MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("someEnvName");
        sdxCluster.setEnvCrn("crn:cdp:environments:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:environment:12474ddc-6e44-4f4c-806a-b197ef12cbb8");
        sdxCluster.setCertExpirationState(CertExpirationState.VALID);
        sdxCluster.setDetached(false);
        sdxCluster.setDeleted(0L);
        sdxCluster.setSdxClusterServiceVersion("7.2.0");
        sdxCluster.setCloudStorageBaseLocation("cloudStorageBaseLocation");
        sdxCluster.setCloudStorageFileSystemType(FileSystemType.ADLS);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseAvailabilityType(SdxDatabaseAvailabilityType.HA);
        sdxDatabase.setDatabaseEngineVersion("1");
        sdxCluster.setSdxDatabase(sdxDatabase);
        StackV4Request stackRequest = new StackV4Request();
        stackRequest.setAzure(new AzureStackV4Parameters());
        sdxCluster.setStackRequest(stackRequest);


        return sdxCluster;
    }

    private void setUpSdxStatusEntity(SdxCluster cluster) {
        lenient().when(mockSdxStatusEntity.getStatus()).thenReturn(DatalakeStatusEnum.RUNNING);
        lenient().when(mockSdxStatusEntity.getStatusReason()).thenReturn("someReason");
        lenient().when(mockSdxStatusEntity.getCreated()).thenReturn(4321L);
        lenient().when(mockSdxStatusEntity.getId()).thenReturn(123L);
        lenient().when(mockSdxStatusEntity.getDatalake()).thenReturn(cluster);
    }

}