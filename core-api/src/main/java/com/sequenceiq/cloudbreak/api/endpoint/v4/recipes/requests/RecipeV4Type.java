package com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.requests;

public enum RecipeV4Type {
    PRE_SERVICE_DEPLOYMENT,
    @Deprecated PRE_CLOUDERA_MANAGER_START,
    @Deprecated PRE_AMBARI_START,
    PRE_TERMINATION,
    POST_SERVICE_DEPLOYMENT,
    POST_CLOUDERA_MANAGER_START,
    @Deprecated POST_AMBARI_START,
    @Deprecated POST_CLUSTER_INSTALL
}
