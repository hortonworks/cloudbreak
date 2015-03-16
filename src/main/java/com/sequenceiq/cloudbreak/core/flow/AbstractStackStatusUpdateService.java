package com.sequenceiq.cloudbreak.core.flow;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;
import com.sequenceiq.cloudbreak.service.stack.resource.StartStopContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.AsyncTaskExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public abstract class AbstractStackStatusUpdateService {

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders;

    @Autowired
    private AsyncTaskExecutor resourceBuilderExecutor;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    protected boolean startStopResources(CloudPlatform cloudPlatform, Stack stack, final boolean start) {
        boolean finished = true;
        try {
            ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
            final StartStopContextObject sSCO = resourceBuilderInit.startStopInit(stack);

            for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(cloudPlatform)) {
                for (Resource resource : stack.getResourcesByType(resourceBuilder.resourceType())) {
                    if (start) {
                        resourceBuilder.start(sSCO, resource, stack.getRegion());
                    } else {
                        resourceBuilder.stop(sSCO, resource, stack.getRegion());
                    }
                }
            }
            List<Future<Boolean>> futures = new ArrayList<>();
            for (final ResourceBuilder resourceBuilder : instanceResourceBuilders.get(cloudPlatform)) {
                List<Resource> resourceByType = stack.getResourcesByType(resourceBuilder.resourceType());
                for (final Resource resource : resourceByType) {
                    final Stack finalStack = stack;
                    Future<Boolean> submit = resourceBuilderExecutor.submit(new Callable<Boolean>() {
                        @Override
                        public Boolean call() throws Exception {
                            if (start) {
                                return resourceBuilder.start(sSCO, resource, finalStack.getRegion());
                            } else {
                                return resourceBuilder.stop(sSCO, resource, finalStack.getRegion());
                            }
                        }
                    });
                    futures.add(submit);
                }
            }
            for (Future<Boolean> future : futures) {
                if (!future.get()) {
                    finished = false;
                }
            }
        } catch (Exception ex) {
            finished = false;
        }
        return finished;
    }
}
