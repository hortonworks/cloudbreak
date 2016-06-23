package com.sequenceiq.cloudbreak.shell.commands

import org.springframework.shell.core.CommandMarker

import com.sequenceiq.cloudbreak.shell.model.ShellContext

interface BaseCommands : CommandMarker {

    fun selectAvailable(): Boolean
    @Throws(Exception::class)
    fun select(id: Long?, name: String): String

    @Throws(Exception::class)
    fun selectById(id: Long?): String

    @Throws(Exception::class)
    fun selectByName(name: String): String

    fun showAvailable(): Boolean
    @Throws(Exception::class)
    fun show(id: Long?, name: String): String

    @Throws(Exception::class)
    fun showById(id: Long?): String

    @Throws(Exception::class)
    fun showByName(name: String): String

    fun deleteAvailable(): Boolean
    @Throws(Exception::class)
    fun delete(id: Long?, name: String): String

    @Throws(Exception::class)
    fun deleteById(id: Long?): String

    @Throws(Exception::class)
    fun deleteByName(name: String): String

    fun listAvailable(): Boolean
    @Throws(Exception::class)
    fun list(): String

    fun shellContext(): ShellContext
}
