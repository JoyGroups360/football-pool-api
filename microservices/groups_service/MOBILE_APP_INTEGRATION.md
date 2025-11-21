# Documentación para App Mobile - Groups Service

## 📱 Resumen General

El `groups_service` gestiona grupos de usuarios dentro de competencias de fútbol. Cada grupo tiene una estructura de torneo completa con fase de grupos y eliminatorias, donde puedes ingresar resultados de partidos que actualizan automáticamente las estadísticas y clasificaciones.

---

## 🔐 Autenticación

**TODOS los endpoints requieren JWT Token** en el header:
```
Authorization: Bearer {jwt_token}
```

El token se obtiene del `auth_service` al hacer login.

---

## 📍 Base URL

```
http://localhost:1292/football-pool/v1/api/groups
```

O a través del Gateway (si está configurado):
```
http://localhost:8080/football-pool/v1/api/groups
```

---

## 🎯 Endpoints Principales

### 1. Obtener Grupos del Usuario

**GET** `/groups`

Obtiene todos los grupos donde el usuario es miembro o creador.

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "groups": [
    {
      "groupId": "691a192a53148e413d0e49b0",
      "name": "Chiqui",
      "competitionId": "club-world-cup",
      "competitionName": "FIFA Club World Cup",
      "competitionImage": "https://...",
      "creatorUserId": "6908d0864077087454146d5c",
      "userIds": ["6908d0864077087454146d5c", "68f696e7dc4cf83be1bce269"],
      "tournamentStructure": {
        "currentStage": "group-stage",
        "config": {
          "totalTeams": 32,
          "numberOfGroups": 8,
          "teamsPerGroup": 4,
          "teamsQualifyPerGroup": 2
        }
      }
    }
  ],
  "count": 1
}
```

---

### 2. Obtener Detalles de un Grupo

**GET** `/groups/{groupId}`

Obtiene todos los detalles de un grupo específico, incluyendo estructura del torneo, equipos, estadísticas y partidos.

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "group": {
    "groupId": "691a192a53148e413d0e49b0",
    "name": "Chiqui",
    "competitionId": "club-world-cup",
    "competitionName": "FIFA Club World Cup",
    "competitionImage": "https://...",
    "creatorUserId": "6908d0864077087454146d5c",
    "userIds": ["6908d0864077087454146d5c"],
    "tournamentStructure": {
      "currentStage": "group-stage",
      "stages": {
        "group-stage": {
          "stageId": "group-stage",
          "stageName": "Fase de Grupos",
          "type": "groups",
          "isActive": true,
          "groups": [
            {
              "groupLetter": "A",
              "groupName": "Grupo A",
              "teams": [
                {
                  "teamId": "team1",
                  "teamName": "Real Madrid",
                  "teamFlag": "🇪🇸",
                  "played": 3,
                  "won": 2,
                  "drawn": 1,
                  "lost": 0,
                  "goalsFor": 7,
                  "goalsAgainst": 2,
                  "goalDifference": 5,
                  "points": 7,
                  "position": 1
                }
              ],
              "matches": [
                {
                  "matchId": "group-a-match-1",
                  "matchNumber": "1",
                  "team1Id": "team1",
                  "team1Name": "Real Madrid",
                  "team1Flag": "🇪🇸",
                  "team2Id": "team2",
                  "team2Name": "Manchester City",
                  "team2Flag": "🏴󠁧󠁢󠁥󠁮󠁧󠁿",
                  "team1Score": null,
                  "team2Score": null,
                  "isPlayed": false,
                  "status": "scheduled",
                  "matchday": 1
                }
              ]
            }
          ]
        }
      }
    }
  }
}
```

---

### 3. Obtener Partidos de un Grupo

**GET** `/groups/{groupId}/matches`

Obtiene todos los partidos de un grupo. Puedes filtrar por etapa, grupo o estado.

**Query Parameters (opcionales):**
- `stageId`: Filtrar por etapa (`group-stage`, `round-of-16`, `quarter-finals`, etc.)
- `groupLetter`: Filtrar por grupo (`A`, `B`, `C`, etc.) - solo para fase de grupos
- `status`: Filtrar por estado (`scheduled`, `finished`, `in-progress`)

