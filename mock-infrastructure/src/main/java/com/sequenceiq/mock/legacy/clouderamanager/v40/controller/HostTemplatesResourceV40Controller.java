package com.sequenceiq.mock.legacy.clouderamanager.v40.controller;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.legacy.clouderamanager.DataProviderService;
import com.sequenceiq.mock.legacy.clouderamanager.DefaultModelService;
import com.sequenceiq.mock.legacy.clouderamanager.ProfileAwareResponse;
import com.sequenceiq.mock.legacy.clouderamanager.ResponseUtil;
import com.sequenceiq.mock.swagger.model.ApiCommand;
import com.sequenceiq.mock.swagger.model.ApiHostRefList;
import com.sequenceiq.mock.swagger.model.ApiHostTemplate;
import com.sequenceiq.mock.swagger.model.ApiHostTemplateList;
import com.sequenceiq.mock.swagger.v40.api.HostTemplatesResourceApi;

@Controller
public class HostTemplatesResourceV40Controller implements HostTemplatesResourceApi {

    @Inject
    private HttpServletRequest request;

    @Inject
    private DefaultModelService defaultModelService;

    @Inject
    private DataProviderService dataProviderService;

    @Override
    public ResponseEntity<ApiCommand> applyHostTemplate(String clusterName, String hostTemplateName, @Valid Boolean startRoles, @Valid ApiHostRefList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiHostTemplateList> createHostTemplates(String clusterName, @Valid ApiHostTemplateList body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiHostTemplate> deleteHostTemplate(String clusterName, String hostTemplateName) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }

    @Override
    public ResponseEntity<ApiHostTemplate> readHostTemplate(String clusterName, String hostTemplateName) {
        ApiHostTemplate apiHostTemplate = dataProviderService.getApiHostTemplate(hostTemplateName, hostTemplateName.toUpperCase());
        return ProfileAwareResponse.exec(apiHostTemplate, defaultModelService);
    }

    @Override
    public ResponseEntity<ApiHostTemplateList> readHostTemplates(String clusterName) {
        ApiHostTemplateList apiHostTemplateList = dataProviderService.hostTemplates();
        return ProfileAwareResponse.exec(apiHostTemplateList, defaultModelService);
    }

    @Override
    public ResponseEntity<ApiHostTemplate> updateHostTemplate(String clusterName, String hostTemplateName, @Valid ApiHostTemplate body) {
        return ResponseUtil.noHandlerFoundResponse(request);
    }
}
