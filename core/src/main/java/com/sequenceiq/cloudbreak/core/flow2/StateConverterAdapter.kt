package com.sequenceiq.cloudbreak.core.flow2

class StateConverterAdapter<S : FlowState>(private val type: Class<S>) : StateConverter<S> {

    override fun convert(stateRepresentation: String): S? {
        for (state in type.enumConstants) {
            if (stateRepresentation.equals(state.toString(), ignoreCase = true)) {
                return state
            }
        }
        return null
    }
}