**Ejemplos:**
```
GET /groups/{groupId}/matches
GET /groups/{groupId}/matches?stageId=group-stage
GET /groups/{groupId}/matches?stageId=group-stage&groupLetter=A
GET /groups/{groupId}/matches?status=scheduled
```

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "groupId": "691a192a53148e413d0e49b0",
  "matches": [
    {
      "matchId": "group-a-match-1",
      "matchNumber": "1",
      "stageId": "group-stage",
      "groupLetter": "A",
      "team1Id": "team1",
      "team1Name": "Real Madrid",
      "team1Flag": "🇪🇸",
      "team2Id": "team2",
      "team2Name": "Manchester City",
      "team2Flag": "🏴󠁧󠁢󠁥󠁮󠁧󠁿",
      "team1Score": null,
      "team2Score": null,
      "winnerTeamId": null,
      "loserTeamId": null,
      "isDraw": null,
      "matchDate": null,
      "playedDate": null,
      "isPlayed": false,
      "venue": null,
      "status": "scheduled",
      "matchday": 1
    }
  ],
  "count": 48,
  "filters": {
    "stageId": "all",
    "groupLetter": "all",
    "status": "all"
  }
}
```

---

### 4. Obtener un Partido Específico

**GET** `/groups/{groupId}/matches/{matchId}`

Obtiene los detalles de un partido específico.

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "groupId": "691a192a53148e413d0e49b0",
  "match": {
    "matchId": "group-a-match-1",
    "matchNumber": "1",
    "stageId": "group-stage",
    "groupLetter": "A",
    "team1Id": "team1",
    "team1Name": "Real Madrid",
    "team1Flag": "🇪🇸",
    "team2Id": "team2",
    "team2Name": "Manchester City",
    "team2Flag": "🏴󠁧󠁢󠁥󠁮󠁧󠁿",
    "team1Score": 3,
    "team2Score": 1,
    "winnerTeamId": "team1",
    "loserTeamId": "team2",
    "isDraw": false,
    "matchDate": null,
    "playedDate": "2025-11-20T15:00:00Z",
    "isPlayed": true,
    "venue": "Estadio Santiago Bernabéu",
    "status": "finished",
    "matchday": 1
  }
}
```

---

### 5. Registrar Resultado de Partido ⚽

**POST** `/groups/{groupId}/matches/{matchId}/result`

Registra o actualiza el resultado de un partido. **Solo el creador del grupo puede registrar resultados.**

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Body:**
```json
{
  "team1Score": 3,
  "team2Score": 1,
  "playedDate": "2025-11-20T15:00:00Z",  // Opcional
  "venue": "Estadio Santiago Bernabéu"   // Opcional
}
```

**Campos requeridos:**
- `team1Score` (Integer): Marcador del equipo 1
- `team2Score` (Integer): Marcador del equipo 2

**Campos opcionales:**
- `playedDate` (String/Date): Fecha en que se jugó el partido
- `venue` (String): Nombre del estadio/venue

**Response (200 OK):**
```json
{
  "message": "Match result registered successfully",
  "match": {
    "matchId": "group-a-match-1",
    "team1Score": 3,
    "team2Score": 1,
    "winnerTeamId": "team1",
    "loserTeamId": "team2",
    "isDraw": false,
    "isPlayed": true,
    "status": "finished"
  },
  "groupId": "691a192a53148e413d0e49b0"
}
```

**⚠️ IMPORTANTE:** Al registrar un resultado:
- Se actualizan automáticamente las estadísticas de los equipos (played, won, drawn, lost, goalsFor, goalsAgainst, goalDifference, points)
- Se recalculan las posiciones en la tabla
- Se actualizan los equipos clasificados (top 2 de cada grupo)
- Si es eliminatoria, el ganador avanza automáticamente al siguiente partido

---

### 6. Actualizar Resultado de Partido

**PUT** `/groups/{groupId}/matches/{matchId}/result`

Igual que POST, pero semánticamente para actualizar un resultado existente.

**Headers y Body:** Igual que POST

---

### 7. Limpiar Resultado de Partido

**DELETE** `/groups/{groupId}/matches/{matchId}/result`

