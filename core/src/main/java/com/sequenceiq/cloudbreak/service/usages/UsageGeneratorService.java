package com.sequenceiq.cloudbreak.service.usages;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAccessor;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.google.api.client.util.Lists;
import com.sequenceiq.cloudbreak.api.model.UsageStatus;
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.ha.CloudbreakNodeConfig;

@Service
public class UsageGeneratorService {

    private static final long HOURS_IN_DAY = 24L;

    @Inject
    private UsagePriceService usagePriceService;

    @Inject
    private UsageTimeService usageTimeService;

    @Inject
    private CloudbreakNodeConfig cloudbreakNodeConfig;

    public CloudbreakUsage createFullClosed(CloudbreakUsage usage, Date day) {
        CloudbreakUsage newUsage = new CloudbreakUsage();
        newUsage.setStackUuid(usage.getStackUuid());
        newUsage.setParentUuid(cloudbreakNodeConfig.getInstanceUUID());
        newUsage.setProvider(usage.getProvider());
        newUsage.setRegion(usage.getRegion());
        newUsage.setAvailabilityZone(usage.getAvailabilityZone());
        newUsage.setInstanceHours(HOURS_IN_DAY * usage.getInstanceNum());
        newUsage.setDay(day);
        newUsage.setStackId(usage.getStackId());
        newUsage.setStackName(usage.getStackName());
        newUsage.setInstanceType(usage.getInstanceType());
        newUsage.setInstanceNum(usage.getInstanceNum());
        newUsage.setPeak(usage.getInstanceNum());
        newUsage.setInstanceGroup(usage.getInstanceGroup());
        newUsage.setBlueprintId(usage.getBlueprintId());
        newUsage.setBlueprintName(usage.getBlueprintName());
        newUsage.setPeriodStarted(day);
        newUsage.setDuration(Duration.of(HOURS_IN_DAY, ChronoUnit.HOURS).multipliedBy(usage.getInstanceNum()).toString());
        newUsage.setStatus(UsageStatus.CLOSED);
        newUsage.setCosts(usagePriceService.calculateCostOfUsage(newUsage));
        newUsage.setFlexId(usage.getFlexId());
        newUsage.setSmartSenseId(usage.getSmartSenseId());
        return newUsage;
    }

    public CloudbreakUsage createNewFromUsage(CloudbreakUsage usage) {
        CloudbreakUsage newUsage = new CloudbreakUsage();
        newUsage.setStackUuid(usage.getStackUuid());
        newUsage.setParentUuid(cloudbreakNodeConfig.getInstanceUUID());
        newUsage.setProvider(usage.getProvider());
        newUsage.setRegion(usage.getRegion());
        newUsage.setAvailabilityZone(usage.getAvailabilityZone());
        newUsage.setInstanceHours(0L);
        newUsage.setCosts(0.0);
        Date day = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        newUsage.setDay(day);
        newUsage.setStackId(usage.getStackId());
        newUsage.setStackName(usage.getStackName());
        newUsage.setInstanceType(usage.getInstanceType());
        newUsage.setInstanceNum(usage.getInstanceNum());
        newUsage.setPeak(usage.getInstanceNum());
        newUsage.setInstanceGroup(usage.getInstanceGroup());
        newUsage.setBlueprintId(usage.getBlueprintId());
        newUsage.setBlueprintName(usage.getBlueprintName());
        newUsage.setPeriodStarted(day);
        newUsage.setStatus(usage.getStatus());
        newUsage.setFlexId(usage.getFlexId());
        newUsage.setSmartSenseId(usage.getSmartSenseId());
        return newUsage;
    }

    public CloudbreakUsage openNewUsage(Stack stack, String instanceType, Integer instanceNum, String groupName, TemporalAccessor started) {
        CloudbreakUsage usage = new CloudbreakUsage();
        usage.setStackUuid(stack.getUuid());
        usage.setParentUuid(cloudbreakNodeConfig.getInstanceUUID());
        usage.setProvider(stack.cloudPlatform());
        usage.setRegion(stack.getRegion());
        usage.setAvailabilityZone(stack.getAvailabilityZone());
        usage.setInstanceHours(0L);
        usage.setCosts(0.0);
        Date day = Date.from(LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant());
        usage.setDay(day);
        usage.setStackId(stack.getId());
        usage.setStackName(stack.getName());
        usage.setInstanceType(instanceType);
        usage.setInstanceNum(instanceNum);
        usage.setPeak(instanceNum);
        usage.setInstanceGroup(groupName);
        if (stack.getCluster() != null && stack.getCluster().getBlueprint() != null) {
            Blueprint bp = stack.getCluster().getBlueprint();
            usage.setBlueprintId(bp.getId());
            usage.setBlueprintName(bp.getAmbariName());
        }
        usage.setPeriodStarted(Date.from(LocalDateTime.from(started).atZone(ZoneId.systemDefault()).toInstant()));
        usage.setStatus(UsageStatus.OPEN);
        FlexSubscription flexSubscription = stack.getFlexSubscription();
        if (flexSubscription != null && flexSubscription.getSmartSenseSubscription() != null) {
            usage.setFlexId(flexSubscription.getSubscriptionId());
            usage.setSmartSenseId(flexSubscription.getSmartSenseSubscription().getSubscriptionId());
        }
        return usage;
    }

    public Collection<CloudbreakUsage> createClosedUsagesUntilNow(CloudbreakUsage usage) {
        List<CloudbreakUsage> result = Lists.newArrayList();
        long days = usageTimeService.daysBetweenDateAndNow(usage.getDay());
        ZonedDateTime zdt = usage.getDay().toInstant().atZone(ZoneId.systemDefault());
        for (int i = 1; i < days; i++) {
            Date day = Date.from(zdt.plusDays(i).toInstant());
            result.add(createFullClosed(usage, day));
        }
        return result;
    }

}
