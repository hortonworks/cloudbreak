package com.sequenceiq.freeipa.ldap;

import static com.sequenceiq.cloudbreak.common.exception.NotFoundException.notFound;
import static java.lang.String.format;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.common.archive.AbstractArchivistService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.freeipa.util.CrnService;

@Service
public class LdapConfigService extends AbstractArchivistService<LdapConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigService.class);

    @Inject
    private LdapConfigRepository ldapConfigRepository;

    @Inject
    private LdapConfigValidator ldapConfigValidator;

    @Inject
    private CrnService crnService;

    public LdapConfig createLdapConfig(LdapConfig ldapConfig) {
        String accountId = crnService.getCurrentAccountId();
        return createLdapConfig(ldapConfig, accountId);
    }

    public LdapConfig createLdapConfig(LdapConfig ldapConfig, String accountId) {
        ldapConfig.setAccountId(accountId);
        ldapConfig.setResourceCrn(crnService.createCrn(ldapConfig.getAccountId(), CrnResourceDescriptor.LDAP));
        LOGGER.debug("Trying to save LdapConfig: {}", ldapConfig);
        checkIfExists(ldapConfig);
        return ldapConfigRepository.save(ldapConfig);
    }

    public LdapConfig get(String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(accountId, environmentCrn)
                .orElseThrow(notFound("LdapConfig for environment", environmentCrn));
    }

    public List<LdapConfig> findAllByEnvironmentAndAccountIdEvenIfArchived(String environmentCrn, String accountId) {
        return ldapConfigRepository.findByAccountIdAndEnvironmentCrn(accountId, environmentCrn);
    }

    public Optional<LdapConfig> find(String environmentCrn, String accountId, String clusterName) {
        return ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameAndArchivedIsFalse(accountId, environmentCrn, clusterName);
    }

    public void delete(String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        delete(environmentCrn, accountId);
    }

    public void delete(String environmentCrn, String accountId) {
        Optional<LdapConfig> ldapConfig = ldapConfigRepository
                .findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(accountId, environmentCrn);
        ldapConfig.ifPresentOrElse(this::delete, () -> {
            throw notFound("LdapConfig for environment", environmentCrn).get();
        });
    }

    public void delete(String environmentCrn, String accountId, String clusterName) {
        Optional<LdapConfig> ldapConfig =
                ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameAndArchivedIsFalse(accountId, environmentCrn, clusterName);
        ldapConfig.ifPresentOrElse(this::delete, () -> {
            throw notFound("LdapConfig for environment", environmentCrn).get();
        });
    }

    public void deleteAllInEnvironment(String environmentCrn, String accountId) {
        ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndArchivedIsFalse(accountId, environmentCrn).forEach(this::delete);
    }

    public String testConnection(String environmentCrn, LdapConfig ldapConfig) {
        if (environmentCrn == null && ldapConfig == null) {
            throw new BadRequestException("Either an environment or an LDAP 'validationRequest' needs to be specified in the request. ");
        }
        try {
            if (environmentCrn != null) {
                ldapConfig = get(environmentCrn);
            }
            ldapConfigValidator.validateLdapConnection(ldapConfig);
            return "connected";
        } catch (BadRequestException e) {
            return e.getMessage();
        }
    }

    @Override
    public JpaRepository repository() {
        return ldapConfigRepository;
    }

    private void checkIfExists(LdapConfig resource) {
        Optional<LdapConfig> ldapConfig = StringUtils.isBlank(resource.getClusterName()) ?
                ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(resource.getAccountId(),
                        resource.getEnvironmentCrn())
                : ldapConfigRepository.findByAccountIdAndEnvironmentCrnAndClusterNameAndArchivedIsFalse(resource.getAccountId(), resource.getEnvironmentCrn(),
                resource.getClusterName());
        ldapConfig.ifPresent(kerberosConfig -> {
            String message = format("LdapConfig in the [%s] account's [%s] environment is already exists", resource.getAccountId(),
                    resource.getEnvironmentCrn());
            LOGGER.info(message);
            throw new BadRequestException(message);
        });
    }
}
