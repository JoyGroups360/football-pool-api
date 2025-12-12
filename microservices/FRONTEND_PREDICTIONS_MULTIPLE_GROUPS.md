# DOCUMENTACIÓN FRONTEND: PREDICCIONES PARA MÚLTIPLES GRUPOS

## RESUMEN

Una predicción se guarda **UNA SOLA VEZ** y se aplica automáticamente a **TODOS los grupos del mismo torneo** (competition) a los que pertenece el usuario.

## CONCEPTO CLAVE

**Ejemplo práctico:**
- Usuario pertenece a 3 grupos diferentes del "FIFA Club World Cup":
  - Grupo "Los Mejores"
  - Grupo "Amigos del Fútbol"  
  - Grupo "Oficina 2025"

- Usuario hace **UNA predicción**: Flamengo 2 - Chelsea 1 (matchId: "group-d-match-3")

- El sistema **AUTOMÁTICAMENTE**:
  - ✅ Guarda la predicción una sola vez
  - ✅ La aplica a los 3 grupos
  - ✅ Calcula puntos para cada grupo por separado (mismo resultado, mismos puntos)
  - ✅ Actualiza el scoreboard de cada grupo

**Resultado:** La misma predicción aparece en los 3 grupos, y los scoreboards se actualizan independientemente.

---

## ENDPOINT PARA GUARDAR PREDICCIONES

### POST /football-pool/v1/api/auth/{userId}/predictions

### Headers
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

### Body (JSON) - Para Fase de Grupos

```json
{
  "matchId": "group-d-match-3",
  "team1Score": 2,
  "team2Score": 1
}
```

### Body (JSON) - Para Fases de Eliminación (Knockout)

```json
{
  "matchId": "round-of-16-1",
  "team1Score": 1,
  "team2Score": 1,
  "userExtraTime": true,              // Opcional: true si predice tiempo extra
  "userPenalties": true,              // Opcional: true si predice penales
  "userPenaltiesTeam1Score": 4,       // Opcional: marcador de penales para equipo 1
  "userPenaltiesTeam2Score": 3        // Opcional: marcador de penales para equipo 2
}
```

### ⚠️ IMPORTANTE

- ❌ **NO enviar `groupId`** - El backend determinará automáticamente a qué grupos aplicar
- ✅ Solo enviar `matchId` y los scores
- El backend identificará el torneo del partido y buscará todos los grupos del usuario en ese torneo

---

## RESPUESTA DEL BACKEND

### Respuesta exitosa (200)

```json
{
  "message": "Prediction saved successfully",
  "userId": "6908d0864077087454146d5c",
  "matchId": "group-d-match-3",
  "team1Score": 2,
  "team2Score": 1,
  "groupsApplied": [
    {
      "groupId": "6923da8daf258e4c81793b31",
      "groupName": "Los Mejores",
      "pointsCalculated": true,          // true si el partido ya se jugó
      "points": 5,                       // Puntos ganados (si pointsCalculated = true)
      "totalScore": 15                   // Score total del usuario en este grupo
    },
    {
      "groupId": "6923da8daf258e4c81793b32",
      "groupName": "Amigos del Fútbol",
      "pointsCalculated": true,
      "points": 5,
      "totalScore": 20
    },
    {
      "groupId": "6923da8daf258e4c81793b33",
      "groupName": "Oficina 2025",
      "pointsCalculated": false,         // false si el partido aún no se jugó
      "points": 0,
      "totalScore": 10
    }
  ]
}
```

### Errores posibles

- **400**: Campos faltantes (matchId, team1Score, team2Score son requeridos)
- **401**: No autenticado
- **404**: Usuario o partido no encontrado
- **500**: Error del servidor

---

## EJEMPLO DE CÓDIGO EN EL FRONTEND

### Función para guardar una predicción (React/React Native)

