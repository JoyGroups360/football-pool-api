#!/bin/bash

# Script para detener todos los servicios del proyecto Football Pool

echo "🛑 Deteniendo todos los servicios Spring Boot..."

# Detener servicios por puerto
for port in 8888 8761 8080 1290 1291 1292 1293; do
  pid=$(lsof -ti:$port 2>/dev/null)
  if [ ! -z "$pid" ]; then
    kill -9 $pid 2>/dev/null
    echo "✅ Puerto $port detenido (PID: $pid)"
  fi
done

# Detener procesos Spring Boot restantes (excluyendo procesos del IDE)
ps aux | grep -E "spring-boot:run|ServiceApplication" | grep -v grep | grep -v "vscode\|cursor\|redhat\|oracle" | awk '{print $2}' | xargs kill -9 2>/dev/null

echo "⏳ Esperando 2 segundos..."
sleep 2

# Verificar que los puertos estén libres
echo ""
echo "🔍 Verificando puertos..."
for port in 8888:Config 8761:Eureka 8080:Gateway 1290:Auth 1291:Competitions 1292:Groups 1293:Payments; do
  IFS=':' read -r p name <<< "$port"
  if lsof -ti:$p > /dev/null 2>&1; then
    echo "⚠️  $name Service - Puerto $p: AÚN EN USO"
  else
    echo "✅ $name Service - Puerto $p: LIBRE"
  fi
done

echo ""
echo "✅ Todos los servicios han sido detenidos!"




