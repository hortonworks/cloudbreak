package com.sequenceiq.cloudbreak.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import com.sequenceiq.cloudbreak.authorization.WorkspaceResource;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.workspace.WorkspaceAwareResource;

@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"subscriptionid", "workspace_id"}),
        @UniqueConstraint(columnNames = {"name", "workspace_id"}),
})
public class FlexSubscription implements ProvisionEntity, WorkspaceAwareResource {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "flexsubscription_generator")
    @SequenceGenerator(name = "flexsubscription_generator", sequenceName = "flexsubscription_id_seq", allocationSize = 1)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String subscriptionId;

    @ManyToOne
    private SmartSenseSubscription smartSenseSubscription;

    private boolean isDefault;

    private boolean usedForController;

    @ManyToOne
    private Workspace workspace;

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    @Override
    public WorkspaceResource getResource() {
        return WorkspaceResource.FLEXSUBSCRIPTION;
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

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public SmartSenseSubscription getSmartSenseSubscription() {
        return smartSenseSubscription;
    }

    public void setSmartSenseSubscription(SmartSenseSubscription smartSenseSubscription) {
        this.smartSenseSubscription = smartSenseSubscription;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public boolean isUsedForController() {
        return usedForController;
    }

    public void setUsedForController(boolean usedForController) {
        this.usedForController = usedForController;
    }

    @Override
    public String toString() {
        return "FlexSubscription{" + "id=" + id
                + ", name='" + name + '\''
                + ", subscriptionId='" + subscriptionId + '\''
                + ", smartSenseSubscription='" + smartSenseSubscription + '}';
    }
}
