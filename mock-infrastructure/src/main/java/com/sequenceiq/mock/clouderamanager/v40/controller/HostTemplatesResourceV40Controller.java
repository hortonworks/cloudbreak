package com.sequenceiq.mock.clouderamanager.v40.controller;

import jakarta.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.base.HostTemplatesResourceOperation;
import com.sequenceiq.mock.swagger.model.ApiHostTemplate;
import com.sequenceiq.mock.swagger.model.ApiHostTemplateList;
import com.sequenceiq.mock.swagger.v40.api.HostTemplatesResourceApi;

@Controller
public class HostTemplatesResourceV40Controller implements HostTemplatesResourceApi {

    @Inject
    private HostTemplatesResourceOperation hostTemplatesResourceOperation;

    @Override
    public ResponseEntity<ApiHostTemplate> readHostTemplate(String mockUuid, String clusterName, String hostTemplateName) {
        return hostTemplatesResourceOperation.readHostTemplate(mockUuid, clusterName, hostTemplateName);
    }

    @Override
    public ResponseEntity<ApiHostTemplateList> readHostTemplates(String mockUuid, String clusterName) {
        return hostTemplatesResourceOperation.readHostTemplates(mockUuid, clusterName);
    }

}
