package com.sequenceiq.cloudbreak.service.securitygroup;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.repository.InstanceGroupRepository;
import com.sequenceiq.cloudbreak.repository.SecurityGroupRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;
import com.sequenceiq.cloudbreak.util.NameUtil;

@Service
@Transactional
public class SecurityGroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityGroupService.class);

    @Inject
    private SecurityGroupRepository groupRepository;

    @Inject
    private InstanceGroupRepository instanceGroupRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Transactional(TxType.NEVER)
    public SecurityGroup create(IdentityUser user, SecurityGroup securityGroup) {
        LOGGER.info("Creating SecurityGroup: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        securityGroup.setOwner(user.getUserId());
        securityGroup.setAccount(user.getAccount());
        try {
            return groupRepository.save(securityGroup);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.SECURITY_GROUP, securityGroup.getName(), ex);
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public SecurityGroup get(Long id) {
        SecurityGroup securityGroup = groupRepository.findById(id);
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found", id));
        }
        return securityGroup;
    }

    public SecurityGroup getPrivateSecurityGroup(String name, IdentityUser user) {
        SecurityGroup securityGroup = groupRepository.findByNameForUser(name, user.getUserId());
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found for user", name));
        }
        return securityGroup;
    }

    public SecurityGroup getPublicSecurityGroup(String name, IdentityUser user) {
        SecurityGroup securityGroup = groupRepository.findByNameInAccount(name, user.getAccount());
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found in account", name));
        }
        return securityGroup;
    }

    public void delete(Long id, IdentityUser user) {
        LOGGER.info("Deleting SecurityGroup with id: {}", id);
        delete(get(id));
    }

    public void delete(String name, IdentityUser user) {
        LOGGER.info("Deleting SecurityGroup with name: {}", name);
        SecurityGroup securityGroup = groupRepository.findByNameInAccount(name, user.getAccount());
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found.", name));
        }
        delete(securityGroup);
    }

    public Set<SecurityGroup> retrievePrivateSecurityGroups(IdentityUser user) {
        return groupRepository.findForUser(user.getUserId());
    }

    public Set<SecurityGroup> retrieveAccountSecurityGroups(IdentityUser user) {
        if (user.getRoles().contains(IdentityUserRole.ADMIN)) {
            return groupRepository.findAllInAccount(user.getAccount());
        } else {
            return groupRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    private void delete(SecurityGroup securityGroup) {
        authorizationService.hasWritePermission(securityGroup);
        if (!instanceGroupRepository.countBySecurityGroup(securityGroup).equals(0L)) {
            throw new BadRequestException(String.format(
                    "There are clusters associated with SecurityGroup '%s'(ID:'%s'). Please remove these before deleting the SecurityGroup.",
                    securityGroup.getName(), securityGroup.getId()));
        }
        if (ResourceStatus.USER_MANAGED.equals(securityGroup.getStatus())) {
            groupRepository.delete(securityGroup);
        } else {
            securityGroup.setName(NameUtil.postfixWithTimestamp(securityGroup.getName()));
            securityGroup.setStatus(ResourceStatus.DEFAULT_DELETED);
            groupRepository.save(securityGroup);
        }
    }
}
