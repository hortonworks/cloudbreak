package com.sequenceiq.cloudbreak.dto;

import java.util.Locale;

import com.google.common.xml.XmlEscapers;
import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.DirectoryType;
import com.sequenceiq.cloudbreak.view.VersionConstant;

@Deprecated(since = VersionConstant.NEW_VIEW_VERSION)
public class LdapView {

    private String bindDn;

    private String bindPassword;

    private DirectoryType directoryType;

    private String userSearchBase;

    private String userNameAttribute;

    private String userObjectClass;

    private String groupSearchBase;

    private String groupNameAttribute;

    private String groupObjectClass;

    private String groupMemberAttribute;

    private String domain;

    private String protocol;

    private String adminGroup;

    private String userGroup;

    private String userDnPattern;

    private String serverHost;

    private String connectionURL;

    private Integer serverPort;

    private String certificate;

    private LdapView() {
    }

    public String getServerHost() {
        return serverHost;
    }

    public Integer getServerPort() {
        return serverPort;
    }

    public boolean isSecure() {
        return "ldaps".equalsIgnoreCase(protocol.toLowerCase(Locale.ROOT));
    }

    public String getProtocol() {
        return protocol;
    }

    public String getConnectionURL() {
        return connectionURL;
    }

    public String getBindDn() {
        return bindDn;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public String getBindPasswordEscaped() {
        return XmlEscapers.xmlAttributeEscaper().escape(bindPassword);
    }

    public DirectoryType getDirectoryType() {
        return directoryType;
    }

    public String getDirectoryTypeShort() {
        if (directoryType == DirectoryType.ACTIVE_DIRECTORY) {
            return "ad";
        }
        return "ldap";
    }

    public boolean isLdap() {
        return directoryType != DirectoryType.ACTIVE_DIRECTORY;
    }

    public boolean isAd() {
        return directoryType == DirectoryType.ACTIVE_DIRECTORY;
    }

    public String getUserSearchBase() {
        return userSearchBase;
    }

    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    public String getUserObjectClass() {
        return userObjectClass;
    }

    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public String getGroupNameAttribute() {
        return groupNameAttribute;
    }

    public String getGroupObjectClass() {
        return groupObjectClass;
    }

    public String getGroupMemberAttribute() {
        return groupMemberAttribute;
    }

    public String getDomain() {
        return domain;
    }

    public String getAdminGroup() {
        return adminGroup;
    }

    public String getUserGroup() {
        return userGroup;
    }

    public String getUserDnPattern() {
        return userDnPattern;
    }

    public String getCertificate() {
        return certificate;
    }

    public static final class LdapViewBuilder {
        private String bindDn;

        private String bindPassword;

        private DirectoryType directoryType;

        private String userSearchBase;

        private String userNameAttribute;

        private String userObjectClass;

        private String groupSearchBase;

        private String groupNameAttribute;

        private String groupObjectClass;

        private String groupMemberAttribute;

        private String domain;

        private String protocol;

        private String adminGroup;

        private String userGroup;

        private String userDnPattern;

        private String serverHost;

        private String connectionURL;

        private Integer serverPort;

        private String certificate;

        private LdapViewBuilder() {
        }

        public static LdapViewBuilder aLdapView() {
            return new LdapViewBuilder();
        }

        public LdapViewBuilder withBindDn(String bindDn) {
            this.bindDn = bindDn;
            return this;
        }

        public LdapViewBuilder withBindPassword(String bindPassword) {
            this.bindPassword = bindPassword;
            return this;
        }

        public LdapViewBuilder withDirectoryType(DirectoryType directoryType) {
            this.directoryType = directoryType;
            return this;
        }

        public LdapViewBuilder withUserSearchBase(String userSearchBase) {
            this.userSearchBase = userSearchBase;
            return this;
        }

        public LdapViewBuilder withUserNameAttribute(String userNameAttribute) {
            this.userNameAttribute = userNameAttribute;
            return this;
        }

        public LdapViewBuilder withUserObjectClass(String userObjectClass) {
            this.userObjectClass = userObjectClass;
            return this;
        }

        public LdapViewBuilder withGroupSearchBase(String groupSearchBase) {
            this.groupSearchBase = groupSearchBase;
            return this;
        }

        public LdapViewBuilder withGroupNameAttribute(String groupNameAttribute) {
            this.groupNameAttribute = groupNameAttribute;
            return this;
        }

        public LdapViewBuilder withGroupObjectClass(String groupObjectClass) {
            this.groupObjectClass = groupObjectClass;
            return this;
        }

        public LdapViewBuilder withGroupMemberAttribute(String groupMemberAttribute) {
            this.groupMemberAttribute = groupMemberAttribute;
            return this;
        }

        public LdapViewBuilder withDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public LdapViewBuilder withProtocol(String protocol) {
            this.protocol = protocol;
            return this;
        }

        public LdapViewBuilder withAdminGroup(String adminGroup) {
            this.adminGroup = adminGroup;
            return this;
        }

        public LdapViewBuilder withUserGroup(String userGroup) {
            this.userGroup = userGroup;
            return this;
        }

        public LdapViewBuilder withUserDnPattern(String userDnPattern) {
            this.userDnPattern = userDnPattern;
            return this;
        }

        public LdapViewBuilder withServerHost(String serverHost) {
            this.serverHost = serverHost;
            return this;
        }

        public LdapViewBuilder withConnectionURL(String connectionURL) {
            this.connectionURL = connectionURL;
            return this;
        }

        public LdapViewBuilder withServerPort(Integer serverPort) {
            this.serverPort = serverPort;
            return this;
        }

        public LdapViewBuilder withCertificate(String certificate) {
            this.certificate = certificate;
            return this;
        }

        public LdapView build() {
            LdapView ldapView = new LdapView();
            ldapView.bindDn = bindDn;
            ldapView.bindPassword = bindPassword;
            ldapView.userNameAttribute = this.userNameAttribute;
            ldapView.protocol = this.protocol;
            ldapView.serverHost = this.serverHost;
            ldapView.groupObjectClass = this.groupObjectClass;
            ldapView.serverPort = this.serverPort;
            ldapView.adminGroup = this.adminGroup;
            ldapView.userGroup = this.userGroup;
            ldapView.directoryType = this.directoryType;
            ldapView.domain = this.domain;
            ldapView.groupMemberAttribute = this.groupMemberAttribute;
            ldapView.groupNameAttribute = this.groupNameAttribute;
            ldapView.userSearchBase = this.userSearchBase;
            ldapView.userDnPattern = this.userDnPattern;
            ldapView.groupSearchBase = this.groupSearchBase;
            ldapView.userObjectClass = this.userObjectClass;
            ldapView.certificate = this.certificate;
            if (connectionURL != null) {
                ldapView.connectionURL = this.connectionURL;
            } else {
                ldapView.connectionURL = protocol + "://" + serverHost;
                if (serverPort != null) {
                    ldapView.connectionURL = ldapView.connectionURL.toLowerCase(Locale.ROOT) + ':' + serverPort;
                }
            }
            return ldapView;
        }
    }
}
