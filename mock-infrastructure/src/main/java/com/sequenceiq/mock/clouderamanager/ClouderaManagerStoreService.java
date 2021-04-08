package com.sequenceiq.mock.clouderamanager;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sequenceiq.mock.swagger.model.ApiAuthRoleRef;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplate;
import com.sequenceiq.mock.swagger.model.ApiClusterTemplateService;
import com.sequenceiq.mock.swagger.model.ApiProductVersion;
import com.sequenceiq.mock.swagger.model.ApiServiceState;
import com.sequenceiq.mock.swagger.model.ApiUser2;
import com.sequenceiq.mock.swagger.model.ApiUser2List;

@Service
public class ClouderaManagerStoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClouderaManagerStoreService.class);

    private Map<String, ClouderaManagerDto> cmDtos = new ConcurrentHashMap<>();

    public List<ApiProductVersion> getClouderaManagerProducts(String mockUuid) {
        LOGGER.info("get cm products by {}", mockUuid);
        return read(mockUuid).getClusterTemplate().getProducts();
    }

    public void terminate(String mockUuid) {
        LOGGER.info("terminate cm by {}", mockUuid);
        cmDtos.remove(mockUuid);
    }

    public Collection<ClouderaManagerDto> getAll() {
        return cmDtos.values();
    }

    public ApiUser2List addUsers(String mockUuid, ApiUser2List body) {
        LOGGER.info("add user in cm for {}, user: {}", mockUuid, body);
        List<ApiUser2> users = read(mockUuid).getUsers();
        body.getItems().forEach(u -> addUser(users, u));
        return body;
    }

    private void addUser(List<ApiUser2> users, ApiUser2 user) {
        Optional<ApiUser2> first = users.stream()
                .filter(s -> s.getName().equals(user.getName()))
                .findFirst();
        if (first.isEmpty()) {
            users.add(user);
        }
    }

    public ApiUser2List getUserList(String mockUuid) {
        LOGGER.info("read user list in cm for {}", mockUuid);
        List<ApiUser2> users = read(mockUuid).getUsers();
        return new ApiUser2List().items(users);
    }

    public ClouderaManagerDto start(String mockUuid) {
        LOGGER.info("start cm for {}", mockUuid);
        ClouderaManagerDto clouderaManagerDto = cmDtos.computeIfAbsent(mockUuid, key -> new ClouderaManagerDto(mockUuid));
        ApiAuthRoleRef authRoleRef = new ApiAuthRoleRef().displayName("Full Administrator").uuid(UUID.randomUUID().toString());
        ApiUser2 admin = new ApiUser2().name("admin").addAuthRolesItem(authRoleRef);
        addUser(clouderaManagerDto.getUsers(), admin);
        return clouderaManagerDto;
    }

    public ClouderaManagerDto read(String mockUuid) {
        LOGGER.info("read cm for {}", mockUuid);
        ClouderaManagerDto clouderaManagerDto = cmDtos.get(mockUuid);
        if (clouderaManagerDto == null) {
            LOGGER.info("cannot find cm for {}", mockUuid);
            throw new ResponseStatusException(NOT_FOUND, "ClouderaManagerDto cannot be found by uuid: " + mockUuid);
        }
        return clouderaManagerDto;
    }

    public boolean exists(String mockUuid) {
        return cmDtos.get(mockUuid) != null;
    }

    public ApiUser2 updateUser(String mockUuid, String userName, ApiUser2 body) {
        LOGGER.info("update user in cm for {}, user: {}", mockUuid, body);
        ApiUser2 apiUser2 = getApiUser2(mockUuid, userName);
        apiUser2.setAuthRoles(body.getAuthRoles());
        apiUser2.setPassword(body.getPassword());
        return apiUser2;
    }

    private ApiUser2 getApiUser2(String mockUuid, String userName) {
        LOGGER.info("read user by {} in cm for {}", userName, mockUuid);
        ClouderaManagerDto read = read(mockUuid);
        Optional<ApiUser2> userOpt = read.getUsers().stream().filter(u -> u.getName().equals(userName)).findFirst();
        if (userOpt.isEmpty()) {
            LOGGER.info("User cannot be found by {} in cm for {}", userName, mockUuid);
            throw new ResponseStatusException(NOT_FOUND, "User cannot be found by username: " + userName);
        }
        return userOpt.get();
    }

    public ApiUser2 removeUser(String mockUuid, String userName) {
        LOGGER.info("remove user by {} in cm for {}", userName, mockUuid);
        ApiUser2 apiUser2 = getApiUser2(mockUuid, userName);
        read(mockUuid).getUsers().remove(apiUser2);
        return apiUser2;
    }

    public void importClusterTemplate(String mockUuid, ApiClusterTemplate body) {
        LOGGER.info("importClusterTemplate cm for {}. Template: {}", mockUuid, body);
        ClouderaManagerDto cmDto = read(mockUuid);
        cmDto.setClusterTemplate(body);
        body.getServices().forEach(s -> cmDto.getServiceStates().put(s.getRefName(), ApiServiceState.STARTED));
    }

    public Optional<ApiClusterTemplateService> getService(String mockUuid, String serviceName) {
        LOGGER.info("read service by {} in cm for {}", serviceName, mockUuid);
        return read(mockUuid).getClusterTemplate().getServices().stream()
                .filter(s -> s.getRefName().equals(serviceName))
                .findFirst();
    }
}
