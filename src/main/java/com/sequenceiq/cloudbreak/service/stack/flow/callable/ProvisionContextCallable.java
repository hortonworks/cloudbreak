package com.sequenceiq.cloudbreak.service.stack.flow.callable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.flow.FutureResult;
import com.sequenceiq.cloudbreak.service.stack.flow.ResourceRequestResult;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderType;

public class ProvisionContextCallable implements Callable<ResourceRequestResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionContextCallable.class);

    private final Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;
    private final int index;
    private final Stack stack;
    private final InstanceGroup instanceGroup;
    private final ProvisionContextObject provisionContextObject;
    private final RetryingStackUpdater stackUpdater;
    private final StackRepository stackRepository;

    private ProvisionContextCallable(Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders, int index, Stack stack,
            InstanceGroup instanceGroup, ProvisionContextObject provisionContextObject, RetryingStackUpdater stackUpdater, StackRepository stackRepository) {
        this.instanceResourceBuilders = instanceResourceBuilders;
        this.index = index;
        this.stack = stack;
        this.instanceGroup = instanceGroup;
        this.provisionContextObject = provisionContextObject;
        this.stackUpdater = stackUpdater;
        this.stackRepository = stackRepository;
    }

    @Override
    public ResourceRequestResult call() {
        LOGGER.info("Node {}. creation starting", index);
        List<Resource> resources = new ArrayList<>();
        List<Resource> buildResources = new ArrayList<>();
        try {
            for (final ResourceBuilder resourceBuilder : instanceResourceBuilders.get(stack.cloudPlatform())) {
                ResourceBuilderType resourceBuilderType = resourceBuilder.resourceBuilderType();
                buildResources = resourceBuilder.buildResources(provisionContextObject, index, resources, Optional.of(instanceGroup));
                CreateResourceRequest createResourceRequest =
                        resourceBuilder.buildCreateRequest(provisionContextObject, resources, buildResources, index, Optional.of(instanceGroup));
                stackUpdater.addStackResources(stack.getId(), createResourceRequest.getBuildableResources());
                if (stackRepository.findById(stack.getId()).isStackInDeletionPhase()) {
                    break;
                }
                resourceBuilder.create(createResourceRequest, stack.getRegion());
                resources.addAll(createResourceRequest.getBuildableResources());
                LOGGER.info("Node {}. creation in progress resource {} creation finished.", index, resourceBuilderType);
            }
        } catch (Exception ex) {
            return ResourceRequestResult.ResourceRequestResultBuilder.builder()
                    .withException(ex)
                    .withResources(buildResources)
                    .withFutureResult(FutureResult.FAILED)
                    .withInstanceGroup(instanceGroup)
                    .build();
        }
        return ResourceRequestResult.ResourceRequestResultBuilder.builder()
                .withFutureResult(FutureResult.SUCCESS)
                .withInstanceGroup(instanceGroup)
                .build();
    }

    public static class ProvisionContextCallableBuilder {

        private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders = new HashMap<>();
        private int index;
        private Stack stack;
        private InstanceGroup instanceGroup;
        private ProvisionContextObject provisionContextObject;
        private RetryingStackUpdater stackUpdater;
        private StackRepository stackRepository;

        public static ProvisionContextCallableBuilder builder() {
            return new ProvisionContextCallableBuilder();
        }

        public ProvisionContextCallableBuilder withInstanceResourceBuilders(Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders) {
            this.instanceResourceBuilders = instanceResourceBuilders;
            return this;
        }

        public ProvisionContextCallableBuilder withIndex(int index) {
            this.index = index;
            return this;
        }

        public ProvisionContextCallableBuilder withStack(Stack stack) {
            this.stack = stack;
            return this;
        }

        public ProvisionContextCallableBuilder withInstanceGroup(InstanceGroup instanceGroup) {
            this.instanceGroup = instanceGroup;
            return this;
        }

        public ProvisionContextCallableBuilder withProvisionContextObject(ProvisionContextObject provisionContextObject) {
            this.provisionContextObject = provisionContextObject;
            return this;
        }

        public ProvisionContextCallableBuilder withStackUpdater(RetryingStackUpdater stackUpdater) {
            this.stackUpdater = stackUpdater;
            return this;
        }

        public ProvisionContextCallableBuilder withStackRepository(StackRepository stackRepository) {
            this.stackRepository = stackRepository;
            return this;
        }

        public ProvisionContextCallable build() {
            return new ProvisionContextCallable(instanceResourceBuilders, index, stack, instanceGroup, provisionContextObject, stackUpdater, stackRepository);
        }

    }
}
