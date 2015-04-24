package com.sequenceiq.cloudbreak.controller.validation.blueprint;

import java.util.Map;

public class StackServiceComponentDescriptors {
    private Map<String, StackServiceComponentDescriptor> stackServiceComponentDescriptorMap;

    public StackServiceComponentDescriptors(Map<String, StackServiceComponentDescriptor> stackServiceComponentDescriptorMap) {
        this.stackServiceComponentDescriptorMap = stackServiceComponentDescriptorMap;
    }

    public StackServiceComponentDescriptor get(String name) {
        return stackServiceComponentDescriptorMap.get(name);
    }
}
