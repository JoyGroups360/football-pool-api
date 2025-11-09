# 🔐 Flujo de Recuperación de Contraseña - RESUMEN

## 📱 Flujo de 2 Pasos

```
┌─────────────────────────────────────────────────────────────────┐
│                         PASO 1                                   │
│                   Solicitar Código                               │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
    POST /football-pool/v1/api/auth/forgot-password
    Body: { "email": "usuario@example.com" }
                                │
                                ▼
              ┌─────────────────────────────┐
              │  Backend genera código      │
              │  aleatorio de 6 dígitos     │
              │  (ej: 123456)               │
              └─────────────────────────────┘
                                │
                                ▼
              ┌─────────────────────────────┐
              │  Se envía email con código  │
              │  a través de Brevo          │
              └─────────────────────────────┘
                                │
                                ▼
              ┌─────────────────────────────┐
              │  Respuesta:                 │
              │  { "success": true }        │
              └─────────────────────────────┘


┌─────────────────────────────────────────────────────────────────┐
│                         PASO 2                                   │
│            Verificar Código y Cambiar Contraseña                │
└─────────────────────────────────────────────────────────────────┘
                                │
                                ▼
    POST /football-pool/v1/api/auth/reset-password
    Body: { 
      "email": "usuario@example.com",
      "code": "123456",
      "newPassword": "nuevaPass123",
      "confirmPassword": "nuevaPass123"
    }
                                │
                                ▼
              ┌─────────────────────────────┐
              │  Backend verifica código    │
              │  - Existe en BD?            │
              │  - No ha expirado? (30min)  │
              └─────────────────────────────┘
                                │
                                ▼
              ┌─────────────────────────────┐
              │  Cambia la contraseña       │
              │  Elimina el código usado    │
              └─────────────────────────────┘
                                │
                                ▼
              ┌─────────────────────────────┐
              │  Respuesta:                 │
              │  { "success": true }        │
              └─────────────────────────────┘
```

---

## 🎯 Endpoints

### 1. POST `/football-pool/v1/api/auth/forgot-password`
**Request:**
```json
{
  "email": "usuario@example.com"
}
```
**Response (Success):**
```json
{
  "success": true
}
```

---

### 2. POST `/football-pool/v1/api/auth/reset-password`
**Request:**
```json
{
  "email": "usuario@example.com",
  "code": "123456",
  "newPassword": "nuevaContraseña123",
  "confirmPassword": "nuevaContraseña123"
}
```
**Response (Success):**
```json
{
  "success": true
}
```

---

## ⚙️ Configuración Importante

### En `MailService.java` debes configurar:
```java
private final String apiKey = "TU_BREVO_API_KEY_REAL";
```

---

## 🔒 Características de Seguridad

- ✅ Código de 6 dígitos aleatorio y seguro
- ✅ Expira automáticamente en 30 minutos
- ✅ Se elimina después de usarse
- ✅ Solo un código activo por email
- ✅ Validación de contraseñas (mínimo 6 caracteres)
- ✅ Historial de últimas 5 contraseñas

---

## 📧 Email Enviado

```
From: noreply@football-pool.test
Subject: Verification code
Body: Your temporary code is: 123456. Expires in 30 minutes.
```


