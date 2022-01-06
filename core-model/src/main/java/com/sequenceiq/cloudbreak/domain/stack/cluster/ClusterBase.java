package com.sequenceiq.cloudbreak.domain.stack.cluster;

import javax.persistence.Entity;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import com.sequenceiq.cloudbreak.domain.stack.StackBase;

@Entity
@Table(name = "Cluster")
public class ClusterBase extends AbstractCluster<ClusterBase> {

    @OneToOne
    private StackBase stack;
}
