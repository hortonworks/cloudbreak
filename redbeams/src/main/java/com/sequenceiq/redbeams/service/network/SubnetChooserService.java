package com.sequenceiq.redbeams.service.network;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.NetworkConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionParameters;
import com.sequenceiq.cloudbreak.cloud.model.SubnetSelectionResult;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.exception.BadRequestException;

@Service
public class SubnetChooserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubnetChooserService.class);

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Inject
    private EntitlementService entitlementService;

    public List<CloudSubnet> chooseSubnets(List<CloudSubnet> subnetMetas, DBStack dbStack) {
        boolean internalTenant = entitlementService.internalTenant(dbStack.getOwnerCrn().toString(), dbStack.getAccountId());
        SubnetSelectionParameters build = SubnetSelectionParameters
                .builder()
                .withHa(dbStack.isHa())
                .withPreferPrivateIfExist()
                .withIsInternalTenant(internalTenant)
                .build();
        NetworkConnector networkConnector = cloudPlatformConnectors.get(new CloudPlatformVariant(dbStack.getCloudPlatform(), dbStack.getPlatformVariant()))
                .networkConnector();
        SubnetSelectionResult subnetSelectionResult = networkConnector.chooseSubnets(subnetMetas, build);
        if (subnetSelectionResult.hasError()) {
            throw new BadRequestException(subnetSelectionResult.getErrorMessage());
        }
        return subnetSelectionResult.getResult();
    }
}
