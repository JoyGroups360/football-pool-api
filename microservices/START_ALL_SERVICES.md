# INSTRUCCIONES PARA INICIAR TODOS LOS SERVICIOS

## PROBLEMA IDENTIFICADO

El frontend está recibiendo errores "Network Error" porque **el Gateway Service no está corriendo**. El gateway es el punto de entrada para todas las peticiones del frontend.

## ORDEN DE INICIO DE SERVICIOS

Los servicios deben iniciarse en este orden específico:

### 1. **Config Service** (Puerto 8888)
```bash
cd config_service
mvn spring-boot:run
```
**O usando VS Code:**
- Abre Run and Debug (⌘⇧D)
- Selecciona "Run Config Service"
- Presiona F5

### 2. **Eureka Service** (Puerto 8761)
```bash
cd eureka_service
mvn spring-boot:run
```
**O usando VS Code:**
- Abre Run and Debug (⌘⇧D)
- Selecciona "Run Eureka Service"
- Presiona F5

### 3. **Gateway Service** (Puerto 8080) ⚠️ **IMPORTANTE**
```bash
cd gateway_service
mvn spring-boot:run
```
**O usando VS Code:**
- Abre Run and Debug (⌘⇧D)
- Selecciona "Run Gateway Service"
- Presiona F5

**Este servicio es CRÍTICO** - Sin él, el frontend no puede comunicarse con los microservicios.

### 4. **Microservicios de Negocio** (en cualquier orden)

#### Auth Service (Puerto 1290)
```bash
cd auth_service
mvn spring-boot:run
```

#### Competitions Service (Puerto 1291)
```bash
cd competitions_service
mvn spring-boot:run
```

#### Groups Service (Puerto 1292)
```bash
cd groups_service
mvn spring-boot:run
```

#### Payments Service (Puerto 1293)
```bash
cd payments_service
mvn spring-boot:run
```

## VERIFICACIÓN DE SERVICIOS

### Verificar que todos los servicios están corriendo:

```bash
# Verificar puertos en uso
lsof -ti:8888 && echo "✅ Config Service"
lsof -ti:8761 && echo "✅ Eureka Service"
lsof -ti:8080 && echo "✅ Gateway Service"
lsof -ti:1290 && echo "✅ Auth Service"
lsof -ti:1291 && echo "✅ Competitions Service"
lsof -ti:1292 && echo "✅ Groups Service"
lsof -ti:1293 && echo "✅ Payments Service"
```

### Verificar que el Gateway responde:

```bash
curl http://localhost:8080/actuator/health
```

### Verificar que los servicios están registrados en Eureka:

Abre en el navegador: `http://localhost:8761`

## SCRIPT DE INICIO RÁPIDO

Puedes crear un script para iniciar todos los servicios. Ejemplo:

```bash
#!/bin/bash

echo "🚀 Iniciando todos los servicios..."

# Config Service
cd config_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Config Service iniciado"

# Eureka Service
cd ../eureka_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Eureka Service iniciado"

# Esperar a que los servicios de infraestructura inicien
sleep 10

# Gateway Service
cd ../gateway_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Gateway Service iniciado"

# Esperar a que el gateway inicie
sleep 5

# Microservicios
cd ../auth_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Auth Service iniciado"

cd ../competitions_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Competitions Service iniciado"

cd ../groups_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Groups Service iniciado"

cd ../payments_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Payments Service iniciado"

echo "✅ Todos los servicios iniciados"
```

## SOLUCIÓN A "NETWORK ERROR"

Si el frontend está recibiendo "Network Error":

1. ✅ Verifica que el **Gateway Service** está corriendo en el puerto 8080
2. ✅ Verifica que el **Config Service** está corriendo (Gateway lo necesita)
3. ✅ Verifica que el **Eureka Service** está corriendo (Gateway lo necesita)
4. ✅ Verifica que el frontend está usando la URL correcta: `http://localhost:8080` (no los puertos directos de los servicios)
5. ✅ Espera 10-15 segundos después de iniciar el Gateway para que termine de inicializar

