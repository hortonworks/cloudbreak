package com.sequenceiq.it.ssh

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

import org.apache.commons.io.IOUtils
import org.apache.sshd.server.Command
import org.apache.sshd.server.Environment
import org.apache.sshd.server.ExitCallback
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class EverythingOkCommand : Command {

    private var callback: ExitCallback? = null
    private var `in`: InputStream? = null
    private var err: OutputStream? = null
    private var out: OutputStream? = null

    override fun setInputStream(`in`: InputStream) {
        LOGGER.info("setInputStream")
        this.`in` = `in`
    }

    override fun setOutputStream(out: OutputStream) {
        LOGGER.info("setErrorStream")
        this.out = out
    }

    override fun setErrorStream(err: OutputStream) {
        LOGGER.info("setErrorStream")
        this.err = err
    }

    override fun setExitCallback(callback: ExitCallback) {
        this.callback = callback
        LOGGER.info("setExitCallback")
    }

    @Throws(IOException::class)
    override fun start(env: Environment) {
        LOGGER.info("start")
        IOUtils.closeQuietly(`in`)
        IOUtils.closeQuietly(out)
        IOUtils.closeQuietly(err)
        callback!!.onExit(0)
    }

    @Throws(Exception::class)
    override fun destroy() {
        LOGGER.info("destroy")
    }

    companion object {

        private val LOGGER = LoggerFactory.getLogger(EverythingOkCommand::class.java)
    }
}
