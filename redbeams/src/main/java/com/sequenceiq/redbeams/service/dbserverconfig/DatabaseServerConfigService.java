package com.sequenceiq.redbeams.service.dbserverconfig;

import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.redbeams.domain.DatabaseServerConfig;
import com.sequenceiq.redbeams.repository.DatabaseServerConfigRepository;

@Service
public class DatabaseServerConfigService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServerConfigService.class);

    @Inject
    private DatabaseServerConfigRepository repository;

    public Set<DatabaseServerConfig> findAllInWorkspaceAndEnvironment(Long workspaceId, String environmentId, Boolean attachGlobal) {
        return repository.findAllByWorkspaceIdAndEnvironmentId(workspaceId, environmentId);
    }

}
