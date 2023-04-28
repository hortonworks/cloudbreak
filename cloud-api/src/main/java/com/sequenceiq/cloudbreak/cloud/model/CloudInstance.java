package com.sequenceiq.cloudbreak.cloud.model;

import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.cloudbreak.cloud.model.generic.DynamicModel;

/**
 * A common, abstract representation of a virtual machine in a cloud provider.
 * <p>
 * For example, this class can be used to represent an AWS EC2 instance, Azure virtual machine, or GCP virtual machine instance.
 * <p>
 * This is a <em>minimal</em> representation, containing the unique (cloud vendor specific) instance ID, template used to create the
 * instance, and necessary information to authenticate and connect to the instance.
 *
 * @see InstanceTemplate
 * @see InstanceAuthentication
 */
public class CloudInstance extends DynamicModel {

    public static final String INSTANCE_NAME = "InstanceName";

    public static final String DISCOVERY_NAME = "DiscoveryName";

    public static final String FQDN = "FQDN";

    public static final String ID = "ID";

    private static final Logger LOGGER = LoggerFactory.getLogger(CloudInstance.class);

    private final String instanceId;

    private String subnetId;

    private String availabilityZone;

    private final InstanceTemplate template;

    private final InstanceAuthentication authentication;

    public CloudInstance(String instanceId,
            InstanceTemplate template,
            InstanceAuthentication authentication,
            String subnetId,
            String availabilityZone) {
        this.instanceId = instanceId;
        this.template = template;
        this.authentication = authentication;
        this.availabilityZone = availabilityZone;
        this.subnetId = subnetId;
    }

    @JsonCreator
    public CloudInstance(@JsonProperty("instanceId") String instanceId,
            @JsonProperty("template") InstanceTemplate template,
            @JsonProperty("authentication") InstanceAuthentication authentication,
            @JsonProperty("subnetId") String subnetId,
            @JsonProperty("availabilityZone") String availabilityZone,
            @JsonProperty("parameters") Map<String, Object> params) {
        super(params);
        this.instanceId = instanceId;
        this.template = template;
        this.authentication = authentication;
        this.subnetId = subnetId;
        this.availabilityZone = availabilityZone;
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

    public String getSubnetId() {
        return subnetId;
    }

    public String getAvailabilityZone() {
        return availabilityZone;
    }

    public void setSubnetId(String subnetId) {
        this.subnetId = subnetId;
    }

    public void setAvailabilityZone(String availabilityZone) {
        this.availabilityZone = availabilityZone;
    }

    public String getDbIdOrDefaultIfNotExists() {
        Long dbId = getParameter(CloudInstance.ID, Long.class);
        if (dbId != null) {
            return Long.toString(dbId);
        } else {
            LOGGER.info("DB id not found, return with 'not_found' instead");
            return "not_found";
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CloudInstance{");
        sb.append("instanceId='").append(instanceId).append('\'');
        sb.append(super.toString());
        sb.append(", template=").append(template);
        sb.append(", authentication=").append(authentication);
        sb.append(", subnetId=").append(subnetId);
        sb.append(", availabilityZone=").append(availabilityZone);
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
                && Objects.equals(subnetId, other.subnetId)
                && Objects.equals(availabilityZone, other.availabilityZone)
                && Objects.equals(authentication, other.authentication)
                && Objects.equals(getParameters(), other.getParameters());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getParameters(), instanceId, template, authentication, subnetId, availabilityZone);
    }
}
