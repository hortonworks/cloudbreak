package com.sequenceiq.cloudbreak.domain.stack.cluster;

import com.sequenceiq.cloudbreak.domain.converter.ClusterCommandTypeConverter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class ClusterCommand {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cluster_command_generator")
    @SequenceGenerator(name = "cluster_command_generator", sequenceName = "clustercommand_id_seq", allocationSize = 1)
    private long id;

    @Column(name = "cluster_id", nullable = false)
    private long clusterId;

    @Column(name = "command_id", nullable = false)
    private BigDecimal commandId;

    @Convert(converter = ClusterCommandTypeConverter.class)
    @Column(name = "cluster_command_type", nullable = false)
    private ClusterCommandType clusterCommandType;

    @Column(nullable = false)
    private long created = new Date().getTime();

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getClusterId() {
        return clusterId;
    }

    public void setClusterId(long clusterId) {
        this.clusterId = clusterId;
    }

    public BigDecimal getCommandId() {
        return commandId;
    }

    public void setCommandId(BigDecimal commandId) {
        this.commandId = commandId;
    }

    public ClusterCommandType getClusterCommandType() {
        return clusterCommandType;
    }

    public void setClusterCommandType(ClusterCommandType clusterCommandType) {
        this.clusterCommandType = clusterCommandType;
    }

    public long getCreated() {
        return created;
    }

    public void setCreated(long created) {
        this.created = created;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClusterCommand that = (ClusterCommand) o;
        return clusterId == that.clusterId &&
                Objects.equals(commandId, that.commandId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clusterId, commandId);
    }

    @Override
    public String toString() {
        return "ClusterCommand{" +
                "id=" + id +
                ", clusterId=" + clusterId +
                ", commandId=" + commandId +
                ", clusterCommandType=" + clusterCommandType +
                ", created=" + created +
                '}';
    }
}
