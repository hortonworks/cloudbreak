package com.sequenceiq.cloudbreak.converter;

import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.TemplateResponse;
import com.sequenceiq.cloudbreak.api.model.v2.template.AwsTemplateParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.AzureTemplateParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.BaseTemplateParameter;
import com.sequenceiq.cloudbreak.api.model.v2.template.Encryption;
import com.sequenceiq.cloudbreak.api.model.v2.template.GcpTemplateParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.OpenStackTemplateParameters;
import com.sequenceiq.cloudbreak.api.model.v2.template.TemplatePlatformType;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.json.Json;

@Component
public class TemplateToTemplateResponseConverter extends AbstractConversionServiceAwareConverter<Template, TemplateResponse> {
    @Override
    public TemplateResponse convert(Template source) {
        TemplateResponse templateJson = new TemplateResponse();
        templateJson.setId(source.getId());
        templateJson.setName(source.getName());
        templateJson.setVolumeCount(source.getVolumeCount());
        templateJson.setVolumeSize(source.getVolumeSize());
        templateJson.setPublicInAccount(source.isPublicInAccount());
        templateJson.setInstanceType(source.getInstanceType());
        templateJson.setVolumeType(source.getVolumeType());
        templateJson.setRootVolumeSize(source.getRootVolumeSize());
        Json attributes = source.getAttributes();
        if (attributes != null) {
            Map<String, Object> atributesMap = attributes.getMap();
            templateJson.setParameters(atributesMap);

            Map<String, Object> map = atributesMap;

            Object platformType = map.get(BaseTemplateParameter.PLATFORM_TYPE);
            if (platformType != null) {
                switch (TemplatePlatformType.valueOf(platformType.toString())) {
                    case AWS:
                        templateJson.setAwsTemplateParameters(getAwsTemplateParameter(map));
                        break;
                    case OS:
                        templateJson.setOpenStackTemplateParameters(getOpenStackTemplateParameter(map));
                        break;
                    case AZURE:
                        templateJson.setAzureTemplateParameters(getAzureTemplateParameter(map));
                        break;
                    case GCP:
                        templateJson.setGcpTemlateParameters(getGcpTemplateParameter(map));
                        break;
                    default:
                }
            }
        }
        templateJson.setCloudPlatform(source.cloudPlatform());
        templateJson.setDescription(source.getDescription() == null ? "" : source.getDescription());
        if (source.getTopology() != null) {
            templateJson.setTopologyId(source.getTopology().getId());
        }
        return templateJson;
    }

    private GcpTemplateParameters getGcpTemplateParameter(Map<String, Object> map) {
        GcpTemplateParameters ret = new GcpTemplateParameters();
        fillBaseTemplateParameters(map, ret);
        return ret;
    }

    private AzureTemplateParameters getAzureTemplateParameter(Map<String, Object> map) {
        AzureTemplateParameters ret = new AzureTemplateParameters();
        ret.setPrivateId(Objects.toString(map.get("privateId"), null));
        fillBaseTemplateParameters(map, ret);
        return ret;
    }

    private OpenStackTemplateParameters getOpenStackTemplateParameter(Map<String, Object> map) {
        OpenStackTemplateParameters ret = new OpenStackTemplateParameters();
        fillBaseTemplateParameters(map, ret);
        return ret;
    }

    private AwsTemplateParameters getAwsTemplateParameter(Map<String, Object> map) {
        AwsTemplateParameters ret = new AwsTemplateParameters();
        Object spotPrice = map.get("spotPrice");
        if (spotPrice != null) {
            ret.setSpotPrice(Double.valueOf(spotPrice.toString()));
        }

        fillBaseTemplateParameters(map, ret);
        return ret;
    }

    private void fillBaseTemplateParameters(Map<String, Object> map, BaseTemplateParameter templateParameter) {
        Object key = map.get("key");
        Object type = map.get("type");
        Object encrypted = map.get("encrypted");

        if (key != null || type != null) {
            Encryption encryption = new Encryption();
            encryption.setKey(Objects.toString(key, null));
            encryption.setType(Objects.toString(type, null));
            templateParameter.setEncryption(encryption);
        }
        if (encrypted != null) {
            templateParameter.setEncrypted(Boolean.TRUE.equals(encrypted));
        }
    }
}
