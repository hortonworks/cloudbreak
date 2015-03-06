package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Resource;

public class ResourceRequestResult {

    private final FutureResult futureResult;
    private final InstanceGroup instanceGroup;
    private final Optional<Exception> exception;
    private final List<Resource> resources;
    private final List<Resource> builtResources;

    private ResourceRequestResult(FutureResult futureResult, InstanceGroup instanceGroup, Optional<Exception> exception,
            List<Resource> resources, List<Resource> builtResources) {
        this.futureResult = futureResult;
        this.instanceGroup = instanceGroup;
        this.exception = exception;
        this.resources = resources;
        this.builtResources = builtResources;
    }

    public List<Resource> getResources() {
        return resources;
    }

    public FutureResult getFutureResult() {
        return futureResult;
    }

    public InstanceGroup getInstanceGroup() {
        return instanceGroup;
    }

    public Optional<Exception> getException() {
        return exception;
    }

    public List<Resource> getBuiltResources() {
        return builtResources;
    }

    public static class ResourceRequestResultBuilder {

        private FutureResult futureResult;
        private InstanceGroup instanceGroup;
        private Optional<Exception> exception = Optional.absent();
        private List<Resource> resources = new ArrayList<>();
        private List<Resource> buildedResources = new ArrayList<>();

        public static ResourceRequestResultBuilder builder() {
            return new ResourceRequestResultBuilder();
        }

        public ResourceRequestResultBuilder withFutureResult(FutureResult futureResult) {
            this.futureResult = futureResult;
            return this;
        }

        public ResourceRequestResultBuilder withInstanceGroup(InstanceGroup instanceGroup) {
            this.instanceGroup = instanceGroup;
            return this;
        }

        public ResourceRequestResultBuilder withBuildedResources(List<Resource> buildedResources) {
            this.buildedResources = buildedResources;
            return this;
        }

        public ResourceRequestResultBuilder withException(Exception exception) {
            this.exception = Optional.fromNullable(exception);
            return this;
        }

        public ResourceRequestResult build() {
            return new ResourceRequestResult(futureResult, instanceGroup, exception, resources, buildedResources);
        }

        public ResourceRequestResultBuilder withResources(List<Resource> resources) {
            this.resources = resources;
            return this;
        }

        public ResourceRequestResultBuilder withResources(Resource... resources) {
            this.resources = Arrays.asList(resources);
            return this;
        }
    }

}
