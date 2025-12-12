# DOCUMENTACIÓN FRONTEND: PREDICCIONES PARA MÚLTIPLES GRUPOS - IMPLEMENTACIÓN COMPLETA

## ✅ ESTADO: IMPLEMENTADO Y LISTO PARA USAR

El backend ha sido actualizado para soportar predicciones que se aplican automáticamente a todos los grupos del mismo torneo.

---

## RESUMEN

Una predicción se guarda **UNA SOLA VEZ** y se aplica automáticamente a **TODOS los grupos del mismo torneo** (competition) a los que pertenece el usuario.

**Ejemplo:**
- Usuario pertenece a 3 grupos del "FIFA Club World Cup"
- Usuario hace UNA predicción: Flamengo 2 - Chelsea 1
- El sistema AUTOMÁTICAMENTE aplica la predicción a los 3 grupos
- Los puntos se calculan una vez (mismo resultado = mismos puntos)
- El scoreboard de cada grupo se actualiza independientemente

---

## ENDPOINT: GUARDAR PREDICCIÓN

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
  "userExtraTime": true,
  "userPenalties": true,
  "userPenaltiesTeam1Score": 4,
  "userPenaltiesTeam2Score": 3
}
```

### ⚠️ IMPORTANTE

- ❌ **NO enviar `groupId`** - El backend determinará automáticamente todos los grupos del mismo torneo
- ✅ Solo enviar `matchId` y los scores
- ✅ El backend buscará automáticamente todos los grupos del usuario que pertenecen al mismo torneo del partido

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
      "pointsCalculated": true,
      "points": 5,
      "totalScore": 15
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
      "pointsCalculated": false,
      "points": 0,
      "totalScore": 10
    }
  ]
}
```

### Campos de la respuesta

- `message`: Mensaje de confirmación
- `userId`: ID del usuario
- `matchId`: ID del partido
- `team1Score`, `team2Score`: Scores de la predicción
- `groupsApplied`: Array con información de cada grupo donde se aplicó la predicción
  - `groupId`: ID del grupo
  - `groupName`: Nombre del grupo
  - `pointsCalculated`: `true` si el partido ya se jugó y se calcularon puntos
  - `points`: Puntos ganados en este grupo (0 si el partido no se jugó aún)
  - `totalScore`: Score total del usuario en este grupo

### Errores posibles

- **400 Bad Request**: 
  - `"All fields are required: userId, matchId, team1Score, team2Score"`
  - `"User does not belong to any groups"`
  - `"User does not belong to any groups for this match's competition"`
  
- **401 Unauthorized**: No autenticado

- **404 Not Found**: 
  - `"Group not found: {groupId}"` (si se proporcionó groupId)
  - `"Match not found in any of user's groups: {matchId}"` (si no se proporcionó groupId)

- **500 Internal Server Error**: Error del servidor

---

## EJEMPLO DE CÓDIGO EN EL FRONTEND

### Función para guardar una predicción (React/React Native)

