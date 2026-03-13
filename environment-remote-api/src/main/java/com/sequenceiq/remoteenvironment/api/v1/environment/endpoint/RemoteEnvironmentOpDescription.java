package com.sequenceiq.remoteenvironment.api.v1.environment.endpoint;

public class RemoteEnvironmentOpDescription {

    public static final String LIST = "list remote environments.";

    public static final String ENVIRONMENT_NOTES = "Environment consists of a credential and various other resources and enables users to quickly "
            + "on private cloud.";

    public static final String DATALAKE_SERVICES_NOTES = "Datalake consists of information for all the services.";

    public static final String DESCRIBE_BY_CRN = "Describe an environment by CRN.";

    public static final String VALIDATE_FOR_DATALAKE_BY_CRN = "Validate for datalake by CRN.";

    public static final String GET_RDC_BY_CRN = "Get the remote data context of an environment by CRN.";

    private RemoteEnvironmentOpDescription() {

    }

}
