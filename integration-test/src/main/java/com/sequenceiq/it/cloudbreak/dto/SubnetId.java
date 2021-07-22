package com.sequenceiq.it.cloudbreak.dto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SubnetId {
    private List<Integer> listOfIntegers;

    private List<String> listOfSubnets;

    private SubnetValues value;

    private SubnetId(List<Integer> listOfIntegers, List<String> listOfSubnets, SubnetValues value) {
        this.listOfIntegers = listOfIntegers;
        this.listOfSubnets = listOfSubnets;
        this.value = value;
    }

    public static SubnetId ordinals(Integer... i) {
        return new SubnetId(Arrays.asList(i), null, SubnetValues.CUSTOM);
    }

    public static SubnetId ids(String... str) {
        return new SubnetId(null, Arrays.asList(str), SubnetValues.CUSTOM);
    }

    public static SubnetId all() {
        return new SubnetId(null, null, SubnetValues.ALL);
    }

    public static SubnetId none() {
        return new SubnetId(null, null, SubnetValues.NONE);
    }

    public List<String> collectSubnets(List<String> subnetsFromConfig) {
        switch (value) {
            case ALL:
                return subnetsFromConfig;
            case NONE:
                return List.of();
            default:
                if (listOfSubnets != null) {
                    return listOfSubnets;
                }
                return listOfIntegers.stream().filter(i -> i < subnetsFromConfig.size()).map(i ->
                        subnetsFromConfig.get(i % subnetsFromConfig.size())
                ).collect(Collectors.toList());
        }
    }

    private enum SubnetValues {
        ALL,
        NONE,
        CUSTOM;
    }
}
