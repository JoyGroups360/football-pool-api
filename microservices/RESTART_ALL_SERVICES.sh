#!/bin/bash

# Script para detener e iniciar todos los servicios del proyecto Football Pool

echo "🛑 Deteniendo todos los servicios..."

# Detener servicios por puerto
for port in 8888 8761 8080 1290 1291 1292 1293; do
  pid=$(lsof -ti:$port 2>/dev/null)
  if [ ! -z "$pid" ]; then
    kill -9 $pid 2>/dev/null
    echo "✅ Puerto $port detenido"
  fi
done

# Detener procesos Spring Boot restantes
ps aux | grep -E "spring-boot:run|ServiceApplication" | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null

echo "⏳ Esperando 3 segundos..."
sleep 3

# Verificar que los puertos estén libres
echo "🔍 Verificando puertos..."
for port in 8888 8761 8080 1290 1291 1292 1293; do
  if lsof -ti:$port > /dev/null 2>&1; then
    echo "⚠️ Puerto $port aún en uso"
  else
    echo "✅ Puerto $port libre"
  fi
done

echo ""
echo "🚀 Iniciando servicios en orden..."

# 1. Config Service
echo "1️⃣ Iniciando Config Service (puerto 8888)..."
cd config_service
mvn spring-boot:run > /tmp/config_service.log 2>&1 &
CONFIG_PID=$!
echo "   ✅ Config Service iniciando (PID: $CONFIG_PID)"
sleep 5

# 2. Eureka Service
echo "2️⃣ Iniciando Eureka Service (puerto 8761)..."
cd ../eureka_service
mvn spring-boot:run > /tmp/eureka_service.log 2>&1 &
EUREKA_PID=$!
echo "   ✅ Eureka Service iniciando (PID: $EUREKA_PID)"
sleep 5

# 3. Gateway Service
echo "3️⃣ Iniciando Gateway Service (puerto 8080)..."
cd ../gateway_service
mvn spring-boot:run > /tmp/gateway_service.log 2>&1 &
GATEWAY_PID=$!
echo "   ✅ Gateway Service iniciando (PID: $GATEWAY_PID)"
sleep 8

# 4. Auth Service
echo "4️⃣ Iniciando Auth Service (puerto 1290)..."
cd ../auth_service
mvn spring-boot:run > /tmp/auth_service.log 2>&1 &
AUTH_PID=$!
echo "   ✅ Auth Service iniciando (PID: $AUTH_PID)"
sleep 3

# 5. Competitions Service
echo "5️⃣ Iniciando Competitions Service (puerto 1291)..."
cd ../competitions_service
mvn spring-boot:run > /tmp/competitions_service.log 2>&1 &
COMPETITIONS_PID=$!
echo "   ✅ Competitions Service iniciando (PID: $COMPETITIONS_PID)"
sleep 3

# 6. Groups Service
echo "6️⃣ Iniciando Groups Service (puerto 1292)..."
cd ../groups_service
mvn spring-boot:run > /tmp/groups_service.log 2>&1 &
GROUPS_PID=$!
echo "   ✅ Groups Service iniciando (PID: $GROUPS_PID)"
sleep 3

# 7. Payments Service
echo "7️⃣ Iniciando Payments Service (puerto 1293)..."
cd ../payments_service
mvn spring-boot:run > /tmp/payments_service.log 2>&1 &
PAYMENTS_PID=$!
echo "   ✅ Payments Service iniciando (PID: $PAYMENTS_PID)"

echo ""
echo "⏳ Esperando 15 segundos para que todos los servicios terminen de iniciar..."
sleep 15

echo ""
echo "📊 ESTADO DE SERVICIOS:"
echo "======================"
for port in 8888:Config 8761:Eureka 8080:Gateway 1290:Auth 1291:Competitions 1292:Groups 1293:Payments; do
  IFS=':' read -r p name <<< "$port"
  if lsof -ti:$p > /dev/null 2>&1; then
    echo "✅ $name Service - Puerto $p: CORRIENDO"
  else
    echo "❌ $name Service - Puerto $p: NO CORRIENDO"
    echo "   💡 Ver logs: tail -f /tmp/${name,,}_service.log"
  fi
done

echo ""
echo "📝 PIDs de procesos:"
echo "   Config: $CONFIG_PID"
echo "   Eureka: $EUREKA_PID"
echo "   Gateway: $GATEWAY_PID"
echo "   Auth: $AUTH_PID"
echo "   Competitions: $COMPETITIONS_PID"
echo "   Groups: $GROUPS_PID"
echo "   Payments: $PAYMENTS_PID"
echo ""
echo "💡 Para ver logs:"
echo "   tail -f /tmp/*_service.log"
echo ""
echo "✅ Proceso completado!"


