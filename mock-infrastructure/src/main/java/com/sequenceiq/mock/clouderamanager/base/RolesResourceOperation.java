package com.sequenceiq.mock.clouderamanager.base;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.mock.swagger.model.ApiRole;
import com.sequenceiq.mock.swagger.model.ApiRoleList;
import com.sequenceiq.mock.swagger.model.ApiServiceRef;

@Controller
public class RolesResourceOperation {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    public ResponseEntity<ApiRoleList> createRoles(String mockUuid, String clusterName, String serviceName, @Valid ApiRoleList body) {
        ApiRoleList roleList = new ApiRoleList().items(List.of(new ApiRole().name("role1").serviceRef(new ApiServiceRef().serviceName(serviceName))));
        return responseCreatorComponent.exec(roleList);
    }

    public ResponseEntity<ApiRole> deleteRole(String mockUuid, String clusterName, String roleName, String serviceName) {
        return responseCreatorComponent.exec(new ApiRole().name(roleName));
    }

    public ResponseEntity<ApiRoleList> readRoles(String mockUuid, String clusterName, String serviceName, @Valid String filter, @Valid String view) {
        Optional<ApiClusterTemplateService> serviceOpt = clouderaManagerStoreService.getService(mockUuid, serviceName);
        ApiRoleList roleList = new ApiRoleList();
        if (serviceOpt.isPresent()) {
            List<ApiRole> apiRoleList = serviceOpt.get().getRoleConfigGroups().stream()
                    .map(r -> new ApiRole()
                            .name(r.getRefName())
                            .type(r.getRoleType())
                            .serviceRef(new ApiServiceRef().serviceName(serviceName)))
                    .collect(Collectors.toList());
            roleList.setItems(apiRoleList);
        }
        return responseCreatorComponent.exec(roleList);
    }
}
