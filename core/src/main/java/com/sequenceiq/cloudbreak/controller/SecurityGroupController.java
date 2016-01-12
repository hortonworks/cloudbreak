package com.sequenceiq.cloudbreak.controller;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.SecurityGroupEndpoint;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.model.IdJson;
import com.sequenceiq.cloudbreak.model.SecurityGroupJson;
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
    public IdJson postPrivate(SecurityGroupJson securityGroupJson) {
        CbUser user = authenticatedUserService.getCbUser();
        return createSecurityGroup(user, securityGroupJson, false);
    }

    @Override
    public IdJson postPublic(SecurityGroupJson securityGroupJson) {
        CbUser user = authenticatedUserService.getCbUser();
        return createSecurityGroup(user, securityGroupJson, true);
    }

    @Override
    public Set<SecurityGroupJson> getPrivates() {
        CbUser user = authenticatedUserService.getCbUser();
        Set<SecurityGroup> securityGroups = defaultSecurityGroupCreator.createDefaultSecurityGroups(user);
        securityGroups.addAll(securityGroupService.retrievePrivateSecurityGroups(user));
        return convert(securityGroups);
    }

    @Override
    public Set<SecurityGroupJson> getPublics() {
        CbUser user = authenticatedUserService.getCbUser();
        Set<SecurityGroup> securityGroups = defaultSecurityGroupCreator.createDefaultSecurityGroups(user);
        securityGroups.addAll(securityGroupService.retrieveAccountSecurityGroups(user));
        return convert(securityGroups);
    }

    @Override
    public SecurityGroupJson get(Long id) {
        SecurityGroup securityGroup = securityGroupService.get(id);
        return convert(securityGroup);
    }

    @Override
    public SecurityGroupJson getPrivate(String name) {
        CbUser user = authenticatedUserService.getCbUser();
        SecurityGroup securityGroup = securityGroupService.getPrivateSecurityGroup(name, user);
        return convert(securityGroup);
    }

    @Override
    public SecurityGroupJson getPublic(String name) {
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

    private IdJson createSecurityGroup(CbUser user, SecurityGroupJson securityGroupJson, boolean publicInAccount) {
        SecurityGroup securityGroup = convert(securityGroupJson, publicInAccount);
        securityGroup = securityGroupService.create(user, securityGroup);
        return new IdJson(securityGroup.getId());
    }

    private SecurityGroup convert(SecurityGroupJson securityGroupJson, boolean publicInAccount) {
        SecurityGroup securityGroup = conversionService.convert(securityGroupJson, SecurityGroup.class);
        securityGroup.setPublicInAccount(publicInAccount);
        return securityGroup;
    }

    private SecurityGroupJson convert(SecurityGroup securityGroup) {
        return conversionService.convert(securityGroup, SecurityGroupJson.class);
    }

    private Set<SecurityGroupJson> convert(Set<SecurityGroup> securityGroups) {
        Set<SecurityGroupJson> jsons = new HashSet<>();
        for (SecurityGroup securityGroup : securityGroups) {
            jsons.add(convert(securityGroup));
        }
        return jsons;
    }
}
