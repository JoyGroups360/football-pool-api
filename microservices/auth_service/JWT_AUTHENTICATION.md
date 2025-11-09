# 🔐 JWT Authentication System

## 📋 Resumen

Este microservicio implementa un sistema de autenticación basado en JWT (JSON Web Tokens) con dos tipos de tokens:
- **Access Token**: Para peticiones autenticadas (24 horas)
- **Refresh Token**: Para renovar el access token sin re-login (7 días)

---

## 🔑 Tipos de Tokens

### 1. Access Token
- **Duración**: 24 horas
- **Uso**: Autenticación de peticiones API
- **Tipo**: `access`
- **Claims incluidos**:
  - `userId`: ID del usuario
  - `email`: Email del usuario
  - `tokenType`: "access"
  - `exp`: Fecha de expiración
  - `iat`: Fecha de emisión

### 2. Refresh Token
- **Duración**: 7 días
- **Uso**: Renovar access token
- **Tipo**: `refresh`
- **Claims incluidos**:
  - `userId`: ID del usuario
  - `email`: Email del usuario
  - `tokenType`: "refresh"
  - `exp`: Fecha de expiración
  - `iat`: Fecha de emisión

---

## 🌐 Endpoints

### 📂 Endpoints Públicos (No requieren autenticación)

#### 1. Login
```http
POST /football-pool/v1/api/auth
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

**Respuesta exitosa (200):**
```json
{
  "_id": "68f696e7dc4cf83be1bce269",
  "email": "user@example.com",
  "name": "John",
  "lastName": "Doe",
  "accessToken": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiI2OGY2OTZlN...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOiI2OGY2OTZ...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

#### 2. Get All Users
```http
GET /football-pool/v1/api/auth
```

#### 3. Forgot Password
```http
POST /football-pool/v1/api/auth/forgot-password
Content-Type: application/json

{
  "email": "user@example.com"
}
```

#### 4. Validate Token
```http
POST /football-pool/v1/api/auth/validate-token
Content-Type: application/json

{
  "token": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Respuesta exitosa (200):**
```json
{
  "valid": true,
  "email": "user@example.com",
  "userId": "68f696e7dc4cf83be1bce269",
  "tokenType": "access",
  "expiresAt": 1698345600000
}
```

#### 5. Refresh Token
```http
POST /football-pool/v1/api/auth/refresh-token
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Respuesta exitosa (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

#### 6. Create User
```http
POST /football-pool/v1/api/auth/create
Content-Type: application/json

{
  "email": "newuser@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "name": "Jane",
  "lastName": "Smith"
}
```

---

### 🔒 Endpoints Protegidos (Requieren autenticación)

Para todos estos endpoints, debes incluir el **Access Token** en el header:

```http
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

#### 1. Update User (PATCH)
```http
PATCH /football-pool/v1/api/auth/id?userId=68f696e7dc4cf83be1bce269
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "name": "Updated Name",
  "preferredTeams": ["Barcelona", "Real Madrid"]
}
```

#### 2. Delete User
```http
DELETE /football-pool/v1/api/auth/id?userId=68f696e7dc4cf83be1bce269
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

#### 3. Reset Password
```http
POST /football-pool/v1/api/auth/reset-password
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
Content-Type: application/json

{
  "email": "user@example.com",
  "code": "123456",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

---

## 🔄 Flujo de Autenticación

### 1. Login Inicial

```
Client -> Server: POST /auth (email, password)
Server -> Client: {accessToken, refreshToken, userData}
Client: Guarda ambos tokens (localStorage/AsyncStorage)
```

### 2. Peticiones Autenticadas

```
Client -> Server: GET /protected-endpoint
                  Headers: Authorization: Bearer {accessToken}
Server: Valida token
Server -> Client: Respuesta con datos
```

### 3. Token Expirado - Renovación Automática

```
Client -> Server: GET /protected-endpoint
                  Headers: Authorization: Bearer {expired_accessToken}
Server -> Client: 401 Unauthorized

Client -> Server: POST /refresh-token
                  Body: {refreshToken}
Server -> Client: {newAccessToken}

Client: Actualiza accessToken
Client -> Server: GET /protected-endpoint (reintento)
                  Headers: Authorization: Bearer {newAccessToken}
Server -> Client: Respuesta con datos
```

### 4. Refresh Token Expirado

```
Client -> Server: POST /refresh-token
                  Body: {expired_refreshToken}
Server -> Client: 401 Unauthorized

Client: Elimina tokens
Client: Redirige a login
```

---

## 📱 Implementación en React Native

### Configuración Inicial

```javascript
import AsyncStorage from '@react-native-async-storage/async-storage';

const API_URL = 'http://localhost:8080/football-pool/v1/api';

// Guardar tokens
const saveTokens = async (accessToken, refreshToken) => {
  await AsyncStorage.setItem('accessToken', accessToken);
  await AsyncStorage.setItem('refreshToken', refreshToken);
};

// Obtener tokens
const getAccessToken = async () => {
  return await AsyncStorage.getItem('accessToken');
};

const getRefreshToken = async () => {
  return await AsyncStorage.getItem('refreshToken');
};

// Limpiar tokens
const clearTokens = async () => {
  await AsyncStorage.removeItem('accessToken');
  await AsyncStorage.removeItem('refreshToken');
};
```

### Login

```javascript
const login = async (email, password) => {
  try {
    const response = await fetch(`${API_URL}/auth`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ email, password }),
    });

    if (!response.ok) {
      throw new Error('Login failed');
    }

    const data = await response.json();
    
    // Guardar tokens
    await saveTokens(data.accessToken, data.refreshToken);
    
    return data;
  } catch (error) {
    console.error('Login error:', error);
    throw error;
  }
};
```

### Petición Autenticada con Auto-Refresh

```javascript
const makeAuthenticatedRequest = async (url, options = {}) => {
  let accessToken = await getAccessToken();
  
  // Primera petición
  let response = await fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${accessToken}`,
      'Content-Type': 'application/json',
    },
  });

  // Si el token expiró, intentar refresh
  if (response.status === 401) {
    const refreshed = await refreshAccessToken();
    
    if (refreshed) {
      // Reintentar la petición con el nuevo token
      accessToken = await getAccessToken();
      response = await fetch(url, {
        ...options,
        headers: {
          ...options.headers,
          'Authorization': `Bearer ${accessToken}`,
          'Content-Type': 'application/json',
        },
      });
    } else {
      // Refresh falló, redirigir a login
      throw new Error('Session expired');
    }
  }

  return response;
};
```

### Refresh Token

```javascript
const refreshAccessToken = async () => {
  try {
    const refreshToken = await getRefreshToken();
    
    if (!refreshToken) {
      return false;
    }

    const response = await fetch(`${API_URL}/auth/refresh-token`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ refreshToken }),
    });

    if (response.ok) {
      const data = await response.json();
      await AsyncStorage.setItem('accessToken', data.accessToken);
      return true;
    } else {
      // Refresh token expiró o es inválido
      await clearTokens();
      return false;
    }
  } catch (error) {
    console.error('Refresh token error:', error);
    await clearTokens();
    return false;
  }
};
```

### Logout

```javascript
const logout = async () => {
  await clearTokens();
  // Navegar a pantalla de login
  navigation.navigate('Login');
};
```

### Ejemplo de Uso Completo

```javascript
// Actualizar perfil de usuario
const updateUserProfile = async (userId, updates) => {
  try {
    const response = await makeAuthenticatedRequest(
      `${API_URL}/auth/id?userId=${userId}`,
      {
        method: 'PATCH',
        body: JSON.stringify(updates),
      }
    );

    if (!response.ok) {
      throw new Error('Update failed');
    }

    const data = await response.json();
    return data;
  } catch (error) {
    if (error.message === 'Session expired') {
      // Manejar sesión expirada
      await logout();
    }
    throw error;
  }
};
```

---

## 🛡️ Seguridad

### Headers de Seguridad

El sistema valida automáticamente:
- ✅ Firma del token (HS256)
- ✅ Fecha de expiración
- ✅ Tipo de token (access/refresh)
- ✅ Integridad de los claims

### Respuestas de Error

#### Token inválido o expirado (401)
```json
{
  "error": "Unauthorized",
  "message": "Authentication token is required",
  "status": 401
}
```

#### Token malformado (400)
```json
{
  "error": "Invalid token format"
}
```

---

## ⚙️ Configuración

En `application.yml`:

```yaml
jwt:
  secret: ${JWT_SECRET:mySecretKeyForJWTTokenGeneration123456789}
  expiration: 86400000  # 24 horas en milisegundos
  refresh-expiration: 604800000  # 7 días en milisegundos
```

### Variables de Entorno

```bash
# Opcional: Usa una clave secreta personalizada
export JWT_SECRET=your-super-secret-key-here
```

---

## 🧪 Pruebas con cURL

### 1. Login y obtener tokens
```bash
curl -X POST http://localhost:8080/football-pool/v1/api/auth \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

### 2. Usar access token
```bash
curl -X PATCH "http://localhost:8080/football-pool/v1/api/auth/id?userId=68f696e7dc4cf83be1bce269" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \
  -d '{
    "name": "Updated Name"
  }'
