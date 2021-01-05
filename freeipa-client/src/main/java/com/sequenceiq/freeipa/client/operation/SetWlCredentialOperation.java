package com.sequenceiq.freeipa.client.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

public class SetWlCredentialOperation extends UserModOperation {

    private String hashedPassword;

    private String unencryptedKrbPrincipalKey;

    private List<String> sshPublicKeys;

    private String expiration;

    public SetWlCredentialOperation(String user, String hashedPassword, String unencryptedKrbPrincipalKey,
            List<String> sshPublicKeys, String expiration) {
        setUser(user);
        this.hashedPassword = hashedPassword;
        this.unencryptedKrbPrincipalKey = unencryptedKrbPrincipalKey;
        this.expiration = expiration;
        this.sshPublicKeys = sshPublicKeys;
    }

    public static SetWlCredentialOperation create(String user, String hashedPassword, String unencryptedKrbPrincipalKey,
            List<String> sshPublicKeys, String expiration) {
        return new SetWlCredentialOperation(user, hashedPassword, unencryptedKrbPrincipalKey, sshPublicKeys, expiration);
    }

    @Override
    protected Map<String, Object> getParams() {
        Map<String, Object> params = sensitiveMap();
        List<String> attributes = new ArrayList<>();
        if (StringUtils.isNotBlank(hashedPassword)) {
            attributes.add("cdpHashedPassword=" + hashedPassword);
            attributes.add("cdpUnencryptedKrbPrincipalKey=" + unencryptedKrbPrincipalKey);
            attributes.add("krbPasswordExpiration=" + expiration);
            params.put("setattr", attributes);
        }
        params.put("ipasshpubkey", sshPublicKeys);
        return params;
    }
}
