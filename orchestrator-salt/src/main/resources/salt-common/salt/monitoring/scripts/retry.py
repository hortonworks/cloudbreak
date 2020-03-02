import time
import traceback

def retry(func, *args, **kwargs):
  delay = kwargs.pop("delay", 10)
  context = kwargs.pop("context", "")
  logger = kwargs.pop("logger")
  while True:
    try:
      func(*args, **kwargs)
      return
    except Exception as e:
      if logger:
        logger.error("Error occurred during {0} operation: {1}".format(context, str(traceback.format_exc())))
    if logger:
      logger.info("{0}: waiting {1} seconds before retyring again.".format(context, delay))
    time.sleep(delay)