{% set timeout = salt['pillar.get']('recipes:timeout') %}
create_recipe_log_dir_pre_start:
  file.directory:
    - name: /var/log/recipes/pre-cloudera-manager-start
    - makedirs: True

{% for hg, args in pillar.get('recipes', {}).items() %}
{% if grains['hostgroup'] == hg %}
{% if args['pre-cloudera-manager-start'] is defined %}
{% for script_name in args['pre-cloudera-manager-start'] %}
/opt/scripts/pre-cloudera-manager-start/{{ script_name }}:
  file.managed:
     - source:
       - salt://pre-recipes/scripts/{{ script_name }}
       - salt://pre-recipes/scripts/pre-date.sh
     - makedirs: True
     - mode: 755

run_pre_cm_start_script_{{ script_name }}:
  cmd.run:
    - name: /opt/scripts/recipe-runner.sh pre-cloudera-manager-start {{ script_name }}
    - onlyif:
      - test -f /opt/scripts/pre-cloudera-manager-start/{{ script_name }}
      - test ! -f /var/log/recipes/pre-cloudera-manager-start/{{ script_name }}.success
    - timeout: {{ timeout }}
{% endfor %}
{% endif %}

{% endif %}
{% endfor %}