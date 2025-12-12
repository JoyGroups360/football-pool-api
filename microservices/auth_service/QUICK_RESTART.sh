#!/bin/bash

# Script para reiniciar el auth_service

echo "🔄 Reiniciando auth_service..."

# Detener proceso existente en puerto 8080
PID_8080=$(lsof -ti:8080 2>/dev/null)
if [ ! -z "$PID_8080" ]; then
    echo "⚠️ Deteniendo proceso en puerto 8080 (PID: $PID_8080)..."
    kill -9 $PID_8080
    sleep 2
fi

# Detener proceso existente en puerto 1290
PID_1290=$(lsof -ti:1290 2>/dev/null)
if [ ! -z "$PID_1290" ]; then
    echo "⚠️ Deteniendo proceso en puerto 1290 (PID: $PID_1290)..."
    kill -9 $PID_1290
    sleep 2
fi

# Cambiar al directorio del servicio
cd "$(dirname "$0")"

# Compilar
echo "📦 Compilando auth_service..."
mvn clean install -DskipTests -q

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
    echo "🚀 Iniciando auth_service..."
    mvn spring-boot:run > /dev/null 2>&1 &
    NEW_PID=$!
    echo "✅ Auth service iniciado (PID: $NEW_PID)"
    echo "⏳ Esperando a que el servicio inicie..."
    sleep 5
    
    # Verificar que esté corriendo
    if lsof -ti:8080 > /dev/null 2>&1 || lsof -ti:1290 > /dev/null 2>&1; then
        echo "✅ Auth service está corriendo correctamente"
    else
        echo "⚠️ El servicio podría estar aún iniciando. Verifica los logs."
    fi
else
    echo "❌ Error en la compilación"
    exit 1
fi


# Script para reiniciar el auth_service

echo "🔄 Reiniciando auth_service..."

# Detener proceso existente en puerto 8080
PID_8080=$(lsof -ti:8080 2>/dev/null)
if [ ! -z "$PID_8080" ]; then
    echo "⚠️ Deteniendo proceso en puerto 8080 (PID: $PID_8080)..."
    kill -9 $PID_8080
    sleep 2
fi

# Detener proceso existente en puerto 1290
PID_1290=$(lsof -ti:1290 2>/dev/null)
if [ ! -z "$PID_1290" ]; then
    echo "⚠️ Deteniendo proceso en puerto 1290 (PID: $PID_1290)..."
    kill -9 $PID_1290
    sleep 2
fi

# Cambiar al directorio del servicio
cd "$(dirname "$0")"

# Compilar
echo "📦 Compilando auth_service..."
mvn clean install -DskipTests -q

if [ $? -eq 0 ]; then
    echo "✅ Compilación exitosa"
    echo "🚀 Iniciando auth_service..."
    mvn spring-boot:run > /dev/null 2>&1 &
    NEW_PID=$!
    echo "✅ Auth service iniciado (PID: $NEW_PID)"
    echo "⏳ Esperando a que el servicio inicie..."
    sleep 5
    
    # Verificar que esté corriendo
    if lsof -ti:8080 > /dev/null 2>&1 || lsof -ti:1290 > /dev/null 2>&1; then
        echo "✅ Auth service está corriendo correctamente"
    else
        echo "⚠️ El servicio podría estar aún iniciando. Verifica los logs."
    fi
else
    echo "❌ Error en la compilación"
    exit 1
fi


