package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "User.findOneWithLists",
                query = "SELECT u FROM User u "
                        + "LEFT JOIN FETCH u.azureTemplates "
                        + "LEFT JOIN FETCH u.awsTemplates "
                        + "LEFT JOIN FETCH u.stacks "
                        + "LEFT JOIN FETCH u.blueprints "
                        + "LEFT JOIN FETCH u.awsCredentials "
                        + "LEFT JOIN FETCH u.azureCredentials "
                        + "LEFT JOIN FETCH u.clusters "
                        + "WHERE u.id= :id"),
        @NamedQuery(
                name = "User.findByEmailAndPassword",
                query = "SELECT u FROM User u "
                        + "LEFT JOIN FETCH u.azureTemplates "
                        + "LEFT JOIN FETCH u.awsTemplates "
                        + "LEFT JOIN FETCH u.stacks "
                        + "LEFT JOIN FETCH u.blueprints "
                        + "LEFT JOIN FETCH u.awsCredentials "
                        + "LEFT JOIN FETCH u.azureCredentials "
                        + "LEFT JOIN FETCH u.clusters "
                        + "WHERE u.email= :email AND u.password= :password")
})
@Table(name = "cloudbreakuser")
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

    @OneToMany(mappedBy = "awsCredentialOwner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AwsCredential> awsCredentials = new HashSet<>();

    @OneToMany(mappedBy = "azureCredentialOwner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AzureCredential> azureCredentials = new HashSet<>();

    @OneToOne
    private TemporaryAwsCredentials temporaryAwsCredentials;

    @NotEmpty
    private String password;

    @OneToMany(mappedBy = "azureTemplateOwner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AzureTemplate> azureTemplates = new HashSet<>();

    @OneToMany(mappedBy = "awsTemplateOwner", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<AwsTemplate> awsTemplates = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Stack> stacks = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Blueprint> blueprints = new HashSet<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Cluster> clusters = new HashSet<>();

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
        this.awsCredentials = user.awsCredentials;
        this.azureCredentials = user.azureCredentials;
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

    public TemporaryAwsCredentials getTemporaryAwsCredentials() {
        return temporaryAwsCredentials;
    }

    public void setTemporaryAwsCredentials(TemporaryAwsCredentials temporaryAwsCredentials) {
        this.temporaryAwsCredentials = temporaryAwsCredentials;
    }

    public Set<AzureCredential> getAzureCredentials() {
        return azureCredentials;
    }

    public void setAzureCredentials(Set<AzureCredential> azureCredentials) {
        this.azureCredentials = azureCredentials;
    }

    public Set<AwsCredential> getAwsCredentials() {

        return awsCredentials;
    }

    public void setAwsCredentials(Set<AwsCredential> awsCredentials) {
        this.awsCredentials = awsCredentials;
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

    public Set<Cluster> getClusters() {
        return clusters;
    }

    public void setClusters(Set<Cluster> clusters) {
        this.clusters = clusters;
    }

    public String emailAsFolder() {
        return email.replaceAll("@", "_").replace(".", "_");
    }
}