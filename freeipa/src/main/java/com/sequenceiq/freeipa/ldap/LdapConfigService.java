package com.sequenceiq.freeipa.ldap;

import static com.sequenceiq.freeipa.controller.exception.NotFoundException.notFound;
import static java.lang.String.format;

import java.util.Optional;
import java.util.UUID;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.archive.AbstractArchivistService;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.kerberos.KerberosConfigService;
import com.sequenceiq.freeipa.util.UserCrnService;

@Service
public class LdapConfigService extends AbstractArchivistService<LdapConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosConfigService.class);

    @Inject
    private LdapConfigRepository ldapConfigRepository;

    @Inject
    private LdapConfigValidator ldapConfigValidator;

    @Inject
    private UserCrnService userCrnService;

    public LdapConfig createLdapConfig(LdapConfig ldapConfig) {
        String accountId = userCrnService.getCurrentAccountId();
        ldapConfig.setAccountId(accountId);
        ldapConfig.setResourceCrn(createCrn(ldapConfig.getAccountId()));
        checkIfExists(ldapConfig);
        return ldapConfigRepository.save(ldapConfig);
    }

    public LdapConfig get(String environmentId) {
        String accountId = userCrnService.getCurrentAccountId();
        return ldapConfigRepository.findByAccountIdAndEnvironmentId(accountId, environmentId)
                .orElseThrow(notFound("LdapConfig for environment", environmentId));
    }

    public void delete(String environmentId) {
        String accountId = userCrnService.getCurrentAccountId();
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

    private String createCrn(String accountId) {
        return Crn.builder()
                .setService(Crn.Service.FREEIPA)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.LDAP)
                .setResource(UUID.randomUUID().toString())
                .build().toString();
    }
}