```javascript
const savePrediction = async (matchId, team1Score, team2Score, userExtraTime, userPenalties) => {
  try {
    const body = {
      matchId: matchId,
      team1Score: team1Score || 0,  // Manejar inputs vacíos como 0
      team2Score: team2Score || 0   // Manejar inputs vacíos como 0
    };
    
    // Agregar campos opcionales solo si es fase de eliminación
    if (userExtraTime !== undefined && userExtraTime !== null) {
      body.userExtraTime = userExtraTime;
    }
    if (userPenalties !== undefined && userPenalties !== null) {
      body.userPenalties = userPenalties;
    }
    if (userPenaltiesTeam1Score !== undefined && userPenaltiesTeam1Score !== null) {
      body.userPenaltiesTeam1Score = userPenaltiesTeam1Score;
    }
    if (userPenaltiesTeam2Score !== undefined && userPenaltiesTeam2Score !== null) {
      body.userPenaltiesTeam2Score = userPenaltiesTeam2Score;
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
      const groupsWithPoints = result.groupsApplied.filter(g => g.pointsCalculated);
      const groupsWithoutPoints = result.groupsApplied.filter(g => !g.pointsCalculated);
      
      // Mostrar información de cada grupo
      result.groupsApplied.forEach(groupResult => {
        if (groupResult.pointsCalculated) {
          console.log(`✅ Grupo "${groupResult.groupName}": ${groupResult.points} puntos (Total: ${groupResult.totalScore})`);
        } else {
          console.log(`⏳ Grupo "${groupResult.groupName}": Predicción guardada. Puntos se calcularán cuando el partido se juegue.`);
        }
      });
      
      // Mostrar mensaje al usuario
      if (groupsWithPoints.length > 0) {
        const totalPoints = groupsWithPoints.reduce((sum, g) => sum + g.points, 0);
        alert(`¡Predicción guardada en ${result.groupsApplied.length} grupos!\n\nGanaste ${totalPoints} puntos en total.\n\n${groupsWithPoints.length} grupos con puntos calculados\n${groupsWithoutPoints.length} grupos esperando resultados`);
      } else {
        alert(`Predicción guardada en ${result.groupsApplied.length} grupos.\n\nLos puntos se calcularán automáticamente cuando el partido se juegue.`);
      }
      
      // Refrescar grupos si se calcularon puntos
      if (groupsWithPoints.length > 0) {
        // Refrescar la lista de grupos para actualizar scoreboards
        refreshGroups();
      }
      
      return result;
    } else {
      throw new Error(result.error || 'Error al guardar la predicción');
    }
  } catch (error) {
    console.error('Error saving prediction:', error);
    alert('Error al guardar la predicción: ' + error.message);
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

## FLUJO COMPLETO

### PASO 1: Usuario ve un partido
- Usuario está viendo cualquier grupo (ej: "Los Mejores")
- Ve el partido: Flamengo vs Chelsea

### PASO 2: Usuario hace la predicción
- Usuario predice: Flamengo 2 - Chelsea 1
- Frontend envía:
  ```json
  {
    "matchId": "group-d-match-3",
    "team1Score": 2,
    "team2Score": 1
  }
  ```
- ❌ **NO envía groupId**

### PASO 3: Backend procesa automáticamente
1. Backend busca el partido en los grupos del usuario
2. Identifica el torneo (competitionId y category)
3. Busca TODOS los grupos del usuario en ese mismo torneo
4. Guarda la predicción UNA vez
5. Aplica la predicción a todos los grupos encontrados
6. Calcula puntos (si el partido ya se jugó)
7. Actualiza scoreboards de todos los grupos

### PASO 4: Frontend recibe respuesta
- Backend responde con `groupsApplied` (lista de grupos afectados)
- Frontend muestra: "Predicción guardada en 3 grupos"
- Usuario puede ver su predicción en cualquiera de los grupos

---

## VALIDACIONES EN EL FRONTEND

### Validaciones antes de enviar

```javascript
const validatePrediction = (matchId, team1Score, team2Score) => {
  const errors = [];
  
  if (!matchId || matchId.trim() === '') {
    errors.push('matchId es requerido');
  }
  
  if (team1Score === null || team1Score === undefined || isNaN(team1Score)) {
    errors.push('team1Score debe ser un número');
  } else if (team1Score < 0) {
    errors.push('team1Score debe ser >= 0');
  }
  
  if (team2Score === null || team2Score === undefined || isNaN(team2Score)) {
    errors.push('team2Score debe ser un número');
  } else if (team2Score < 0) {
    errors.push('team2Score debe ser >= 0');
  }
  
  return errors;
};

