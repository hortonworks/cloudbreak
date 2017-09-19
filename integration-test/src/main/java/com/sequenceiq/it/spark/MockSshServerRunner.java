package com.sequenceiq.it.spark;

import java.io.IOException;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.sequenceiq.it.ssh.MockSshServer;

public class MockSshServerRunner {

    private MockSshServerRunner() {

    }

    public static void main(String[] args) throws IOException {
        MockSshServer server = getContext().getBean(MockSshServer.class);
        server.start(2020);
        wait(server);
    }

    private static ApplicationContext getContext() {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.register(MockSshServer.class);
        context.refresh();
        return context;
    }

    private static void wait(MockSshServer s) {
        try {
            synchronized (s) {
                s.wait();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("interrupted");
        }
    }

}
