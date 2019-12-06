package com.sequenceiq.cloudbreak.auth;

import java.io.IOException;

import javax.servlet.ServletException;

@FunctionalInterface
public interface ServletRunnable {
    void run() throws ServletException, IOException;
}
