package com.sequenceiq.cloudbreak.domain;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;

@Entity
@NamedQueries({
        @NamedQuery(
                name = "Topology.findAllInAccount",
                query = "SELECT t FROM Topology t "
                        + "WHERE t.account= :account AND deleted IS NOT TRUE "),
        @NamedQuery(
                name = "Topology.findByIdInAccount",
                query = "SELECT t FROM Topology t "
                        + "WHERE t.id= :id and t.account= :account AND deleted IS NOT TRUE "),
})
public class Topology {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "topology_generator")
    @SequenceGenerator(name = "topology_generator", sequenceName = "topology_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String cloudPlatform;

    @Column(nullable = false)
    private String account;

    @Column(nullable = false)
    private String owner;

    @Column(nullable = false)
    private boolean deleted;

    @ElementCollection(fetch = FetchType.EAGER)
    private List<TopologyRecord> records = new ArrayList<>();

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

    public String getCloudPlatform() {
        return cloudPlatform;
    }

    public void setCloudPlatform(String cloudPlatform) {
        this.cloudPlatform = cloudPlatform;
    }

    public List<TopologyRecord> getRecords() {
        return records;
    }

    public void setRecords(List<TopologyRecord> records) {
        this.records = records;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}
