# 🌉 API Gateway + JWT Configuration

## ✅ Estado Actual

### Servicios Configurados:
- ✅ **Auth Service** (Puerto 1290) - Con JWT y CORS
- ✅ **Config Service** (Puerto 8888) - Configuración centralizada
- ✅ **Gateway Service** (Puerto 8080) - API Gateway con routing
- ✅ **Eureka Service** (Puerto 8761) - Service Discovery

---

## 🔧 Configuración Completa

### 1. Gateway Service (Puerto 8080)

**Archivo**: `config_service/src/main/resources/configurations/gateway_service.yml`

```yaml
server:
  port: 8080

spring:
  cloud:
    gateway:
      globalcors:
        corsConfigurations:
          "[/**]":
            allowedOriginPatterns:
              - "http://localhost:*"
              - "http://127.0.0.1:*"
              - "http://192.168.*.*:*"
              - "http://10.*.*.*:*"
              - "exp://*"
            allowedMethods:
              - GET
              - POST
              - PUT
              - PATCH
              - DELETE
              - OPTIONS
            allowedHeaders:
              - "*"
            exposedHeaders:
              - Authorization
              - Content-Type
            allowCredentials: true
            maxAge: 3600
      routes:
        - id: auth
          uri: http://localhost:1290
          predicates:
            - Path=/football-pool/v1/api/auth/**
          filters:
            - PreserveHostHeader
```

---

## 🚀 Uso con React Native

### Opción 1: A través del Gateway (Recomendado para producción)

```javascript
// config/api.js
const API_URL = __DEV__ 
  ? 'http://192.168.X.X:8080/football-pool/v1/api'  // Gateway
  : 'https://api.tu-dominio.com/football-pool/v1/api';

export default API_URL;
```

###  Opción 2: Directo al Auth Service (Desarrollo)

```javascript
// config/api.js
const API_URL = 'http://192.168.X.X:1290/football-pool/v1/api';  // Directo
```

---

## 🧪 Testing

### Test Login a través del Gateway (8080)

```bash
curl -X POST http://localhost:8080/football-pool/v1/api/auth \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Respuesta esperada:**
```json
{
  "_id": "68f696e7dc4cf83be1bce269",
  "email": "user@example.com",
  "accessToken": "eyJhbGci...",
  "refreshToken": "eyJhbGci...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

### Test PATCH con JWT a través del Gateway

```bash
curl -X PATCH "http://localhost:8080/football-pool/v1/api/auth/id?userId=68f696e7dc4cf83be1bce269" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer TU_ACCESS_TOKEN" \
  -d '{"name": "Nuevo Nombre"}'
```

### Test Login Directo (Puerto 1290)

```bash
curl -X POST http://localhost:1290/football-pool/v1/api/auth \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

---

## 📋 Endpoints

### A través del Gateway (http://localhost:8080)

| Método | Endpoint | Autenticación | Descripción |
|--------|----------|---------------|-------------|
| POST | `/football-pool/v1/api/auth` | No | Login |
| GET | `/football-pool/v1/api/auth` | No | Get all users |
| POST | `/football-pool/v1/api/auth/create` | No | Crear usuario |
| POST | `/football-pool/v1/api/auth/forgot-password` | No | Forgot password |
| POST | `/football-pool/v1/api/auth/reset-password` | No | Reset password |
| POST | `/football-pool/v1/api/auth/validate-token` | No | Validar token |
| POST | `/football-pool/v1/api/auth/refresh-token` | No | Refresh token |
| PATCH | `/football-pool/v1/api/auth/id?userId=xxx` | **Sí** | Actualizar usuario |
| DELETE | `/football-pool/v1/api/auth/id?userId=xxx` | **Sí** | Eliminar usuario |

---

## 🔄 Flujo Completo

### 1. Login
```
Cliente → Gateway (8080) → Auth Service (1290) → MongoDB
         ← accessToken + refreshToken ←
```

### 2. Petición Protegida
```
Cliente → Gateway (8080) → Auth Service (1290)
         [Authorization: Bearer token]
                       ↓
                  JWT Filter valida token
                       ↓
                  Spring Security autoriza
                       ↓
                  Controller ejecuta
         ← Respuesta ←
```

### 3. Token Expirado - Refresh
```
Cliente → Gateway (8080) → /refresh-token
         [refreshToken]
         ← nuevo accessToken ←
```

---

## ⚙️ Iniciar Servicios

```bash
# 1. Eureka Service (Puerto 8761)
cd eureka_service && mvn spring-boot:run &

# 2. Config Service (Puerto 8888)
cd config_service && mvn spring-boot:run &

# Esperar 10 segundos

# 3. Auth Service (Puerto 1290)
cd auth_service && mvn spring-boot:run &

# 4. Gateway Service (Puerto 8080)
cd gateway_service && mvn spring-boot:run &
```

---

## 🛑 Detener Servicios

```bash
# Detener todos los servicios
lsof -ti:8761 | xargs kill -9  # Eureka
lsof -ti:8888 | xargs kill -9  # Config
lsof -ti:1290 | xargs kill -9  # Auth
lsof -ti:8080 | xargs kill -9  # Gateway
```

---

## 🐛 Troubleshooting

### El Gateway no responde

```bash
# Verificar que el Config Service esté corriendo
curl http://localhost:8888/gateway_service/default

# Verificar logs
tail -f /tmp/gateway_service.log
```

### Headers no se pasan al microservicio

- ✅ Asegúrate de tener `PreserveHostHeader` en el filtro
- ✅ Verifica que `allowedHeaders: "*"` esté configurado
- ✅ Revisa que `allowCredentials: true` esté habilitado

### CORS Errors

- ✅ Verifica tu IP local: `ifconfig | grep "inet "`
- ✅ Asegúrate de usar `http://TU_IP:8080` no `http://localhost:8080`
- ✅ Revisa que el origen esté en `allowedOriginPatterns`

---

## 📚 Documentación Relacionada

- `JWT_QUICK_START.md` - Guía rápida de JWT
- `JWT_AUTHENTICATION.md` - Documentación completa de JWT
- `CORS_CONFIG.md` - Configuración de CORS
- `PATCH_USER_DOCUMENTATION.md` - Documentación de PATCH endpoint

---

## ✅ Checklist de Configuración

- [x] Config Service configurado y corriendo
- [x] Gateway Service configurado con CORS
- [x] Gateway route para auth_service
- [x] PreserveHostHeader configurado
- [x] CORS con allowedOriginPatterns para React Native
- [x] Headers Authorization permitidos
- [x] Auth Service con JWT funcionando
- [x] Login a través del Gateway funciona
- [ ] PATCH/DELETE a través del Gateway (en revisión)

---

## 🎯 Próximos Pasos

1. ✅ Usar Gateway para todas las peticiones
2. ⚠️ Configurar SSL/TLS para producción
3. ⚠️ Agregar rate limiting
4. ⚠️ Implementar circuit breaker
5. ⚠️ Agregar logging centralizado


