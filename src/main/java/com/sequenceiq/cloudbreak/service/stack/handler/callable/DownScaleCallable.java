package com.sequenceiq.cloudbreak.service.stack.handler.callable;

import java.util.concurrent.Callable;

import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.flow.FutureResult;
import com.sequenceiq.cloudbreak.service.stack.flow.ResourceRequestResult;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;

public class DownScaleCallable implements Callable<ResourceRequestResult> {

    private final ResourceBuilder resourceBuilder;
    private final Stack stack;
    private final DeleteContextObject dCO;
    private final Resource resource;

    private DownScaleCallable(ResourceBuilder resourceBuilder, Stack stack, DeleteContextObject dCO, Resource resource) {
        this.resourceBuilder = resourceBuilder;
        this.stack = stack;
        this.dCO = dCO;
        this.resource = resource;
    }

    @Override
    public ResourceRequestResult call() throws Exception {
        try {
            resourceBuilder.delete(resource, dCO, stack.getRegion());
        } catch (Exception ex) {
            return ResourceRequestResult.ResourceRequestResultBuilder.builder()
                    .withException(ex)
                    .withFutureResult(FutureResult.FAILED)
                    .withResources(resource)
                    .withInstanceGroup(stack.getInstanceGroupByInstanceGroupName(resource.getInstanceGroup()))
                    .build();
        }
        return ResourceRequestResult.ResourceRequestResultBuilder.builder()
                .withFutureResult(FutureResult.SUCCESS)
                .withInstanceGroup(stack.getInstanceGroupByInstanceGroupName(resource.getInstanceGroup()))
                .build();
    }

    public static class DownScaleCallableBuilder {

        private ResourceBuilder resourceBuilder;
        private Stack stack;
        private DeleteContextObject deleteContextObject;
        private Resource resource;

        public static DownScaleCallableBuilder builder() {
            return new DownScaleCallableBuilder();
        }

        public DownScaleCallableBuilder withResourceBuilder(ResourceBuilder resourceBuilder) {
            this.resourceBuilder = resourceBuilder;
            return this;
        }

        public DownScaleCallableBuilder withStack(Stack stack) {
            this.stack = stack;
            return this;
        }

        public DownScaleCallableBuilder withResource(Resource resource) {
            this.resource = resource;
            return this;
        }

        public DownScaleCallableBuilder withDeleteContextObject(DeleteContextObject deleteContextObject) {
            this.deleteContextObject = deleteContextObject;
            return this;
        }

        public DownScaleCallable build() {
            return new DownScaleCallable(resourceBuilder, stack, deleteContextObject, resource);
        }

    }
}
