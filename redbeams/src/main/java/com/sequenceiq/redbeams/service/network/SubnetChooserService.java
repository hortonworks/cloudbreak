package com.sequenceiq.redbeams.service.network;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.redbeams.exception.BadRequestException;

@Service
public class SubnetChooserService {

    public List<CloudSubnet> chooseSubnetsFromDifferentAzs(List<CloudSubnet> subnetMetas) {
        if (subnetMetas.size() < 2) {
            throw new BadRequestException("Insufficient number of subnets: at least two subnets in two different availability zones needed");
        }

        List<CloudSubnet> subnetsDistinctedByAZ = subnetMetas.stream().filter(distinctByKey(CloudSubnet::getAvailabilityZone)).collect(Collectors.toList());

        if (subnetsDistinctedByAZ.size() < 2) {
            throw new BadRequestException("All subnets in the same availability zone: at least two subnets in two different availability zones needed");
        } else {
            return List.of(subnetsDistinctedByAZ.get(0), subnetsDistinctedByAZ.get(1));
        }
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }
}
