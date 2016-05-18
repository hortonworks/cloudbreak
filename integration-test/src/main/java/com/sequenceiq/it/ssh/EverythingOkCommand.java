package com.sequenceiq.it.ssh;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.sshd.server.Command;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.ExitCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EverythingOkCommand implements Command {

    private static final Logger LOGGER = LoggerFactory.getLogger(EverythingOkCommand.class);

    private ExitCallback callback;
    private InputStream in;
    private OutputStream err;
    private OutputStream out;

    @Override
    public void setInputStream(InputStream in) {
        LOGGER.info("setInputStream");
        this.in = in;
    }

    @Override
    public void setOutputStream(OutputStream out) {
        LOGGER.info("setErrorStream");
        this.out = out;
    }

    @Override
    public void setErrorStream(OutputStream err) {
        LOGGER.info("setErrorStream");
        this.err = err;
    }

    @Override
    public void setExitCallback(ExitCallback callback) {
        this.callback = callback;
        LOGGER.info("setExitCallback");
    }

    @Override
    public void start(Environment env) throws IOException {
        LOGGER.info("start");
        IOUtils.closeQuietly(in);
        IOUtils.closeQuietly(out);
        IOUtils.closeQuietly(err);
        callback.onExit(0);
    }

    @Override
    public void destroy() throws Exception {
        LOGGER.info("destroy");
    }
}
