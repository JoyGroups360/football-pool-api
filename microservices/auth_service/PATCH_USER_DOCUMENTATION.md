# 📝 PATCH User Info - Documentación

## 🎯 **Endpoint para actualizar información del usuario**

**Endpoint:** `PATCH /football-pool/v1/api/auth/id`

---

## 📋 **Parámetros**

### **Query Parameter:**
- `userId` (String, requerido): ID del usuario a actualizar

### **Request Body (JSON):**
Cualquier campo del usuario que quieras actualizar (excepto campos protegidos).

---

## ✅ **Campos que SÍ se pueden actualizar:**

```json
{
  "name": "Nuevo Nombre",
  "lastName": "Nuevo Apellido", 
  "birth": "1990-01-01",
  "preferredTeams": ["Barcelona", "Real Madrid", "Liverpool"],
  "preferredLeagues": ["LaLiga", "Premier League", "Champions League"]
}
```

---

## 🚫 **Campos protegidos (NO se pueden actualizar):**

- ❌ `_id` - ID del usuario
- ❌ `passwords` - Array de contraseñas (usa endpoint específico para cambiar contraseña)

---

## 📝 **Ejemplos de uso:**

### **1. Actualizar solo el nombre:**
```json
{
  "name": "Joel Actualizado"
}
```

### **2. Actualizar equipos favoritos:**
```json
{
  "preferredTeams": ["Barcelona", "Manchester City", "Bayern Munich"]
}
```

### **3. Actualizar múltiples campos:**
```json
{
  "name": "Joel",
  "lastName": "Leon Actualizado",
  "birth": "1995-07-15",
  "preferredTeams": ["Barcelona", "Liverpool"],
  "preferredLeagues": ["LaLiga", "Premier League"]
}
```

---

## 📮 **Respuestas:**

### ✅ **Success (200 OK):**
```json
{
  "_id": "68f696e7dc4cf83be1bce269",
  "email": "joleogon174@gmail.com",
  "name": "Joel Actualizado",
  "lastName": "Leon",
  "birth": "1995-07-15",
  "preferredTeams": ["Barcelona", "Liverpool"],
  "preferredLeagues": ["LaLiga", "Premier League"],
  "passwords": ["123456"]
}
```

### ❌ **Usuario no encontrado (404 NOT FOUND):**
```json
{
  "error": "User not found"
}
```

### ❌ **ID faltante (400 BAD REQUEST):**
```json
{
  "error": "User ID is required"
}
```

### ❌ **Sin campos para actualizar (400 BAD REQUEST):**
```json
{
  "error": "No fields to update"
}
```

### ❌ **Solo campos protegidos (400 BAD REQUEST):**
```json
{
  "error": "No valid fields to update"
}
```

---

## 🧪 **Ejemplos en Postman:**

### **URL completa:**
```
PATCH http://localhost:8080/football-pool/v1/api/auth/id?userId=68f696e7dc4cf83be1bce269
```

### **Headers:**
```
Content-Type: application/json
```

### **Body (raw JSON):**
```json
{
  "name": "Joel Actualizado",
  "preferredTeams": ["Barcelona", "Real Madrid"]
}
```

---

## 🔒 **Características de seguridad:**

- ✅ **Validación de ID**: Verifica que el ID sea válido
- ✅ **Validación de campos**: Verifica que hay campos para actualizar
- ✅ **Campos protegidos**: No permite actualizar `_id` o `passwords`
- ✅ **Actualización selectiva**: Solo actualiza los campos enviados
- ✅ **Preserva datos**: Mantiene los campos no enviados intactos

---

## 📊 **Estructura de datos actualizada:**

```javascript
{
  "_id": ObjectId,
  "email": "joleogon174@gmail.com",           // ← No se puede cambiar
  "passwords": ["123456"],                   // ← No se puede cambiar
  "name": "Joel Actualizado",                // ← Actualizable
  "lastName": "Leon",                        // ← Actualizable  
  "birth": "1995-07-15",                     // ← Actualizable
  "preferredTeams": ["Barcelona", "Liverpool"], // ← Actualizable
  "preferredLeagues": ["LaLiga", "Premier League"] // ← Actualizable
}
```

---

## 🎯 **Casos de uso comunes:**

1. **Actualizar perfil personal**: Nombre, apellido, fecha de nacimiento
2. **Cambiar preferencias**: Equipos y ligas favoritas
3. **Actualización parcial**: Solo los campos que necesitas cambiar
4. **Mantenimiento de datos**: Preserva información no enviada

