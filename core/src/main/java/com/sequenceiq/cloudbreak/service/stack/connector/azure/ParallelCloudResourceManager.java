package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.common.type.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackUpdater;
import com.sequenceiq.cloudbreak.service.stack.flow.FutureResult;
import com.sequenceiq.cloudbreak.service.stack.flow.ResourceRequestResult;
import com.sequenceiq.cloudbreak.service.stack.resource.DeleteContextObject;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilderInit;

@Service
// TODO Have to be removed when the termination of the old version of azure clusters won't be supported anymore
public class ParallelCloudResourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParallelCloudResourceManager.class);

    @Inject
    private AsyncTaskExecutor resourceBuilderExecutor;
    @Inject
    private StackUpdater stackUpdater;
    @Inject
    private ProvisionUtil provisionUtil;
    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> networkBuilders;
    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceBuilders;

    public void terminateResources(final Stack stack, ResourceBuilderInit resourceBuilderInit) {
        try {
            final Map<String, String> mdcCtxMap = MDC.getCopyOfContextMap();
            final CloudPlatform cloudPlatform = stack.cloudPlatform();
            final DeleteContextObject dCO = resourceBuilderInit.deleteInit(stack);
            List<Future<ResourceRequestResult>> futures = new ArrayList<>();
            List<ResourceBuilder> resourceBuilders = instanceBuilders.get(cloudPlatform);
            for (int i = resourceBuilders.size() - 1; i >= 0; i--) {
                final ResourceBuilder resourceBuilder = resourceBuilders.get(i);
                List<Resource> resourceByType = stack.getResourcesByType(resourceBuilder.resourceType());
                for (final Resource resource : resourceByType) {
                    Future<ResourceRequestResult> submit = resourceBuilderExecutor.submit(new Callable<ResourceRequestResult>() {
                        @Override
                        public ResourceRequestResult call() throws Exception {
                            try {
                                MDC.setContextMap(mdcCtxMap);
                                resourceBuilder.delete(resource, dCO, stack.getRegion());
                                stackUpdater.removeStackResources(Arrays.asList(resource));
                                return ResourceRequestResult.ResourceRequestResultBuilder.builder()
                                        .withFutureResult(FutureResult.SUCCESS)
                                        .withInstanceGroup(stack.getInstanceGroupByInstanceGroupName(resource.getInstanceGroup()))
                                        .build();
                            } catch (Exception ex) {
                                return ResourceRequestResult.ResourceRequestResultBuilder.builder()
                                        .withException(ex)
                                        .withFutureResult(FutureResult.FAILED)
                                        .withInstanceGroup(stack.getInstanceGroupByInstanceGroupName(resource.getInstanceGroup()))
                                        .build();
                            }
                        }
                    });
                    futures.add(submit);
                    if (provisionUtil.isRequestFull(stack, futures.size() + 1)) {
                        Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(futures);
                        checkErrorOccurred(result);
                        futures = new ArrayList<>();
                    }
                }
            }
            Map<FutureResult, List<ResourceRequestResult>> result = provisionUtil.waitForRequestToFinish(futures);
            checkErrorOccurred(result);
            List<ResourceBuilder> networkResourceBuilders = networkBuilders.get(cloudPlatform);
            for (int i = networkResourceBuilders.size() - 1; i >= 0; i--) {
                ResourceBuilder resourceBuilder = networkResourceBuilders.get(i);
                for (Resource resource : stack.getResourcesByType(resourceBuilder.resourceType())) {
                    resourceBuilder.delete(resource, dCO, stack.getRegion());
                    stackUpdater.removeStackResources(Arrays.asList(resource));
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while terminating stack resources. Error message: {}", e.getMessage());
            throw new CloudConnectorException(e);
        }
    }

    private void checkErrorOccurred(Map<FutureResult, List<ResourceRequestResult>> futureResultListMap) throws Exception {
        List<ResourceRequestResult> resourceRequestResults = futureResultListMap.get(FutureResult.FAILED);
        if (!resourceRequestResults.isEmpty()) {
            throw resourceRequestResults.get(0).getException().orNull();
        }
    }
}
