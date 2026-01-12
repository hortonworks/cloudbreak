package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.api.swagger.CdpResourceApi;
import com.cloudera.api.swagger.ClouderaManagerResourceApi;
import com.cloudera.api.swagger.ClustersResourceApi;
import com.cloudera.api.swagger.ParcelsResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.api.swagger.model.ApiCluster;
import com.cloudera.api.swagger.model.ApiCmServer;
import com.cloudera.api.swagger.model.ApiCmServerList;
import com.cloudera.api.swagger.model.ApiEndPoint;
import com.cloudera.api.swagger.model.ApiEndPointHost;
import com.cloudera.api.swagger.model.ApiEntityStatus;
import com.cloudera.api.swagger.model.ApiHost;
import com.cloudera.api.swagger.model.ApiHostList;
import com.cloudera.api.swagger.model.ApiKerberosInfo;
import com.cloudera.api.swagger.model.ApiMapEntry;
import com.cloudera.api.swagger.model.ApiParcel;
import com.cloudera.api.swagger.model.ApiParcelList;
import com.cloudera.api.swagger.model.ApiRemoteDataContext;
import com.cloudera.api.swagger.model.ApiVersionInfo;
import com.cloudera.thunderhead.service.environments2api.model.Application;
import com.cloudera.thunderhead.service.environments2api.model.Environment;
import com.cloudera.thunderhead.service.environments2api.model.Instance;
import com.cloudera.thunderhead.service.environments2api.model.KerberosInfo;
import com.cloudera.thunderhead.service.environments2api.model.PrivateDatalakeDetails;
import com.cloudera.thunderhead.service.environments2api.model.PvcEnvironmentDetails;
import com.cloudera.thunderhead.service.environments2api.model.Service;
import com.cloudera.thunderhead.service.environments2api.model.ServiceEndPoint;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.cloudbreak.util.test.AsyncTaskExecutorTestImpl;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentPropertiesV2Response;
import com.sequenceiq.remoteenvironment.DescribeEnvironmentV2Response;
import com.sequenceiq.remoteenvironment.exception.OnPremCMApiException;

import okhttp3.OkHttpClient;

@ExtendWith(MockitoExtension.class)
class ClassicClusterDescribeServiceTest {
    @Mock
    private ClassicClusterToEnvironmentConverter classicClusterToEnvironmentConverter;

    @Mock
    private ClassicClusterClouderaManagerApiClientProvider apiClientProvider;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @Mock
    private ApiClient apiClient;

    @Mock
    private ApiClient apiV51Client;

    @Mock
    private ClustersResourceApi clustersResourceApi;

    @Mock
    private ClouderaManagerResourceApi cmResourceApi;

    @Mock
    private ParcelsResourceApi parcelsResourceApi;

    @Mock
    private CdpResourceApi cdpResourceApi;

    @Spy
    private AsyncTaskExecutorTestImpl taskExecutor;

    @InjectMocks
    private ClassicClusterDescribeService underTest;

    @BeforeEach
    void setup() {
        lenient().when(apiClientProvider.getClouderaManagerRootClient(any())).thenReturn(apiClient);
        lenient().when(apiClientProvider.getClouderaManagerV51Client(any())).thenReturn(apiV51Client);
        lenient().when(clouderaManagerApiFactory.getClustersResourceApi(apiV51Client)).thenReturn(clustersResourceApi);
        lenient().when(clouderaManagerApiFactory.getClouderaManagerResourceApi(apiV51Client)).thenReturn(cmResourceApi);
        lenient().when(clouderaManagerApiFactory.getParcelsResourceApi(apiV51Client)).thenReturn(parcelsResourceApi);
        lenient().when(clouderaManagerApiFactory.getCdpResourceApi(apiClient)).thenReturn(cdpResourceApi);
        lenient().when(apiV51Client.getHttpClient()).thenReturn(new OkHttpClient());
        ReflectionTestUtils.setField(underTest, "taskExecutor", taskExecutor);
    }

