package com.sequenceiq.cloudbreak.ha.service;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.ha.domain.CloudbreakNode;

public interface FlowDistributor {

    Map<CloudbreakNode, List<String>> distribute(List<String> flows, List<CloudbreakNode> nodes);
}
