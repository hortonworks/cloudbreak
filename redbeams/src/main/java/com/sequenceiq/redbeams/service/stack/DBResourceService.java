package com.sequenceiq.redbeams.service.stack;

import static java.util.stream.Collectors.toCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.redbeams.converter.spi.DBResourceToCloudResourceConverter;
import com.sequenceiq.redbeams.domain.stack.DBResource;
import com.sequenceiq.redbeams.repository.DBResourceRepository;

@Service
public class DBResourceService {

    @Inject
    private DBResourceRepository dbResourceRepository;

    @Inject
    private DBResourceToCloudResourceConverter dbResourceToCloudResourceConverter;

    public List<CloudResource> getAllAsCloudResource(Long dbStackId) {
        return dbResourceRepository.findAllByStackId(dbStackId).stream()
                .map(dbResourceToCloudResourceConverter::convert)
                .collect(toCollection(ArrayList::new));
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

    public boolean existsByStackAndNameAndType(Long dbStackId, String name, ResourceType type) {
        return dbResourceRepository.existsByStackAndNameAndType(dbStackId, name, type);
    }

    public Optional<DBResource> findByStatusAndTypeAndStack(CommonStatus status, ResourceType resourceType, Long stackId) {
        return dbResourceRepository.findByResourceStatusAndResourceTypeAndDbStack(status, resourceType, stackId);
    }
}