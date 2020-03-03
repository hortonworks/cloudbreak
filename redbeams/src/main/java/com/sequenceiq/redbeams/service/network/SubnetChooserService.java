package com.sequenceiq.redbeams.service.network;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.exception.BadRequestException;

@Service
public class SubnetChooserService {

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    public List<CloudSubnet> chooseSubnets(List<CloudSubnet> subnetMetas, CloudPlatform cloudPlatform, DBStack dbStack) {
        SubnetSelectionResult subnetSelectionResult =
                cloudPlatformConnectors.get(new CloudPlatformVariant(dbStack.getCloudPlatform(), dbStack.getPlatformVariant()))
                .networkConnector()
                .selectSubnets(
                        subnetMetas,
                        SubnetSelectionParameters.builder()
                                .withHa(dbStack.isHa())
                                .withForDatabase()
                                .build()
                );
        if (subnetSelectionResult.hasError()) {
            throw new BadRequestException(subnetSelectionResult.getErrorMessage());
        }
        return subnetSelectionResult.getResult();
    }
}
