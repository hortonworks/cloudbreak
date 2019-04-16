package com.sequenceiq.cloudbreak.service.servicedescriptor;

import javax.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ServiceDescriptor;
import com.sequenceiq.cloudbreak.repository.cluster.ServiceDescriptorRepository;

@Service
public class ServiceDescriptorService {

    @Inject
    private ServiceDescriptorRepository repository;

    public ServiceDescriptor save(ServiceDescriptor serviceDescriptor) {
        return repository.save(serviceDescriptor);
    }

}
