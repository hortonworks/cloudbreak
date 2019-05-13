package com.sequenceiq.cloudbreak.ha.service;

import java.util.List;
import java.util.Map;

import com.sequenceiq.cloudbreak.ha.domain.Node;

public interface FlowDistributor {

    Map<Node, List<String>> distribute(List<String> flows, List<Node> nodes);
}
