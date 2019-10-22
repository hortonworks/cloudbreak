package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy.SubnetStratgyType.FILL;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.microsoft.azure.management.network.Subnet;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStorageView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;

@Component
class AzureStackViewProvider {

    private static final int AZURE_NUMBER_OF_RESERVED_IPS = 5;

    @Value("${cb.azure.host.name.prefix.length}")
    private int stackNamePrefixLength;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureStorage azureStorage;

    AzureStackView getAzureStack(AzureCredentialView azureCredentialView, CloudStack cloudStack, AzureClient client, AuthenticatedContext ac) {
        Map<String, String> customImageNamePerInstance = getCustomImageNamePerInstance(ac, cloudStack);
        Network network = cloudStack.getNetwork();
        Map<String, Integer> availableIPs = getNumberOfAvailableIPsInSubnets(client, network);
        return new AzureStackView(ac.getCloudContext().getName(), stackNamePrefixLength, cloudStack.getGroups(), new AzureStorageView(azureCredentialView,
                ac.getCloudContext(),
                azureStorage, azureStorage.getArmAttachedStorageOption(cloudStack.getParameters())),
                AzureSubnetStrategy.getAzureSubnetStrategy(FILL, azureUtils.getCustomSubnetIds(network), availableIPs),
                customImageNamePerInstance);
    }

    private Map<String, String> getCustomImageNamePerInstance(AuthenticatedContext ac, CloudStack cloudStack) {
        AzureClient client = ac.getParameter(AzureClient.class);
        Map<String, String> imageNameMap = new HashMap<>();
        Map<String, String> customImageNamePerInstance = new HashMap<>();
        for (Group group : cloudStack.getGroups()) {
            for (CloudInstance instance : group.getInstances()) {
                String imageId = instance.getTemplate().getImageId();
                if (StringUtils.isNotBlank(imageId)) {
                    String imageCustomName = imageNameMap.computeIfAbsent(imageId, s -> azureStorage.getCustomImageId(client, ac, cloudStack, imageId));
                    customImageNamePerInstance.put(instance.getInstanceId(), imageCustomName);
                }
            }
        }
        return customImageNamePerInstance;
    }

    private Map<String, Integer> getNumberOfAvailableIPsInSubnets(AzureClient client, Network network) {
        Map<String, Integer> result = new HashMap<>();
        String resourceGroup = network.getStringParameter("resourceGroupName");
        String networkId = network.getStringParameter("networkId");
        Collection<String> subnetIds = azureUtils.getCustomSubnetIds(network);
        for (String subnetId : subnetIds) {
            Subnet subnet = client.getSubnetProperties(resourceGroup, networkId, subnetId);
            int available = getAvailableAddresses(subnet);
            result.put(subnetId, available);
        }
        return result;
    }

    private int getAvailableAddresses(Subnet subnet) {
        SubnetUtils su = new SubnetUtils(subnet.addressPrefix());
        su.setInclusiveHostCount(true);
        int available = su.getInfo().getAddressCount();
        int used = subnet.networkInterfaceIPConfigurationCount();
        return available - used - AZURE_NUMBER_OF_RESERVED_IPS;
    }
}
