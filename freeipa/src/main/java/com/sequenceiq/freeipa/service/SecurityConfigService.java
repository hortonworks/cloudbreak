package com.sequenceiq.freeipa.service;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.SecurityConfig;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.SecurityConfigRepository;

@Service
public class SecurityConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityConfigService.class);

    @Inject
    private SecurityConfigRepository securityConfigRepository;

    public SecurityConfig save(SecurityConfig securityConfig) {
        return securityConfigRepository.save(securityConfig);
    }

    public SecurityConfig findOneByStack(Stack stack) {
        SecurityConfig securityConfig = securityConfigRepository.findOneByStackId(stack.getId());
        if ((StringUtils.isBlank(securityConfig.getClientCertVault()) && StringUtils.isNotBlank(securityConfig.getClientCert()))
                || (StringUtils.isBlank(securityConfig.getClientKeyVault()) && StringUtils.isNotBlank(securityConfig.getClientKey()))) {
            LOGGER.debug("Migrate SecurityConfig secrets to vault with id: [{}]", securityConfig.getId());
            securityConfig.setAccountId(stack.getAccountId());
            securityConfig.setClientCertVault(securityConfig.getClientCert());
            securityConfig.setClientKeyVault(securityConfig.getClientKey());
            securityConfig = securityConfigRepository.save(securityConfig);
        }
        return securityConfig;
    }
}
