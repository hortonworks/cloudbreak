package com.sequenceiq.environment.api.environment.doc;

public class EnvironmentDescription {
    public static final String CREATE = "Create an environment.";
    public static final String GET = "Get an environment.";
    public static final String ATTACH_RESOURCES = "Attach resources to an environment.";
    public static final String DETACH_RESOURCES = "Detach resources from an environment.";
    public static final String LIST = "List all environments in the workspace.";
    public static final String DELETE = "Delete an environment. Only possible if no cluster is running in the environment.";
    public static final String CHANGE_CREDENTIAL = "Changes the credential of the environment and the clusters in the environment.";
    public static final String REGISTER_EXTERNAL_DATALAKE = "Register external datalake";
    public static final String EDIT = "Edit and environment. Location, regions and description can be changed.";
    public static final String DATALAKE_PREREQUISITES = "returns datalake prerequisites";

    public static final String ENVIRONMENT_NOTES = "Environment consists of a credential and various other resources and enables users to quickly "
            + "create clusters in given regions in a given cloud provider.";

    public static final String REGIONS = "regions";
    public static final String REGION_DISPLAYNAMES = "regions with displayNames";
}
