package com.sequenceiq.redbeams.service.stack;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.ConversionService;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.redbeams.domain.stack.DBResource;
import com.sequenceiq.redbeams.repository.DBResourceRepository;

@Service
public class DBResourceService {

    @Inject
    private DBResourceRepository dbResourceRepository;

    @Inject
    @Qualifier("conversionService")
    private ConversionService conversionService;

    public List<CloudResourceStatus> getAllAsCloudResourceStatus(Long dbStackId) {
        return getAllAsCloudResource(dbStackId).stream()
                .map(r -> new CloudResourceStatus(r, ResourceStatus.CREATED))
                .collect(Collectors.toList());
    }

    public List<CloudResource> getAllAsCloudResource(Long dbStackId) {
        return dbResourceRepository.findAllByStackId(dbStackId).stream()
            .map(r -> conversionService.convert(r, CloudResource.class)).collect(Collectors.toList());
    }

    public DBResource save(DBResource resource) {
        return dbResourceRepository.save(resource);
    }

    public void delete(DBResource resource) {
        dbResourceRepository.delete(resource);
    }

    public Optional<DBResource> findByStackAndNameAndType(Long id, String name, ResourceType resourceType) {
        return dbResourceRepository.findByStackIdAndNameAndType(id, name, resourceType);
    }
}
