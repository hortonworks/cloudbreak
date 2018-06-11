package com.sequenceiq.cloudbreak.service.ldapconfig;

import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.controller.exception.NotFoundException;
import com.sequenceiq.cloudbreak.domain.LdapConfig;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.LdapConfigRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.sequenceiq.cloudbreak.util.SqlUtil.getProperSqlErrorMessage;

@Service
public class LdapConfigService {

    @Inject
    private LdapConfigRepository ldapConfigRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private AuthorizationService authorizationService;

    public LdapConfig create(IdentityUser user, LdapConfig ldapConfig) {
        ldapConfig.setOwner(user.getUserId());
        ldapConfig.setAccount(user.getAccount());
        try {
            return ldapConfigRepository.save(ldapConfig);
        } catch (DataIntegrityViolationException ex) {
            String msg = String.format("Error with resource [%s], error: [%s]", APIResourceType.LDAP_CONFIG, getProperSqlErrorMessage(ex));
            throw new BadRequestException(msg);
        }
    }

    public LdapConfig get(Long id) {
        LdapConfig ldapConfig = ldapConfigRepository.findOne(id);
        if (ldapConfig == null) {
            throw new NotFoundException(String.format("LdapConfig '%s' not found", id));
        }
        authorizationService.hasReadPermission(ldapConfig);
        return ldapConfig;
    }

    public LdapConfig getByName(String name, IdentityUser user) {
        LdapConfig ldapConfig = ldapConfigRepository.findByNameInAccount(name, user.getAccount());
        if (ldapConfig == null) {
            throw new NotFoundException(String.format("LdapConfig '%s' not found", name));
        }
        authorizationService.hasReadPermission(ldapConfig);
        return ldapConfig;
    }

    public Set<LdapConfig> retrievePrivateConfigs(IdentityUser user) {
        return ldapConfigRepository.findForUser(user.getUserId());
    }

    public Set<LdapConfig> retrieveAccountConfigs(IdentityUser user) {
        return user.getRoles().contains(IdentityUserRole.ADMIN) ? ldapConfigRepository.findAllInAccount(user.getAccount())
                : ldapConfigRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
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

    public void delete(Long id, IdentityUser user) {
        delete(get(id).getName(), user);
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
        List<Cluster> clustersWithLdap = clusterRepository.findByLdapConfig(ldapConfig);
        if (!clustersWithLdap.isEmpty()) {
            if (clustersWithLdap.size() > 1) {
                String clusters = clustersWithLdap
                        .stream()
                        .map(Cluster::getName)
                        .collect(Collectors.joining(", "));
                throw new BadRequestException(String.format(
                        "There are clusters associated with LDAP config '%s'. Please remove these before deleting the LDAP config. "
                                + "The following clusters are using this LDAP: [%s]", ldapConfig.getName(), clusters));
            } else {
                throw new BadRequestException(String.format("There is a cluster ['%s'] which uses LDAP config '%s'. Please remove this "
                        + "cluster before deleting the LDAP config", clustersWithLdap.get(0).getName(), ldapConfig.getName()));
            }
        }
        ldapConfigRepository.delete(ldapConfig);
    }
}
