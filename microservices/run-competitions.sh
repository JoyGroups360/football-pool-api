#!/bin/bash

echo "🚀 Starting Competitions Service on port 1291..."
cd "$(dirname "$0")/competitions_service"
mvn spring-boot:run



