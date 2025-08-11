package com.sequenceiq.cloudbreak.cm.converter;

import static com.sequenceiq.cloudbreak.util.NullUtil.doIfNotNull;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateHostTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateInstantiator;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.cm.exception.ClouderaManagerOperationFailedException;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Service
public class ApiClusterTemplateToCmTemplateConverter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiClusterTemplateToCmTemplateConverter.class);

    public String convert(ApiClusterTemplate apiClusterTemplate, String extendedBlueprintText) {
        ApiClusterTemplate newApiClusterTemplate = new ApiClusterTemplate();
        doIfNotNull(apiClusterTemplate.getDisplayName(), newApiClusterTemplate::setDisplayName);
        doIfNotNull(apiClusterTemplate.getCdhVersion(), newApiClusterTemplate::setCdhVersion);
        doIfNotNull(apiClusterTemplate.getServices(), newApiClusterTemplate::setServices);
        doIfNotNull(apiClusterTemplate.getHostTemplates(), newApiClusterTemplate::setHostTemplates);
        doIfNotNull(apiClusterTemplate.getTags(), newApiClusterTemplate::setTags);
        doIfNotNull(apiClusterTemplate.getRepositories(), newApiClusterTemplate::setRepositories);
        doIfNotNull(apiClusterTemplate.getCmVersion(), newApiClusterTemplate::setCmVersion);
        doIfNotNull(apiClusterTemplate.getClusterSpec(), newApiClusterTemplate::setClusterSpec);
        doIfNotNull(apiClusterTemplate.getProducts(), newApiClusterTemplate::setProducts);
        doIfNotNull(apiClusterTemplate.getDataServicesVersion(), newApiClusterTemplate::setDataServicesVersion);
        newApiClusterTemplate.setInstantiator(getApiClusterTemplateInstantiatorFromExtendedBlueprintText(extendedBlueprintText));
        return JsonUtil.writeValueAsStringSilent(newApiClusterTemplate, true);
    }

    private ApiClusterTemplateInstantiator getApiClusterTemplateInstantiatorFromExtendedBlueprintText(String extendedBlueprintText) {
        try {
            ApiClusterTemplate previousClusterTemplates = JsonUtil.readValue(extendedBlueprintText, ApiClusterTemplate.class);
            return previousClusterTemplates.getInstantiator();
        } catch (Exception e) {
            String msg = "Failed to read extended blueprint to gather ApiClusterTemplateInstantiator for the new template setup";
            LOGGER.warn(msg, e);
            throw new ClouderaManagerOperationFailedException(msg, e);
        }
    }

    private List<ApiClusterTemplateHostTemplate> getHostTemplates(ApiClusterTemplate apiClusterTemplate) {
        return apiClusterTemplate.getHostTemplates();
    }

    private List<ApiClusterTemplateService> getServices(ApiClusterTemplate apiClusterTemplate) {
        return apiClusterTemplate.getServices();
    }

}
