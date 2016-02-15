package com.sequenceiq.cloudbreak.service.usages;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.CloudbreakEvent;
import com.sequenceiq.cloudbreak.domain.CloudbreakUsage;
import com.sequenceiq.cloudbreak.domain.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventRepository;
import com.sequenceiq.cloudbreak.repository.CloudbreakEventSpecifications;
import com.sequenceiq.cloudbreak.repository.CloudbreakUsageRepository;
import com.sequenceiq.cloudbreak.repository.FileSystemRepository;
import com.sequenceiq.cloudbreak.repository.OrchestratorRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.repository.TemplateRepository;

@Service
public class DefaultCloudbreakUsageGeneratorService implements CloudbreakUsageGeneratorService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCloudbreakUsageGeneratorService.class);

    @Inject
    private CloudbreakUsageRepository usageRepository;

    @Inject
    private CloudbreakEventRepository eventRepository;

    @Inject
    private StackUsageGenerator stackUsageGenerator;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private TemplateRepository templateRepository;

    @Inject
    private FileSystemRepository fileSystemRepository;

    @Inject
    private OrchestratorRepository orchestratorRepository;

    @Override
    @Scheduled(cron = "0 01 0 * * *")
    public void generate() {
        List<CloudbreakUsage> usageList = new ArrayList<>();
        Iterable<CloudbreakEvent> cloudbreakEvents = getCloudbreakEvents();
        Map<Long, List<CloudbreakEvent>> stackEvents = groupCloudbreakEventsByStack(cloudbreakEvents);
        generateDailyUsageForStacks(usageList, stackEvents);
        Set<Long> stackIds = new HashSet<>();
        stackIds.addAll(stackEvents.keySet());
        stackIds.addAll(stackRepository.findStacksWithoutEvents());
        deleteTerminatedStacks(stackIds);
        usageRepository.save(usageList);
    }

    private Iterable<CloudbreakEvent> getCloudbreakEvents() {
        Iterable<CloudbreakEvent> cloudbreakEvents;
        Long usagesCount = usageRepository.count();
        Sort sortByTimestamp = new Sort("eventTimestamp");
        if (usagesCount > 0) {
            Long startOfPreviousDay = getStartOfPreviousDay();
            LOGGER.info("Generate usages from events since '{}'.", new Date(startOfPreviousDay));
            Specification<CloudbreakEvent> sinceSpecification = CloudbreakEventSpecifications.eventsSince(startOfPreviousDay);
            cloudbreakEvents = eventRepository.findAll(Specifications.where(sinceSpecification), sortByTimestamp);
        } else {
            LOGGER.info("Generate usages from all events....");
            cloudbreakEvents = eventRepository.findAll(sortByTimestamp);
        }
        return cloudbreakEvents;
    }

    private Long getStartOfPreviousDay() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private Map<Long, List<CloudbreakEvent>> groupCloudbreakEventsByStack(Iterable<CloudbreakEvent> userStackEvents) {
        Map<Long, List<CloudbreakEvent>> stackIdToCbEventMap = new HashMap<>();
        for (CloudbreakEvent cbEvent : userStackEvents) {
            LOGGER.debug("Processing stack {} for user {}", cbEvent.getStackId(), cbEvent.getOwner());
            if (!stackIdToCbEventMap.containsKey(cbEvent.getStackId())) {
                stackIdToCbEventMap.put(cbEvent.getStackId(), new ArrayList<CloudbreakEvent>());
            }
            stackIdToCbEventMap.get(cbEvent.getStackId()).add(cbEvent);
        }
        return stackIdToCbEventMap;
    }

    private void generateDailyUsageForStacks(List<CloudbreakUsage> usageList, Map<Long, List<CloudbreakEvent>> stackEvents) {
        for (Map.Entry<Long, List<CloudbreakEvent>> stackEventEntry : stackEvents.entrySet()) {
            LOGGER.debug("Processing stackId {} for userid {}", stackEventEntry.getKey());
            List<CloudbreakUsage> stackDailyUsages = stackUsageGenerator.generate(stackEventEntry.getValue());
            usageList.addAll(stackDailyUsages);
        }
    }

    private void deleteTerminatedStacks(Set<Long> stackIds) {
        for (Long stackId : stackIds) {
            Stack stack = stackRepository.findById(stackId);
            if (stack != null && stack.isDeleteCompleted()) {
                Long fsId = null;
                if (stack.getCluster() != null && stack.getCluster().getFileSystem() != null) {
                    fsId = stack.getCluster().getFileSystem().getId();
                }
                Long orchestratorId = null;
                if (stack.getOrchestrator() != null) {
                    orchestratorId = stack.getOrchestrator().getId();
                }
                stackRepository.delete(stack);
                deleteTemplatesOfStack(stack);
                if (fsId != null) {
                    fileSystemRepository.delete(fsId);
                }
                if (orchestratorId != null) {
                    orchestratorRepository.delete(orchestratorId);
                }
                eventRepository.delete(eventRepository.findCloudbreakEventsForStack(stackId));
            }
        }
    }

    private void deleteTemplatesOfStack(Stack stack) {
        for (InstanceGroup instanceGroup : stack.getInstanceGroups()) {
            Template template = instanceGroup.getTemplate();
            if (template != null) {
                List<Stack> allStackForTemplate = stackRepository.findAllStackForTemplate(template.getId());
                if (template.isDeleted() && allStackForTemplate.size() <= 1) {
                    templateRepository.delete(template);
                }
            }
        }
    }
}
