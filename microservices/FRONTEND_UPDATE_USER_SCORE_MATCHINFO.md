# Documentación Frontend: Actualizar Score y MatchInfo en Múltiples Grupos

## 🚨 ATENCIÓN FRONTEND: DEBES ENVIAR LOS groupIds

### ⚠️ REQUERIMIENTO CRÍTICO

**Cuando el Frontend guarda una predicción, DEBE enviar los IDs de los grupos (`groupIds`) en el request.**

El backend necesita estos `groupIds` para saber en qué grupos actualizar el campo `users` con el nuevo `score` y `matchesInfo`.

### 📋 Checklist del Frontend:

- [ ] **Obtener los grupos del usuario** para la competencia antes de guardar la predicción
- [ ] **Extraer los `_id`** de cada grupo
- [ ] **Incluir el array `groupIds`** en el request al guardar la predicción
- [ ] **Enviar TODOS los grupos** donde el usuario participa en esa competencia

### Ejemplo mínimo del request:

```json
{
  "matchId": "match-123",
  "team1Score": 2,
  "team2Score": 1,
  "competitionId": "club-world-cup",
  "groupIds": ["692e69c026c8733c0b79d7ae", "692e69c026c8733c0b79d7af"],  // ⚠️ ESTO ES OBLIGATORIO
  "team1": "Real Madrid",  // ⚠️ REQUERIDO
  "team2": "Barcelona",     // ⚠️ REQUERIDO
  "realTeam1Score": 2,      // ⚠️ REQUERIDO (score real del partido)
  "realTeam2Score": 1       // ⚠️ REQUERIDO (score real del partido)
}
```

**Nota sobre `userId`:** 
- El `userId` se obtiene automáticamente del token JWT (header `Authorization: Bearer <token>`)
- Si el token no está disponible, puedes enviarlo en el body como `"userId": "..."` (para compatibilidad)
- **Sin los `groupIds`, el backend NO sabrá en qué grupos actualizar el usuario.**

## Descripción

Este documento describe cómo el Frontend debe actualizar el campo `users` en múltiples grupos después de guardar una predicción. El campo `users` contiene información de cada usuario en el grupo, incluyendo su `score` y `matchesInfo` (información de todas las predicciones del usuario).

## Flujo del Frontend

El Frontend debe seguir este flujo cuando guarda una predicción:

1. **Obtener los groupIds** → El usuario puede estar en múltiples grupos de la misma competencia
2. **Guardar predicción** → Llamar al endpoint de `auth_service` para guardar la predicción
3. **Incluir groupIds en el request** → **¡MUY IMPORTANTE!** El Frontend debe enviar los `groupIds` en el request
4. **Backend actualiza grupos** → El `auth_service` llamará al `groups_service` para actualizar cada grupo con el nuevo score y matchInfo

## Endpoint del Frontend

**POST** `/football-pool/v1/api/auth/predictions` (o el endpoint que uses para guardar predicciones)

> **Nota:** El endpoint interno `/football-pool/v1/api/groups/internal/update-matches-detail-multiple` es llamado automáticamente por `auth_service`. El Frontend NO debe llamarlo directamente.

## Estructura del Request desde el Frontend

**El Frontend debe enviar los `groupIds` cuando guarda una predicción.** El request debe incluir:

```json
{
  "matchId": "match-1",
  "team1Score": 2,
  "team2Score": 1,
  "groupIds": [
    "692e69c026c8733c0b79d7ae",
    "692e69c026c8733c0b79d7af",
    "692e69c026c8733c0b79d7b0"
  ]
}
```

### ⚠️ Campo CRÍTICO: `groupIds`

**El Frontend DEBE enviar el array `groupIds` con todos los IDs de los grupos donde el usuario participa en esa competencia.**

- Si el usuario está en 1 grupo → `groupIds: ["id-grupo-1"]`
- Si el usuario está en 3 grupos → `groupIds: ["id-grupo-1", "id-grupo-2", "id-grupo-3"]`
- Si el usuario está en 0 grupos → `groupIds: []` (aunque esto no debería pasar)

