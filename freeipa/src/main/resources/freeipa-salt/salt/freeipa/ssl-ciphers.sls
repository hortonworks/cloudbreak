{#-
  Hardens the 389-ds directory server (ns-slapd) TLS configuration on :636 (LDAPS) and :389
  (STARTTLS) from the environment's encryption profile (freeipa:encryptionConfig). This is the
  same profile that drives the nginx front end (see salt/nginx/conf/ssl.conf), so the directory
  server follows the operator-selected profile instead of a hardcoded floor. With the platform
  default profile (TLS 1.2 only) the directory server stays on TLS 1.2; selecting a TLS 1.3-only
  profile is what delivers the TLS 1.3-only posture.

  Pillar (populated by FreeIpaEncryptionConfigView#toMap):
    freeipa:encryptionConfig:dirsrvTlsMinVersion  e.g. "TLS1.2" / "TLS1.3"
    freeipa:encryptionConfig:dirsrvTlsMaxVersion  e.g. "TLS1.3"
    freeipa:encryptionConfig:dirsrvCipherSuites   comma-separated IANA cipher names (TLS1.3 + TLS1.2)

  Runs in the provision highstate on every FreeIPA node (see top.sls), so new installs, upscaled
  replicas and repaired/rebuilt nodes inherit the hardening. It is idempotent (unless: guards) and
  only restarts dirsrv when something actually changed.

  dsconf command forms (validated against the target 389-ds via `... -h`):
    - version:  dsconf INST security set --tls-protocol-min TLS1.3 --tls-protocol-max TLS1.3
                (writes sslVersionMin/sslVersionMax on cn=encryption,cn=config)
    - ciphers:  dsconf INST security ciphers set -- '<cipher-string>'
                <cipher-string> is the raw nsSSL3Ciphers syntax: a comma-separated list of cipher
                names each prefixed with + or -, optionally including +all/-all, or the keyword
                'default'. To pin an exclusive set we reset with -all then enable only the approved
                ciphers, e.g. '-all,+TLS_AES_128_GCM_SHA256,+TLS_AES_256_GCM_SHA384'. The '--' is
                required: the cipher-string starts with '-all', which argparse otherwise treats as
                an option flag ("the following arguments are required: cipher-string").
    - restart:  dsctl INST restart  (NOT `dsconf INST restart` -- dsconf has no restart subcommand;
                start/stop/restart of the instance is dsctl's job).

-#}
{%- set os = salt['grains.get']('os') %}
{%- set osMajorRelease = salt['grains.get']('osmajorrelease') | int %}
{%- if os == 'RedHat' and (osMajorRelease == 8 or osMajorRelease == 9) %}
{%- set instance = '$(dsctl -l | head -n1)' %}
{%- set tlsMin = salt['pillar.get']('freeipa:encryptionConfig:dirsrvTlsMinVersion') %}
{%- set tlsMax = salt['pillar.get']('freeipa:encryptionConfig:dirsrvTlsMaxVersion') %}
{%- set ciphers = salt['pillar.get']('freeipa:encryptionConfig:dirsrvCipherSuites') %}
{%- if ciphers %}
{#- Transform the plain IANA list into nsSSL3Ciphers +/- syntax: reset all, enable approved only. #}
{%- set cipherString = '-all,+' ~ (ciphers.split(',') | join(',+')) %}
{%- endif %}

{%- if tlsMin and tlsMax %}
harden_dirsrv_tls_version:
  cmd.run:
    - name: dsconf {{ instance }} security set --tls-protocol-min {{ tlsMin }} --tls-protocol-max {{ tlsMax }}
    - unless: >
        dsconf {{ instance }} security get | grep -iq 'sslversionmin:[[:space:]]*{{ tlsMin }}'
{%- endif %}

{%- if ciphers %}
restrict_dirsrv_ciphers:
  cmd.run:
    - name: dsconf {{ instance }} security ciphers set -- '{{ cipherString }}'
    - unless: >
        dsconf {{ instance }} security get | grep -iqF 'nsSSL3Ciphers: {{ cipherString }}'
{%- endif %}

{%- if (tlsMin and tlsMax) or ciphers %}
restart_dirsrv_after_tls_change:
  cmd.run:
    - name: dsctl {{ instance }} restart
{%- if ciphers %}
    {#- Restart when the RUNNING effective cipher set differs from the profile's approved set.
        cn=encryption changes need a restart to take effect, and `security ciphers list --enabled`
        reports the running state (not the on-disk config) -- so this guard also self-heals a prior
        run that applied the config but whose restart did not complete, instead of being gated on
        "did the config change in *this* run" (which never re-fires after a failed restart). #}
    - unless: >
        test "$(dsconf {{ instance }} security ciphers list --enabled 2>/dev/null | sort | tr '\n' ',')"
        = "$(echo '{{ ciphers }}' | tr ',' '\n' | sort | tr '\n' ',')"
{%- else %}
    - onchanges:
      - cmd: harden_dirsrv_tls_version
{%- endif %}
{%- endif %}
{%- endif %}
