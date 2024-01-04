package com.sequenceiq.cloudbreak.domain.view;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
@Table(name = "Cluster")
@Deprecated
public class ClusterTemplateClusterApiView extends CompactView {
    @OneToOne(fetch = FetchType.LAZY)
    private ClusterTemplateStackApiView stack;

    @ManyToOne(fetch = FetchType.EAGER)
    private BlueprintView blueprint;

    private String environmentCrn;

    public String getEnvironmentCrn() {
        return environmentCrn;
    }

    public void setEnvironmentCrn(String environmentCrn) {
        this.environmentCrn = environmentCrn;
    }

    public ClusterTemplateStackApiView getStack() {
        return stack;
    }

    public void setStack(ClusterTemplateStackApiView stack) {
        this.stack = stack;
    }

    public BlueprintView getBlueprint() {
        return blueprint;
    }

    public void setBlueprint(BlueprintView blueprint) {
        this.blueprint = blueprint;
    }
}
