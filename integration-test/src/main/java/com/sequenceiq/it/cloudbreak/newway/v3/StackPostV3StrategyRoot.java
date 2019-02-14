package com.sequenceiq.it.cloudbreak.newway.v3;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;
import java.util.stream.Collectors;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.AccessConfigEntity;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Cluster;
import com.sequenceiq.it.cloudbreak.newway.ClusterGateway;
import com.sequenceiq.it.cloudbreak.newway.Credential;
import com.sequenceiq.it.cloudbreak.newway.HostGroups;
import com.sequenceiq.it.cloudbreak.newway.ImageSettingsEntity;
import com.sequenceiq.it.cloudbreak.newway.StackEntity;
import com.sequenceiq.it.cloudbreak.newway.Strategy;

public abstract class StackPostV3StrategyRoot implements Strategy {

    protected static final String SUBNET_ID_KEY = "subnetId";

    protected static final String NETWORK_ID_KEY = "networkId";

    protected void postStackAndSetRequestForEntity(IntegrationTestContext integrationTestContext, CloudbreakClient client, StackEntity stackEntity)
            throws Exception {
        log(" Name:\n" + stackEntity.getRequest().getName());
        logJSON(" Stack post request:\n", stackEntity.getRequest());
        Long workspaceId = integrationTestContext.getContextParam(CloudbreakTest.WORKSPACE_ID, Long.class);
        stackEntity.setResponse(
                client.getCloudbreakClient()
                        .stackV4Endpoint()
                        .post(workspaceId, stackEntity.getRequest()));
        logJSON(" Stack post response:\n", stackEntity.getResponse());
        log(" ID:\n" + stackEntity.getResponse().getId());
    }

    protected void setImageSettingsIfNeeded(StackEntity stackEntity, IntegrationTestContext integrationTestContext) {
        var imageSettings = ImageSettingsEntity.getTestContextImageSettings().apply(integrationTestContext);
        if (imageSettings != null) {
            stackEntity.getRequest().setImage(imageSettings.getRequest());
        }
    }

    protected void setHostGroupIfNeeded(StackEntity stackEntity, IntegrationTestContext integrationTestContext) {
        var hostGroups = HostGroups.getTestContextHostGroups().apply(integrationTestContext);
        if (hostGroups != null) {
            stackEntity.getRequest().setInstanceGroups(hostGroups.getRequest());
        }
    }

    protected Credential setCredentialIfNeededAndReturnIt(StackEntity stackEntity, IntegrationTestContext integrationTestContext) {
        var credential = Credential.getTestContextCredential().apply(integrationTestContext);
        if (credential != null && stackEntity.getRequest().getEnvironment().getCredentialName() == null) {
            stackEntity.getRequest().getEnvironment().setCredentialName(credential.getName());
        }
        return credential;
    }

    protected void setClusterIfNeeded(StackEntity stackEntity, IntegrationTestContext integrationTestContext, Credential credential) {
        var cluster = Cluster.getTestContextCluster().apply(integrationTestContext);
        if (cluster != null) {
            if (cluster.getRequest().getCloudStorage().getS3() != null && isEmpty(cluster.getRequest().getCloudStorage().getS3().getInstanceProfile())) {
                setS3CloudStorageForCluster(cluster, integrationTestContext);
            } else if (cluster.getRequest().getCloudStorage().getGcs() != null && credential != null) {
                setGcsCloudStorageForCluster(cluster, credential);
            }
            if (stackEntity.getRequest().getCluster() == null) {
                stackEntity.getRequest().setCluster(cluster.getRequest());
            }
        }
    }

    protected void setKerberosIfNeeded(StackEntity stackEntity, IntegrationTestContext integrationTestContext) {
//        var kerberos = KerberosEntity.getTestContextCluster().apply(integrationTestContext);
//        boolean updateKerberos = stackEntity.getRequest().getCluster() != null && stackEntity.getRequest().getCluster().getAmbari() != null
//                && stackEntity.getRequest().getCluster().getKerberosName() == null;
//        if (kerberos != null && updateKerberos) {
//            stackEntity.getRequest().getCluster().setKerberosName(kerberos.getRequest().getName());
//        }
    }

    protected void setGatewayIfNeeded(StackEntity stackEntity, IntegrationTestContext integrationTestContext) {
        var clusterGateway = ClusterGateway.getTestContextGateway().apply(integrationTestContext);
        if (clusterGateway != null) {
            if (stackEntity.hasCluster()) {
                stackEntity.getRequest().getCluster().setGateway(clusterGateway.getRequest());
            }
        }
    }

    private void setS3CloudStorageForCluster(Cluster cluster, IntegrationTestContext integrationTestContext) {
        var accessConfig = AccessConfigEntity.getTestContextAccessConfig().apply(integrationTestContext);
        List<String> arns = accessConfig
                .getResponse()
                .getAccessConfigs()
                .stream()
                .map(accessConfigJson -> accessConfigJson.getProperties().get("arn").toString())
                .sorted()
                .distinct()
                .collect(Collectors.toList());
        cluster.getRequest().getCloudStorage().getS3().setInstanceProfile(arns.get(0));
    }

    private void setGcsCloudStorageForCluster(Cluster cluster, Credential credential) {
        cluster.getRequest().getCloudStorage().getGcs()
                .setServiceAccountEmail(credential.getResponse().getGcp().getP12().getServiceAccountId());
    }
}
