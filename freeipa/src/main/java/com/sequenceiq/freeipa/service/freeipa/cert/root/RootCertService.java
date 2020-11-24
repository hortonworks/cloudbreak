package com.sequenceiq.freeipa.service.freeipa.cert.root;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.freeipa.entity.RootCert;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.repository.RootCertRepository;

@Service
public class RootCertService {

    @Inject
    private RootCertRepository repository;

    public Optional<RootCert> findByEnvironmentCrn(String environmentCrn) {
        return repository.findByEnvironmentCrn(environmentCrn);
    }

    public void deleteByStack(Stack stack) {
        repository.deleteByStackId(stack.getId());
    }

    public RootCert save(RootCert rootCert) {
        return repository.save(rootCert);
    }
}
