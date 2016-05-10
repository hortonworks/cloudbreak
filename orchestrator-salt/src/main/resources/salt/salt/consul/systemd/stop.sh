#!/bin/bash

set -x

kill -9 $(ps -o pid= -C consul)