package com.sequenceiq.cloudbreak.sdx.pdl.util;

import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HDFS_SERVICE;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HIVE_SERVICE;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HdfsNameNode.HDFS_NAMENODE_NAMESERVICE;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.HdfsNameNode.HDFS_NAMENODE_ROLE_TYPE;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.Hive.HIVE_WAREHOUSE_DIRECTORY;
import static com.sequenceiq.cloudbreak.sdx.RdcConstants.Hive.HIVE_WAREHOUSE_EXTERNAL_DIRECTORY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.cdp.servicediscovery.model.ApiMapEntry;
import com.cloudera.cdp.servicediscovery.model.ApiRemoteDataContext;
import com.cloudera.cdp.servicediscovery.model.Application;
import com.cloudera.cdp.servicediscovery.model.DescribeDatalakeServicesResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.sdx.RdcView;
import com.sequenceiq.cloudbreak.util.FileReaderUtils;

@ExtendWith(MockitoExtension.class)
class PdlRdcUtilTest {

    private static final String RDC;

    static {
        try {
            RDC = FileReaderUtils.readFileFromClasspath("com/sequenceiq/cloudbreak/sdx/common/service/rdc.json");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @InjectMocks
    private PdlRdcUtil underTest;

    private DescribeDatalakeServicesResponse describeDatalakeServicesResponse;

    private RdcView rdcView;

    @BeforeEach
    void setUp() {
        rdcView = new RdcView("crn", RDC, null, null, null);
        describeDatalakeServicesResponse = new DescribeDatalakeServicesResponse();
    }

    @Test
    void extendRdcViewWithoutHdfs() {
        rdcView = mock();
        describeDatalakeServicesResponse.applications(Map.of());
        RdcView result = underTest.extendRdcView(rdcView, describeDatalakeServicesResponse);
        verifyNoInteractions(rdcView);
    }

    @Test
    void extendRdcViewWithNonHaHdfs() throws Exception {
        String namenodeEndpoint = "hdfs://telematics-hybrid-env-dl-master0.hybrid.cloudera.org:8020";
        describeDatalakeServicesResponse.applications(Map.of(
                HDFS_SERVICE, new Application().config(Map.of(
                        "fs.defaultFS", namenodeEndpoint
                ))
        ));

        RdcView result = underTest.extendRdcView(rdcView, describeDatalakeServicesResponse);

        assertThat(result.getEndpoints(HDFS_SERVICE, HDFS_NAMENODE_ROLE_TYPE)).containsExactly(namenodeEndpoint);
        ApiRemoteDataContext apiRemoteDataContext = new ObjectMapper().readValue(rdcView.getRemoteDataContext().get(), ApiRemoteDataContext.class);
        List<ApiMapEntry> hiveConfigs = apiRemoteDataContext.getEndPoints().stream()
                .filter(apiEndPoint -> HIVE_SERVICE.equalsIgnoreCase(apiEndPoint.getServiceType()))
                .flatMap(apiEndPoint -> apiEndPoint.getServiceConfigs().stream())
                .toList();
        assertThat(hiveConfigs)
                .contains(
                        new ApiMapEntry().key(HIVE_WAREHOUSE_EXTERNAL_DIRECTORY).value(namenodeEndpoint + "/warehouse/tablespace/external/hive"),
                        new ApiMapEntry().key(HIVE_WAREHOUSE_DIRECTORY).value("s3a://cb-group/akanto/data/warehouse/tablespace/managed/hive")
                );
    }

    @Test
    void extendRdcViewWithHaHdfs() throws Exception {
        String namenodeEndpoint = "hdfs://ns1";
        Map<String, String> hdfsConfig = Map.of(
                "fs.defaultFS", namenodeEndpoint,
                "dfs_nameservices", "ns1",
                "dfs.ha.namenodes.ns1", "namenode1546336453,namenode1546336482",
                "dfs.namenode.rpc-address.ns1.namenode1546336453", "b-dbajzath-dl-worker0.hybrid.cloudera.org:8020",
                "dfs.namenode.rpc-address.ns1.namenode1546336482", "b-dbajzath-dl-worker1.hybrid.cloudera.org:8020"
        );
        describeDatalakeServicesResponse.applications(Map.of(
                HDFS_SERVICE, new Application().config(hdfsConfig)
        ));

        RdcView result = underTest.extendRdcView(rdcView, describeDatalakeServicesResponse);

        assertThat(result.getEndpoints(HDFS_SERVICE, HDFS_NAMENODE_ROLE_TYPE)).containsExactly(namenodeEndpoint);
        assertThat(result.getRoleConfigs(HDFS_SERVICE, HDFS_NAMENODE_ROLE_TYPE)).isEqualTo(Map.of(
                HDFS_NAMENODE_NAMESERVICE, "ns1",
                "dfs.ha.namenodes.ns1", "namenode1546336453,namenode1546336482",
                "dfs.namenode.rpc-address.ns1.namenode1546336453", "b-dbajzath-dl-worker0.hybrid.cloudera.org:8020",
                "dfs.namenode.rpc-address.ns1.namenode1546336482", "b-dbajzath-dl-worker1.hybrid.cloudera.org:8020"
        ));
        ApiRemoteDataContext apiRemoteDataContext = new ObjectMapper().readValue(rdcView.getRemoteDataContext().get(), ApiRemoteDataContext.class);
        List<ApiMapEntry> hiveConfigs = apiRemoteDataContext.getEndPoints().stream()
                .filter(apiEndPoint -> HIVE_SERVICE.equalsIgnoreCase(apiEndPoint.getServiceType()))
                .flatMap(apiEndPoint -> apiEndPoint.getServiceConfigs().stream())
                .toList();
        assertThat(hiveConfigs)
                .contains(
                        new ApiMapEntry().key(HIVE_WAREHOUSE_EXTERNAL_DIRECTORY).value(namenodeEndpoint + "/warehouse/tablespace/external/hive"),
                        new ApiMapEntry().key(HIVE_WAREHOUSE_DIRECTORY).value("s3a://cb-group/akanto/data/warehouse/tablespace/managed/hive")
                );
    }

}
