# 📮 Postman Guide - Football Pool API

## 🌐 Base URLs

```
Gateway (Recomendado):  http://localhost:8080/football-pool/v1/api
Directo al Auth:        http://localhost:1290/football-pool/v1/api
```

---

## 🔓 1. LOGIN (Obtener Tokens)

### Request
- **Method**: `POST`
- **URL**: `http://localhost:8080/football-pool/v1/api/auth`
- **Headers**:
  - `Content-Type`: `application/json`
- **Body** (raw JSON):
```json
{
  "email": "joleogon174@gmail.com",
  "password": "Joelito1990!"
}
```

### Response (200 OK)
```json
{
  "_id": "68f696e7dc4cf83be1bce269",
  "email": "joleogon174@gmail.com",
  "name": "Joel Actualizado",
  "lastName": "Leon",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

**⚠️ IMPORTANTE**: Copia el `accessToken` para usarlo en las siguientes peticiones.

---

## 🔒 2. ACTUALIZAR USUARIO (PATCH) - Requiere JWT

### Request
- **Method**: `PATCH`
- **URL**: `http://localhost:8080/football-pool/v1/api/auth/id?userId=68f696e7dc4cf83be1bce269`
- **Headers**:
  - `Content-Type`: `application/json`
  - `Authorization`: `Bearer TU_ACCESS_TOKEN_AQUI` ⬅️ **Pega el token del login**
- **Body** (raw JSON):
```json
{
  "name": "Joel Updated"
}
```

### Response (200 OK)
```json
{
  "_id": "68f696e7dc4cf83be1bce269",
  "email": "joleogon174@gmail.com",
  "name": "Joel Updated",
  "lastName": "Leon",
  ...
}
```

---

## 🗑️ 3. ELIMINAR USUARIO (DELETE) - Requiere JWT

### Request
- **Method**: `DELETE`
- **URL**: `http://localhost:8080/football-pool/v1/api/auth/id?userId=68f8520309ab8a5031e38242`
- **Headers**:
  - `Authorization`: `Bearer TU_ACCESS_TOKEN_AQUI`

### Response (200 OK)
```json
{
  "message": "User deleted successfully"
}
```

---

## 🔄 4. REFRESH TOKEN

### Request
- **Method**: `POST`
- **URL**: `http://localhost:8080/football-pool/v1/api/auth/refresh-token`
- **Headers**:
  - `Content-Type`: `application/json`
- **Body** (raw JSON):
```json
{
  "refreshToken": "TU_REFRESH_TOKEN_AQUI"
}
```

### Response (200 OK)
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

---

## ✅ 5. VALIDAR TOKEN

### Request
- **Method**: `POST`
- **URL**: `http://localhost:8080/football-pool/v1/api/auth/validate-token`
- **Headers**:
  - `Content-Type`: `application/json`
- **Body** (raw JSON):
```json
{
  "token": "TU_ACCESS_TOKEN_AQUI"
}
```

### Response (200 OK)
```json
{
  "valid": true,
  "email": "joleogon174@gmail.com",
  "userId": "68f696e7dc4cf83be1bce269",
  "tokenType": "access",
  "expiresAt": 1761272749000
}
```

---

## 📋 6. OBTENER TODOS LOS USUARIOS

### Request
- **Method**: `GET`
- **URL**: `http://localhost:8080/football-pool/v1/api/auth`
- **Headers**: (ninguno requerido)

### Response (200 OK)
```json
{
  "users": [
    {
      "_id": "68f696e7dc4cf83be1bce269",
      "email": "joleogon174@gmail.com",
      "name": "Joel Actualizado",
      "lastName": "Leon"
    }
  ]
}
```

---

## ➕ 7. CREAR USUARIO

### Request
- **Method**: `POST`
- **URL**: `http://localhost:8080/football-pool/v1/api/auth/create`
- **Headers**:
  - `Content-Type`: `application/json`
- **Body** (raw JSON):
```json
{
  "email": "newuser@example.com",
  "password": "Password123!",
  "confirmPassword": "Password123!",
  "name": "New",
  "lastName": "User",
  "birth": "1990-01-01",
  "preferredTeams": ["Barcelona"],
  "preferredLeagues": ["LaLiga"]
}
```

### Response (201 Created)
```json
{
  "_id": "68f8520309ab8a5031e38242",
  "email": "newuser@example.com",
  "name": "New",
  "lastName": "User"
}
```

---

## 🔑 8. FORGOT PASSWORD (Enviar Código)

### Request
- **Method**: `POST`
- **URL**: `http://localhost:8080/football-pool/v1/api/auth/forgot-password`
- **Headers**:
  - `Content-Type`: `application/json`
- **Body** (raw JSON):
```json
{
  "email": "joleogon174@gmail.com"
}
```

### Response (200 OK)
```json
{
  "success": true
}
```

**Nota**: El código aparecerá en los logs del servidor.

---

## 🔐 9. RESET PASSWORD (Con Código)

### Request
- **Method**: `POST`
- **URL**: `http://localhost:8080/football-pool/v1/api/auth/reset-password`
- **Headers**:
  - `Content-Type`: `application/json`
