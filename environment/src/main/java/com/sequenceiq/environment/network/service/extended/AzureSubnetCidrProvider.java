package com.sequenceiq.environment.network.service.extended;

import static com.sequenceiq.environment.network.service.Cidrs.cidrs;
import static com.sequenceiq.environment.network.service.extended.DwxSubnetConstants.DWX_SUBNET_IP_COUNT;
import static com.sequenceiq.environment.network.service.extended.DwxSubnetConstants.DWX_SUBNET_MASK;
import static com.sequenceiq.environment.network.service.extended.DwxSubnetConstants.DWX_SUBNET_IP_OFFSET;
import static com.sequenceiq.environment.network.service.extended.ExtendedSubnetTypeProvider.PLUS_BITS_FOR_19_MASK;
import static com.sequenceiq.environment.network.service.extended.ExtendedSubnetTypeProvider.PLUS_BITS_FOR_24_MASK;
import static com.sequenceiq.environment.network.service.extended.MlxSubnetConstants.MLX_SUBNET_IP_COUNT;
import static com.sequenceiq.environment.network.service.extended.MlxSubnetConstants.MLX_SUBNET_MASK;
import static com.sequenceiq.environment.network.service.extended.MlxSubnetConstants.MLX_SUBNET_IP_OFFSET;
import static com.sequenceiq.environment.network.service.extended.PrivateSubnetConstants.PRIVATE_SUBNET_IP_COUNT;
import static com.sequenceiq.environment.network.service.extended.PrivateSubnetConstants.PRIVATE_SUBNET_IP_OFFSET;
import static com.sequenceiq.environment.network.service.extended.PrivateSubnetConstants.PRIVATE_SUBNET_MASK;
import static com.sequenceiq.environment.network.service.extended.PublicSubnetConstants.PUBLIC_SUBNET_IP_COUNT;
import static com.sequenceiq.environment.network.service.extended.PublicSubnetConstants.PUBLIC_SUBNET_MASK;
import static com.sequenceiq.environment.network.service.extended.PublicSubnetConstants.PUBLIC_SUBNET_IP_OFFSET;

import java.util.HashSet;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.network.NetworkSubnetRequest;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.environment.network.service.Cidrs;
import com.sequenceiq.environment.network.service.SubnetCidrProvider;

@Component
public class AzureSubnetCidrProvider implements SubnetCidrProvider {

    private final ExtendedSubnetTypeProvider extendedSubnetTypeProvider;

    public AzureSubnetCidrProvider(ExtendedSubnetTypeProvider extendedSubnetTypeProvider) {
        this.extendedSubnetTypeProvider = extendedSubnetTypeProvider;
    }

    @Override
    public Cidrs provide(String networkCidr, boolean privateSubnetEnabled) {
        String[] ip = extendedSubnetTypeProvider.getIp(networkCidr);
        Set<NetworkSubnetRequest> publicSubnets = new HashSet<>();
        Set<NetworkSubnetRequest> privateSubnets = new HashSet<>();

        extendedSubnetTypeProvider.updateCidrAndAddToList(PUBLIC_SUBNET_IP_OFFSET, PUBLIC_SUBNET_IP_COUNT, PLUS_BITS_FOR_24_MASK, ip, publicSubnets,
                SubnetType.PUBLIC, PUBLIC_SUBNET_MASK);
        extendedSubnetTypeProvider.updateCidrAndAddToList(MLX_SUBNET_IP_OFFSET, MLX_SUBNET_IP_COUNT, PLUS_BITS_FOR_24_MASK, ip, privateSubnets,
                SubnetType.MLX, MLX_SUBNET_MASK);
        extendedSubnetTypeProvider.updateCidrAndAddToList(DWX_SUBNET_IP_OFFSET, DWX_SUBNET_IP_COUNT, PLUS_BITS_FOR_19_MASK, ip, privateSubnets,
                SubnetType.DWX, DWX_SUBNET_MASK);
        extendedSubnetTypeProvider.updateCidrAndAddToList(PRIVATE_SUBNET_IP_OFFSET, PRIVATE_SUBNET_IP_COUNT, PLUS_BITS_FOR_19_MASK, ip, privateSubnets,
                SubnetType.PRIVATE, PRIVATE_SUBNET_MASK);
        return cidrs(publicSubnets, privateSubnetEnabled ? privateSubnets : new HashSet<>());
    }

    @Override
    public String cloudPlatform() {
        return "AZURE";
    }

}
