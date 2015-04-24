package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.stack.resource.ResourceBuilder;

@Component
public class ProvisionUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionUtil.class);

    @Autowired
    private StackRepository stackRepository;

    @javax.annotation.Resource
    private Map<CloudPlatform, List<ResourceBuilder>> instanceResourceBuilders;

    public boolean isRequestFull(Stack stack, int fullIndex) {
        return fullIndex % stack.cloudPlatform().parallelNumber() == 0;
    }

    public boolean isRequestFullWithCloudPlatform(Stack stack, int fullIndex) {
        return (fullIndex * instanceResourceBuilders.get(stack.cloudPlatform()).size()) % stack.cloudPlatform().parallelNumber() == 0;
    }

    public Map<FutureResult, List<ResourceRequestResult>> waitForRequestToFinish(Long stackId, List<Future<ResourceRequestResult>> futures)
            throws Exception {
        Stack stack = stackRepository.findOneWithLists(stackId);
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
