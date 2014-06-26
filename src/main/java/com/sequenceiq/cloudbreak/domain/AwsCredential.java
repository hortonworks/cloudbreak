package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
public class AwsCredential extends Credential implements ProvisionEntity {

    private String roleArn;
    private String instanceProfileRoleArn;

    @ManyToOne
    @JoinColumn(name = "awsCredential_awsCredentialOwner")
    private User awsCredentialOwner;

    @Column(nullable = false)
    private String name;

    @OneToOne
    private TemporaryAwsCredentials temporaryAwsCredentials;

    @OneToMany(mappedBy = "credential", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SnsTopic> snsTopics = new HashSet<>();

    public AwsCredential() {

    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getAwsCredentialOwner() {
        return awsCredentialOwner;
    }

    public void setAwsCredentialOwner(User awsCredentialOwner) {
        this.awsCredentialOwner = awsCredentialOwner;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    public String getInstanceProfileRoleArn() {
        return instanceProfileRoleArn;
    }

    public void setInstanceProfileRoleArn(String instanceProfileRoleArn) {
        this.instanceProfileRoleArn = instanceProfileRoleArn;
    }

    public TemporaryAwsCredentials getTemporaryAwsCredentials() {
        return temporaryAwsCredentials;
    }

    public void setTemporaryAwsCredentials(TemporaryAwsCredentials temporaryAwsCredentials) {
        this.temporaryAwsCredentials = temporaryAwsCredentials;
    }

    public Set<SnsTopic> getSnsTopics() {
        return snsTopics;
    }

    public void setSnsTopics(Set<SnsTopic> snsTopics) {
        this.snsTopics = snsTopics;
    }

    @Override
    public CloudPlatform cloudPlatform() {
        return CloudPlatform.AWS;
    }

    @Override
    public User getOwner() {
        return awsCredentialOwner;
    }

    @Override
    public String getCredentialName() {
        return name;
    }

}
