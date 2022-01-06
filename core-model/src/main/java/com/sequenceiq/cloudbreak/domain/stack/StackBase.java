package com.sequenceiq.cloudbreak.domain.stack;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.stack.cluster.ClusterBase;

@Entity
@Table(name = "Stack")
public class StackBase extends AbstractStack<StackBase> {

    @OneToOne(mappedBy = "stack", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private ClusterBase cluster;

    public ClusterBase getCluster() {
        return cluster;
    }

    public void setCluster(ClusterBase cluster) {
        this.cluster = cluster;
    }
}
