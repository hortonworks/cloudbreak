package com.sequenceiq.mock.spi;

import static org.springframework.http.HttpStatus.NOT_FOUND;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.ExternalDatabaseStatus;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.mock.clouderamanager.DefaultModelService;

@Service
public class DbStoreService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DbStoreService.class);

    @Inject
    private DefaultModelService defaultModelService;

    private final Map<String, DbDto> dbMap = new ConcurrentHashMap<>();

    public List<CloudResourceStatus> store(String mockUuid, DatabaseStack databaseStack) {
        LOGGER.info("New databaseStack will be created for {}, {}", mockUuid, databaseStack);
        DbDto dbDto = new DbDto(mockUuid, databaseStack, ExternalDatabaseStatus.STARTED);
        dbMap.put(mockUuid, dbDto);
        return databaseCloudResourceStatus(databaseStack);
    }

    private List<CloudResourceStatus> databaseCloudResourceStatus(DatabaseStack databaseStack) {
        return List.of(
                new CloudResourceStatus(CloudResource.builder()
                        .withType(ResourceType.MOCK_DATABASE)
                        .withName("mock database")
                        .withStatus(CommonStatus.CREATED)
                        .withParameters(Map.of())
                        .build(),
                        ResourceStatus.CREATED),
                new CloudResourceStatus(CloudResource.builder()
                        .withType(ResourceType.RDS_HOSTNAME)
                        .withName("mock_database")
                        .withStatus(CommonStatus.CREATED)
                        .withParameters(Map.of())
                        .build(),
                        ResourceStatus.CREATED),
                new CloudResourceStatus(CloudResource.builder()
                        .withType(ResourceType.RDS_PORT)
                        .withName("8989")
                        .withStatus(CommonStatus.CREATED)
                        .withParameters(Map.of())
                        .build(),
                        ResourceStatus.CREATED));
    }

    public DbDto read(String mockUuid) {
        LOGGER.info("Fetch db by {}", mockUuid);
        DbDto dbDto = dbMap.get(mockUuid);
        if (dbDto == null) {
            LOGGER.info("Cannot find any db by {}", mockUuid);
            throw new ResponseStatusException(NOT_FOUND, "DbDto cannot be found by uuid: " + mockUuid);
        }
        return dbDto;
    }

    public void terminate(String mockUuid) {
        LOGGER.info("Terminate {}", mockUuid);
        dbMap.remove(mockUuid);
    }

    public Collection<DbDto> getAll() {
        LOGGER.info("List all spi");
        return dbMap.values();
    }
}
