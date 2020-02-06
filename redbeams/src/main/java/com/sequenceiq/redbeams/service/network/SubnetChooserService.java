package com.sequenceiq.redbeams.service.network;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.exception.BadRequestException;
import com.sequenceiq.redbeams.exception.RedbeamsException;

@Service
public class SubnetChooserService {

    @Inject
    private AwsSubnetChooser awsSubnetChooser;

    public List<CloudSubnet> chooseSubnets(List<CloudSubnet> subnetMetas, CloudPlatform cloudPlatform, Map<String, String> dbParameters) {
        switch (cloudPlatform) {
            case AWS:
                return awsSubnetChooser.chooseSubnets(subnetMetas, dbParameters);
            case AZURE:
                return chooseSubnetsAzure(subnetMetas);
            case MOCK:
                return chooseSubnetsMock(subnetMetas);
            default:
                throw new RedbeamsException(String.format("Support for cloud platform %s not yet added", cloudPlatform.name()));
        }
    }

    private List<CloudSubnet> chooseSubnetsAzure(List<CloudSubnet> subnetMetas) {
        if (subnetMetas.isEmpty()) {
            throw new BadRequestException("Insufficient number of subnets: at least one subnet needed");
        }
        return subnetMetas.subList(0, 1);
    }

    private List<CloudSubnet> chooseSubnetsMock(List<CloudSubnet> subnetMetas) {
        return subnetMetas;
    }
}
