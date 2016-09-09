package com.sequenceiq.cloudbreak.service.ldapconfig;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.type.CbUserRole;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.LdapConfigRepository;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
@Transactional
public class LdapConfigService {

    @Inject
    private LdapConfigRepository ldapConfigRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Transactional(Transactional.TxType.NEVER)
    public LdapConfig create(CbUser user, LdapConfig ldapConfig) {
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

    public Set<LdapConfig> retrievePrivateConfigs(CbUser user) {
        return ldapConfigRepository.findForUser(user.getUserId());
    }

    public Set<LdapConfig> retrieveAccountConfigs(CbUser user) {
        if (user.getRoles().contains(CbUserRole.ADMIN)) {
            return ldapConfigRepository.findAllInAccount(user.getAccount());
        } else {
            return ldapConfigRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    public LdapConfig getPrivateConfig(String name, CbUser user) {
        LdapConfig ldapConfig = ldapConfigRepository.findByNameForUser(name, user.getUserId());
        if (ldapConfig == null) {
            throw new NotFoundException(String.format("LdapConfig '%s' not found.", name));
        }
        return ldapConfig;
    }

    public LdapConfig getPublicConfig(String name, CbUser user) {
        LdapConfig ldapConfig = ldapConfigRepository.findByNameInAccount(name, user.getAccount());
        if (ldapConfig == null) {
            throw new NotFoundException(String.format("LdapConfig '%s' not found.", name));
        }
        return ldapConfig;
    }

    public void delete(Long id, CbUser user) {
        LdapConfig ldapConfig = get(id);
        if (ldapConfig == null) {
            throw new NotFoundException(String.format("LdapConfig '%s' not found.", id));
        }
        delete(ldapConfig, user);
    }

    public void delete(String name, CbUser user) {
        LdapConfig ldapConfig = ldapConfigRepository.findByNameInAccount(name, user.getAccount());
        if (ldapConfig == null) {
            throw new NotFoundException(String.format("LdapConfig '%s' not found.", name));
        }
        delete(ldapConfig, user);
    }

    private void delete(LdapConfig ldapConfig, CbUser user) {
        if (clusterRepository.findAllClustersByLdapConfig(ldapConfig.getId()).isEmpty()) {
            if (!user.getUserId().equals(ldapConfig.getOwner()) && !user.getRoles().contains(CbUserRole.ADMIN)) {
                throw new BadRequestException("Public LDAP configs can only be deleted by owners or account admins.");
            } else {
                ldapConfigRepository.delete(ldapConfig);
            }
        } else {
            throw new BadRequestException(String.format(
                    "There are clusters associated with LDAP config '%s'. Please remove these before deleting the LDAP config.", ldapConfig.getId()));
        }
    }
}
