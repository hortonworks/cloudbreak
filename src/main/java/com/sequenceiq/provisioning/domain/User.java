package com.sequenceiq.provisioning.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

@Entity
@NamedQuery(
        name = "User.findOneWithLists",
        query = "SELECT u FROM User u "
                + "LEFT JOIN FETCH u.azureTemplates "
                + "LEFT JOIN FETCH u.awsTemplates "
                + "LEFT JOIN FETCH u.stacks "
                + "LEFT JOIN FETCH u.blueprints "
                + "WHERE u.id= :id")
public class User implements ProvisionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotEmpty
    private String firstName;

    @NotEmpty
    private String lastName;

    @Email
    @NotEmpty
    @Column(unique = true, nullable = false)
    private String email;

    private String roleArn;

    @OneToOne
    private TemporaryAwsCredentials temporaryAwsCredentials;

    private String subscriptionId;

    private String jks;

    @NotEmpty
    private String password;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AzureTemplate> azureTemplates = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AwsTemplate> awsTemplates = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Stack> stacks = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Blueprint> blueprints = new HashSet<>();

    public User() {
    }

    public User(User user) {
        this.id = user.id;
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.email = user.email;
        this.password = user.password;
        this.awsTemplates = user.awsTemplates;
        this.azureTemplates = user.azureTemplates;
        this.jks = user.jks;
        this.subscriptionId = user.subscriptionId;
        this.roleArn = user.roleArn;
        this.stacks = user.stacks;
        this.blueprints = user.blueprints;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Set<AwsTemplate> getAwsTemplates() {
        return awsTemplates;
    }

    public void setAwsTemplates(Set<AwsTemplate> awsTemplates) {
        this.awsTemplates = awsTemplates;
    }

    public Set<AzureTemplate> getAzureTemplates() {
        return azureTemplates;
    }

    public void setAzureTemplates(Set<AzureTemplate> azureTemplates) {
        this.azureTemplates = azureTemplates;
    }

    public String getRoleArn() {
        return roleArn;
    }

    public void setRoleArn(String roleArn) {
        this.roleArn = roleArn;
    }

    public TemporaryAwsCredentials getTemporaryAwsCredentials() {
        return temporaryAwsCredentials;
    }

    public void setTemporaryAwsCredentials(TemporaryAwsCredentials temporaryAwsCredentials) {
        this.temporaryAwsCredentials = temporaryAwsCredentials;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getJks() {
        return jks;
    }

    public void setJks(String jks) {
        this.jks = jks;
    }

    public Set<Stack> getStacks() {
        return stacks;
    }

    public void setStacks(Set<Stack> stacks) {
        this.stacks = stacks;
    }

    public Set<Blueprint> getBlueprints() {
        return blueprints;
    }

    public void setBlueprints(Set<Blueprint> blueprints) {
        this.blueprints = blueprints;
    }

    public String emailAsFolder() {
        return email.replaceAll("@", "_").replace(".", "_");
    }
}