package com.sequenceiq.cloudbreak.service.usages;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.SmartSenseSubscription;
import com.sequenceiq.cloudbreak.service.flex.FlexSubscriptionService;
import com.sequenceiq.cloudbreak.service.smartsense.SmartSenseSubscriptionService;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

@Service
public class FlexUsageGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(FlexUsageGenerator.class);

    private static final String DATE_FORMAT_PATTERN = "yyyy-MM-dd";

    private static final String CLOUDBREAK_PRODUCT_ID = "cloudbreak";

    private static final String CBD_COMPONENT_ID = "cloudbreak-cbd";

    private static final String HDP_COMPONENT_ID = "cloudbreak-hdp";

    @Inject
    private UserDetailsService userDetailsService;

    @Inject
    private SmartSenseSubscriptionService smartSenseSubscriptionService;

    @Inject
    private FlexSubscriptionService flexSubscriptionService;

    @Value("${cb.instance.uuid:}")
    private String parentUuid;

    public CloudbreakFlexUsageJson getUsages(List<CloudbreakUsage> usages) {
        LOGGER.info("Generating Cloudbreak Flex related usages.");
        CloudbreakFlexUsageJson result = new CloudbreakFlexUsageJson();
        Optional<CloudbreakUsage> aUsage = usages.stream().findFirst();
        result.setController(getFlexUsageControllerJson(usages, aUsage));
        result.setProducts(getFlexUsageProductJsons(usages, aUsage));
        return result;
    }

    private FlexUsageControllerJson getFlexUsageControllerJson(List<CloudbreakUsage> usages, Optional<CloudbreakUsage> aUsage) {
        Optional<SmartSenseSubscription> smartSenseSubscriptionOptional = smartSenseSubscriptionService.getOne();
        FlexUsageControllerJson controllerJson = new FlexUsageControllerJson();
        controllerJson.setGuid(parentUuid);
        //TODO get the details of the deployment somehow Provider/Region/CreationDate
        controllerJson.setInstanceId("");
        controllerJson.setProvider("");
        controllerJson.setRegion("");
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

    private List<FlexUsageProductJson> getFlexUsageProductJsons(List<CloudbreakUsage> usages, Optional<CloudbreakUsage> aUsage) {
        List<FlexUsageProductJson> flexUsageProducts = new ArrayList<>();
        FlexUsageProductJson flexUsageProductJson = new FlexUsageProductJson();
        flexUsageProductJson.setProductId(CLOUDBREAK_PRODUCT_ID);
        List<FlexUsageComponentJson> components = new ArrayList<>();

        FlexUsageComponentJson cbdComponent = new FlexUsageComponentJson();
        cbdComponent.setComponentId(CBD_COMPONENT_ID);
        FlexUsageCbdInstanceJson cbdComponentInstance = getFlexUsageCbdInstance();
        cbdComponent.setInstances(Collections.singletonList(cbdComponentInstance));
        components.add(cbdComponent);

        FlexUsageComponentJson hdpComponent = new FlexUsageComponentJson();
        hdpComponent.setComponentId(HDP_COMPONENT_ID);
        hdpComponent.setInstances(getFlexUsageHdpInstances(usages));
        components.add(hdpComponent);

        flexUsageProductJson.setComponents(components);
        flexUsageProducts.add(flexUsageProductJson);
        return flexUsageProducts;
    }

    private FlexUsageCbdInstanceJson getFlexUsageCbdInstance() {
        FlexUsageCbdInstanceJson cbdComponentInstance = new FlexUsageCbdInstanceJson();
        cbdComponentInstance.setGuid(parentUuid);
        cbdComponentInstance.setPeakUsage(1);
        //TODO get the details of the deployment somehow Provider/Region/CreationDate
        cbdComponentInstance.setProvider("");
        cbdComponentInstance.setRegion("");
        cbdComponentInstance.setCreationTime("");
        //TODO create a findDefaults method to flexSubscriptionService that will return with the default subscription for controllers
        cbdComponentInstance.setFlexPlanId("");
        //TODO add the date as String with the required pattern
        cbdComponentInstance.setUsageDate("");
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

                usageJson.setUsageDate("");
                usageJson.setCreationTime("");
                usageJson.setTerminationTime("");
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

    private String getDayAsString(Date day) {
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_PATTERN);
        return sdf.format(day);
    }
}
