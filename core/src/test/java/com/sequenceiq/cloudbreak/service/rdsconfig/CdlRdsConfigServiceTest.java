package com.sequenceiq.cloudbreak.service.rdsconfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import java.util.Collections;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.cdp.servicediscovery.ServiceDiscoveryProto;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.dto.StackDtoDelegate;
import com.sequenceiq.cloudbreak.grpc.ManagedChannelWrapper;
import com.sequenceiq.cloudbreak.sdx.cdl.CdlSdxService;
import com.sequenceiq.cloudbreak.sdx.cdl.GrpcServiceDiscoveryClient;
import com.sequenceiq.cloudbreak.sdx.cdl.ServiceDiscoveryClient;
import com.sequenceiq.cloudbreak.sdx.cdl.config.ServiceDiscoveryChannelConfig;
import com.sequenceiq.cloudbreak.sdx.common.PlatformAwareSdxConnector;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

public class CdlRdsConfigServiceTest {

    private static final Long CLUSTER_ID = 1L;

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "test-stack";

    private static final String HIVE = "HIVE";

    private static final Crn ENVIRONMENT_CRN =
            Crn.safeFromString("crn:cdp:environments:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:environment:2903f097-dae6-4129-b4ba-b283dfd63138");

    private static final Crn CDL_CRN =
            Crn.safeFromString("crn:cdp:sdxsvc:us-west-1:9d74eee4-1cad-45d7-b645-7ccf9edbb73d:instance:ab79a335-70cc-4c06-90af-ea74efe02636");

    @InjectMocks
    private HiveCdlRdsConfigProvider underTest;

    @Mock
    private RdsConfigService rdsConfigService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private PlatformAwareSdxConnector platformAwareSdxConnector;

    @Mock
    private RdsConfigWithoutClusterService rdsConfigWithoutClusterService;

    @Mock
    private GrpcServiceDiscoveryClient grpcServiceDiscoveryClient;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private ManagedChannelWrapper managedChannelWrapper;

    @Mock
    private ServiceDiscoveryChannelConfig serviceDiscoveryChannelConfig;

    @Mock
    private ServiceDiscoveryClient serviceDiscoveryClient;

    private CdlSdxService cdlSdxService;

    @Before
    public void setUp() {
        openMocks(this);
        this.cdlSdxService = new CdlSdxService();
        when(entitlementService.isEntitledFor(anyString(), any())).thenReturn(true);
        ReflectionTestUtils.setField(grpcServiceDiscoveryClient, "channelWrapper", managedChannelWrapper);
        ReflectionTestUtils.setField(grpcServiceDiscoveryClient, "serviceDiscoveryChannelConfig", serviceDiscoveryChannelConfig);
        ReflectionTestUtils.setField(cdlSdxService, "grpcServiceDiscoveryClient", grpcServiceDiscoveryClient);
        ReflectionTestUtils.setField(cdlSdxService, "cdlEnabled", true);
        ReflectionTestUtils.setField(cdlSdxService, "entitlementService", entitlementService);
        ReflectionTestUtils.setField(underTest, "cdlSdxService", cdlSdxService);
    }

    @Test
    public void testRetrieveRdsConfigs() {
        when(rdsConfigService.findByClusterId(eq(CLUSTER_ID))).thenReturn(Collections.emptySet());
        when(platformAwareSdxConnector.getSdxCrnByEnvironmentCrn(eq(ENVIRONMENT_CRN.toString()))).thenReturn(Optional.of(CDL_CRN.toString()));
        when(grpcServiceDiscoveryClient.makeClient()).thenReturn(serviceDiscoveryClient);
        when(grpcServiceDiscoveryClient.getServiceConfiguration(eq(CDL_CRN.toString()), eq(HIVE))).thenCallRealMethod();
        when(serviceDiscoveryClient.getRemoteDataContext(eq(CDL_CRN.toString()))).thenReturn(getCastedRdc());
        ArgumentCaptor<RDSConfig> rdsConfigCaptor = ArgumentCaptor.forClass(RDSConfig.class);

        underTest.createPostgresRdsConfigIfNeeded(getStack());

        verify(serviceDiscoveryClient, times(1)).getRemoteDataContext(eq(CDL_CRN.toString()));
        verify(platformAwareSdxConnector, times(2)).getSdxCrnByEnvironmentCrn(eq(ENVIRONMENT_CRN.toString()));
        verify(clusterService, times(1)).saveRdsConfig(rdsConfigCaptor.capture());
        verify(clusterService, times(1)).addRdsConfigToCluster(any(), eq(CLUSTER_ID));

        RDSConfig result = rdsConfigCaptor.getValue();

        assertEquals("jdbc:postgresql://test-internal.test.xcu2-8y8x.dev.cldr.work:5432/hms", result.getConnectionURL());
        assertEquals("as5NNb3La50gla8CxK4UPbxciFpYLW7XklOxBA5b", result.getConnectionUserName());
        assertEquals("EiULUn4KLVSrp3BtJ0Bo1r21Q2ec1fmiDtVjkr2E", result.getConnectionPassword());
        assertEquals(STACK_NAME + "_" + STACK_ID + "_" + "hms", result.getName());
        assertTrue(result.getClusters().stream().map(Cluster::getId).toList().contains(CLUSTER_ID));
    }

