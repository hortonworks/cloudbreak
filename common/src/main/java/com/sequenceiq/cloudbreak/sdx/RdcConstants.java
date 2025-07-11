package com.sequenceiq.cloudbreak.sdx;

public class RdcConstants {

    public static final String HIVE_SERVICE = "HIVE";

    public static final String HDFS_SERVICE = "HDFS";

    public static final String TEZ_SERVICE = "TEZ";

    private RdcConstants() {

    }

    public static class HiveMetastoreDatabase {

        public static final String HIVE_METASTORE_DATABASE_HOST = "hive_metastore_database_host";

        public static final String HIVE_METASTORE_DATABASE_NAME = "hive_metastore_database_name";

        public static final String HIVE_METASTORE_DATABASE_PASSWORD = "hive_metastore_database_password";

        public static final String HIVE_METASTORE_DATABASE_PORT = "hive_metastore_database_port";

        public static final String HIVE_METASTORE_DATABASE_TYPE = "hive_metastore_database_type";

        public static final String HIVE_METASTORE_DATABASE_USER = "hive_metastore_database_user";

    }

    public static class Hive {

        public static final String HIVE_WAREHOUSE_DIRECTORY = "hive_warehouse_directory";

        public static final String HIVE_WAREHOUSE_EXTERNAL_DIRECTORY = "hive_warehouse_external_directory";
    }

    public static class HdfsNameNode {

        public static final String HDFS_NAMENODE_ROLE_TYPE = "NAMENODE";

        public static final String HDFS_NAMENODE_NAMESERVICE = "dfs.federation.namenode.nameservice";

    }
}
