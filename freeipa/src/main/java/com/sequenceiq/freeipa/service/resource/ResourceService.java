package com.sequenceiq.freeipa.service.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.converter.cloud.ResourceToCloudResourceConverter;
import com.sequenceiq.freeipa.entity.Resource;
import com.sequenceiq.freeipa.repository.ResourceRepository;

@Service
public class ResourceService {

    @Inject
    private ResourceRepository repository;

    @Inject
    private ResourceToCloudResourceConverter resourceToCloudResourceConverter;

    public List<CloudResourceStatus> getAllAsCloudResourceStatus(Long stackId) {
        List<Resource> resources = repository.findAllByStackId(stackId);
        List<CloudResourceStatus> list = new ArrayList<>();
        resources.forEach(r -> {
            CloudResource cloudResource = resourceToCloudResourceConverter.convert(r);
            list.add(new CloudResourceStatus(cloudResource, ResourceStatus.CREATED));
        });

        return list;
    }

    public Optional<Resource> findResourceById(Long resourceId) {
        return repository.findById(resourceId);
    }

    public Iterable<Resource> findAllByResourceId(Iterable<Long> resourceIds) {
        return repository.findAllById(resourceIds);
    }

    public List<Resource> findAllByStackId(Long id) {
        return repository.findAllByStackId(id);
    }

    public Optional<Resource> findByStackIdAndNameAndType(Long stackId, String name, ResourceType type) {
        return repository.findByStackIdAndNameAndType(stackId, name, type);
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

    public List<Resource> findByResourceReferencesAndStatusAndType(List<String> resourceReferences, CommonStatus status, ResourceType resourceType) {
        return repository.findByResourceReferencesAndStatusAndType(resourceReferences, status, resourceType);
    }

    public Optional<Resource> findByResourceReferenceAndType(String resourceReference, ResourceType resourceType) {
        return repository.findByResourceReferenceAndType(resourceReference, resourceType);
    }

    public List<Resource> findByResourceReferencesAndStatusAndTypeAndStack(List<String> resourceReferences, CommonStatus status,
            ResourceType resourceType, Long stackId) {
        return repository.findByResourceReferencesAndStatusAndTypeAndStack(resourceReferences, status, resourceType, stackId);
    }

    public List<Resource> findAllByResourceStatusAndResourceTypeAndStackId(CommonStatus status, ResourceType resourceType, Long stackId) {
        return repository.findAllByResourceStatusAndResourceTypeAndStackId(status, resourceType, stackId);
    }

    public boolean existsByStackIdAndNameAndType(Long stackId, String name, ResourceType type) {
        return repository.existsByStackIdAndNameAndType(stackId, name, type);
    }

    public boolean existsByResourceReferenceAndType(String resourceReference, ResourceType resourceType) {
        return repository.existsByResourceReferenceAndType(resourceReference, resourceType);
    }

    public void deleteByStackIdAndNameAndType(Long stackId, String name, ResourceType type) {
        repository.deleteByStackIdAndNameAndType(stackId, name, type);
    }

    public Optional<Resource> findFirstByStatusAndTypeAndStack(CommonStatus status, ResourceType resourceType, Long stackId) {
        return repository.findFirstByResourceStatusAndResourceTypeAndStackId(status, resourceType, stackId);
    }

    public List<CloudResource> getAllCloudResource(Long stackId) {
        return findAllByStackId(stackId).stream()
                .map(resource -> resourceToCloudResourceConverter.convert(resource))
                .collect(Collectors.toList());
    }

    public List<Resource> findAllByStackIdAndInstanceGroupAndResourceTypeIn(Long stackId, String instanceGroup, Collection<ResourceType> resourceTypes) {
        return repository.findAllByStackIdAndInstanceGroupAndResourceTypeInAndInstanceIdIsNotNull(stackId, instanceGroup, resourceTypes);
    }
}
