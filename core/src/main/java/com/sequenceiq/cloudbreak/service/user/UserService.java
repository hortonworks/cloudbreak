package com.sequenceiq.cloudbreak.service.user;

import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.Tenant;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.repository.security.TenantRepository;
import com.sequenceiq.cloudbreak.repository.security.UserOrgPermissionsRepository;
import com.sequenceiq.cloudbreak.repository.security.UserRepository;
import com.sequenceiq.cloudbreak.service.TransactionService;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionExecutionException;
import com.sequenceiq.cloudbreak.service.TransactionService.TransactionRuntimeExecutionException;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;

@Service
public class UserService {
    @Inject
    private UserRepository userRepository;

    @Inject
    private TenantRepository tenantRepository;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private UserOrgPermissionsRepository userOrgPermissionsRepository;

    @Inject
    private TransactionService transactionService;

    public User getOrCreate(IdentityUser identityUser) {
        try {
            return transactionService.required(() -> {
                User user = userRepository.findByUserId(identityUser.getUsername());
                if (user == null) {
                    user = new User();
                    user.setUserId(identityUser.getUsername());

                    Tenant tenant = tenantRepository.findByName("DEFAULT");
                    user.setTenant(tenant);
                    user.setTenantPermissionSet(Collections.emptySet());
                    user = userRepository.save(user);

                    //create organization
                    Organization organization = new Organization();
                    organization.setTenant(tenant);
                    organization.setName(identityUser.getUsername());
                    organization.setDescription("Default organization for the user.");
                    organizationService.create(identityUser, organization);
                }
                return user;
            });
        } catch (TransactionExecutionException e) {
            throw new TransactionRuntimeExecutionException(e);
        }
    }

    public User getByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }

    public Set<User> getByUsersIds(Set<String> userIds) {
        return userRepository.findByUserIdIn(userIds);
    }

    public Set<User> getAll(IdentityUser identityUser) {
        User user = userRepository.findByUserId(identityUser.getUsername());
        return userRepository.findAllByTenant(user.getTenant());
    }
}