Elimina el resultado de un partido y revierte las estadísticas. **Solo el creador del grupo puede hacer esto.**

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "message": "Match result cleared successfully",
  "groupId": "691a192a53148e413d0e49b0",
  "match": {
    "matchId": "group-a-match-1",
    "team1Score": null,
    "team2Score": null,
    "isPlayed": false,
    "status": "scheduled"
  }
}
```

---

## 📊 Estructura de Datos

### Grupo (Group)

```json
{
  "groupId": "String - ID único del grupo",
  "creatorUserId": "String - ID del usuario creador",
  "competitionId": "String - ID de la competencia",
  "competitionName": "String - Nombre de la competencia",
  "competitionImage": "String - URL de la imagen",
  "name": "String - Nombre del grupo",
  "userIds": ["String - IDs de usuarios miembros"],
  "tournamentStructure": {
    "tournamentFormat": "groups-then-knockout",
    "currentStage": "group-stage",
    "config": {
      "totalTeams": 32,
      "numberOfGroups": 8,
      "teamsPerGroup": 4,
      "teamsQualifyPerGroup": 2
    },
    "stages": {
      "group-stage": { /* Etapa de grupos */ },
      "round-of-16": { /* Octavos */ },
      "quarter-finals": { /* Cuartos */ },
      "semi-finals": { /* Semifinales */ },
      "third-place": { /* Tercer lugar */ },
      "final": { /* Final */ }
    }
  }
}
```

### Partido (Match)

```json
{
  "matchId": "String - ID único del partido",
  "matchNumber": "String - Número del partido",
  "stageId": "String - ID de la etapa (group-stage, round-of-16, etc.)",
  "groupLetter": "String o null - Letra del grupo (solo para fase de grupos)",
  "team1Id": "String - ID del equipo 1",
  "team1Name": "String - Nombre del equipo 1",
  "team1Flag": "String - Bandera/emoji del equipo 1",
  "team2Id": "String - ID del equipo 2",
  "team2Name": "String - Nombre del equipo 2",
  "team2Flag": "String - Bandera/emoji del equipo 2",
  "team1Score": "Integer o null - Marcador equipo 1",
  "team2Score": "Integer o null - Marcador equipo 2",
  "winnerTeamId": "String o null - ID del ganador",
  "loserTeamId": "String o null - ID del perdedor",
  "isDraw": "Boolean o null - true si fue empate",
  "matchDate": "Date o null - Fecha programada",
  "playedDate": "Date o null - Fecha en que se jugó",
  "isPlayed": "Boolean - true si ya se jugó",
  "venue": "String o null - Nombre del estadio",
  "status": "String - scheduled | in-progress | finished | postponed | cancelled",
  "nextMatchId": "String o null - ID del siguiente partido (eliminatorias)",
  "nextStageId": "String o null - ID de la siguiente etapa (eliminatorias)",
  "matchday": "Integer o null - Jornada (solo fase de grupos)"
}
```

### Estadísticas de Equipo (TeamScore)

```json
{
  "teamId": "String - ID del equipo",
  "teamName": "String - Nombre del equipo",
  "teamFlag": "String - Bandera/emoji",
  "played": "Integer - Partidos jugados",
  "won": "Integer - Partidos ganados",
  "drawn": "Integer - Partidos empatados",
  "lost": "Integer - Partidos perdidos",
  "goalsFor": "Integer - Goles a favor",
  "goalsAgainst": "Integer - Goles en contra",
  "goalDifference": "Integer - Diferencia de goles",
  "points": "Integer - Puntos totales",
  "position": "Integer - Posición en la tabla (1, 2, 3, 4)"
}
```

---

## 🔄 Flujos Principales para la App Mobile

### Flujo 1: Ver Grupos del Usuario

1. Usuario hace login → obtiene JWT token
2. **GET** `/groups` → Obtiene lista de grupos
3. App muestra lista de grupos con:
   - Nombre del grupo
   - Nombre de la competencia
   - Imagen de la competencia
   - Estado actual (fase de grupos, octavos, etc.)

---

### Flujo 2: Ver Detalles de un Grupo

1. Usuario selecciona un grupo
2. **GET** `/groups/{groupId}` → Obtiene detalles completos
3. App muestra:
   - **Pestaña "Grupos"**: Tablas de clasificación por grupo (A, B, C, etc.)
   - **Pestaña "Partidos"**: Lista de partidos filtrados por grupo/jornada
   - **Pestaña "Eliminatorias"**: Bracket de octavos, cuartos, semifinales, final

---

### Flujo 3: Ver Partidos de un Grupo

1. Usuario entra a un grupo
2. **GET** `/groups/{groupId}/matches?stageId=group-stage&groupLetter=A`
3. App muestra partidos del Grupo A:
   - Partidos programados (sin resultado)
   - Partidos jugados (con resultado)
   - Agrupados por jornada (matchday)

---

### Flujo 4: Registrar Resultado (Solo Creador)

1. Creador del grupo selecciona un partido
2. **GET** `/groups/{groupId}/matches/{matchId}` → Ver detalles del partido
3. Creador ingresa marcadores:
   - `team1Score`: 2
   - `team2Score`: 1
4. **POST** `/groups/{groupId}/matches/{matchId}/result` → Envía resultado
5. App actualiza:
   - Estadísticas de equipos
   - Posiciones en tabla
   - Estado del partido (finished)
   - Si es eliminatoria, muestra ganador avanzando

---

### Flujo 5: Ver Tabla de Clasificación

1. **GET** `/groups/{groupId}` → Obtiene grupo completo
2. Navegar a: `tournamentStructure.stages.group-stage.groups[0].teams`
3. Ordenar por `position` o por `points` (descendente)
4. Mostrar tabla con:
   - Posición
   - Equipo (nombre + bandera)
   - PJ (played)
   - G (won)
   - E (drawn)
   - P (lost)
   - GF (goalsFor)
   - GC (goalsAgainst)
   - DG (goalDifference)
   - Pts (points)

---

### Flujo 6: Ver Bracket de Eliminatorias

1. **GET** `/groups/{groupId}/matches?stageId=round-of-16`
2. App muestra bracket visual:
   - Octavos → Cuartos → Semifinales → Final
   - Cada partido muestra equipos y resultado (si está jugado)
   - Ganadores avanzan automáticamente al siguiente partido

---

## 🎨 Campos Importantes para UI

### Para Mostrar Partidos:

- **Partido sin jugar:**
  - `team1Score: null`, `team2Score: null`
  - `status: "scheduled"`
  - `isPlayed: false`
  - Mostrar: "vs" o "Programado"

- **Partido jugado:**
  - `team1Score: 3`, `team2Score: 1`
  - `status: "finished"`
  - `isPlayed: true`
  - `winnerTeamId: "team1"`
  - Mostrar: "3 - 1" con ganador destacado

- **Empate:**
  - `team1Score: 2`, `team2Score: 2`
  - `isDraw: true`
  - `winnerTeamId: null`
  - Mostrar: "2 - 2" (Empate)

### Para Mostrar Tabla de Clasificación:

- Ordenar por `position` (1, 2, 3, 4)
- O por `points` descendente, luego `goalDifference`, luego `goalsFor`
- Resaltar los top 2 (equipos clasificados)

### Para Mostrar Bracket:

- Usar `nextMatchId` y `nextStageId` para conectar partidos
- Mostrar ganador avanzando al siguiente partido
- Si `team1Id` o `team2Id` es `null`, el partido aún no tiene equipos asignados

---

## ⚠️ Validaciones y Errores

### Error 401 - No Autenticado
```json
{
  "error": "Unauthorized",
  "message": "Authentication required. Please provide a valid JWT token."
}
```
**Solución:** Verificar que el token JWT esté en el header `Authorization: Bearer {token}`

### Error 403 - Sin Permisos
```json
{
  "error": "You don't have access to this group"
}
```
**Solución:** El usuario no es miembro ni creador del grupo

### Error 403 - Solo Creador Puede Registrar Resultados
```json
{
  "error": "Only the group creator can register match results"
}
```
**Solución:** Solo el creador del grupo puede ingresar resultados

### Error 404 - Grupo No Encontrado
```json
{
  "error": "Group not found"
}
```
**Solución:** Verificar que el `groupId` sea correcto

### Error 404 - Partido No Encontrado
```json
{
  "error": "Match not found"
}
```
**Solución:** Verificar que el `matchId` sea correcto

### Error 400 - Campos Requeridos Faltantes
```json
{
  "error": "team1Score and team2Score are required"
}
```
**Solución:** Enviar ambos campos en el body del request

---

## 📝 Ejemplos de Uso

### Ejemplo 1: Obtener todos los grupos del usuario

```javascript
// React Native / JavaScript
const response = await fetch('http://localhost:1292/football-pool/v1/api/groups', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${jwtToken}`,
    'Content-Type': 'application/json'
  }
});

const data = await response.json();
console.log('Grupos del usuario:', data.groups);
```

### Ejemplo 2: Obtener partidos de un grupo específico

```javascript
const groupId = '691a192a53148e413d0e49b0';
const response = await fetch(
  `http://localhost:1292/football-pool/v1/api/groups/${groupId}/matches?stageId=group-stage&groupLetter=A`,
  {
    method: 'GET',
    headers: {
      'Authorization': `Bearer ${jwtToken}`,
      'Content-Type': 'application/json'
    }
  }
);

