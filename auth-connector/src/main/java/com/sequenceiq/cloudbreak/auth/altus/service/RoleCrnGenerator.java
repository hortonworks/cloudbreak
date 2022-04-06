package com.sequenceiq.cloudbreak.auth.altus.service;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;

@Service
public class RoleCrnGenerator {

    @Inject
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    public String getBuiltInDatabusRoleCrn() {
        return getRoleCrn("DbusUploader").toString();
    }

    public String getBuiltInOwnerResourceRoleCrn() {
        return getResourceRoleCrn("Owner").toString();
    }

    public String getBuiltInEnvironmentAdminResourceRoleCrn() {
        return getResourceRoleCrn("EnvironmentAdmin").toString();
    }

    public String getBuiltInEnvironmentUserResourceRoleCrn() {
        return getResourceRoleCrn("EnvironmentUser").toString();
    }

    public Crn getResourceRoleCrn(String resourceRoleName) {
        // we need to find out the proper partition and region in case of every cdp deployment, we stick to altus partition and current region
        return regionAwareCrnGenerator.generateAltusCrn(CrnResourceDescriptor.RESOURCE_ROLE, resourceRoleName);
    }

    public Crn getRoleCrn(String roleName) {
        // we need to find out the proper partition and region in case of every cdp deployment, we stick to altus partition and current region
        return regionAwareCrnGenerator.generateAltusCrn(CrnResourceDescriptor.ROLE, roleName);
    }
}
