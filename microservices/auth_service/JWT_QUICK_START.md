# 🚀 JWT Quick Start Guide

## 🎯 Resumen Rápido

Este microservicio usa **JWT (JSON Web Tokens)** con dos tipos de tokens:
- **Access Token**: 24 horas (para peticiones API)
- **Refresh Token**: 7 días (para renovar sin re-login)

---

## 🔑 Obtener Tokens (Login)

```bash
curl -X POST http://localhost:1290/football-pool/v1/api/auth \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Respuesta:**
```json
{
  "_id": "68f696e7dc4cf83be1bce269",
  "email": "user@example.com",
  "name": "John",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

---

## 🔄 Renovar Access Token

Cuando el access token expire (24h), usa el refresh token:

```bash
curl -X POST http://localhost:1290/football-pool/v1/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
  }'
```

**Respuesta:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

---

## 🛡️ Usar Token en Peticiones Protegidas

Agrega el header `Authorization` con el access token:

```bash
curl -X PATCH "http://localhost:1290/football-pool/v1/api/auth/id?userId=123" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..." \
  -d '{
    "name": "Updated Name"
  }'
```

---

## 📱 Endpoints Públicos (No requieren token)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| POST | `/football-pool/v1/api/auth` | Login |
| GET | `/football-pool/v1/api/auth` | Get all users |
| POST | `/football-pool/v1/api/auth/create` | Crear usuario |
| POST | `/football-pool/v1/api/auth/forgot-password` | Enviar código reset |
| POST | `/football-pool/v1/api/auth/reset-password` | Resetear contraseña |
| POST | `/football-pool/v1/api/auth/validate-token` | Validar token |
| POST | `/football-pool/v1/api/auth/refresh-token` | Renovar access token |

---

## 🔒 Endpoints Protegidos (Requieren token)

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| PATCH | `/football-pool/v1/api/auth/id` | Actualizar usuario |
| DELETE | `/football-pool/v1/api/auth/id` | Eliminar usuario |

---

## 📱 React Native Implementation

### Setup
```javascript
import AsyncStorage from '@react-native-async-storage/async-storage';

const API_URL = 'http://your-server:1290/football-pool/v1/api';
```

### Login
```javascript
const login = async (email, password) => {
  const response = await fetch(`${API_URL}/auth`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  
  const data = await response.json();
  
  // Guardar tokens
  await AsyncStorage.setItem('accessToken', data.accessToken);
  await AsyncStorage.setItem('refreshToken', data.refreshToken);
  
  return data;
};
```

### Peticiones Autenticadas
```javascript
const makeAuthRequest = async (url, options = {}) => {
  const accessToken = await AsyncStorage.getItem('accessToken');
  
  const response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json'
    }
  });
  
  // Si token expiró, intentar refresh
  if (response.status === 401 || response.status === 403) {
    const refreshed = await refreshToken();
    if (refreshed) {
      // Reintentar con nuevo token
      return makeAuthRequest(url, options);
    }
  }
  
  return response;
};
```

### Refresh Token
```javascript
const refreshToken = async () => {
  const refreshToken = await AsyncStorage.getItem('refreshToken');
  
  const response = await fetch(`${API_URL}/auth/refresh-token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken })
  });
  
  if (response.ok) {
    const data = await response.json();
    await AsyncStorage.setItem('accessToken', data.accessToken);
    return true;
  }
  
  // Refresh token expiró, hacer logout
  await logout();
  return false;
};
```

### Logout
```javascript
const logout = async () => {
  await AsyncStorage.removeItem('accessToken');
  await AsyncStorage.removeItem('refreshToken');
  navigation.navigate('Login');
};
```

---

## 🧪 Testing con Postman

### 1. Login
- **Method**: POST
- **URL**: `http://localhost:1290/football-pool/v1/api/auth`
- **Body** (JSON):
  ```json
  {
    "email": "user@example.com",
    "password": "password123"
  }
  ```
- **Save**: Copiar `accessToken` y `refreshToken` de la respuesta

### 2. Usar en Peticiones Protegidas
- **Method**: PATCH, DELETE, etc.
- **URL**: Endpoint protegido
- **Headers**:
  - `Content-Type`: `application/json`
  - `Authorization`: `Bearer {accessToken}`

### 3. Renovar Token
- **Method**: POST
- **URL**: `http://localhost:1290/football-pool/v1/api/auth/refresh-token`
- **Body** (JSON):
  ```json
  {
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }
  ```

---

## ⚠️ Errores Comunes

### 401 Unauthorized
```json
{
  "error": "Unauthorized",
  "message": "Authentication token is required",
  "status": 401
}
```
**Solución**: Agregar header `Authorization: Bearer {token}`

### 403 Forbidden
```json
{
  "timestamp": "2025-10-23T01:19:04.422+00:00",
  "status": 403,
  "error": "Forbidden"
}
```
**Solución**: El token es inválido o expiró, usar refresh token

---

## ⚙️ Configuración

En `application.yml`:
```yaml
jwt:
  secret: ${JWT_SECRET:base64_encoded_secret_here}
  expiration: 86400000  # 24 horas
  refresh-expiration: 604800000  # 7 días
```

**Variable de entorno (opcional)**:
```bash
export JWT_SECRET=your-custom-secret-key-base64
```

---

## 📚 Documentación Completa

Ver `JWT_AUTHENTICATION.md` para documentación detallada con más ejemplos y casos de uso.

---

## ✅ Status

| Feature | Status |
|---------|--------|
| Login con JWT | ✅ |
| Access Token (24h) | ✅ |
| Refresh Token (7 días) | ✅ |
| Endpoints Protegidos | ✅ |
| Renovación Automática | ✅ |
| React Native Ready | ✅ |


