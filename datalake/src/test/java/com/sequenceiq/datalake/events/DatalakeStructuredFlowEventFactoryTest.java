package com.sequenceiq.datalake.events;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.cloudbreak.structuredevent.event.FlowDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredEventType;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.CDPStructuredFlowEvent;
import com.sequenceiq.common.api.type.CertExpirationState;
import com.sequenceiq.common.model.FileSystemType;
import com.sequenceiq.datalake.entity.DatalakeStatusEnum;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.entity.SdxStatusEntity;
import com.sequenceiq.datalake.repository.SdxStatusRepository;
import com.sequenceiq.datalake.service.sdx.SdxService;
import com.sequenceiq.flow.ha.NodeConfig;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
class DatalakeStructuredFlowEventFactoryTest {

    private static final long SDX_ID = 1L;

    private static final long TIMESTAMP = 1L;

    @Mock
    private SdxService mockSdxService;

    @Mock
    private SdxClusterDtoConverter mockSdxClusterDtoConverter;

    @Mock
    private NodeConfig mockNodeConfig;

    @Mock
    private Clock mockClock;

    @Mock
    private SdxStatusRepository mockSdxStatusRepository;

    @Mock
    private SdxClusterDto mockSdxClusterDto;

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
        lenient().when(mockSdxClusterDtoConverter.sdxClusterToDto(createSdxCluster())).thenReturn(mockSdxClusterDto);
        lenient().when(mockSdxStatusRepository.findFirstByDatalakeIsOrderByIdDesc(createSdxCluster())).thenReturn(mockSdxStatusEntity);
    }

    @Test
    void testMakeCdpOperationDetailsWhenExceptionIsNull() {
        CDPStructuredFlowEvent<SdxClusterDto> result = underTest.createStructuredFlowEvent(SDX_ID, mockFlowDetails, null);

        assertEquals(result.getOperation().getResourceId(), SDX_ID);
        assertEquals(result.getOperation().getResourceCrn(), cluster.getResourceCrn());
        assertEquals(result.getOperation().getResourceName(), cluster.getClusterName());
        assertEquals(result.getOperation().getAccountId(), cluster.getAccountId());
        assertEquals(result.getOperation().getEnvironmentCrn(), cluster.getEnvCrn());
        assertEquals(result.getOperation().getTimestamp(), TIMESTAMP);
        assertTrue(StringUtils.isNotEmpty(result.getOperation().getUuid()));
        assertEquals(result.getOperation().getResourceType(), "datalake");
        assertEquals(result.getOperation().getEventType(), StructuredEventType.FLOW);
        assertEquals(result.getOperation().getCloudbreakId(), "nodeId");
        assertEquals(result.getStatus(), mockSdxStatusEntity.getStatus().name());
        assertEquals(result.getStatusReason(), mockSdxStatusEntity.getStatusReason());
        assertNull(result.getException());
    }

    @Test
    void testMakeCdpOperationDetailsWhenExceptionIsNotNull() {
        Exception exception = new Exception("someMessage");
        CDPStructuredFlowEvent<SdxClusterDto> result = underTest.createStructuredFlowEvent(SDX_ID, null, exception);

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
        sdxCluster.setSdxDatabase(new SdxDatabase());


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