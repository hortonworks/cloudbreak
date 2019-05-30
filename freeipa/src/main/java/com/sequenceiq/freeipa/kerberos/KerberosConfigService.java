package com.sequenceiq.freeipa.kerberos;

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
public class KerberosConfigService extends AbstractArchivistService<KerberosConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosConfigService.class);

    @Inject
    private KerberosConfigRepository kerberosConfigRepository;

    @Inject
    private CrnService crnService;

    public KerberosConfig createKerberosConfig(KerberosConfig kerberosConfig) {
        String accountId = crnService.getCurrentAccountId();
        return createKerberosConfig(kerberosConfig, accountId);
    }

    public KerberosConfig createKerberosConfig(KerberosConfig kerberosConfig, String accountId) {
        kerberosConfig.setAccountId(accountId);
        kerberosConfig.setResourceCrn(crnService.createCrn(kerberosConfig.getAccountId(), Crn.ResourceType.KERBEROS));
        checkIfExists(kerberosConfig);
        return kerberosConfigRepository.save(kerberosConfig);
    }

    public KerberosConfig get(String environmentId) {
        String accountId = crnService.getCurrentAccountId();
        return kerberosConfigRepository.findByAccountIdAndEnvironmentId(accountId, environmentId)
                .orElseThrow(notFound("KerberosConfig for environment", environmentId));
    }

    public void delete(String environmentId) {
        String accountId = crnService.getCurrentAccountId();
        delete(environmentId, accountId);
    }

    public void delete(String environmentId, String accountId) {
        Optional<KerberosConfig> kerberosConfig = kerberosConfigRepository.findByAccountIdAndEnvironmentId(accountId, environmentId);
        kerberosConfig.ifPresentOrElse(this::delete, () -> {
            throw notFound("KerberosConfig for environment", environmentId).get();
        });
    }

    @Override
    public JpaRepository repository() {
        return kerberosConfigRepository;
    }

    private void checkIfExists(KerberosConfig resource) {
        kerberosConfigRepository.findByAccountIdAndEnvironmentId(resource.getAccountId(), resource.getEnvironmentId())
                .ifPresent(kerberosConfig -> {
                    String message = format("KerberosConfig in the [%s] account's [%s] environment is already exists", resource.getAccountId(),
                            resource.getEnvironmentId());
                    LOGGER.info(message);
                    throw new BadRequestException(message);
                });
    }
}
