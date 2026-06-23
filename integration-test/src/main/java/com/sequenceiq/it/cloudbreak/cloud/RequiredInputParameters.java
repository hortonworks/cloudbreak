package com.sequenceiq.it.cloudbreak.cloud;

public class RequiredInputParameters {

    public static class Aws {

        public static class Database {

            public static class Hive {
                public static final String CONFIG_NAME = "NN_AWS_DB_HIVE_CONFIG_NAME";

                public static final String USER_NAME_KEY = "NN_AWS_DB_HIVE_USER_NAME";

                public static final String PASSWORD_KEY = "NN_AWS_DB_HIVE_PASSWORD";

                public static final String CONNECTION_URL_KEY = "NN_AWS_DB_HIVE_CONNECTION_URL";
            }

            public static class Ranger {
                public static final String CONFIG_NAME = "NN_AWS_DB_RANGER_CONFIG_NAME";

                public static final String USER_NAME_KEY = "NN_AWS_DB_RANGER_USER_NAME";

                public static final String PASSWORD_KEY = "NN_AWS_DB_RANGER_PASSWORD";

                public static final String CONNECTION_URL_KEY = "NN_AWS_DB_RANGER_CONNECTION_URL";

            }
        }

        public static class Storage {
            public static final String S3_BUCKET_NAME = "NN_AWS_S3_BUCKET_NAME";
        }
    }

    public static class Gcp {
        public static class Database {

            public static class Hive {
                public static final String CONFIG_NAME = "NN_GCP_DB_HIVE_CONFIG_NAME";

                public static final String USER_NAME_KEY = "NN_GCP_DB_HIVE_USER_NAME";

                public static final String PASSWORD_KEY = "NN_GCP_DB_HIVE_PASSWORD";

                public static final String CONNECTION_URL_KEY = "NN_GCP_DB_HIVE_CONNECTION_URL";
            }

            public static class Ranger {
                public static final String CONFIG_NAME = "NN_GCP_DB_RANGER_CONFIG_NAME";

                public static final String USER_NAME_KEY = "NN_GCP_DB_RANGER_USER_NAME";

                public static final String PASSWORD_KEY = "NN_GCP_DB_RANGER_PASSWORD";

                public static final String CONNECTION_URL_KEY = "NN_GCP_DB_RANGER_CONNECTION_URL";

            }
        }

        public static class Storage {
            public static final String BUCKET_NAME = "NN_GCP_BUCKET_NAME";
        }
    }

    public static class Azure {

        public static class Database {

            public static class Hive {
                public static final String CONFIG_NAME = "NN_AZ_DB_HIVE_CONFIG_NAME";

                public static final String USER_NAME_KEY = "NN_AZ_DB_HIVE_USERNAME";

                public static final String PASSWORD_KEY = "NN_AZ_DB_HIVE_PASSWORD";

                public static final String CONNECTION_URL_KEY = "NN_AZ_DB_HIVE_CONNECTION_URL";
            }

            public static class Ranger {
                public static final String CONFIG_NAME = "NN_AZ_DB_RANGER_CONFIG_NAME";

                public static final String USER_NAME_KEY = "NN_AZ_DB_RANGER_USERNAME";

                public static final String PASSWORD_KEY = "NN_AZ_DB_RANGER_PASSWORD";

                public static final String CONNECTION_URL_KEY = "NN_AZ_DB_RANGER_CONNECTION_URL";

            }
        }

        public static class Storage {

            public static class Wasb {

                public static final String STORAGE_NAME = "NN_AZ_STORAGE_BLOB";

                public static final String ACCESS_KEY = "NN_AZ_STORAGE_ACCESS_KEY";

                public static final String ACCOUNT = "NN_AZ_STORAGE_ACCOUNT";
            }

            public static class Adls {

                public static final String ACCOUNT_NAME = "NN_AZ_DATALAKE";

            }
        }
    }

    public static class Ldap {
        public static final String BIND_DN = "NN_LDAP_BIND_DN";

        public static final String LDAP_CONFIG_NAME = "NN_LDAP";

        public static final String LDAP_DOMAIN = "NN_LDAP_DOMAIN";

        public static final String SERVER_HOST = "NN_LDAP_SERVER_HOST";

        public static final String SERVER_PORT = "NN_LDAP_SERVER_PORT";

        public static final String ADMIN_GROUP = "NN_LDAP_ADMIN_GROUP";

        public static final String BIND_PASSWORD = "NN_LDAP_BIND_PASSWORD";

        public static final String DIRECTORY_TYPE = "NN_LDAP_DIRECTORY_TYPE";

        public static final String USER_DN_PATTERN = "NN_LDAP_USER_DN_PATTERN";

        public static final String SERVER_PROTOCOL = "NN_LDAP_SERVER_PROTOCOL";

        public static final String USER_SEARCH_BASE = "NN_LDAP_USER_SEARCH_BASE";

        public static final String USER_OBJECT_CLASS = "NN_LDAP_USER_OBJECT_CLASS";

        public static final String GROUP_SEARCH_BASE = "NN_LDAP_GROUP_SEARCH_BASE";

        public static final String GROUP_OBJECT_CLASS = "NN_LDAP_GROUP_OBJECT_CLASS";

        public static final String USER_NAME_ATTRIBUTE = "NN_LDAP_USER_NAME_ATTRIBUTE";

        public static final String GROUP_NAME_ATTRIBUTE = "NN_LDAP_GROUP_NAME_ATTRIBUTE";

        public static final String GROUP_MEMBER_ATTRIBUTE = "NN_LDAP_GROUP_MEMBER_ATTRIBUTE";
    }
}
