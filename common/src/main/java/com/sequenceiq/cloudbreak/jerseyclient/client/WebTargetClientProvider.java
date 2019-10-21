package com.sequenceiq.cloudbreak.jerseyclient.client;

import com.sequenceiq.cloudbreak.client.RestClientUtil;
import org.springframework.stereotype.Component;

import javax.ws.rs.client.Client;

/**
 * Simple component to make testing classes/methods easier which uses the javax.ws.rs.client.Client
 */
@Component
public class WebTargetClientProvider {

    public Client getClient() {
        return RestClientUtil.get();
    }

}
