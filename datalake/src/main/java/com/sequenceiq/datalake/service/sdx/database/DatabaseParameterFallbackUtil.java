package com.sequenceiq.datalake.service.sdx.database;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

// TODO It's only needed for handling backward compatibility, can be removed in CB-21369
public class DatabaseParameterFallbackUtil {
    private DatabaseParameterFallbackUtil() {
    }

    public static SdxDatabase setupDatabaseInitParams(SdxCluster sdxCluster, SdxDatabaseAvailabilityType databaseAvailabilityType, String dbEngineVersion) {
        return setupDatabaseInitParams(sdxCluster, databaseAvailabilityType, dbEngineVersion, null);
    }

    public static SdxDatabase setupDatabaseInitParams(SdxCluster sdxCluster, SdxDatabaseAvailabilityType databaseAvailabilityType, String dbEngineVersion,
            Json attributes) {
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseAvailabilityType(databaseAvailabilityType);
        sdxDatabase.setDatabaseEngineVersion(dbEngineVersion);
        sdxDatabase.setAttributes(attributes);
        // TODO only for backward compatibility, can be removed in CB-21369
        sdxCluster.setDatabaseAvailabilityType(databaseAvailabilityType);
        sdxCluster.setDatabaseEngineVersion(dbEngineVersion);
        return sdxDatabase;
    }

    public static void setDatabaseCrn(SdxCluster sdxCluster, String databaseCrn) {
        createSdxDatabaseIfMissing(sdxCluster);
        sdxCluster.getSdxDatabase().setDatabaseCrn(databaseCrn);
        // TODO only for backward compatibility, can be removed in CB-21369
        sdxCluster.setDatabaseCrn(databaseCrn);
    }

    public static String getDatabaseCrn(SdxDatabase sdxDatabase, String fallbackDatabaseCrn) {
        return sdxDatabase != null ? sdxDatabase.getDatabaseCrn() : fallbackDatabaseCrn;
    }

    public static String getDatabaseEngineVersion(SdxDatabase sdxDatabase, String fallbackDatabaseEngineVersion) {
        return sdxDatabase != null ? sdxDatabase.getDatabaseEngineVersion() : fallbackDatabaseEngineVersion;
    }

    public static boolean isCreateDatabase(SdxDatabase sdxDatabase, boolean fallbackCreateDatabase) {
        return sdxDatabase != null ? sdxDatabase.isCreateDatabase() : fallbackCreateDatabase;
    }

    public static SdxDatabaseAvailabilityType getDatabaseAvailabilityType(SdxDatabase sdxDatabase, SdxDatabaseAvailabilityType fallbackDatabaseAvailabilityType,
            boolean fallbackCreateDatabase) {
        if (sdxDatabase != null) {
            return getDatabaseAvailabilityType(sdxDatabase.getDatabaseAvailabilityType(), sdxDatabase.isCreateDatabase());
        } else {
            return getDatabaseAvailabilityType(fallbackDatabaseAvailabilityType, fallbackCreateDatabase);
        }
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

    private static void createSdxDatabaseIfMissing(SdxCluster sdxCluster) {
        SdxDatabase sdxDatabase = sdxCluster.getSdxDatabase();
        if (sdxDatabase == null) {
            sdxDatabase = new SdxDatabase();
            sdxDatabase.setDatabaseEngineVersion(sdxCluster.getDatabaseEngineVersion());
            sdxDatabase.setDatabaseAvailabilityType(sdxCluster.getDatabaseAvailabilityType());
            sdxDatabase.setDatabaseCrn(sdxCluster.getDatabaseCrn());
            sdxCluster.setSdxDatabase(sdxDatabase);
        }
    }
}
