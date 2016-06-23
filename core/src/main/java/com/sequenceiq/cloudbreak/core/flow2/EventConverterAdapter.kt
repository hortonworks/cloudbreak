package com.sequenceiq.cloudbreak.core.flow2

class EventConverterAdapter<E : FlowEvent>(private val type: Class<E>) : EventConverter<E> {

    override fun convert(key: String): E? {
        for (event in type.enumConstants) {
            if (key.equals(event.stringRepresentation(), ignoreCase = true)) {
                return event
            }
        }
        return null
    }
}
