# 🔧 Troubleshooting: Error 401 Unauthorized en `/groups/{groupId}/matches`

## 📋 Problema

El frontend está recibiendo un error **401 Unauthorized** al intentar acceder al endpoint `GET /groups/{groupId}/matches`, aunque el token JWT se está enviando correctamente.

## 🔍 Pasos para Diagnosticar

### 1. Verificar que el Groups Service está corriendo

```bash
# Verificar que el servicio está activo en el puerto 1292
curl http://localhost:1292/actuator/health
```

### 2. Verificar que el Gateway está corriendo

```bash
# Verificar que el Gateway está activo en el puerto 8080
curl http://localhost:8080/actuator/health
```

### 3. Probar el endpoint directamente (sin Gateway)

```bash
# Probar directamente al groups_service (puerto 1292)
curl -X GET \
  "http://localhost:1292/football-pool/v1/api/groups/691a192a53148e413d0e49b0/matches" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

**Si esto funciona:** El problema está en el Gateway, no en el groups_service.

**Si esto NO funciona:** El problema está en el groups_service (JWT validation o SecurityConfig).

### 4. Probar el endpoint a través del Gateway

```bash
# Probar a través del Gateway (puerto 8080)
curl -X GET \
  "http://localhost:8080/football-pool/v1/api/groups/691a192a53148e413d0e49b0/matches" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "Content-Type: application/json"
```

### 5. Verificar los logs del Groups Service

Busca en los logs del `groups_service`:

```
🔐 JWT Filter - Processing request: /football-pool/v1/api/groups/...
⚽ GET /groups/{groupId}/matches - ENDPOINT CALLED
```

**Si NO ves estos logs:** La request no está llegando al groups_service (problema en el Gateway).

**Si ves los logs pero hay errores:** Revisa los errores específicos:
- `❌ JWT Filter - Invalid signature` → JWT secret no coincide
- `❌ JWT Filter - Token expired` → Token expirado
- `❌ JWT Filter - Malformed token` → Token inválido
- `❌ ERROR: userId is NULL!` → JWT filter no estableció la autenticación

### 6. Verificar la configuración del JWT Secret

Asegúrate de que `groups_service` y `auth_service` usan el **mismo** `jwt.secret`:

**En `config_service/src/main/resources/configurations/groups_service.yml`:**
```yaml
jwt:
  secret: YOUR_BASE64_ENCODED_SECRET
  expiration: 86400000
```

**En `config_service/src/main/resources/configurations/auth_service.yml`:**
```yaml
jwt:
  secret: YOUR_BASE64_ENCODED_SECRET  # DEBE SER EL MISMO
  expiration: 86400000
```

### 7. Verificar que el Gateway está enrutando correctamente

**En `config_service/src/main/resources/configurations/gateway_service.yml`:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: groups
          uri: http://localhost:1292
          predicates:
            - Path=/football-pool/v1/api/groups/**
          filters:
            - PreserveHostHeader
```

### 8. Verificar el formato del token

El token debe tener el formato:
```
Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**NO debe ser:**
- `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` (sin "Bearer ")
- `bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` (minúscula)

## 🛠️ Soluciones Comunes

### Solución 1: JWT Secret no coincide

**Síntoma:** Logs muestran `Invalid signature (JWT secret mismatch?)`

**Solución:**
1. Verifica que ambos servicios usan el mismo `jwt.secret` en el config service
2. Reinicia ambos servicios después de cambiar el secret
3. Genera un nuevo token desde `auth_service`

### Solución 2: Token expirado

**Síntoma:** Logs muestran `Token expired`

**Solución:**
1. Haz login nuevamente para obtener un nuevo token
2. Verifica que `jwt.expiration` está configurado correctamente (en milisegundos)

### Solución 3: Gateway no está pasando el header Authorization

**Síntoma:** Logs del groups_service muestran `Authorization header: MISSING`

**Solución:**
1. Verifica que el Gateway está configurado para pasar headers
2. Agrega un filtro en el Gateway para preservar el header Authorization:

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: groups
          uri: http://localhost:1292
          predicates:
            - Path=/football-pool/v1/api/groups/**
          filters:
            - PreserveHostHeader
            - AddRequestHeader=Authorization, ${header.Authorization}
```

### Solución 4: El endpoint no está siendo encontrado

**Síntoma:** No hay logs del endpoint en groups_service

**Solución:**
1. Verifica que el endpoint está correctamente mapeado en `GroupsController`
2. Verifica que no hay conflictos de rutas (por ejemplo, `/groups/{id}` vs `/groups/{groupId}/matches`)
3. Asegúrate de que el orden de los endpoints en el controller es correcto (más específicos primero)

## 📊 Logs Esperados (Cuando Funciona Correctamente)

### En Groups Service:

```
🔐 JWT Filter - Processing request: /football-pool/v1/api/groups/691a192a53148e413d0e49b0/matches
🔐 JWT Filter - Authorization header present: true
🔐 JWT Filter - Token extracted (length: 200)
🔐 JWT Filter - Token valid. Email: user@example.com, UserId: 6908d0864077087454146d5c
✅ JWT Filter - Authentication set successfully
═══════════════════════════════════════════════════════
⚽ GET /groups/{groupId}/matches - ENDPOINT CALLED
═══════════════════════════════════════════════════════
Group ID: 691a192a53148e413d0e49b0
Request URI: /football-pool/v1/api/groups/691a192a53148e413d0e49b0/matches
Request Method: GET
Authorization Header: PRESENT
User ID from request attribute: 6908d0864077087454146d5c
User Email from request attribute: user@example.com
✅ Proceeding to get matches...
```

## 🎯 Próximos Pasos

1. **Ejecuta las pruebas** de arriba para identificar dónde está fallando
2. **Revisa los logs** del groups_service cuando hagas la request
3. **Comparte los logs** si el problema persiste para diagnóstico adicional

## 📝 Notas

- El error `path: "/error"` en la respuesta sugiere que el Gateway está retornando un error genérico
- Si otros endpoints funcionan con el mismo token, el problema es específico de este endpoint
- El JWT filter ahora tiene logging detallado para ayudar a diagnosticar problemas

