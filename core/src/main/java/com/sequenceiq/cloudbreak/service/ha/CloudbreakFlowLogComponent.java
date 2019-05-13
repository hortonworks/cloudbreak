package com.sequenceiq.cloudbreak.service.ha;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.ha.service.ServiceFlowLogComponent;
import com.sequenceiq.cloudbreak.repository.CloudbreakFlowLogRepository;

@Primary
@Component
public class CloudbreakFlowLogComponent implements ServiceFlowLogComponent {
    @Inject
    private CloudbreakFlowLogRepository cloudbreakFlowLogRepository;

    @Override
    public int purgeTerminatedStackLogs() {
        return cloudbreakFlowLogRepository.purgeTerminatedStackLogs();
    }

    @Override
    public Set<Long> findTerminatingStacksByCloudbreakNodeId(String cloudbreakNodeId) {
        return cloudbreakFlowLogRepository.findTerminatingStacksByCloudbreakNodeId(cloudbreakNodeId);
    }
}
