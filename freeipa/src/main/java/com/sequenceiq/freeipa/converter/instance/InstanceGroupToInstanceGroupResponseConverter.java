package com.sequenceiq.freeipa.converter.instance;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.converter.instance.template.TemplateToInstanceTemplateResponseConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;
import com.sequenceiq.freeipa.entity.InstanceMetaData;

@Component
public class InstanceGroupToInstanceGroupResponseConverter {

    @Inject
    private TemplateToInstanceTemplateResponseConverter templateResponseConverter;

    @Inject
    private SecurityGroupToSecurityGroupResponseConverter securityGroupConverter;

    @Inject
    private InstanceGroupNetworkToInstanceGroupNetworkResponseConverter instanceGroupNetworkConverter;

    @Inject
    private InstanceMetaDataToInstanceMetaDataResponseConverter metaDataConverter;

    public InstanceGroupResponse convert(InstanceGroup source, Boolean includeAllInstances) {
        InstanceGroupResponse instanceGroupResponse = new InstanceGroupResponse();
        instanceGroupResponse.setName(source.getGroupName());
        if (source.getTemplate() != null) {
            instanceGroupResponse.setInstanceTemplate(templateResponseConverter.convert(source.getTemplate()));
        }
        Set<InstanceMetaData> metaDataSet = includeAllInstances ? source.getInstanceMetaDataSet() : source.getNotTerminatedInstanceMetaDataSet();
        instanceGroupResponse.setMetaData(metaDataConverter.convert(metaDataSet));
        if (source.getSecurityGroup() != null) {
            instanceGroupResponse.setSecurityGroup(securityGroupConverter.convert(source.getSecurityGroup()));
        }
        if (source.getInstanceGroupNetwork() != null) {
            instanceGroupResponse.setNetwork(instanceGroupNetworkConverter.convert(source.getInstanceGroupNetwork()));
        }
        instanceGroupResponse.setNodeCount(source.getNodeCount());
        instanceGroupResponse.setName(source.getGroupName());
        return instanceGroupResponse;
    }

    public List<InstanceGroupResponse> convert(Iterable<InstanceGroup> source, Boolean includeAllInstances) {
        return StreamSupport.stream(source.spliterator(), false).map(s -> convert(s, includeAllInstances)).collect(Collectors.toList());
    }
}
