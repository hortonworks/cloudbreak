package com.sequenceiq.freeipa.converter.credential;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.sequenceiq.cloudbreak.service.secret.domain.Secret;
import com.sequenceiq.cloudbreak.service.secret.model.SecretResponse;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultKvV2Engine;
import com.sequenceiq.cloudbreak.service.secret.vault.VaultSecret;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.credential.CredentialRequest;
import com.sequenceiq.freeipa.entity.Credential;

@Component
public class CredentialRequestToCredentialConverter implements Converter<CredentialRequest, Credential> {

    @Override
    public Credential convert(CredentialRequest source) {
        Credential credential = new Credential();
        SecretResponse sourceSecret = source.getSecret();
        VaultSecret vaultSecret = new VaultSecret(sourceSecret.getEnginePath(), VaultKvV2Engine.class.getCanonicalName(), sourceSecret.getSecretPath());
        String json = new Gson().toJson(vaultSecret);
        Secret secret = new Secret(null, json);
        credential.setAttributes(secret);
        credential.setName(source.getName());
        return credential;
    }

}
