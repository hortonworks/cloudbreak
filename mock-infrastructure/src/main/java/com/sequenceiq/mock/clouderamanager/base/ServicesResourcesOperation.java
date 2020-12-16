package com.sequenceiq.mock.clouderamanager.base;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.DataProviderService;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.mock.swagger.model.ApiConfig;
import com.sequenceiq.mock.swagger.model.ApiServiceConfig;
import com.sequenceiq.mock.swagger.model.ApiServiceList;

@Controller
public class ServicesResourcesOperation {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    @Inject
    private DataProviderService dataProviderService;

    public ResponseEntity<ApiServiceList> readServices(String mockUuid, String clusterName, @Valid String view) {
        ApiServiceList response = dataProviderService.readServices(mockUuid, clusterName, view);
        return responseCreatorComponent.exec(response);
    }

    public ResponseEntity<ApiServiceConfig> readServiceConfig(String mockUuid, String clusterName, String serviceName, @Valid String view) {
        Optional<ApiClusterTemplateService> service = clouderaManagerStoreService.getService(mockUuid, serviceName);

        ApiServiceConfig response = null;
        if (service.isPresent()) {
            response = new ApiServiceConfig();
            List<ApiConfig> apiConfigs = service.get().getServiceConfigs().stream()
                    .map(sc -> new ApiConfig().name(sc.getName()).value(sc.getValue()))
                    .collect(Collectors.toList());
            response.setItems(apiConfigs);
        }
        return responseCreatorComponent.exec(response);
    }
}