// Usar antes de enviar
const errors = validatePrediction(matchId, team1Score, team2Score);
if (errors.length > 0) {
  alert('Errores de validación:\n' + errors.join('\n'));
  return;
}
```

### Manejo de inputs vacíos

```javascript
// Si el usuario deja un input vacío, enviar 0
const team1Score = team1ScoreInput === '' || team1ScoreInput === null ? 0 : parseInt(team1ScoreInput);
const team2Score = team2ScoreInput === '' || team2ScoreInput === null ? 0 : parseInt(team2ScoreInput);
```

---

## MANEJO DE RESPUESTA

### Mostrar información de grupos afectados

```javascript
const handlePredictionResponse = (result) => {
  const { groupsApplied } = result;
  
  // Agrupar por estado
  const withPoints = groupsApplied.filter(g => g.pointsCalculated);
  const withoutPoints = groupsApplied.filter(g => !g.pointsCalculated);
  
  // Mostrar en UI
  return (
    <View>
      <Text>Predicción guardada en {groupsApplied.length} grupos</Text>
      
      {withPoints.length > 0 && (
        <View>
          <Text>Grupos con puntos calculados:</Text>
          {withPoints.map(group => (
            <Text key={group.groupId}>
              {group.groupName}: {group.points} puntos (Total: {group.totalScore})
            </Text>
          ))}
        </View>
      )}
      
      {withoutPoints.length > 0 && (
        <View>
          <Text>Grupos esperando resultados:</Text>
          {withoutPoints.map(group => (
            <Text key={group.groupId}>
              {group.groupName}: Puntos se calcularán cuando el partido se juegue
            </Text>
          ))}
        </View>
      )}
    </View>
  );
};
```

---

## CASOS ESPECIALES

### Caso 1: Usuario no pertenece a ningún grupo del torneo

**Respuesta del backend:**
```json
{
  "error": "User does not belong to any groups for this match's competition"
}
```

**Manejo en frontend:**
```javascript
if (result.error && result.error.includes("does not belong to any groups")) {
  alert('No perteneces a ningún grupo de este torneo. Únete a un grupo primero.');
}
```

### Caso 2: Partido no encontrado

**Respuesta del backend:**
```json
{
  "error": "Match not found in any of user's groups: group-d-match-3"
}
```

**Manejo en frontend:**
```javascript
if (result.error && result.error.includes("Match not found")) {
  alert('Este partido no está disponible en ninguno de tus grupos.');
}
```

### Caso 3: Usuario sin grupos

**Respuesta del backend:**
```json
{
  "error": "User does not belong to any groups"
}
```

**Manejo en frontend:**
```javascript
if (result.error && result.error.includes("does not belong to any groups")) {
  alert('No perteneces a ningún grupo. Crea o únete a un grupo primero.');
}
```

---

## EJEMPLOS DE REQUEST

### Ejemplo 1: Predicción simple (fase de grupos)

```javascript
POST /football-pool/v1/api/auth/6908d0864077087454146d5c/predictions

Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
  Content-Type: application/json

Body:
{
  "matchId": "group-d-match-3",
  "team1Score": 2,
  "team2Score": 1
}
```

### Ejemplo 2: Predicción con tiempo extra y penales (knockout)

```javascript
POST /football-pool/v1/api/auth/6908d0864077087454146d5c/predictions

Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
  Content-Type: application/json

Body:
{
  "matchId": "round-of-16-1",
  "team1Score": 1,
  "team2Score": 1,
  "userExtraTime": true,
  "userPenalties": true,
  "userPenaltiesTeam1Score": 4,
  "userPenaltiesTeam2Score": 3
}
```

### Ejemplo 3: Predicción con valores en 0 (inputs vacíos)

```javascript
POST /football-pool/v1/api/auth/6908d0864077087454146d5c/predictions

Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
  Content-Type: application/json

