package com.sequenceiq.cloudbreak.domain.stack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.domain.Resource;

public class ResourceUtil {

    private ResourceUtil() {
    }

    // CB-15963 - this is to ensure that we consider only the latest VolumeSet for each instance if there are more than 1
    public static List<Resource> getLatestResourceByInstanceId(List<Resource> resourceList) {
        return new ArrayList<>(resourceList.stream()
                .collect(Collectors.toMap(Resource::getInstanceId, Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(Resource::getId))))
                .values());
    }
}
