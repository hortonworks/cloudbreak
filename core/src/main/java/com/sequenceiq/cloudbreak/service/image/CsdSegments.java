package com.sequenceiq.cloudbreak.service.image;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public class CsdSegments {

    private final Set<String> componentList;

    private CsdSegments(String... components) {
        this.componentList = Arrays.stream(components).collect(Collectors.toSet());
    }

    public Set<String> getComponentList() {
        return componentList;
    }

    public static CsdSegments segments(String... components) {
        return new CsdSegments(components);
    }
}
