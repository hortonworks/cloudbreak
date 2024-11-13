package com.sequenceiq.freeipa.flow.stack.start;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dyngr.core.AttemptMaker;
import com.dyngr.core.AttemptResult;
import com.dyngr.core.AttemptResults;
import com.sequenceiq.cloudbreak.client.RPCResponse;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.stack.FreeIpaInstanceHealthDetailsService;

public class OneFreeIpaReachableAttempt implements AttemptMaker<Void> {
    private static final Logger LOGGER = LoggerFactory.getLogger(OneFreeIpaReachableAttempt.class);

    private final FreeIpaInstanceHealthDetailsService freeIpaInstanceHealthDetailsService;

    private final Stack stack;

    private final Set<InstanceMetaData> instanceMetaDataSet;

    private final int consecutiveSuccess;

    private final Map<String, Integer> consecutiveSuccessCounter;

    public OneFreeIpaReachableAttempt(FreeIpaInstanceHealthDetailsService freeIpaInstanceHealthDetailsService, Stack stack,
            Set<InstanceMetaData> instanceMetaDataSet, int consecutiveSuccess) {
        this.freeIpaInstanceHealthDetailsService = freeIpaInstanceHealthDetailsService;
        this.stack = stack;
        this.instanceMetaDataSet = instanceMetaDataSet;
        this.consecutiveSuccess = consecutiveSuccess;
        consecutiveSuccessCounter = new HashMap<>(instanceMetaDataSet.size());
    }

    @Override
    public AttemptResult<Void> process() {
        Optional<PollGroup> pollGroup = Optional.ofNullable(InMemoryStateStore.getStack(stack.getId()));
        if (pollGroup.isEmpty() || PollGroup.CANCELLED.equals(pollGroup.get())) {
            LOGGER.info("FreeIpa polling cancelled in inMemory store.");
            return AttemptResults.breakFor("FreeIpa polling cancelled in inMemory store");
        }
        for (InstanceMetaData instanceMetaData : instanceMetaDataSet) {
            try {
                RPCResponse<Boolean> result = checkedMeasure(() -> freeIpaInstanceHealthDetailsService.checkFreeIpaHealth(stack, instanceMetaData), LOGGER,
                        ":::Freeipa start::: FreeIPA health check ran in {}ms");
                if (result.getResult()) {
                    Integer currentSuccessCount = consecutiveSuccessCounter.compute(instanceMetaData.getInstanceId(),
                            (key, value) -> Optional.ofNullable(value).orElse(0) + 1);
                    LOGGER.debug("Freeipa services are available and freeipa is working (on instance: {}). Consecutive success [{}/{}]",
                            instanceMetaData.getInstanceId(), currentSuccessCount, consecutiveSuccess);
                    if (currentSuccessCount >= consecutiveSuccess) {
                        return AttemptResults.finishWith(null);
                    }
                } else {
                    LOGGER.debug("Freeipa services are available, but freeipa is not working (on instance: {})", instanceMetaData.getInstanceId());
                    consecutiveSuccessCounter.computeIfPresent(instanceMetaData.getInstanceId(), (key, value) -> 0);
                }
            } catch (Exception e) {
                LOGGER.debug("Freeipa services have not been available yet (on instance: {})", instanceMetaData.getInstanceId());
                consecutiveSuccessCounter.computeIfPresent(instanceMetaData.getInstanceId(), (key, value) -> 0);
            }
        }
        return AttemptResults.justContinue();
    }
}
