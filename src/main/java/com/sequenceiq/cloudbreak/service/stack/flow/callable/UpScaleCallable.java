package com.sequenceiq.cloudbreak.service.stack.flow.callable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.RetryingStackUpdater;
import com.sequenceiq.cloudbreak.service.stack.flow.FutureResult;
import com.sequenceiq.cloudbreak.service.stack.flow.ResourceRequestResult;
import com.sequenceiq.cloudbreak.service.stack.resource.CreateResourceRequest;
import com.sequenceiq.cloudbreak.service.stack.resource.ProvisionContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;

public class UpScaleCallable implements Callable<ResourceRequestResult> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpScaleCallable.class);

    private final Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;
    private final int index;
    private final Stack stack;
    private final ProvisionContextObject provisionContextObject;
    private final RetryingStackUpdater stackUpdater;
    private final String instanceGroup;
    private final Optional<String> userData;
    private final Map<String, String> mdcCtxMap;

    private UpScaleCallable(Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders, int index, Stack stack, String instanceGroup,
            ProvisionContextObject provisionContextObject, RetryingStackUpdater stackUpdater, Optional<String> userData, Map<String, String> mdcCtxMap) {
        this.instanceResourceBuilders = instanceResourceBuilders;
        this.index = index;
        this.stack = stack;
        this.instanceGroup = instanceGroup;
        this.provisionContextObject = provisionContextObject;
        this.stackUpdater = stackUpdater;
        this.userData = userData;
        this.mdcCtxMap = mdcCtxMap;
    }

    @Override
    public ResourceRequestResult call() throws Exception {
        List<Resource> resources = new ArrayList<>();
        MDC.setContextMap(mdcCtxMap);
        try {
            for (final ResourceBuilder resourceBuilder : instanceResourceBuilders.get(stack.cloudPlatform())) {
                CreateResourceRequest createResourceRequest =
                        resourceBuilder.buildCreateRequest(provisionContextObject,
                                resources,
                                resourceBuilder.buildResources(provisionContextObject, index, resources,
                                        Optional.of(stack.getInstanceGroupByInstanceGroupName(instanceGroup))),
                                index,
                                Optional.of(stack.getInstanceGroupByInstanceGroupName(instanceGroup)),
                                userData);
                stackUpdater.addStackResources(stack.getId(), createResourceRequest.getBuildableResources());
                resources.addAll(createResourceRequest.getBuildableResources());
                resourceBuilder.create(createResourceRequest, stack.getRegion());
            }
        } catch (Exception ex) {
            return ResourceRequestResult.ResourceRequestResultBuilder.builder()
                    .withException(ex)
                    .withResources(resources)
                    .withFutureResult(FutureResult.FAILED)
                    .withInstanceGroup(stack.getInstanceGroupByInstanceGroupName(instanceGroup))
                    .build();
        }
        return ResourceRequestResult.ResourceRequestResultBuilder.builder()
                .withFutureResult(FutureResult.SUCCESS)
                .withBuildedResources(resources)
                .withInstanceGroup(stack.getInstanceGroupByInstanceGroupName(instanceGroup))
                .build();
    }

    public static class UpScaleCallableBuilder {

        private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders = new HashMap<>();
        private int index;
        private Stack stack;
        private String instanceGroup;
        private ProvisionContextObject provisionContextObject;
        private RetryingStackUpdater stackUpdater;
        private String userData;
        private Map<String, String> mdcCtxMap;

        public static UpScaleCallableBuilder builder() {
            return new UpScaleCallableBuilder();
        }

        public UpScaleCallableBuilder withInstanceResourceBuilders(Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders) {
            this.instanceResourceBuilders = instanceResourceBuilders;
            return this;
        }

        public UpScaleCallableBuilder withIndex(int index) {
            this.index = index;
            return this;
        }

        public UpScaleCallableBuilder withStack(Stack stack) {
            this.stack = stack;
            return this;
        }

        public UpScaleCallableBuilder withInstanceGroup(String instanceGroup) {
            this.instanceGroup = instanceGroup;
            return this;
        }

        public UpScaleCallableBuilder withProvisionContextObject(ProvisionContextObject provisionContextObject) {
            this.provisionContextObject = provisionContextObject;
            return this;
        }

        public UpScaleCallableBuilder withStackUpdater(RetryingStackUpdater stackUpdater) {
            this.stackUpdater = stackUpdater;
            return this;
        }

        public UpScaleCallableBuilder withUserData(String userData) {
            this.userData = userData;
            return this;
        }

        public UpScaleCallableBuilder withMdcContextMap(Map<String, String> mdcCtxMap) {
            this.mdcCtxMap = mdcCtxMap;
            return this;
        }

        public UpScaleCallable build() {
            return new UpScaleCallable(instanceResourceBuilders, index, stack, instanceGroup, provisionContextObject, stackUpdater,
                    userData == null ? Optional.<String>absent() : Optional.fromNullable(userData), mdcCtxMap);
        }

    }
}
