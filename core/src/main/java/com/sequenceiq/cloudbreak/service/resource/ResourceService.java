package com.sequenceiq.cloudbreak.service.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;

@Service
public class ResourceService {

    @Inject
    private ResourceRepository resourceRepository;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public List<CloudResourceStatus> getAllAsCloudResourceStatus(Long stackId) {
        List<Resource> resources = resourceRepository.findAllByStackId(stackId);
        List<CloudResourceStatus> list = new ArrayList<>();
        resources.forEach(r -> {
            CloudResource cloudResource = conversionService.convert(r, CloudResource.class);
            list.add(new CloudResourceStatus(cloudResource, ResourceStatus.CREATED));
        });

        return list;
    }

    public Collection<Resource> getAllByStackId(Long stackId) {
        return resourceRepository.findAllByStackId(stackId);
    }

    public Set<Resource> getNotInstanceRelatedByStackId(Long stackId) {
        return resourceRepository.findAllByStackIdNotInstanceOrDisk(stackId);
    }
}
