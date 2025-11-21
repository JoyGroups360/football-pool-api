# 🔧 Guía para Frontend: Error 401 en `/groups/{groupId}/matches`

## 📋 Problema Actual

El endpoint `GET /groups/{groupId}/matches` está retornando error **401 Unauthorized**, aunque el token JWT se está enviando correctamente en otros endpoints.

---

## ✅ Verificaciones en el Frontend

### 1. Verificar que el Token se Está Enviando

**En el servicio de grupos (`group-service.ts`):**

```typescript
// Verificar que el interceptor de Axios está configurado correctamente
// El header debe ser: "Authorization: Bearer {token}"
```

**Verificar en la consola del navegador/React Native:**
- ¿El log muestra `hasToken: true`?
- ¿El token tiene el formato correcto `Bearer eyJ...`?

### 2. Verificar el Formato del Token

El token debe tener este formato exacto:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**NO debe ser:**
- ❌ `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` (sin "Bearer ")
- ❌ `bearer eyJ...` (minúscula)
- ❌ `BEARER eyJ...` (mayúscula completa)

### 3. Verificar la URL Completa

La URL debe ser:
```
http://localhost:8080/football-pool/v1/api/groups/{groupId}/matches
```

**Verificar:**
- ¿Estás usando el Gateway (puerto 8080) o directamente el servicio (puerto 1292)?
- ¿La URL está correctamente formada?
- ¿El `groupId` es válido?

### 4. Verificar que el Token No Está Expirado

**Síntoma:** Otros endpoints funcionan, pero este específico no.

**Solución temporal:**
- Haz logout y login nuevamente para obtener un token fresco
- Verifica la expiración del token (debe ser 24 horas)

---

## 🔍 Debugging desde el Frontend

### Agregar Logs Detallados

**En `group-service.ts`, antes de hacer la request:**

```typescript
export const getGroupMatches = async (
  id: string,
  filters?: {
    stageId?: string;
    groupLetter?: string;
    status?: string;
  }
) => {
  try {
    const token = await getAuthToken(); // Tu función para obtener el token
    
    console.log('🔍 DEBUG - getGroupMatches');
    console.log('Group ID:', id);
    console.log('Filters:', filters);
    console.log('Token exists:', !!token);
    console.log('Token length:', token?.length);
    console.log('Token starts with Bearer:', token?.startsWith('Bearer '));
    
    const url = `${API_BASE_URL}/groups/${id}/matches`;
    console.log('Full URL:', url);
    
    const response = await apiClient.get(url, {
      params: filters,
      headers: {
        'Authorization': token,
        'Content-Type': 'application/json'
      }
    });
    
    return response.data;
  } catch (error) {
    console.error('❌ Error in getGroupMatches:', error);
    console.error('Error response:', error.response?.data);
    console.error('Error status:', error.response?.status);
    throw error;
  }
};
```

### Verificar la Request Real

**En el interceptor de Axios o en el servicio:**

```typescript
// Agregar un interceptor para ver todas las requests
axios.interceptors.request.use(
  (config) => {
    console.log('🌐 REQUEST:', {
      method: config.method,
      url: config.url,
      headers: config.headers,
      hasAuth: !!config.headers?.Authorization,
      authHeader: config.headers?.Authorization?.substring(0, 20) + '...'
    });
    return config;
  },
  (error) => {
    console.error('❌ REQUEST ERROR:', error);
    return Promise.reject(error);
  }
);

// Interceptor para responses
axios.interceptors.response.use(
  (response) => {
    console.log('✅ RESPONSE:', {
      status: response.status,
      url: response.config.url,
      data: response.data
    });
    return response;
  },
  (error) => {
    console.error('❌ RESPONSE ERROR:', {
      status: error.response?.status,
      url: error.config?.url,
      data: error.response?.data,
      headers: error.response?.headers
    });
    return Promise.reject(error);
  }
);
```

---

## 🛠️ Soluciones Temporales desde el Frontend

### Solución 1: Fallback - Obtener Matches del Grupo Completo

Si el endpoint específico falla, puedes obtener los matches del objeto `Group` completo:

