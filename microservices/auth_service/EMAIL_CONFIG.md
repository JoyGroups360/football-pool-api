# 📧 Configuración de Email

## 🚀 Modo Desarrollo (Actual)

Por defecto, el servicio está en **modo desarrollo**. Los emails **NO se envían realmente**, solo se imprimen en la consola.

### ¿Cómo funciona?

Cuando haces una petición a `/forgot-password`, verás en la consola algo como:

```
========================================
📧 DEV MODE - EMAIL NO ENVIADO
========================================
To: joleogon174@gmail.com
Subject: Verification code
Code: 123456
Message: Your temporary code is: 123456. Expires in 30 minutes.
========================================
⚠️  Para enviar emails reales, configura BREVO_API_KEY
========================================
```

### Ventajas del Modo Desarrollo:
✅ No necesitas configurar nada
✅ No necesitas cuenta de Brevo
✅ Puedes ver el código directamente en la consola
✅ Perfecto para testing

---

## 🔧 Configuración para Producción (Envío Real de Emails)

### Paso 1: Crear cuenta en Brevo

1. Ve a https://www.brevo.com/
2. Crea una cuenta gratuita
3. Ve a **Settings** → **SMTP & API**
4. Crea una nueva **API Key**
5. Copia la API key

### Paso 2: Configurar la API Key

Tienes **3 opciones**:

#### Opción A: Variable de Entorno (Recomendado)
```bash
export BREVO_API_KEY="tu_api_key_real"
export DEV_MODE=false
```

#### Opción B: En `application.yml`
```yaml
mail:
  brevo:
    api-key: "tu_api_key_real"
  dev-mode: false
```

#### Opción C: Al iniciar la aplicación
```bash
java -jar auth_service.jar \
  --mail.brevo.api-key=tu_api_key_real \
  --mail.dev-mode=false
```

### Paso 3: Verificar el sender email en Brevo

En Brevo, verifica el dominio o email sender:
1. Ve a **Senders & IP**
2. Agrega y verifica tu email o dominio
3. Actualiza `sender-email` en `application.yml` si es necesario

---

## 🧪 Testing en Desarrollo

### Para probar AHORA sin configurar nada:

1. **Reinicia** el servicio auth
2. Haz la petición a `/forgot-password`:
   ```bash
   curl -X POST http://localhost:8080/football-pool/v1/api/auth/forgot-password \
     -H "Content-Type: application/json" \
     -d '{"email": "joleogon174@gmail.com"}'
   ```
3. **Mira la consola** del servicio, ahí verás el código
4. Usa ese código en `/reset-password`:
   ```bash
   curl -X POST http://localhost:8080/football-pool/v1/api/auth/reset-password \
     -H "Content-Type: application/json" \
     -d '{
       "email": "joleogon174@gmail.com",
       "code": "EL_CODIGO_QUE_VISTE_EN_CONSOLA",
       "newPassword": "nuevaPass123",
       "confirmPassword": "nuevaPass123"
     }'
   ```

---

## 📝 Configuración Actual (`application.yml`)

```yaml
mail:
  brevo:
    api-key: ${BREVO_API_KEY:DEV_MODE}  # Lee de variable de entorno o usa DEV_MODE
    api-url: https://api.brevo.com/v3/smtp/email
    sender-email: noreply@football-pool.test
    sender-name: Football Pool
  dev-mode: ${DEV_MODE:true}  # true = modo desarrollo, false = producción
```

---

## 🔍 Debugging

### Ver logs del servicio:
```bash
# Si usas Maven
mvn spring-boot:run

# Si usas JAR
java -jar target/auth_service.jar
```

### El código se imprime en la consola cuando `dev-mode: true`

---

## ✅ Checklist para Producción

- [ ] Crear cuenta en Brevo
- [ ] Obtener API Key
- [ ] Verificar sender email en Brevo
- [ ] Configurar `BREVO_API_KEY` como variable de entorno
- [ ] Configurar `DEV_MODE=false`
- [ ] Probar envío de email real
- [ ] Verificar que llegue el email
- [ ] Configurar sender email personalizado (opcional)

---

## 🎯 Resumen

| Modo | dev-mode | api-key | Comportamiento |
|------|----------|---------|----------------|
| Desarrollo | `true` | `DEV_MODE` | Imprime en consola |
| Producción | `false` | Tu API Key real | Envía emails por Brevo |

**Por defecto**: Modo Desarrollo (no necesitas configurar nada para testing)


