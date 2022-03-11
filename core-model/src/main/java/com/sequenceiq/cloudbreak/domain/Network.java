package com.sequenceiq.cloudbreak.domain;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Where;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.json.JsonToString;
import com.sequenceiq.cloudbreak.converter.OutboundInternetTrafficConverter;
import com.sequenceiq.cloudbreak.domain.converter.ResourceStatusConverter;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.model.WorkspaceAwareResource;
import com.sequenceiq.common.api.type.OutboundInternetTraffic;

@Entity
@Where(clause = "archived = false")
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"workspace_id", "name"}))
public class Network implements ProvisionEntity, WorkspaceAwareResource, ArchivableResource {
    private static final String DELIMITER = ",";

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "network_generator")
    @SequenceGenerator(name = "network_generator", sequenceName = "network_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    /**
     * @deprecated only existing subnets are supported
     */
    @Deprecated
    private String subnetCIDR;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Convert(converter = ResourceStatusConverter.class)
    private ResourceStatus status;

    private String cloudPlatform;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json attributes;

    @Column(nullable = false)
    @Convert(converter = OutboundInternetTrafficConverter.class)
    private OutboundInternetTraffic outboundInternetTraffic = OutboundInternetTraffic.ENABLED;

    private String networkCidrs;

    @ManyToOne
    private Topology topology;

    @ManyToOne
    private Workspace workspace;

    private boolean archived;

    private Long deletionTimestamp = -1L;

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSubnetCIDR() {
        return subnetCIDR;
    }

    public void setSubnetCIDR(String subnetCIDR) {
        this.subnetCIDR = subnetCIDR;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ResourceStatus getStatus() {
        return status;
    }

    public void setStatus(ResourceStatus status) {
        this.status = status;
    }

    public String cloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public Json getAttributes() {
        return attributes;
    }

    public void setAttributes(Json attributes) {
        this.attributes = attributes;
    }

    public OutboundInternetTraffic getOutboundInternetTraffic() {
        return outboundInternetTraffic;
    }

    public void setOutboundInternetTraffic(OutboundInternetTraffic outboundInternetTraffic) {
        this.outboundInternetTraffic = outboundInternetTraffic;
    }

    public List<String> getNetworkCidrs() {
        return StringUtils.isNotEmpty(networkCidrs) ? Stream.of(networkCidrs.split(DELIMITER)).collect(Collectors.toList()) : List.of();
    }

    public void setNetworkCidrs(Collection<String> networkCidrs) {
        if (CollectionUtils.isNotEmpty(networkCidrs)) {
            this.networkCidrs = String.join(DELIMITER, networkCidrs);
        } else {
            this.networkCidrs = null;
        }
    }

    public Topology getTopology() {
        return topology;
    }

    public void setTopology(Topology topology) {
        this.topology = topology;
    }

    public boolean isArchived() {
        return archived;
    }

    public Long getDeletionTimestamp() {
        return deletionTimestamp;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    @Override
    public void unsetRelationsToEntitiesToBeDeleted() {

    }

    public void setDeletionTimestamp(Long deletionTimestamp) {
        this.deletionTimestamp = deletionTimestamp;
    }

    @Override
    public String toString() {
        return "Network{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", subnetCIDR='" + subnetCIDR + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", cloudPlatform='" + cloudPlatform + '\'' +
                ", attributes=" + attributes +
                ", outboundInternetTraffic=" + outboundInternetTraffic +
                ", networkCidrs='" + networkCidrs + '\'' +
                ", topology=" + topology +
                ", workspace=" + workspace +
                ", archived=" + archived +
                ", deletionTimestamp=" + deletionTimestamp +
                '}';
    }
}
