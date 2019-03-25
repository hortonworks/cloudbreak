package com.sequenceiq.it.cloudbreak.newway.util.storagelocation;

import java.util.Set;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.storage.location.StorageLocationV4Request;

public enum StorageComponent {

    HIVE {
        @Override
        public Set<StorageLocationV4Request> getLocations(TestStorageLocation tsl) {
            return Set.of(
                    createStorageLocationRequest(
                            String.format("%s//%s/warehouse/tablespace/managed/hive", tsl.getStorageTypePrefix(), tsl.getPathInfix()),
                            "hive-site",
                            "hive.metastore.warehouse.dir"),
                    createStorageLocationRequest(
                            String.format("%s//%s/warehouse/tablespace/external/hive", tsl.getStorageTypePrefix(), tsl.getPathInfix()),
                            "hive-site",
                            "hive.metastore.warehouse.external.dir"));
        }
    },

    SPARK2 {
        @Override
        public Set<StorageLocationV4Request> getLocations(TestStorageLocation tsl) {
            return Set.of(
                    createStorageLocationRequest(
                            String.format("%s//%s/%s/oplogs/spark2-history", tsl.getStorageTypePrefix(), tsl.getPathInfix(), tsl.getClusterName()),
                            "spark2-defaults",
                            "spark.eventLog.dir"),
                    createStorageLocationRequest(
                            String.format("%s//%s/%s/oplogs/spark2-history", tsl.getStorageTypePrefix(), tsl.getPathInfix(), tsl.getClusterName()),
                            "spark2-defaults",
                            "spark.history.fs.logDirectory"),
                    createStorageLocationRequest(
                            String.format("%s//%s/warehouse/spark", tsl.getStorageTypePrefix(), tsl.getPathInfix()),
                            "spark2-defaults",
                            "spark.sql.warehouse.dir")
            );
        }
    },

    TEZ {
        @Override
        public Set<StorageLocationV4Request> getLocations(TestStorageLocation tsl) {
            return Set.of(
                    createStorageLocationRequest(
                            String.format("%s//%s/warehouse/tablespace/external/hive/sys.db/query_data", tsl.getStorageTypePrefix(), tsl.getPathInfix()),
                            "tez-site",
                            "tez.history.logging.proto-base-dir")
            );
        }
    },

    RANGER {
        @Override
        public Set<StorageLocationV4Request> getLocations(TestStorageLocation tsl) {
            return Set.of(
                    createStorageLocationRequest(
                            String.format("%s//%s/ranger/audit", tsl.getStorageTypePrefix(), tsl.getPathInfix()),
                            "ranger-hive-audit",
                            "xasecure.audit.destination.hdfs.dir")
            );
        }
    },

    YARN {
        @Override
        public Set<StorageLocationV4Request> getLocations(TestStorageLocation tsl) {
            return Set.of(
                    createStorageLocationRequest(
                            String.format("%s//%s/%s/oplogs/yarn-app-logs", tsl.getStorageTypePrefix(), tsl.getPathInfix(), tsl.getClusterName()),
                            "yarn-site",
                            "yarn.nodemanager.remote-app-log-dir")
            );
        }
    },

    ZEPPELIN {
        @Override
        public Set<StorageLocationV4Request> getLocations(TestStorageLocation tsl) {
            return Set.of(createStorageLocationRequest(
                    String.format("%s//%s/zeppelin/notebook", tsl.getStorageTypePrefix(), tsl.getPathInfix()),
                    "zeppelin-site",
                    "zeppelin.notebook.dir"));
        }
    };

    public abstract Set<StorageLocationV4Request> getLocations(TestStorageLocation tsl);

    private static StorageLocationV4Request createStorageLocationRequest(String value, String propertyFile, String propertyName) {
        StorageLocationV4Request request = new StorageLocationV4Request();
        request.setValue(value);
        request.setPropertyFile(propertyFile);
        request.setPropertyName(propertyName);
        return request;
    }

}
