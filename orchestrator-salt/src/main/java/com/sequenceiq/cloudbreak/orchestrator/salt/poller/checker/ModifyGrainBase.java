package com.sequenceiq.cloudbreak.orchestrator.salt.poller.checker;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.SaltConnector;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.HostList;
import com.sequenceiq.cloudbreak.orchestrator.salt.client.target.Target;
import com.sequenceiq.cloudbreak.orchestrator.salt.domain.ApplyResponse;
import com.sequenceiq.cloudbreak.orchestrator.salt.poller.BaseSaltJobRunner;
import com.sequenceiq.cloudbreak.orchestrator.salt.states.SaltStates;

public abstract class ModifyGrainBase extends BaseSaltJobRunner {

    private static final int RETRY_LIMIT = 5;

    private static final int RETRY_BACKOFF_MILLIS = 5_000;

    private static final Logger LOGGER = LoggerFactory.getLogger(ModifyGrainBase.class);

    private final String key;

    private final String value;

    private final boolean addGrain;

    protected ModifyGrainBase(Set<String> target, Set<Node> allNode, String key, String value, boolean addGrain) {
        super(target, allNode);
        this.key = key;
        this.value = value;
        this.addGrain = addGrain;
    }

    @Override
    public String submit(SaltConnector saltConnector) throws SaltJobFailedException {
        Target<String> target = new HostList(getTargetHostnames());
        LOGGER.info("Starting salt modify grain process. {}", this);
        ApplyResponse response = modifyGrain(saltConnector, target);
        Map<String, JsonNode> grains = SaltStates.getGrains(saltConnector, target, key);
        if (isModificationFailed(grains)) {
            LOGGER.info("Modify grain process failed. Starting to retry. {}", this);
            response = retryModification(saltConnector, target, response);
        }
        Set<String> missingHostnames = collectMissingHostnames(collectSucceededNodes(response));
        setTargetHostnames(missingHostnames);
        return missingHostnames.toString();
    }

    private ApplyResponse retryModification(SaltConnector saltConnector, Target<String> target, ApplyResponse response) throws SaltJobFailedException {
        boolean modificationFailed = true;
        Map<String, JsonNode> grains = new HashMap<>();
        int retryCounter;
        for (retryCounter = 0; retryCounter < RETRY_LIMIT && modificationFailed; retryCounter++) {
            backoff();
            LOGGER.info("Retry #{} for salt modify grain process. {}", retryCounter, this);
            SaltStates.syncAll(saltConnector);
            response = modifyGrain(saltConnector, target);
            grains = SaltStates.getGrains(saltConnector, target, key);
            modificationFailed = isModificationFailed(grains);
        }
        if (modificationFailed) {
            LOGGER.info("Salt modify grain process failed after {} retries. {}", RETRY_LIMIT, this);
            checkFinalModification(grains);
        } else {
            LOGGER.info("Salt modify grain process succeeded for retry #{}. {}", retryCounter, this);
        }
        return response;
    }

    private ApplyResponse modifyGrain(SaltConnector saltConnector, Target<String> target) {
        return addGrain ? SaltStates.addGrain(saltConnector, target, key, value)
                : SaltStates.removeGrain(saltConnector, target, key, value);
    }

    private boolean isModificationFailed(Map<String, JsonNode> grains) throws SaltJobFailedException {
        boolean modificationFailed = false;
        for (Node node : getAllNode()) {
            if (getTargetHostnames().contains(node.getHostname())) {
                if (!grains.containsKey(node.getHostname())) {
                    throw new SaltJobFailedException("Can not find node in grains result. target="
                            + node.getHostname() + ", key=" + key + ", value=" + value);
                } else {
                    boolean foundGrain = isGrainFound(grains, node);
                    if (isAddGrainFailed(foundGrain)) {
                        modificationFailed = true;
                    }
                    if (isRemoveGrainFailed(foundGrain)) {
                        modificationFailed = true;
                    }
                }
            }
        }
        return modificationFailed;
    }

    private boolean isGrainFound(Map<String, JsonNode> finalGrains, Node node) {
        Iterable<JsonNode> elements = () -> finalGrains.get(node.getHostname()).elements();
        return StreamSupport.stream(elements.spliterator(), false)
                .anyMatch(element -> value.equals(element.asText()));
    }

    private void backoff() {
        try {
            LOGGER.info("Backing off in the modify grain process for {}ms. {}", RETRY_BACKOFF_MILLIS, this);
            Thread.sleep(RETRY_BACKOFF_MILLIS);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Sleeping was interrupted.", e);
        }
    }

    private void checkFinalModification(Map<String, JsonNode> grains) throws SaltJobFailedException {
        final Map<String, JsonNode> finalGrains = new HashMap<>(grains);
        for (Node node : getAllNode()) {
            if (getTargetHostnames().contains(node.getHostname())) {
                if (!finalGrains.containsKey(node.getHostname())) {
                    throw new SaltJobFailedException("Can not find node in grains result. target="
                            + node.getHostname() + ", key=" + key + ", value=" + value);
                } else {
                    boolean foundGrain = isGrainFound(finalGrains, node);
                    if (isAddGrainFailed(foundGrain)) {
                        throw new SaltJobFailedException("Grain append was unsuccessful. key=" + key + ", value=" + value);
                    }
                    if (isRemoveGrainFailed(foundGrain)) {
                        throw new SaltJobFailedException("Grain value removal was unsuccessful. key=" + key + ", value=" + value);
                    }
                }
            }
        }
    }

    private boolean isAddGrainFailed(boolean foundGrain) {
        return addGrain && !foundGrain;
    }

    private boolean isRemoveGrainFailed(boolean foundGrain) {
        return !addGrain && foundGrain;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ModifyGrainBase{");
        sb.append(super.toString());
        sb.append("key='").append(key).append('\'');
        sb.append(", value='").append(value).append('\'');
        sb.append(", addGrain=").append(addGrain);
        sb.append('}');
        return sb.toString();
    }
}
