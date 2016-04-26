package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "HostService.findServicesInCluster",
                query = "SELECT c FROM HostService c "
                        + "WHERE c.cluster.id= :clusterId")
})
public class HostService {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "hostservice_generator")
    @SequenceGenerator(name = "hostservice_generator", sequenceName = "hostservice_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    private String host;

    @ManyToOne
    private Cluster cluster;


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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public Cluster getCluster() {
        return cluster;
    }

    public void setCluster(Cluster cluster) {
        this.cluster = cluster;
    }

}