---

**Última actualización:** 2025-11-28


## PROBLEMA IDENTIFICADO

El frontend está recibiendo errores "Network Error" porque **el Gateway Service no está corriendo**. El gateway es el punto de entrada para todas las peticiones del frontend.

## ORDEN DE INICIO DE SERVICIOS

Los servicios deben iniciarse en este orden específico:

### 1. **Config Service** (Puerto 8888)
```bash
cd config_service
mvn spring-boot:run
```
**O usando VS Code:**
- Abre Run and Debug (⌘⇧D)
- Selecciona "Run Config Service"
- Presiona F5

### 2. **Eureka Service** (Puerto 8761)
```bash
cd eureka_service
mvn spring-boot:run
```
**O usando VS Code:**
- Abre Run and Debug (⌘⇧D)
- Selecciona "Run Eureka Service"
- Presiona F5

### 3. **Gateway Service** (Puerto 8080) ⚠️ **IMPORTANTE**
```bash
cd gateway_service
mvn spring-boot:run
```
**O usando VS Code:**
- Abre Run and Debug (⌘⇧D)
- Selecciona "Run Gateway Service"
- Presiona F5

**Este servicio es CRÍTICO** - Sin él, el frontend no puede comunicarse con los microservicios.

### 4. **Microservicios de Negocio** (en cualquier orden)

#### Auth Service (Puerto 1290)
```bash
cd auth_service
mvn spring-boot:run
```

#### Competitions Service (Puerto 1291)
```bash
cd competitions_service
mvn spring-boot:run
```

#### Groups Service (Puerto 1292)
```bash
cd groups_service
mvn spring-boot:run
```

#### Payments Service (Puerto 1293)
```bash
cd payments_service
mvn spring-boot:run
```

## VERIFICACIÓN DE SERVICIOS

### Verificar que todos los servicios están corriendo:

```bash
# Verificar puertos en uso
lsof -ti:8888 && echo "✅ Config Service"
lsof -ti:8761 && echo "✅ Eureka Service"
lsof -ti:8080 && echo "✅ Gateway Service"
lsof -ti:1290 && echo "✅ Auth Service"
lsof -ti:1291 && echo "✅ Competitions Service"
lsof -ti:1292 && echo "✅ Groups Service"
lsof -ti:1293 && echo "✅ Payments Service"
```

### Verificar que el Gateway responde:

```bash
curl http://localhost:8080/actuator/health
```

### Verificar que los servicios están registrados en Eureka:

Abre en el navegador: `http://localhost:8761`

## SCRIPT DE INICIO RÁPIDO

Puedes crear un script para iniciar todos los servicios. Ejemplo:

```bash
#!/bin/bash

echo "🚀 Iniciando todos los servicios..."

# Config Service
cd config_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Config Service iniciado"

# Eureka Service
cd ../eureka_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Eureka Service iniciado"

# Esperar a que los servicios de infraestructura inicien
sleep 10

# Gateway Service
cd ../gateway_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Gateway Service iniciado"

# Esperar a que el gateway inicie
sleep 5

# Microservicios
cd ../auth_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Auth Service iniciado"

cd ../competitions_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Competitions Service iniciado"

cd ../groups_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Groups Service iniciado"

cd ../payments_service && mvn spring-boot:run > /dev/null 2>&1 &
echo "✅ Payments Service iniciado"

echo "✅ Todos los servicios iniciados"
```

## SOLUCIÓN A "NETWORK ERROR"

Si el frontend está recibiendo "Network Error":

1. ✅ Verifica que el **Gateway Service** está corriendo en el puerto 8080
2. ✅ Verifica que el **Config Service** está corriendo (Gateway lo necesita)
3. ✅ Verifica que el **Eureka Service** está corriendo (Gateway lo necesita)
4. ✅ Verifica que el frontend está usando la URL correcta: `http://localhost:8080` (no los puertos directos de los servicios)
5. ✅ Espera 10-15 segundos después de iniciar el Gateway para que termine de inicializar

---

**Última actualización:** 2025-11-28


