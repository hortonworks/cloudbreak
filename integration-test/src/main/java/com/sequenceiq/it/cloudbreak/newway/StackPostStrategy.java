package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.it.IntegrationTestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static com.sequenceiq.it.cloudbreak.newway.CloudbreakClient.getTestContextCloudbreakClient;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class StackPostStrategy implements Strategy {
    private static final Logger LOGGER = LoggerFactory.getLogger(StackPostStrategy.class);

    @Override
    public void doAction(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        StackEntity stackEntity = (StackEntity) entity;
        CloudbreakClient client = getTestContextCloudbreakClient().apply(integrationTestContext);


        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);

        if (credential != null && stackEntity.getRequest().getGeneral().getCredentialName() == null) {
            stackEntity.getRequest().getGeneral().setCredentialName(credential.getName());
        }

        Cluster cluster = Cluster.getTestContextCluster().apply(integrationTestContext);
        if (cluster != null) {
            if (stackEntity.getRequest().getCluster() == null) {
                stackEntity.getRequest().setCluster(cluster.getRequest());
            }
            if (cluster.getRequest().getCloudStorage().getS3() != null && isEmpty(cluster.getRequest().getCloudStorage().getS3().getInstanceProfile())) {
                AccessConfig accessConfig = AccessConfig.getTestContextAccessConfig().apply(integrationTestContext);
                List<String> arns = accessConfig
                        .getResponse()
                        .getAccessConfigs()
                        .stream()
                        .map(accessConfigJson -> accessConfigJson.getProperties().get("arn").toString())
                        .sorted()
                        .distinct()
                        .collect(Collectors.toList());
                cluster.getRequest().getCloudStorage().getS3().setInstanceProfile(arns.get(0));
            } else if (cluster.getRequest().getCloudStorage().getGcs() != null && credential != null) {
                cluster.getRequest().getCloudStorage().getGcs()
                        .setServiceAccountEmail(credential.getResponse().getParameters().get("serviceAccountId").toString());
            }
        }

        Kerberos kerberos = Kerberos.getTestContextCluster().apply(integrationTestContext);
        boolean updateKerberos = stackEntity.getRequest().getCluster() != null && stackEntity.getRequest().getCluster().getAmbari() != null
                && stackEntity.getRequest().getCluster().getAmbari().getKerberos() == null;
        if (kerberos != null && updateKerberos) {
            AmbariV2Request ambariReq = stackEntity.getRequest().getCluster().getAmbari();
            ambariReq.setEnableSecurity(true);
            ambariReq.setKerberos(kerberos.getRequest());
        }

        ClusterGateway clusterGateway = ClusterGateway.getTestContextGateway().apply(integrationTestContext);
        if (clusterGateway != null) {
            if (stackEntity.hasCluster()) {
                ClusterV2Request clusterV2Request = stackEntity.getRequest().getCluster();
                AmbariV2Request ambariV2Request = clusterV2Request.getAmbari();
                if (ambariV2Request != null) {
                    ambariV2Request.setGateway(clusterGateway.getRequest());
                }
            }
        }

        ImageSettings imageSettings = ImageSettings.getTestContextImageSettings().apply(integrationTestContext);
        if (imageSettings != null) {
            stackEntity.getRequest().setImageSettings(imageSettings.getRequest());
        }

        HostGroups hostGroups = HostGroups.getTestContextHostGroups().apply(integrationTestContext);
        if (hostGroups != null) {
            stackEntity.getRequest().setInstanceGroups(hostGroups.getRequest());
        }

        log(" Name:\n" + stackEntity.getRequest().getGeneral().getName());
        logJSON(" Stack post request:\n", stackEntity.getRequest());
        stackEntity.setResponse(
                client.getCloudbreakClient()
                        .stackV2Endpoint()
                        .postPrivate(stackEntity.getRequest()));
        logJSON(" Stack post response:\n", stackEntity.getResponse());
        log(" ID:\n" + stackEntity.getResponse().getId());
    }
}
