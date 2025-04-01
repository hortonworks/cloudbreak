package com.sequenceiq.cloudbreak.sdx.cdl;

import static com.sequenceiq.common.model.CloudStorageCdpService.DEFAULT_FS;
import static com.sequenceiq.common.model.CloudStorageCdpService.HIVE_METASTORE_EXTERNAL_WAREHOUSE;
import static com.sequenceiq.common.model.CloudStorageCdpService.HIVE_METASTORE_WAREHOUSE;
import static com.sequenceiq.common.model.CloudStorageCdpService.HIVE_REPLICA_WAREHOUSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.ReflectionUtils;

import com.cloudera.thunderhead.service.cdlcrud.CdlCrudProto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.protobuf.InvalidProtocolBufferException;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.sdx.cdl.grpc.GrpcSdxCdlClient;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxDescribeService;
import com.sequenceiq.cloudbreak.sdx.cdl.service.CdlSdxStatusService;
import com.sequenceiq.cloudbreak.sdx.common.grpc.GrpcServiceDiscoveryClient;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxAccessView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxBasicView;
import com.sequenceiq.cloudbreak.sdx.common.model.SdxFileSystemView;
import com.sequenceiq.common.model.FileSystemType;

@ExtendWith(MockitoExtension.class)
public class CdlSdxDescribeServiceTest {

    private static final String CDL_CRN = "crn:cdp:sdxsvc:us-west-1:tenant:instance:crn2";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:crn1";

    private static final String CDL_NAME = "test-cdl";

    private static final String CDL_RUNTIME = "7.2.17";

    private static final boolean CDL_RAZ_ENABLED = true;

    private static final long CDL_CREATED_TIME = 1L;

    private static final String CDL_DB_CRN = "crn:cdp:redbeams:us-west-1:tenant:instance:crn2";

    private static final String CDL_CLOUD_STORAGE = "s3a://bucket";

    private static final String HIVE_WAREHOUSE_SERVICE_CONFIG = "hive_warehouse_directory";

    private static final String HIVE_WAREHOUSE_DIR = "s3a://bucket/hive_warehouse";

    private static final String HIVE_EXTERNAL_WAREHOUSE_SERVICE_CONFIG = "hive_warehouse_external_directory";

    private static final String HIVE_EXTERNAL_WAREHOUSE_DIR = "s3a://bucket/hive_warehouse_external";

    private static final String HIVE_REPLICA_WAREHOUSE_SERVICE_CONFIG = "hive_repl_replica_functions_root_dir";

    private static final String HIVE_REPLICA_WAREHOUSE_DIR = "s3a://bucket/hive_repl_replica";

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private GrpcSdxCdlClient sdxClient;

    @Mock
    private GrpcServiceDiscoveryClient grpcServiceDiscoveryClient;

    @InjectMocks
    private CdlSdxDescribeService underTest;

