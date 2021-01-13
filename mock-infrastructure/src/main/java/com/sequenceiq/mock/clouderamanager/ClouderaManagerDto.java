package com.sequenceiq.mock.clouderamanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sequenceiq.mock.swagger.model.ApiClusterTemplate;
import com.sequenceiq.mock.swagger.model.ApiExternalUserMapping;
import com.sequenceiq.mock.swagger.model.ApiServiceState;
import com.sequenceiq.mock.swagger.model.ApiUser2;

public class ClouderaManagerDto {

    private String mockUuid;

    private List<ApiUser2> users = new ArrayList<>();

    private List<ApiExternalUserMapping> externalUsers = new ArrayList<>();

    private ApiClusterTemplate clusterTemplate;

    private Map<String, ApiServiceState> serviceStates = new HashMap<>();

    private ApiServiceState status = ApiServiceState.NA;

    private List<CmProfile> activeProfiles = new ArrayList<>();

    public ClouderaManagerDto(String mockUuid) {
        this.mockUuid = mockUuid;
    }

    public String getMockUuid() {
        return mockUuid;
    }

    public List<ApiUser2> getUsers() {
        return users;
    }

    public void setUsers(List<ApiUser2> users) {
        this.users = users;
    }

    public ApiClusterTemplate getClusterTemplate() {
        return clusterTemplate;
    }

    public void setClusterTemplate(ApiClusterTemplate clusterTemplate) {
        this.clusterTemplate = clusterTemplate;
    }

    public Map<String, ApiServiceState> getServiceStates() {
        return serviceStates;
    }

    public void setServiceStates(Map<String, ApiServiceState> serviceStates) {
        this.serviceStates = serviceStates;
    }

    public ApiServiceState getStatus() {
        return status;
    }

    public void setStatus(ApiServiceState status) {
        this.status = status;
    }

    public List<CmProfile> getActiveProfiles() {
        return activeProfiles;
    }

    public List<ApiExternalUserMapping> getExternalUsers() {
        return externalUsers;
    }

    public void setExternalUsers(List<ApiExternalUserMapping> externalUsers) {
        this.externalUsers = externalUsers;
    }
}
