#!/bin/bash

echo "🚀 Starting Groups Service on port 1292..."
cd "$(dirname "$0")/groups_service"
mvn spring-boot:run



