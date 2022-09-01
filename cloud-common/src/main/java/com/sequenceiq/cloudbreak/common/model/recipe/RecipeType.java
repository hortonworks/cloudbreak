package com.sequenceiq.cloudbreak.common.model.recipe;

public enum RecipeType {
    @Deprecated PRE_CLOUDERA_MANAGER_START,
    PRE_SERVICE_DEPLOYMENT,
    PRE_TERMINATION,
    POST_CLOUDERA_MANAGER_START,
    @Deprecated POST_CLUSTER_INSTALL,
    POST_SERVICE_DEPLOYMENT
}
