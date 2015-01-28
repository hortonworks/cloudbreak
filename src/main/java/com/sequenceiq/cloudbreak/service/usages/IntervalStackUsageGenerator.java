package com.sequenceiq.cloudbreak.service.usages;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
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

    public Map<String, CloudbreakUsage> generateUsages(Date startTime, Date stopTime, CloudbreakEvent startEvent) throws ParseException {
        Map<String, CloudbreakUsage> dailyStackUsages = new HashMap<>();
        Stack stack = stackRepository.findById(startEvent.getStackId());

        if (stack != null) {
            Set<InstanceMetaData> instancesMetaData = stack.getAllInstanceMetaData();
            PriceGenerator priceGenerator = selectPriceGeneratorByPlatform(stack);

            for (InstanceMetaData instance : instancesMetaData) {
                Template template = instance.getInstanceGroup().getTemplate();
                Map<String, Long> instanceHours = instanceUsageGenerator.getInstanceHours(instance, startTime, stopTime);
                addInstanceHoursToStackUsages(dailyStackUsages, instanceHours, startEvent, priceGenerator, template);
            }
        }
        return dailyStackUsages;
    }


    private PriceGenerator selectPriceGeneratorByPlatform(Stack stack) {
        PriceGenerator result = null;
        CloudPlatform stackCloudPlatform = stack.cloudPlatform();
        for (PriceGenerator generator : priceGenerators) {
            CloudPlatform generatorCloudPlatform = generator.getCloudPlatform();
            if (stackCloudPlatform.equals(generatorCloudPlatform)) {
                result = generator;
                break;
            }
        }
        return result;
    }

    private void addInstanceHoursToStackUsages(Map<String, CloudbreakUsage> dailyStackUsages, Map<String, Long> instanceUsages,
            CloudbreakEvent event, PriceGenerator priceGenerator, Template template) throws ParseException {

        for (Map.Entry<String, Long> entry : instanceUsages.entrySet()) {
            String day = entry.getKey();
            Long instanceHours = entry.getValue();
            Double costOfInstance = calculateCostOfInstance(priceGenerator, template, instanceHours);
            if (dailyStackUsages.containsKey(day)) {
                CloudbreakUsage usage = dailyStackUsages.get(day);
                long numberOfHours = usage.getInstanceHours() + instanceHours;
                double costOfStack = usage.getCosts() + costOfInstance;
                usage.setInstanceHours(numberOfHours);
                usage.setCosts(costOfStack);
            } else {
                CloudbreakUsage usage = getCloudbreakUsage(event, instanceHours, day, costOfInstance);
                dailyStackUsages.put(day, usage);
            }
        }
    }

    private Double calculateCostOfInstance(PriceGenerator priceGenerator, Template template, Long instanceHours) {
        Double result = 0.0;
        if (priceGenerator != null) {
            result = priceGenerator.calculate(template, instanceHours);
        }
        return result;
    }

    private CloudbreakUsage getCloudbreakUsage(CloudbreakEvent event, long instanceHours, String dayString, Double costOfInstance) throws ParseException {
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
        usage.setCosts(costOfInstance);
        return usage;
    }
}
