package com.sequenceiq.it.cloudbreak.mock.freeipa;

import org.springframework.stereotype.Component;

import com.sequenceiq.freeipa.client.model.Config;

import spark.Request;
import spark.Response;

@Component
public class ConfigShowResponse extends AbstractFreeIpaResponse<Config> {
    @Override
    public String method() {
        return "config_show";
    }

    @Override
    protected Config handleInternal(Request request, Response response) {
        Config config = new Config();
        config.setIpamaxusernamelength(255);
        return config;
    }
}
