package com.sequenceiq.cloudbreak.service;

import static com.sequenceiq.cloudbreak.util.NameUtil.generateArchiveName;

import java.io.Serializable;

import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.ArchivableResource;

public interface ArchivableResourceService<T extends ArchivableResource, ID extends Serializable> {

    default void archive(T resource, DisabledBaseRepository<T, ID> repository) {
        setArchivedName(resource);
        unsetConnectionsToDeletables(resource);
        resource.setArchived(true);
        repository.save(resource);
    }

    void unsetConnectionsToDeletables(T resource);

    default void setArchivedName(T resource) {
        resource.setName(generateArchiveName(resource.getName()));
    }
}
