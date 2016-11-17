package com.sequenceiq.cloudbreak.service.usages;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.api.model.Status;
import com.sequenceiq.cloudbreak.api.model.UsageStatus;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Service
public class UsageService {

    @Inject
    private UsageTimeService usageTimeService;

    @Inject
    private UsagePriceService usagePriceService;

    @Inject
    private UsageGeneratorService usageGeneratorService;

    @Inject
    private CloudbreakUsageRepository usageRepository;

    @Inject
    private StackRepository stackRepository;

    public void openUsagesForStack(Stack stack) {
        LocalDateTime ldt = LocalDateTime.now();
        List<CloudbreakUsage> usages = Lists.newArrayList();
        for (InstanceGroup ig : stack.getInstanceGroups()) {
            Template template = ig.getTemplate();
            String instanceType = template.getInstanceType();
            String groupName = ig.getGroupName();
            Integer instanceNum = ig.getNodeCount();
            usages.add(usageGeneratorService.openNewUsage(stack, instanceType, instanceNum, groupName, ldt));
        }
        usageRepository.save(usages);
    }

    public void closeUsagesForStack(Stack stack) {
        List<CloudbreakUsage> usages = usageRepository.findOpensForStack(stack.getId());
        for (CloudbreakUsage usage : usages) {
            closeUsage(usage);
            usageRepository.save(usage);
        }
    }

    public void stopUsagesForStack(Stack stack) {
        List<CloudbreakUsage> usages = usageRepository.findOpensForStack(stack.getId());
        for (CloudbreakUsage usage : usages) {
            usage.setStatus(UsageStatus.STOPPED);
            Duration newDuration = usageTimeService.calculateNewDuration(usage);
            usage.setInstanceHours(usageTimeService.convertToInstanceHours(newDuration));
            usage.setDuration(newDuration.toString());
            usage.setPeriodStarted(null);
            usageRepository.save(usage);
        }
    }

    public void startUsagesForStack(Stack stack) {
        List<CloudbreakUsage> usages = usageRepository.findStoppedForStack(stack.getId());
        for (CloudbreakUsage usage : usages) {
            usage.setStatus(UsageStatus.OPEN);
            usage.setPeriodStarted(Date.from(ZonedDateTime.now().toInstant()));
            usageRepository.save(usage);
        }
    }

    public void scaleUsagesForStack(Long stackId, String instanceGroupName, int nodeCount) {
        CloudbreakUsage usage = usageRepository.getOpenUsageByStackAndGroupName(stackId, instanceGroupName);
        if (usage != null) {
            Duration newDuration = usageTimeService.calculateNewDuration(usage);
            usage.setInstanceHours(usageTimeService.convertToInstanceHours(newDuration));
            usage.setDuration(newDuration.toString());
            usage.setPeriodStarted(Date.from(ZonedDateTime.now().toInstant()));
            usage.setInstanceNum(nodeCount);
            usageRepository.save(usage);
        }
    }

    @Scheduled(cron = "0 01 0 * * *")
    public void fixUsages() {
        reopenOldUsages();
        openNewIfNotFound();
    }

    private void reopenOldUsages() {
        List<CloudbreakUsage> usages = usageRepository.findAllOpenAndStopped(Date.from(ZonedDateTime.now().minusDays(1).toInstant()));
        List<CloudbreakUsage> newUsages = Lists.newArrayList();
        for (CloudbreakUsage usage : usages) {
            newUsages.addAll(usageGeneratorService.createClosedUsagesUntilNow(usage));
            newUsages.add(closeUsageIfStackKilled(usageGeneratorService.createNewFromUsage(usage)));
            closeUsage(usage);
            usageRepository.save(usage);
        }
        usageRepository.save(newUsages);
    }

    private CloudbreakUsage closeUsageIfStackKilled(CloudbreakUsage usage) {
        Stack s = stackRepository.findById(usage.getStackId());
        if (s == null || s.getStatus() == Status.DELETE_COMPLETED) {
            return closeUsage(usage);
        }
        return usage;
    }

    private void openNewIfNotFound() {
        List<CloudbreakUsage> usages = usageRepository.findAllOpenAndStopped(Date.from(ZonedDateTime.now().toInstant()));
        Set<Long> stackIdsForOpenUsages = usages.stream().map(CloudbreakUsage::getStackId).collect(Collectors.toSet());
        List<Stack> stacks = stackRepository.findAllAlive();
        for (Stack stack : stacks) {
            if (!stackIdsForOpenUsages.contains(stack.getId())) {
                openUsagesForStack(stackRepository.findById(stack.getId()));
            }
        }
    }

    private CloudbreakUsage closeUsage(CloudbreakUsage usage) {
        if (usage.getStatus() == UsageStatus.OPEN) {
            Duration newDuration = usageTimeService.calculateNewDuration(usage);
            usage.setInstanceHours(usageTimeService.convertToInstanceHours(newDuration));
            usage.setDuration(newDuration.toString());
            usage.setCosts(usagePriceService.calculateCostOfUsage(usage));
        }
        usage.setStatus(UsageStatus.CLOSED);
        return usage;
    }

}
