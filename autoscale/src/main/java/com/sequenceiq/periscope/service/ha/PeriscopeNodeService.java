package com.sequenceiq.periscope.service.ha;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.periscope.repository.PeriscopeNodeRepository;

@Service
public class PeriscopeNodeService {

    @Inject
    private PeriscopeNodeRepository periscopeNodeRepository;

    public boolean isLeader(String nodeId) {
        return isNullOrEmpty(nodeId) || periscopeNodeRepository.findById(nodeId).orElseThrow(() -> new NotFoundException(format("PeriscopeNode '%s' not found",
                nodeId))).isLeader();
    }
}