const data = await response.json();
console.log('Partidos del Grupo A:', data.matches);
```

### Ejemplo 3: Registrar resultado de partido

```javascript
const groupId = '691a192a53148e413d0e49b0';
const matchId = 'group-a-match-1';

const response = await fetch(
  `http://localhost:1292/football-pool/v1/api/groups/${groupId}/matches/${matchId}/result`,
  {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${jwtToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      team1Score: 3,
      team2Score: 1,
      venue: 'Estadio Santiago Bernabéu'
    })
  }
);

const data = await response.json();
console.log('Resultado registrado:', data);
```

---

## 🔍 Ubicación de Matches en el Documento

Los matches se encuentran en:

**Fase de Grupos:**
```
tournamentStructure.stages.group-stage.groups[0].matches[]
```

**Eliminatorias:**
```
tournamentStructure.stages.round-of-16.matches[]
tournamentStructure.stages.quarter-finals.matches[]
tournamentStructure.stages.semi-finals.matches[]
tournamentStructure.stages.third-place.matches[]
tournamentStructure.stages.final.matches[]
```

---

## 📌 Notas Importantes

1. **Solo el creador puede registrar resultados**: Verifica que el usuario sea el `creatorUserId` antes de mostrar el botón de "Ingresar resultado"

2. **Actualización automática**: Al registrar un resultado, las estadísticas se actualizan automáticamente. No necesitas hacer otra llamada para actualizar la tabla.

3. **Filtros útiles**: Usa los query parameters para filtrar partidos:
   - Por etapa: `?stageId=group-stage`
   - Por grupo: `?groupLetter=A`
   - Por estado: `?status=scheduled` o `?status=finished`

4. **Estructura flexible**: El sistema soporta diferentes formatos de torneo. La estructura se adapta automáticamente según el número de equipos.

5. **Jornadas (Matchdays)**: En fase de grupos, los partidos tienen `matchday` (1, 2, 3, etc.) para agruparlos por fecha/jornada.

---

## 🎯 Predicciones de Marcadores

Los usuarios pueden hacer predicciones de marcadores para cada partido. Las predicciones se guardan en el documento `User` en `auth_service` y se comparan con los resultados reales para calcular scores.

### Estructura de Predicción

Las predicciones se guardan en el campo `predictions` del documento User:

```json
{
  "predictions": [
    {
      "groupId": "691a192a53148e413d0e49b0",
      "matchId": "group-a-match-1",
      "team1Score": 2,
      "team2Score": 1,
      "predictedDate": "2025-11-20T10:00:00Z",
      "points": 0  // Se calcula después comparando con resultado real
    }
  ]
}
```

### Sistema de Puntos

- **Marcador exacto**: 3 puntos (predicción: 2-1, resultado: 2-1)
- **Resultado correcto**: 1 punto (predicción: 2-1, resultado: 3-2 - ambos ganó equipo 1)
- **Resultado incorrecto**: 0 puntos

---

### 8. Guardar Predicción de Partido

**POST** `/groups/{groupId}/matches/{matchId}/predict`

Guarda o actualiza la predicción del usuario autenticado para un partido específico.

**⚠️ IMPORTANTE:** No se puede predecir un partido que ya fue jugado.

**Headers:**
```
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**Body:**
```json
{
  "team1Score": 2,
  "team2Score": 1
}
```

