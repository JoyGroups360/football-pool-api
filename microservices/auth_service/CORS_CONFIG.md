# 🌐 CORS Configuration

## 📋 Resumen

CORS (Cross-Origin Resource Sharing) está configurado para permitir peticiones desde:
- **React Native Expo** (puertos 19000-19002, 8081)
- **Localhost** (cualquier puerto)
- **Red local** (192.168.x.x, 10.x.x.x)

---

## ✅ Orígenes Permitidos

| Origen | Descripción |
|--------|-------------|
| `http://localhost:*` | Localhost cualquier puerto |
| `http://127.0.0.1:*` | 127.0.0.1 cualquier puerto |
| `http://192.168.*.*:*` | Red local Wi-Fi |
| `http://10.*.*.*:*` | Red local alternativa |
| `exp://*` | Expo development |
| `http://*:19000` | Expo Metro Bundler default |
| `http://*:19001` | Expo Metro Bundler alternate |
| `http://*:19002` | Expo Metro Bundler alternate |
| `http://*:8081` | React Native Metro Bundler |

---

## 🔧 Métodos HTTP Permitidos

- `GET`
- `POST`
- `PUT`
- `PATCH`
- `DELETE`
- `OPTIONS` (preflight)
- `HEAD`

---

## 📤 Headers Permitidos

El cliente puede enviar estos headers:

- `Authorization` (para JWT)
- `Content-Type`
- `Accept`
- `Origin`
- `Access-Control-Request-Method`
- `Access-Control-Request-Headers`
- `X-Requested-With`

---

## 📥 Headers Expuestos

El servidor expone estos headers al cliente:

- `Authorization`
- `Content-Type`
- `Access-Control-Allow-Origin`
- `Access-Control-Allow-Credentials`

---

## 📱 Configuración en React Native Expo

### 1. Obtener la IP local del servidor

```bash
# En Mac/Linux
ifconfig | grep "inet " | grep -v 127.0.0.1

# En Windows
ipconfig | findstr IPv4
```

**Ejemplo de salida**: `192.168.1.100`

### 2. Configurar la URL base en tu app

```javascript
// config/api.js
const getApiUrl = () => {
  if (__DEV__) {
    // Desarrollo: usa tu IP local
    return 'http://192.168.1.100:1290/football-pool/v1/api';
  } else {
    // Producción: usa el dominio real
    return 'https://api.football-pool.com/v1/api';
  }
};

export const API_URL = getApiUrl();
```

### 3. Hacer peticiones

```javascript
// services/auth.service.js
import { API_URL } from '../config/api';

export const login = async (email, password) => {
  const response = await fetch(`${API_URL}/auth`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({ email, password })
  });
  
  return await response.json();
};
```

---

## 🧪 Probar CORS

### Desde el navegador (Chrome DevTools Console)

```javascript
fetch('http://localhost:1290/football-pool/v1/api/auth', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    email: 'user@example.com',
    password: 'password123'
  })
})
.then(res => res.json())
.then(data => console.log(data))
.catch(err => console.error('CORS Error:', err));
```

### Desde React Native

```javascript
// App.js o componente de login
const testCORS = async () => {
  try {
    const response = await fetch('http://192.168.1.100:1290/football-pool/v1/api/auth', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email: 'user@example.com',
        password: 'password123'
      })
    });
    
    const data = await response.json();
    console.log('✅ CORS works!', data);
  } catch (error) {
    console.error('❌ CORS Error:', error);
  }
};
```

---

## ⚠️ Errores Comunes

### Error: "CORS policy: No 'Access-Control-Allow-Origin' header"

**Causa**: El servidor no está configurado correctamente o el origen no está en la lista permitida.

**Solución**:
1. Verifica que el servicio esté corriendo: `lsof -i:1290`
2. Verifica tu IP local: `ifconfig | grep "inet "`
3. Asegúrate de usar la IP correcta en tu app

### Error: "Network request failed"

**Causa**: No se puede conectar al servidor.

**Solución**:
1. Verifica que estés en la misma red Wi-Fi
2. Verifica que el firewall no bloquee el puerto 1290
3. En Mac: `System Preferences > Security & Privacy > Firewall`
4. Prueba con: `curl http://192.168.1.100:1290/football-pool/v1/api/auth`

---

## 🔒 Producción

Para producción, deberías restringir los orígenes permitidos:

```java
// CorsConfig.java (producción)
configuration.setAllowedOriginPatterns(Arrays.asList(
    "https://tu-dominio.com",
    "https://www.tu-dominio.com",
    "https://app.tu-dominio.com"
));
```

---

## 📝 Configuración Actual

**Archivo**: `auth_service/src/main/java/com/leon/ideas/auth/configs/CorsConfig.java`

**Estado**: ✅ Configurado para desarrollo (React Native Expo)

**Credenciales**: ✅ Habilitado (necesario para JWT en Authorization header)

**Cache de preflight**: ⏱️ 1 hora (3600 segundos)

---

## 🧰 Comandos Útiles

### Ver procesos en puerto 1290
```bash
lsof -i:1290
```

### Matar proceso en puerto 1290
```bash
lsof -ti:1290 | xargs kill -9
```

### Iniciar servicio
```bash
cd auth_service && mvn spring-boot:run
```

### Ver IP local
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

### Probar desde terminal
```bash
curl -X POST http://192.168.1.100:1290/football-pool/v1/api/auth \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"123456"}'
```

---

## ✅ Checklist para React Native

- [ ] Obtener IP local del servidor
- [ ] Configurar `API_URL` con la IP local
- [ ] Verificar que el servidor esté corriendo
- [ ] Verificar que estés en la misma red Wi-Fi
- [ ] Probar petición de login desde la app
- [ ] Verificar headers `Content-Type` y `Authorization`
- [ ] Manejar errores de red adecuadamente

---

## 📚 Referencias

- [MDN: CORS](https://developer.mozilla.org/en-US/docs/Web/HTTP/CORS)
- [Spring CORS](https://spring.io/guides/gs/rest-service-cors/)
- [Expo Network](https://docs.expo.dev/guides/using-network/)


