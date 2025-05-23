package com.sequenceiq.it.cloudbreak.client;

import org.springframework.stereotype.Service;

import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.action.externalizedcompute.ExternalizedComputeClusterDeleteAction;
import com.sequenceiq.it.cloudbreak.action.externalizedcompute.ExternalizedComputeClusterDescribeAction;
import com.sequenceiq.it.cloudbreak.action.externalizedcompute.ExternalizedComputeClusterDescribeDefaultAction;
import com.sequenceiq.it.cloudbreak.action.externalizedcompute.ExternalizedComputeClusterForceReinitializeAction;
import com.sequenceiq.it.cloudbreak.action.externalizedcompute.ExternalizedComputeClusterNotFoundAction;
import com.sequenceiq.it.cloudbreak.dto.externalizedcompute.ExternalizedComputeClusterTestDto;
import com.sequenceiq.it.cloudbreak.microservice.ExternalizedComputeClusterClient;

@Service
public class ExternalizedComputeClusterTestClient {

    public Action<ExternalizedComputeClusterTestDto, ExternalizedComputeClusterClient> describeDefaultNotExists() {
        return new ExternalizedComputeClusterDescribeDefaultAction();
    }

    public Action<ExternalizedComputeClusterTestDto, ExternalizedComputeClusterClient> describeDefault() {
        return new ExternalizedComputeClusterDescribeDefaultAction();
    }

    public Action<ExternalizedComputeClusterTestDto, ExternalizedComputeClusterClient> describe() {
        return new ExternalizedComputeClusterDescribeAction();
    }

    public Action<ExternalizedComputeClusterTestDto, ExternalizedComputeClusterClient> describeDeleted() {
        return new ExternalizedComputeClusterNotFoundAction();
    }

    public Action<ExternalizedComputeClusterTestDto, ExternalizedComputeClusterClient> delete() {
        return new ExternalizedComputeClusterDeleteAction();
    }

    public Action<ExternalizedComputeClusterTestDto, ExternalizedComputeClusterClient> forceReinitialize() {
        return new ExternalizedComputeClusterForceReinitializeAction();
    }
}
