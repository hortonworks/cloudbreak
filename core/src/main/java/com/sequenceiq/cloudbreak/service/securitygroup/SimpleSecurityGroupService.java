package com.sequenceiq.cloudbreak.service.securitygroup;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.APIResourceType;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.CbUserRole;
import com.sequenceiq.cloudbreak.domain.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.SecurityGroup;
import com.sequenceiq.cloudbreak.repository.SecurityGroupRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
public class SimpleSecurityGroupService implements SecurityGroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSecurityGroupService.class);

    @Inject
    private SecurityGroupRepository repository;

    @Inject
    private StackRepository stackRepository;

    @Override
    public SecurityGroup create(CbUser user, SecurityGroup securityGroup) {
        LOGGER.info("Creating SecurityGroup: [User: '{}', Account: '{}']", user.getUsername(), user.getAccount());
        securityGroup.setOwner(user.getUserId());
        securityGroup.setAccount(user.getAccount());
        try {
            return repository.save(securityGroup);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.SECURITY_GROUP, securityGroup.getName(), ex);
        }
    }

    @Override
    public SecurityGroup get(Long id) {
        SecurityGroup securityGroup = repository.findById(id);
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found", id));
        }
        return securityGroup;
    }

    @Override
    public SecurityGroup getById(Long id) {
        SecurityGroup securityGroup = repository.findOneById(id);
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found", id));
        }
        return securityGroup;
    }

    @Override
    public SecurityGroup getPrivateSecurityGroup(String name, CbUser user) {
        SecurityGroup securityGroup = repository.findByNameForUser(name, user.getUserId());
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found for user", name));
        }
        return securityGroup;
    }

    @Override
    public SecurityGroup getPublicSecurityGroup(String name, CbUser user) {
        SecurityGroup securityGroup = repository.findByNameInAccount(name, user.getAccount());
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found in account", name));
        }
        return securityGroup;
    }

    @Override
    public void delete(Long id, CbUser user) {
        LOGGER.info("Deleting SecurityGroup with id: {}", id);
        SecurityGroup securityGroup = repository.findOne(id);
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found.", id));
        }
        delete(user, securityGroup);
    }

    @Override
    public void delete(String name, CbUser user) {
        LOGGER.info("Deleting SecurityGroup with name: {}", name);
        SecurityGroup securityGroup = repository.findByNameInAccount(name, user.getAccount());
        if (securityGroup == null) {
            throw new NotFoundException(String.format("SecurityGroup '%s' not found.", name));
        }
        delete(user, securityGroup);
    }

    @Override
    public Set<SecurityGroup> retrievePrivateSecurityGroups(CbUser user) {
        return repository.findForUser(user.getUserId());
    }

    @Override
    public Set<SecurityGroup> retrieveAccountSecurityGroups(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return repository.findAllInAccount(user.getAccount());
        } else {
            return repository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    private void delete(CbUser user, SecurityGroup securityGroup) {
        if (stackRepository.findAllBySecurityGroup(securityGroup.getId()).isEmpty()) {
            if (!user.getUserId().equals(securityGroup.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
                throw new BadRequestException("Public SecurityGroups can only be deleted by owners or account admins.");
            } else {
                if (ResourceStatus.USER_MANAGED.equals(securityGroup.getStatus())) {
                    repository.delete(securityGroup);
                } else {
                    securityGroup.setStatus(ResourceStatus.DEFAULT_DELETED);
                    repository.save(securityGroup);
                }
            }
        } else {
            throw new BadRequestException(String.format(
                    "There are clusters associated with SecurityGroup '%s'(ID:'%s'). Please remove these before deleting the SecurityGroup.",
                    securityGroup.getName(), securityGroup.getId()));
        }
    }
}
