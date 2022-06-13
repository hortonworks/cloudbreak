package com.sequenceiq.freeipa.service;

import java.util.Optional;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.SaltSecurityConfig;
import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.DisabledSaltSecurityConfigRepository;
import com.sequenceiq.freeipa.repository.SecurityConfigRepository;

@Service
public class SecurityConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfigService.class);

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    @Inject
    private DisabledSaltSecurityConfigRepository disabledSaltSecurityConfigRepository;

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
                if (!saltSecurityConfig.getSaltSignPrivateKey().equals(saltSecurityConfig.getSaltPasswordVault())) {
                    saltSecurityConfig.setSaltPasswordVault(saltSecurityConfig.getSaltPassword());
                }
                if (!saltSecurityConfig.getSaltSignPrivateKey().equals(saltSecurityConfig.getSaltSignPrivateKeyVault())) {
                    saltSecurityConfig.setSaltSignPrivateKeyVault(saltSecurityConfig.getSaltSignPrivateKey());
                }
                saltSecurityConfig = disabledSaltSecurityConfigRepository.save(saltSecurityConfig);
                securityConfig.setSaltSecurityConfig(saltSecurityConfig);
            }
        }
        return securityConfig;
    }

    public void changeSaltPassword(Stack stack, String password) {
        SaltSecurityConfig saltSecurityConfig = Optional.ofNullable(stack.getSecurityConfig())
                .map(SecurityConfig::getSaltSecurityConfig)
                .orElseThrow(() -> new IllegalStateException("Stack " + stack.getResourceCrn() + " does not yet have a salt security config"));
        saltSecurityConfig.setSaltPassword(password);
        disabledSaltSecurityConfigRepository.save(saltSecurityConfig);
    }
}