    @Test
    public void testListCrn() {
        setEnabled();
        when(sdxClient.listDatalakes(anyString(), anyString())).thenReturn(getDatalakeList());
        Set<String> sdxCrns = underTest.listSdxCrns(ENV_CRN);
        assertTrue(sdxCrns.contains(CDL_CRN));
        assertEquals(2, sdxCrns.size());
        verify(sdxClient).listDatalakes(eq(ENV_CRN), any());

        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.FALSE);
        assertTrue(underTest.listSdxCrns(ENV_CRN).isEmpty());
        verifyNoMoreInteractions(sdxClient);
    }

    @Test
    public void testGetSdxAccessView() {
        setEnabled();
        when(sdxClient.findDatalake(anyString(), anyString())).thenReturn(CdlCrudProto.DatalakeResponse.newBuilder().setCrn(CDL_CRN).build());
        CdlCrudProto.EndpointHost endpointHost = CdlCrudProto.EndpointHost.newBuilder().setUri("https://rangerhost:1234").build();
        CdlCrudProto.EndpointInfo endpointInfo = CdlCrudProto.EndpointInfo.newBuilder().setName("RANGER_ADMIN").addEndpointHosts(endpointHost).build();
        when(sdxClient.describeDatalakeServices(anyString())).thenReturn(CdlCrudProto.DescribeServicesResponse.newBuilder().addEndpoints(endpointInfo).build());

        Optional<SdxAccessView> sdxAccessView = underTest.getSdxAccessViewByEnvironmentCrn(ENV_CRN);
        assertNull(sdxAccessView.get().clusterManagerFqdn());
        assertNull(sdxAccessView.get().clusterManagerIp());
        assertEquals("rangerhost", sdxAccessView.get().rangerFqdn());
    }

    @Test
    void testGetSdxByEnvironmentCRN() {
        setEnabled();
        when(sdxClient.findDatalake(anyString(), anyString())).thenReturn(CdlCrudProto.DatalakeResponse.newBuilder().setCrn(CDL_CRN).build());
        when(sdxClient.describeDatalake(anyString())).thenReturn(CdlCrudProto.DescribeDatalakeResponse.newBuilder()
                .setName(CDL_NAME)
                .setCrn(CDL_CRN)
                .setRuntimeVersion(CDL_RUNTIME)
                .setRangerRazEnabled(CDL_RAZ_ENABLED)
                .setCreated(CDL_CREATED_TIME)
                .setDatabaseDetails(CdlCrudProto.DatabaseInfo.newBuilder()
                        .setCrn(CDL_DB_CRN)
                        .build())
                .setCloudStorageBaseLocation(CDL_CLOUD_STORAGE)
                .build());

        CdlCrudProto.EndpointInfo hiveEndpointInfo = CdlCrudProto.EndpointInfo.newBuilder()
                .setName("hive")
                .addServiceConfigs(CdlCrudProto.Config.newBuilder()
                        .setKey(HIVE_WAREHOUSE_SERVICE_CONFIG)
                        .setValue(HIVE_WAREHOUSE_DIR)
                        .build())
                .addServiceConfigs(CdlCrudProto.Config.newBuilder()
                        .setKey(HIVE_EXTERNAL_WAREHOUSE_SERVICE_CONFIG)
                        .setValue(HIVE_EXTERNAL_WAREHOUSE_DIR)
                        .build())
                .addServiceConfigs(CdlCrudProto.Config.newBuilder()
                        .setKey(HIVE_REPLICA_WAREHOUSE_SERVICE_CONFIG)
                        .setValue(HIVE_REPLICA_WAREHOUSE_DIR)
                        .build())
                .build();
        when(sdxClient.describeDatalakeServices(anyString())).thenReturn(CdlCrudProto.DescribeServicesResponse.newBuilder()
                .addEndpoints(hiveEndpointInfo)
                .build());

        Optional<SdxBasicView> response = underTest.getSdxByEnvironmentCrn(ENV_CRN);
        assertTrue(response.isPresent());

        SdxBasicView sdxBasicView = response.get();
        assertEquals(CDL_NAME, sdxBasicView.name());
        assertEquals(CDL_CRN, sdxBasicView.crn());
        assertEquals(CDL_RUNTIME, sdxBasicView.runtime());
        assertEquals(CDL_RAZ_ENABLED, sdxBasicView.razEnabled());
        assertEquals(CDL_CREATED_TIME, sdxBasicView.created());
        assertEquals(CDL_DB_CRN, sdxBasicView.dbServerCrn());
        assertNotNull(sdxBasicView.fileSystemView());

        SdxFileSystemView fileSystemView = sdxBasicView.fileSystemView().orElse(new SdxFileSystemView(null, Map.of()));
        assertEquals(FileSystemType.S3.name(), fileSystemView.fileSystemType());
        assertEquals(CDL_CLOUD_STORAGE, fileSystemView.sharedFileSystemLocationsByService().get(DEFAULT_FS.name()));
        assertEquals(HIVE_WAREHOUSE_DIR, fileSystemView.sharedFileSystemLocationsByService().get(HIVE_METASTORE_WAREHOUSE.name()));
        assertEquals(HIVE_EXTERNAL_WAREHOUSE_DIR, fileSystemView.sharedFileSystemLocationsByService().get(HIVE_METASTORE_EXTERNAL_WAREHOUSE.name()));
        assertEquals(HIVE_REPLICA_WAREHOUSE_DIR, fileSystemView.sharedFileSystemLocationsByService().get(HIVE_REPLICA_WAREHOUSE.name()));
    }

    @Test
    void getRemoteDataContextThrowsError() throws InvalidProtocolBufferException, JsonProcessingException {
        setEnabled();
        when(grpcServiceDiscoveryClient.getRemoteDataContext(CDL_CRN)).thenThrow(new RuntimeException());

        RuntimeException e = assertThrows(RuntimeException.class, () -> underTest.getRemoteDataContext(CDL_CRN));
        assertEquals("Not able to fetch the RDC for CDL from Service Discovery", e.getMessage());
    }

    private CdlCrudProto.ListDatalakesResponse getDatalakeList() {
        CdlCrudProto.ListDatalakesResponse.Builder listDatalakesResponseBuilder = CdlCrudProto.ListDatalakesResponse.newBuilder();
        CdlCrudProto.DatalakeResponse datalakeResponse1 = CdlCrudProto.DatalakeResponse.newBuilder()
                .setCrn(CDL_CRN)
                .setName("dl-name1")
                .build();
        CdlCrudProto.DatalakeResponse datalakeResponse2 = CdlCrudProto.DatalakeResponse.newBuilder()
                .setCrn("other_CRN")
                .setName("dl-name2")
                .build();
        listDatalakesResponseBuilder.addDatalakeResponse(datalakeResponse1);
        listDatalakesResponseBuilder.addDatalakeResponse(datalakeResponse2);

        return listDatalakesResponseBuilder.build();
    }

    private void setEnabled() {
        Field cdlEnabled = ReflectionUtils.findField(CdlSdxStatusService.class, "cdlEnabled");
        ReflectionUtils.makeAccessible(cdlEnabled);
        ReflectionUtils.setField(cdlEnabled, underTest, true);
        when(entitlementService.isEntitledFor(any(), any())).thenReturn(Boolean.TRUE);
    }
}
