#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
log_dir=$(mktemp -d /tmp/order-relationships.XXXXXX)
for experiment in many-to-one owning-side cascade-persist orphan-removal one-to-one many-to-many embeddable; do
    log_file="$log_dir/$experiment.log"
    mvn spring-boot:run "-Dspring-boot.run.arguments=--app.demo.relationship-experiment=$experiment" > "$log_file" 2>&1
    rg -q "=== PASS: $experiment ===" "$log_file"
    echo "$experiment PASS: $log_file"
done
