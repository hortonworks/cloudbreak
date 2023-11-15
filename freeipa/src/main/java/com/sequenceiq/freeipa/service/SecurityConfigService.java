package com.sequenceiq.freeipa.service;

import static com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.SaltSecurityConfigRepository;
import com.sequenceiq.freeipa.repository.SecurityConfigRepository;
import com.sequenceiq.freeipa.service.stack.StackService;

@Service
public class SecurityConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfigService.class);

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private SaltSecurityConfigRepository saltSecurityConfigRepository;

    @Inject
    private TlsSecurityService tlsSecurityService;

    @Inject
    private StackService stackService;

    @Inject
    private TransactionService transactionService;

    public SecurityConfig save(SecurityConfig securityConfig) {
        return securityConfigRepository.save(securityConfig);
    }

    public SecurityConfig findOneByStack(Stack stack) {
        SecurityConfig securityConfig = securityConfigRepository.findOneByStackId(stack.getId());
        if (securityConfig != null && securityConfig.getSaltSecurityConfig() != null) {
            SaltSecurityConfig saltSecurityConfig = securityConfig.getSaltSecurityConfig();
            if (StringUtils.isAnyBlank(saltSecurityConfig.getSaltBootPasswordVault(), saltSecurityConfig.getSaltBootSignPrivateKeyVault(),
                    saltSecurityConfig.getSaltPasswordVault(), saltSecurityConfig.getSaltSignPrivateKeyVault())) {
                LOGGER.debug("Migrate SaltSecurityConfig with id [{}] to vault", saltSecurityConfig.getId());
                if (!saltSecurityConfig.getSaltBootPassword().equals(saltSecurityConfig.getSaltBootPasswordVault())) {
                    saltSecurityConfig.setSaltBootPasswordVault(saltSecurityConfig.getSaltBootPassword());
                }
                if (!saltSecurityConfig.getSaltBootSignPrivateKey().equals(saltSecurityConfig.getSaltBootSignPrivateKeyVault())) {
                    saltSecurityConfig.setSaltBootSignPrivateKeyVault(saltSecurityConfig.getSaltBootSignPrivateKey());
                }
                if (!saltSecurityConfig.getSaltPassword().equals(saltSecurityConfig.getSaltPasswordVault())) {
                    saltSecurityConfig.setSaltPasswordVault(saltSecurityConfig.getSaltPassword());
                }
                if (!saltSecurityConfig.getSaltSignPrivateKey().equals(saltSecurityConfig.getSaltSignPrivateKeyVault())) {
                    saltSecurityConfig.setSaltSignPrivateKeyVault(saltSecurityConfig.getSaltSignPrivateKey());
                }
                saltSecurityConfig = saltSecurityConfigRepository.save(saltSecurityConfig);
                securityConfig.setSaltSecurityConfig(saltSecurityConfig);
            }
        }
        return securityConfig;
    }

    public void createIfDoesntExists(Long stackId) throws TransactionExecutionException {
        Stack stack = stackService.getStackById(stackId);
        if (stack.getSecurityConfig() == null) {
            LOGGER.debug("Create SecurityConfig for stack {}", stack.getResourceCrn());
            SecurityConfig securityConfig = measure(() -> tlsSecurityService.generateSecurityKeys(stack.getAccountId()), LOGGER,
                    "Generating security keys took {} ms for {}", stack.getName());
            transactionService.required(() -> {
                saltSecurityConfigRepository.save(securityConfig.getSaltSecurityConfig());
                SecurityConfig savedSecurityConfig = securityConfigRepository.save(securityConfig);
                stack.setSecurityConfig(savedSecurityConfig);
                stackService.save(stack);
            });
        } else {
            LOGGER.debug("SecurityConfig for stack {} already exists", stack.getResourceCrn());
        }
    }

    public void changeSaltPassword(Stack stack, String password) {
        SaltSecurityConfig saltSecurityConfig = Optional.ofNullable(stack.getSecurityConfig())
                .map(SecurityConfig::getSaltSecurityConfig)
                .orElseThrow(() -> new IllegalStateException("Stack " + stack.getResourceCrn() + " does not yet have a salt security config"));
        saltSecurityConfig.setSaltPassword(password);
        saltSecurityConfig.setSaltPasswordVault(password);
        saltSecurityConfigRepository.save(saltSecurityConfig);
    }
}
