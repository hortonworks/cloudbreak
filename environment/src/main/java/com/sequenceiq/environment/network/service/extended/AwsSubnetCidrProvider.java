package com.sequenceiq.environment.network.service.extended;

import static com.sequenceiq.environment.network.service.Cidrs.cidrs;
import static com.sequenceiq.environment.network.service.extended.ExtendedSubnetTypeProvider.PLUS_BITS_FOR_19_MASK;
import static com.sequenceiq.environment.network.service.extended.ExtendedSubnetTypeProvider.PLUS_BITS_FOR_24_MASK;
import static com.sequenceiq.environment.network.service.extended.PrivateSubnetConstants.PRIVATE_SUBNET_IP_COUNT;
import static com.sequenceiq.environment.network.service.extended.PrivateSubnetConstants.PRIVATE_SUBNET_IP_OFFSET;
import static com.sequenceiq.environment.network.service.extended.PrivateSubnetConstants.PRIVATE_SUBNET_MASK;
import static com.sequenceiq.environment.network.service.extended.PublicSubnetConstants.PUBLIC_SUBNET_IP_COUNT;
import static com.sequenceiq.environment.network.service.extended.PublicSubnetConstants.PUBLIC_SUBNET_IP_OFFSET;
import static com.sequenceiq.environment.network.service.extended.PublicSubnetConstants.PUBLIC_SUBNET_MASK;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.environment.network.service.Cidrs;
import com.sequenceiq.environment.network.service.SubnetCidrProvider;

@Component
public class AwsSubnetCidrProvider implements SubnetCidrProvider {

    private final ExtendedSubnetTypeProvider extendedSubnetTypeProvider;

    public AwsSubnetCidrProvider(ExtendedSubnetTypeProvider extendedSubnetTypeProvider) {
        this.extendedSubnetTypeProvider = extendedSubnetTypeProvider;
    }

    @Override
    public Cidrs provide(String networkCidr) {
        String[] ip = extendedSubnetTypeProvider.getIp(networkCidr);
        Set<NetworkSubnetRequest> publicSubnets = new HashSet<>();
        Set<NetworkSubnetRequest> privateSubnets = new HashSet<>();

        extendedSubnetTypeProvider.updateCidrAndAddToList(PUBLIC_SUBNET_IP_OFFSET, PUBLIC_SUBNET_IP_COUNT, PLUS_BITS_FOR_24_MASK, ip, publicSubnets,
                SubnetType.PUBLIC, PUBLIC_SUBNET_MASK);
        extendedSubnetTypeProvider.updateCidrAndAddToList(PRIVATE_SUBNET_IP_OFFSET, PRIVATE_SUBNET_IP_COUNT, PLUS_BITS_FOR_19_MASK, ip, privateSubnets,
                SubnetType.PRIVATE, PRIVATE_SUBNET_MASK);
        return cidrs(publicSubnets, privateSubnets);
    }

    @Override
    public String cloudPlatform() {
        return "AWS";
    }

}