**Response (200 OK):**
```json
{
  "message": "Prediction saved successfully",
  "prediction": {
    "userId": "6908d0864077087454146d5c",
    "groupId": "691a192a53148e413d0e49b0",
    "matchId": "group-a-match-1",
    "team1Score": 2,
    "team2Score": 1
  }
}
```

**Errores:**
- `400`: Partido ya fue jugado (no se puede predecir)
- `403`: Usuario no tiene acceso al grupo
- `404`: Grupo o partido no encontrado

---

### 9. Obtener Todas las Predicciones del Usuario en un Grupo

**GET** `/groups/{groupId}/predictions`

Obtiene todas las predicciones del usuario autenticado para un grupo específico.

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "userId": "6908d0864077087454146d5c",
  "groupId": "691a192a53148e413d0e49b0",
  "predictions": [
    {
      "groupId": "691a192a53148e413d0e49b0",
      "matchId": "group-a-match-1",
      "team1Score": 2,
      "team2Score": 1,
      "predictedDate": "2025-11-20T10:00:00Z",
      "points": 0
    },
    {
      "groupId": "691a192a53148e413d0e49b0",
      "matchId": "group-a-match-2",
      "team1Score": 1,
      "team2Score": 1,
      "predictedDate": "2025-11-20T11:00:00Z",
      "points": 1
    }
  ],
  "count": 2
}
```

---

### 10. Obtener Predicción Específica

**GET** `/groups/{groupId}/matches/{matchId}/predict`

Obtiene la predicción del usuario autenticado para un partido específico.

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "exists": true,
  "prediction": {
    "groupId": "691a192a53148e413d0e49b0",
    "matchId": "group-a-match-1",
    "team1Score": 2,
    "team2Score": 1,
    "predictedDate": "2025-11-20T10:00:00Z",
    "points": 0
  }
}
```

