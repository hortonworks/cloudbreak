import cm_client
import os
import json
import logging
import signal
import socket
import sys
import time
import datetime
import traceback
from cm_client.rest import ApiException
from pprint import pprint
from threading import Timer
from logging.handlers import RotatingFileHandler
from metrics_logger import MetricsLogger

import urllib3
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)

class ClouderaManagerMetricsCollector:

    def __init__(self, config):
        self.config = config
        self.logger = logging.getLogger('ClouderaManagerMetricsCollector')
        self._setup_logging(config)
        self.logger.info("Load configuration: " + str(config))
        self.cluster = None
        self.services = []
        self.availableMetrics = {}
        self.serviceMetricsMapFromConfig = {}
        self._create_service_metrics_map()
        self._setup_termination_handler()
        self._setup_metrics_processor(config)
        self._setup_health_check_metrics_processor(config)
        cm_client.configuration.username = config["clouderaManager"]["user"]
        cm_client.configuration.password = config["clouderaManager"]["password"]
        cm_client.configuration.verify_ssl = False
        self.cluster_type = config["clouderaManager"]["clusterType"]
        if socket.gethostname().find('.') >= 0:
            api_host = socket.gethostname()
        else:
            api_host = socket.gethostbyaddr(socket.gethostname())[0]
        protocol = config["clouderaManager"]["protocol"]
        port = config["clouderaManager"]["port"]
        api_version = config["clouderaManager"]["apiVersion"]
        api_url = protocol + "://" + api_host + ':' + port + '/api/' + api_version
        self.api_client = cm_client.ApiClient(api_url)
        self.logger.info("Cloudera Manager Api URL: " + api_url)
        metrics_api_url = protocol + "://" + api_host + ':' + port + '/api/v5'
        self.metrics_api_client = cm_client.ApiClient(metrics_api_url)
        self.logger.info("Cloudera Manager Metrics Api URL: " + metrics_api_url)
        self.globalFields = config["globalFields"]

    def start(self):
        self.logger.info("Starting Cloudera Manager Metrics Collector daemon.")
        self._get_cluster_periodically(self.config['config']['availableClusterUpdatePeriod'])
        self._get_services_periodically(self.config['config']['availableServicesUpdatePeriod'])
        self._get_available_metrics_periodically(self.config['config']['availableMetricsUpdatePeriod'] )
        self._get_metrics_periodically(self.config['config']['metricsCollectPeriod'] )

    def _get_cluster_periodically(self, period):
        from retry import retry
        retry(self._get_cluster, logger=self.logger, context="Gather cluster details", delay=30)
        timer = Timer(period, self._get_cluster_periodically, args=[period])
        timer.start()

    def _get_services_periodically(self, period):
        try:
            self._get_services()
        except Exception as e:
            self.logger.error("Error occurred during get_services operation: %s" % str(traceback.format_exc()))
        timer = Timer(period, self._get_services_periodically, args=[period])
        timer.start()

    def _get_available_metrics_periodically(self, period):
        try:
            self._get_available_metrics()
        except Exception as e:
            self.logger.error("Error occurred during get_available_metric operation: %s" % str(traceback.format_exc()))
        timer = Timer(period, self._get_available_metrics_periodically, args=[period])
        timer.start()

    def _get_metrics_periodically(self, period):
        try:
            self._get_metrics()
        except Exception as e:
            self.logger.error("Error occurred during get_metrics operation: %s" % str(traceback.format_exc()))
        timer = Timer(period, self._get_metrics_periodically, args=[period])
        timer.start()

    def _get_metrics(self):
        ts_api_instance = cm_client.TimeSeriesResourceApi(self.api_client)
        from_time = datetime.datetime.fromtimestamp(time.time() - 180000)
        to_time = datetime.datetime.fromtimestamp(time.time())
        if self.services:
            for service in self.services:
                metrics_list = self._get_common_service_metrics(service.name)
                if metrics_list:
                    tsquery = self._create_metrics_tsquery(service.name, metrics_list)
                    result = ts_api_instance.query_time_series(_from=from_time, query=tsquery, to=to_time)
                    if result:
                        ts_list = result.items[0]
                        for ts in ts_list.time_series:
                            metric_event = {}
                            metric_event["serviceName"] = service.name
                            metric_event["serviceType"] = service.type
                            metric_event["name"] = ts.metadata.metric_name
                            metric_event["longName"] = service.name + "." + ts.metadata.metric_name
                            for point in ts.data:
                                d = datetime.datetime.strptime(point.timestamp, "%Y-%m-%dT%H:%M:%S.%fZ")
                                unixtime = time.mktime(d.timetuple())
                                metric_event["timestamp"] = unixtime
                                metric_event["value"] = point.value
                            self.metrics_processor.process(self._append_global_fields(metric_event))

    def _create_metrics_tsquery(self, service_name, metrics):
        joined_metrics=', '.join(metrics)
        return "select %s where serviceName=%s and category = \"SERVICE\"" % (joined_metrics, service_name)

    def _get_common_service_metrics(self, service_name):
        common_service_metrics = []
        if service_name in self.serviceMetricsMapFromConfig:
            service_metrics_from_config = self.serviceMetricsMapFromConfig[service_name]
            if service_name in self.availableMetrics:
                available_metrics_for_service = self.availableMetrics[service_name]
                if available_metrics_for_service:
                    for service_metric in service_metrics_from_config:
                         if service_metric in available_metrics_for_service:
                            common_service_metrics.append(service_metric)
        return common_service_metrics

    def _get_cluster(self):
        cluster_api_instance = cm_client.ClustersResourceApi(self.api_client)
        api_response = cluster_api_instance.read_clusters(view='SUMMARY',  _request_timeout = 20.0)
        clusterResponse = None
        for cluster in api_response.items:
            if cluster.cluster_type == self.cluster_type:
                clusterResponse = cluster
        if clusterResponse:
            self.cluster = clusterResponse
        else:
            raise Exception("Not found any clusters yet.")

    def _get_services(self):
        services_api_instance = cm_client.ServicesResourceApi(self.api_client)
        api_response = services_api_instance.read_services(self.cluster.name, view='FULL')
        self.services = api_response.items
        if self.services:
            for service in self.services:
                if service.health_checks:
                    for health_check in service.health_checks:
                        health_check_event = {}
                        health_check_event["serviceName"] = service.name
                        health_check_event["serviceType"] = service.type
                        health_check_event["explanation"] = health_check.explanation
                        health_check_event["name"] = health_check.name
                        health_check_event["value"] = health_check.summary
                        health_check_event["timestamp"] = time.time()
                        self.health_checks_processor.process(self._append_global_fields(health_check_event))

    def _create_service_metrics_map(self):
        services = self.config["services"]
        for service in services:
            name = service["name"]
            metrics = service["metrics"]
            self.serviceMetricsMapFromConfig[name] = metrics

    def _get_available_metrics(self):
       if self.services:
          new_available_metrics_map = {}
          for service in self.services:
              services_api_instance = cm_client.ServicesResourceApi(self.metrics_api_client)
              metrics_api_response = services_api_instance.get_metrics(self.cluster.name, service.name)
              new_metrics_list = []
              for m in metrics_api_response.items:
                 if m.data:
                    new_metrics_list.append(m.name)
              new_available_metrics_map[service.name] = new_metrics_list
          self.availableMetrics = new_available_metrics_map

    def _setup_metrics_processor(self, config):
        name = config["jsonMetricsLogger"]['name']
        path = config["jsonMetricsLogger"]['path']
        max_bytes = config["jsonMetricsLogger"]['max_bytes']
        backup_count = config["jsonMetricsLogger"]['backup_count']
        self.metrics_processor = MetricsLogger(name, path, max_bytes, backup_count)

    def _setup_health_check_metrics_processor(self, config):
        name = config["jsonHealthChecksLogger"]['name']
        path = config["jsonHealthChecksLogger"]['path']
        max_bytes = config["jsonHealthChecksLogger"]['max_bytes']
        backup_count = config["jsonHealthChecksLogger"]['backup_count']
        self.health_checks_processor = MetricsLogger(name, path, max_bytes, backup_count)

    def _setup_termination_handler(self):
        def termination_handler(signum, frame):
            self.logger.info("Termination called.")
            sys.exit(0)
        signal.signal(signal.SIGINT, termination_handler)
        signal.signal(signal.SIGTERM, termination_handler)

    def _setup_logging(self, config):
         fmt="%(asctime)s - %(levelname)s - %(name)s - %(message)s"
         logging.basicConfig(
             level=logging.INFO,
             format=fmt,
         )
         formatter = logging.Formatter(fmt)
         name = "ClouderaManagerMetricsCollector"
         path = config["logger"]['path']
         max_bytes = config["logger"]['max_bytes']
         backup_count = config["logger"]['backup_count']
         handler = RotatingFileHandler(path, maxBytes=max_bytes, backupCount=backup_count, mode='a')
         handler.setLevel(logging.INFO)
         handler.setFormatter(formatter)
         self.logger.addHandler(handler)

    def _append_global_fields(self, obj):
        if self.globalFields:
            for key in self.globalFields:
                obj[key]=self.globalFields[key]
        return obj