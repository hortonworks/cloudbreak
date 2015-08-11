package com.sequenceiq.periscope.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.periscope.service.security.TokenService;

@Service
public class CloudbreakService {

    @Autowired
    private TokenService tokenService;

    @Autowired
    @Qualifier("cloudbreakUrl")
    private String cloudbreakUrl;

    public CloudbreakClient getClient() {
        return new CloudbreakClient(cloudbreakUrl, tokenService.getToken());
    }
}
