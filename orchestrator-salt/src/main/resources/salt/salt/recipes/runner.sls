/opt/scripts/recipe-runner.sh:
  file.managed:
     - source:
       - salt://recipes/scripts/recipe-runner.sh
     - makedirs: True
     - mode: 755