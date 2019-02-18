package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Compound.CompoundType;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public abstract class ModifyGrainBase extends BaseSaltJobRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyGrainBase.class);

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
    public String submit(SaltConnector saltConnector) throws SaltJobFailedException {
        Compound target = new Compound(getTarget(), compoundType);
        ApplyResponse response = addGrain ? SaltStates.addGrain(saltConnector, target, key, value)
                : SaltStates.removeGrain(saltConnector, target, key, value);
        Set<String> missingIps = collectMissingNodes(collectNodes(response));
        Map<String, JsonNode> grains = SaltStates.getGrains(saltConnector, target, key);
        for (Node node : getAllNode()) {
            if (getTarget().contains(node.getPrivateIp())) {
                if (!grains.containsKey(node.getHostname())) {
                    throw new SaltJobFailedException("Can not find node in grains result. target=" + node.getHostname() + ", key=" + key + ", value=" + value);
                } else {
                    Iterable<JsonNode> elements = () -> grains.get(node.getHostname()).elements();
                    boolean foundGrain = StreamSupport.stream(elements.spliterator(), false)
                            .anyMatch(element -> value.equals(element.asText()));
                    if (addGrain && !foundGrain) {
                        throw new SaltJobFailedException("Grain append was unsuccessful. key=" + key + ", value=" + value);
                    }
                    if (!addGrain && foundGrain) {
                        throw new SaltJobFailedException("Grain value removal was unsuccessful. key=" + key + ", value=" + value);
                    }
                }
            }
        }
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
