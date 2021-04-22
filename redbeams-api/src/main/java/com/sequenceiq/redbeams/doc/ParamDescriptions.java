package com.sequenceiq.redbeams.doc;

public final class ParamDescriptions {

    private ParamDescriptions() {
    }

    public static final class DatabaseParamDescriptions {

        public static final String CRN = "CRN of the database";
        public static final String CRNS = "CRNs of the databases";
        public static final String NAME = "Name of the database";
        public static final String ENVIRONMENT_CRN = "CRN of the environment of the database(s)";

        public static final String DATABASE_REQUEST = ModelDescriptions.DATABASE_REQUEST;
        public static final String DATABASE_TEST_REQUEST = ModelDescriptions.DATABASE_TEST_REQUEST;

        private DatabaseParamDescriptions() {
        }
    }

    public static final class DatabaseServerParamDescriptions {

        public static final String CRN = "CRN of the database server";
        public static final String CRNS = "CRNs of the database servers";
        public static final String NAME = "Name of the database server";
        public static final String ENVIRONMENT_CRN = "CRN of the environment of the database server(s)";
        public static final String CLUSTER_CRN = "CRN of cluster of the database server";

        public static final String ALLOCATE_DATABASE_SERVER_REQUEST = ModelDescriptions.ALLOCATE_DATABASE_SERVER_REQUEST;
        public static final String CREATE_DATABASE_REQUEST = ModelDescriptions.CREATE_DATABASE_REQUEST;
        public static final String DATABASE_SERVER_REQUEST = ModelDescriptions.DATABASE_SERVER_REQUEST;
        public static final String DATABASE_SERVER_TEST_REQUEST = ModelDescriptions.DATABASE_SERVER_TEST_REQUEST;

        private DatabaseServerParamDescriptions() {
        }
    }

}
