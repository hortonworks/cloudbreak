package com.sequenceiq.cloudbreak.ha.service;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.ha.domain.Node;
import com.sequenceiq.cloudbreak.ha.repository.NodeRepository;

@Service
public class NodeService {

    @Inject
    private NodeRepository repository;

    public Optional<Node> findById(String nodeId) {
        return repository.findById(nodeId);
    }

    public Node save(Node node) {
        return repository.save(node);
    }

    public Iterable<Node> findAll() {
        return repository.findAll();
    }

    public void deleteAll(Iterable<Node> nodes) {
        repository.deleteAll(nodes);
    }

}
