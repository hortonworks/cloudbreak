{% set timeout = salt['pillar.get']('recipes:timeout') %}
create_recipe_log_dir_post_start:
  file.directory:
    - name: /var/log/recipes/post-cloudera-manager-start
    - makedirs: True

cleanup_post_cloudera_manager_start_scripts:
  cmd.run:
    - name: rm -rf /opt/scripts/post-cloudera-manager-start/*

{% for hg, args in pillar.get('recipes', {}).items() %}
{% if grains['hostgroup'] == hg %}
{% if args['post-cloudera-manager-start'] is defined %}
{% for script_name in args['post-cloudera-manager-start'] %}
/opt/scripts/post-cloudera-manager-start/{{ script_name }}:
  file.managed:
     - source:
       - salt://pre-recipes/scripts/{{ script_name }}
       - salt://pre-recipes/scripts/pre-date.sh
     - makedirs: True
     - mode: 700

run_post_cm_start_script_{{ script_name }}:
  cmd.run:
    - name: /opt/scripts/recipe-runner.sh post-cloudera-manager-start {{ script_name }}
    - onlyif:
      - test -f /opt/scripts/post-cloudera-manager-start/{{ script_name }}
      - test ! -f /var/log/recipes/post-cloudera-manager-start/{{ script_name }}.success
    - timeout: {{ timeout }}
{% endfor %}
{% endif %}

{% endif %}
{% endfor %}