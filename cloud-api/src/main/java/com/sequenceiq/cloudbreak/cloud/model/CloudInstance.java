package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

/**
 * A common, abstract representation of a virtual machine in a cloud provider.
 *
 * For example, this class can be used to represent an AWS EC2 instance, Azure virtual machine, or GCP virtual machine instance.
 *
 * This is a <em>minimal</em> representation, containing the unique (cloud vendor specific) instance ID, template used to create the
 * instance, and necessary information to authenticate and connect to the instance.
 *
 * @see InstanceTemplate
 * @see InstanceAuthentication
 */
public class CloudInstance extends DynamicModel {

    public static final String INSTANCE_NAME = "InstanceName";

    public static final String DISCOVERY_NAME = "DiscoveryName";

    /**
     * Key of the optional dynamic parameter denoting the ID of the subnet (in a cloud platform specific format) the cloud instance is deployed in.
     * May be absent if the subnet assignment is not (yet) known.
     */
    public static final String SUBNET_ID = "subnetId";

    /**
     * Key of the optional dynamic parameter denoting the name of the availability zone (in a cloud platform specific format) the cloud instance is deployed in.
     * Absent if the cloud platform does not support this construct, or if the availability zone assignment is not (yet) known.
     */
    public static final String AVAILABILITY_ZONE = "availabilityZone";

    private final String instanceId;

    private final InstanceTemplate template;

    private final InstanceAuthentication authentication;

    public CloudInstance(String instanceId, InstanceTemplate template, InstanceAuthentication authentication) {
        this.instanceId = instanceId;
        this.template = template;
        this.authentication = authentication;
    }

    @JsonCreator
    public CloudInstance(@JsonProperty("instanceId") String instanceId,
            @JsonProperty("template") InstanceTemplate template,
            @JsonProperty("authentication") InstanceAuthentication authentication,
            @JsonProperty("params") Map<String, Object> params) {
        super(params);
        this.instanceId = instanceId;
        this.template = template;
        this.authentication = authentication;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public InstanceTemplate getTemplate() {
        return template;
    }

    public InstanceAuthentication getAuthentication() {
        return authentication;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CloudInstance{");
        sb.append("instanceId='").append(instanceId).append('\'');
        sb.append(super.toString());
        sb.append(", template=").append(template);
        sb.append(", authentication=").append(authentication);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        CloudInstance other = (CloudInstance) obj;
        return Objects.equals(instanceId, other.instanceId)
                && Objects.equals(template, other.template)
                && Objects.equals(authentication, other.authentication)
                && Objects.equals(getParameters(), other.getParameters());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getParameters(), instanceId, template, authentication);
    }
}