```

### 3. Renovar access token
```bash
curl -X POST http://localhost:8080/football-pool/v1/api/auth/refresh-token \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

### 4. Validar token
```bash
curl -X POST http://localhost:8080/football-pool/v1/api/auth/validate-token \
  -H "Content-Type: application/json" \
  -d '{
    "token": "YOUR_ACCESS_TOKEN"
  }'
```

---

## 📊 Ventajas del Sistema

1. **🔄 Sesión Persistente**: 7 días sin re-login
2. **⚡ Performance**: Access token corto para mayor seguridad
3. **🛡️ Seguridad**: Tokens separados para diferentes propósitos
4. **📱 UX Mejorada**: Renovación automática transparente
5. **🔐 Stateless**: No requiere almacenamiento de sesiones en servidor
6. **🚀 Escalable**: Funciona con múltiples instancias del microservicio

---

## 🔧 Troubleshooting

### Token no válido
- Verifica que el token no esté expirado
- Asegúrate de incluir "Bearer " antes del token
- Revisa que la clave secreta JWT sea la correcta

### 401 en todas las peticiones
- Verifica que el access token sea válido
- Intenta usar el refresh token para obtener un nuevo access token
- Si el refresh token también falló, haz login de nuevo

### Tokens no se guardan en React Native
- Verifica que AsyncStorage esté correctamente instalado
- Asegúrate de usar `await` al guardar/obtener tokens
- Revisa los permisos de la app


