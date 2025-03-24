package com.sequenceiq.cloudbreak.service.securityconfig;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SecurityV4Request;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.model.SeLinux;

@Service
public class SecurityConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfigService.class);

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private TransactionService transactionService;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private SaltSecurityConfigService saltSecurityConfigService;

    @Inject
    private StackService stackService;

    @Inject
    private EntitlementService entitlementService;

    public SecurityConfig save(SecurityConfig securityConfig) {
        return securityConfigRepository.save(securityConfig);
    }

    public Optional<SecurityConfig> findOneByStackId(Long stackId) {
        return securityConfigRepository.findOneByStackId(stackId);
    }

    public void deleteByStackId(Long stackId) {
        Optional<SecurityConfig> securityConfig = findOneByStackId(stackId);
        if (securityConfig.isPresent()) {
            LOGGER.debug("Security config was presented for stackid {}.", stackId);
            securityConfigRepository.deleteById(securityConfig.get().getId());
        } else {
            LOGGER.debug("Security config was not found for stackid {}.", stackId);
        }
    }

    public void validateRequest(SecurityV4Request securityRequest, String accountId) {
        if (SeLinux.ENFORCING.equals(getSeLinuxFromRequest(securityRequest))
                && !entitlementService.isCdpSecurityEnforcingSELinux(accountId)) {
            throw new BadRequestException("SELinux enforcing requires CDP_SECURITY_ENFORCING_SELINUX entitlement for your account.");
        }
    }

    private SeLinux getSeLinuxFromRequest(SecurityV4Request securityRequest) {
        String seLinuxAsString = securityRequest != null ? securityRequest.getSeLinux() : SeLinux.PERMISSIVE.name();
        return SeLinux.fromStringWithFallback(seLinuxAsString);
    }

    public SecurityConfig create(Stack stack, SecurityV4Request securityRequest) {
        SeLinux seLinux = getSeLinuxFromRequest(securityRequest);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setWorkspace(stack.getWorkspace());
        securityConfig.setStack(stack);
        securityConfig.setSeLinux(seLinux);
        SecurityConfig savedSecurityConfig = securityConfigRepository.save(securityConfig);
        stack.setSecurityConfig(savedSecurityConfig);
        stackService.save(stack);
        return savedSecurityConfig;
    }

    public SecurityConfig initSaltSecurityConfigs(Stack stack) {
        try {
            return transactionService.required(() -> {
                Optional<SecurityConfig> securityConfigOptional = findOneByStackId(stack.getId());
                if (securityConfigOptional.isEmpty() || securityConfigOptional.get().getSaltSecurityConfig() == null) {
                    LOGGER.info("Security config does not exist for stack, it is going to be generated");
                    SecurityConfig securityConfig = securityConfigOptional.isEmpty() ? new SecurityConfig() : securityConfigOptional.get();
                    securityConfig.setWorkspace(stack.getWorkspace());
                    SecurityConfig config = tlsSecurityService.generateSecurityKeys(stack.getWorkspace(), securityConfig);
                    saltSecurityConfigService.save(config.getSaltSecurityConfig());
                    return save(config);
                }
                LOGGER.info("Security already exsist for stack, probably the flow was restarted!");
                return securityConfigOptional.get();
            });
        } catch (TransactionService.TransactionExecutionException e) {
            LOGGER.error("Error while saving SecurityConfig", e);
            throw e.getCause();
        }
    }

    public void changeSaltPassword(SecurityConfig securityConfig, String password) {
        LOGGER.info("Changing salt password of security config {}", securityConfig.getId());
        SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
        saltSecurityConfig.setSaltPassword(password);
        saltSecurityConfigService.save(saltSecurityConfig);
        LOGGER.info("Successfully changed salt password of security config {}", securityConfig.getId());
    }

    public int updateSeLinuxSecurityConfig(Long securityConfigId, SeLinux selinuxMode) {
        return securityConfigRepository.updateSeLinuxSecurityConfig(selinuxMode, securityConfigId);
    }
}
