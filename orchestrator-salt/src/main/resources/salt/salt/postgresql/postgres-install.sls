start-postgresql:
  service.running:
    - enable: True
    - name: postgresql

/opt/salt/scripts/init_db.sh:
  file.managed:
    - makedirs: True
    - mode: 755
    - source: salt://postgresql/scripts/init_db.sh

{% for service, values in pillar.get('postgres', {}).items()  %}

{% if values['user'] is defined %}
init-{{ service }}-db:
  cmd.run:
    - name: /opt/salt/scripts/init_db.sh
    - runas: postgres
    - env:
      - DBUSER: {{ values['user'] }}
      - PASSWORD: {{ values['password'] }}
      - DATABASE:  {{ values['database'] }}
      - SERVICE: {{ service }}
{% endif %}

{% endfor %}

reload-postgresql:
  cmd.run:
    - name: service postgresql reload
