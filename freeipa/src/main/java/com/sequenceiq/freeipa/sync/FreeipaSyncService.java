package com.sequenceiq.freeipa.sync;

import static com.sequenceiq.cloudbreak.util.Benchmark.checkedMeasure;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.security.InternalCrnBuilder;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.logger.MdcContext;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.DetailedStackStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceStatus;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaService;
import com.sequenceiq.freeipa.service.stack.StackService;
import com.sequenceiq.freeipa.service.stack.StackUpdater;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

@Component
public class FreeipaSyncService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeipaSyncService.class);

    @Inject
    private FreeipaChecker freeipaChecker;

    @Inject
    private ProviderChecker providerChecker;

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaService freeIpaService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private ThreadPoolExecutor executorService;

    @Inject
    private StackUpdater stackUpdater;

    @Value("${freeipa.autosync.enabled:true}")
    private boolean enabled;

    @Value("${freeipa.autosync.update.status:true}")
    private boolean updateStatus;

    @PostConstruct
    void logEnablement() {
        LOGGER.info("Auto sync is {}", enabled ? "enabled" : "disabled");
        LOGGER.info("Status update is {} by auto sync ", updateStatus ? "enabled" : "disabled");
    }

    public void sync() {
        if (!enabled) {
            return;
        }
        checkedMeasure(() -> {
            var list = new ArrayList<Future>();
            List<Stack> allRunning = checkedMeasure(() -> stackService.findAllForAutoSync(), LOGGER, ":::Auto sync::: stacks are fetched from db in {}ms");
            for (Stack stack : allRunning) {
                list.add(executorService.submit(() -> {
                    prepareMdcContextWithStack(stack);
                    syncAStack(stack);
                    MDCBuilder.cleanupMdc();
                }));
            }
            waitForFinish(list);
            return null;
        }, LOGGER, ":::Auto sync measure::: full sync in {}ms");
    }

    private void prepareMdcContextWithStack(Stack stack) {
        MdcContext.builder()
                .resourceCrn(stack.getResourceCrn())
                .resourceName(stack.getName())
                .resourceType("STACK")
                .environmentCrn(stack.getEnvironmentCrn())
                .buildMdc();
    }

    private void waitForFinish(ArrayList<Future> list) {
        LOGGER.info(":::Auto sync::: wait for finish: {}", list.size());
        list.forEach(l -> {
            try {
                l.get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(":::Auto sync::: " + e.getMessage(), e);
            }
        });
    }

    private void syncAStack(Stack stack) {
        try {
            checkedMeasure(() -> {
                Crn internalCrnForService = new InternalCrnBuilder(Crn.Service.IAM).getInternalCrnForService();
                ThreadBasedUserCrnProvider.doAs(internalCrnForService.toString(), () -> {
                    Set<InstanceMetaData> notTerminatedForStack = instanceMetaDataService.findNotTerminatedForStack(stack.getId());
                    Set<InstanceMetaData> checkableInstances = notTerminatedForStack.stream().filter(i -> !i.isDeletedOnProvider())
                            .collect(Collectors.toSet());

                    int alreadyDeletedCount = notTerminatedForStack.size() - checkableInstances.size();
                    if (alreadyDeletedCount > 0) {
                        LOGGER.info(":::Auto sync::: Count of already in deleted on provider side state: {}", alreadyDeletedCount);
                    }
                    if (!checkableInstances.isEmpty()) {
                        SyncResult syncResult = freeipaChecker.getStatus(stack, checkableInstances);
                        List<ProviderSyncResult> results = providerChecker.updateAndGetStatuses(stack, checkableInstances);
                        updateStackStatus(stack, syncResult, results);
                    }
                });
                return null;
            }, LOGGER, ":::Auto sync::: freeipa stack sync in {}ms");
        } catch (Exception e) {
            LOGGER.info(":::Auto sync::: Error occurred during freeipa sync: {}", e.getMessage(), e);
        }
    }

    private void updateStackStatus(Stack stack, SyncResult result, List<ProviderSyncResult> providerSyncResults) {
        DetailedStackStatus status = getStackStatus(providerSyncResults, result);
        if (status != stack.getStackStatus().getDetailedStackStatus()) {
            if (updateStatus) {
                stackUpdater.updateStackStatus(stack, status, result.getMessage());
            } else {
                LOGGER.info(":::Auto sync::: The stack status would be had to update from {} to {}",
                        stack.getStackStatus().getDetailedStackStatus(), status);
            }
        }
    }

    private DetailedStackStatus getStackStatus(List<ProviderSyncResult> providerSyncResults, SyncResult result) {
        if (result.getStatus() == DetailedStackStatus.PROVISIONED) {
            return DetailedStackStatus.PROVISIONED;
        }
        if (providerSyncResults.stream().allMatch(r -> r.getStatus() == InstanceStatus.STOPPED)) {
            return DetailedStackStatus.STOPPED;
        }
        if (providerSyncResults.stream().anyMatch(r -> r.getStatus() == InstanceStatus.STOPPED)) {
            return DetailedStackStatus.PROVISIONED;
        }
        if (providerSyncResults.stream().allMatch(r -> r.getStatus() == InstanceStatus.DELETED_ON_PROVIDER_SIDE)) {
            return DetailedStackStatus.DELETED_ON_PROVIDER_SIDE;
        }
        if (providerSyncResults.stream().anyMatch(r -> r.getStatus() == InstanceStatus.DELETED_ON_PROVIDER_SIDE)) {
            return DetailedStackStatus.PROVISIONED;
        }
        return result.getStatus();
    }
}
