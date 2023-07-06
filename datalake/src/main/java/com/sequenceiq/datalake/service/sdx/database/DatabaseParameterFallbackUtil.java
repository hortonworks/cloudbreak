package com.sequenceiq.datalake.service.sdx.database;

import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

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
        return sdxDatabase;
    }

    public static void setDatabaseCrn(SdxCluster sdxCluster, String databaseCrn) {
        if (sdxCluster.getSdxDatabase() == null) {
            throw new IllegalArgumentException("SdxDatabase cannot be null in sdxcluster!");
        }
        sdxCluster.getSdxDatabase().setDatabaseCrn(databaseCrn);
    }

    public static String getDatabaseCrn(SdxDatabase sdxDatabase, String fallbackDatabaseCrn) {
        return sdxDatabase != null ? sdxDatabase.getDatabaseCrn() : fallbackDatabaseCrn;
    }

    public static String getDatabaseEngineVersion(SdxDatabase sdxDatabase, String fallbackDatabaseEngineVersion) {
        String result;
        if (sdxDatabase != null) {
            String dbEngineVersion = Optional.of(sdxDatabase).map(SdxDatabase::getDatabaseEngineVersion).orElse(null);
            if (fallbackDatabaseEngineVersion != null && !StringUtils.equals(dbEngineVersion, fallbackDatabaseEngineVersion)) {
                result = fallbackDatabaseEngineVersion;
            } else {
                result = dbEngineVersion;
            }
        } else {
            result = fallbackDatabaseEngineVersion;
        }
        return result;
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
}
