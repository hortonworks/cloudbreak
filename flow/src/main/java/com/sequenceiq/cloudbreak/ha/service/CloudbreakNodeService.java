package com.sequenceiq.cloudbreak.ha.service;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.ha.domain.CloudbreakNode;
import com.sequenceiq.cloudbreak.ha.repository.CloudbreakNodeRepository;

@Service
public class CloudbreakNodeService {

    @Inject
    private CloudbreakNodeRepository repository;

    public Optional<CloudbreakNode> findById(String nodeId) {
        return repository.findById(nodeId);
    }

    public CloudbreakNode save(CloudbreakNode node) {
        return repository.save(node);
    }

    public Iterable<CloudbreakNode> findAll() {
        return repository.findAll();
    }

    public void deleteAll(Iterable<CloudbreakNode> nodes) {
        repository.deleteAll(nodes);
    }

}
