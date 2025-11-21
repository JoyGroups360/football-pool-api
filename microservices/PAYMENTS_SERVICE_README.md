# 💳 Payments Service - Guía de Ejecución

## 🚀 Cómo Ejecutar el Payments Service

### Opción 1: Usando el Script (Recomendado)

Desde el directorio raíz de `microservices`:

```bash
./run-payments.sh
```

### Opción 2: Manualmente con Maven

```bash
cd payments_service
mvn spring-boot:run
```

### Opción 3: Desde el Directorio Raíz

```bash
cd payments_service && mvn spring-boot:run
```

---

## ⚠️ IMPORTANTE: Verificar el Directorio

**Asegúrate de estar en el directorio correcto antes de ejecutar:**

```bash
# Verificar que estás en payments_service
pwd
# Debe mostrar: .../microservices/payments_service

# O desde el directorio raíz:
ls payments_service/pom.xml
# Debe existir el archivo
```

---

## 🔧 Configuración

### Puerto
- **Puerto del servicio**: `1293`
- **URL**: `http://localhost:1293`

### Base de Datos MongoDB
- **Base de datos**: `football_pool_payments`
- **URI por defecto**: `mongodb://localhost:27017/football_pool_payments`

### Variables de Entorno

Antes de ejecutar, configura tus claves de Stripe:

```bash
export STRIPE_SECRET_KEY=sk_test_tu_clave_secreta_aqui
export STRIPE_PUBLIC_KEY=pk_test_tu_clave_publica_aqui
export STRIPE_WEBHOOK_SECRET=whsec_tu_webhook_secret_aqui
```

O edita `config_service/src/main/resources/configurations/payments_service.yml`

---

## 📋 Orden de Ejecución de Microservicios

1. **Config Service** (puerto 8888)
2. **Eureka Service** (puerto 8761)
3. **Gateway Service** (puerto 8080)
4. **Auth Service** (puerto 1290)
5. **Competitions Service** (puerto 1291)
6. **Groups Service** (puerto 1292)
7. **Payments Service** (puerto 1293) ← **Este servicio**

---

## ✅ Verificar que el Servicio Está Corriendo

### 1. Verificar el Log
Deberías ver algo como:
```
🚀 Starting Payments Service on port 1293...
...
Started PaymentsServiceApplication in X.XXX seconds
```

### 2. Health Check
```bash
curl http://localhost:1293/football-pool/v1/api/payments/health
```

Respuesta esperada:
```json
{
  "status": "UP",
  "service": "payments_service"
}
```

### 3. Verificar en Eureka
Visita: `http://localhost:8761`
Deberías ver `PAYMENTS_SERVICE` registrado.

---

## 🐛 Solución de Problemas

### Error: "Port 1293 was already in use"

**Solución:**
```bash
# Encontrar el proceso que usa el puerto
lsof -i :1293

# Matar el proceso
kill -9 <PID>
```

### Error: "Port 1292 was already in use" (ejecutando payments_service)

**Problema**: Estás ejecutando desde el directorio incorrecto (probablemente `groups_service`).

**Solución:**
```bash
# Verificar dónde estás
pwd

# Si estás en groups_service, sal y ejecuta el script correcto
cd ..
./run-payments.sh
```

### Error: "Application context failed to start"

**Verificar:**
1. MongoDB está corriendo: `mongosh` o `mongo`
2. Eureka está corriendo: `http://localhost:8761`
3. Config Service está corriendo: `http://localhost:8888`
4. Las variables de entorno de Stripe están configuradas

---

## 📝 Notas

- El servicio usa **Stripe** para procesar pagos
- Requiere claves de API de Stripe (test o production)
- Se integra con `groups_service` para actualizar estados de pago
- Todos los endpoints requieren autenticación JWT (excepto `/health`)

---

## 🔗 Endpoints Principales

- `POST /football-pool/v1/api/payments/intent` - Crear PaymentIntent
- `GET /football-pool/v1/api/payments/{paymentId}` - Obtener pago
- `GET /football-pool/v1/api/payments/user` - Pagos del usuario
- `POST /football-pool/v1/api/payments/{paymentIntentId}/confirm` - Confirmar pago
- `POST /football-pool/v1/api/payments/refund` - Crear reembolso
- `GET /football-pool/v1/api/payments/health` - Health check

---

¿Problemas? Verifica que estás ejecutando desde el directorio correcto: `payments_service/`

