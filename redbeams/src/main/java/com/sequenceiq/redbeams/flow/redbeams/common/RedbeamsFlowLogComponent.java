package com.sequenceiq.redbeams.flow.redbeams.common;

import java.util.Set;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.ha.service.ServiceFlowLogComponent;
import com.sequenceiq.redbeams.repository.RedbeamsFlowLogRepository;

@Primary
@Component
public class RedbeamsFlowLogComponent implements ServiceFlowLogComponent {

    private final RedbeamsFlowLogRepository redbeamsFlowLogRepository;

    public RedbeamsFlowLogComponent(RedbeamsFlowLogRepository redbeamsFlowLogRepository) {
        this.redbeamsFlowLogRepository = redbeamsFlowLogRepository;
    }

    @Override
    public int purgeTerminatedResourceLogs() {
        return redbeamsFlowLogRepository.purgeDeletedDbStacksLogs();
    }

    @Override
    public Set<Long> findTerminatingResourcesByNodeId(String nodeId) {
        return redbeamsFlowLogRepository.findTerminatingResourcesByNodeId(nodeId);
    }
}