Body:
{
  "matchId": "group-a-match-1",
  "team1Score": 0,
  "team2Score": 0
}
```

---

## COMPORTAMIENTO DEL BACKEND

### Qué hace el backend automáticamente:

1. **Si NO se envía `groupId`:**
   - Busca el partido en todos los grupos del usuario
   - Identifica el torneo del partido (competitionId y category)
   - Busca TODOS los grupos del usuario en ese mismo torneo
   - Aplica la predicción a todos esos grupos

2. **Si SÍ se envía `groupId` (compatibilidad hacia atrás):**
   - Usa ese grupo específico
   - Valida que el grupo pertenece al mismo torneo del partido
   - Aplica la predicción solo a ese grupo

3. **Cálculo de puntos:**
   - Si el partido ya tiene resultados reales (`isPlayed = true`), calcula puntos automáticamente
   - Los mismos puntos se aplican a todos los grupos (misma predicción vs mismos resultados)
   - Actualiza el `totalScore` del usuario en cada grupo

4. **Actualización de scoreboards:**
   - Actualiza el scoreboard de cada grupo automáticamente
   - Reordena usuarios por score (descendente)
   - Actualiza posiciones

---

## PREGUNTAS FRECUENTES

**Q: ¿Qué pasa si el usuario no pertenece a ningún grupo del torneo?**
A: El backend devolverá un error 400: "User does not belong to any groups for this match's competition"

**Q: ¿Puedo hacer predicciones diferentes para diferentes grupos del mismo torneo?**
A: No. Una predicción es única por partido y se aplica a todos los grupos del mismo torneo. Si quieres cambiar tu predicción, se actualizará en todos los grupos.

**Q: ¿Qué pasa si hago la misma predicción dos veces?**
A: El backend hace un "upsert" (actualizar si existe, crear si no existe). No se duplicará la predicción.

**Q: ¿Cómo sé en qué grupos se aplicó mi predicción?**
A: La respuesta del backend incluye el campo `groupsApplied` con la lista completa de grupos donde se aplicó.

**Q: ¿Los puntos son diferentes en cada grupo?**
A: No, los puntos son los mismos porque es la misma predicción vs los mismos resultados. Sin embargo, el `totalScore` puede ser diferente en cada grupo porque el usuario puede tener otras predicciones en cada grupo.

**Q: ¿Qué pasa si me uno a un nuevo grupo después de hacer una predicción?**
A: Las predicciones existentes NO se aplican automáticamente a grupos nuevos. Solo las nuevas predicciones se aplican a todos los grupos del torneo.

**Q: ¿Puedo seguir enviando `groupId` si quiero?**
A: Sí, por compatibilidad hacia atrás, el backend acepta `groupId` opcional. Si se envía, solo aplicará la predicción a ese grupo específico.

---

## RESUMEN EJECUTIVO

📌 **QUÉ ENVIAR:**
   - `matchId` (obligatorio)
   - `team1Score` (obligatorio, puede ser 0)
   - `team2Score` (obligatorio, puede ser 0)
   - `userExtraTime`, `userPenalties`, etc. (opcional, solo para knockout)
   - ❌ **NO enviar `groupId`** (opcional, solo para compatibilidad)

📌 **QUÉ RECIBIR:**
   - `groupsApplied`: Lista de grupos donde se aplicó la predicción
   - `points`: Puntos ganados en cada grupo (si el partido ya se jugó)
   - `totalScore`: Score total del usuario en cada grupo

📌 **VENTAJAS:**
   - Una sola predicción para todos los grupos del mismo torneo
   - Consistencia entre grupos
   - Mejor experiencia de usuario
   - Menos llamadas al backend

---

## NOTAS IMPORTANTES

1. **El frontend NO debe enviar `groupId`** - El backend lo determinará automáticamente
2. **El frontend debe manejar inputs vacíos como 0** - Si el usuario no ingresa nada, enviar 0
3. **El frontend debe refrescar grupos después de guardar** - Si `pointsCalculated = true` en algún grupo, refrescar para ver scoreboards actualizados
4. **El frontend debe mostrar información de todos los grupos** - Usar `groupsApplied` para mostrar dónde se aplicó la predicción

---

## FIN DE DOCUMENTACIÓN

Esta funcionalidad está **IMPLEMENTADA Y LISTA PARA USAR**. El backend soporta completamente el flujo de predicciones multi-grupo.


## ✅ ESTADO: IMPLEMENTADO Y LISTO PARA USAR

El backend ha sido actualizado para soportar predicciones que se aplican automáticamente a todos los grupos del mismo torneo.

---

## RESUMEN

Una predicción se guarda **UNA SOLA VEZ** y se aplica automáticamente a **TODOS los grupos del mismo torneo** (competition) a los que pertenece el usuario.

**Ejemplo:**
- Usuario pertenece a 3 grupos del "FIFA Club World Cup"
- Usuario hace UNA predicción: Flamengo 2 - Chelsea 1
- El sistema AUTOMÁTICAMENTE aplica la predicción a los 3 grupos
- Los puntos se calculan una vez (mismo resultado = mismos puntos)
- El scoreboard de cada grupo se actualiza independientemente

---

## ENDPOINT: GUARDAR PREDICCIÓN

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
  "userExtraTime": true,
  "userPenalties": true,
  "userPenaltiesTeam1Score": 4,
  "userPenaltiesTeam2Score": 3
}
```

