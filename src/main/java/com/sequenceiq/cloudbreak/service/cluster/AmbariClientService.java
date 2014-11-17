package com.sequenceiq.cloudbreak.service.cluster;

import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;

@Service
public class AmbariClientService {

    private static final String PORT = "8080";

    public AmbariClient create(Stack stack) {
        return create(stack.getAmbariIp());
    }

    public AmbariClient create(String ambariAddress) {
        return new AmbariClient(ambariAddress, PORT);
    }

}
