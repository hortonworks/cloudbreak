/opt/scripts/recipe-runner.sh:
  file.managed:
     - source:
       - salt://recipes/scripts/recipe-runner.sh
     - makedirs: True
     - mode: 755

fix_file_context_of_recipe_runner:
  cmd.run:
    - name: restorecon -vFi /opt/scripts/recipe-runner.sh
    - onlyif: command -v restorecon
