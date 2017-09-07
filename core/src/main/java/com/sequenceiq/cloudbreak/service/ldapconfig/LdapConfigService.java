package com.sequenceiq.cloudbreak.service.ldapconfig;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.LdapConfigRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
@Transactional
public class LdapConfigService {

    @Inject
    private LdapConfigRepository ldapConfigRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private AuthorizationService authorizationService;

    @Transactional(TxType.NEVER)
    public LdapConfig create(IdentityUser user, LdapConfig ldapConfig) {
        ldapConfig.setOwner(user.getUserId());
        ldapConfig.setAccount(user.getAccount());
        try {
            return ldapConfigRepository.save(ldapConfig);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.LDAP_CONFIG, ldapConfig.getName(), ex);
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public LdapConfig get(Long id) {
        LdapConfig ldapConfig = ldapConfigRepository.findOne(id);
        if (ldapConfig == null) {
            throw new NotFoundException(String.format("LdapConfig '%s' not found", id));
        }
        return ldapConfig;
    }

    public Set<LdapConfig> retrievePrivateConfigs(IdentityUser user) {
        return ldapConfigRepository.findForUser(user.getUserId());
    }

    public Set<LdapConfig> retrieveAccountConfigs(IdentityUser user) {
        if (user.getRoles().contains(IdentityUserRole.ADMIN)) {
            return ldapConfigRepository.findAllInAccount(user.getAccount());
        } else {
            return ldapConfigRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    public LdapConfig getPrivateConfig(String name, IdentityUser user) {
        LdapConfig ldapConfig = ldapConfigRepository.findByNameForUser(name, user.getUserId());
        if (ldapConfig == null) {
            throw new NotFoundException(String.format("LdapConfig '%s' not found.", name));
        }
        return ldapConfig;
    }

    public LdapConfig getPublicConfig(String name, IdentityUser user) {
        LdapConfig ldapConfig = ldapConfigRepository.findByNameInAccount(name, user.getAccount());
        if (ldapConfig == null) {
            throw new NotFoundException(String.format("LdapConfig '%s' not found.", name));
        }
        return ldapConfig;
    }

    public void delete(Long id) {
        delete(get(id));
    }

    public void delete(String name, IdentityUser user) {
        LdapConfig ldapConfig = ldapConfigRepository.findByNameInAccount(name, user.getAccount());
        if (ldapConfig == null) {
            throw new NotFoundException(String.format("LdapConfig '%s' not found.", name));
        }
        delete(ldapConfig);
    }

    private void delete(LdapConfig ldapConfig) {
        authorizationService.hasWritePermission(ldapConfig);
        if (!clusterRepository.countByLdapConfig(ldapConfig).equals(0L)) {
            throw new BadRequestException(String.format(
                    "There are clusters associated with LDAP config '%s'. Please remove these before deleting the LDAP config.", ldapConfig.getId()));
        }
        ldapConfigRepository.delete(ldapConfig);
    }
}
