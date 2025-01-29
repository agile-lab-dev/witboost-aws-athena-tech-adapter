#!/bin/bash

exec java -javaagent:opentelemetry-javaagent.jar -jar athena-tech-adapter.jar
