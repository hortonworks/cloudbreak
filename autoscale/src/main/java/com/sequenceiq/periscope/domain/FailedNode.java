package com.sequenceiq.periscope.domain;

import java.util.Date;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity(name = "failed_node")
public class FailedNode {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "failed_node_generator")
    @SequenceGenerator(name = "failed_node_generator", sequenceName = "failed_node_id_seq", allocationSize = 1)
    private long id;

    private Long created = new Date().getTime();

    @Column(name = "cluster_id")
    private long clusterId;

    private String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    public long getClusterId() {
        return clusterId;
    }

    public void setClusterId(long clusterId) {
        this.clusterId = clusterId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !getClass().equals(o.getClass())) {
            return false;
        }
        FailedNode that = (FailedNode) o;
        return id == that.id
                && clusterId == that.clusterId
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, clusterId, name);
    }
}