package com.sequenceiq.periscope.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ClusterManagerVariant;

@Entity(name = "cluster_manager")
public class ClusterManager {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "cluster_manager_generator")
    @SequenceGenerator(name = "cluster_manager_generator", sequenceName = "cluster_manager_id_seq", allocationSize = 1)
    private long id;

    @Column(name = "host")
    private String host;

    @Column(name = "port")
    private String port;

    @Column(name = "lgn_user")
    private String user;

    @Column(name = "lgn_pass")
    private String pass;

    @Column(name = "variant")
    @Enumerated(EnumType.STRING)
    private ClusterManagerVariant variant;

    public ClusterManager() {
    }

    public ClusterManager(String host, String port, String user, String pass, ClusterManagerVariant variant) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.pass = pass;
        this.variant = variant;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String pass) {
        this.pass = pass;
    }

    public ClusterManagerVariant getVariant() {
        return variant;
    }

    public void setVariant(ClusterManagerVariant variant) {
        this.variant = variant;
    }
}