- **Body** (raw JSON):
```json
{
  "email": "joleogon174@gmail.com",
  "code": "517474",
  "newPassword": "NewPassword123!",
  "confirmPassword": "NewPassword123!"
}
```

### Response (200 OK)
```json
{
  "success": true
}
```

---

## 🎯 COLECCIÓN DE POSTMAN

### Crear una Collection

1. **Crear Colección**:
   - Name: `Football Pool API`
   - Base URL: `http://localhost:8080/football-pool/v1/api`

2. **Variables de Colección**:
   - `baseUrl`: `http://localhost:8080/football-pool/v1/api`
   - `accessToken`: (vacío, se llenará después del login)
   - `refreshToken`: (vacío, se llenará después del login)
   - `userId`: `68f696e7dc4cf83be1bce269`

3. **Usar variables**:
   - URL: `{{baseUrl}}/auth`
   - Authorization: `Bearer {{accessToken}}`

---

## 🔧 Configurar Auto-Token en Postman

### Paso 1: Script Post-Response del Login

En la petición de **Login**, ve a la pestaña **Tests** y agrega:

```javascript
// Parse response
const response = pm.response.json();

// Save tokens to collection variables
if (response.accessToken) {
    pm.collectionVariables.set("accessToken", response.accessToken);
    console.log("✅ Access Token guardado");
}

if (response.refreshToken) {
    pm.collectionVariables.set("refreshToken", response.refreshToken);
    console.log("✅ Refresh Token guardado");
}

if (response._id) {
    pm.collectionVariables.set("userId", response._id);
    console.log("✅ User ID guardado");
}
```

### Paso 2: Configurar Authorization en la Collection

1. Click derecho en la colección → **Edit**
2. Pestaña **Authorization**
3. Type: `Bearer Token`
4. Token: `{{accessToken}}`
5. **Save**

Ahora todas las peticiones heredarán el token automáticamente.

---

## 📊 Ejemplos de Errores

### 401 Unauthorized (Sin Token)
```json
{
  "error": "Unauthorized",
  "message": "Authentication token is required",
  "status": 401
}
```

### 403 Forbidden (Token Inválido/Expirado)
```json
{
  "timestamp": "2025-10-23T01:19:04.422+00:00",
  "status": 403,
  "error": "Forbidden"
}
```

### 400 Bad Request
```json
{
  "error": "Passwords do not match"
}
```

### 404 Not Found
```json
{
  "error": "User not found"
}
```

---

## 🚀 Workflow Típico en Postman

### 1️⃣ Login
```
POST /auth
Body: { email, password }
→ Guarda accessToken y refreshToken
```

### 2️⃣ Usar Endpoints Protegidos
```
PATCH /auth/id?userId=xxx
Headers: Authorization: Bearer {{accessToken}}
Body: { name: "Nuevo Nombre" }
```

### 3️⃣ Si el Token Expira
```
POST /auth/refresh-token
Body: { refreshToken: "{{refreshToken}}" }
→ Actualiza accessToken
```

---

## 🔄 URLs Alternativas

### Usar Auth Service Directamente (Puerto 1290)

Cambia `baseUrl` a:
```
http://localhost:1290/football-pool/v1/api
```

**Ventajas**:
- ✅ Más rápido (sin Gateway)
- ✅ Útil para debugging

**Desventajas**:
- ❌ No simula el entorno real
- ❌ Sin balanceo de carga

---

## 📱 Para Testing con IP Local

Si quieres simular desde tu móvil:

1. Obtén tu IP local:
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

2. Cambia `baseUrl` a:
```
http://192.168.X.X:8080/football-pool/v1/api
```

---

## ✅ Checklist

- [ ] Collection creada en Postman
- [ ] Variables configuradas (baseUrl, accessToken, refreshToken)
- [ ] Script de Tests en Login para guardar tokens
- [ ] Authorization configurada a nivel Collection
- [ ] Login exitoso
- [ ] Token guardado automáticamente
- [ ] Endpoints protegidos funcionan con token
- [ ] Refresh token funciona

---

## 🆘 Troubleshooting

### "Error: Could not get response"
- ✅ Verifica que el servicio esté corriendo: `lsof -i:8080`
- ✅ Revisa los logs: `tail -f /tmp/gateway_service.log`

### "401 Unauthorized" en endpoints protegidos
- ✅ Verifica que el token esté en el header `Authorization`
- ✅ Asegúrate de usar `Bearer ` antes del token
- ✅ Verifica que el token no haya expirado (24h)

### "User not found" en PATCH/DELETE
- ✅ Verifica que el `userId` sea correcto
- ✅ Lista usuarios con `GET /auth` para ver IDs válidos

### Token no se guarda automáticamente
- ✅ Verifica que el script esté en la pestaña **Tests** (no Pre-request)
- ✅ Revisa la consola de Postman (View → Show Postman Console)

---

## 📚 Más Información

- `JWT_QUICK_START.md` - Guía rápida de JWT
- `GATEWAY_JWT_CONFIG.md` - Configuración del Gateway
- `CORS_CONFIG.md` - Configuración CORS


