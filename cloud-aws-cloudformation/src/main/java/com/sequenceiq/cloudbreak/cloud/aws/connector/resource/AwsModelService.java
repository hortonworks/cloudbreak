package com.sequenceiq.cloudbreak.cloud.aws.connector.resource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.amazonaws.services.ec2.model.PrefixList;
import com.sequenceiq.cloudbreak.cloud.aws.AwsNetworkCfTemplateProvider;
import com.sequenceiq.cloudbreak.cloud.aws.CloudFormationTemplateBuilder.ModelContext;
import com.sequenceiq.cloudbreak.cloud.aws.LegacyAwsClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsCredentialView;
import com.sequenceiq.cloudbreak.cloud.aws.efs.AwsEfsFileSystem;
import com.sequenceiq.cloudbreak.cloud.aws.view.AwsInstanceProfileView;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AwsNetworkView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.notification.PersistenceNotifier;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

@Service
public class AwsModelService {

    @Value("${cb.aws.vpcendpoints.enabled.gateway.services}")
    private Set<String> enabledGatewayServices;

    @Inject
    private LegacyAwsClient awsClient;

    @Inject
    private AwsNetworkService awsNetworkService;

    public ModelContext buildDefaultModelContext(AuthenticatedContext ac, CloudStack stack, PersistenceNotifier resourceNotifier) {
        Network network = stack.getNetwork();
        AwsNetworkView awsNetworkView = new AwsNetworkView(network);
        AwsCredentialView credentialView = new AwsCredentialView(ac.getCloudCredential());
        String regionName = ac.getCloudContext().getLocation().getRegion().value();
        AmazonEc2Client amazonEC2Client = awsClient.createEc2Client(credentialView, regionName);
        boolean mapPublicIpOnLaunch = awsNetworkService.isMapPublicOnLaunch(awsNetworkView, amazonEC2Client);

        boolean existingVPC = awsNetworkView.isExistingVPC();
        boolean existingSubnet = awsNetworkView.isExistingSubnet();

        String cidr = network.getSubnet().getCidr();
        String subnet = isNoCIDRProvided(existingVPC, existingSubnet, cidr) ? awsNetworkService.findNonOverLappingCIDR(ac, stack) : cidr;
        AwsInstanceProfileView awsInstanceProfileView = new AwsInstanceProfileView(stack);
        ModelContext modelContext = new ModelContext()
            .withAuthenticatedContext(ac)
            .withStack(stack)
            .withExistingVpc(existingVPC)
            .withExistingIGW(awsNetworkView.isExistingIGW())
            .withExistingSubnetCidr(existingSubnet ? awsNetworkService.getExistingSubnetCidr(ac, stack) : null)
            .withExistinVpcCidr(awsNetworkService.getVpcCidrs(ac, stack))
            .withExistingSubnetIds(existingSubnet ? awsNetworkView.getSubnetList() : null)
            .mapPublicIpOnLaunch(mapPublicIpOnLaunch)
            .withEnableInstanceProfile(awsInstanceProfileView.isInstanceProfileAvailable())
            .withInstanceProfileAvailable(awsInstanceProfileView.isInstanceProfileAvailable())
            .withTemplate(stack.getTemplate())
            .withDefaultSubnet(subnet)
            .withOutboundInternetTraffic(network.getOutboundInternetTraffic())
            .withVpcCidrs(network.getNetworkCidrs())
            .withPrefixListIds(getPrefixListIds(amazonEC2Client, regionName, network.getOutboundInternetTraffic()));

        AwsEfsFileSystem efsFileSystem = getAwsEfsFileSystem(stack);

        if (efsFileSystem != null) {
            modelContext.withEnableEfs(true);
            modelContext.withEfsFileSystem(efsFileSystem);
        } else {
            modelContext.withEnableEfs(false);
        }

        return modelContext;
    }

    // there should be at most one file system configured for EFS. return the first EFS configuration
    private AwsEfsFileSystem getAwsEfsFileSystem(CloudStack stack) {
        AwsEfsFileSystem efsFileSystem = null;

        if (stack.getFileSystem().isPresent()) {
            efsFileSystem = AwsEfsFileSystem.toAwsEfsFileSystem(stack.getFileSystem().get());
        }

        if (efsFileSystem == null && stack.getAdditionalFileSystem().isPresent()) {
            return AwsEfsFileSystem.toAwsEfsFileSystem(stack.getAdditionalFileSystem().get());
        }
        return efsFileSystem;
    }

    public boolean isNoCIDRProvided(boolean existingVPC, boolean existingSubnet, String cidr) {
        return existingVPC && !existingSubnet && cidr == null;
    }

    private List<String> getPrefixListIds(AmazonEc2Client amazonEC2Client, String regionName, OutboundInternetTraffic outboundInternetTraffic) {
        List<String> result = List.of();
        if (outboundInternetTraffic == OutboundInternetTraffic.DISABLED && CollectionUtils.isNotEmpty(enabledGatewayServices)) {
            Set<String> gatewayRegionServices = enabledGatewayServices.stream()
                .map(s -> String.format(AwsNetworkCfTemplateProvider.VPC_INTERFACE_SERVICE_ENDPOINT_NAME_PATTERN, regionName, s))
                .collect(Collectors.toSet());
            result = amazonEC2Client.describePrefixLists().getPrefixLists().stream()
                .filter(pl -> gatewayRegionServices.contains(pl.getPrefixListName()))
                .map(PrefixList::getPrefixListId)
                .collect(Collectors.toList());
        }
        return result;
    }
}
