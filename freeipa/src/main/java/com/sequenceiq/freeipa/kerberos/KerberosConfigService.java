package com.sequenceiq.freeipa.kerberos;

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
import com.sequenceiq.freeipa.util.UserCrnService;

@Service
public class KerberosConfigService extends AbstractArchivistService<KerberosConfig> {
    private static final Logger LOGGER = LoggerFactory.getLogger(KerberosConfigService.class);

    @Inject
    private KerberosConfigRepository kerberosConfigRepository;

    @Inject
    private UserCrnService userCrnService;

    public KerberosConfig createKerberosConfig(KerberosConfig kerberosConfig) {
        String accountId = userCrnService.getCurrentAccountId();
        kerberosConfig.setAccountId(accountId);
        kerberosConfig.setResourceCrn(createCrn(kerberosConfig.getAccountId()));
        checkIfExists(kerberosConfig);
        return kerberosConfigRepository.save(kerberosConfig);
    }

    public KerberosConfig get(String environmentId) {
        String accountId = userCrnService.getCurrentAccountId();
        return kerberosConfigRepository.findByAccountIdAndEnvironmentId(accountId, environmentId)
                .orElseThrow(notFound("KerberosConfig for environment", environmentId));
    }

    public void delete(String environmentId) {
        String accountId = userCrnService.getCurrentAccountId();
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

    private String createCrn(String accountId) {
        return Crn.builder()
                .setService(Crn.Service.FREEIPA)
                .setAccountId(accountId)
                .setResourceType(Crn.ResourceType.KERBEROS)
                .setResource(UUID.randomUUID().toString())
                .build().toString();
    }
}
