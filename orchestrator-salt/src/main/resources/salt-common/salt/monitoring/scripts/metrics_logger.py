import json
import logging
from logging.handlers import RotatingFileHandler

class MetricsLogger:
    def __init__(self, name, path, max_bytes, backup_count, debug=False):
        fmt = '%(message)s'
        logging.basicConfig(
                 level=logging.INFO,
                 format=fmt,
        )
        formatter = logging.Formatter(fmt)
        handler = RotatingFileHandler(path, maxBytes=max_bytes, backupCount=backup_count, mode='a')
        handler.setLevel(logging.INFO)
        handler.setFormatter(formatter)
        self.metrics_logger = logging.getLogger(name)
        self.metrics_logger.addHandler(handler)

    def process(self, event):
        self.metrics_logger.info(json.dumps(event))