package com.sequenceiq.cloudbreak.domain.view;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@Entity
@SuppressFBWarnings("UWF_UNWRITTEN_FIELD")
@Table(name = "Cluster")
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

    @Override
    public String toString() {
        return "ClusterTemplateClusterApiView{" +
                ", blueprint=" + blueprint +
                ", environmentCrn='" + environmentCrn + '\'' +
                '}';
    }
}