```javascript
const savePrediction = async (matchId, team1Score, team2Score, userExtraTime, userPenalties) => {
  try {
    const body = {
      matchId: matchId,
      team1Score: team1Score,
      team2Score: team2Score
    };
    
    // Agregar campos opcionales solo si es fase de eliminación
    if (userExtraTime !== undefined) {
      body.userExtraTime = userExtraTime;
    }
    if (userPenalties !== undefined) {
      body.userPenalties = userPenalties;
    }
    
    const response = await fetch(
      `http://localhost:8080/football-pool/v1/api/auth/${userId}/predictions`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${userToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
      }
    );
    
    const result = await response.json();
    
    if (response.ok) {
      // La predicción se aplicó a múltiples grupos
      result.groupsApplied.forEach(groupResult => {
        if (groupResult.pointsCalculated) {
          console.log(`Grupo "${groupResult.groupName}": ${groupResult.points} puntos (Total: ${groupResult.totalScore})`);
        } else {
          console.log(`Grupo "${groupResult.groupName}": Predicción guardada. Puntos se calcularán cuando el partido se juegue.`);
        }
      });
      
      // Mostrar mensaje al usuario
      const groupsWithPoints = result.groupsApplied.filter(g => g.pointsCalculated);
      if (groupsWithPoints.length > 0) {
        const totalPoints = groupsWithPoints.reduce((sum, g) => sum + g.points, 0);
        alert(`¡Predicción guardada en ${result.groupsApplied.length} grupos!\nGanaste ${totalPoints} puntos en total.`);
      } else {
        alert(`Predicción guardada en ${result.groupsApplied.length} grupos. Los puntos se calcularán cuando el partido se juegue.`);
      }
      
      return result;
    } else {
      throw new Error(result.error || 'Error al guardar la predicción');
    }
  } catch (error) {
    console.error('Error saving prediction:', error);
    throw error;
  }
};