Si no existe predicción:
```json
{
  "exists": false,
  "prediction": null
}
```

---

### 11. Calcular Scores (Solo Creador)

**POST** `/groups/{groupId}/calculate-scores`

Calcula los scores de todas las predicciones comparándolas con los resultados reales. **Solo el creador del grupo puede ejecutar esto.**

**Headers:**
```
Authorization: Bearer {jwt_token}
```

**Response (200 OK):**
```json
{
  "message": "Scores calculated successfully",
  "groupId": "691a192a53148e413d0e49b0",
  "totalMatchesProcessed": 12,
  "userScores": {
    "6908d0864077087454146d5c": 15,
    "68f696e7dc4cf83be1bce269": 8,
    "68f696e7dc4cf83be1bce270": 12
  }
}
```

**⚠️ IMPORTANTE:** Este endpoint:
- Itera todos los partidos jugados en el grupo
- Compara predicciones vs resultados reales
- Asigna puntos según el sistema (3 puntos exacto, 1 punto resultado correcto, 0 puntos incorrecto)
- Actualiza el campo `points` en cada predicción en `auth_service`
- Retorna el total de puntos por usuario

**Cuándo ejecutar:**
- Después de registrar resultados de partidos
- Para recalcular scores si se actualizaron resultados
- Al finalizar una etapa del torneo

---

## 🔄 Flujos de Predicciones

### Flujo 1: Usuario Hace Predicción

1. Usuario selecciona un partido (que aún no se ha jugado)
2. **GET** `/groups/{groupId}/matches/{matchId}` → Ver detalles del partido
3. Usuario ingresa marcadores predichos:
   - `team1Score`: 2
   - `team2Score`: 1
4. **POST** `/groups/{groupId}/matches/{matchId}/predict` → Guarda predicción
5. App muestra confirmación

---

### Flujo 2: Ver Mis Predicciones

1. Usuario entra a un grupo
2. **GET** `/groups/{groupId}/predictions` → Obtiene todas sus predicciones
3. App muestra lista de predicciones con:
   - Partido (equipos)
   - Marcador predicho
   - Estado (jugado/no jugado)
   - Puntos obtenidos (si ya se jugó)

---

### Flujo 3: Calcular Scores (Creador)

1. Creador registra resultados de varios partidos
2. **POST** `/groups/{groupId}/calculate-scores` → Calcula scores
3. Sistema compara todas las predicciones vs resultados reales
4. Actualiza puntos en cada predicción
5. Retorna tabla de clasificación de usuarios

---

## 📊 Ejemplo de Cálculo de Scores

**Partido Real:**
- Equipo 1: 2
- Equipo 2: 1
- Resultado: Equipo 1 gana

**Predicciones de Usuarios:**

| Usuario | Predicción | Puntos Obtenidos | Razón |
|---------|-----------|------------------|-------|
| Usuario A | 2-1 | 3 puntos | Marcador exacto |
| Usuario B | 3-1 | 1 punto | Resultado correcto (equipo 1 gana) |
| Usuario C | 1-1 | 0 puntos | Resultado incorrecto (empate vs victoria) |
| Usuario D | 1-2 | 0 puntos | Resultado incorrecto (equipo 2 gana vs equipo 1) |

---

## 🚀 Próximos Pasos

- Más adelante se automatizará la obtención de resultados desde APIs externas
- Por ahora, los resultados se ingresan manualmente desde el dashboard o la app mobile (solo creador)
- Los scores se calculan manualmente ejecutando el endpoint `/calculate-scores` (solo creador)

---

## 📞 Soporte

Si tienes dudas sobre la integración, revisa:
- `TOURNAMENT_STRUCTURE_EXAMPLE.json` - Ejemplo completo del documento
- `MONGODB_MATCHES_LOCATION.md` - Ubicación exacta de matches en MongoDB
- `GROUP_DOCUMENT_STRUCTURE.md` - Estructura completa del documento

