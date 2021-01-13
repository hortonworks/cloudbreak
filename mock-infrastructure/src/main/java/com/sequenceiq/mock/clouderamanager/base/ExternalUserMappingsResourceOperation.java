package com.sequenceiq.mock.clouderamanager.base;

import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.sequenceiq.mock.clouderamanager.ClouderaManagerDto;
import com.sequenceiq.mock.clouderamanager.ClouderaManagerStoreService;
import com.sequenceiq.mock.clouderamanager.ResponseCreatorComponent;
import com.sequenceiq.mock.swagger.model.ApiExternalUserMapping;
import com.sequenceiq.mock.swagger.model.ApiExternalUserMappingList;

@Service
public class ExternalUserMappingsResourceOperation {

    @Inject
    private ResponseCreatorComponent responseCreatorComponent;

    @Inject
    private ClouderaManagerStoreService clouderaManagerStoreService;

    public ResponseEntity<ApiExternalUserMappingList> createExternalUserMappings(String mockUuid, @Valid ApiExternalUserMappingList body) {
        ClouderaManagerDto dto = clouderaManagerStoreService.read(mockUuid);
        List<ApiExternalUserMapping> items = body.getItems();
        items.forEach(u -> u.uuid(UUID.randomUUID().toString()));
        dto.getExternalUsers().addAll(items);
        return responseCreatorComponent.exec(body);
    }

    public ResponseEntity<ApiExternalUserMapping> deleteExternalUserMapping(String mockUuid, String uuid) {
        ClouderaManagerDto dto = clouderaManagerStoreService.read(mockUuid);
        return responseCreatorComponent.exec(getUser(dto, uuid));
    }

    public ResponseEntity<ApiExternalUserMapping> readExternalUserMapping(String mockUuid, String uuid) {
        ClouderaManagerDto dto = clouderaManagerStoreService.read(mockUuid);
        return responseCreatorComponent.exec(getUser(dto, uuid));
    }

    public ResponseEntity<ApiExternalUserMappingList> readExternalUserMappings(String mockUuid, @Valid String view) {
        ClouderaManagerDto dto = clouderaManagerStoreService.read(mockUuid);
        return responseCreatorComponent.exec(new ApiExternalUserMappingList().items(dto.getExternalUsers()));
    }

    public ResponseEntity<ApiExternalUserMapping> updateExternalUserMapping(String mockUuid, String uuid, @Valid ApiExternalUserMapping body) {
        ClouderaManagerDto dto = clouderaManagerStoreService.read(mockUuid);
        ApiExternalUserMapping user = getUser(dto, uuid);
        user.setAuthRoles(body.getAuthRoles());
        return responseCreatorComponent.exec(body);
    }

    private ApiExternalUserMapping getUser(ClouderaManagerDto dto, String uuid) {
        return dto.getExternalUsers().stream().filter(u -> u.getUuid().equals(uuid)).findFirst().orElse(null);
    }
}
