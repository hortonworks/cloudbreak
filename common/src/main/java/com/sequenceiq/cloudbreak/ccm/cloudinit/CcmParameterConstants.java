package com.sequenceiq.cloudbreak.ccm.cloudinit;

/**
 * Provides constants used when passing CCM parameters to cloud-init scripts.
 */
@SuppressWarnings("WeakerAccess")
public class CcmParameterConstants {

    /**
     * The template model key for whether CCM is enabled.
     */
    public static final String CCM_ENABLED_KEY = "ccmEnabled";

    /**
     * The template model key for the hostname used for connecting to CCM via SSH.
     */
    public static final String CCM_HOST_KEY = "ccmHost";

    /**
     * The template model key for the port used for connecting to CCM via SSH.
     */
    public static final String CCM_SSH_PORT_KEY = "ccmSshPort";

    /**
     * The default CCM SSH port.
     */
    public static final int DEFAULT_CCM_SSH_PORT = 8990;

    /**
     * The template model key for the public key for CCM, which allows the client to verify that it is talking to a valid CCM.
     */
    public static final String CCM_PUBLIC_KEY_KEY = "ccmPublicKey";

    /**
     * The (known hosts file) format for the public key for CCM. The first placeholder is the host address,
     * the second placeholder is for the SSH port, and the third placeholder is for the public key itself.
     */
    public static final String CCM_PUBLIC_KEY_FORMAT = "[%s]:%d %s";

    /**
     * The template model key for the optional tunnel initiator ID.
     */
    public static final String TUNNEL_INITIATOR_ID_KEY = "ccmTunnelInitiatorId";

    /**
     * The template model key for the key ID under which the private key was registered with CCM.
     */
    public static final String KEY_ID_KEY = "ccmKeyId";

    /**
     * The template model key for the enciphered private key, which CCM uses to authenticate the instance.
     */
    public static final String ENCIPHERED_PRIVATE_KEY_KEY = "ccmEncipheredPrivateKey";

    /**
     * The template model key for specifying the port for which a tunnel is being registered.
     * The first placeholder is for the first character of the well-known service identifier, which will
     * be uppercased, and the second placeholder is for the remainder of the well-known service identifier,
     * which will remain in its current case.
     */
    public static final String SERVICE_PORT_KEY_FORMAT = "ccm%C%sPort";

    /**
     * Private constructor to prevent instantiation.
     */
    private CcmParameterConstants() {
    }
}
