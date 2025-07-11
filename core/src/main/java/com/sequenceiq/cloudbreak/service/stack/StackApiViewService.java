package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.converter.stack.StackListItemToStackApiViewConverter;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.domain.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.HostGroupViewRepository;
import com.sequenceiq.cloudbreak.repository.StackApiViewRepository;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentService;
import com.sequenceiq.flow.core.FlowLogService;

@Service
public class StackApiViewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackApiViewService.class);

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private EnvironmentService environmentClientService;

    @Inject
    private StackApiViewRepository stackApiViewRepository;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private StackListItemToStackApiViewConverter stackListItemToStackApiViewConverter;

    @Inject
    private StackService stackService;

    @Inject
    private HostGroupViewRepository hostGroupViewRepository;

    @Inject
    private InstanceGroupService instanceGroupService;

    public StackApiViewService() {
    }

    public StackApiView save(StackApiView stackApiView) {
        return stackApiViewRepository.save(stackApiView);
    }

    public Set<StackApiView> retrieveStackViewsByWorkspaceIdAndEnvironmentName(Long workspaceId, String environmentName, List<StackType> stackTypes) {
        if (StringUtils.isEmpty(environmentName)) {
            LOGGER.info("Environment name was empty so we will query all the stack.");
            return retrieveStackViewsByWorkspaceIdAndEnvironmentCrn(workspaceId, null, stackTypes);
        } else {
            LOGGER.info("Environment name was defined so we will query all the stack in the {} environment.", environmentName);
            String environmentCrn = environmentClientService.getCrnByName(environmentName);
            return retrieveStackViewsByWorkspaceIdAndEnvironmentCrn(workspaceId, environmentCrn, stackTypes);
        }
    }

    public Set<StackApiView> retrieveStackViewsByWorkspaceIdAndEnvironmentCrn(Long workspaceId, String environmentCrn, List<StackType> stackTypes) {
        LOGGER.info("Retrieving stacks of type: {}, in environment: {}", stackTypes, environmentCrn);

        Map<Long, Integer> instanceCounts = instanceMetaDataService.countByWorkspaceId(workspaceId, environmentCrn, stackTypes).stream()
                .collect(Collectors.toMap(StackInstanceCount::getStackId, StackInstanceCount::getInstanceCount));

        Set<StackListItem> stackList = stackService.getByWorkspaceId(workspaceId, environmentCrn, stackTypes);
        return getStackApiViews(instanceCounts, stackList);
    }

    private Set<StackApiView> getStackApiViews(Map<Long, Integer> instanceCounts, Set<StackListItem> stackList) {
        Set<Long> clusterIds = stackList.stream().map(StackListItem::getClusterId).collect(Collectors.toSet());
        Multimap<Long, HostGroupView> clusterHgMap = getClusterHostGroupMap(clusterIds);
        Multimap<Long, InstanceGroupView> stackIgMap = getStackInstanceGroupMap(stackList.stream().map(StackListItem::getId).collect(Collectors.toSet()));
        return stackList.stream()
                .map(item ->
                        stackListItemToStackApiViewConverter.convert(item, instanceCounts, clusterHgMap.get(item.getClusterId()), stackIgMap.get(item.getId())))
                .collect(Collectors.toSet());
    }

    private Multimap<Long, InstanceGroupView> getStackInstanceGroupMap(Set<Long> stackIds) {
        if (CollectionUtils.isEmpty(stackIds)) {
            return HashMultimap.create();
        }
        Set<InstanceGroupView> allInstanceGroups = instanceGroupService.getInstanceGroupViewByStackIds(stackIds);
        Multimap<Long, InstanceGroupView> result = HashMultimap.create();
        allInstanceGroups.forEach(ig -> result.put(ig.getStackId(), ig));
        return result;
    }

    public Set<StackApiView> retrieveStackViewsByStackIdsAndEnvironmentCrn(Long workspaceId, List<Long> stackIds, String environmentCrn,
            List<StackType> stackTypes) {
        LOGGER.info("Retrieving stacks of type: {}, in environment: {}", stackTypes, environmentCrn);

        Map<Long, Integer> instanceCounts = instanceMetaDataService.countByWorkspaceId(workspaceId, environmentCrn, stackTypes).stream()
                .collect(Collectors.toMap(StackInstanceCount::getStackId, StackInstanceCount::getInstanceCount));

        Set<StackListItem> stackList = stackService.getByWorkspaceIdAndStackIds(workspaceId, stackIds, stackTypes);
        return getStackApiViews(instanceCounts, stackList);
    }

    private Multimap<Long, HostGroupView> getClusterHostGroupMap(Set<Long> clusterIds) {
        if (CollectionUtils.isEmpty(clusterIds)) {
            return HashMultimap.create();
        }

        Set<HostGroupView> allHostGroups = hostGroupViewRepository.findHostGroupsInClusterList(clusterIds);
        Multimap<Long, HostGroupView> result = HashMultimap.create();
        allHostGroups.forEach(hg -> result.put(hg.getClusterId(), hg));
        return result;
    }

    public StackApiView retrieveStackByCrnAndType(String crn, StackType stackType) {
        return stackApiViewRepository.findByResourceCrnAndStackType(crn, stackType).orElseThrow(notFound("Stack", crn));
    }

    public Set<InstanceMetaData> retrieveInstancesByCrnAndType(String crn, StackType stackType) {
        StackApiView stackApiView = retrieveStackByCrnAndType(crn, stackType);
        return instanceMetaDataService.findNotTerminatedAndNotZombieForStack(stackApiView.getId());
    }
}