package com.sequenceiq.cloudbreak.auth.crn;

import java.util.Arrays;

import org.apache.commons.lang3.tuple.Pair;

public enum CrnResourceDescriptor {
    // CDL
    CDL(Crn.ResourceType.INSTANCE, Crn.Service.SDXSVC),
    // ums (iam) service
    GROUP(Crn.ResourceType.GROUP, Crn.Service.IAM),
    MACHINE_USER(Crn.ResourceType.MACHINE_USER, Crn.Service.IAM),
    POLICY(Crn.ResourceType.POLICY, Crn.Service.IAM),
    PUBLIC_KEY(Crn.ResourceType.PUBLIC_KEY, Crn.Service.IAM),
    USER(Crn.ResourceType.USER, Crn.Service.IAM),
    // Environment service
    ACCOUNT_TAG(Crn.ResourceType.ACCOUNT_TAG, Crn.Service.ACCOUNTTAG),
    ACCOUNT_TELEMETRY(Crn.ResourceType.ACCOUNT_TELEMETRY, Crn.Service.ACCOUNTTELEMETRY),
    CREDENTIAL(Crn.ResourceType.CREDENTIAL, Crn.Service.ENVIRONMENTS),
    ENVIRONMENT(Crn.ResourceType.ENVIRONMENT, Crn.Service.ENVIRONMENTS),
    NETWORK(Crn.ResourceType.NETWORK, Crn.Service.ENVIRONMENTS),
    PROXY(Crn.ResourceType.PROXY_CONFIG, Crn.Service.ENVIRONMENTS),
    // cloudbreak (datahub) service
    CLUSTER_DEF(Crn.ResourceType.CLUSTER_DEFINITION, Crn.Service.DATAHUB),
    REMOTE_CLUSTER(Crn.ResourceType.PVC_CONTROL_PLANE, Crn.Service.REMOTECLUSTER),
    CLUSTER_TEMPLATE(Crn.ResourceType.CLUSTER_TEMPLATE, Crn.Service.DATAHUB),
    CUSTOM_CONFIGURATIONS(Crn.ResourceType.CUSTOM_CONFIGURATIONS, Crn.Service.DATAHUB),
    DATAHUB(Crn.ResourceType.CLUSTER, Crn.Service.DATAHUB),
    IMAGE_CATALOG(Crn.ResourceType.IMAGE_CATALOG, Crn.Service.DATAHUB),
    RECIPE(Crn.ResourceType.RECIPE, Crn.Service.RECIPE),
    // freeipa management service
    FREEIPA(Crn.ResourceType.FREEIPA, Crn.Service.FREEIPA),
    KERBEROS(Crn.ResourceType.KERBEROS, Crn.Service.FREEIPA),
    LDAP(Crn.ResourceType.LDAP, Crn.Service.FREEIPA),
    // redbeams service
    DATABASE(Crn.ResourceType.DATABASE, Crn.Service.REDBEAMS),
    DATABASE_SERVER(Crn.ResourceType.DATABASE_SERVER, Crn.Service.REDBEAMS),
    // datalake service
    VM_DATALAKE(Crn.ResourceType.DATALAKE, Crn.Service.DATALAKE),
    // externalized compute cluster service
    EXTERNALIZED_COMPUTE(Crn.ResourceType.EXTERNALIZED_COMPUTE, Crn.Service.EXTERNALIZED_COMPUTE),
    // periscope (autoscale) service
    ALERT(Crn.ResourceType.DATAHUB_AUTOSCALE_CONFIG, Crn.Service.AUTOSCALE),
    // DFX service
    DFX_INTERIM(Crn.ResourceType.ENVIRONMENT, Crn.Service.DF),
    DFX(Crn.ResourceType.SERVICE, Crn.Service.DF),
    WXM_ENVIRONMENT(Crn.ResourceType.WXM_ENVIRONMENT, Crn.Service.ENVIRONMENTS),
    COMPUTE_DOCKER_CONFIG(Crn.ResourceType.DOCKER_CONFIG, Crn.Service.COMPUTE),
    COMPUTE_CLUSTER(Crn.ResourceType.CLUSTER, Crn.Service.COMPUTE),
    HYBRID(Crn.ResourceType.PVC_CONTROL_PLANE, Crn.Service.HYBRID),
    ENCYRPTION_PROFILE(Crn.ResourceType.ENCRYPTION_PROFILE, Crn.Service.ENVIRONMENTS),
    APP(Crn.ResourceType.APP, Crn.Service.APP_FACTORY);

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

    public static CrnResourceDescriptor getByCrnString(String crn) {
        return Arrays.stream(values()).filter(crnResourceDescriptor -> crnResourceDescriptor.checkIfCrnMatches(Crn.safeFromString(crn)))
                .findFirst().orElseThrow(() -> new IllegalStateException(String.format("There is no matching crn descriptor for crn %s", crn)));
    }
}
