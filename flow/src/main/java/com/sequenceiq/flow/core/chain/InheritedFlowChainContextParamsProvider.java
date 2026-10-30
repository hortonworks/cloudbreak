package com.sequenceiq.flow.core.chain;

import java.util.Set;

/**
 * Defines the context variables that nested flow chains may inherit from their parent flow.
 */
public interface InheritedFlowChainContextParamsProvider {

    Set<String> inheritedContextParamKeys();
}
