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
        public static final String GET_BY_NAME =
            "Gets information on a database server by its name.";
        public static final String GET_BY_CRN =
            "Gets information on a database server by its CRN.";
        public static final String GET_BY_CLUSTER_CRN =
                "Gets information on a database server by cluster CRN";
        public static final String CREATE =
            "Creates a new database server. The database server starts out with only default databases.";
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
        public static final String STOP =
                "Stop a running database server.";
        private DatabaseServerNotes() {
        }
    }

}
