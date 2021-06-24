package com.sequenceiq.cloudbreak.auth.crn;

import org.apache.commons.lang3.tuple.Pair;

public enum CrnResourceDescriptor {
    // ums (iam) service
    GROUP(Crn.ResourceType.GROUP, Crn.Service.IAM),
    MACHINE_USER(Crn.ResourceType.MACHINE_USER, Crn.Service.IAM),
    POLICY(Crn.ResourceType.POLICY, Crn.Service.IAM),
    PUBLIC_KEY(Crn.ResourceType.PUBLIC_KEY, Crn.Service.IAM),
    RESOURCE_ROLE(Crn.ResourceType.RESOURCE_ROLE, Crn.Service.IAM),
    ROLE(Crn.ResourceType.ROLE, Crn.Service.IAM),
    USER(Crn.ResourceType.USER, Crn.Service.IAM),
    // Environment service
    ACCOUNT_TAG(Crn.ResourceType.ACCOUNT_TAG, Crn.Service.ACCOUNTTAG),
    ACCOUNT_TELEMETRY(Crn.ResourceType.ACCOUNT_TELEMETRY, Crn.Service.ACCOUNTTELEMETRY),
    CREDENTIAL(Crn.ResourceType.CREDENTIAL, Crn.Service.ENVIRONMENTS),
    ENVIRONMENT(Crn.ResourceType.ENVIRONMENT, Crn.Service.ENVIRONMENTS),
    NETWORK(Crn.ResourceType.NETWORK, Crn.Service.ENVIRONMENTS),
    PROXY(Crn.ResourceType.PROXY_CONIFG, Crn.Service.ENVIRONMENTS),
    // cloudbreak (datahub) service
    CLUSTER_DEF(Crn.ResourceType.CLUSTER_DEFINITION, Crn.Service.DATAHUB),
    CLUSTER_TEMPLATE(Crn.ResourceType.CLUSTER_TEMPLATE, Crn.Service.DATAHUB),
    CUSTOM_CONFIGS(Crn.ResourceType.CUSTOM_CONFIGS, Crn.Service.DATAHUB),
    DATAHUB(Crn.ResourceType.CLUSTER, Crn.Service.DATAHUB),
    IMAGE_CATALOG(Crn.ResourceType.IMAGE_CATALOG, Crn.Service.DATAHUB),
    RECIPE(Crn.ResourceType.RECIPE, Crn.Service.DATAHUB),
    // freeipa management service
    FREEIPA(Crn.ResourceType.FREEIPA, Crn.Service.FREEIPA),
    KERBEROS(Crn.ResourceType.KERBEROS, Crn.Service.FREEIPA),
    LDAP(Crn.ResourceType.LDAP, Crn.Service.FREEIPA),
    // redbeams service
    DATABASE(Crn.ResourceType.DATABASE, Crn.Service.REDBEAMS),
    DATABASE_SERVER(Crn.ResourceType.DATABASE_SERVER, Crn.Service.REDBEAMS),
    // datalake service
    DATALAKE(Crn.ResourceType.DATALAKE, Crn.Service.DATALAKE),
    // periscope (autoscale) service
    ALERT(Crn.ResourceType.DATAHUB_AUTOSCALE_CONFIG, Crn.Service.AUTOSCALE),
    // DFX service
    DFX_INTERIM(Crn.ResourceType.ENVIRONMENT, Crn.Service.DF),
    DFX(Crn.ResourceType.SERVICE, Crn.Service.DF);

    private Crn.ResourceType resourceType;

    private Crn.Service serviceType;

    CrnResourceDescriptor(Crn.ResourceType resourceType, Crn.Service serviceType) {
        this.resourceType = resourceType;
        this.serviceType = serviceType;
    }

    public Crn.ResourceType getResourceType() {
        return resourceType;
    }

    public Crn.Service getServiceType() {
        return serviceType;
    }

    public boolean checkIfCrnMatches(Crn source) {
        return source.getResourceType().equals(getResourceType()) &&
                source.getService().equals(getServiceType());
    }

    public Pair createServiceAndResourceTypePair() {
        return Pair.of(getServiceType().getName(), getResourceType().getName());
    }
}
