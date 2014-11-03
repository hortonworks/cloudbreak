package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ConcurrentTaskExecutor;
import org.springframework.stereotype.Service;

import com.google.common.base.Optional;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.DetailedStackDescription;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.StackDescription;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.connector.CloudPlatformConnector;
import com.sequenceiq.cloudbreak.service.stack.resource.DescribeContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

@Service
public class DescribeContext {

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkResourceBuilders;

    @javax.annotation.Resource
    private ConcurrentTaskExecutor resourceBuilderExecutor;

    @javax.annotation.Resource
    private Map<CloudPlatform, ResourceBuilderInit> resourceBuilderInits;

    @javax.annotation.Resource
    private Map<CloudPlatform, CloudPlatformConnector> cloudPlatformConnectors;

    @Autowired
    private StackRepository stackRepository;

    public StackDescription describeStackWithResources(Stack actualStack) {
        Stack stack = stackRepository.findById(actualStack.getId());
        Credential credential = stack.getCredential();
        final CloudPlatform cloudPlatform = credential.cloudPlatform();
        if (cloudPlatform.isWithTemplate()) {
            return cloudPlatformConnectors.get(cloudPlatform).describeStackWithResources(stack, stack.getCredential());
        } else {
            try {
                DetailedStackDescription dSD = new DetailedStackDescription();
                ResourceBuilderInit resourceBuilderInit = resourceBuilderInits.get(cloudPlatform);
                final DescribeContextObject describeContextObject = resourceBuilderInit.describeInit(stack);
                for (ResourceBuilder resourceBuilder : networkResourceBuilders.get(cloudPlatform)) {
                    for (Resource resource : stack.getResourcesByType(resourceBuilder.resourceType())) {
                        Optional<String> describe = resourceBuilder.describe(resource, describeContextObject);
                        if (describe.isPresent()) {
                            dSD.getResources().add(describe.get());
                        }
                    }
                }
                for (final ResourceBuilder resourceBuilder : instanceResourceBuilders.get(cloudPlatform)) {
                    List<Future<Optional<String>>> futures = new ArrayList<>();
                    for (final Resource resource : stack.getResourcesByType(resourceBuilder.resourceType())) {
                        Future<Optional<String>> submit = resourceBuilderExecutor.submit(new Callable<Optional<String>>() {
                            @Override
                            public Optional<String> call() throws Exception {
                                return resourceBuilder.describe(resource, describeContextObject);
                            }
                        });
                        futures.add(submit);
                    }
                    for (Future<Optional<String>> future : futures) {
                        if (future.get().isPresent()) {
                            dSD.getResources().add(future.get().get());
                        }
                    }
                }
                return dSD;
            } catch (Exception ex) {
                throw new InternalServerException(String.format("Stack describe problem on {} stack", stack.getId()), ex);
            }
        }
    }
}
