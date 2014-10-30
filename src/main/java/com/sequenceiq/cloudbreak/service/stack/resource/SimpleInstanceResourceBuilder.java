package com.sequenceiq.cloudbreak.service.stack.resource;

public abstract class SimpleInstanceResourceBuilder<P extends ProvisionContextObject, D extends DeleteContextObject, T extends DescribeContextObject>
        implements InstanceResourceBuilder<P, D, T> {

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.INSTANCE_RESOURCE;
    }
}
