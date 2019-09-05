package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.model.v2.AmbariV2Request;
import com.sequenceiq.cloudbreak.api.model.v2.ClusterV2Request;
import com.sequenceiq.it.IntegrationTestContext;

public abstract class StackPostStrategyRoot implements Strategy {

    protected static final String SUBNET_ID_KEY = "subnetId";

    protected static final String NETWORK_ID_KEY = "networkId";

    protected void postStackAndSetRequestForEntity(IntegrationTestContext context, CloudbreakClient client, StackEntity stackEntity) throws Exception {
        log(" Name:\n" + stackEntity.getRequest().getGeneral().getName());
        logJSON(" Stack post request:\n", stackEntity.getRequest());
        stackEntity.setResponse(
                client.getCloudbreakClient()
                        .stackV3Endpoint()
                        .createInWorkspace(client.getWorkspaceId(), stackEntity.getRequest()));
        logJSON(" Stack post response:\n", stackEntity.getResponse());
        log(" ID:\n" + stackEntity.getResponse().getId());
    }

    protected void setImageSettingsIfNeeded(StackEntity stackEntity, IntegrationTestContext integrationTestContext) {
        var imageSettings = ImageSettingsEntity.getTestContextImageSettings().apply(integrationTestContext);
        if (imageSettings != null) {
            stackEntity.getRequest().setImageSettings(imageSettings.getRequest());
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
        if (credential != null && stackEntity.getRequest().getGeneral().getCredentialName() == null) {
            stackEntity.getRequest().getGeneral().setCredentialName(credential.getName());
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
        var kerberos = KerberosEntity.getTestContextCluster().apply(integrationTestContext);
        boolean updateKerberos = stackEntity.getRequest().getCluster() != null && stackEntity.getRequest().getCluster().getAmbari() != null
                && stackEntity.getRequest().getCluster().getAmbari().getKerberos() == null;
        if (kerberos != null && updateKerberos) {
            AmbariV2Request ambariReq = stackEntity.getRequest().getCluster().getAmbari();
            ambariReq.setEnableSecurity(true);
            ambariReq.setKerberos(kerberos.getRequest());
        }
    }

    protected void setGatewayIfNeeded(StackEntity stackEntity, IntegrationTestContext integrationTestContext) {
        var clusterGateway = ClusterGateway.getTestContextGateway().apply(integrationTestContext);
        if (clusterGateway != null) {
            if (stackEntity.hasCluster()) {
                ClusterV2Request clusterV2Request = stackEntity.getRequest().getCluster();
                AmbariV2Request ambariV2Request = clusterV2Request.getAmbari();
                if (ambariV2Request != null) {
                    ambariV2Request.setGateway(clusterGateway.getRequest());
                }
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
                .setServiceAccountEmail(credential.getResponse().getParameters().get("serviceAccountId").toString());
    }
}
