package com.sequenceiq.redbeams.service.network;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.redbeams.exception.RedbeamsException;

@Service
public class SubnetChooserService {

    public List<CloudSubnet> chooseSubnetsFromDifferentAzs(List<CloudSubnet> subnetMetas) {
        if (subnetMetas.size() < 2) {
            throw new RedbeamsException("Insufficient number of subnets: at least two subnets in two different availability zones needed");
        }

        CloudSubnet firstSubnet = subnetMetas.get(0);
        int secondIndex = 1;
        CloudSubnet secondSubnet = subnetMetas.get(secondIndex);
        while (firstSubnet.getAvailabilityZone().equals(secondSubnet.getAvailabilityZone()) && secondIndex < subnetMetas.size()) {
            ++secondIndex;
        }
        if (secondIndex == subnetMetas.size()) {
            throw new RedbeamsException("All subnets in the same availability zone: at least two subnets in two different availability zones needed");
        }

        return List.of(firstSubnet, secondSubnet);
    }
}
