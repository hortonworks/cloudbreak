create-root-certs-file:
  file.managed:
    - name: /opt/databases/root-ssl-certs/certs.pem
    - makedirs: True
    - contents_pillar: postgres_root_certs:ssl_certs
    - user: root
    - group: root
    - mode: 644
    - unless: test -f /opt/databases/root-ssl-certs/certs.pem