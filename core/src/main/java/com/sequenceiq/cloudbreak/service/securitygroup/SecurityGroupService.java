package com.sequenceiq.cloudbreak.service.securitygroup;

import java.util.Set;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;

public interface SecurityGroupService {

    SecurityGroup create(CbUser user, SecurityGroup securityGroup);

    SecurityGroup get(Long id);

    SecurityGroup getById(Long id);

    SecurityGroup getPrivateSecurityGroup(String name, CbUser user);

    SecurityGroup getPublicSecurityGroup(String name, CbUser user);

    void delete(Long id, CbUser user);

    void delete(String name, CbUser user);

    Set<SecurityGroup> retrievePrivateSecurityGroups(CbUser user);

    Set<SecurityGroup> retrieveAccountSecurityGroups(CbUser user);
}
