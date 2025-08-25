package com.sequenceiq.redbeams.doc;

public final class Notes {

    private Notes() {
    }

    public static final class DatabaseNotes {

        public static final String TEST_CONNECTION =
            "Tests connectivity to a database. Use this to verify access to the database from this service, and also "
            + "to verify authentication credentials.";
        public static final String LIST =
            "Lists all databases that are known, either because they were registered or because this service created "
            + "them.";
        public static final String GET_BY_CRN =
            "Gets information on a database by its CRN.";
        public static final String GET_BY_NAME =
            "Gets information on a database by its name.";
        public static final String CREATE =
            "Creates a new database on a database server. The database starts out empty. A new user with credentials "
            + "separate from the database server's administrative user is also created, with full rights to the new database.";
        public static final String REGISTER =
            "Registers an existing database, residing on some database server.";
        public static final String DELETE_BY_CRN =
            "Deletes a database by its CRN. If the database was registered with this service, then this operation "
            + "merely deregisters it. Otherwise, this operation deletes the database from the database server, along "
            + "with its corresponding user.";
        public static final String DELETE_BY_NAME =
            "Deletes a database by its name. If the database was registered with this service, then this operation "
            + "merely deregisters it. Otherwise, this operation deletes the database from the database server, along "
            + "with its corresponding user.";
        public static final String DELETE_MULTIPLE_BY_CRN =
            "Deletes multiple databases, each by CRN. See the notes on the single delete operation for details.";
        public static final String GET_USED_SUBNETS_BY_ENVIRONMENT_CRN = "List the used subnets by the given Environment resource CRN";

        private DatabaseNotes() {
        }
    }

    public static final class DatabaseServerNotes {

        public static final String TEST_CONNECTION =
            "Tests connectivity to a database. Use this to verify access to the database server from this service, "
            + "and also to verify authentication credentials.";
        public static final String LIST =
            "Lists all database servers that are known, either because they were registered or because this service "
            + "created them.";
        public static final String LIST_CERTIFICATE_STATUS =
                "Lists all database servers certificate status that are known, either because they were registered or because this service "
                        + "created them.";
        public static final String GET_BY_NAME =
            "Gets information on a database server by its name.";
        public static final String GET_BY_CRN =
            "Gets information on a database server by its CRN.";
        public static final String GET_BY_CLUSTER_CRN =
                "Gets information on a database server by cluster CRN";
        public static final String LIST_BY_CLUSTER_CRN =
                "Lists all database servers by cluster CRN";
        public static final String CREATE =
            "Creates a new database server. The database server starts out with only default databases.";
        public static final String UPGRADE =
                "Upgrades a database server to a higher major version.";
        public static final String ROTATE =
                "Rotates database server secrets";
        public static final String VALIDATE_UPGRADE =
                "Validates if upgrade is possible on the database server to a higher major version.";
        public static final String VALIDATE_UPGRADE_CLEANUP =
                "Cleans up the validation related cloud resources of the database server.";

        public static final String UPDATE_CLUSTER_CRN =
                "Updates the cluster crn associated with the database";
        public static final String RELEASE =
            "Releases management of a service-managed database server. Resource tracking information is discarded, "
            + " but the server remains registered as user-managed.";
        public static final String REGISTER =
            "Registers an existing database server.";
        public static final String DELETE_BY_CRN =
            "Terminates and/or deregisters a database server by its CRN.";
        public static final String DELETE_BY_NAME =
            "Terminates and/or deregisters a database server by its name.";
        public static final String DELETE_MULTIPLE_BY_CRN =
            "Terminates and/or deregisters multiple database servers, each by CRN.";
        public static final String CREATE_DATABASE =
            "Creates a new database on a database server. The database starts out empty. A new user with credentials "
            + "separate from the database server's administrative user is also created, with full rights to the new database.";
        public static final String START =
                "Start a previosly stopped database server.";
        public static final String MIGRATE_DATABASE_TO_SSL =
                "Migrate database to ssl.";
        public static final String ENFORCE_SSL_ON_DATABASE =
                "Enforce ssl on database.";
        public static final String ROTATE_SSL_CERT =
                "Rotate the root cert of a database server.";
        public static final String UPDATE_SSL_CERT =
                "Update the root cert of a database server.";
        public static final String STOP =
                "Stop a running database server.";
        public static final String CERT_SWAP =
                "Changes the certificate on mock provider";
        public static final String LATEST_CERT =
                "Query latest certificate for provider and region";
        public static final String RETRY = "Retries the latest failed operation";
        public static final String LIST_RETRYABLE_FLOWS = "List retryable failed flows";
        public static final String TURN_ON_DB_SSL =
                "Turns on SSL/TLS connection for the database server. The database will refresh with SSL turned on after restart.";
        private DatabaseServerNotes() {
        }
    }
}