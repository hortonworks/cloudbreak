package com.sequenceiq.cloudbreak.cm.converter;

import java.util.List;

import org.springframework.stereotype.Service;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateHostTemplate;
import com.cloudera.api.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;

@Service
public class ApiClusterTemplateToCmTemplateConverter {

    public String convert(ApiClusterTemplate apiClusterTemplate) {
        ApiClusterTemplate newApiClusterTemplate = new ApiClusterTemplate();
        newApiClusterTemplate.setDisplayName(apiClusterTemplate.getDisplayName());
        newApiClusterTemplate.setCdhVersion(apiClusterTemplate.getCdhVersion());
        newApiClusterTemplate.setServices(getServices(apiClusterTemplate));
        newApiClusterTemplate.setHostTemplates(getHostTemplates(apiClusterTemplate));
        return JsonUtil.writeValueAsStringSilent(newApiClusterTemplate, true);
    }

    private List<ApiClusterTemplateHostTemplate> getHostTemplates(ApiClusterTemplate apiClusterTemplate) {
        return apiClusterTemplate.getHostTemplates();
    }

    private List<ApiClusterTemplateService> getServices(ApiClusterTemplate apiClusterTemplate) {
        return apiClusterTemplate.getServices();
    }

}
