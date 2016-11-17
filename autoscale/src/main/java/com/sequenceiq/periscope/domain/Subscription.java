package com.sequenceiq.periscope.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;

@Entity
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "subscription_generator")
    @SequenceGenerator(name = "subscription_generator", sequenceName = "subscription_id_seq", allocationSize = 1)
    private Long id;

    private String clientId;

    private String endpoint;

    public Subscription() {
    }

    public Subscription(String clientId, String endpoint) {
        this.clientId = clientId;
        this.endpoint = endpoint;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
}
