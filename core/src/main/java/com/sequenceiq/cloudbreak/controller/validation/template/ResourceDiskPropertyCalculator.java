package com.sequenceiq.cloudbreak.controller.validation.template;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.VmType;
import com.sequenceiq.cloudbreak.cloud.model.instance.AzureInstanceTemplate;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.dto.credential.Credential;
import com.sequenceiq.cloudbreak.service.template.TemplateService;

@Service
public class ResourceDiskPropertyCalculator {

    @Inject
    private TemplateService templateService;

    public void updateWithResourceDiskAttached(Credential credential, Template template, VmType vmType) {
        if (credential.cloudPlatform().equalsIgnoreCase(CloudPlatform.AZURE.name())) {
            if (template.getCloudPlatform() != null && template.getAttributes() != null) {
                Json attributesJson = template.getAttributes();
                Map<String, Object> attributes = Optional.ofNullable(attributesJson).map(Json::getMap).orElseGet(HashMap::new);
                attributes.put(AzureInstanceTemplate.RESOURCE_DISK_ATTACHED, vmType.getMetaData().getResourceDiskAttached());
                template.setAttributes(new Json(attributes));
                templateService.savePure(template);
            }
        }
    }

}
