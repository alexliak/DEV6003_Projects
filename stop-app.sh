#!/bin/bash

echo "Stopping Hospital Management System..."

# Find and kill processes on port 8443
echo "Checking for processes on port 8443..."
PID=$(sudo lsof -ti:8443)
if [ ! -z "$PID" ]; then
    echo "Found process $PID on port 8443, killing it..."
    sudo kill -9 $PID
    echo "Process killed."
else
    echo "No process found on port 8443"
fi

# Also check port 8080
echo "Checking for processes on port 8080..."
PID=$(sudo lsof -ti:8080)
if [ ! -z "$PID" ]; then
    echo "Found process $PID on port 8080, killing it..."
    sudo kill -9 $PID
    echo "Process killed."
else
    echo "No process found on port 8080"
fi

# Kill any remaining Spring Boot processes
echo "Checking for Spring Boot processes..."
pkill -f "spring-boot:run"
pkill -f "com.nyc.hosp.HospApplication"

echo "Application stopped."
