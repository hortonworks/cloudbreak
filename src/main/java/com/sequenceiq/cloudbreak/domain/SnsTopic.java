package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;

import com.amazonaws.regions.Regions;

@Entity
@NamedQuery(name = "SnsTopic.findOneForCredentialInRegion",
        query = "SELECT s FROM SnsTopic s "
                + "WHERE s.credential.id= :credentialId "
                + "AND s.region= :region")
public class SnsTopic implements ProvisionEntity {

    @Id
    @GeneratedValue
    private Long id;
    private String name;
    private String topicArn;
    private Regions region;
    private boolean confirmed;

    @ManyToOne
    private AwsCredential credential;

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

    public String getTopicArn() {
        return topicArn;
    }

    public void setTopicArn(String topicArn) {
        this.topicArn = topicArn;
    }

    public Regions getRegion() {
        return region;
    }

    public void setRegion(Regions region) {
        this.region = region;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public AwsCredential getCredential() {
        return credential;
    }

    public void setCredential(AwsCredential credential) {
        this.credential = credential;
    }

}