**¿Cómo obtener los groupIds?**
- El Frontend debe obtener los grupos del usuario para esa competencia antes de guardar la predicción
- Puede usar el endpoint `GET /football-pool/v1/api/groups` para obtener todos los grupos del usuario
- Filtrar por `competitionId` para obtener solo los grupos de esa competencia
- Extraer los `_id` de cada grupo y enviarlos en el array `groupIds`

## Estructura del Request Interno (auth_service → groups_service)

Cuando `auth_service` llama internamente a `groups_service`, el request tiene esta estructura:

```json
{
  "groupIds": [
    "692e69c026c8733c0b79d7ae",
    "692e69c026c8733c0b79d7af",
    "692e69c026c8733c0b79d7b0"
  ],
  "userId": "6908d0864077087454146d5c",
  "competitionId": "club-world-cup",
  "matchesDetail": [
    {
      "matchId": "match-1",
      "team1Id": "team-1",
      "team1Name": "Team 1",
      "team1Flag": "https://...",
      "team2Id": "team-2",
      "team2Name": "Team 2",
      "team2Flag": "https://...",
      "userTeam1Score": 2,
      "userTeam2Score": 1,
      "team1Score": 2,
      "team2Score": 1,
      "points": 3,
      "matchDate": "2025-12-15T10:00:00Z",
      "isPlayed": true,
      "stageId": "group-stage",
      "groupLetter": "A"
    },
    {
      "matchId": "match-2",
      "team1Id": "team-3",
      "team1Name": "Team 3",
      "team1Flag": "https://...",
      "team2Id": "team-4",
      "team2Name": "Team 4",
      "team2Flag": "https://...",
      "userTeam1Score": 1,
      "userTeam2Score": 1,
      "team1Score": 0,
      "team2Score": 0,
      "points": 0,
      "matchDate": "2025-12-16T10:00:00Z",
      "isPlayed": false,
      "stageId": "group-stage",
      "groupLetter": "B"
    }
  ],
  "userScore": 150
}
```

### Campos Requeridos

- **groupIds** (Array<String>): Lista de IDs de grupos donde se debe actualizar el usuario
- **userId** (String): ID del usuario que hizo la predicción
- **competitionId** (String): ID de la competencia
- **matchesDetail** (Array<Object>): Array con información de todas las predicciones del usuario para esta competencia
- **userScore** (Integer): Score acumulado del usuario para esta competencia

### Estructura de matchesDetail

Cada objeto en `matchesDetail` debe contener:

```typescript
interface MatchInfo {
  matchId: string;              // ID del partido
  team1Id: string;               // ID del equipo 1
  team1Name: string;             // Nombre del equipo 1
  team1Flag: string;             // URL de la bandera del equipo 1
  team2Id: string;               // ID del equipo 2
  team2Name: string;             // Nombre del equipo 2
  team2Flag: string;             // URL de la bandera del equipo 2
  userTeam1Score: number;        // Predicción del usuario para equipo 1
  userTeam2Score: number;        // Predicción del usuario para equipo 2
  team1Score?: number;           // Score real del equipo 1 (si el partido ya se jugó)
  team2Score?: number;           // Score real del equipo 2 (si el partido ya se jugó)
  points?: number;                // Puntos obtenidos por esta predicción
  matchDate: string;             // Fecha del partido (ISO 8601)
  isPlayed: boolean;             // Si el partido ya se jugó
  stageId: string;               // ID de la etapa (ej: "group-stage", "round-of-16")
  groupLetter?: string;          // Letra del grupo (solo para fase de grupos)
  // Campos opcionales para knockout stages
  userExtraTime?: boolean;        // Si el usuario predijo tiempo extra
  userPenalties?: boolean;       // Si el usuario predijo penales
  userPenaltiesTeam1Score?: number;
  userPenaltiesTeam2Score?: number;
  extraTime?: boolean;            // Si hubo tiempo extra (resultado real)
  penalties?: boolean;           // Si hubo penales (resultado real)
  penaltiesTeam1Score?: number;
  penaltiesTeam2Score?: number;
}
```

