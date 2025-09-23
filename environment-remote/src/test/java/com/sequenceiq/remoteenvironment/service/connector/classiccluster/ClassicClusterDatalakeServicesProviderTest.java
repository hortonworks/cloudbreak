package com.sequenceiq.remoteenvironment.service.connector.classiccluster;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;

import com.cloudera.api.swagger.ServicesResourceApi;
import com.cloudera.api.swagger.client.ApiClient;
import com.cloudera.api.swagger.client.ApiException;
import com.cloudera.cdp.servicediscovery.model.DeploymentType;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.cloudera.thunderhead.service.onpremises.OnPremisesApiProto;
import com.sequenceiq.cloudbreak.cm.client.retry.ClouderaManagerApiFactory;
import com.sequenceiq.remoteenvironment.RemoteEnvironmentException;

@ExtendWith(MockitoExtension.class)
class ClassicClusterDatalakeServicesProviderTest {

    private static final String CLUSTER_NAME = "clusterName";

    private static final String CLUSTER_CRN = "clusterCrn";

    @Mock
    private ClassicClusterClouderaManagerApiClientProvider apiClientProvider;

    @Mock
    private ClouderaManagerApiFactory clouderaManagerApiFactory;

    @InjectMocks
    private ClassicClusterDatalakeServicesProvider underTest;

    private OnPremisesApiProto.Cluster cluster;

    @Mock
    private ApiClient apiClient;

    @Mock
    private ServicesResourceApi servicesResourceApi;

    @BeforeEach
    void setUp() {
        cluster = OnPremisesApiProto.Cluster.newBuilder()
                .setName(CLUSTER_NAME)
                .setClusterCrn(CLUSTER_CRN)
                .build();
        when(apiClientProvider.getClouderaManagerV51Client(cluster)).thenReturn(apiClient);
        when(clouderaManagerApiFactory.getServicesResourceApi(apiClient)).thenReturn(servicesResourceApi);
    }

    @Test
    void testGetDatalakeServicesResponseHdfsNonHa() throws Exception {
        when(servicesResourceApi.getClientConfig(CLUSTER_NAME, "hdfs"))
                .thenReturn(new ClassPathResource("cmclientconfig/hdfs-nonha.zip").getFile());

        DescribeDatalakeServicesResponse result = underTest.getDatalakeServices(cluster);

        assertThat(result.getClusterid()).isEqualTo(CLUSTER_CRN);
        assertThat(result.getDeploymentType()).isEqualTo(DeploymentType.PDL);
        assertThat(result.getApplications()).containsKey("HDFS");

        Map<String, String> expected = new HashMap<>();
        expected.put("fs.defaultFS", "hdfs://transactions-master0.hybrid.cloudera.org:8020");
        assertThat(result.getApplications().get("HDFS").getConfig()).containsExactlyEntriesOf(expected);
    }

    @Test
    void testGetDatalakeServicesResponseHdfsHa() throws Exception {
        when(servicesResourceApi.getClientConfig(CLUSTER_NAME, "hdfs"))
                .thenReturn(new ClassPathResource("cmclientconfig/hdfs-ha.zip").getFile());

        DescribeDatalakeServicesResponse result = underTest.getDatalakeServices(cluster);

        assertThat(result.getClusterid()).isEqualTo(CLUSTER_CRN);
        assertThat(result.getDeploymentType()).isEqualTo(DeploymentType.PDL);
        assertThat(result.getApplications()).containsKey("HDFS");

        Map<String, String> expected = new HashMap<>();
        expected.put("dfs_nameservices", "ns1");
        expected.put("dfs.ha.namenodes.ns1", "namenode1546336375,namenode1546336404");
        expected.put("dfs.namenode.rpc-address.ns1.namenode1546336375", "b-dbajzath2-dl-worker0.hybrid.cloudera.org:8020");
        expected.put("dfs.namenode.servicerpc-address.ns1.namenode1546336375", "b-dbajzath2-dl-worker0.hybrid.cloudera.org:8022");
        expected.put("dfs.namenode.http-address.ns1.namenode1546336375", "b-dbajzath2-dl-worker0.hybrid.cloudera.org:9870");
        expected.put("dfs.namenode.https-address.ns1.namenode1546336375", "b-dbajzath2-dl-worker0.hybrid.cloudera.org:9871");
        expected.put("dfs.namenode.rpc-address.ns1.namenode1546336404", "b-dbajzath2-dl-worker1.hybrid.cloudera.org:8020");
        expected.put("dfs.namenode.servicerpc-address.ns1.namenode1546336404", "b-dbajzath2-dl-worker1.hybrid.cloudera.org:8022");
        expected.put("dfs.namenode.http-address.ns1.namenode1546336404", "b-dbajzath2-dl-worker1.hybrid.cloudera.org:9870");
        expected.put("dfs.namenode.https-address.ns1.namenode1546336404", "b-dbajzath2-dl-worker1.hybrid.cloudera.org:9871");
        expected.put("fs.defaultFS", "hdfs://ns1");
        assertThat(result.getApplications().get("HDFS").getConfig()).containsExactlyEntriesOf(expected);
    }

    @Test
    void testGetDatalakeServicesResponseApiException() throws Exception {
        ApiException cause = new ApiException("500");
        when(servicesResourceApi.getClientConfig(CLUSTER_NAME, "hdfs")).thenThrow(cause);

        assertThatThrownBy(() -> underTest.getDatalakeServices(cluster))
                .isInstanceOf(RemoteEnvironmentException.class)
                .hasCause(cause)
                .hasMessage("Failed to get HDFS client config from Cloudera Manager");
    }

    @Test
    void testGetDatalakeServicesResponseIOException() throws Exception {
        when(servicesResourceApi.getClientConfig(CLUSTER_NAME, "hdfs"))
                .thenReturn(new File("invalid-path"));

        assertThatThrownBy(() -> underTest.getDatalakeServices(cluster))
                .isInstanceOf(RemoteEnvironmentException.class)
                .hasNoCause()
                .hasMessage("Failed to read HDFS client config zip downloaded from Cloudera Manager. Please contact Cloudera support to get this resolved.");
    }

    @Test
    void testGetDatalakeServicesResponseInvalidXml() throws Exception {
        when(servicesResourceApi.getClientConfig(CLUSTER_NAME, "hdfs"))
                .thenReturn(new ClassPathResource("cmclientconfig/hdfs-invalid-site-xml.zip").getFile());

        assertThatThrownBy(() -> underTest.getDatalakeServices(cluster))
                .isInstanceOf(RemoteEnvironmentException.class)
                .hasNoCause()
                .hasMessage("Failed to parse XML configuration received from Cloudera Manager. Please contact Cloudera support to get this resolved.");
    }

}
