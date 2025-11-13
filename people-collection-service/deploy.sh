#!/usr/bin/env bash

./gradlew clean build

cp people-ejb/build/libs/people-ejb.jar $WILDFLY_HOME/standalone/deployments/
cp people-web/build/libs/people-web.war $WILDFLY_HOME/standalone/deployments/

$WILDFLY_HOME/bin/standalone.sh
