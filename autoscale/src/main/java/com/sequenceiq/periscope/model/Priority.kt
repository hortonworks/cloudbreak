package com.sequenceiq.periscope.model

class Priority private constructor(val value: Int) : Comparable<Priority> {

    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val priority = o as Priority?

        if (value != priority.value) {
            return false
        }

        return true
    }

    override fun hashCode(): Int {
        return value
    }

    override fun compareTo(o: Priority): Int {
        return Integer.compare(value, o.value)
    }

    companion object {

        val HIGH = Priority.of(0)
        val NORMAL = Priority.of(100)
        val LOW = Priority.of(Integer.MAX_VALUE)

        fun of(value: Int): Priority {
            return Priority(if (value < 0) 0 else value)
        }
    }
}
