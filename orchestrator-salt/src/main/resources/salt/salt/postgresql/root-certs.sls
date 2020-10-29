{% if salt['pillar.get']('postgres_root_certs:ssl_certs') is defined and salt['pillar.get']('postgres_root_certs:ssl_certs')|length > 1 %}
create-root-certs-file:
  file.managed:
    - name: /hadoopfs/fs1/database-cacerts/certs.pem
    - makedirs: True
    - contents_pillar: postgres_root_certs:ssl_certs
    - user: root
    - group: root
    - mode: 644
    - unless: test -f /hadoopfs/fs1/database-cacerts/certs.pem
{% endif %}