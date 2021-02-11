package com.sequenceiq.freeipa.client.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;

public class SetWlCredentialOperation extends UserModOperation {

    private String hashedPassword;

    private String unencryptedKrbPrincipalKey;

    private List<String> sshPublicKeys;

    private String expiration;

    private Optional<String> title;

    public SetWlCredentialOperation(String user, String hashedPassword, String unencryptedKrbPrincipalKey,
            List<String> sshPublicKeys, String expiration, Optional<String> title) {
        setUser(user);
        this.hashedPassword = hashedPassword;
        this.unencryptedKrbPrincipalKey = unencryptedKrbPrincipalKey;
        this.expiration = expiration;
        this.sshPublicKeys = sshPublicKeys;
        this.title = title;
    }

    public static SetWlCredentialOperation create(String user, String hashedPassword, String unencryptedKrbPrincipalKey,
            List<String> sshPublicKeys, String expiration) {
        return new SetWlCredentialOperation(user, hashedPassword, unencryptedKrbPrincipalKey, sshPublicKeys, expiration, Optional.empty());
    }

    public static SetWlCredentialOperation create(String user, String hashedPassword, String unencryptedKrbPrincipalKey,
            List<String> sshPublicKeys, String expiration, String title) {
        return new SetWlCredentialOperation(user, hashedPassword, unencryptedKrbPrincipalKey, sshPublicKeys, expiration, Optional.of(title));
    }

    @Override
    protected Map<String, Object> getParams() {
        Map<String, Object> params = sensitiveMap();
        List<String> attributes = new ArrayList<>();
        if (StringUtils.isNotBlank(hashedPassword)) {
            attributes.add("cdpHashedPassword=" + hashedPassword);
            attributes.add("cdpUnencryptedKrbPrincipalKey=" + unencryptedKrbPrincipalKey);
            attributes.add("krbPasswordExpiration=" + expiration);
        }

        // In order for the workload credentials update optimization to work correctly, we must update the title attribute
        // whenever we set workload credentials in the IPA, whether or not a workload password is set. This is because the
        // credentials version might have changed; for example, due to the addition or deletion of an SSH public key.
        // However, when the optimization is disabled, we don't modify the title attribute at all.
        title.ifPresent(titleAttr -> attributes.add("title=" + titleAttr));
        if (!attributes.isEmpty()) {
            params.put("setattr", attributes);
        }

        params.put("ipasshpubkey", sshPublicKeys);
        return params;
    }
}