### ⚠️ IMPORTANTE

- ❌ **NO enviar `groupId`** - El backend determinará automáticamente todos los grupos del mismo torneo
- ✅ Solo enviar `matchId` y los scores
- ✅ El backend buscará automáticamente todos los grupos del usuario que pertenecen al mismo torneo del partido

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
      "pointsCalculated": true,
      "points": 5,
      "totalScore": 15
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
      "pointsCalculated": false,
      "points": 0,
      "totalScore": 10
    }
  ]
}
```

### Campos de la respuesta

- `message`: Mensaje de confirmación
- `userId`: ID del usuario
- `matchId`: ID del partido
- `team1Score`, `team2Score`: Scores de la predicción
- `groupsApplied`: Array con información de cada grupo donde se aplicó la predicción
  - `groupId`: ID del grupo
  - `groupName`: Nombre del grupo
  - `pointsCalculated`: `true` si el partido ya se jugó y se calcularon puntos
  - `points`: Puntos ganados en este grupo (0 si el partido no se jugó aún)
  - `totalScore`: Score total del usuario en este grupo

### Errores posibles

- **400 Bad Request**: 
  - `"All fields are required: userId, matchId, team1Score, team2Score"`
  - `"User does not belong to any groups"`
  - `"User does not belong to any groups for this match's competition"`
  
- **401 Unauthorized**: No autenticado

- **404 Not Found**: 
  - `"Group not found: {groupId}"` (si se proporcionó groupId)
  - `"Match not found in any of user's groups: {matchId}"` (si no se proporcionó groupId)

- **500 Internal Server Error**: Error del servidor

---

## EJEMPLO DE CÓDIGO EN EL FRONTEND

### Función para guardar una predicción (React/React Native)

```javascript
const savePrediction = async (matchId, team1Score, team2Score, userExtraTime, userPenalties) => {
  try {
    const body = {
      matchId: matchId,
      team1Score: team1Score || 0,  // Manejar inputs vacíos como 0
      team2Score: team2Score || 0   // Manejar inputs vacíos como 0
    };
    
    // Agregar campos opcionales solo si es fase de eliminación
    if (userExtraTime !== undefined && userExtraTime !== null) {
      body.userExtraTime = userExtraTime;
    }
    if (userPenalties !== undefined && userPenalties !== null) {
      body.userPenalties = userPenalties;
    }
    if (userPenaltiesTeam1Score !== undefined && userPenaltiesTeam1Score !== null) {
      body.userPenaltiesTeam1Score = userPenaltiesTeam1Score;
    }
    if (userPenaltiesTeam2Score !== undefined && userPenaltiesTeam2Score !== null) {
      body.userPenaltiesTeam2Score = userPenaltiesTeam2Score;
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
      const groupsWithPoints = result.groupsApplied.filter(g => g.pointsCalculated);
      const groupsWithoutPoints = result.groupsApplied.filter(g => !g.pointsCalculated);
      
      // Mostrar información de cada grupo
      result.groupsApplied.forEach(groupResult => {
        if (groupResult.pointsCalculated) {
          console.log(`✅ Grupo "${groupResult.groupName}": ${groupResult.points} puntos (Total: ${groupResult.totalScore})`);
        } else {
          console.log(`⏳ Grupo "${groupResult.groupName}": Predicción guardada. Puntos se calcularán cuando el partido se juegue.`);
        }
      });
      
      // Mostrar mensaje al usuario
      if (groupsWithPoints.length > 0) {
        const totalPoints = groupsWithPoints.reduce((sum, g) => sum + g.points, 0);
        alert(`¡Predicción guardada en ${result.groupsApplied.length} grupos!\n\nGanaste ${totalPoints} puntos en total.\n\n${groupsWithPoints.length} grupos con puntos calculados\n${groupsWithoutPoints.length} grupos esperando resultados`);
      } else {
        alert(`Predicción guardada en ${result.groupsApplied.length} grupos.\n\nLos puntos se calcularán automáticamente cuando el partido se juegue.`);
      }
      
      // Refrescar grupos si se calcularon puntos
      if (groupsWithPoints.length > 0) {
        // Refrescar la lista de grupos para actualizar scoreboards
        refreshGroups();
      }
      
      return result;
    } else {
      throw new Error(result.error || 'Error al guardar la predicción');
    }
  } catch (error) {
    console.error('Error saving prediction:', error);
    alert('Error al guardar la predicción: ' + error.message);
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

## FLUJO COMPLETO

### PASO 1: Usuario ve un partido
- Usuario está viendo cualquier grupo (ej: "Los Mejores")
- Ve el partido: Flamengo vs Chelsea

### PASO 2: Usuario hace la predicción
- Usuario predice: Flamengo 2 - Chelsea 1
- Frontend envía:
  ```json
  {
    "matchId": "group-d-match-3",
    "team1Score": 2,
    "team2Score": 1
  }
  ```
- ❌ **NO envía groupId**

### PASO 3: Backend procesa automáticamente
1. Backend busca el partido en los grupos del usuario
2. Identifica el torneo (competitionId y category)
3. Busca TODOS los grupos del usuario en ese mismo torneo
4. Guarda la predicción UNA vez
5. Aplica la predicción a todos los grupos encontrados
6. Calcula puntos (si el partido ya se jugó)
7. Actualiza scoreboards de todos los grupos

### PASO 4: Frontend recibe respuesta
- Backend responde con `groupsApplied` (lista de grupos afectados)
- Frontend muestra: "Predicción guardada en 3 grupos"
- Usuario puede ver su predicción en cualquiera de los grupos

---

## VALIDACIONES EN EL FRONTEND

### Validaciones antes de enviar

```javascript
const validatePrediction = (matchId, team1Score, team2Score) => {
  const errors = [];
  
  if (!matchId || matchId.trim() === '') {
    errors.push('matchId es requerido');
  }
  
  if (team1Score === null || team1Score === undefined || isNaN(team1Score)) {
    errors.push('team1Score debe ser un número');
  } else if (team1Score < 0) {
    errors.push('team1Score debe ser >= 0');
  }
  
  if (team2Score === null || team2Score === undefined || isNaN(team2Score)) {
    errors.push('team2Score debe ser un número');
  } else if (team2Score < 0) {
    errors.push('team2Score debe ser >= 0');
  }
  
  return errors;
};

// Usar antes de enviar
const errors = validatePrediction(matchId, team1Score, team2Score);
if (errors.length > 0) {
  alert('Errores de validación:\n' + errors.join('\n'));
  return;
}
```

### Manejo de inputs vacíos

```javascript
// Si el usuario deja un input vacío, enviar 0
const team1Score = team1ScoreInput === '' || team1ScoreInput === null ? 0 : parseInt(team1ScoreInput);
const team2Score = team2ScoreInput === '' || team2ScoreInput === null ? 0 : parseInt(team2ScoreInput);
```

---

## MANEJO DE RESPUESTA

### Mostrar información de grupos afectados

```javascript
const handlePredictionResponse = (result) => {
  const { groupsApplied } = result;
  
  // Agrupar por estado
  const withPoints = groupsApplied.filter(g => g.pointsCalculated);
  const withoutPoints = groupsApplied.filter(g => !g.pointsCalculated);
  
  // Mostrar en UI
  return (
    <View>
      <Text>Predicción guardada en {groupsApplied.length} grupos</Text>
      
      {withPoints.length > 0 && (
        <View>
          <Text>Grupos con puntos calculados:</Text>
          {withPoints.map(group => (
            <Text key={group.groupId}>
              {group.groupName}: {group.points} puntos (Total: {group.totalScore})
            </Text>
          ))}
        </View>
      )}
      
      {withoutPoints.length > 0 && (
        <View>
          <Text>Grupos esperando resultados:</Text>
          {withoutPoints.map(group => (
            <Text key={group.groupId}>
              {group.groupName}: Puntos se calcularán cuando el partido se juegue
            </Text>
          ))}
        </View>
      )}
    </View>
  );
};
```

---

## CASOS ESPECIALES

### Caso 1: Usuario no pertenece a ningún grupo del torneo

**Respuesta del backend:**
```json
{
  "error": "User does not belong to any groups for this match's competition"
}
```

**Manejo en frontend:**
```javascript
if (result.error && result.error.includes("does not belong to any groups")) {
  alert('No perteneces a ningún grupo de este torneo. Únete a un grupo primero.');
}
```

### Caso 2: Partido no encontrado

**Respuesta del backend:**
```json
{
  "error": "Match not found in any of user's groups: group-d-match-3"
}
```

**Manejo en frontend:**
```javascript
if (result.error && result.error.includes("Match not found")) {
  alert('Este partido no está disponible en ninguno de tus grupos.');
}
```

### Caso 3: Usuario sin grupos

**Respuesta del backend:**
```json
{
  "error": "User does not belong to any groups"
}
```

**Manejo en frontend:**
```javascript
if (result.error && result.error.includes("does not belong to any groups")) {
  alert('No perteneces a ningún grupo. Crea o únete a un grupo primero.');
}
```

---

## EJEMPLOS DE REQUEST

### Ejemplo 1: Predicción simple (fase de grupos)

```javascript
POST /football-pool/v1/api/auth/6908d0864077087454146d5c/predictions

Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
  Content-Type: application/json

Body:
{
  "matchId": "group-d-match-3",
  "team1Score": 2,
  "team2Score": 1
}
```

### Ejemplo 2: Predicción con tiempo extra y penales (knockout)

```javascript
POST /football-pool/v1/api/auth/6908d0864077087454146d5c/predictions

Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
  Content-Type: application/json

Body:
{
  "matchId": "round-of-16-1",
  "team1Score": 1,
  "team2Score": 1,
  "userExtraTime": true,
  "userPenalties": true,
  "userPenaltiesTeam1Score": 4,
  "userPenaltiesTeam2Score": 3
}
```

### Ejemplo 3: Predicción con valores en 0 (inputs vacíos)

```javascript
POST /football-pool/v1/api/auth/6908d0864077087454146d5c/predictions

Headers:
  Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
  Content-Type: application/json

Body:
{
  "matchId": "group-a-match-1",
  "team1Score": 0,
  "team2Score": 0
}
```

---

## COMPORTAMIENTO DEL BACKEND

### Qué hace el backend automáticamente:

1. **Si NO se envía `groupId`:**
   - Busca el partido en todos los grupos del usuario
   - Identifica el torneo del partido (competitionId y category)
   - Busca TODOS los grupos del usuario en ese mismo torneo
   - Aplica la predicción a todos esos grupos

2. **Si SÍ se envía `groupId` (compatibilidad hacia atrás):**
   - Usa ese grupo específico
   - Valida que el grupo pertenece al mismo torneo del partido
   - Aplica la predicción solo a ese grupo

3. **Cálculo de puntos:**
   - Si el partido ya tiene resultados reales (`isPlayed = true`), calcula puntos automáticamente
   - Los mismos puntos se aplican a todos los grupos (misma predicción vs mismos resultados)
   - Actualiza el `totalScore` del usuario en cada grupo

4. **Actualización de scoreboards:**
   - Actualiza el scoreboard de cada grupo automáticamente
   - Reordena usuarios por score (descendente)
   - Actualiza posiciones

---

## PREGUNTAS FRECUENTES

**Q: ¿Qué pasa si el usuario no pertenece a ningún grupo del torneo?**
A: El backend devolverá un error 400: "User does not belong to any groups for this match's competition"

**Q: ¿Puedo hacer predicciones diferentes para diferentes grupos del mismo torneo?**
A: No. Una predicción es única por partido y se aplica a todos los grupos del mismo torneo. Si quieres cambiar tu predicción, se actualizará en todos los grupos.

**Q: ¿Qué pasa si hago la misma predicción dos veces?**
A: El backend hace un "upsert" (actualizar si existe, crear si no existe). No se duplicará la predicción.

**Q: ¿Cómo sé en qué grupos se aplicó mi predicción?**
A: La respuesta del backend incluye el campo `groupsApplied` con la lista completa de grupos donde se aplicó.

**Q: ¿Los puntos son diferentes en cada grupo?**
A: No, los puntos son los mismos porque es la misma predicción vs los mismos resultados. Sin embargo, el `totalScore` puede ser diferente en cada grupo porque el usuario puede tener otras predicciones en cada grupo.

**Q: ¿Qué pasa si me uno a un nuevo grupo después de hacer una predicción?**
A: Las predicciones existentes NO se aplican automáticamente a grupos nuevos. Solo las nuevas predicciones se aplican a todos los grupos del torneo.

**Q: ¿Puedo seguir enviando `groupId` si quiero?**
A: Sí, por compatibilidad hacia atrás, el backend acepta `groupId` opcional. Si se envía, solo aplicará la predicción a ese grupo específico.

---

## RESUMEN EJECUTIVO

📌 **QUÉ ENVIAR:**
   - `matchId` (obligatorio)
   - `team1Score` (obligatorio, puede ser 0)
   - `team2Score` (obligatorio, puede ser 0)
   - `userExtraTime`, `userPenalties`, etc. (opcional, solo para knockout)
   - ❌ **NO enviar `groupId`** (opcional, solo para compatibilidad)

📌 **QUÉ RECIBIR:**
   - `groupsApplied`: Lista de grupos donde se aplicó la predicción
   - `points`: Puntos ganados en cada grupo (si el partido ya se jugó)
   - `totalScore`: Score total del usuario en cada grupo

📌 **VENTAJAS:**
   - Una sola predicción para todos los grupos del mismo torneo
   - Consistencia entre grupos
   - Mejor experiencia de usuario
   - Menos llamadas al backend

---

## NOTAS IMPORTANTES

1. **El frontend NO debe enviar `groupId`** - El backend lo determinará automáticamente
2. **El frontend debe manejar inputs vacíos como 0** - Si el usuario no ingresa nada, enviar 0
3. **El frontend debe refrescar grupos después de guardar** - Si `pointsCalculated = true` en algún grupo, refrescar para ver scoreboards actualizados
4. **El frontend debe mostrar información de todos los grupos** - Usar `groupsApplied` para mostrar dónde se aplicó la predicción

---

## FIN DE DOCUMENTACIÓN

Esta funcionalidad está **IMPLEMENTADA Y LISTA PARA USAR**. El backend soporta completamente el flujo de predicciones multi-grupo.


