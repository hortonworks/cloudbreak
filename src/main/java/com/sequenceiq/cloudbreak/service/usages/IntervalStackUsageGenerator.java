package com.sequenceiq.cloudbreak.service.usages;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.AwsTemplate;
import com.sequenceiq.cloudbreak.domain.AzureTemplate;
import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.GccTemplate;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.OpenStackTemplate;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.price.PriceGenerator;

@Component
public class IntervalStackUsageGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntervalStackUsageGenerator.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private IntervalInstanceUsageGenerator instanceUsageGenerator;

    @Autowired
    private List<PriceGenerator> priceGenerators;

    public List<CloudbreakUsage> generateUsages(Date startTime, Date stopTime, CloudbreakEvent startEvent) throws ParseException {
        List<CloudbreakUsage> dailyUsagesByHostGroup = new ArrayList<>();
        Stack stack = stackRepository.findById(startEvent.getStackId());

        if (stack != null) {
            for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
                Map<String, CloudbreakUsage> instanceGroupDailyUsages = new HashMap<>();
                Template template = instanceGroup.getTemplate();
                String instanceType = getInstanceType(template);
                String groupName = instanceGroup.getGroupName();
                PriceGenerator priceGenerator = selectPriceGeneratorByPlatform(template.cloudPlatform());

                for (InstanceMetaData metaData : instanceGroup.getAllInstanceMetaData()) {
                    Map<String, Long> instanceHours = instanceUsageGenerator.getInstanceHours(metaData, startTime, stopTime);
                    addInstanceHoursToStackUsages(instanceGroupDailyUsages, instanceHours, startEvent, instanceType, groupName);
                }

                addCalculatedPrice(instanceGroupDailyUsages, priceGenerator, template);
                dailyUsagesByHostGroup.addAll(instanceGroupDailyUsages.values());
            }
        }
        return dailyUsagesByHostGroup;
    }

    private String getInstanceType(Template template) {
        CloudPlatform cloudPlatform = template.cloudPlatform();
        String instanceType = "";

        if (CloudPlatform.AWS.equals(cloudPlatform)) {
            AwsTemplate awsTemp = (AwsTemplate) template;
            instanceType = awsTemp.getInstanceType().name();
        } else if (CloudPlatform.GCC.equals(cloudPlatform)) {
            GccTemplate gceTemp = (GccTemplate) template;
            instanceType = gceTemp.getGccInstanceType().name();
        } else if (CloudPlatform.AZURE.equals(cloudPlatform)) {
            AzureTemplate azureTemp = (AzureTemplate) template;
            instanceType = azureTemp.getVmType().toString();
        } else if (CloudPlatform.OPENSTACK.equals(cloudPlatform)) {
            OpenStackTemplate openTemp = (OpenStackTemplate) template;
            instanceType = openTemp.getInstanceType().toString();
        }
        return instanceType;
    }

    private PriceGenerator selectPriceGeneratorByPlatform(CloudPlatform cloudPlatform) {
        PriceGenerator result = null;
        for (PriceGenerator generator : priceGenerators) {
            CloudPlatform generatorCloudPlatform = generator.getCloudPlatform();
            if (cloudPlatform.equals(generatorCloudPlatform)) {
                result = generator;
                break;
            }
        }
        return result;
    }

    private void addInstanceHoursToStackUsages(Map<String, CloudbreakUsage> dailyStackUsages, Map<String, Long> instanceUsages,
            CloudbreakEvent event, String instanceType, String groupName) throws ParseException {

        for (Map.Entry<String, Long> entry : instanceUsages.entrySet()) {
            String day = entry.getKey();
            Long instanceHours = entry.getValue();
            if (dailyStackUsages.containsKey(day)) {
                CloudbreakUsage usage = dailyStackUsages.get(day);
                long numberOfHours = usage.getInstanceHours() + instanceHours;
                usage.setInstanceHours(numberOfHours);
            } else {
                CloudbreakUsage usage = getCloudbreakUsage(event, instanceHours, day, instanceType, groupName);
                dailyStackUsages.put(day, usage);
            }
        }
    }

    private CloudbreakUsage getCloudbreakUsage(CloudbreakEvent event, long instanceHours, String dayString, String instanceType, String groupName)
            throws ParseException {
        Date day = DATE_FORMAT.parse(dayString);
        CloudbreakUsage usage = new CloudbreakUsage();
        usage.setOwner(event.getOwner());
        usage.setAccount(event.getAccount());
        usage.setProvider(event.getCloud());
        usage.setRegion(event.getRegion());
        usage.setInstanceHours(instanceHours);
        usage.setDay(day);
        usage.setStackId(event.getStackId());
        usage.setStackName(event.getStackName());
        usage.setInstanceType(instanceType);
        usage.setHostGroup(groupName);
        return usage;
    }

    private void addCalculatedPrice(Map<String, CloudbreakUsage> instanceGroupDailyUsages, PriceGenerator priceGenerator, Template template) {
        for (CloudbreakUsage usage : instanceGroupDailyUsages.values()) {
            Long instanceHours = usage.getInstanceHours();
            Double costs = calculateCostOfInstance(priceGenerator, template, instanceHours);
            usage.setCosts(costs);
        }
    }

    private Double calculateCostOfInstance(PriceGenerator priceGenerator, Template template, Long instanceHours) {
        Double result = 0.0;
        if (priceGenerator != null) {
            result = priceGenerator.calculate(template, instanceHours);
        }
        return result;
    }
}