```typescript
export const getGroupMatches = async (
  id: string,
  filters?: {
    stageId?: string;
    groupLetter?: string;
    status?: string;
  }
) => {
  try {
    // Intentar obtener matches directamente
    const response = await apiClient.get(`/groups/${id}/matches`, {
      params: filters
    });
    return response.data;
  } catch (error) {
    if (error.response?.status === 401) {
      console.warn('⚠️ 401 Error on /matches endpoint, using fallback');
      
      // Fallback: obtener grupo completo y extraer matches
      const groupResponse = await apiClient.get(`/groups/${id}`);
      const group = groupResponse.data.group;
      
      // Extraer matches del tournamentStructure
      const matches = extractMatchesFromGroup(group, filters);
      
      return {
        groupId: id,
        matches: matches,
        count: matches.length,
        filters: filters || {},
        source: 'fallback' // Indicar que viene del fallback
      };
    }
    throw error;
  }
};

function extractMatchesFromGroup(
  group: any,
  filters?: {
    stageId?: string;
    groupLetter?: string;
    status?: string;
  }
): any[] {
  const allMatches: any[] = [];
  
  if (!group.tournamentStructure?.stages) {
    return [];
  }
  
  // Iterar por todas las etapas
  for (const stage of Object.values(group.tournamentStructure.stages)) {
    const stageData = stage as any;
    
    // Filtrar por stageId si se especifica
    if (filters?.stageId && stageData.stageId !== filters.stageId) {
      continue;
    }
    
    // Matches de fase de grupos
    if (stageData.groups) {
      for (const groupStage of stageData.groups) {
        // Filtrar por groupLetter si se especifica
        if (filters?.groupLetter && groupStage.groupLetter !== filters.groupLetter) {
          continue;
        }
        
        if (groupStage.matches) {
          for (const match of groupStage.matches) {
            // Filtrar por status si se especifica
            if (filters?.status && match.status !== filters.status) {
              continue;
            }
            allMatches.push(match);
          }
        }
      }
    }
    
    // Matches de eliminatorias
    if (stageData.matches) {
      for (const match of stageData.matches) {
        // Filtrar por status si se especifica
        if (filters?.status && match.status !== filters.status) {
          continue;
        }
        allMatches.push(match);
      }
    }
  }
  
  return allMatches;
}
```

### Solución 2: Verificar Token Antes de Hacer Request

```typescript
export const getGroupMatches = async (
  id: string,
  filters?: {
    stageId?: string;
    groupLetter?: string;
    status?: string;
  }
) => {
  // Verificar token antes de hacer la request
  const token = await getAuthToken();
  
  if (!token) {
    throw new Error('No authentication token available. Please login.');
  }
  
  if (!token.startsWith('Bearer ')) {
    console.warn('⚠️ Token format incorrect, fixing...');
    const fixedToken = token.startsWith('Bearer ') ? token : `Bearer ${token}`;
    // Actualizar token en storage
    await saveAuthToken(fixedToken);
  }
  
  // Continuar con la request...
};
```

---

## 📊 Información que Necesitas del Backend

Para diagnosticar completamente, el backend necesita:

1. **Logs del Groups Service:**
   - ¿Aparece el log `🔐 JWT Filter - Processing request`?
   - ¿Aparece el log `⚽ GET /groups/{groupId}/matches - ENDPOINT CALLED`?
   - ¿Hay algún error con `❌`?

2. **Si NO aparecen logs:**
   - El problema está en el Gateway (no está enrutando correctamente)
   - La request no está llegando al groups_service

3. **Si aparecen logs pero hay errores:**
   - `Invalid signature` → JWT secret no coincide
   - `Token expired` → Token expirado
   - `userId is NULL` → JWT filter no estableció la autenticación

---

## 🎯 Checklist de Verificación

Antes de reportar el problema al backend, verifica:

- [ ] El token se está obteniendo correctamente de AsyncStorage
- [ ] El token tiene el formato `Bearer {token}`
- [ ] El token no está expirado (haz login nuevamente)
- [ ] La URL está correcta: `/groups/{groupId}/matches`
- [ ] El `groupId` es válido (existe en la base de datos)
- [ ] Otros endpoints del mismo servicio funcionan (ej: `GET /groups`)
- [ ] Los logs del frontend muestran que el token se está enviando
- [ ] La request se está haciendo al Gateway (puerto 8080), no directamente al servicio

---

## 🔄 Flujo de Debugging Recomendado

1. **Agregar logs detallados** en el servicio de grupos
2. **Verificar token** antes de hacer la request
3. **Implementar fallback** temporal (obtener matches del grupo completo)
4. **Compartir logs** con el backend para diagnóstico
5. **Probar directamente** al servicio (puerto 1292) para ver si el problema es del Gateway

---

## 📝 Notas Importantes

- El error `path: "/error"` en la respuesta sugiere que el Gateway está retornando un error genérico
- Si otros endpoints funcionan con el mismo token, el problema es específico de este endpoint
- El backend ha agregado logging detallado para ayudar a diagnosticar
- La solución de fallback es temporal hasta que se resuelva el problema de autenticación

---

## 🚀 Próximos Pasos

1. Implementa el fallback temporal para que la app funcione
2. Agrega los logs detallados para debugging
3. Prueba hacer la request directamente al servicio (puerto 1292) para ver si el problema es del Gateway
4. Comparte los logs con el backend para diagnóstico completo