## Estructura del Response

```json
{
  "message": "Processed 3 groups",
  "successCount": 3,
  "errorCount": 0,
  "userId": "6908d0864077087454146d5c",
  "competitionId": "club-world-cup",
  "matchesDetailCount": 36,
  "userScore": 150,
  "results": [
    {
      "groupId": "692e69c026c8733c0b79d7ae",
      "status": "success",
      "message": "User score and matchesInfo updated successfully"
    },
    {
      "groupId": "692e69c026c8733c0b79d7af",
      "status": "success",
      "message": "User score and matchesInfo updated successfully"
    },
    {
      "groupId": "692e69c026c8733c0b79d7b0",
      "status": "error",
      "message": "Group not found"
    }
  ]
}
```

## ¿Qué hace el backend?

Cuando el Frontend guarda una predicción enviando los `groupIds`:

1. **Guarda la predicción en el documento `users`** en el campo `predictions[competitionId].matchInfo`
2. **Calcula el score acumulado** del usuario para esa competencia
3. **Para cada `groupId` enviado:**
   - Busca el documento `Group` por ese ID
   - En el campo `users[]` del grupo, busca el usuario por `userId`
   - **Actualiza el usuario:**
     - `users[].score` → con el score acumulado
     - `users[].matchesInfo` → copia exacta de `predictions[competitionId].matchInfo` del usuario

## Estructura del Documento Group después de la actualización

Después de guardar una predicción, el documento `Group` en MongoDB tendrá la siguiente estructura en el campo `users`:

```json
{
  "_id": "692e69c026c8733c0b79d7ae",
  "creatorUserId": "6908d0864077087454146d5c",
  "competitionId": "club-world-cup",
  "competitionName": "FIFA Club World Cup",
  "name": "Chiqui",
  "users": [
    {
      "_id": "6908d0864077087454146d5c",
      "userId": "6908d0864077087454146d5c",
      "nombre": "Juan",
      "score": 150,
      "matchesInfo": [
        {
          "matchId": "group-a-match-1",
          "team1": "Al-Ahly",
          "team2": "Inter Miami",
          "team1Score": 0,
          "team2Score": 3,
          "realTeam1Score": 0,
          "realTeam2Score": 0,
          "predictedDate": "2025-12-11T02:56:10.461+00:00"
        },
        {
          "matchId": "group-a-match-2",
          "team1": "Team 3",
          "team2": "Team 4",
          "team1Score": 1,
          "team2Score": 1,
          "realTeam1Score": 0,
          "realTeam2Score": 0,
          "predictedDate": "2025-12-12T02:56:10.461+00:00"
        }
      ]
    },
    {
      "_id": "6908d55a4077087454146d5e",
      "userId": "6908d55a4077087454146d5e",
      "nombre": "Joel",
      "score": 120,
      "matchesInfo": [
        // ... predicciones de Joel
      ]
    }
  ],
  "matchesDetail": [
    // Array con información de todos los partidos de la competencia
    // (mismo formato que matchesInfo pero a nivel de grupo)
  ]
}
```

## Comportamiento del Backend

Cuando guardas una predicción con `groupIds`:

1. **Guarda en `users.predictions[competitionId].matchInfo`**: La predicción se guarda en el documento del usuario
2. **Calcula el score acumulado**: Suma todos los puntos de las predicciones del usuario para esa competencia
3. **Para cada `groupId` enviado:**
   - Busca el documento `Group` por ID
   - En `users[]` del grupo, busca el usuario por `userId`
   - **Actualiza `users[].score`**: Con el score acumulado del usuario
   - **Actualiza `users[].matchesInfo`**: Copia exacta de `predictions[competitionId].matchInfo` del usuario
   - Si el usuario no existe en el grupo, lo crea automáticamente

