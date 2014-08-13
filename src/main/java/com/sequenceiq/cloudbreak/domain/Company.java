package com.sequenceiq.cloudbreak.domain;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

@Entity

@NamedQueries({
        @NamedQuery(
                name = "Company.findByName",
                query = "SELECT c FROM Company c WHERE c.name = :name"),
        @NamedQuery(
                name = "Company.companyUsers",
                query = "SELECT cu FROM User cu "
                        + "LEFT JOIN FETCH cu.azureTemplates "
                        + "LEFT JOIN FETCH cu.awsTemplates "
                        + "LEFT JOIN FETCH cu.stacks "
                        + "LEFT JOIN FETCH cu.blueprints "
                        + "LEFT JOIN FETCH cu.awsCredentials "
                        + "LEFT JOIN FETCH cu.azureCredentials "
                        + "LEFT JOIN FETCH cu.clusters "
                        + "WHERE cu.company.id= :companyId"),
        @NamedQuery(
                name = "Company.findCompanyAdmin",
                query = "SELECT u FROM User u "
                        + "WHERE 'COMPANY_ADMIN' in elements(u.userRoles) "
                        + "AND u.company.id= :companyId")

})

@Table(name = "company")
public class Company implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(nullable = false)
    private String name;

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<User> users = new HashSet<>();

    public Company() {

    }

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

    public Set<User> getUsers() {
        return users;
    }

    public void setUsers(Set<User> users) {
        this.users = users;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, "users");
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, "users");
    }
}
