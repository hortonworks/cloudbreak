package com.sequenceiq.provisioning.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.TemplateJson;
import com.sequenceiq.provisioning.controller.validation.AzureTemplateParam;
import com.sequenceiq.provisioning.domain.AzureTemplate;
import com.sequenceiq.provisioning.domain.CloudPlatform;

@Component
public class AzureTemplateConverter extends AbstractConverter<TemplateJson, AzureTemplate> {

    @Override
    public TemplateJson convert(AzureTemplate entity) {
        TemplateJson azureTemplateJson = new TemplateJson();
        azureTemplateJson.setClusterName(entity.getName());
        azureTemplateJson.setCloudPlatform(CloudPlatform.AZURE);
        azureTemplateJson.setId(entity.getId());
        Map<String, String> props = new HashMap<>();
        putProperty(props, AzureTemplateParam.LOCATION.getName(), entity.getLocation());
        putProperty(props, AzureTemplateParam.DESCRIPTION.getName(), entity.getDescription());
        putProperty(props, AzureTemplateParam.ADDRESSPREFIX.getName(), entity.getSubnetAddressPrefix());
        putProperty(props, AzureTemplateParam.DEPLOYMENTSLOT.getName(), entity.getDeploymentSlot());
        putProperty(props, AzureTemplateParam.IMAGENAME.getName(), entity.getImageName());
        putProperty(props, AzureTemplateParam.VMTYPE.getName(), entity.getVmType());
        putProperty(props, AzureTemplateParam.USERNAME.getName(), entity.getUserName());
        putProperty(props, AzureTemplateParam.PASSWORD.getName(), entity.getPassword());
        putProperty(props, AzureTemplateParam.SSH_PUBLIC_KEY_FINGERPRINT.getName(), entity.getSshPublicKeyFingerprint());
        putProperty(props, AzureTemplateParam.SSH_PUBLIC_KEY_PATH.getName(), entity.getSshPublicKeyPath());
        putProperty(props, AzureTemplateParam.SUBNETADDRESSPREFIX.getName(), entity.getSubnetAddressPrefix());
        putProperty(props, AzureTemplateParam.PORTS.getName(), entity.getPorts());
        azureTemplateJson.setCloudPlatform(CloudPlatform.AZURE);
        azureTemplateJson.setParameters(props);
        return azureTemplateJson;
    }

    @Override
    public AzureTemplate convert(TemplateJson json) {
        AzureTemplate azureTemplate = new AzureTemplate();
        azureTemplate.setName(json.getClusterName());
        azureTemplate.setDeploymentSlot(json.getParameters().get(AzureTemplateParam.DEPLOYMENTSLOT.getName()));
        azureTemplate.setDescription(json.getParameters().get(AzureTemplateParam.DESCRIPTION.getName()));
        azureTemplate.setImageName(json.getParameters().get(AzureTemplateParam.IMAGENAME.getName()));
        azureTemplate.setLocation(json.getParameters().get(AzureTemplateParam.LOCATION.getName()));
        azureTemplate.setName(json.getClusterName());
        azureTemplate.setPassword(json.getParameters().get(AzureTemplateParam.PASSWORD.getName()));
        azureTemplate.setSshPublicKeyFingerprint(json.getParameters().get(AzureTemplateParam.SSH_PUBLIC_KEY_FINGERPRINT.getName()));
        azureTemplate.setSshPublicKeyPath(json.getParameters().get(AzureTemplateParam.SSH_PUBLIC_KEY_PATH.getName()));
        azureTemplate.setAddressPrefix(json.getParameters().get(AzureTemplateParam.ADDRESSPREFIX.getName()));
        azureTemplate.setUserName(json.getParameters().get(AzureTemplateParam.USERNAME.getName()));
        azureTemplate.setVmType(json.getParameters().get(AzureTemplateParam.VMTYPE.getName()));
        azureTemplate.setSubnetAddressPrefix(json.getParameters().get(AzureTemplateParam.SUBNETADDRESSPREFIX.getName()));
        azureTemplate.setPorts(json.getParameters().get(AzureTemplateParam.PORTS.getName()));
        return azureTemplate;
    }

    private void putProperty(Map<String, String> props, String key, Object property) {
        if (property != null) {
            props.put(key, property.toString());
        }
    }

}
