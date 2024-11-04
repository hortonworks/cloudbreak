package com.sequenceiq.freeipa.service;

import static com.sequenceiq.cloudbreak.common.service.TransactionService.TransactionExecutionException;
import static com.sequenceiq.cloudbreak.util.Benchmark.measure;

import java.util.Optional;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.service.TransactionService;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.SecurityRequest;
import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.SaltSecurityConfigRepository;
import com.sequenceiq.freeipa.repository.SecurityConfigRepository;
import com.sequenceiq.freeipa.service.stack.StackService;

import io.micrometer.common.util.StringUtils;

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
        return securityConfigRepository.findOneByStackId(stack.getId());
    }

    public SecurityConfig create(Stack stack, SecurityRequest securityRequest) {
        String seLinuxAsString = (securityRequest != null && StringUtils.isNotBlank(securityRequest.getSeLinux())) ?
                securityRequest.getSeLinux() : SeLinux.PERMISSIVE.name();
        SeLinux seLinux = SeLinux.fromStringWithFallback(seLinuxAsString);
        SecurityConfig securityConfig = new SecurityConfig();
        securityConfig.setAccountId(stack.getAccountId());
        securityConfig.setSeLinux(seLinux);
        SecurityConfig savedSecurityConfig = securityConfigRepository.save(securityConfig);
        stack.setSecurityConfig(savedSecurityConfig);
        stackService.save(stack);

        return securityConfig;
    }

    public void initSaltSecurityConfigs(Long stackId) throws TransactionExecutionException {
        Stack stack = stackService.getStackById(stackId);
        if (stack.getSecurityConfig() == null || stack.getSecurityConfig().getSaltSecurityConfig() == null) {
            LOGGER.debug("Create SecurityConfig for stack {}", stack.getResourceCrn());
            SecurityConfig securityConfig = measure(() ->
                            tlsSecurityService.generateSecurityKeys(stack.getAccountId(), stack.getSecurityConfig()), LOGGER,
                    "Generating security keys took {} ms for {}", stack.getName());
            transactionService.required(() -> {
                saltSecurityConfigRepository.save(securityConfig.getSaltSecurityConfig());
                SecurityConfig savedSecurityConfig = securityConfigRepository.save(securityConfig);
                if (stack.getSecurityConfig() == null) {
                    stack.setSecurityConfig(savedSecurityConfig);
                    stackService.save(stack);
                }
            });
        } else {
            LOGGER.debug("SecurityConfig for stack {} already exists", stack.getResourceCrn());
        }
    }

    public void changeSaltPassword(Stack stack, String password) {
        SaltSecurityConfig saltSecurityConfig = Optional.ofNullable(stack.getSecurityConfig())
                .map(SecurityConfig::getSaltSecurityConfig)
                .orElseThrow(() -> new IllegalStateException("Stack " + stack.getResourceCrn() + " does not yet have a salt security config"));
        saltSecurityConfig.setSaltPasswordVault(password);
        saltSecurityConfigRepository.save(saltSecurityConfig);
    }
}