**Importante:** El campo `matchesInfo` en el grupo contiene exactamente la misma información que `matchInfo` en `predictions[competitionId]` del usuario, incluyendo:
- `matchId`
- `team1`, `team2`
- `team1Score`, `team2Score` (predicción del usuario)
- `realTeam1Score`, `realTeam2Score` (scores reales)
- `predictedDate` (fecha de la predicción)
- Campos opcionales para knockout (extraTime, penalties, etc.)

## Ejemplo de Código Frontend (TypeScript/JavaScript)

### Ejemplo 1: Guardar predicción con groupIds

```typescript
// 1. Primero obtener los grupos del usuario para la competencia
async function getUserGroupsForCompetition(competitionId: string): Promise<string[]> {
  const response = await fetch(
    `/football-pool/v1/api/groups?competitionId=${competitionId}`,
    {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    }
  );
  
  const data = await response.json();
  const groups = data.groups || [];
  
  // Extraer los IDs de los grupos
  return groups.map((group: any) => group._id || group.groupId);
}

// 2. Guardar predicción incluyendo los groupIds
async function savePrediction(
  matchId: string,
  team1Score: number,
  team2Score: number,
  competitionId: string,
  team1: string,        // ⚠️ REQUERIDO: Nombre del equipo 1
  team2: string,        // ⚠️ REQUERIDO: Nombre del equipo 2
  realTeam1Score: number, // ⚠️ REQUERIDO: Score real del equipo 1
  realTeam2Score: number, // ⚠️ REQUERIDO: Score real del equipo 2
  token: string
) {
  // ⚠️ IMPORTANTE: Obtener los groupIds antes de guardar
  const groupIds = await getUserGroupsForCompetition(competitionId);
  
  if (groupIds.length === 0) {
    console.warn('⚠️ Usuario no está en ningún grupo para esta competencia');
    // Puedes decidir si quieres guardar la predicción de todas formas
  }
  
  const response = await fetch('/football-pool/v1/api/auth/predictions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`  // ⚠️ El userId se obtiene automáticamente del token
    },
    body: JSON.stringify({
      matchId,
      team1Score,        // Predicción del usuario
      team2Score,        // Predicción del usuario
      competitionId,
      groupIds,          // ⚠️ CRÍTICO: Enviar los groupIds
      team1,            // ⚠️ REQUERIDO
      team2,            // ⚠️ REQUERIDO
      realTeam1Score,   // ⚠️ REQUERIDO: Score real del partido
      realTeam2Score    // ⚠️ REQUERIDO: Score real del partido
      // userId NO es necesario en el body, se obtiene del token JWT
    })
  });
  
  const result = await response.json();
  
  if (result.success) {
    console.log(`✅ Predicción guardada y ${groupIds.length} grupos actualizados`);
  }
  
  return result;
}

// Uso:
savePrediction('match-123', 2, 1, 'club-world-cup');
```

### Ejemplo 2: Si ya tienes los grupos en el estado

```typescript
// Si ya tienes los grupos cargados en tu estado/context
function savePredictionWithGroups(
  matchId: string,
  team1Score: number,
  team2Score: number,
  competitionId: string,
  userGroups: Group[]  // Grupos que ya tienes en el estado
) {
  // Filtrar grupos por competencia y extraer IDs
  const groupIds = userGroups
    .filter(group => group.competitionId === competitionId)
    .map(group => group._id || group.groupId);
  
  return fetch('/football-pool/v1/api/auth/predictions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({
      matchId,
      team1Score,
      team2Score,
      competitionId,
      groupIds  // ⚠️ Enviar los groupIds
    })
  });
}
```

### Ejemplo 3: React Hook personalizado

```typescript
import { useState, useCallback } from 'react';

function useSavePrediction() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const savePrediction = useCallback(async (
    matchId: string,
    team1Score: number,
    team2Score: number,
    competitionId: string,
    groupIds: string[]  // ⚠️ Recibir groupIds como parámetro
  ) => {
    setLoading(true);
    setError(null);
    
    try {
      const response = await fetch('/football-pool/v1/api/auth/predictions', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          matchId,
          team1Score,
          team2Score,
          competitionId,
          groupIds  // ⚠️ Incluir groupIds en el body
        })
      });
      
      if (!response.ok) {
        throw new Error('Error al guardar predicción');
      }
      
      const result = await response.json();
      return result;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Error desconocido');
      throw err;
    } finally {
      setLoading(false);
    }
  }, []);
  
  return { savePrediction, loading, error };
}

