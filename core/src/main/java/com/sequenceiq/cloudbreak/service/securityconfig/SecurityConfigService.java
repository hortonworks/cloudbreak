package com.sequenceiq.cloudbreak.service.securityconfig;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.cloudbreak.domain.SaltSecurityConfig;
import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.repository.SecurityConfigRepository;
import com.sequenceiq.cloudbreak.service.TlsSecurityService;
import com.sequenceiq.cloudbreak.service.saltsecurityconf.SaltSecurityConfigService;

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

    public SecurityConfig getOneByStackId(Long stackId) {
        return findOneByStackId(stackId)
                .orElseThrow(() -> new NotFoundException("Stack does not have a security config"));
    }

    public SecurityConfig generateAndSaveSecurityConfig(Stack stack) {
        try {
            return transactionService.required(() -> {
                Optional<SecurityConfig> securityConfig = findOneByStackId(stack.getId());
                if (securityConfig.isEmpty()) {
                    LOGGER.info("Security config does not exist for stack, it is going to be generated");
                    SecurityConfig config = tlsSecurityService.generateSecurityKeys(stack.getWorkspace());
                    config.setStack(stack);
                    saltSecurityConfigService.save(config.getSaltSecurityConfig());
                    return save(config);
                }
                LOGGER.info("Security already exsist for stack, probably the flow was restarted!");
                return securityConfig.get();
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

}
