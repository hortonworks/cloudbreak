ALERT ${alertName}
  IF sum(irate(node_cpu{mode!="idle"}[1m])) / sum (count without (cpu, mode)(node_cpu{mode="system"})) * 100 ${operator} ${threshold}
  FOR ${period}m
  LABELS {severity="critical"}
  ANNOTATIONS {description="The overall CPU usage exceeded the threshold with a value of {{ $value }}.", summary="Overall CPU usage is dangerously high."}