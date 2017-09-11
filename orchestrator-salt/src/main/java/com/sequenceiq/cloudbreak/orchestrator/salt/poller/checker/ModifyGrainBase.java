package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Set;

import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound.CompoundType;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public abstract class ModifyGrainBase extends BaseSaltJobRunner {

    private final String key;

    private final String value;

    private final CompoundType compoundType;

    private final boolean addGrain;

    protected ModifyGrainBase(Set<String> target, Set<Node> allNode, String key, String value, CompoundType type, boolean addGrain) {
        super(target, allNode);
        this.key = key;
        this.value = value;
        compoundType = type;
        this.addGrain = addGrain;
    }

    @Override
    public String submit(SaltConnector saltConnector) {
        ApplyResponse response;
        if (addGrain) {
            response = SaltStates.addGrain(saltConnector, new Compound(getTarget(), compoundType), key, value);
        } else {
            response = SaltStates.removeGrain(saltConnector, new Compound(getTarget(), compoundType), key, value);
        }
        Set<String> missingIps = collectMissingNodes(collectNodes(response));
        setTarget(missingIps);
        return missingIps.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ModifyGrainBase{");
        sb.append("key='").append(key).append('\'');
        sb.append(", value='").append(value).append('\'');
        sb.append(", compoundType=").append(compoundType);
        sb.append(", addGrain=").append(addGrain);
        sb.append('}');
        return sb.toString();
    }
}
