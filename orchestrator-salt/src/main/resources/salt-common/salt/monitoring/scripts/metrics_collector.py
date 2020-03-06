import argparse
import logging
import time
import uuid
import sys
import yaml

def parse_args():
    parser = argparse.ArgumentParser(
        description='Python script to generate periodic metrics from a source '
                    'metrics collector')
    parser.add_argument('--config', type=str, required=True,
                        help='Path to metrics collector configuration')
    args = parser.parse_args()
    return args

def main():
    args = parse_args()
    with open(args.config) as file:
        config = yaml.load(file)
        if "clouderaManager" in config:
            from cm_metrics_collector import ClouderaManagerMetricsCollector
            ClouderaManagerMetricsCollector(config).start()
        elif "freeIpa" in config:
            # TODO
            pass
        else:
            print "Configuration should contain Cloudera Manager or FreeIPA section. Exiting ..."
            sys.exit(1)

if __name__ == '__main__':
   main()