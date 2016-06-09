{% for hg, args in pillar.get('recipes', {}).items() %}
{% if grains['hostgroup'] == hg %}
{% for scrip_name in args['pre'] %}
/opt/scripts/pre/{{ scrip_name }}:
  file.managed:
     - source: salt://pre-recipes/scripts/{{ scrip_name }}
     - makedirs: True
     - mode: 755

run_pre_script_{{ scrip_name }}:
  cmd.run:
    - name: /opt/scripts/pre/{{ scrip_name }}
{% endfor %}
{% endif %}
{% endfor %}