{% set timeout = salt['pillar.get']('recipes:timeout') %}
create_recipe_log_dir_pre_termination:
  file.directory:
    - name: /var/log/recipes/pre-termination
    - makedirs: True

cleanup_pre_termination_scripts:
  cmd.run:
    - name: rm -rf /opt/scripts/pre-termination/*

{% for hg, args in pillar.get('recipes', {}).items() %}
{% if grains['hostgroup'] == hg %}
{% if args['pre-termination'] is defined %}
{% for script_name in args['pre-termination'] %}
/opt/scripts/pre-termination/{{ script_name }}:
  file.managed:
     - source:
       - salt://pre-recipes/scripts/{{ script_name }}
       - salt://pre-recipes/scripts/pre-date.sh
     - makedirs: True
     - mode: 700

run_pre_termination_script_{{ script_name }}:
  cmd.run:
    - name: /opt/scripts/recipe-runner.sh pre-termination {{ script_name }}
    - onlyif:
      - test -f /opt/scripts/pre-termination/{{ script_name }}
      - test ! -f /var/log/recipes/pre-termination/{{ script_name }}.success
    - timeout: {{ timeout }}
{% endfor %}
{% endif %}

{% endif %}
{% endfor %}