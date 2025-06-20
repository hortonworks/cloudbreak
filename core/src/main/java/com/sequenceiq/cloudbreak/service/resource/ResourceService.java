package com.sequenceiq.cloudbreak.service.resource;

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
import com.sequenceiq.cloudbreak.converter.spi.ResourceToCloudResourceConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@Service
public class ResourceService {

    @Inject
    private ResourceRepository repository;

    @Inject
    private ResourceToCloudResourceConverter cloudResourceToResourceConverter;

    public List<CloudResourceStatus> getAllAsCloudResourceStatus(Long stackId) {
        List<Resource> resources = repository.findAllByStackId(stackId);
        List<CloudResourceStatus> list = new ArrayList<>();
        resources.forEach(r -> {
            CloudResource cloudResource = cloudResourceToResourceConverter.convert(r);
            list.add(new CloudResourceStatus(cloudResource, ResourceStatus.CREATED));
        });

        return list;
    }

    public List<CloudResource> getAllCloudResource(Long stackId) {
        return repository.findAllByStackId(stackId).stream()
                .map(resource -> cloudResourceToResourceConverter.convert(resource))
                .collect(Collectors.toList());
    }

    public Collection<Resource> getAllByStackId(Long stackId) {
        return repository.findAllByStackId(stackId);
    }

    public Optional<Resource> findByStackIdAndNameAndType(Long stackId, String name, ResourceType type) {
        return repository.findByStackIdAndNameAndType(stackId, name, type);
    }

    public List<Resource> findByStackIdAndType(Long stackId, ResourceType type) {
        return repository.findByStackIdAndType(stackId, type);
    }

    public List<Resource> findByStackIdsAndType(List<Long> stackIdList, ResourceType type) {
        return repository.findByStackIdsAndType(stackIdList, type);
    }

    public List<Resource> findAllByStackIdAndResourceTypeIn(Long stackId, Collection<ResourceType> resourceTypes) {
        return repository.findAllByStackIdAndResourceTypeIn(stackId, resourceTypes);
    }

    public List<Resource> findAllByResourceStatusAndResourceTypeAndStackIdAndInstanceGroup(CommonStatus status, ResourceType resourceType, Long stackId,
            String instanceGroup) {
        return repository.findAllByResourceStatusAndResourceTypeAndStackIdAndInstanceGroup(status, resourceType, stackId, instanceGroup);
    }

    public List<Resource> findAllByResourceStatusAndResourceTypeAndStackId(CommonStatus status, ResourceType resourceType, Long stackId) {
        return repository.findAllByResourceStatusAndResourceTypeAndStackId(status, resourceType, stackId);
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

    public List<Resource> findByResourceReferencesAndStatusAndTypeAndStack(List<String> resourceReferences, CommonStatus status, ResourceType resourceType,
            Long stackId) {
        return repository.findByResourceReferenceAndStatusAndTypeAndStack(resourceReferences, status, resourceType, stackId);
    }

    public Optional<Resource> findFirstByStatusAndTypeAndStack(CommonStatus status, ResourceType resourceType, Long stackId) {
        return repository.findFirstByResourceStatusAndResourceTypeAndStackId(status, resourceType, stackId);
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

    public void deleteByResourceReferenceAndType(String reference, ResourceType type) {
        repository.deleteByResourceReferenceAndType(reference, type);
    }

    public void deleteAll(List<Resource> resources) {
        repository.deleteAll(resources);
    }

    public void deleteByStackId(Long stackId) {
        repository.deleteByStackId(stackId);
    }

    public List<Resource> findAllByStackIdAndInstanceGroupAndResourceTypeIn(Long stackId, String instanceGroup, Collection<ResourceType> resourceTypes) {
        return repository.findAllByStackIdAndInstanceGroupAndResourceTypeInAndInstanceIdIsNotNull(stackId, instanceGroup, resourceTypes);
    }

    public Iterable<Resource> findAllByResourceId(Iterable<Long> resourceIds) {
        return repository.findAllById(resourceIds);
    }

    public Optional<Resource> findById(Long id) {
        return repository.findById(id);
    }
}