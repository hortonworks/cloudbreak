package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Convert;
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

import com.sequenceiq.cloudbreak.api.model.ClusterTemplateType;
import com.sequenceiq.cloudbreak.domain.json.Json;
import com.sequenceiq.cloudbreak.domain.json.JsonToString;

@Entity
@Table(name = "ClusterTemplate", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "account", "name" })
})
@NamedQueries({
        @NamedQuery(
                name = "ClusterTemplate.findForUser",
                query = "SELECT b FROM ClusterTemplate b "
                        + "WHERE b.owner= :user "),
        @NamedQuery(
                name = "ClusterTemplate.findPublicInAccountForUser",
                query = "SELECT b FROM ClusterTemplate b "
                        + "WHERE ((b.account= :account AND b.publicInAccount= true) "
                        + "OR b.owner= :user) "),
        @NamedQuery(
                name = "ClusterTemplate.findAllInAccount",
                query = "SELECT b FROM ClusterTemplate b "
                        + "WHERE b.account= :account "),
        @NamedQuery(
                name = "ClusterTemplate.findOneByName",
                query = "SELECT b FROM ClusterTemplate b "
                        + "WHERE b.name= :name and b.account= :account "),
        @NamedQuery(
                name = "ClusterTemplate.findByIdInAccount",
                query = "SELECT b FROM ClusterTemplate b "
                        + "WHERE  b.id= :id and b.account= :account "),
        @NamedQuery(
                name = "ClusterTemplate.findByNameInAccount",
                query = "SELECT b FROM ClusterTemplate b "
                        + "WHERE  b.name= :name and ((b.publicInAccount=true and b.account= :account) or b.owner= :owner) "),
        @NamedQuery(
                name = "ClusterTemplate.findByNameInUser",
                query = "SELECT b FROM ClusterTemplate b "
                        + "WHERE b.owner= :owner and b.name= :name "),
        @NamedQuery(
                name = "ClusterTemplate.findAllDefaultInAccount",
                query = "SELECT b FROM ClusterTemplate b "
                        + "WHERE b.account= :account ")
})
public class ClusterTemplate implements ProvisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "clustertemplate_generator")
    @SequenceGenerator(name = "clustertemplate_generator", sequenceName = "clustertemplate_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Convert(converter = JsonToString.class)
    @Column(columnDefinition = "TEXT")
    private Json template;

    @Enumerated(EnumType.STRING)
    private ClusterTemplateType type;

    private boolean publicInAccount;

    private String owner;
    private String account;

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

    public Json getTemplate() {
        return template;
    }

    public void setTemplate(Json template) {
        this.template = template;
    }

    public ClusterTemplateType getType() {
        return type;
    }

    public void setType(ClusterTemplateType type) {
        this.type = type;
    }

    public boolean isPublicInAccount() {
        return publicInAccount;
    }

    public void setPublicInAccount(boolean publicInAccount) {
        this.publicInAccount = publicInAccount;
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
}
