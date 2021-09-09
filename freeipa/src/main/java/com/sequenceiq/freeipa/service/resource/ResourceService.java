package com.sequenceiq.freeipa.service.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

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

    public Optional<Resource> findByResourceReferenceAndStatusAndType(String resourceReference, CommonStatus status, ResourceType resourceType) {
        return repository.findByResourceReferenceAndStatusAndType(resourceReference, status, resourceType);
    }

    public Optional<Resource> findByResourceReferenceAndType(String resourceReference, ResourceType resourceType) {
        return repository.findByResourceReferenceAndType(resourceReference, resourceType);
    }

    public Optional<Resource> findByResourceReferenceAndStatusAndTypeAndStack(String resourceReference, CommonStatus status,
            ResourceType resourceType, Long stackId) {
        return repository.findByResourceReferenceAndStatusAndTypeAndStack(resourceReference, status, resourceType, stackId);
    }

}
