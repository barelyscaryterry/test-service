#!/bin/bash
set -euxo pipefail

systemctl stop test-service-test || true
