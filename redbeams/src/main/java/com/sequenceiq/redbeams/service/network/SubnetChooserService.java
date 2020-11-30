package com.sequenceiq.redbeams.service.network;

import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@Service
public class SubnetChooserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetChooserService.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    public List<CloudSubnet> chooseSubnets(List<CloudSubnet> subnetMetas, CloudPlatform cloudPlatform, DBStack dbStack) {
        NetworkConnector networkConnector = cloudPlatformConnectors.get(new CloudPlatformVariant(dbStack.getCloudPlatform(), dbStack.getPlatformVariant()))
                .networkConnector();
        SubnetSelectionParameters build = SubnetSelectionParameters
                .builder()
                .withHa(dbStack.isHa())
                .withPreferPrivateIfExist()
                .build();
        SubnetSelectionResult subnetSelectionResult = networkConnector.chooseSubnets(subnetMetas, build);
        if (subnetSelectionResult.hasError()) {
            throw new BadRequestException(subnetSelectionResult.getErrorMessage());
        }
        return subnetSelectionResult.getResult();
    }

    public List<CloudSubnet> chooseSubnetForPrivateEndpoint(Collection<CloudSubnet> subnetMetas, DBStack dbStack, boolean existingNetwork) {
        NetworkConnector networkConnector = cloudPlatformConnectors.get(new CloudPlatformVariant(dbStack.getCloudPlatform(), dbStack.getPlatformVariant()))
                .networkConnector();
        List<CloudSubnet> suitableCloudSubnets = networkConnector.chooseSubnetsForPrivateEndpoint(subnetMetas, existingNetwork).getResult();
        LOGGER.debug("Subnets suitable for private endpoint: {}", suitableCloudSubnets);
        return suitableCloudSubnets;
    }
}
