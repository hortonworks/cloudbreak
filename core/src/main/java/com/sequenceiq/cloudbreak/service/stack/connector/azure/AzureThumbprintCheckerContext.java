package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class AzureThumbprintCheckerContext extends StackContext {
    private ObjectMapper mapper;
    private Resource resource;
    private Map<String, String> props = new HashMap<>();

    public AzureThumbprintCheckerContext(Stack stack, ObjectMapper mapper, Resource resource, Map<String, String> props) {
        super(stack);
        this.mapper = mapper;
        this.resource = resource;
        this.props = props;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public Resource getResource() {
        return resource;
    }

    public Map<String, String> getProps() {
        return props;
    }
}
