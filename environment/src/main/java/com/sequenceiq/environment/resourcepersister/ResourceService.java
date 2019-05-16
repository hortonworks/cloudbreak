package com.sequenceiq.environment.resourcepersister;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

@Service
public class ResourceService {

    @Inject
    private ResourceRepository repository;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public List<CloudResourceStatus> getAllAsCloudResourceStatus(String crn) {
        List<Resource> resources = repository.findAllByStackId(crn);
        List<CloudResourceStatus> list = new ArrayList<>();
        resources.forEach(r -> {
            CloudResource cloudResource = conversionService.convert(r, CloudResource.class);
            list.add(new CloudResourceStatus(cloudResource, ResourceStatus.CREATED));
        });

        return list;
    }

    public List<Resource> findAllByStackId(String crn) {
        return repository.findAllByStackId(crn);
    }

    public Optional<Resource> findByStackIdAndNameAndType(String crn, String name, ResourceType type) {
        return repository.findByStackIdAndNameAndType(crn, name, type);
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

}
