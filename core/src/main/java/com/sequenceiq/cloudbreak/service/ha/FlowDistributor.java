package com.sequenceiq.cloudbreak.service.ha;

import com.sequenceiq.cloudbreak.domain.CloudbreakNode;

import java.util.List;
import java.util.Map;

public interface FlowDistributor {

    Map<CloudbreakNode, List<String>> distribute(List<String> flows, List<CloudbreakNode> nodes);
}
