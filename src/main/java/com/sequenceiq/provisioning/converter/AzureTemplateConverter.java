package com.sequenceiq.provisioning.converter;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.sequenceiq.provisioning.controller.json.TemplateJson;
import com.sequenceiq.provisioning.controller.validation.RequiredAzureTemplateParam;
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
        putProperty(props, RequiredAzureTemplateParam.LOCATION.getName(), entity.getLocation());
        putProperty(props, RequiredAzureTemplateParam.DESCRIPTION.getName(), entity.getDescription());
        putProperty(props, RequiredAzureTemplateParam.ADDRESSPREFIX.getName(), entity.getSubnetAddressPrefix());
        putProperty(props, RequiredAzureTemplateParam.DEPLOYMENTSLOT.getName(), entity.getDeploymentSlot());
        putProperty(props, RequiredAzureTemplateParam.IMAGENAME.getName(), entity.getImageName());
        putProperty(props, RequiredAzureTemplateParam.VMTYPE.getName(), entity.getVmType());
        putProperty(props, RequiredAzureTemplateParam.USERNAME.getName(), entity.getUserName());
        putProperty(props, RequiredAzureTemplateParam.PASSWORD.getName(), entity.getPassword());
        putProperty(props, RequiredAzureTemplateParam.SSH_PUBLIC_KEY_FINGERPRINT.getName(), entity.getSshPublicKeyFingerprint());
        putProperty(props, RequiredAzureTemplateParam.SSH_PUBLIC_KEY_PATH.getName(), entity.getSshPublicKeyPath());
        putProperty(props, RequiredAzureTemplateParam.SUBNETADDRESSPREFIX.getName(), entity.getSubnetAddressPrefix());
        azureTemplateJson.setCloudPlatform(CloudPlatform.AZURE);
        azureTemplateJson.setParameters(props);
        return azureTemplateJson;
    }

    @Override
    public AzureTemplate convert(TemplateJson json) {
        AzureTemplate azureTemplate = new AzureTemplate();
        azureTemplate.setName(json.getClusterName());
        azureTemplate.setDeploymentSlot(json.getParameters().get(RequiredAzureTemplateParam.DEPLOYMENTSLOT.getName()));
        azureTemplate.setDescription(json.getParameters().get(RequiredAzureTemplateParam.DESCRIPTION.getName()));
        azureTemplate.setImageName(json.getParameters().get(RequiredAzureTemplateParam.IMAGENAME.getName()));
        azureTemplate.setLocation(json.getParameters().get(RequiredAzureTemplateParam.LOCATION.getName()));
        azureTemplate.setName(json.getClusterName());
        azureTemplate.setPassword(json.getParameters().get(RequiredAzureTemplateParam.PASSWORD.getName()));
        azureTemplate.setSshPublicKeyFingerprint(json.getParameters().get(RequiredAzureTemplateParam.SSH_PUBLIC_KEY_FINGERPRINT.getName()));
        azureTemplate.setSshPublicKeyPath(json.getParameters().get(RequiredAzureTemplateParam.SSH_PUBLIC_KEY_PATH.getName()));
        azureTemplate.setAddressPrefix(json.getParameters().get(RequiredAzureTemplateParam.ADDRESSPREFIX.getName()));
        azureTemplate.setUserName(json.getParameters().get(RequiredAzureTemplateParam.USERNAME.getName()));
        azureTemplate.setVmType(json.getParameters().get(RequiredAzureTemplateParam.VMTYPE.getName()));
        azureTemplate.setSubnetAddressPrefix(json.getParameters().get(RequiredAzureTemplateParam.SUBNETADDRESSPREFIX.getName()));
        return azureTemplate;
    }

    private void putProperty(Map<String, String> props, String key, Object property) {
        if (property != null) {
            props.put(key, property.toString());
        }
    }

}
