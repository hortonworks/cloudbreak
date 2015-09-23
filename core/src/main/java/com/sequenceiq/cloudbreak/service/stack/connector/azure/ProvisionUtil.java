package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.stack.flow.FutureResult;
import com.sequenceiq.cloudbreak.service.stack.flow.ResourceRequestResult;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;

@Component
public class ProvisionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionUtil.class);
    private static final int PARALLEL_NUMBER = 6;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceBuilders;

    public boolean isRequestFull(Stack stack, int fullIndex) {
        return fullIndex % PARALLEL_NUMBER == 0;
    }

    public boolean isRequestFullWithCloudPlatform(Stack stack, int fullIndex) {
        return (fullIndex * instanceBuilders.get(stack.cloudPlatform()).size()) % PARALLEL_NUMBER == 0;
    }

    public Map<FutureResult, List<ResourceRequestResult>> waitForRequestToFinish(List<Future<ResourceRequestResult>> futures) throws Exception {
        Map<FutureResult, List<ResourceRequestResult>> result = new HashMap<>();
        result.put(FutureResult.FAILED, new ArrayList<ResourceRequestResult>());
        result.put(FutureResult.SUCCESS, new ArrayList<ResourceRequestResult>());
        LOGGER.info("Waiting for futures to finishing.");
        for (Future<ResourceRequestResult> future : futures) {
            ResourceRequestResult resourceRequestResult = future.get();
            if (FutureResult.FAILED.equals(resourceRequestResult.getFutureResult())) {
                result.get(FutureResult.FAILED).add(resourceRequestResult);
            } else {
                result.get(FutureResult.SUCCESS).add(resourceRequestResult);
            }
        }
        LOGGER.info("All futures finished continue with the next group.");
        return result;
    }

}
