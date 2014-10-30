package com.sequenceiq.cloudbreak.service.stack.resource;

public interface InstanceResourceBuilder
        <P extends ProvisionContextObject, D extends DeleteContextObject, DCO extends DescribeContextObject> extends ResourceBuilder<P, D, DCO> {

}
