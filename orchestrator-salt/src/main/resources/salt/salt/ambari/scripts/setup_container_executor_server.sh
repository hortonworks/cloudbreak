# container executor
for FILE in $(find /var/lib/ambari-server/resources/stacks/HDP/ -name container-executor.cfg.j2)
do
  LINE="feature.docker.enabled=1"
  grep -q "$LINE" "$FILE" || echo "$LINE" >> $FILE
  LINE="yarn.nodemanager.linux-container-executor.nonsecure-mode.local.user=false"
  grep -q "$LINE" "$FILE" || echo "$LINE" >> $FILE
done