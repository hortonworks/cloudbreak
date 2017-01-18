ALERT ${alertName}
  IF Hadoop_NameNode_CapacityUsedGB{}/Hadoop_NameNode_CapacityTotalGB{} * 100 ${operator} ${threshold}
  FOR ${period}m
  LABELS {severity="critical"}
  ANNOTATIONS {description="HDFS usage has exceeded the threshold with a value of {{ $value }}.", summary="Instance {{ $labels.instance }} memory usage is dangerously high"}