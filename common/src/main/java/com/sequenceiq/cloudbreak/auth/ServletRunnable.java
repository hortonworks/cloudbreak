package com.sequenceiq.cloudbreak.auth;

import java.io.IOException;

import jakarta.servlet.ServletException;

@FunctionalInterface
public interface ServletRunnable {
    void run() throws ServletException, IOException;
}