    @Test
    void testDescribe() throws ApiException {
        OnPremisesApiProto.Cluster cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setName("cluster")
                .setClusterCrn("clusterCrn")
                .setManagerUri("managerUri")
                .setKnoxEnabled(true)
                .setKnoxUrl("knoxUrl")
                .build();
        List<ApiParcel> parcelList = List.of(new ApiParcel()
                .product("CDH")
                .stage("ACTIVATED")
                .version("cdpVersion"));
        when(parcelsResourceApi.readParcels(cluster.getName(), "SUMMARY")).thenReturn(new ApiParcelList().items(parcelList));
        ApiVersionInfo apiVersionInfo = new ApiVersionInfo().version("cmVersion");
        when(cmResourceApi.getVersion()).thenReturn(apiVersionInfo);
        List<ApiHost> apiHosts = List.of(new ApiHost()
                .hostname("hostName")
                .ipAddress("ipAddress")
                .hostId("hostId"));
        when(clustersResourceApi.listHosts(cluster.getName(), null, null, "SUMMARY")).thenReturn(new ApiHostList().items(apiHosts));
        ApiCluster apiCluster = new ApiCluster().entityStatus(ApiEntityStatus.GOOD_HEALTH);
        when(clustersResourceApi.readCluster(cluster.getName())).thenReturn(apiCluster);
        ApiRemoteDataContext apiRemoteDataContext = createRemoteDataContext();
        when(cdpResourceApi.getRemoteContextByCluster(cluster.getName())).thenReturn(apiRemoteDataContext);
        List<ApiCmServer> apiCmServers = List.of(new ApiCmServer()
                .ipAddress("cmIp")
                .name("cmName")
                .cmServerId("cmServerId"));
        when(cmResourceApi.readInstances()).thenReturn(new ApiCmServerList().items(apiCmServers));
        ApiKerberosInfo apiKerberosInfo = new ApiKerberosInfo()
                .kerberized(true)
                .kdcType("kdcType")
                .kerberosRealm("realm")
                .kdcHost("kdcHost");
        when(clustersResourceApi.getKerberosInfo(cluster.getName())).thenReturn(apiKerberosInfo);

        DescribeEnvironmentV2Response actualResponse = underTest.describe(cluster);

        DescribeEnvironmentPropertiesV2Response additionalProperties = actualResponse.getAdditionalProperties();
        assertEquals(cluster.getKnoxUrl(), additionalProperties.getRemotenvironmentUrl());

        Environment environment = actualResponse.getEnvironment();
        assertEquals(cluster.getName(), environment.getEnvironmentName());
        assertEquals(cluster.getClusterCrn(), environment.getCrn());
        assertEquals(parcelList.getFirst().getVersion(), environment.getCdpRuntimeVersion());
        assertEquals(apiVersionInfo.getVersion(), environment.getClouderaManagerVersion());

        PvcEnvironmentDetails environmentDetails = environment.getPvcEnvironmentDetails();
        assertEquals(cluster.getManagerUri(), environmentDetails.getCmHost());

        Application application = environmentDetails.getApplications().get("serviceType");
        Service service = application.getServices().get("serviceType");
        ServiceEndPoint serviceEndPoint = service.getEndpoints().getFirst();
        assertEquals("http://host:1234", serviceEndPoint.getUri());
        assertEquals("host", serviceEndPoint.getHost());
        assertEquals(1234, serviceEndPoint.getPort());
        assertEquals("configValue", application.getConfig().get("configKey"));

        PrivateDatalakeDetails datalakeDetails = environmentDetails.getPrivateDatalakeDetails();
        assertEquals(apiCmServers.getFirst().getName(), datalakeDetails.getCmFQDN());
        assertEquals(apiCmServers.getFirst().getIpAddress(), datalakeDetails.getCmIP());
        assertEquals(apiCmServers.getFirst().getCmServerId(), datalakeDetails.getCmServerId());
        assertEquals(PrivateDatalakeDetails.StatusEnum.AVAILABLE, datalakeDetails.getStatus());

        KerberosInfo kerberosInfo = datalakeDetails.getKerberosInfo();
        assertEquals(apiKerberosInfo.getKdcHost(), kerberosInfo.getKdcHost());
        assertEquals(apiKerberosInfo.getKerberosRealm(), kerberosInfo.getKerberosRealm());
        assertEquals(apiKerberosInfo.getKdcType(), kerberosInfo.getKdcType());
        assertNull(kerberosInfo.getKdcHostIp());

        Instance instance = datalakeDetails.getInstances().getFirst();
        assertEquals(apiHosts.getFirst().getHostname(), instance.getDiscoveryFQDN());
        assertEquals(apiHosts.getFirst().getHostId(), instance.getInstanceId());
        assertEquals(apiHosts.getFirst().getIpAddress(), instance.getPrivateIp());

        verify(taskExecutor, times(6)).submit(any(Callable.class));
    }

