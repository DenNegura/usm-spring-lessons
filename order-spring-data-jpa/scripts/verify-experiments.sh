#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
mvn -q -DskipTests package
for experiment in repository-basic repository-save save-assigned-id derived-query jpql-query fetch-basic lazy-outside-context n-plus-one join-fetch entity-graph transactional-service transactional-rollback dto-projection optimistic-locking; do
  log="/tmp/order-spring-data-jpa.${experiment}.log"
  java -jar target/order-spring-data-jpa-1.0-SNAPSHOT.jar "--app.demo.data-experiment=$experiment" > "$log" 2>&1
  rg -F "=== PASS: $experiment ===" "$log"
done
