package com.sequenceiq.cloudbreak.service.cluster;

import org.springframework.stereotype.Service;

import com.sequenceiq.ambari.client.AmbariClient;
import com.sequenceiq.cloudbreak.domain.Stack;

@Service
public class AmbariClientService {

    private static final String PORT = "8080";

    public AmbariClient create(Stack stack) {
        return new AmbariClient(stack.getAmbariIp(), PORT, stack.getUserName(), stack.getPassword());
    }

    public AmbariClient createDefault(String ambariAddress) {
        return new AmbariClient(ambariAddress);
    }

}
