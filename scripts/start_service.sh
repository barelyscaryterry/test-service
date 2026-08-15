#!/bin/bash
set -euxo pipefail

systemctl daemon-reload
systemctl start test-service-test
