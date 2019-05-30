package com.sequenceiq.freeipa.ldap;

import static com.sequenceiq.freeipa.controller.exception.NotFoundException.notFound;
import static java.lang.String.format;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.archive.AbstractArchivistService;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
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
        ldapConfig.setResourceCrn(crnService.createCrn(ldapConfig.getAccountId(), Crn.ResourceType.LDAP));
        checkIfExists(ldapConfig);
        return ldapConfigRepository.save(ldapConfig);
    }

    public LdapConfig get(String environmentId) {
        String accountId = crnService.getCurrentAccountId();
        return ldapConfigRepository.findByAccountIdAndEnvironmentId(accountId, environmentId)
                .orElseThrow(notFound("LdapConfig for environment", environmentId));
    }

    public void delete(String environmentId) {
        String accountId = crnService.getCurrentAccountId();
        delete(environmentId, accountId);
    }

    public void delete(String environmentId, String accountId) {
        Optional<LdapConfig> ldapConfig = ldapConfigRepository.findByAccountIdAndEnvironmentId(accountId, environmentId);
        ldapConfig.ifPresentOrElse(this::delete, () -> {
            throw notFound("LdapConfig for environment", environmentId).get();
        });
    }

    public String testConnection(String environmentId, LdapConfig ldapConfig) {
        if (environmentId == null && ldapConfig == null) {
            throw new BadRequestException("Either an environment or an LDAP 'validationRequest' needs to be specified in the request. ");
        }
        try {
            if (environmentId != null) {
                ldapConfig = get(environmentId);
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
        ldapConfigRepository.findByAccountIdAndEnvironmentId(resource.getAccountId(), resource.getEnvironmentId())
                .ifPresent(kerberosConfig -> {
                    String message = format("LdapConfig in the [%s] account's [%s] environment is already exists", resource.getAccountId(),
                            resource.getEnvironmentId());
                    LOGGER.info(message);
                    throw new BadRequestException(message);
                });
    }
}
