package com.sequenceiq.it.cloudbreak.util.storagelocation;

import java.util.List;

import com.sequenceiq.common.api.cloudstorage.StorageLocationBase;
import com.sequenceiq.common.model.CloudStorageCdpService;

public enum StorageComponent {

    HIVE_METASTORE_WAREHOUSE {
        @Override
        public List<StorageLocationBase> getLocations(TestStorageLocation tsl) {
            return List.of(
                    createStorageLocationRequest(name(),
                            String.format("%s//%s/warehouse/tablespace/managed/hive", tsl.getStorageTypePrefix(), tsl.getPathInfix())
                    )
            );
        }
    },

    HIVE_METASTORE_EXTERNAL_WAREHOUSE {
        @Override
        public List<StorageLocationBase> getLocations(TestStorageLocation tsl) {
            return List.of(
                    createStorageLocationRequest(name(),
                            String.format("%s//%s/warehouse/tablespace/external/hive", tsl.getStorageTypePrefix(), tsl.getPathInfix())
                    )
            );
        }
    },

    SPARK2 {
        @Override
        public List<StorageLocationBase> getLocations(TestStorageLocation tsl) {
            return List.of(
                    createStorageLocationRequest(name(),
                            String.format("%s//%s/%s/oplogs/spark2-history", tsl.getStorageTypePrefix(), tsl.getPathInfix(), tsl.getClusterName())),
                    createStorageLocationRequest(name(),
                            String.format("%s//%s/%s/oplogs/spark2-history", tsl.getStorageTypePrefix(), tsl.getPathInfix(), tsl.getClusterName())),
                    createStorageLocationRequest(name(),
                            String.format("%s//%s/warehouse/spark", tsl.getStorageTypePrefix(), tsl.getPathInfix()))
            );
        }
    },

    TEZ {
        @Override
        public List<StorageLocationBase> getLocations(TestStorageLocation tsl) {
            return List.of(
                    createStorageLocationRequest(name(),
                            String.format("%s//%s/warehouse/tablespace/external/hive/sys.db/query_data",
                                    tsl.getStorageTypePrefix(), tsl.getPathInfix()))
            );
        }
    },

    RANGER {
        @Override
        public List<StorageLocationBase> getLocations(TestStorageLocation tsl) {
            return List.of(
                    createStorageLocationRequest(name(),
                            String.format("%s//%s/ranger/audit", tsl.getStorageTypePrefix(), tsl.getPathInfix()))
            );
        }
    },

    YARN {
        @Override
        public List<StorageLocationBase> getLocations(TestStorageLocation tsl) {
            return List.of(
                    createStorageLocationRequest(name(),
                            String.format("%s//%s/%s/oplogs/yarn-app-logs", tsl.getStorageTypePrefix(), tsl.getPathInfix(), tsl.getClusterName()))
            );
        }
    },

    ZEPPELIN {
        @Override
        public List<StorageLocationBase> getLocations(TestStorageLocation tsl) {
            return List.of(createStorageLocationRequest(name(),
                    String.format("%s//%s/zeppelin/notebook", tsl.getStorageTypePrefix(), tsl.getPathInfix())));
        }
    };

    public abstract List<StorageLocationBase> getLocations(TestStorageLocation tsl);

    private static StorageLocationBase createStorageLocationRequest(String type, String value) {
        StorageLocationBase request = new StorageLocationBase();
        request.setType(CloudStorageCdpService.valueOf(type));
        request.setValue(value);
        return request;
    }

}
