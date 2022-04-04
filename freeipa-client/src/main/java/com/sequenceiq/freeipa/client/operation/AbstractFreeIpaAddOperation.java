package com.sequenceiq.freeipa.client.operation;

import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;

import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionUtil;

public abstract class AbstractFreeIpaAddOperation<R> extends AbstractFreeipaOperation<R> {

    private final String name;

    private Optional<AbstractFreeipaOperation<R>> getOperation = Optional.empty();

    protected AbstractFreeIpaAddOperation(String name) {
        this.name = name;
    }

    protected AbstractFreeIpaAddOperation(String name, AbstractFreeipaOperation<R> getOperation) {
        this.name = name;
        this.getOperation = Optional.ofNullable(getOperation);
    }

    protected abstract Logger getLogger();

    @Override
    protected List<Object> getFlags() {
        return List.of(name);
    }

    protected Optional<R> invokeAdd(FreeIpaClient freeIpaClient, Class<R> clazz) throws FreeIpaClientException {
        try {
            getLogger().debug("Adding '{}'", name);
            R added = invoke(freeIpaClient, clazz);
            getLogger().debug("Added '{}'", added);
            return Optional.of(added);
        } catch (FreeIpaClientException e) {
            if (FreeIpaClientExceptionUtil.isDuplicateEntryException(e)) {
                getLogger().debug("'{}' already exists", name);
                if (getOperation.isPresent()) {
                    return getOperation.get().invoke(freeIpaClient);
                } else {
                    return Optional.empty();
                }
            } else {
                getLogger().error("Failed to add '{}'", name, e);
                throw e;
            }
        }
    }
}
