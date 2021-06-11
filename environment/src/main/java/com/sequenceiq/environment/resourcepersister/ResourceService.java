package com.sequenceiq.environment.resourcepersister;

import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class ResourceService {

    @Inject
    private ResourceRepository repository;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public Optional<Resource> findByEnvironmentIdAndNameAndType(Long environmentId, String name, ResourceType type) {
        return repository.findByEnvironmentIdAndNameAndType(environmentId, name, type);
    }

    public void delete(Resource resource) {
        repository.delete(resource);
    }

    public Resource save(Resource resource) {
        return repository.save(resource);
    }

    public Iterable<Resource> saveAll(Iterable<Resource> resources) {
        return repository.saveAll(resources);
    }

    public Optional<Resource> findByResourceReferenceAndStatusAndType(String resourceReference, CommonStatus status, ResourceType resourceType) {
        return repository.findByResourceReferenceAndStatusAndType(resourceReference, status, resourceType);
    }

    public Optional<Resource> findByResourceReferenceAndType(String resourceReference, ResourceType resourceType) {
        return repository.findByResourceReferenceAndType(resourceReference, resourceType);
    }

    public Optional<Resource> findByEnvironmentIdAndType(Long environmentId, ResourceType resourceType) {
        return repository.findByEnvironmentIdAndType(environmentId, resourceType);
    }
}
