package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "Blueprint", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account", "name" })
})
@NamedQueries({
        @NamedQuery(
                name = "Blueprint.findForUser",
                query = "SELECT b FROM Blueprint b "
                        + "WHERE b.owner= :user"),
        @NamedQuery(
                name = "Blueprint.findPublicsInAccount",
                query = "SELECT b FROM Blueprint b "
                        + "WHERE b.account= :account "
                        + "AND b.publicInAccount= true"),
        @NamedQuery(
                name = "Blueprint.findAllInAccount",
                query = "SELECT b FROM Blueprint b "
                        + "WHERE b.account= :account ")
})
public class Blueprint implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "blueprint_generator")
    @SequenceGenerator(name = "blueprint_generator", sequenceName = "blueprint_table")
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000000, columnDefinition = "TEXT")
    private String blueprintText;

    private String blueprintName;

    private String description;

    private int hostGroupCount;

    private String owner;
    private String account;

    @Enumerated(EnumType.STRING)
    private BlueprintVersion blueprintVersion;

    private boolean publicInAccount;

    public Blueprint() {

    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBlueprintText() {
        return blueprintText;
    }

    public void setBlueprintText(String blueprintText) {
        this.blueprintText = blueprintText;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public void setBlueprintName(String blueprintName) {
        this.blueprintName = blueprintName;
    }

    public int getHostGroupCount() {
        return hostGroupCount;
    }

    public void setHostGroupCount(int hostGroupCount) {
        this.hostGroupCount = hostGroupCount;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public BlueprintVersion getBlueprintVersion() {
        return blueprintVersion;
    }

    public void setBlueprintVersion(BlueprintVersion blueprintVersion) {
        this.blueprintVersion = blueprintVersion;
    }

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
    }
}
