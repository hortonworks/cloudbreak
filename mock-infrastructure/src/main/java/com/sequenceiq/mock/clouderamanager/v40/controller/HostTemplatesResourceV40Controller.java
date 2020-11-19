package com.sequenceiq.mock.clouderamanager.v40.controller;

import javax.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.clouderamanager.ProfileAwareComponent;
import com.sequenceiq.mock.swagger.model.ApiHostTemplate;
import com.sequenceiq.mock.swagger.model.ApiHostTemplateList;
import com.sequenceiq.mock.swagger.v40.api.HostTemplatesResourceApi;

@Controller
public class HostTemplatesResourceV40Controller implements HostTemplatesResourceApi {

    @Inject
    private ProfileAwareComponent profileAwareComponent;

    @Inject
    private DataProviderService dataProviderService;

    @Override
    public ResponseEntity<ApiHostTemplate> readHostTemplate(String mockUuid, String clusterName, String hostTemplateName) {
        ApiHostTemplate apiHostTemplate = dataProviderService.getApiHostTemplate(hostTemplateName, hostTemplateName.toUpperCase());
        return profileAwareComponent.exec(apiHostTemplate);
    }

    @Override
    public ResponseEntity<ApiHostTemplateList> readHostTemplates(String mockUuid, String clusterName) {
        ApiHostTemplateList apiHostTemplateList = dataProviderService.hostTemplates();
        return profileAwareComponent.exec(apiHostTemplateList);
    }

}
