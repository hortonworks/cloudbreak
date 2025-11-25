package com.sequenceiq.mock.freeipa.response;

import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.CloudVmMetaDataStatus;
import com.sequenceiq.freeipa.client.model.Config;

@Component
public class ConfigShowResponse extends AbstractFreeIpaResponse<Config> {

    private static final int IPA_MAX_USERNAME_LENGTH = 255;

    @Override
    public String method() {
        return "config_show";
    }

    @Override
    protected Config handleInternal(List<CloudVmMetaDataStatus> metadatas, String body) {
        Config config = new Config();
        config.setIpamaxusernamelength(IPA_MAX_USERNAME_LENGTH);
        config.setIpauserobjectclasses(Set.of("cdpUserAttr"));
        return config;
    }
}
