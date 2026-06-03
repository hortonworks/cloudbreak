{% if grains['os_family'] == 'RedHat' and grains['osmajorrelease'] | int == 8 %}
configure_crond_args:
  file.replace:
    - name: /etc/sysconfig/crond
    - pattern: '^CRONDARGS=$'
    - repl: 'CRONDARGS=-s'

restart_crond_service:
  service.running:
    - name: crond
    - enable: True
    - watch:
        - file: configure_crond_args

{% endif %}