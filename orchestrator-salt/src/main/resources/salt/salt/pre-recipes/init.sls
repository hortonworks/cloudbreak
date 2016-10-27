create_recipe_log_dir:
  file.directory:
    - name: /var/log/recipes

{% for hg, args in pillar.get('recipes', {}).items() %}
{% if grains['hostgroup'] == hg %}
{% for scrip_name in args['pre'] %}
/opt/scripts/pre/{{ scrip_name }}:
  file.managed:
     - source:
       - salt://pre-recipes/scripts/{{ scrip_name }}
       - salt://pre-recipes/scripts/pre-date.sh
     - makedirs: True
     - mode: 755

run_pre_script_{{ scrip_name }}:
  cmd.run:
    - name: sh -x /opt/scripts/pre/{{ scrip_name }} 2>&1 | tee -a /var/log/recipes/pre-{{ scrip_name }}.log && exit ${PIPESTATUS[0]}
    - onlyif:
      - ls /opt/scripts/pre/{{ scrip_name }}
    - unless: ls /var/log/recipes/pre-{{ scrip_name }}.log
{% endfor %}
{% endif %}
{% endfor %}