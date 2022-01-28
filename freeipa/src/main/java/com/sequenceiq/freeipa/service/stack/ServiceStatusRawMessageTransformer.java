package com.sequenceiq.freeipa.service.stack;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import com.sequenceiq.common.api.type.Tunnel;

@Service
public class ServiceStatusRawMessageTransformer {

    public String transformMessage(String rawNewStatusReason, Tunnel tunnel) {
        // TODO Needs to be deprecated after cluster proxy team introduce the new cluster-proxy.ccmv2.endpoint-unavailable status
        if (!tunnel.useCcmV1()) {
            return StringUtils.isNotEmpty(rawNewStatusReason)
                    ? rawNewStatusReason.replaceAll(".ccm.", ".ccmv2.")
                    : rawNewStatusReason;
        }
        return rawNewStatusReason;
    }
}
