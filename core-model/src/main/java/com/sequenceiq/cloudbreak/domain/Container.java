package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;

@Entity
public class Container implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "container_generator")
    @SequenceGenerator(name = "container_generator", sequenceName = "container_id_seq", allocationSize = 1)
    private Long id;

    private String containerId;

    private String name;

    private String image;

    private String host;

    @ManyToOne
    private Cluster cluster;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getContainerId() {
        return containerId;
    }

    public void setContainerId(String containerId) {
        this.containerId = containerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

    @Override
    public String toString() {
        return "Container{" +
                "id=" + id +
                ", containerId='" + containerId + '\'' +
                ", name='" + name + '\'' +
                ", image='" + image + '\'' +
                ", host='" + host + '\'' +
                '}';
    }
}
