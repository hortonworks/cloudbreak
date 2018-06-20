package com.sequenceiq.cloudbreak.converter.stack.instance;

import static com.sequenceiq.cloudbreak.converter.util.ExceptionMessageFormatterUtil.formatAccessDeniedMessage;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sequenceiq.cloudbreak.api.model.stack.instance.InstanceGroupRequest;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Component
public class InstanceGroupRequestToInstanceGroupConverter extends AbstractConversionServiceAwareConverter<InstanceGroupRequest, InstanceGroup> {

    @Inject
    private TemplateService templateService;

    @Inject
    private SecurityGroupService securityGroupService;

    @Override
    public InstanceGroup convert(InstanceGroupRequest json) {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName(json.getGroup());
        instanceGroup.setInstanceGroupType(json.getType());
        setTemplate(json, instanceGroup);
        setSecurityGroup(json, instanceGroup);
        setAttributes(json, instanceGroup);
        addInstanceMetadatas(json, instanceGroup);

        return instanceGroup;
    }

    private void setTemplate(InstanceGroupRequest json, InstanceGroup instanceGroup) {
        if (json.getTemplateId() != null) {
            formatAccessDeniedMessage(
                    () -> instanceGroup.setTemplate(templateService.get(json.getTemplateId())), "template", json.getTemplateId()
            );
        }
        if (json.getTemplate() != null) {
            Template template = getConversionService().convert(json.getTemplate(), Template.class);
            instanceGroup.setTemplate(template);
        }
    }

    private void setSecurityGroup(InstanceGroupRequest json, InstanceGroup instanceGroup) {
        if (json.getSecurityGroupId() != null) {
            formatAccessDeniedMessage(
                    () -> instanceGroup.setSecurityGroup(securityGroupService.get(json.getSecurityGroupId())),
                    "securitygroup", json.getSecurityGroupId()
            );
        }
        if (json.getSecurityGroup() != null) {
            instanceGroup.setSecurityGroup(getConversionService().convert(json.getSecurityGroup(), SecurityGroup.class));
        }
    }

    private void setAttributes(InstanceGroupRequest json, InstanceGroup instanceGroup) {
        try {
            Json jsonProperties = new Json(json.getParameters());
            instanceGroup.setAttributes(jsonProperties);
        } catch (JsonProcessingException ignored) {
            instanceGroup.setAttributes(null);
        }
    }

    private void addInstanceMetadatas(InstanceGroupRequest json, InstanceGroup instanceGroup) {
        Set<InstanceMetaData> instanceMetaDataSet = new HashSet<>();
        for (int i = 0; i < json.getNodeCount(); i++) {
            InstanceMetaData instanceMetaData = new InstanceMetaData();
            instanceMetaData.setInstanceGroup(instanceGroup);
            instanceMetaDataSet.add(instanceMetaData);
        }
        instanceGroup.setInstanceMetaData(instanceMetaDataSet);
    }
}
