create_recipe_log_dir_pre_start:
  file.directory:
    - name: /var/log/recipes/pre-ambari-start
    - makedirs: True

{% for hg, args in pillar.get('recipes', {}).items() %}
{% if grains['hostgroup'] == hg %}
{% if args['pre-ambari-start'] is defined %}
{% for script_name in args['pre-ambari-start'] %}
/opt/scripts/pre-ambari-start/{{ script_name }}:
  file.managed:
     - source:
       - salt://pre-recipes/scripts/{{ script_name }}
       - salt://pre-recipes/scripts/pre-date.sh
     - makedirs: True
     - mode: 755

run_pre_ambari_start_script_{{ script_name }}:
  cmd.run:
    - name: /opt/scripts/recipe-runner.sh pre-ambari-start {{ script_name }}
    - onlyif:
      - test -f /opt/scripts/pre-ambari-start/{{ script_name }}
      - test ! -f /var/log/recipes/pre-ambari-start/{{ script_name }}.success
    - timeout: 600
{% endfor %}
{% endif %}

{% endif %}
{% endfor %}