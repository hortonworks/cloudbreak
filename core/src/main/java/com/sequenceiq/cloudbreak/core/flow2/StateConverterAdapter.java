package com.sequenceiq.cloudbreak.core.flow2;

public class StateConverterAdapter<S extends FlowState> implements StateConverter<S> {

    private final Class<S> type;

    public StateConverterAdapter(Class<S> clazz) {
        type = clazz;
    }

    @Override
    public S convert(String stateRepresentation) {
        for (S state : type.getEnumConstants()) {
            if (stateRepresentation.equalsIgnoreCase(state.toString())) {
                return state;
            }
        }
        return null;
    }
}
