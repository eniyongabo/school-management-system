#!/bin/sh
# Isolated browser-test server. This profile is never used for deployment.
set -eu
SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$SCRIPT_DIR/.."
if [ -n "${SCHOOL_MAVEN_REPO:-}" ]; then
  set -- "-Dmaven.repo.local=$SCHOOL_MAVEN_REPO"
else
  set --
fi
exec mvn -B -ntp -f backend/pom.xml "$@" spring-boot:test-run \
  -Dspring-boot.run.profiles=test \
  '-Dspring-boot.run.arguments=--server.address=127.0.0.1 --server.port=18080 --spring.datasource.url=jdbc:h2:mem:browser-school;MODE=MySQL;DB_CLOSE_DELAY=-1 --spring.datasource.username=sa --spring.datasource.password= --spring.datasource.driver-class-name=org.h2.Driver --app.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA= --app.bootstrap.email=browser-admin@example.test --app.bootstrap.password=Browser-Test-Password-42 --app.cors.allowed-origin=http://127.0.0.1:15173 --management.health.mail.enabled=false'
