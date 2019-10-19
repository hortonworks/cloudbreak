package com.sequenceiq.cloudbreak.cmtemplate.validation;

import java.util.Map;

public class StackServiceComponentDescriptors {

    private final Map<String, StackServiceComponentDescriptor> stackServiceComponentDescriptorMap;

    public StackServiceComponentDescriptors(Map<String, StackServiceComponentDescriptor> stackServiceComponentDescriptorMap) {
        this.stackServiceComponentDescriptorMap = stackServiceComponentDescriptorMap;
    }

    public StackServiceComponentDescriptor get(String name) {
        return stackServiceComponentDescriptorMap.get(name);
    }
}
