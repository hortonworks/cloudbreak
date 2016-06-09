package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public class SimpleAddGrainRunner extends BaseSaltJobRunner {

    private String key;
    private String value;
    private Compound.CompoundType compoundType;

    public SimpleAddGrainRunner(Set<String> target, Set<Node> allNode, String role) {
        this(target, allNode, "roles", role, Compound.CompoundType.IP);
    }

    public SimpleAddGrainRunner(Set<String> target, Set<Node> allNode, String key, String value, Compound.CompoundType type) {
        super(target, allNode);
        this.key = key;
        this.value = value;
        this.compoundType = type;
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        ApplyResponse response = SaltStates.addGrain(saltConnector, new Compound(getTarget(), compoundType), key, value);
        Set<String> missingIps = collectMissingNodes(collectNodes(response));
        setTarget(missingIps);
        return missingIps.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SimpleAddGrainChecker{");
        sb.append("key='").append(key).append('\'');
        sb.append(", value='").append(value).append('\'');
        sb.append(", compoundType=").append(compoundType);
        sb.append('}');
        return sb.toString();
    }
}
