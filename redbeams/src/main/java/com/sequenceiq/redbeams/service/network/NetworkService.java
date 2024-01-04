package com.sequenceiq.redbeams.service.network;

import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.redbeams.domain.stack.Network;
import com.sequenceiq.redbeams.repository.NetworkRepository;

@Service
public class NetworkService {

    @Inject
    private NetworkRepository repository;

    public Optional<Network> findById(Long id) {
        return repository.findById(id);
    }

    public Network getById(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException(String.format("Network [%s] not found", id)));
    }

    public Network save(Network network) {
        return repository.save(network);
    }

    public void delete(Long id) {
        Optional.ofNullable(id).ifPresent(networkId -> repository.deleteById(networkId));
    }
}
