package com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup;

import static com.sequenceiq.cloudbreak.util.NullUtil.getIfNotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.InstanceGroupV4Request;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.ProviderParameterCalculator;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.network.InstanceGroupNetworkV4RequestToInstanceGroupNetworkConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.securitygroup.SecurityGroupV4RequestToSecurityGroupConverter;
import com.sequenceiq.cloudbreak.converter.v4.stacks.instancegroup.template.InstanceTemplateV4RequestToTemplateConverter;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.instance.network.InstanceGroupNetwork;
import com.sequenceiq.common.api.type.ScalabilityOption;

@Component
public class InstanceGroupV4RequestToInstanceGroupConverter {

    @Inject
    private ProviderParameterCalculator providerParameterCalculator;

    @Inject
    private SecurityGroupV4RequestToSecurityGroupConverter securityGroupV4RequestToSecurityGroupConverter;

    @Inject
    private InstanceGroupNetworkV4RequestToInstanceGroupNetworkConverter instanceGroupNetworkV4RequestToInstanceGroupNetworkConverter;

    @Inject
    private InstanceTemplateV4RequestToTemplateConverter instanceTemplateV4RequestToTemplateConverter;

    public InstanceGroup convert(InstanceGroupV4Request source, String variant) {
        InstanceGroup instanceGroup = new InstanceGroup();
        source.getTemplate().setCloudPlatform(source.getCloudPlatform());
        instanceGroup.setTemplate(instanceTemplateV4RequestToTemplateConverter.convert(source.getTemplate()));
        instanceGroup.setSecurityGroup(getIfNotNull(source.getSecurityGroup(), securityGroupV4RequestToSecurityGroupConverter
                ::convert));
        instanceGroup.setGroupName(source.getName().toLowerCase());
        instanceGroup.setMinimumNodeCount(source.getMinimumNodeCount() == null ? 0 : source.getMinimumNodeCount());
        setAttributes(source, instanceGroup);
        instanceGroup.setInstanceGroupType(source.getType());
        instanceGroup.setInitialNodeCount(source.getNodeCount());
        instanceGroup.setScalabilityOption(source.getScalabilityOption() == null ? ScalabilityOption.ALLOWED : source.getScalabilityOption());
        setNetwork(source, instanceGroup);
        if (source.getNodeCount() > 0) {
            addInstanceMetadatas(source, instanceGroup, variant);
        }
        return instanceGroup;
    }

    private void setNetwork(InstanceGroupV4Request source, InstanceGroup instanceGroup) {
        if (source.getNetwork() != null) {
            source.getNetwork().setCloudPlatform(source.getCloudPlatform());
            InstanceGroupNetwork instanceGroupNetwork = instanceGroupNetworkV4RequestToInstanceGroupNetworkConverter
                    .convert(source.getNetwork());
            instanceGroup.setInstanceGroupNetwork(instanceGroupNetwork);
        }
    }

    private void addInstanceMetadatas(InstanceGroupV4Request request, InstanceGroup instanceGroup, String variant) {
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        for (int i = 0; i < request.getNodeCount(); i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaData.setVariant(variant);
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
