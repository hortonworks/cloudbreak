package com.sequenceiq.cloudbreak.service.stack.resource;

public abstract class SimpleNetworkResourceBuilder<P extends ProvisionContextObject, D extends DeleteContextObject, T extends DescribeContextObject>
        implements NetworkResourceBuilder<P, D, T> {

    @Override
    public ResourceBuilderType resourceBuilderType() {
        return ResourceBuilderType.NETWORK_RESOURCE;
    }
}