// Uso en componente:
function PredictionForm({ matchId, competitionId, userGroups }) {
  const { savePrediction, loading } = useSavePrediction();
  
  const handleSubmit = async (team1Score, team2Score) => {
    // Extraer groupIds de los grupos del usuario para esta competencia
    const groupIds = userGroups
      .filter(g => g.competitionId === competitionId)
      .map(g => g._id);
    
    await savePrediction(matchId, team1Score, team2Score, competitionId, groupIds);
  };
  
  // ...
}
```

## Notas Importantes

1. **El usuario siempre existe**: El endpoint asume que el usuario ya existe en el grupo. Si no existe, lo crea automáticamente.

2. **Actualización completa de matchesInfo**: El campo `matchesInfo` se reemplaza completamente con el nuevo array `matchesDetail`. No se hace merge parcial.

3. **Múltiples grupos**: El endpoint puede procesar múltiples grupos en una sola llamada, lo cual es más eficiente que hacer llamadas individuales.

4. **Manejo de errores**: Si un grupo no se encuentra o falla la actualización, el endpoint continúa procesando los demás grupos y retorna un resumen con los resultados.

5. **Autenticación**: Este endpoint requiere el header `X-Service-Token` para autenticación entre servicios.

## Resumen para el Frontend

### ✅ Lo que el Frontend DEBE hacer:

1. **Obtener los groupIds antes de guardar la predicción**
   - Usar `GET /football-pool/v1/api/groups` para obtener los grupos del usuario
   - Filtrar por `competitionId` para obtener solo los grupos de esa competencia
   - Extraer los `_id` de cada grupo

2. **Incluir `groupIds` en el request al guardar la predicción**
   - El campo `groupIds` es un array de strings
   - Debe incluir TODOS los grupos donde el usuario participa en esa competencia
   - Si el usuario está en 0 grupos, enviar array vacío `[]`

3. **Incluir el token JWT en el header Authorization**
   - El `userId` se obtiene automáticamente del token JWT
   - No necesitas enviar `userId` en el body (aunque puedes hacerlo para compatibilidad)
   - Header: `Authorization: Bearer <tu-token-jwt>`

4. **Incluir campos requeridos en el body**
   - `matchId`: ID del partido
   - `team1Score`, `team2Score`: Predicción del usuario
   - `realTeam1Score`, `realTeam2Score`: Scores reales del partido
   - `team1`, `team2`: Nombres de los equipos
   - `competitionId`: ID de la competencia
   - `groupIds`: Array de IDs de grupos ⚠️ CRÍTICO

5. **Manejar la respuesta**
   - El backend actualizará automáticamente los grupos con el nuevo score y matchInfo
   - No necesitas hacer llamadas adicionales para actualizar los grupos

### ❌ Lo que el Frontend NO debe hacer:

- ❌ NO llamar directamente al endpoint `/internal/update-matches-detail-multiple` (es interno)
- ❌ NO olvidar enviar el campo `groupIds` (es crítico)
- ❌ NO asumir que el backend sabe en qué grupos está el usuario (debes enviarlos)
- ❌ NO olvidar incluir el token JWT en el header `Authorization` (necesario para obtener el userId)
- ❌ NO olvidar enviar los campos requeridos: `team1`, `team2`, `realTeam1Score`, `realTeam2Score`

## Integración con auth_service

El `auth_service` recibirá los `groupIds` del Frontend y automáticamente:
1. Guardará la predicción exitosamente
2. Calculará el nuevo score del usuario
3. Obtendrá todas las predicciones del usuario para la competencia (para construir `matchesDetail`)
4. Llamará al `groups_service` con los `groupIds` recibidos del Frontend para actualizar cada grupo

**El `auth_service` NO necesita buscar los grupos del usuario porque el Frontend ya los envió en el request.**