    @Test
    public void testCreationWhenRdcNotFound() {
        when(rdsConfigService.findByClusterId(eq(CLUSTER_ID))).thenReturn(Collections.emptySet());
        when(platformAwareSdxConnector.getSdxCrnByEnvironmentCrn(eq(ENVIRONMENT_CRN.toString()))).thenReturn(Optional.of(CDL_CRN.toString()));
        assertThrows(IllegalArgumentException.class,  () -> underTest.createPostgresRdsConfigIfNeeded(getStack()));

        verify(platformAwareSdxConnector, times(0)).getRemoteDataContext(eq(CDL_CRN.toString()));
        verify(platformAwareSdxConnector, times(2)).getSdxCrnByEnvironmentCrn(eq(ENVIRONMENT_CRN.toString()));
        verify(clusterService, times(0)).saveRdsConfig(any(RDSConfig.class));
        verify(clusterService, times(0)).addRdsConfigToCluster(any(), eq(CLUSTER_ID));
    }

    private StackDtoDelegate getStack() {
        StackDto stackDto = mock(StackDto.class);
        when(stackDto.getEnvironmentCrn()).thenReturn(ENVIRONMENT_CRN.toString());
        when(stackDto.getId()).thenReturn(STACK_ID);
        when(stackDto.getName()).thenReturn(STACK_NAME);
        Cluster cluster = getCluster();
        when(stackDto.getCluster()).thenReturn(cluster);
        return stackDto;
    }

    private Cluster getCluster() {
        Cluster cluster = mock(Cluster.class);
        when(cluster.getId()).thenReturn(CLUSTER_ID);
        return cluster;
    }

    private ServiceDiscoveryProto.ApiRemoteDataContext getCastedRdc() {
        ServiceDiscoveryProto.ApiMapEntry port = ServiceDiscoveryProto.ApiMapEntry.newBuilder()
                .setKey("hive_metastore_database_port")
                .setValue("5432")
                .build();
        ServiceDiscoveryProto.ApiMapEntry dbName = ServiceDiscoveryProto.ApiMapEntry.newBuilder()
                .setKey("hive_metastore_database_name")
                .setValue("hms")
                .build();
        ServiceDiscoveryProto.ApiMapEntry host = ServiceDiscoveryProto.ApiMapEntry.newBuilder()
                .setKey("hive_metastore_database_host")
                .setValue("test-internal.test.xcu2-8y8x.dev.cldr.work")
                .build();
        ServiceDiscoveryProto.ApiMapEntry pw = ServiceDiscoveryProto.ApiMapEntry.newBuilder()
                .setKey("hive_metastore_database_password")
                .setValue("EiULUn4KLVSrp3BtJ0Bo1r21Q2ec1fmiDtVjkr2E")
                .build();
        ServiceDiscoveryProto.ApiMapEntry user = ServiceDiscoveryProto.ApiMapEntry.newBuilder()
                .setKey("hive_metastore_database_user")
                .setValue("as5NNb3La50gla8CxK4UPbxciFpYLW7XklOxBA5b")
                .build();
        ServiceDiscoveryProto.ApiEndPoint apiEndPoint = ServiceDiscoveryProto.ApiEndPoint.newBuilder()
                .setName(HIVE)
                .addServiceConfigs(port)
                .addServiceConfigs(dbName)
                .addServiceConfigs(host)
                .addServiceConfigs(pw)
                .addServiceConfigs(user)
                .build();
        return ServiceDiscoveryProto.ApiRemoteDataContext.newBuilder()
                .setEndPointId("endpointId")
                .addEndPoints(apiEndPoint)
                .build();
    }
}
