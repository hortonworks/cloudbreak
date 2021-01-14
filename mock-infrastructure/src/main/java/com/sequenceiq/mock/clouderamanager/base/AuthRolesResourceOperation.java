package com.sequenceiq.mock.clouderamanager.base;

import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerDto;
import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiAuthRoleMetadata;
import com.sequenceiq.mock.swagger.model.ApiAuthRoleMetadataList;

@Controller
public class AuthRolesResourceOperation {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    public ResponseEntity<ApiAuthRoleMetadataList> readAuthRolesMetadata(String mockUuid, @Valid String view) {
        ClouderaManagerDto dto = clouderaManagerStoreService.read(mockUuid);
        List<ApiAuthRoleMetadata> collect = dto.getUsers().stream().flatMap(u -> u.getAuthRoles().stream())
                .map(r -> new ApiAuthRoleMetadata()
                        .role("ROLE_ADMIN")
                        .uuid(r.getUuid())
                        .displayName(r.getDisplayName()))
                .collect(Collectors.toList());
        return responseCreatorComponent.exec(new ApiAuthRoleMetadataList().items(collect));
    }
}
