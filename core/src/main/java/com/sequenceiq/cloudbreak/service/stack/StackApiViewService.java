package com.sequenceiq.cloudbreak.service.stack;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.sequenceiq.cloudbreak.api.model.stack.StackViewResponse;
import com.sequenceiq.cloudbreak.converter.stack.StackListItemToStackViewResponseConverter;
import com.sequenceiq.cloudbreak.domain.projection.StackInstanceCount;
import com.sequenceiq.cloudbreak.domain.projection.StackListItem;
import com.sequenceiq.cloudbreak.domain.view.HostGroupView;
import com.sequenceiq.cloudbreak.repository.HostGroupViewRepository;
import com.sequenceiq.cloudbreak.repository.InstanceMetaDataRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;

@Service
public class StackApiViewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StackApiViewService.class);

    @Inject
    private StackRepository stackRepository;

    @Inject
    private InstanceMetaDataRepository instanceMetaDataRepository;

    @Inject
    private HostGroupViewRepository hostGroupViewRepository;

    @Inject
    private StackListItemToStackViewResponseConverter stackListItemToStackViewResponseConverter;

    public Set<StackViewResponse> retrieveStackViewsByWorkspaceId(Long workspaceId) {
        Map<Long, Integer> stackInstanceCounts = instanceMetaDataRepository.countByWorkspaceId(workspaceId).stream()
                .collect(Collectors.toMap(StackInstanceCount::getStackId, StackInstanceCount::getInstanceCount));
        Map<Long, Integer> stackUnhealthyInstanceCounts = instanceMetaDataRepository.countUnhealthyByWorkspaceId(workspaceId).stream()
                .collect(Collectors.toMap(StackInstanceCount::getStackId, StackInstanceCount::getInstanceCount));
        Set<StackListItem> stackList = stackRepository.findByWorkspaceId(workspaceId);
        Set<Long> clusterIds = stackList.stream().map(StackListItem::getClusterId).collect(Collectors.toSet());
        Map<Long, List<HostGroupView>> clusterHgMap = getClusterHostGroupMap(clusterIds);
        return stackList.stream()
                .map(item -> {
                    String sharedClusterName = Optional.ofNullable(item.getSharedClusterId())
                            .map(sharedClusterId -> stackRepository.findNameByIdAndWorkspaceId(sharedClusterId, workspaceId)).orElse(null);
                    return stackListItemToStackViewResponseConverter.convert(item, stackInstanceCounts, stackUnhealthyInstanceCounts, sharedClusterName,
                            clusterHgMap.get(item.getClusterId()));
                })
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
}
