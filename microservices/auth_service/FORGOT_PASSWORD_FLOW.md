# Flujo de Recuperación de Contraseña (Forgot Password)

## 📋 Descripción General
Sistema completo de recuperación de contraseña mediante código de verificación enviado por email en **2 pasos**.

## 🔄 Flujo Completo (2 Pasos)

### 1️⃣ **PASO 1: Solicitar Código de Reseteo**

**Endpoint:** `POST /football-pool/v1/api/auth/forgot-password`

**Request Body:**
```json
{
  "email": "usuario@example.com"
}
```

**Respuestas:**

✅ **Success (200 OK):**
```json
{
  "success": true
}
```

❌ **Usuario no encontrado (404 NOT FOUND):**
```json
{
  "error": "User not found"
}
```

❌ **Email vacío (400 BAD REQUEST):**
```json
{
  "error": "Email is required"
}
```

❌ **Error al enviar email (500 INTERNAL SERVER ERROR):**
```json
{
  "error": "Failed to send email: [detalle del error]"
}
```

**Proceso:**
1. Verifica que el email existe en la BD
2. Genera un código numérico de 6 dígitos
3. Guarda el código en la colección `password_reset_codes` con timestamp
4. Envía el código al email del usuario vía Brevo
5. El código expira en 30 minutos

---

### 2️⃣ **PASO 2: Verificar Código y Resetear Contraseña**

**Endpoint:** `POST /football-pool/v1/api/auth/reset-password`

**Request Body:**
```json
{
  "email": "usuario@example.com",
  "code": "123456",
  "newPassword": "nuevaContraseña123",
  "confirmPassword": "nuevaContraseña123"
}
```

**Respuestas:**

✅ **Success (200 OK):**
```json
{
  "success": true
}
```

❌ **Campos faltantes (400 BAD REQUEST):**
```json
{
  "error": "All fields are required"
}
```

❌ **Contraseñas no coinciden (400 BAD REQUEST):**
```json
{
  "error": "Passwords do not match"
}
```

❌ **Contraseña muy corta (400 BAD REQUEST):**
```json
{
  "error": "Password must be at least 6 characters"
}
```

❌ **Usuario no encontrado (404 NOT FOUND):**
```json
{
  "error": "User not found"
}
```

❌ **Código inválido o expirado (401 UNAUTHORIZED):**
```json
{
  "error": "Invalid or expired reset code"
}
```

❌ **Código expirado (401 UNAUTHORIZED):**
```json
{
  "error": "Reset code has expired. Please request a new one."
}
```

❌ **Contraseña repetida (400 BAD REQUEST):**
```json
{
  "error": "You cannot reuse a previous password. Please choose a different password."
}
```

**Proceso:**
1. Valida todos los campos requeridos
2. Verifica que las contraseñas coincidan
3. Valida longitud mínima de contraseña (6 caracteres)
4. Verifica que el usuario existe
5. Busca el código en la BD para ese email
6. Verifica que el código no haya expirado (30 minutos)
7. **Verifica que la nueva contraseña no esté en el historial** (últimas 5 contraseñas)
8. Actualiza la contraseña del usuario (agrega al historial)
9. Elimina el código usado de la BD

---

## 🗄️ Estructura de Base de Datos

### Colección: `password_reset_codes`
```javascript
{
  "_id": ObjectId,
  "email": "usuario@example.com",
  "code": "123456",
  "createdAt": 1698765432000  // timestamp en milisegundos
}
```

### Colección: `users`
```javascript
{
  "_id": ObjectId,
  "email": "usuario@example.com",
  "passwords": ["nuevaContraseña", "antiguaContraseña1", ...],  // máximo 5
  "name": "Juan",
  "lastName": "Pérez",
  // ... otros campos
}
```

---

## ⏰ Sistema de Limpieza Automática

El sistema incluye un **Scheduled Task** que se ejecuta cada 30 minutos para eliminar códigos expirados.

**Clase:** `ScheduledTasks.java`
```java
@Scheduled(fixedRate = 1800000) // 30 minutos
public void cleanExpiredResetCodes()
```

---

## 📧 Configuración de Email

**Servicio:** Brevo (anteriormente SendinBlue)

**Variables a configurar en `MailService.java`:**
- `apiKey`: Tu API key de Brevo
- `apiUrl`: https://api.brevo.com/v3/smtp/email

**Email enviado:**
```
From: noreply@football-pool.test (Football Pool)
Subject: Verification code
Body: Your temporary code is: [código]. Expires in 30 minutes.
```

---

## 🔒 Seguridad

1. ✅ **Código aleatorio seguro:** Usa `RandomStringUtils.secure().nextNumeric(6)`
2. ✅ **Expiración:** Los códigos expiran automáticamente en 30 minutos
3. ✅ **Un código por email:** Al solicitar un nuevo código, se elimina el anterior
4. ✅ **Eliminación después de uso:** El código se elimina después de usarse exitosamente
5. ✅ **Limpieza automática:** Scheduled task elimina códigos expirados periódicamente
6. ✅ **Validación de email:** Case-insensitive regex search
7. ✅ **Historial de contraseñas:** Mantiene las últimas 5 contraseñas
8. ✅ **Validación de contraseñas repetidas:** No permite reutilizar contraseñas anteriores

---

## 🧪 Ejemplo de Uso Completo

### Paso 1: Solicitar código
```bash
curl -X POST http://localhost:8081/football-pool/v1/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{"email": "usuario@example.com"}'

# Respuesta: {"success": true}
```

### Paso 2: Usuario recibe email con código de 6 dígitos (ej: 123456)

### Paso 3: Verificar código y cambiar contraseña (todo en uno)
```bash
curl -X POST http://localhost:8081/football-pool/v1/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "usuario@example.com",
    "code": "123456",
    "newPassword": "miNuevaContraseña123",
    "confirmPassword": "miNuevaContraseña123"
  }'

# Respuesta: {"success": true}
```

---

## 📝 Notas Importantes

1. **Brevo API Key:** Asegúrate de configurar tu API key real en `MailService.java`
2. **MongoDB:** La colección `password_reset_codes` se crea automáticamente
3. **Testing:** Para testing, puedes consultar directamente la BD para obtener el código
4. **Rate Limiting:** Considera agregar rate limiting para prevenir abuso
5. **Email Template:** Puedes mejorar el template del email con HTML

---

## 🚀 Deployment Checklist

- [ ] Configurar Brevo API key real
- [ ] Configurar email sender verificado en Brevo
- [ ] Crear índice en MongoDB para `password_reset_codes.createdAt` (TTL)
- [ ] Considerar agregar rate limiting
- [ ] Configurar email templates HTML
- [ ] Agregar logging de intentos de reset
- [ ] Implementar notificación al usuario cuando se cambia contraseña

