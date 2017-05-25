package com.sequenceiq.cloudbreak.service.usages;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.flex.CloudbreakFlexUsageJson;
import com.sequenceiq.cloudbreak.api.model.flex.FlexUsageCbdInstanceJson;
import com.sequenceiq.cloudbreak.api.model.flex.FlexUsageComponentJson;
import com.sequenceiq.cloudbreak.api.model.flex.FlexUsageControllerJson;
import com.sequenceiq.cloudbreak.api.model.flex.FlexUsageHdpInstanceJson;
import com.sequenceiq.cloudbreak.api.model.flex.FlexUsageProductJson;
import com.sequenceiq.cloudbreak.common.service.user.UserFilterField;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.FlexSubscription;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.FlexSubscriptionRepository;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;

@Service
public class FlexUsageGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlexUsageGenerator.class);

    private static final String FLEX_TIME_ZONE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss Z";

    private static final String FLEX_USAGE_DAY_FORMAT_PATTERN = "yyyy-MM-dd";

    private static final String STACK_NAME_DELIMITER = "_";

    private static final long TIMESTAMP_MAX_SEC = 9999999999L;

    private static final long REDUCE_TO_SEC = 1000;

    @Inject
    private UserDetailsService userDetailsService;

    @Inject
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Inject
    private StackService stackService;

    @Inject
    private FlexSubscriptionRepository flexSubscriptionRepository;

    @Value("${cb.instance.uuid:}")
    private String parentUuid;

    @Value("${cb.instance.provider:on-prem}")
    private String cbInstanceProvider;

    @Value("${cb.instance.region:local}")
    private String cbInstanceRegion;

    @Value("${cb.product.id:cloudbreak}")
    private String productId;

    @Value("${cb.component.id:cloudbreak-cbd}")
    private String controllerComponentId;

    @Value("${cb.component.created:}")
    private Long controllerCreated;

    @Value("${cb.component.cluster.id:cloudbreak-hdp}")
    private String clustersComponentId;

    public CloudbreakFlexUsageJson getUsages(List<CloudbreakUsage> usages, Long fromDate) {
        LOGGER.info("Generating Cloudbreak Flex related usages.");
        CloudbreakFlexUsageJson result = new CloudbreakFlexUsageJson();
        Optional<CloudbreakUsage> aUsage = usages.stream().findFirst();
        result.setController(getFlexUsageControllerJson(usages, aUsage));
        result.setProducts(getFlexUsageProductJsons(usages, aUsage, fromDate));
        return result;
    }

    private FlexUsageControllerJson getFlexUsageControllerJson(List<CloudbreakUsage> usages, Optional<CloudbreakUsage> aUsage) {
        Optional<SmartSenseSubscription> smartSenseSubscriptionOptional = smartSenseSubscriptionService.getOne();
        FlexUsageControllerJson controllerJson = new FlexUsageControllerJson();
        controllerJson.setGuid(parentUuid);
        controllerJson.setInstanceId(parentUuid);
        controllerJson.setProvider(cbInstanceProvider);
        controllerJson.setRegion(cbInstanceRegion);
        aUsage.ifPresent(cloudbreakUsage -> controllerJson.setUserName(getUserEmail(cloudbreakUsage)));
        smartSenseSubscriptionOptional.ifPresent(smartSenseSubscription -> controllerJson.setSmartSenseId(smartSenseSubscription.getSubscriptionId()));
        return controllerJson;
    }

    private String getUserEmail(CloudbreakUsage source) {
        String cbUser;
        try {
            cbUser = userDetailsService.getDetails(source.getOwner(), UserFilterField.USERID).getUsername();
        } catch (Exception ex) {
            LOGGER.warn(String.format("Expected user was not found with '%s' id. Maybe it was deleted by the admin user.", source.getOwner()));
            cbUser = source.getOwner();
        }
        return cbUser;
    }

    private List<FlexUsageProductJson> getFlexUsageProductJsons(List<CloudbreakUsage> usages, Optional<CloudbreakUsage> aUsage, Long fromDate) {
        List<FlexUsageProductJson> flexUsageProducts = new ArrayList<>();
        FlexUsageProductJson flexUsageProductJson = new FlexUsageProductJson();
        flexUsageProductJson.setProductId(productId);
        List<FlexUsageComponentJson> components = new ArrayList<>();

        FlexUsageComponentJson cbdComponent = new FlexUsageComponentJson();
        cbdComponent.setComponentId(controllerComponentId);
        FlexUsageCbdInstanceJson cbdComponentInstance = getFlexUsageCbdInstance(fromDate);
        cbdComponent.setInstances(Collections.singletonList(cbdComponentInstance));
        components.add(cbdComponent);

        FlexUsageComponentJson hdpComponent = new FlexUsageComponentJson();
        hdpComponent.setComponentId(clustersComponentId);
        hdpComponent.setInstances(getFlexUsageHdpInstances(usages));
        components.add(hdpComponent);

        flexUsageProductJson.setComponents(components);
        flexUsageProducts.add(flexUsageProductJson);
        return flexUsageProducts;
    }

    private FlexUsageCbdInstanceJson getFlexUsageCbdInstance(Long fromDate) {
        FlexUsageCbdInstanceJson cbdComponentInstance = new FlexUsageCbdInstanceJson();
        cbdComponentInstance.setGuid(parentUuid);
        cbdComponentInstance.setPeakUsage(1);
        cbdComponentInstance.setProvider(cbInstanceProvider);
        cbdComponentInstance.setRegion(cbInstanceRegion);
        String creationTime = "";
        if (controllerCreated != null) {
            long created = controllerCreated > TIMESTAMP_MAX_SEC ? controllerCreated / REDUCE_TO_SEC : controllerCreated;
            creationTime = formatInstant(Instant.ofEpochSecond(created), FLEX_TIME_ZONE_FORMAT_PATTERN);
        }
        cbdComponentInstance.setCreationTime(creationTime);
        FlexSubscription usedForController = Optional.ofNullable(flexSubscriptionRepository.findFirstByUsedForController(true))
                .orElse(flexSubscriptionRepository.findFirstByIsDefault(true));
        cbdComponentInstance.setFlexPlanId(usedForController == null ? "" : usedForController.getSubscriptionId());
        cbdComponentInstance.setUsageDate(formatInstant(Instant.ofEpochMilli(fromDate), FLEX_USAGE_DAY_FORMAT_PATTERN));
        return cbdComponentInstance;
    }

    private List<FlexUsageHdpInstanceJson> getFlexUsageHdpInstances(List<CloudbreakUsage> usages) {
        Map<Long, FlexUsageHdpInstanceJson> flexUsageJsonsByStackId = new HashMap<>();
        for (CloudbreakUsage usage : usages) {
            Long stackId = usage.getStackId();
            if (!flexUsageJsonsByStackId.containsKey(stackId)) {
                FlexUsageHdpInstanceJson usageJson = new FlexUsageHdpInstanceJson();
                usageJson.setGuid(usage.getStackUuid());
                usageJson.setParentGuid(usage.getParentUuid());
                usageJson.setClusterName(usage.getStackName());
                usageJson.setBlueprintName(usage.getBlueprintName());
                usageJson.setFlexPlanId(usage.getFlexId());
                usageJson.setProvider(usage.getProvider());
                usageJson.setRegion(usage.getRegion());
                usageJson.setPeakUsage(usage.getPeak());
                usageJson.setNodeCount(usage.getInstanceNum());
                usageJson.setUsageDate(formatInstant(usage.getDay().toInstant(), FLEX_USAGE_DAY_FORMAT_PATTERN));
                Stack stack = stackService.getById(usage.getStackId());
                usageJson.setCreationTime(formatInstant(Instant.ofEpochMilli(stack.getCreated()), FLEX_TIME_ZONE_FORMAT_PATTERN));
                usageJson.setTerminationTime(getTerminationTime(stack));
                flexUsageJsonsByStackId.put(stackId, usageJson);
            } else {
                FlexUsageHdpInstanceJson usageJson = flexUsageJsonsByStackId.get(stackId);
                Integer actPeak = usage.getPeak() != null ? usage.getPeak() : 0;
                Integer peak = usageJson.getPeakUsage() != null ? usageJson.getPeakUsage() : 0;
                int newPeak = peak + actPeak;
                int instanceNum = usageJson.getNodeCount() + usage.getInstanceNum();
                usageJson.setPeakUsage(newPeak);
                usageJson.setNodeCount(instanceNum);
            }
        }
        return new ArrayList<>(flexUsageJsonsByStackId.values());
    }

    private String formatInstant(Instant instant, String pattern) {
        ZonedDateTime usageDayZoneDate = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault());
        return usageDayZoneDate.format(DateTimeFormatter.ofPattern(pattern));
    }

    private String getTerminationTime(Stack stack) {
        String terminationTime = "";
        if (stack.isDeleteCompleted()) {
            try {
                String stackName = stack.getName();
                int indexOfDelimiter = stackName.lastIndexOf(STACK_NAME_DELIMITER) + 1;
                Long stackTerminationInMillis = Long.valueOf(stackName.substring(indexOfDelimiter));
                terminationTime = formatInstant(Instant.ofEpochMilli(stackTerminationInMillis), FLEX_TIME_ZONE_FORMAT_PATTERN);
            } catch (Exception ex) {
                LOGGER.warn("Stack termination time could not be calculated.", ex);
            }
        }
        return terminationTime;
    }
}
