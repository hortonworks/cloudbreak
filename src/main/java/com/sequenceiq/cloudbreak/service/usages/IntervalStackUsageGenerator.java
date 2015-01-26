package com.sequenceiq.cloudbreak.service.usages;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Component
public class IntervalStackUsageGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(IntervalStackUsageGenerator.class);
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    @Autowired
    private StackRepository stackRepository;

    @Autowired
    private IntervalInstanceUsageGenerator instanceUsageGenerator;

    public Map<String, CloudbreakUsage> generateUsages(Date startTime, Date stopTime, CloudbreakEvent startEvent) throws ParseException {
        Map<String, CloudbreakUsage> dailyStackUsages = new HashMap<>();
        Stack stack = stackRepository.findById(startEvent.getStackId());
        Set<InstanceMetaData> instancesMetaData = stack.getAllInstanceMetaData();

        for (InstanceMetaData instance : instancesMetaData) {
            Map<String, Long> instanceUsages = instanceUsageGenerator.getInstanceHours(instance, startTime, stopTime);
            addInstanceUsagesToStackUsages(dailyStackUsages, instanceUsages, startEvent);
        }

        return dailyStackUsages;
    }

    private void addInstanceUsagesToStackUsages(Map<String, CloudbreakUsage> dailyStackUsages, Map<String, Long> instanceUsages,
            CloudbreakEvent event) throws ParseException {
        for (Map.Entry<String, Long> entry : instanceUsages.entrySet()) {
            String day = entry.getKey();
            Long instanceHours = entry.getValue();
            if (dailyStackUsages.containsKey(day)) {
                CloudbreakUsage usage = dailyStackUsages.get(day);
                long numberOfHours = usage.getInstanceHours() + instanceHours;
                usage.setInstanceHours(numberOfHours);
//                real calculated usage should be set here
//                usage.setCosts();
            } else {
                //real calculated usage should be set here
                CloudbreakUsage usage = getCloudbreakUsage(event, instanceHours, day);
                dailyStackUsages.put(day, usage);
            }
        }
    }

    private CloudbreakUsage getCloudbreakUsage(CloudbreakEvent event, long runningHours, String dayString) throws ParseException {
        Date day = DATE_FORMAT.parse(dayString);
        long nodesRunningHours = runningHours * event.getNodeCount();
        CloudbreakUsage usage = new CloudbreakUsage();
        usage.setOwner(event.getOwner());
        usage.setAccount(event.getAccount());
        usage.setProvider(event.getCloud());
        usage.setRegion(event.getRegion());
        usage.setInstanceHours(nodesRunningHours);
        usage.setDay(day);
        usage.setStackId(event.getStackId());
        usage.setStackName(event.getStackName());

        //real calculated usage should be set here
        usage.setCosts(0.0);
        return usage;
    }
}
