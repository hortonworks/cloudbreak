package com.sequenceiq.cloudbreak.service.securitygroup;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.common.type.ResourceStatus;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.repository.SecurityGroupRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
@Transactional
public class SecurityGroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityGroupService.class);

    @Inject
    private SecurityGroupRepository groupRepository;

    @Inject
    private StackRepository stackRepository;

    @Transactional(Transactional.TxType.NEVER)
    public SecurityGroup create(CbUser user, SecurityGroup securityGroup) {
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

    public SecurityGroup getPrivateSecurityGroup(String name, CbUser user) {
        SecurityGroup securityGroup = groupRepository.findByNameForUser(name, user.getUserId());
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found for user", name));
        }
        return securityGroup;
    }

    public SecurityGroup getPublicSecurityGroup(String name, CbUser user) {
        SecurityGroup securityGroup = groupRepository.findByNameInAccount(name, user.getAccount());
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found in account", name));
        }
        return securityGroup;
    }

    public void delete(Long id, CbUser user) {
        LOGGER.info("Deleting SecurityGroup with id: {}", id);
        SecurityGroup securityGroup = get(id);
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found.", id));
        }
        delete(user, securityGroup);
    }

    public void delete(String name, CbUser user) {
        LOGGER.info("Deleting SecurityGroup with name: {}", name);
        SecurityGroup securityGroup = groupRepository.findByNameInAccount(name, user.getAccount());
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found.", name));
        }
        delete(user, securityGroup);
    }

    public Set<SecurityGroup> retrievePrivateSecurityGroups(CbUser user) {
        return groupRepository.findForUser(user.getUserId());
    }

    public Set<SecurityGroup> retrieveAccountSecurityGroups(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return groupRepository.findAllInAccount(user.getAccount());
        } else {
            return groupRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    private void delete(CbUser user, SecurityGroup securityGroup) {
        if (stackRepository.findAllBySecurityGroup(securityGroup.getId()).isEmpty()) {
            if (!user.getUserId().equals(securityGroup.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
                throw new BadRequestException("Public SecurityGroups can only be deleted by owners or account admins.");
            } else {
                if (ResourceStatus.USER_MANAGED.equals(securityGroup.getStatus())) {
                    groupRepository.delete(securityGroup);
                } else {
                    securityGroup.setStatus(ResourceStatus.DEFAULT_DELETED);
                    groupRepository.save(securityGroup);
                }
            }
        } else {
            throw new BadRequestException(String.format(
                    "There are clusters associated with SecurityGroup '%s'(ID:'%s'). Please remove these before deleting the SecurityGroup.",
                    securityGroup.getName(), securityGroup.getId()));
        }
    }
}