    @Test
    void testDescribeNoKerberos() throws ApiException {
        OnPremisesApiProto.Cluster cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setName("cluster")
                .setClusterCrn("clusterCrn")
                .setManagerUri("managerUri")
                .setKnoxEnabled(true)
                .setKnoxUrl("knoxUrl")
                .build();
        List<ApiParcel> parcelList = List.of(new ApiParcel()
                .product("CDH")
                .stage("ACTIVATED")
                .version("cdpVersion"));
        when(parcelsResourceApi.readParcels(cluster.getName(), "SUMMARY")).thenReturn(new ApiParcelList().items(parcelList));
        ApiVersionInfo apiVersionInfo = new ApiVersionInfo().version("cmVersion");
        when(cmResourceApi.getVersion()).thenReturn(apiVersionInfo);
        List<ApiHost> apiHosts = List.of(new ApiHost()
                .hostname("hostName")
                .ipAddress("ipAddress")
                .hostId("hostId"));
        when(clustersResourceApi.listHosts(cluster.getName(), null, null, "SUMMARY")).thenReturn(new ApiHostList().items(apiHosts));
        ApiCluster apiCluster = new ApiCluster().entityStatus(ApiEntityStatus.GOOD_HEALTH);
        when(clustersResourceApi.readCluster(cluster.getName())).thenReturn(apiCluster);
        ApiRemoteDataContext apiRemoteDataContext = createRemoteDataContext();
        when(cdpResourceApi.getRemoteContextByCluster(cluster.getName())).thenReturn(apiRemoteDataContext);
        List<ApiCmServer> apiCmServers = List.of(new ApiCmServer()
                .ipAddress("cmIp")
                .name("cmName")
                .cmServerId("cmServerId"));
        when(cmResourceApi.readInstances()).thenReturn(new ApiCmServerList().items(apiCmServers));
        ApiKerberosInfo apiKerberosInfo = new ApiKerberosInfo().kerberized(false);
        when(clustersResourceApi.getKerberosInfo(cluster.getName())).thenReturn(apiKerberosInfo);

        DescribeEnvironmentV2Response actualResponse = underTest.describe(cluster);

        KerberosInfo kerberosInfo = actualResponse.getEnvironment().getPvcEnvironmentDetails().getPrivateDatalakeDetails().getKerberosInfo();
        assertFalse(kerberosInfo.getKerberized());
    }

