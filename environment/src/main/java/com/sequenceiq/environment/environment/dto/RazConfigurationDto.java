package com.sequenceiq.environment.environment.dto;

public class RazConfigurationDto {

    private final boolean razEnabled;

    private final String securityGroupIdForRaz;

    public RazConfigurationDto(boolean razEnabled, String securityGroupIdForRaz) {
        this.razEnabled = razEnabled;
        this.securityGroupIdForRaz = securityGroupIdForRaz;
    }

    public boolean isRazEnabled() {
        return razEnabled;
    }

    public String getSecurityGroupIdForRaz() {
        return securityGroupIdForRaz;
    }

    public static RazConfigurationDtoBuilder builder() {
        return new RazConfigurationDtoBuilder();
    }

    public static final class RazConfigurationDtoBuilder {

        private boolean razEnabled;

        private String securityGroupIdForRaz;

        private RazConfigurationDtoBuilder() {
        }

        public RazConfigurationDtoBuilder withRazEnabled(boolean razEnabled) {
            this.razEnabled = razEnabled;
            return this;
        }

        public RazConfigurationDtoBuilder withSecurityGroupIdForRaz(String securityGroupIdForRaz) {
            this.securityGroupIdForRaz = securityGroupIdForRaz;
            return this;
        }

        public RazConfigurationDto build() {
            return new RazConfigurationDto(razEnabled, securityGroupIdForRaz);
        }
    }
}
