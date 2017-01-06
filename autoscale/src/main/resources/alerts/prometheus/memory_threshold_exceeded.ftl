ALERT ${alertName}
  IF (100 - (sum((node_memory_MemFree + node_memory_Cached + node_memory_Buffers)) / sum(node_memory_MemTotal) * 100)) ${operator} ${threshold}
  FOR ${period}m
  LABELS {severity="critical"}
  ANNOTATIONS {description="The overall memory usage has exceeded the threshold with a value of {{ $value }}.", summary="The overall memory usage is dangerously high"}