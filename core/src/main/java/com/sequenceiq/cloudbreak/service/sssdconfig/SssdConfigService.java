package com.sequenceiq.cloudbreak.service.sssdconfig;

import java.util.Set;

import javax.inject.Inject;
import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.model.SssdProviderType;
import com.sequenceiq.cloudbreak.api.model.SssdSchemaType;
import com.sequenceiq.cloudbreak.api.model.SssdTlsReqcertType;
import com.sequenceiq.cloudbreak.common.type.APIResourceType;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUserRole;
import com.sequenceiq.cloudbreak.controller.BadRequestException;
import com.sequenceiq.cloudbreak.controller.NotFoundException;
import com.sequenceiq.cloudbreak.common.model.user.IdentityUser;
import com.sequenceiq.cloudbreak.domain.SssdConfig;
import com.sequenceiq.cloudbreak.repository.ClusterRepository;
import com.sequenceiq.cloudbreak.repository.SssdConfigRepository;
import com.sequenceiq.cloudbreak.service.AuthorizationService;
import com.sequenceiq.cloudbreak.service.DuplicateKeyValueException;

@Service
@Transactional
public class SssdConfigService {

    private static final Object LOCKER = new Object();

    @Value("${cb.sssd.name}")
    private String sssdName;

    @Value("${cb.sssd.type}")
    private String sssdType;

    @Value("${cb.sssd.url}")
    private String sssdUrl;

    @Value("${cb.sssd.schema}")
    private String sssdSchema;

    @Value("${cb.sssd.base}")
    private String sssdBase;

    @Inject
    private SssdConfigRepository sssdConfigRepository;

    @Inject
    private ClusterRepository clusterRepository;

    @Inject
    private AuthorizationService authorizationService;

    public SssdConfig getDefaultSssdConfig(IdentityUser user) {
        SssdConfig config = sssdConfigRepository.findByNameInAccount(sssdName, user.getAccount());
        if (config == null) {
            synchronized (LOCKER) {
                config = sssdConfigRepository.findByNameInAccount(sssdName, user.getAccount());
                if (config == null) {
                    config = new SssdConfig();
                    config.setPublicInAccount(true);
                    config.setAccount(user.getAccount());
                    config.setOwner(user.getUserId());
                    config.setName(sssdName);
                    config.setProviderType(SssdProviderType.valueOf(sssdType));
                    config.setUrl(sssdUrl);
                    config.setSchema(SssdSchemaType.valueOf(sssdSchema));
                    config.setBaseSearch(sssdBase);
                    config.setTlsReqcert(SssdTlsReqcertType.NEVER);
                    sssdConfigRepository.save(config);
                }
            }
        }
        return config;
    }

    @Transactional(Transactional.TxType.NEVER)
    public SssdConfig create(IdentityUser user, SssdConfig sssdConfig) {
        sssdConfig.setOwner(user.getUserId());
        sssdConfig.setAccount(user.getAccount());
        try {
            return sssdConfigRepository.save(sssdConfig);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateKeyValueException(APIResourceType.SSSDCONFIG, sssdConfig.getName(), ex);
        }
    }

    @PostAuthorize("hasPermission(returnObject,'read')")
    public SssdConfig get(Long id) {
        SssdConfig sssdConfig = sssdConfigRepository.findOne(id);
        if (sssdConfig == null) {
            throw new NotFoundException(String.format("SssdConfig '%s' not found", id));
        }
        return sssdConfig;
    }

    public Set<SssdConfig> retrievePrivateConfigs(IdentityUser user) {
        return sssdConfigRepository.findForUser(user.getUserId());
    }

    public Set<SssdConfig> retrieveAccountConfigs(IdentityUser user) {
        if (user.getRoles().contains(IdentityUserRole.ADMIN)) {
            return sssdConfigRepository.findAllInAccount(user.getAccount());
        } else {
            return sssdConfigRepository.findPublicInAccountForUser(user.getUserId(), user.getAccount());
        }
    }

    public SssdConfig getPrivateConfig(String name, IdentityUser user) {
        SssdConfig sssdConfig = sssdConfigRepository.findByNameForUser(name, user.getUserId());
        if (sssdConfig == null) {
            throw new NotFoundException(String.format("SssdConfig '%s' not found.", name));
        }
        return sssdConfig;
    }

    public SssdConfig getPublicConfig(String name, IdentityUser user) {
        SssdConfig sssdConfig = sssdConfigRepository.findByNameInAccount(name, user.getAccount());
        if (sssdConfig == null) {
            throw new NotFoundException(String.format("SssdConfig '%s' not found.", name));
        }
        return sssdConfig;
    }

    public void delete(Long id) {
        SssdConfig sssdConfig = get(id);
        if (sssdConfig == null) {
            throw new NotFoundException(String.format("SssdConfig '%s' not found.", id));
        }
        delete(sssdConfig);
    }

    public void delete(String name, IdentityUser user) {
        SssdConfig sssdConfig = sssdConfigRepository.findByNameInAccount(name, user.getAccount());
        if (sssdConfig == null) {
            throw new NotFoundException(String.format("SssdConfig '%s' not found.", name));
        }
        delete(sssdConfig);
    }

    private void delete(SssdConfig sssdConfig) {
        authorizationService.hasWritePermission(sssdConfig);
        if (!clusterRepository.countBySssdConfig(sssdConfig).equals(0L)) {
            throw new BadRequestException(String.format(
                    "There are clusters associated with SSSD config '%s'. Please remove these before deleting the SSSD config.", sssdConfig.getId()));
        }
        sssdConfigRepository.delete(sssdConfig);
    }
}
