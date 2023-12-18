package com.sequenceiq.cloudbreak.cloud.azure;

import static com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy.SubnetStratgyType.FILL;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.NETWORK_ID;
import static com.sequenceiq.cloudbreak.constant.AzureConstants.RESOURCE_GROUP_NAME;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.azure.resourcemanager.network.models.Subnet;
import com.sequenceiq.cloudbreak.client.ProviderAuthenticationFailedException;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.subnetstrategy.AzureSubnetStrategy;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureCredentialView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStorageView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.service.Retry;

@Component
public class AzureStackViewProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureStackViewProvider.class);

    private static final long AZURE_NUMBER_OF_RESERVED_IPS = 5;

    @Value("${cb.azure.host.name.prefix.length}")
    private int stackNamePrefixLength;

    @Inject
    private AzureUtils azureUtils;

    @Inject
    private AzureStorage azureStorage;

    @Inject
    private AzureImageFormatValidator azureImageFormatValidator;

    @Inject
    @Qualifier("DefaultRetryService")
    private Retry retryService;

    public AzureStackView getAzureStack(AzureCredentialView azureCredentialView, CloudStack cloudStack, AzureClient client, AuthenticatedContext ac) {
        Map<String, String> customImageNamePerInstance = getCustomImageNamePerInstance(ac, cloudStack);
        Network network = cloudStack.getNetwork();
        Map<String, Long> availableIPs = getNumberOfAvailableIPsInSubnets(client, network);
        return new AzureStackView(ac.getCloudContext().getName(), stackNamePrefixLength, cloudStack.getGroups(), new AzureStorageView(azureCredentialView,
                ac.getCloudContext(),
                azureStorage, azureStorage.getArmAttachedStorageOption(cloudStack.getParameters())),
                AzureSubnetStrategy.getAzureSubnetStrategy(FILL, azureUtils.getCustomSubnetIds(network), availableIPs),
                customImageNamePerInstance);
    }

    private Map<String, String> getCustomImageNamePerInstance(AuthenticatedContext ac, CloudStack cloudStack) {
        Map<String, String> customImageNamePerInstance = new HashMap<>();

        AzureClient client = ac.getParameter(AzureClient.class);
        Map<String, String> imageNameMap = new HashMap<>();
        for (Group group : cloudStack.getGroups()) {
            for (CloudInstance instance : group.getInstances()) {
                String imageId = instance.getTemplate().getImageId();
                if (StringUtils.isNotBlank(imageId) && !azureImageFormatValidator.isMarketplaceImageFormat(imageId)) {
                    String imageCustomName = imageNameMap.computeIfAbsent(
                            imageId, s -> azureStorage.getCustomImage(client, ac, cloudStack, imageId).getId());
                    customImageNamePerInstance.put(instance.getInstanceId(), imageCustomName);
                    LOGGER.debug("Collect customImageName {} for instance {}", imageCustomName, instance.getInstanceId());
                } else {
                    LOGGER.debug("ImageId {} is empty or is a marketplace image identifier for instance {}, custom image name collection is not needed.",
                            imageId, instance.getInstanceId());
                }
            }
        }
        return customImageNamePerInstance;
    }

    private Map<String, Long> getNumberOfAvailableIPsInSubnets(AzureClient client, Network network) {
        Map<String, Long> result = new HashMap<>();
        String resourceGroup = network.getStringParameter(RESOURCE_GROUP_NAME);
        String networkId = network.getStringParameter(NETWORK_ID);
        Collection<String> subnetIds = azureUtils.getCustomSubnetIds(network);
        com.azure.resourcemanager.network.models.Network networkByResourceGroup =
                retryService.testWith1SecDelayMax5Times(
                        () -> {
                            try {
                                return client.getNetworkByResourceGroup(resourceGroup, networkId);
                            } catch (ProviderAuthenticationFailedException e) {
                                throw e;
                            } catch (RuntimeException e) {
                                LOGGER.debug("Azure network query request failed, operation will be retried.");
                                throw new Retry.ActionFailedException(e.getMessage());
                            }
                        }
                );
        for (String subnetId : subnetIds) {
            Subnet subnet = networkByResourceGroup.subnets().get(subnetId);
            long available = getAvailableAddresses(subnet);
            result.put(subnetId, available);
        }
        return result;
    }

    private long getAvailableAddresses(Subnet subnet) {
        SubnetUtils su = new SubnetUtils(subnet.addressPrefix());
        su.setInclusiveHostCount(true);
        long available = su.getInfo().getAddressCountLong();
        long used = subnet.networkInterfaceIPConfigurationCount();
        return available - used - AZURE_NUMBER_OF_RESERVED_IPS;
    }
}
