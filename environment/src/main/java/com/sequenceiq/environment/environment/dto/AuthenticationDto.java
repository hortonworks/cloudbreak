package com.sequenceiq.environment.environment.dto;

public class AuthenticationDto {

    private String publicKey;

    private String publicKeyId;

    private String loginUserName;

    private AuthenticationDto() {
    }

    public String getPublicKey() {
        return publicKey;
    }

    public String getPublicKeyId() {
        return publicKeyId;
    }

    public String getLoginUserName() {
        return loginUserName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String loginUserName;

        private String publicKey;

        private String publicKeyId;

        private Builder() {
        }

        public Builder withLoginUserName(String loginUserName) {
            this.loginUserName = loginUserName;
            return this;
        }

        public Builder withPublicKey(String publicKey) {
            this.publicKey = publicKey;
            return this;
        }

        public Builder withPublicKeyId(String publicKeyId) {
            this.publicKeyId = publicKeyId;
            return this;
        }

        public AuthenticationDto build() {
            AuthenticationDto authenticationDto = new AuthenticationDto();
            authenticationDto.loginUserName = loginUserName;
            authenticationDto.publicKey = publicKey;
            authenticationDto.publicKeyId = publicKeyId;
            return authenticationDto;
        }
    }
}
