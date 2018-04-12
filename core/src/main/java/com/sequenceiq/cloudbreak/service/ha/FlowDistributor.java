package com.sequenceiq.cloudbreak.service.ha;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.domain.CloudbreakNode;

public interface FlowDistributor {

    Map<CloudbreakNode, List<String>> distribute(List<String> flows, List<CloudbreakNode> nodes);
}
