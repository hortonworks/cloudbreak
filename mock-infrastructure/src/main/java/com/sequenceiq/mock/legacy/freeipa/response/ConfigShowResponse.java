package com.sequenceiq.mock.legacy.freeipa.response;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.Config;

@Component
public class ConfigShowResponse extends AbstractFreeIpaResponse<Config> {

    private static final int IPA_MAX_USERNAME_LENGTH = 255;

    @Override
    public String method() {
        return "config_show";
    }

    @Override
    protected Config handleInternal(String body) {
        Config config = new Config();
        config.setIpamaxusernamelength(IPA_MAX_USERNAME_LENGTH);
        return config;
    }
}
