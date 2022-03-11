package com.sequenceiq.redbeams.domain.stack;

import java.util.HashSet;
import java.util.Set;

// import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
// import javax.persistence.EnumType;
// import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table
public class SecurityGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "securitygroup_generator")
    @SequenceGenerator(name = "securitygroup_generator", sequenceName = "securitygroup_id_seq", allocationSize = 1)
    private Long id;

    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @Column(name = "securitygroupid_value")
    private Set<String> securityGroupIds = new HashSet<>();

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<String> getSecurityGroupIds() {
        return securityGroupIds;
    }

    public void setSecurityGroupIds(Set<String> securityGroupIds) {
        this.securityGroupIds = securityGroupIds;
    }

    @Override
    public String toString() {
        return "SecurityGroup{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", securityGroupIds=" + securityGroupIds +
                '}';
    }
}
