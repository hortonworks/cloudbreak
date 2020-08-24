package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.network.InstanceGroupNetworkV4Request;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;

@Component
public class InstanceGroupV4RequestToInstanceGroupConverter extends AbstractConversionServiceAwareConverter<InstanceGroupV4Request, InstanceGroup> {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Override
    public InstanceGroup convert(InstanceGroupV4Request source) {
        InstanceGroup instanceGroup = new InstanceGroup();
        source.getTemplate().setCloudPlatform(source.getCloudPlatform());
        instanceGroup.setTemplate(getConversionService().convert(source.getTemplate(), Template.class));
        instanceGroup.setSecurityGroup(getConversionService().convert(source.getSecurityGroup(), SecurityGroup.class));
        instanceGroup.setGroupName(source.getName());
        setAttributes(source, instanceGroup);
        instanceGroup.setInstanceGroupType(source.getType());
        instanceGroup.setInitialNodeCount(source.getNodeCount());
        if (source.getNodeCount() > 0) {
            addInstanceMetadatas(source, instanceGroup);
        }
        if (source.getNetwork() != null) {
            source.getNetwork().setCloudPlatform(source.getCloudPlatform());
            instanceGroup.setNetwork(getInstanceGroupNetwork(source.getNetwork()));
        }

        return instanceGroup;
    }

    public InstanceGroupNetwork getInstanceGroupNetwork(InstanceGroupNetworkV4Request source) {
        InstanceGroupNetwork instanceGroupNetwork = new InstanceGroupNetwork();
        instanceGroupNetwork.setCloudPlatform(source.getCloudPlatform().name());
        Map<String, Object> parameters = providerParameterCalculator.get(source).asMap();
        if (parameters != null) {
            try {
                instanceGroupNetwork.setAttributes(new Json(parameters));
            } catch (IllegalArgumentException ignored) {
                throw new BadRequestException("Invalid parameters");
            }
        }
        return instanceGroupNetwork;
    }

    private void addInstanceMetadatas(InstanceGroupV4Request request, InstanceGroup instanceGroup) {
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        for (int i = 0; i < request.getNodeCount(); i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaDataSet.add(instanceMetaData);
        }
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);
    }

    private void setAttributes(InstanceGroupV4Request source, InstanceGroup instanceGroup) {
        Map<String, Object> parameters = providerParameterCalculator.get(source).asMap();
        if (parameters != null) {
            try {
                instanceGroup.setAttributes(new Json(parameters));
            } catch (IllegalArgumentException ignored) {
                throw new BadRequestException("Invalid parameters");
            }
        }
    }
}
