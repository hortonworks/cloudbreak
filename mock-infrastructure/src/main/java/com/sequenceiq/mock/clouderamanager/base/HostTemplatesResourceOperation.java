package com.sequenceiq.mock.clouderamanager.base;

import javax.inject.Inject;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiHostTemplate;
import com.sequenceiq.mock.swagger.model.ApiHostTemplateList;

@Controller
public class HostTemplatesResourceOperation {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @Inject
    private DataProviderService dataProviderService;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    public ResponseEntity<ApiHostTemplate> readHostTemplate(String mockUuid, String clusterName, String hostTemplateName) {
        ApiHostTemplate apiHostTemplate = dataProviderService.getApiHostTemplate(hostTemplateName, hostTemplateName.toUpperCase());
        return responseCreatorComponent.exec(apiHostTemplate);
    }

    public ResponseEntity<ApiHostTemplateList> readHostTemplates(String mockUuid, String clusterName) {
        ApiHostTemplateList apiHostTemplateList = dataProviderService.hostTemplates(mockUuid);
        return responseCreatorComponent.exec(apiHostTemplateList);
    }

}
