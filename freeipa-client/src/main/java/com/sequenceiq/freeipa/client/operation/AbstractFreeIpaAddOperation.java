package com.sequenceiq.freeipa.client.operation;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;

public abstract class AbstractFreeIpaAddOperation<R> extends AbstractFreeipaOperation<R> {

    private final String flag;

    private final Class<R> respType;

    private Optional<AbstractFreeipaOperation<R>> getOperation = Optional.empty();

    protected AbstractFreeIpaAddOperation(String flag, Class<R> respType) {
        this.flag = flag;
        this.respType = respType;
    }

    protected AbstractFreeIpaAddOperation(String flag, AbstractFreeipaOperation<R> getOperation, Class<R> respType) {
        this.flag = flag;
        this.getOperation = Optional.ofNullable(getOperation);
        this.respType = respType;
    }

    protected abstract Logger getLogger();

    @Override
    protected List<Object> getFlags() {
        return List.of(flag);
    }

    public Optional<R> invoke(FreeIpaClient freeIpaClient) throws FreeIpaClientException {
        try {
            getLogger().debug("Adding '{}'", flag);
            R added = invoke(freeIpaClient, respType);
            getLogger().debug("Added '{}'", added);
            return Optional.of(added);
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isDuplicateEntryException(e)) {
                getLogger().debug("'{}' already exists", flag);
                if (getOperation.isPresent()) {
                    return getOperation.get().invoke(freeIpaClient);
                } else {
                    return Optional.empty();
                }
            } else {
                getLogger().error("Failed to add '{}'", flag, e);
                throw e;
            }
        }
    }
}