# Script para detener e iniciar todos los servicios del proyecto Football Pool

echo "🛑 Deteniendo todos los servicios..."

# Detener servicios por puerto
for port in 8888 8761 8080 1290 1291 1292 1293; do
  pid=$(lsof -ti:$port 2>/dev/null)
  if [ ! -z "$pid" ]; then
    kill -9 $pid 2>/dev/null
    echo "✅ Puerto $port detenido"
  fi
done

# Detener procesos Spring Boot restantes
ps aux | grep -E "spring-boot:run|ServiceApplication" | grep -v grep | awk '{print $2}' | xargs kill -9 2>/dev/null

echo "⏳ Esperando 3 segundos..."
sleep 3

# Verificar que los puertos estén libres
echo "🔍 Verificando puertos..."
for port in 8888 8761 8080 1290 1291 1292 1293; do
  if lsof -ti:$port > /dev/null 2>&1; then
    echo "⚠️ Puerto $port aún en uso"
  else
    echo "✅ Puerto $port libre"
  fi
done

echo ""
echo "🚀 Iniciando servicios en orden..."

# 1. Config Service
echo "1️⃣ Iniciando Config Service (puerto 8888)..."
cd config_service
mvn spring-boot:run > /tmp/config_service.log 2>&1 &
CONFIG_PID=$!
echo "   ✅ Config Service iniciando (PID: $CONFIG_PID)"
sleep 5

# 2. Eureka Service
echo "2️⃣ Iniciando Eureka Service (puerto 8761)..."
cd ../eureka_service
mvn spring-boot:run > /tmp/eureka_service.log 2>&1 &
EUREKA_PID=$!
echo "   ✅ Eureka Service iniciando (PID: $EUREKA_PID)"
sleep 5

# 3. Gateway Service
echo "3️⃣ Iniciando Gateway Service (puerto 8080)..."
cd ../gateway_service
mvn spring-boot:run > /tmp/gateway_service.log 2>&1 &
GATEWAY_PID=$!
echo "   ✅ Gateway Service iniciando (PID: $GATEWAY_PID)"
sleep 8

# 4. Auth Service
echo "4️⃣ Iniciando Auth Service (puerto 1290)..."
cd ../auth_service
mvn spring-boot:run > /tmp/auth_service.log 2>&1 &
AUTH_PID=$!
echo "   ✅ Auth Service iniciando (PID: $AUTH_PID)"
sleep 3

# 5. Competitions Service
echo "5️⃣ Iniciando Competitions Service (puerto 1291)..."
cd ../competitions_service
mvn spring-boot:run > /tmp/competitions_service.log 2>&1 &
COMPETITIONS_PID=$!
echo "   ✅ Competitions Service iniciando (PID: $COMPETITIONS_PID)"
sleep 3

# 6. Groups Service
echo "6️⃣ Iniciando Groups Service (puerto 1292)..."
cd ../groups_service
mvn spring-boot:run > /tmp/groups_service.log 2>&1 &
GROUPS_PID=$!
echo "   ✅ Groups Service iniciando (PID: $GROUPS_PID)"
sleep 3

# 7. Payments Service
echo "7️⃣ Iniciando Payments Service (puerto 1293)..."
cd ../payments_service
mvn spring-boot:run > /tmp/payments_service.log 2>&1 &
PAYMENTS_PID=$!
echo "   ✅ Payments Service iniciando (PID: $PAYMENTS_PID)"

echo ""
echo "⏳ Esperando 15 segundos para que todos los servicios terminen de iniciar..."
sleep 15

echo ""
echo "📊 ESTADO DE SERVICIOS:"
echo "======================"
for port in 8888:Config 8761:Eureka 8080:Gateway 1290:Auth 1291:Competitions 1292:Groups 1293:Payments; do
  IFS=':' read -r p name <<< "$port"
  if lsof -ti:$p > /dev/null 2>&1; then
    echo "✅ $name Service - Puerto $p: CORRIENDO"
  else
    echo "❌ $name Service - Puerto $p: NO CORRIENDO"
    echo "   💡 Ver logs: tail -f /tmp/${name,,}_service.log"
  fi
done

echo ""
echo "📝 PIDs de procesos:"
echo "   Config: $CONFIG_PID"
echo "   Eureka: $EUREKA_PID"
echo "   Gateway: $GATEWAY_PID"
echo "   Auth: $AUTH_PID"
echo "   Competitions: $COMPETITIONS_PID"
echo "   Groups: $GROUPS_PID"
echo "   Payments: $PAYMENTS_PID"
echo ""
echo "💡 Para ver logs:"
echo "   tail -f /tmp/*_service.log"
echo ""
echo "✅ Proceso completado!"


