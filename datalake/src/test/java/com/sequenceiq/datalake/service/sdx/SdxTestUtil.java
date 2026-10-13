package com.sequenceiq.datalake.service.sdx;

import java.util.List;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.product.ClouderaManagerProductV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.repository.ClouderaManagerRepositoryV4Request;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;

public class SdxTestUtil {

    public static final String USER_CRN = "crn:cdp:iam:us-west-1:hortonworks:user:perdos@hortonworks.com";

    public static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:default:environment:e438a2db-d650-4132-ae62-242c5ba2f784";

    public static final Long SDX_ID = 2L;

    public static final String SDX_CRN = "crn";

    public static final String CLUSTER_NAME = "test-sdx-cluster";

    public static final String ENVIRONMENT_NAME = "environment-name";

    private SdxTestUtil() {
    }

    public static SdxCluster getSdxCluster() {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setId(SDX_ID);
        sdxCluster.setCrn(SDX_CRN);
        sdxCluster.setEnvCrn(ENVIRONMENT_CRN);
        sdxCluster.setEnvName(ENVIRONMENT_NAME);
        sdxCluster.setClusterName(CLUSTER_NAME);
        sdxCluster.setSdxDatabase(new SdxDatabase());
        return sdxCluster;
    }

    public static ClouderaManagerV4Request getUpgradeComponents() {
        ClouderaManagerRepositoryV4Request repository = new ClouderaManagerRepositoryV4Request();
        repository.setVersion("7.13.2.20000-83309838");
        repository.setBaseUrl("https://repo.example.com/cm/");
        repository.setGpgKeyUrl("https://repo.example.com/cm/key.asc");
        ClouderaManagerProductV4Request cdh = new ClouderaManagerProductV4Request();
        cdh.setName("CDH");
        cdh.setVersion("7.3.2-1.cdh7.3.2.p20000.83281140");
        cdh.setParcel("https://repo.example.com/cdh/");
        ClouderaManagerProductV4Request cfm = new ClouderaManagerProductV4Request();
        cfm.setName("CFM");
        cfm.setVersion("2.2.9");
        cfm.setParcel("https://repo.example.com/cfm/");
        cfm.setCsd(List.of("https://repo.example.com/NIFI.jar", "https://repo.example.com/NIFIREGISTRY.jar"));
        return new ClouderaManagerV4Request().withRepository(repository).withProducts(List.of(cdh, cfm));
    }

}
