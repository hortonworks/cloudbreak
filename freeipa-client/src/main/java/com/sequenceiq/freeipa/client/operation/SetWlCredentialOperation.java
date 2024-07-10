package com.sequenceiq.freeipa.client.operation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetWlCredentialOperation extends UserModOperation {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetWlCredentialOperation.class);

    private static final String CDP_HASHED_PASSWORD = "cdpHashedPassword";

    private static final String CDP_UNENCRYPTED_KRB_PRINCIPAL_KEY = "cdpUnencryptedKrbPrincipalKey";

    private final String hashedPassword;

    private final String unencryptedKrbPrincipalKey;

    private final List<String> sshPublicKeys;

    private final String expiration;

    private final Optional<String> title;

    private final boolean postfixSupported;

    public SetWlCredentialOperation(String user, String hashedPassword, String unencryptedKrbPrincipalKey,
            List<String> sshPublicKeys, String expiration, Optional<String> title, boolean postfixSupported) {
        setUser(user);
        this.hashedPassword = hashedPassword;
        this.unencryptedKrbPrincipalKey = unencryptedKrbPrincipalKey;
        this.expiration = expiration;
        this.sshPublicKeys = sshPublicKeys;
        this.title = title;
        this.postfixSupported = postfixSupported;
    }

    public static SetWlCredentialOperation create(String user, String hashedPassword, String unencryptedKrbPrincipalKey,
            List<String> sshPublicKeys, String expiration, boolean postfixSupported) {
        return new SetWlCredentialOperation(user, hashedPassword, unencryptedKrbPrincipalKey, sshPublicKeys, expiration, Optional.empty(), postfixSupported);
    }

    public static SetWlCredentialOperation create(String user, String hashedPassword, String unencryptedKrbPrincipalKey,
            List<String> sshPublicKeys, String expiration, String title, boolean postfixSupported) {
        return new SetWlCredentialOperation(user, hashedPassword, unencryptedKrbPrincipalKey, sshPublicKeys, expiration, Optional.of(title),
                postfixSupported);
    }

    @Override
    protected Map<String, Object> getParams() {
        Map<String, Object> params = sensitiveMap();
        List<String> attributes = new ArrayList<>();
        if (StringUtils.isNoneBlank(hashedPassword, unencryptedKrbPrincipalKey)) {
            String postFix = postfixSupported ? "POSTTS" + System.currentTimeMillis() : "";
            attributes.add(CDP_HASHED_PASSWORD + "=" + hashedPassword + postFix);
            attributes.add(CDP_UNENCRYPTED_KRB_PRINCIPAL_KEY + "=" + unencryptedKrbPrincipalKey + postFix);
            attributes.add("krbPasswordExpiration=" + expiration);
        } else if (StringUtils.isNotBlank(hashedPassword) ^ StringUtils.isNotBlank(unencryptedKrbPrincipalKey)) {
            LOGGER.warn("Not possible to update password as hashedPassword is [{}] and unencryptedKrbPrincipalKey is [{}]",
                    StringUtils.isBlank(hashedPassword) ? "BLANK" : "PRESENT", StringUtils.isBlank(unencryptedKrbPrincipalKey) ? "BLANK" : "PRESENT");
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
