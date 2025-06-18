{%- if salt['file.directory_exists']('/cdp/ipaldapagent') %}

/cdp/ipaldapagent/config.yaml:
  file.managed:
    - makedirs: True
    - user: root
    - group: root
    - mode: 600
    - source: salt://freeipa/templates/ldapagent_config.yaml.j2
    - template: jinja

start-freeipa-ldapagent:
  service.running:
    - name: cdp-freeipa-ldapagent
    - failhard: True
    - enable: True
    - require:
      - file: /cdp/ipaldapagent/config.yaml
    - watch:
      - file: /cdp/ipaldapagent/config.yaml

  {% endif %}
