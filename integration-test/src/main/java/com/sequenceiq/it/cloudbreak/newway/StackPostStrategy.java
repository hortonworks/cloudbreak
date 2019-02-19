package com.sequenceiq.it.cloudbreak.newway;

import static com.sequenceiq.it.cloudbreak.newway.CloudbreakClient.getTestContextCloudbreakClient;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.log;
import static com.sequenceiq.it.cloudbreak.newway.log.Log.logJSON;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;

public class StackPostStrategy implements Strategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackPostStrategy.class);

    private static final String SUBNET_ID_KEY = "subnetId";

    private static final String NETWORK_ID_KEY = "networkId";

    @Override
    public void doAction(IntegrationTestContext integrationTestContext, Entity entity) throws Exception {
        StackTestDto stackTestDto = (StackTestDto) entity;
        CloudbreakClient client = getTestContextCloudbreakClient().apply(integrationTestContext);

        Credential credential = Credential.getTestContextCredential().apply(integrationTestContext);

        if (credential != null && stackTestDto.getRequest().getEnvironment().getCredentialName() == null) {
            stackTestDto.getRequest().getEnvironment().setCredentialName(credential.getName());
        }

        Cluster cluster = Cluster.getTestContextCluster().apply(integrationTestContext);
        if (cluster != null) {
            if (stackTestDto.getRequest().getCluster() == null) {
                stackTestDto.getRequest().setCluster(cluster.getRequest());
            }
            if (cluster.getRequest().getCloudStorage() != null && cluster.getRequest().getCloudStorage().getS3()
                    != null && isEmpty(cluster.getRequest().getCloudStorage().getS3().getInstanceProfile())) {
                AccessConfigEntity accessConfig = AccessConfigEntity.getTestContextAccessConfig().apply(integrationTestContext);
                List<String> arns = accessConfig
                        .getResponse()
                        .getAccessConfigs()
                        .stream()
                        .map(accessConfigJson -> accessConfigJson.getProperties().get("arn").toString())
                        .sorted()
                        .distinct()
                        .collect(Collectors.toList());
                cluster.getRequest().getCloudStorage().getS3().setInstanceProfile(arns.get(0));
            } else if (cluster.getRequest().getCloudStorage() != null && cluster.getRequest().getCloudStorage().getGcs() != null && credential != null) {
                cluster.getRequest().getCloudStorage().getGcs()
                        .setServiceAccountEmail(credential.getResponse().getGcp().getP12().getServiceAccountId());
            }
        }

        Integer gatewayPort = integrationTestContext.getContextParam("MOCK_PORT", Integer.class);
        if (gatewayPort != null) {
            stackTestDto.getRequest().setGatewayPort(gatewayPort);
        }

        KerberosTestDto kerberos = new KerberosTestDto(null);
        boolean updateKerberos = stackTestDto.getRequest().getCluster() != null && stackTestDto.getRequest().getCluster().getAmbari() != null
                && stackTestDto.getRequest().getCluster().getKerberosName() == null;
        if (kerberos != null && updateKerberos) {
            var environment = stackTestDto.getRequest().getCluster();
            environment.setKerberosName(kerberos.getRequest().getName());
        }

        ClusterGateway clusterGateway = ClusterGateway.getTestContextGateway().apply(integrationTestContext);
        if (clusterGateway != null) {
            if (stackTestDto.hasCluster()) {
                var clusterV2Request = stackTestDto.getRequest().getCluster();
                clusterV2Request.setGateway(clusterGateway.getRequest());
            }
        }

        ImageSettingsEntity imageSettings = ImageSettingsEntity.getTestContextImageSettings().apply(integrationTestContext);
        if (imageSettings != null) {
            stackTestDto.getRequest().setImage(imageSettings.getRequest());
        }

        HostGroups hostGroups = HostGroups.getTestContextHostGroups().apply(integrationTestContext);
        if (hostGroups != null) {
            stackTestDto.getRequest().setInstanceGroups(hostGroups.getRequest());
        }

        // TODO: we have to move this logic ot from here to the test or cloudprovider(helper) level -> @afarsang
        // until that, I keep this body of logic here to keep in mind
        /*var datalakeStack = DatalakeCluster.getTestContextDatalakeCluster().apply(integrationTestContext);
        if (datalakeStack != null && datalakeStack.getResponse() != null && datalakeStack.getResponse().getNetwork() != null) {
            String subnetId = null;
            String networkId = null;
            var properties = Optional.ofNullable(datalakeStack.getResponse().getNetwork().getParameters());
            if (properties.isPresent()) {
                if (!isEmpty((CharSequence) properties.get().get(SUBNET_ID_KEY))) {
                    subnetId = properties.get().get(SUBNET_ID_KEY).toString();
                }
                if (!isEmpty((CharSequence) properties.get().get(NETWORK_ID_KEY))) {
                    networkId = properties.get().get(NETWORK_ID_KEY).toString();
                }
            }
            if (stackEntity.getRequest().getNetwork() != null && stackEntity.getRequest().getNetwork().getParameters() != null) {
                stackEntity.getRequest().getNetwork().getParameters().put(SUBNET_ID_KEY, subnetId);
                stackEntity.getRequest().getNetwork().getParameters().put(NETWORK_ID_KEY, networkId);
            } else {
                var network = new NetworkV2Request();
                var params = new LinkedHashMap<String, Object>();
                params.put(SUBNET_ID_KEY, subnetId);
                params.put(NETWORK_ID_KEY, networkId);
                network.setParameters(params);
                stackEntity.getRequest().setNetwork(network);
            }
            stackEntity.getRequest().getNetwork().setSubnetCIDR(null);
            stackEntity.getRequest().getNetwork().getParameters().put("routerId", null);
            stackEntity.getRequest().getNetwork().getParameters().put("publicNetId", null);
            stackEntity.getRequest().getNetwork().getParameters().put("noPublicIp", false);
            stackEntity.getRequest().getNetwork().getParameters().put("noFirewallRules", false);
            stackEntity.getRequest().getNetwork().getParameters().put("internetGatewayId", null);
        }*/

        log(" Name:\n" + stackTestDto.getRequest().getName());
        logJSON(" Stack post request:\n", stackTestDto.getRequest());
        stackTestDto.setResponse(
                client.getCloudbreakClient()
                        .stackV4Endpoint()
                        .post(client.getWorkspaceId(), stackTestDto.getRequest()));
        logJSON(" Stack post response:\n", stackTestDto.getResponse());
        log(" ID:\n" + stackTestDto.getResponse().getId());
    }
}
