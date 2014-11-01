package com.sequenceiq.cloudbreak.service.stack.connector.gcc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.DetailedGccStackDescription;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.DescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

@Service
public class GccConnector implements CloudPlatformConnector {

    private static final Logger LOGGER = LoggerFactory.getLogger(GccConnector.class);

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @javax.annotation.Resource
    private ConcurrentTaskExecutor resourceBuilderExecutor;

    @Override
    public StackDescription describeStackWithResources(Stack stack, Credential credential) {
        DetailedGccStackDescription detailedGccStackDescription = new DetailedGccStackDescription();
        ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(CloudPlatform.GCC);

        try {
            final DescribeContextObject dCO = resourceBuilderInit.describeInit(stack);
            for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(CloudPlatform.GCC)) {
                List<Resource> resourceByType = stack.getResourcesByType(resourceBuilder.resourceType());
                for (Resource resource : resourceByType) {
                    Optional<String> describe = resourceBuilder.describe(resource, dCO);
                    if (describe.isPresent()) {
                        detailedGccStackDescription.getResources().add(describe.get());
                    }
                }
            }
            for (final ResourceBuilder resourceBuilder : instanceResourceBuilders.get(CloudPlatform.GCC)) {
                List<Resource> resourceByType = stack.getResourcesByType(resourceBuilder.resourceType());
                List<Future<Optional<String>>> futures = new ArrayList<>();
                for (final Resource resource : resourceByType) {
                    Future<Optional<String>> submit = resourceBuilderExecutor.submit(new Callable<Optional<String>>() {
                        @Override
                        public Optional<String> call() throws Exception {
                            return resourceBuilder.describe(resource, dCO);
                        }
                    });
                    futures.add(submit);
                }
                for (Future<Optional<String>> future : futures) {
                    if (future.get().isPresent()) {
                        detailedGccStackDescription.getResources().add(future.get().get());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
        return detailedGccStackDescription;
    }

    public void rollback(Stack stack, Credential credential, Set<Resource> resourceSet) {
        ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(stack.getTemplate().cloudPlatform());
        try {
            final DeleteContextObject dCO = resourceBuilderInit.deleteInit(stack);
            for (int i = instanceResourceBuilders.get(CloudPlatform.GCC).size() - 1; i >= 0; i--) {
                List<Future<Boolean>> futures = new ArrayList<>();
                final int index = i;
                for (final Resource resource : resourceSet) {
                    if (resource.getResourceType().equals(instanceResourceBuilders.get(CloudPlatform.GCC).get(i).resourceType())) {
                        Future<Boolean> submit = resourceBuilderExecutor.submit(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return instanceResourceBuilders.get(CloudPlatform.GCC).get(index).delete(resource, dCO);
                            }
                        });
                        futures.add(submit);
                    }
                }
                for (Future<Boolean> future : futures) {
                    future.get();
                }
            }
            for (int i = instanceResourceBuilders.get(CloudPlatform.GCC).size() - 1; i >= 0; i--) {
                for (Resource resource : resourceSet) {
                    if (resource.getResourceType().equals(instanceResourceBuilders.get(CloudPlatform.GCC).get(i).resourceType())) {
                        networkResourceBuilders.get(CloudPlatform.GCC).get(i).delete(resource, dCO);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }
    }

    @Override
    public void deleteStack(Stack stack, Credential credential) {

    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCC;
    }

    @Override
    public boolean startAll(Stack stack) {
        return true;
    }

    @Override
    public boolean stopAll(Stack stack) {
        return true;
    }
}
