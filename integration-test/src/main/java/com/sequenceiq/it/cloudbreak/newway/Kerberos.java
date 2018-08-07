package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.KerberosRequest;
import com.sequenceiq.it.IntegrationTestContext;

public class Kerberos extends Entity  {
    public static final String KERBEROS_REQUEST = "KERBEROS_REQUEST";

    public static final String DEFAULT_MASTERKEY = "masterkey";

    public static final String DEFAULT_ADMIN_USER = "admin";

    public static final String DEFAULT_ADMIN_PASSWORD = "password";

    private KerberosRequest request;

    Kerberos(String newId) {
        super(newId);
        this.request = new KerberosRequest();
    }

    Kerberos() {
        this(KERBEROS_REQUEST);
    }

    public KerberosRequest getRequest() {
        return request;
    }

    public void setRequest(KerberosRequest request) {
        this.request = request;
    }

    public Kerberos withMasterKey(String masterKey) {
        request.setMasterKey(masterKey);
        return this;
    }

    public Kerberos withAdmin(String admin) {
        request.setAdmin(admin);
        return this;
    }

    public Kerberos withPassword(String password) {
        request.setPassword(password);
        return this;
    }

    public static Kerberos request(String key) {
        return new Kerberos(key);
    }

    public static Kerberos request() {
        return new Kerberos();
    }

    public static Function<IntegrationTestContext, Kerberos> getTestContextCluster(String key) {
        return testContext -> testContext.getContextParam(key, Kerberos.class);
    }

    public static Function<IntegrationTestContext, Kerberos> getTestContextCluster() {
        return getTestContextCluster(KERBEROS_REQUEST);
    }
}
