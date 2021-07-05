package com.sequenceiq.environment.environment.dto;

public class AuthenticationDto {

    private final String publicKey;

    private final String publicKeyId;

    private final String loginUserName;

    private final boolean managedKey;

    private AuthenticationDto(Builder builder) {
        loginUserName = builder.loginUserName;
        publicKey = builder.publicKey;
        publicKeyId = builder.publicKeyId;
        managedKey = builder.managedKey;
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

    public boolean isManagedKey() {
        return managedKey;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "AuthenticationDto{" +
                "publicKey='" + publicKey + '\'' +
                ", publicKeyId='" + publicKeyId + '\'' +
                ", loginUserName='" + loginUserName + '\'' +
                ", managedKey=" + managedKey +
                '}';
    }

    public static final class Builder {

        private String loginUserName;

        private String publicKey;

        private String publicKeyId;

        private boolean managedKey;

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

        public Builder withManagedKey(boolean managedKey) {
            this.managedKey = managedKey;
            return this;
        }

        public AuthenticationDto build() {
            return new AuthenticationDto(this);
        }
    }
}
