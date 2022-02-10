package com.sequenceiq.cloudbreak.doc;

public class ApiDescription {

    public static final String CUSTOM_CONFIGURATIONS_NOTES = "Custom Configurations are sets of properties or name value pairs that "
            + "belong to any of the services present in Cluster Definitions (Blueprints). These can be used to override "
            + "and/or append properties to the corresponding Cluster Definition while launching Data Hub Clusters.";

    private ApiDescription() {
    }

    public static class CustomConfigurationsOpDescription {
        public static final String GET_ALL = "retrieve all custom configs";
        public static final String GET_BY_NAME = "retrieve custom configs by name";
        public static final String GET_BY_CRN = "get custom configs by crn";
        public static final String CREATE = "create new custom configs";
        public static final String CLONE_BY_NAME = "clone new custom configs from existing custom configs by name";
        public static final String CLONE_BY_CRN = "clone new custom configs from existing custom configs by crn";
        public static final String DELETE_BY_CRN = "delete custom configs by crn";
        public static final String DELETE_BY_NAME = "delete custom configs by name";
        public static final String GET_SERVICE_TYPES = "retrieves a list of serviceTypes";
        public static final String GET_ROLE_TYPES = "retrieves a list of all roleTypes";
    }

    public static class CustomConfigurationsJsonProperties {
        public static final String CUSTOM_CONFIGURATIONS_NAME = "unique name of the custom configs";
        public static final String CRN = "unique crn of the custom configs";
        public static final String CONFIGURATION_PROPERTIES = "list of properties";
        public static final String NAME = "name of the property";
        public static final String VALUE = "value of the property";
        public static final String RUNTIME_VERSION = "Runtime version that custom configs point to";
    }
}