    @Test
    void testDescribeWithKnoxGateway() throws ApiException {
        OnPremisesApiProto.Cluster cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setName("cluster")
                .setClusterCrn("clusterCrn")
                .setManagerUri("managerUri")
                .setKnoxEnabled(true)
                .setKnoxUrl("knoxUrl")
                .build();
        List<ApiParcel> parcelList = List.of(new ApiParcel()
                .product("CDH")
                .stage("ACTIVATED")
                .version("cdpVersion"));
        when(parcelsResourceApi.readParcels(cluster.getName(), "SUMMARY")).thenReturn(new ApiParcelList().items(parcelList));
        ApiVersionInfo apiVersionInfo = new ApiVersionInfo().version("cmVersion");
        when(cmResourceApi.getVersion()).thenReturn(apiVersionInfo);
        List<ApiHost> apiHosts = List.of(new ApiHost()
                .hostname("hostName")
                .ipAddress("ipAddress")
                .hostId("hostId"));
        when(clustersResourceApi.listHosts(cluster.getName(), null, null, "SUMMARY")).thenReturn(new ApiHostList().items(apiHosts));
        ApiCluster apiCluster = new ApiCluster().entityStatus(ApiEntityStatus.GOOD_HEALTH);
        when(clustersResourceApi.readCluster(cluster.getName())).thenReturn(apiCluster);
        ApiRemoteDataContext apiRemoteDataContext = createRemoteDataContextWithKnox();
        when(cdpResourceApi.getRemoteContextByCluster(cluster.getName())).thenReturn(apiRemoteDataContext);
        List<ApiCmServer> apiCmServers = List.of(new ApiCmServer()
                .ipAddress("cmIp")
                .name("cmName")
                .cmServerId("cmServerId"));
        when(cmResourceApi.readInstances()).thenReturn(new ApiCmServerList().items(apiCmServers));
        ApiKerberosInfo apiKerberosInfo = new ApiKerberosInfo().kerberized(false);
        when(clustersResourceApi.getKerberosInfo(cluster.getName())).thenReturn(apiKerberosInfo);

        DescribeEnvironmentV2Response actualResponse = underTest.describe(cluster);

        assertEquals("knoxUri", actualResponse.getEnvironment().getPvcEnvironmentDetails().getKnoxGatewayUrl());
    }

    @Test
    void testDescribeWhenApiException() throws ApiException {
        OnPremisesApiProto.Cluster cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setName("cluster")
                .setClusterCrn("clusterCrn")
                .setManagerUri("managerUri")
                .setKnoxEnabled(true)
                .setKnoxUrl("knoxUrl")
                .build();
        when(parcelsResourceApi.readParcels(cluster.getName(), "SUMMARY")).thenThrow(new ApiException());

        assertThrows(OnPremCMApiException.class, () -> underTest.describe(cluster));
    }

    @Test
    void testDescribeWithDetails() throws ApiException {
        OnPremisesApiProto.Cluster cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setName("cluster")
                .setClusterCrn("clusterCrn")
                .setManagerUri("managerUri")
                .setKnoxEnabled(true)
                .setKnoxUrl("knoxUrl")
                .setOnPremEnvironmentDetails(OnPremisesApiProto.OnPremEnvironmentDetails.newBuilder().build())
                .build();
        Environment environment = new Environment();
        when(classicClusterToEnvironmentConverter.createEnvironment(cluster)).thenReturn(environment);

        DescribeEnvironmentV2Response describeEnvironmentV2Response = underTest.describe(cluster);

        assertEquals(environment, describeEnvironmentV2Response.getEnvironment());
    }

    private ApiRemoteDataContext createRemoteDataContext() {
        List<ApiEndPointHost> apiEndPointHosts = List.of(new ApiEndPointHost().type("serviceType").uri("http://host:1234"));
        List<ApiMapEntry> apiServiceConfigs = List.of(new ApiMapEntry().key("configKey").value("configValue"),
                new ApiMapEntry().key("configKey").value("configValue"));
        List<ApiEndPoint> apiEndPoints = List.of(
                new ApiEndPoint().serviceType("serviceType").endPointHostList(apiEndPointHosts).serviceConfigs(apiServiceConfigs));
        return new ApiRemoteDataContext().endPoints(apiEndPoints);
    }

    private ApiRemoteDataContext createRemoteDataContextWithKnox() {
        List<ApiEndPointHost> apiEndPointHosts = List.of(new ApiEndPointHost().type("KNOX_GATEWAY").uri("knoxUri"));
        List<ApiMapEntry> apiServiceConfigs = List.of(new ApiMapEntry().key("configKey").value("configValue"));
        List<ApiEndPoint> apiEndPoints = List.of(
                new ApiEndPoint().serviceType("KNOX").endPointHostList(apiEndPointHosts).serviceConfigs(apiServiceConfigs));
        return new ApiRemoteDataContext().endPoints(apiEndPoints);
    }
}
