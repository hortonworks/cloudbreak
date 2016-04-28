package com.sequenceiq.cloudbreak.orchestrator.onhost.salt;

import com.suse.salt.netapi.AuthModule;
import com.suse.salt.netapi.client.SaltClient;
import com.suse.salt.netapi.exception.SaltException;

import java.net.URI;

public class SaltConnection {

    private static final String USER = "saltuser";
    private static final String PASSWORD = "saltpass";
    private static final int PORT = 3080;

    public SaltClient get(String address) throws SaltException {
        // Init the client
        SaltClient saltClient = new com.suse.salt.netapi.client.SaltClient(URI.create("http://" + address + ":" + PORT));
        saltClient.login(USER, PASSWORD, AuthModule.PAM);
        return saltClient;
    }
}
