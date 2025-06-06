package com.sequenceiq.datalake.service.sdx.database;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

public class DatabaseParameterInitUtil {
    private DatabaseParameterInitUtil() {
    }

    public static SdxDatabase setupDatabaseInitParams(SdxDatabaseAvailabilityType databaseAvailabilityType, String dbEngineVersion) {
        return setupDatabaseInitParams(databaseAvailabilityType, dbEngineVersion, null);
    }

    public static SdxDatabase setupDatabaseInitParams(SdxDatabaseAvailabilityType databaseAvailabilityType, String dbEngineVersion,
            Json attributes) {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseAvailabilityType(databaseAvailabilityType);
        sdxDatabase.setDatabaseEngineVersion(dbEngineVersion);
        sdxDatabase.setAttributes(attributes);
        return sdxDatabase;
    }

    public static void setDatabaseCrn(SdxCluster sdxCluster, String databaseCrn) {
        if (sdxCluster.getSdxDatabase() == null) {
            throw new IllegalArgumentException("SdxDatabase cannot be null in sdxcluster!");
        }
        sdxCluster.getSdxDatabase().setDatabaseCrn(databaseCrn);
    }

    public static SdxDatabaseAvailabilityType getDatabaseAvailabilityType(SdxDatabaseAvailabilityType databaseAvailabilityType, boolean createDatabase) {
        if (databaseAvailabilityType != null) {
            return databaseAvailabilityType;
        } else {
            if (createDatabase) {
                return SdxDatabaseAvailabilityType.HA;
            } else {
                return SdxDatabaseAvailabilityType.NONE;
            }
        }
    }
}
