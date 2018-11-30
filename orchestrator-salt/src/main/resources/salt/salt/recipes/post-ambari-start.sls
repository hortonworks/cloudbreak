{% set timeout = salt['pillar.get']('recipes:timeout') %}
create_recipe_log_dir_post_start:
  file.directory:
    - name: /var/log/recipes/post-ambari-start
    - makedirs: True

{% for hg, args in pillar.get('recipes', {}).items() %}
{% if grains['hostgroup'] == hg %}
{% if args['post-ambari-start'] is defined %}
{% for script_name in args['post-ambari-start'] %}
/opt/scripts/post-ambari-start/{{ script_name }}:
  file.managed:
     - source:
       - salt://pre-recipes/scripts/{{ script_name }}
       - salt://pre-recipes/scripts/pre-date.sh
     - makedirs: True
     - mode: 755

run_post_ambari_start_script_{{ script_name }}:
  cmd.run:
    - name: /opt/scripts/recipe-runner.sh post-ambari-start {{ script_name }}
    - onlyif:
      - test -f /opt/scripts/post-ambari-start/{{ script_name }}
      - test ! -f /var/log/recipes/post-ambari-start/{{ script_name }}.success
    - timeout: {{ timeout }}
{% endfor %}
{% endif %}

{% endif %}
{% endfor %}