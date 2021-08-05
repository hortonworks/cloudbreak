package com.sequenceiq.freeipa.converter.instance;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.converter.instance.template.TemplateToInstanceTemplateResponseConverter;
import com.sequenceiq.freeipa.entity.InstanceGroup;

@Component
public class InstanceGroupToInstanceGroupResponseConverter implements Converter<InstanceGroup, InstanceGroupResponse> {

    @Inject
    private TemplateToInstanceTemplateResponseConverter templateResponseConverter;

    @Inject
    private SecurityGroupToSecurityGroupResponseConverter securityGroupConverter;

    @Inject
    private InstanceGroupNetworkToInstanceGroupNetworkResponseConverter instanceGroupNetworkConverter;

    @Inject
    private InstanceMetaDataToInstanceMetaDataResponseConverter metaDataConverter;

    @Override
    public InstanceGroupResponse convert(InstanceGroup source) {
        InstanceGroupResponse instanceGroupResponse = new InstanceGroupResponse();
        instanceGroupResponse.setName(source.getGroupName());
        if (source.getTemplate() != null) {
            instanceGroupResponse.setInstanceTemplate(templateResponseConverter.convert(source.getTemplate()));
        }
        instanceGroupResponse.setMetaData(metaDataConverter.convert(source.getNotTerminatedInstanceMetaDataSet()));
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

    public List<InstanceGroupResponse> convert(Iterable<InstanceGroup> source) {
        return StreamSupport.stream(source.spliterator(), false).map(this::convert).collect(Collectors.toList());
    }
}
