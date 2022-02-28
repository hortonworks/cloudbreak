package com.sequenceiq.cloudbreak.service.stack;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("cb.stoprestriction")
public class StackStopRestrictionConfiguration {

    private String restrictedCloudPlatform;

    private String ephemeralCachingMinVersion;

    private String ephemeralOnlyMinVersion;

    private List<ServiceRoleGroup> permittedServiceRoleGroups;

    public String getRestrictedCloudPlatform() {
        return restrictedCloudPlatform;
    }

    public void setRestrictedCloudPlatform(String restrictedCloudPlatform) {
        this.restrictedCloudPlatform = restrictedCloudPlatform;
    }

    public String getEphemeralCachingMinVersion() {
        return ephemeralCachingMinVersion;
    }

    public void setEphemeralCachingMinVersion(String ephemeralCachingMinVersion) {
        this.ephemeralCachingMinVersion = ephemeralCachingMinVersion;
    }

    public String getEphemeralOnlyMinVersion() {
        return ephemeralOnlyMinVersion;
    }

    public void setEphemeralOnlyMinVersion(String ephemeralOnlyMinVersion) {
        this.ephemeralOnlyMinVersion = ephemeralOnlyMinVersion;
    }

    public List<ServiceRoleGroup> getPermittedServiceRoleGroups() {
        return permittedServiceRoleGroups;
    }

    public void setPermittedServiceRoleGroups(List<ServiceRoleGroup> permittedServiceRoleGroups) {
        this.permittedServiceRoleGroups = permittedServiceRoleGroups;
    }

    public static class ServiceRoleGroup {

        private String name;

        private Set<ServiceRole> serviceRoles;

        private Set<ServiceRole> roles;

        public Set<ServiceRole> getRequiredServiceRoles() {
            return serviceRoles.stream()
                    .filter(ServiceRole::getRequired)
                    .collect(Collectors.toSet());
        }

        public Set<ServiceRole> getRequiredRoles() {
            return roles.stream()
                    .filter(ServiceRole::getRequired)
                    .collect(Collectors.toSet());
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Set<ServiceRole> getServiceRoles() {
            return serviceRoles;
        }

        public void setServiceRoles(Set<ServiceRole> serviceRoles) {
            this.serviceRoles = serviceRoles;
        }

        public Set<ServiceRole> getRoles() {
            return roles;
        }

        public void setRoles(Set<ServiceRole> roles) {
            this.roles = roles;
        }

        public static class ServiceRole {

            private String service;

            private String role;

            private boolean required;

            public String getService() {
                return service;
            }

            public void setService(String service) {
                this.service = service;
            }

            public String getRole() {
                return role;
            }

            public void setRole(String role) {
                this.role = role;
            }

            public boolean getRequired() {
                return required;
            }

            public void setRequired(boolean required) {
                this.required = required;
            }
        }
    }
}

