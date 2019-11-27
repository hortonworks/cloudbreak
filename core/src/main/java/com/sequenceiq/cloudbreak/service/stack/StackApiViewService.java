package com.sequenceiq.cloudbreak.service.stack;

import static com.sequenceiq.cloudbreak.exception.NotFoundException.notFound;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.converter.stack.StackListItemToStackApiViewConverter;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.domain.view.StackApiView;
import com.sequenceiq.cloudbreak.repository.HostGroupViewRepository;
import com.sequenceiq.cloudbreak.repository.StackApiViewRepository;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.flow.core.FlowLogService;

@Service
public class StackApiViewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackApiViewService.class);

    @Inject
    private FlowLogService flowLogService;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Inject
    private ShowTerminatedClusterConfigService showTerminatedClusterConfigService;

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

    public StackApiViewService() {
    }

    public boolean canChangeCredential(StackApiView stackApiView) {
        if (stackApiView.getStatus() != null) {
            if (stackApiView.getStatus() == Status.AVAILABLE) {
                return !flowLogService.isOtherFlowRunning(stackApiView.getId());
            }
        }
        return false;
    }

    public StackApiView save(StackApiView stackApiView) {
        return stackApiViewRepository.save(stackApiView);
    }

    public Set<StackApiView> retrieveStackViewsByWorkspaceIdAndEnvironmentName(Long workspaceId, String environmentName, @Nullable StackType stackType) {
        if (StringUtils.isEmpty(environmentName)) {
            LOGGER.info("Environment name was empty so we will query all the stack.");
            return retrieveStackViewsByWorkspaceIdAndEnvironmentCrn(workspaceId, null, stackType);
        } else {
            LOGGER.info("Environment name was defined so we will query all the stack in the {} environment.", environmentName);
            String environmentCrn = environmentClientService.getCrnByName(environmentName);
            return retrieveStackViewsByWorkspaceIdAndEnvironmentCrn(workspaceId, environmentCrn, stackType);
        }
    }

    public Set<StackApiView> retrieveStackViewsByWorkspaceIdAndEnvironmentCrn(Long workspaceId, String environmentCrn, @Nullable StackType stackType) {
        LOGGER.info("Retrieving stacks of type: {}, in environment: {}", stackType, environmentCrn);

        Map<Long, Integer> instanceCounts = instanceMetaDataService.countByWorkspaceId(workspaceId, environmentCrn, stackType).stream()
                .collect(Collectors.toMap(StackInstanceCount::getStackId, StackInstanceCount::getInstanceCount));

        Set<StackListItem> stackList = stackService.getByWorkspaceId(workspaceId, environmentCrn, stackType);
        Set<Long> clusterIds = stackList.stream().map(StackListItem::getClusterId).collect(Collectors.toSet());
        Map<Long, List<HostGroupView>> clusterHgMap = getClusterHostGroupMap(clusterIds);
        return stackList.stream()
                .map(item -> stackListItemToStackApiViewConverter.convert(item, instanceCounts, clusterHgMap.get(item.getClusterId())))
                .collect(Collectors.toSet());
    }

    private Map<Long, List<HostGroupView>> getClusterHostGroupMap(Set<Long> clusterIds) {
        if (CollectionUtils.isEmpty(clusterIds)) {
            return Map.of();
        }

        Set<HostGroupView> allHostGroups = hostGroupViewRepository.findHostGroupsInClusterList(clusterIds);
        return allHostGroups.stream().collect(Collectors.toMap(
                hg -> hg.getCluster().getId(),
                List::of,
                (left, right) -> Stream.concat(left.stream(), right.stream()).collect(Collectors.toList())
        ));
    }

    @PreAuthorize("hasRole('INTERNAL')")
    public StackApiView retrieveStackByCrnAndType(String crn, StackType stackType) {
        return stackApiViewRepository.findByResourceCrnAndStackType(crn, stackType).orElseThrow(notFound("Stack", crn));
    }
}
