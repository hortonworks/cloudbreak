package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.SecurityGroupEndpoint;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupRequest;
import com.sequenceiq.cloudbreak.api.model.SecurityGroupResponse;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.service.securitygroup.DefaultSecurityGroupCreator;
import com.sequenceiq.cloudbreak.service.securitygroup.SecurityGroupService;

@Component
public class SecurityGroupController implements SecurityGroupEndpoint {
    @Autowired
    @Qualifier("conversionService")
    private ConversionService conversionService;

    @Autowired
    private SecurityGroupService securityGroupService;

    @Autowired
    private DefaultSecurityGroupCreator defaultSecurityGroupCreator;

    @Autowired
    private AuthenticatedUserService authenticatedUserService;

    @Override
    public SecurityGroupResponse postPrivate(SecurityGroupRequest securityGroupRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        return createSecurityGroup(user, securityGroupRequest, false);
    }

    @Override
    public SecurityGroupResponse postPublic(SecurityGroupRequest securityGroupRequest) {
        CbUser user = authenticatedUserService.getCbUser();
        return createSecurityGroup(user, securityGroupRequest, true);
    }

    @Override
    public Set<SecurityGroupResponse> getPrivates() {
        CbUser user = authenticatedUserService.getCbUser();
        defaultSecurityGroupCreator.createDefaultSecurityGroups(user);
        Set<SecurityGroup> securityGroups = securityGroupService.retrievePrivateSecurityGroups(user);
        return convert(securityGroups);
    }

    @Override
    public Set<SecurityGroupResponse> getPublics() {
        CbUser user = authenticatedUserService.getCbUser();
        defaultSecurityGroupCreator.createDefaultSecurityGroups(user);
        Set<SecurityGroup> securityGroups = securityGroupService.retrieveAccountSecurityGroups(user);
        return convert(securityGroups);
    }

    @Override
    public SecurityGroupResponse get(Long id) {
        SecurityGroup securityGroup = securityGroupService.get(id);
        return convert(securityGroup);
    }

    @Override
    public SecurityGroupResponse getPrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        SecurityGroup securityGroup = securityGroupService.getPrivateSecurityGroup(name, user);
        return convert(securityGroup);
    }

    @Override
    public SecurityGroupResponse getPublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        SecurityGroup securityGroup = securityGroupService.getPublicSecurityGroup(name, user);
        return convert(securityGroup);
    }

    @Override
    public void delete(Long id) {
        CbUser user = authenticatedUserService.getCbUser();
        securityGroupService.delete(id, user);
    }

    @Override
    public void deletePublic(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        securityGroupService.delete(name, user);
    }

    @Override
    public void deletePrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        securityGroupService.delete(name, user);
    }

    private SecurityGroupResponse createSecurityGroup(CbUser user, SecurityGroupRequest securityGroupRequest, boolean publicInAccount) {
        SecurityGroup securityGroup = convert(securityGroupRequest, publicInAccount);
        securityGroup = securityGroupService.create(user, securityGroup);
        return conversionService.convert(securityGroup, SecurityGroupResponse.class);
    }

    private SecurityGroup convert(SecurityGroupRequest securityGroupRequest, boolean publicInAccount) {
        SecurityGroup securityGroup = conversionService.convert(securityGroupRequest, SecurityGroup.class);
        securityGroup.setPublicInAccount(publicInAccount);
        return securityGroup;
    }

    private SecurityGroupResponse convert(SecurityGroup securityGroup) {
        return conversionService.convert(securityGroup, SecurityGroupResponse.class);
    }

    private Set<SecurityGroupResponse> convert(Set<SecurityGroup> securityGroups) {
        Set<SecurityGroupResponse> jsons = new HashSet<>();
        for (SecurityGroup securityGroup : securityGroups) {
            jsons.add(convert(securityGroup));
        }
        return jsons;
    }
}
