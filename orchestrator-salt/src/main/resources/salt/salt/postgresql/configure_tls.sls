{%- from 'postgresql/settings.sls' import postgresql with context %}

{% set postgres_data_on_attached_disk = salt['pillar.get']('postgres:postgres_data_on_attached_disk', 'False') %}
{% set configure_remote_db = salt['pillar.get']('postgres:configure_remote_db', 'None') %}
{% set postgres_version = salt['pillar.get']('postgres:postgres_version', '10') | int %}
{%- set pg_dir = '/etc/systemd/system/postgresql-' ~ postgres_version ~ '.service.d' %}
{%- set openssl_file = pg_dir ~ '/openssl.conf' %}
{%- set tls_advanced_control = postgresql.tls_advanced_control == True %}

{%- if postgres_version == 17 and 'None' == configure_remote_db and postgres_data_on_attached_disk and postgresql.ssl_enabled == True %}

{%- if tls_advanced_control %}

/etc/pki/tls/postgres-openssl.cnf:
  file.managed:
    - user: root
    - group: postgres
    - mode: 640
    - require:
      - cmd: configure-ssl
      - cmd: init-services-db
    - contents: |
        # DO NOT edit - Managed by Cloudbreak
        openssl_conf = openssl_init

        [openssl_init]
        ssl_conf = ssl_sect

        [ssl_sect]
        system_default = system_default_sect

        [system_default_sect]
        {%- if postgresql.tls_min_version %}
        MinProtocol = {{ postgresql.tls_min_version }}
        {%- endif %}
        {%- if postgresql.tls_max_version %}
        MaxProtocol = {{ postgresql.tls_max_version }}
        {%- endif %}
        {%- if postgresql.tls13_ciphers %}
        Ciphersuites = {{ postgresql.tls13_ciphers }}
        {%- endif %}

{{ pg_dir }}:
  file.directory:
    - user: root
    - group: root
    - mode: 755
    - makedirs: True
    - require:
      - cmd: configure-ssl
      - cmd: init-services-db

{{ openssl_file }}:
  file.managed:
    - user: root
    - group: root
    - mode: 644
    - require:
      - file: {{ pg_dir }}
      - file: /etc/pki/tls/postgres-openssl.cnf
    - contents: |
        [Service]
        Environment=OPENSSL_CONF=/etc/pki/tls/postgres-openssl.cnf

{%- else %}

/etc/pki/tls/postgres-openssl.cnf:
  file.absent:
    - require:
      - cmd: configure-ssl
      - cmd: init-services-db

{{ openssl_file }}:
  file.absent:
    - require:
      - file: /etc/pki/tls/postgres-openssl.cnf

{%- endif %}

systemctl-daemon-reload-for-postgres-openssl:
  cmd.run:
    - name: systemctl daemon-reload
    - onchanges:
      - file: {{ openssl_file }}

{%- endif %}
