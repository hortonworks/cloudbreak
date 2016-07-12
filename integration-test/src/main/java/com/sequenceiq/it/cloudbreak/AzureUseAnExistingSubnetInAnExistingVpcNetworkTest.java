package com.sequenceiq.it.cloudbreak;

import static com.amazonaws.services.cloudformation.model.StackStatus.CREATE_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_COMPLETE;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_FAILED;
import static com.amazonaws.services.cloudformation.model.StackStatus.ROLLBACK_IN_PROGRESS;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.amazonaws.services.cloudformation.model.StackStatus;
import com.microsoft.azure.Azure;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.rest.credentials.ServiceClientCredentials;
import com.sequenceiq.cloudbreak.api.model.NetworkJson;

public class AzureUseAnExistingSubnetInAnExistingVpcNetworkTest extends AbstractCloudbreakIntegrationTest {

    private static final List<StackStatus> FAILED_STATUSES = Arrays.asList(CREATE_FAILED, ROLLBACK_IN_PROGRESS, ROLLBACK_FAILED, ROLLBACK_COMPLETE);
    private static final Logger LOGGER = LoggerFactory.getLogger(AzureUseAnExistingSubnetInAnExistingVpcNetworkTest.class);
    private static final int MAX_TRY = 30;
    @Value("${integrationtest.azurermcredential.name}")
    private String defaultName;
    @Value("${integrationtest.azurermcredential.subscriptionId}")
    private String defaultSubscriptionId;
    @Value("${integrationtest.azurermcredential.secretKey}")
    private String defaultSecretKey;
    @Value("${integrationtest.azurermcredential.accessKey}")
    private String defaultAccesKey;
    @Value("${integrationtest.azurermcredential.tenantId}")
    private String defaultTenantId;

    @Test
    @Parameters({"networkName", "description", "publicInAccount", "regionName", "resourceGroupName", "vpcName", "vpcSubnet"})
    public void createNetwork(String networkName, @Optional("") String description, @Optional("false") boolean publicInAccount,
            String regionName, @Optional("it-vpc-resource-group") String resourceGroupName, @Optional("it-vpc") String vpcName,
            @Optional("it-vpc-subnet") String vpcSubnet) throws Exception {

        ServiceClientCredentials serviceClientCredentials = new ApplicationTokenCredentials(defaultAccesKey, defaultTenantId, defaultSecretKey, null);
        Azure azure = Azure.authenticate(serviceClientCredentials).withSubscription(defaultSubscriptionId);

        azure.networks()
                .define(vpcName)
                .withRegion(regionName)
                .withNewResourceGroup(resourceGroupName)
                .withAddressSpace("10.0.0.0/16")
                .withSubnet(vpcSubnet, "10.0.0.0/16")
                .create();

        NetworkJson networkJson = new NetworkJson();
        networkJson.setName(networkName);
        networkJson.setDescription(description);
        Map<String, Object> map = new HashMap<>();
        map.put("networkId", vpcName);
        map.put("subnetId", vpcSubnet);
        map.put("resourceGroupName", resourceGroupName);
        networkJson.setParameters(map);
        networkJson.setCloudPlatform("AZURE_RM");
        networkJson.setPublicInAccount(publicInAccount);
        String id = getCloudbreakClient().networkEndpoint().postPrivate(networkJson).getId().toString();
        getItContext().putContextParam(CloudbreakITContextConstants.NETWORK_ID, id, true);
    }
}
