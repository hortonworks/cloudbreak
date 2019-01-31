package com.sequenceiq.cloudbreak.startup;

import static com.sequenceiq.cloudbreak.util.GovCloudFlagUtil.GOV_CLOUD_KEY;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.service.credential.CredentialService;
import com.sequenceiq.cloudbreak.util.GovCloudFlagUtil;

@Component
public class GovCloudFlagMigrator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GovCloudFlagMigrator.class);

    @Inject
    private CredentialService credentialService;

    private final AtomicBoolean finished = new AtomicBoolean(false);

    public void run() {
        Iterable<Credential> credentials = credentialService.findAll();
        Set<Credential> modifiedCredentials = StreamSupport.stream(credentials.spliterator(), false)
                .filter(credential -> credential.getAttributes() != null
                        && new Json(credential.getAttributes()).getValue(GOV_CLOUD_KEY) != null)
                .peek(credential -> {
                    Object govCloudFlag = new Json(credential.getAttributes()).getValue(GOV_CLOUD_KEY);
                    credential.setGovCloud(GovCloudFlagUtil.extractGovCloudFlag(govCloudFlag));
                    LOGGER.info("Credential {} updated with govCloud flag: {}.", credential.getName(), credential.getGovCloud());
                })
                .collect(Collectors.toSet());
        credentialService.saveAll(modifiedCredentials);
        finished.set(true);
    }

    public boolean isFinished() {
        return finished.get();
    }
}
