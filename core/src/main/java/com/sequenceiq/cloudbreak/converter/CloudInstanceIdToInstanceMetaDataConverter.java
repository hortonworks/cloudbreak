package com.sequenceiq.cloudbreak.converter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;

@Component
public class CloudInstanceIdToInstanceMetaDataConverter {

    public List<InstanceMetaData> getNotDeletedInstances(Stack stack, String hostGroupname, Set<String> cloudInstanceIds) {
        return stack.getInstanceGroupByInstanceGroupName(hostGroupname).getNotDeletedInstanceMetaDataSet().stream()
                .filter(im -> im.getInstanceId() == null ? false : cloudInstanceIds.contains(im.getInstanceId()))
                .collect(Collectors.toList());
    }

}
