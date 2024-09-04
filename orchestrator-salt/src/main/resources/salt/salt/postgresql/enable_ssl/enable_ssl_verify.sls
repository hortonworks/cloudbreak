{%- from 'postgresql/settings.sls' import postgresql with context %}

{% if postgresql.root_certs_enabled == True %}
create-root-certs-file:
  file.managed:
    - name: {{ postgresql.root_certs_file }}
    - makedirs: True
    - contents_pillar: postgres_root_certs:ssl_certs
    - user: root
    - group: root
    - mode: 644
{% endif %}

# Configure JDBC URL for CM after DB init if client side DB server certificate validation is enabled for the cluster and the CM version is too old to support this natively
{% if postgresql.ssl_enabled == True and postgresql.ssl_for_cm_db_natively_supported == False %}
replace-db-connection-url:
  file.replace:
    - name: /etc/cloudera-scm-server/db.properties
    - pattern: "(#UPDATED BY CDP CP:\n)?com.cloudera.cmf.orm.hibernate.connection.url=.*"
    - repl: "#UPDATED BY CDP CP:\ncom.cloudera.cmf.orm.hibernate.connection.url={{ cloudera_manager_database_connection_url }}"
{% endif %}

{% if postgresql.ssl_enabled == True %}
/opt/salt/scripts/enable_ssl_verify.sh:
  file.managed:
    - makedirs: True
    - user: root
    - group: postgres
    - mode: 750
    - source: salt://postgresql/scripts/enable_ssl_verify.sh
    - template: jinja
{% endif %}

// todo restart postgres