// Usar la función
await savePrediction(
  "group-d-match-3",  // matchId
  2,                  // team1Score (Flamengo)
  1                   // team2Score (Chelsea)
);
// La predicción se aplicará automáticamente a TODOS los grupos del mismo torneo
```

---

## FLUJO COMPLETO DE PREDICCIÓN

### PASO 1: Usuario ve un partido en cualquier grupo
- El usuario está viendo el grupo "Los Mejores"
- Ve el partido: Flamengo vs Chelsea

### PASO 2: Usuario hace la predicción
- Usuario predice: Flamengo 2 - Chelsea 1
- Frontend envía al backend:
  ```json
  {
    "matchId": "group-d-match-3",
    "team1Score": 2,
    "team2Score": 1
  }
  ```
- ❌ **NO envía groupId**

### PASO 3: Backend procesa la predicción
1. Backend identifica el torneo del partido (usando matchId)
2. Backend busca todos los grupos del usuario en ese torneo:
   - "Los Mejores"
   - "Amigos del Fútbol"
   - "Oficina 2025"
3. Backend guarda la predicción **UNA vez**
4. Backend aplica la predicción a los 3 grupos
5. Backend calcula puntos (si el partido ya se jugó)
6. Backend actualiza scoreboard de cada grupo

### PASO 4: Usuario ve la respuesta
- Backend responde con información de los 3 grupos
- Frontend muestra: "Predicción guardada en 3 grupos"
- Usuario puede ver su predicción en cualquiera de los 3 grupos

### PASO 5: Partido se juega (si aún no se jugó)
- Admin actualiza el resultado real del partido
- Backend recalcula puntos automáticamente para TODOS los usuarios
- Backend actualiza scoreboards de TODOS los grupos afectados
- La predicción del usuario ahora tiene puntos en los 3 grupos

---

## OBTENER PREDICCIONES DE UN USUARIO

### Obtener todas las predicciones (de todos los grupos)

**GET** `/football-pool/v1/api/auth/{userId}/predictions`

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Respuesta:**
```json
{
  "predictions": [
    {
      "matchId": "group-d-match-3",
      "team1Score": 2,
      "team2Score": 1,
      "groups": [
        {
          "groupId": "6923da8daf258e4c81793b31",
          "groupName": "Los Mejores",
          "points": 5,
          "predictedDate": "2025-01-15T10:30:00.000Z"
        },
        {
          "groupId": "6923da8daf258e4c81793b32",
          "groupName": "Amigos del Fútbol",
          "points": 5,
          "predictedDate": "2025-01-15T10:30:00.000Z"
        }
      ]
    }
  ]
}
```

### Obtener predicciones filtradas por grupo

**GET** `/football-pool/v1/api/auth/{userId}/predictions?groupId={groupId}`

Esto devuelve solo las predicciones que están en ese grupo específico.

---

## VENTAJAS DE ESTE ENFOQUE

✅ **Facilita al usuario:**
   - Solo necesita hacer la predicción una vez
   - Se aplica automáticamente a todos sus grupos del mismo torneo
   - No necesita repetir la misma predicción múltiples veces

✅ **Consistencia:**
   - La misma predicción en todos los grupos
   - No hay riesgo de predicciones diferentes en grupos del mismo torneo

✅ **Eficiencia:**
   - Menos llamadas al backend
   - Menos almacenamiento (predicción única, referencias múltiples)

✅ **Experiencia de usuario:**
   - El usuario puede hacer la predicción desde cualquier grupo
   - La predicción aparece automáticamente en todos los grupos relevantes

---

## PREGUNTAS FRECUENTES

**Q: ¿Qué pasa si el usuario no pertenece a ningún grupo del torneo?**
A: El backend devolverá un error indicando que no hay grupos donde aplicar la predicción.

**Q: ¿Puedo hacer predicciones diferentes para diferentes grupos del mismo torneo?**
A: No. Una predicción es única por partido y se aplica a todos los grupos del mismo torneo. Si quieres cambiar tu predicción, se actualizará en todos los grupos.

**Q: ¿Qué pasa si hago la misma predicción dos veces?**
A: El backend hará un "upsert" (actualizar si existe, crear si no existe). No se duplicará la predicción.

**Q: ¿Cómo sé en qué grupos se aplicó mi predicción?**
A: La respuesta del backend incluye el campo `groupsApplied` con la lista de grupos donde se aplicó la predicción.

**Q: ¿Los puntos son diferentes en cada grupo?**
A: No, los puntos son los mismos porque es la misma predicción vs los mismos resultados. Sin embargo, el score TOTAL puede ser diferente en cada grupo porque el usuario puede tener otras predicciones diferentes en cada grupo.

**Q: ¿Qué pasa si me uno a un nuevo grupo después de hacer una predicción?**
A: Las predicciones existentes NO se aplican automáticamente a grupos nuevos. Solo las nuevas predicciones se aplican a todos los grupos del torneo.

---

## RESUMEN EJECUTIVO

📌 **QUÉ ENVIAR:**
   - `matchId` (obligatorio)
   - `team1Score` (obligatorio)
   - `team2Score` (obligatorio)
   - `userExtraTime`, `userPenalties`, etc. (opcional, solo para knockout)
   - ❌ **NO enviar `groupId`**

📌 **QUÉ RECIBIR:**
   - `groupsApplied`: Lista de grupos donde se aplicó la predicción
   - `points`: Puntos ganados (si el partido ya se jugó)
   - `totalScore`: Score total del usuario en cada grupo

📌 **VENTAJAS:**
   - Una sola predicción para todos los grupos del mismo torneo
   - Consistencia entre grupos
   - Mejor experiencia de usuario

---

## NOTA PARA DESARROLLADORES

Esta funcionalidad requiere cambios en el backend. El endpoint actual aún requiere `groupId`. 

**Estado del backend:** 
- ⏳ Pendiente de implementación
- El backend debe modificarse para:
  1. Hacer `groupId` opcional en el endpoint
  2. Buscar automáticamente todos los grupos del usuario para el mismo torneo
  3. Aplicar la predicción a todos esos grupos
  4. Retornar información de todos los grupos afectados

Una vez que el backend esté listo, esta documentación describe exactamente cómo debe funcionar.


## RESUMEN

Una predicción se guarda **UNA SOLA VEZ** y se aplica automáticamente a **TODOS los grupos del mismo torneo** (competition) a los que pertenece el usuario.

## CONCEPTO CLAVE

**Ejemplo práctico:**
- Usuario pertenece a 3 grupos diferentes del "FIFA Club World Cup":
  - Grupo "Los Mejores"
  - Grupo "Amigos del Fútbol"  
  - Grupo "Oficina 2025"

- Usuario hace **UNA predicción**: Flamengo 2 - Chelsea 1 (matchId: "group-d-match-3")

- El sistema **AUTOMÁTICAMENTE**:
  - ✅ Guarda la predicción una sola vez
  - ✅ La aplica a los 3 grupos
  - ✅ Calcula puntos para cada grupo por separado (mismo resultado, mismos puntos)
  - ✅ Actualiza el scoreboard de cada grupo

**Resultado:** La misma predicción aparece en los 3 grupos, y los scoreboards se actualizan independientemente.

---

## ENDPOINT PARA GUARDAR PREDICCIONES

### POST /football-pool/v1/api/auth/{userId}/predictions

### Headers
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

### Body (JSON) - Para Fase de Grupos

```json
{
  "matchId": "group-d-match-3",
  "team1Score": 2,
  "team2Score": 1
}
```

### Body (JSON) - Para Fases de Eliminación (Knockout)

```json
{
  "matchId": "round-of-16-1",
  "team1Score": 1,
  "team2Score": 1,
  "userExtraTime": true,              // Opcional: true si predice tiempo extra
  "userPenalties": true,              // Opcional: true si predice penales
  "userPenaltiesTeam1Score": 4,       // Opcional: marcador de penales para equipo 1
  "userPenaltiesTeam2Score": 3        // Opcional: marcador de penales para equipo 2
}
```

### ⚠️ IMPORTANTE

- ❌ **NO enviar `groupId`** - El backend determinará automáticamente a qué grupos aplicar
- ✅ Solo enviar `matchId` y los scores
- El backend identificará el torneo del partido y buscará todos los grupos del usuario en ese torneo

---

## RESPUESTA DEL BACKEND

### Respuesta exitosa (200)

```json
{
  "message": "Prediction saved successfully",
  "userId": "6908d0864077087454146d5c",
  "matchId": "group-d-match-3",
  "team1Score": 2,
  "team2Score": 1,
  "groupsApplied": [
    {
      "groupId": "6923da8daf258e4c81793b31",
      "groupName": "Los Mejores",
      "pointsCalculated": true,          // true si el partido ya se jugó
      "points": 5,                       // Puntos ganados (si pointsCalculated = true)
      "totalScore": 15                   // Score total del usuario en este grupo
    },
    {
      "groupId": "6923da8daf258e4c81793b32",
      "groupName": "Amigos del Fútbol",
      "pointsCalculated": true,
      "points": 5,
      "totalScore": 20
    },
    {
      "groupId": "6923da8daf258e4c81793b33",
      "groupName": "Oficina 2025",
      "pointsCalculated": false,         // false si el partido aún no se jugó
      "points": 0,
      "totalScore": 10
    }
  ]
}
```

### Errores posibles

- **400**: Campos faltantes (matchId, team1Score, team2Score son requeridos)
- **401**: No autenticado
- **404**: Usuario o partido no encontrado
- **500**: Error del servidor

---

## EJEMPLO DE CÓDIGO EN EL FRONTEND

### Función para guardar una predicción (React/React Native)

```javascript
const savePrediction = async (matchId, team1Score, team2Score, userExtraTime, userPenalties) => {
  try {
    const body = {
      matchId: matchId,
      team1Score: team1Score,
      team2Score: team2Score
    };
    
    // Agregar campos opcionales solo si es fase de eliminación
    if (userExtraTime !== undefined) {
      body.userExtraTime = userExtraTime;
    }
    if (userPenalties !== undefined) {
      body.userPenalties = userPenalties;
    }
    
    const response = await fetch(
      `http://localhost:8080/football-pool/v1/api/auth/${userId}/predictions`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${userToken}`,
          'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
      }
    );
    
    const result = await response.json();
    
    if (response.ok) {
      // La predicción se aplicó a múltiples grupos
      result.groupsApplied.forEach(groupResult => {
        if (groupResult.pointsCalculated) {
          console.log(`Grupo "${groupResult.groupName}": ${groupResult.points} puntos (Total: ${groupResult.totalScore})`);
        } else {
          console.log(`Grupo "${groupResult.groupName}": Predicción guardada. Puntos se calcularán cuando el partido se juegue.`);
        }
      });
      
      // Mostrar mensaje al usuario
      const groupsWithPoints = result.groupsApplied.filter(g => g.pointsCalculated);
      if (groupsWithPoints.length > 0) {
        const totalPoints = groupsWithPoints.reduce((sum, g) => sum + g.points, 0);
        alert(`¡Predicción guardada en ${result.groupsApplied.length} grupos!\nGanaste ${totalPoints} puntos en total.`);
      } else {
        alert(`Predicción guardada en ${result.groupsApplied.length} grupos. Los puntos se calcularán cuando el partido se juegue.`);
      }
      
      return result;
    } else {
      throw new Error(result.error || 'Error al guardar la predicción');
    }
  } catch (error) {
    console.error('Error saving prediction:', error);
    throw error;
  }
};

// Usar la función
await savePrediction(
  "group-d-match-3",  // matchId
  2,                  // team1Score (Flamengo)
  1                   // team2Score (Chelsea)
);
// La predicción se aplicará automáticamente a TODOS los grupos del mismo torneo
```

---

## FLUJO COMPLETO DE PREDICCIÓN

### PASO 1: Usuario ve un partido en cualquier grupo
- El usuario está viendo el grupo "Los Mejores"
- Ve el partido: Flamengo vs Chelsea

### PASO 2: Usuario hace la predicción
- Usuario predice: Flamengo 2 - Chelsea 1
- Frontend envía al backend:
  ```json
  {
    "matchId": "group-d-match-3",
    "team1Score": 2,
    "team2Score": 1
  }
  ```
- ❌ **NO envía groupId**

### PASO 3: Backend procesa la predicción
1. Backend identifica el torneo del partido (usando matchId)
2. Backend busca todos los grupos del usuario en ese torneo:
   - "Los Mejores"
   - "Amigos del Fútbol"
   - "Oficina 2025"
3. Backend guarda la predicción **UNA vez**
4. Backend aplica la predicción a los 3 grupos
5. Backend calcula puntos (si el partido ya se jugó)
6. Backend actualiza scoreboard de cada grupo

### PASO 4: Usuario ve la respuesta
- Backend responde con información de los 3 grupos
- Frontend muestra: "Predicción guardada en 3 grupos"
- Usuario puede ver su predicción en cualquiera de los 3 grupos

### PASO 5: Partido se juega (si aún no se jugó)
- Admin actualiza el resultado real del partido
- Backend recalcula puntos automáticamente para TODOS los usuarios
- Backend actualiza scoreboards de TODOS los grupos afectados
- La predicción del usuario ahora tiene puntos en los 3 grupos

---

## OBTENER PREDICCIONES DE UN USUARIO

### Obtener todas las predicciones (de todos los grupos)

**GET** `/football-pool/v1/api/auth/{userId}/predictions`

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Respuesta:**
```json
{
  "predictions": [
    {
      "matchId": "group-d-match-3",
      "team1Score": 2,
      "team2Score": 1,
      "groups": [
        {
          "groupId": "6923da8daf258e4c81793b31",
          "groupName": "Los Mejores",
          "points": 5,
          "predictedDate": "2025-01-15T10:30:00.000Z"
        },
        {
          "groupId": "6923da8daf258e4c81793b32",
          "groupName": "Amigos del Fútbol",
          "points": 5,
          "predictedDate": "2025-01-15T10:30:00.000Z"
        }
      ]
    }
  ]
}
```

### Obtener predicciones filtradas por grupo

**GET** `/football-pool/v1/api/auth/{userId}/predictions?groupId={groupId}`

Esto devuelve solo las predicciones que están en ese grupo específico.

---

## VENTAJAS DE ESTE ENFOQUE

✅ **Facilita al usuario:**
   - Solo necesita hacer la predicción una vez
   - Se aplica automáticamente a todos sus grupos del mismo torneo
   - No necesita repetir la misma predicción múltiples veces

✅ **Consistencia:**
   - La misma predicción en todos los grupos
   - No hay riesgo de predicciones diferentes en grupos del mismo torneo

✅ **Eficiencia:**
   - Menos llamadas al backend
   - Menos almacenamiento (predicción única, referencias múltiples)

✅ **Experiencia de usuario:**
   - El usuario puede hacer la predicción desde cualquier grupo
   - La predicción aparece automáticamente en todos los grupos relevantes

---

## PREGUNTAS FRECUENTES

**Q: ¿Qué pasa si el usuario no pertenece a ningún grupo del torneo?**
A: El backend devolverá un error indicando que no hay grupos donde aplicar la predicción.

**Q: ¿Puedo hacer predicciones diferentes para diferentes grupos del mismo torneo?**
A: No. Una predicción es única por partido y se aplica a todos los grupos del mismo torneo. Si quieres cambiar tu predicción, se actualizará en todos los grupos.

**Q: ¿Qué pasa si hago la misma predicción dos veces?**
A: El backend hará un "upsert" (actualizar si existe, crear si no existe). No se duplicará la predicción.

**Q: ¿Cómo sé en qué grupos se aplicó mi predicción?**
A: La respuesta del backend incluye el campo `groupsApplied` con la lista de grupos donde se aplicó la predicción.

**Q: ¿Los puntos son diferentes en cada grupo?**
A: No, los puntos son los mismos porque es la misma predicción vs los mismos resultados. Sin embargo, el score TOTAL puede ser diferente en cada grupo porque el usuario puede tener otras predicciones diferentes en cada grupo.

**Q: ¿Qué pasa si me uno a un nuevo grupo después de hacer una predicción?**
A: Las predicciones existentes NO se aplican automáticamente a grupos nuevos. Solo las nuevas predicciones se aplican a todos los grupos del torneo.

---

## RESUMEN EJECUTIVO

📌 **QUÉ ENVIAR:**
   - `matchId` (obligatorio)
   - `team1Score` (obligatorio)
   - `team2Score` (obligatorio)
   - `userExtraTime`, `userPenalties`, etc. (opcional, solo para knockout)
   - ❌ **NO enviar `groupId`**

📌 **QUÉ RECIBIR:**
   - `groupsApplied`: Lista de grupos donde se aplicó la predicción
   - `points`: Puntos ganados (si el partido ya se jugó)
   - `totalScore`: Score total del usuario en cada grupo

📌 **VENTAJAS:**
   - Una sola predicción para todos los grupos del mismo torneo
   - Consistencia entre grupos
   - Mejor experiencia de usuario

---

## NOTA PARA DESARROLLADORES

Esta funcionalidad requiere cambios en el backend. El endpoint actual aún requiere `groupId`. 

**Estado del backend:** 
- ⏳ Pendiente de implementación
- El backend debe modificarse para:
  1. Hacer `groupId` opcional en el endpoint
  2. Buscar automáticamente todos los grupos del usuario para el mismo torneo
  3. Aplicar la predicción a todos esos grupos
  4. Retornar información de todos los grupos afectados

Una vez que el backend esté listo, esta documentación describe exactamente cómo debe funcionar.


