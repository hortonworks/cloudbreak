package com.sequenceiq.datalake.service.sdx.database;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.database.DatabaseServerIncreaseStorageSizeV4Response;

@Service
public class SdxDatabaseServerIncreaseStorageSizeService {

    // TODO: replace the stub with the increase storage size flow trigger
    public DatabaseServerIncreaseStorageSizeV4Response increaseStorageSize(String userCrn, NameOrCrn nameOrCrn, int storageSizeInGb) {
        throw new UnsupportedOperationException("Increasing the storage size of the database server of a Data Lake is not implemented yet");
    }
}
