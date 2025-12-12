================================================================================
DOCUMENTACIÓN FRONTEND: GUARDAR PREDICCIONES - NUEVA ESTRUCTURA
================================================================================

RESUMEN EJECUTIVO
================================================================================

Las predicciones ahora se organizan por COMPETICIÓN (competitionId), no por grupo.
Una misma predicción funciona para TODOS los grupos de la misma competencia.
El backend calcula automáticamente los puntos comparando predicción vs resultado real.
Los puntos se acumulan por competencia y se actualizan en todos los grupos.

IMPORTANTE:
-----------
- Las predicciones se guardan por COMPETICIÓN (competitionId), no por grupo
- Una predicción es la MISMA para todos los grupos de la misma competencia
- El FE debe enviar los scores REALES para que el backend pueda calcular puntos
- El backend CALCULA automáticamente los puntos comparando predicción vs real
- Los puntos se acumulan por competencia (suma de todos los matches)
- El score en cada grupo se actualiza con los puntos acumulados de la competencia

================================================================================
ENDPOINT
================================================================================

POST /football-pool/v1/api/auth/predictions

Headers:
--------
Authorization: Bearer {jwt_token}
Content-Type: application/json

Body (JSON) - REQUERIDO:
------------------------
{
  "userId": "6908d0864077087454146d5c",        // REQUERIDO: ID del usuario (va en el body)
  "matchId": "group-a-match-1",                // REQUERIDO: ID del partido
  "competitionId": "club-world-cup",           // REQUERIDO: ID de la competencia
  "team1Score": 2,                             // REQUERIDO: Marcador predicho equipo 1
  "team2Score": 1,                             // REQUERIDO: Marcador predicho equipo 2
  "realTeam1Score": 2,                         // REQUERIDO: Marcador real equipo 1
  "realTeam2Score": 1,                         // REQUERIDO: Marcador real equipo 2
  "groupIds": [                                // REQUERIDO: Array con IDs de grupos
    "691d5cc672ce370633800343",
    "691fee9cd05a2b08479ce37b",
    "6923da8daf258e4c81793b31"
  ],
  "extraTime": false,                          // OPCIONAL: Si predijiste tiempo extra
  "realExtraTime": false,                      // OPCIONAL: Si hubo tiempo extra real
  "penaltiesteam1Score": null,                 // OPCIONAL: Penales predichos equipo 1
  "penaltiesteam2Score": null                  // OPCIONAL: Penales predichos equipo 2
}

⚠️ CAMPOS REQUERIDOS (TODOS VAN EN EL BODY):
---------------------------------------------
✅ userId: String - ID del usuario
✅ matchId: String - ID del partido
✅ competitionId: String - ID de la competencia (ej: "club-world-cup", "champions-league")
✅ team1Score: Number - Marcador predicho para equipo 1
✅ team2Score: Number - Marcador predicho para equipo 2
✅ realTeam1Score: Number - Marcador REAL para equipo 1 (para cálculo de puntos)
✅ realTeam2Score: Number - Marcador REAL para equipo 2 (para cálculo de puntos)
✅ groupIds: Array<String> - IDs de TODOS los grupos donde aplicar la predicción
   IMPORTANTE: Debe enviar TODOS los grupos de la misma competencia donde el usuario participa

📝 CAMPOS OPCIONALES:
---------------------
- extraTime: Boolean - Si predijiste tiempo extra (solo fases de eliminación)
- realExtraTime: Boolean - Si hubo tiempo extra real (solo fases de eliminación)
- penaltiesteam1Score: Number | null - Penales predichos equipo 1
- penaltiesteam2Score: Number | null - Penales predichos equipo 2

NOTA: Si el partido aún no se ha jugado, enviar 0 en realTeam1Score y realTeam2Score.
Los puntos se calcularán cuando se actualicen los resultados reales.

================================================================================
ESTRUCTURA DE DATOS EN BACKEND
================================================================================

PREDICCIÓN EN USERS (users.predictions):
------------------------------------------
{
  "_id": "6908d0864077087454146d5c",
  "predictions": {
    "club-world-cup": {
      "matchInfo": [
        {
          "matchId": "group-a-match-1",
          "team1Score": 2,
          "team2Score": 1,
          "realTeam1Score": 2,
          "realTeam2Score": 1,
          "extraTime": false,
          "realExtraTime": false,
          "penaltiesteam1Score": null,
          "penaltiesteam2Score": null,
          "predictedDate": "2025-12-05T16:16:24.919Z"
        },
        {
          "matchId": "group-a-match-2",
          "team1Score": 3,
          "team2Score": 0,
          "realTeam1Score": 2,
          "realTeam2Score": 1,
          "extraTime": false,
          "realExtraTime": false,
          "penaltiesteam1Score": null,
          "penaltiesteam2Score": null,
          "predictedDate": "2025-12-05T16:20:15.123Z"
        }
      ],
      "points": 8  // ← ACUMULADO: 5 (match 1 exacto) + 3 (match 2 resultado correcto) = 8
    },
    "champions-league": {
      "matchInfo": [
        {
          "matchId": "round-of-16-1",
          "team1Score": 1,
          "team2Score": 1,
          "realTeam1Score": 1,
          "realTeam2Score": 1,
          "extraTime": true,
          "realExtraTime": true,
          "penaltiesteam1Score": 4,
          "penaltiesteam2Score": 3,
          "predictedDate": "2025-12-05T16:25:30.456Z"
        }
      ],
      "points": 11  // ← 5 (exacto) + 1 (extra time) + 2 (penalties) + 3 (exacto penalties) = 11
    }
  }
}

IMPORTANTE:
-----------
- Las predicciones están organizadas por competitionId (clave del objeto)
- Cada competencia tiene un array matchInfo con todos los partidos
- Los puntos (points) son ACUMULADOS: suma de todos los matches de esa competencia
- Una misma predicción funciona para TODOS los grupos de la misma competencia

ESTRUCTURA EN GROUPS (groups):
-------------------------------
{
  "groupId": "691d5cc672ce370633800343",
  "name": "Chiqui",
  "competitionId": "club-world-cup",
  "matchesDetail": [  // ← Array de matches (igual a matchInfo de la competencia)
    {
      "matchId": "group-a-match-1",
      "team1Score": 2,
      "team2Score": 1,
      "realTeam1Score": 2,
      "realTeam2Score": 1,
      "extraTime": false,
      "realExtraTime": false,
      "penaltiesteam1Score": null,
      "penaltiesteam2Score": null,
      "predictedDate": "2025-12-05T16:16:24.919Z"
    },
    {
      "matchId": "group-a-match-2",
      "team1Score": 3,
      "team2Score": 0,
      "realTeam1Score": 2,
      "realTeam2Score": 1,
      "predictedDate": "2025-12-05T16:20:15.123Z"
    }
  ],
  "users": [  // ← Lista de usuarios en el grupo
    {
      "_id": "6908d0864077087454146d5c",
      "userId": "6908d0864077087454146d5c",
      "nombre": "Juan",
      "score": 8,  // ← Puntos acumulados de la competencia
      "matchesInfo": [  // ← matchInfo del usuario para esta competencia
        {
          "matchId": "group-a-match-1",
          "team1Score": 2,
          "team2Score": 1,
          "realTeam1Score": 2,
          "realTeam2Score": 1,
          "predictedDate": "2025-12-05T16:16:24.919Z"
        },
        {
          "matchId": "group-a-match-2",
          "team1Score": 3,
          "team2Score": 0,
          "realTeam1Score": 2,
          "realTeam2Score": 1,
          "predictedDate": "2025-12-05T16:20:15.123Z"
        }
      ]
    },
    {
      "_id": "6908d55a4077087454146d5e",
      "userId": "6908d55a4077087454146d5e",
      "nombre": "Joel",
      "score": 5,
      "matchesInfo": [
        {
          "matchId": "group-a-match-1",
          "team1Score": 0,
          "team2Score": 5,
          "realTeam1Score": 0,
          "realTeam2Score": 0,
          "predictedDate": "2025-12-05T17:47:14.553Z"
        }
      ]
    }
  ]
}

IMPORTANTE:
-----------
- `matchesDetail`: Array de matches a nivel de grupo (igual a matchInfo de la competencia)
- `users[].matchesInfo`: Array de matches del usuario específico para esta competencia
- `users[].score`: Puntos acumulados del usuario para la competencia
- `users[].userId`: ID del usuario (lo envía el FE)
- Si el usuario ya existe en `users[]`, se actualiza; si no existe, se crea

================================================================================
RESPUESTA DEL BACKEND
================================================================================

200 OK:
-------
{
  "message": "Prediction saved successfully",
  "userId": "6908d0864077087454146d5c",
  "matchId": "group-a-match-1",
  "competitionId": "club-world-cup",
  "team1Score": 2,
  "team2Score": 1,
  "realTeam1Score": 2,
  "realTeam2Score": 1,
  "points": 8,                              // ← PUNTOS ACUMULADOS de la competencia
  "groupsApplied": [
    {
      "groupId": "691d5cc672ce370633800343",
      "groupName": "Los Mejores",
      "pointsCalculated": true,
      "points": 8,                            // ← Mismos puntos (acumulados de la competencia)
      "totalScore": 8                         // ← Score total del usuario en ese grupo
    },
    {
      "groupId": "691fee9cd05a2b08479ce37b",
      "groupName": "Grupo de Amigos",
      "pointsCalculated": true,
      "points": 8,                            // ← Mismos puntos
      "totalScore": 8
    }
  ]
}

Campos de respuesta:
--------------------
- message: Mensaje de confirmación
- userId: ID del usuario
- matchId: ID del partido
- competitionId: ID de la competencia
- team1Score: Marcador predicho para equipo 1
- team2Score: Marcador predicho para equipo 2
- realTeam1Score: Marcador real para equipo 1
- realTeam2Score: Marcador real para equipo 2
- points: Puntos ACUMULADOS de la competencia (suma de todos los matches)
- groupsApplied: Array con información de cada grupo donde se aplicó la predicción
  - groupId: ID del grupo
  - groupName: Nombre del grupo
  - pointsCalculated: true (siempre true, puntos se calculan automáticamente)
  - points: Puntos acumulados de la competencia
  - totalScore: Score total del usuario en ese grupo (igual a points)

400 Bad Request:
----------------
{
  "error": "All fields are required: userId, matchId, team1Score, team2Score, realTeam1Score, realTeam2Score, groupIds, competitionId"
}

O también puede ser:
{
  "error": "userId is required in the body"
}

O:
{
  "error": "groupIds array is required"
}
}

500 Internal Server Error:
--------------------------
{
  "error": "Error saving prediction: [detalles del error]"
}

================================================================================
TABLA DE PUNTUACIÓN
================================================================================

PUNTOS BASE:
------------
• Marcador exacto: 5 puntos
  Ejemplo: Predices 2-1 y el resultado es 2-1 → 5 puntos

• Resultado correcto (ganar/empatar/perder) sin marcador exacto: 3 puntos
  Ejemplo: Predices 2-1 (gana equipo 1) y el resultado es 3-0 (gana equipo 1) → 3 puntos
  Ejemplo: Predices 1-1 (empate) y el resultado es 2-2 (empate) → 3 puntos

• Resultado incorrecto: 0 puntos
  Ejemplo: Predices 2-1 (gana equipo 1) y el resultado es 1-2 (gana equipo 2) → 0 puntos

PUNTOS ADICIONALES (Solo Fases de Eliminación):
------------------------------------------------
• +1 punto si predijiste tiempo extra y ocurrió
  Ejemplo: Predices extraTime: true y el partido va a tiempo extra → +1 punto

• +2 puntos si predijiste penales y ocurrieron
  Ejemplo: Predices penaltiesteam1Score: 4, penaltiesteam2Score: 3 
           y el partido se define por penales → +2 puntos

• +3 puntos si predijiste el marcador exacto de penales
  Ejemplo: Predices penaltiesteam1Score: 4, penaltiesteam2Score: 3 
           y los penales terminan 4-3 → +3 puntos adicionales

NOTA IMPORTANTE:
----------------
• El backend hace TODOS los cálculos automáticamente
• El frontend SOLO envía las predicciones y los resultados reales
• El frontend NO debe calcular puntos manualmente
• Los puntos se calculan en el momento de guardar
• Los puntos se ACUMULAN por competencia (suma de todos los matches)

================================================================================
EJEMPLOS DE USO
================================================================================

Ejemplo 1: Predicción simple (Fase de Grupos) - Partido ya jugado
--------------------------------------------------------------------
POST /football-pool/v1/api/auth/predictions

{
  "userId": "6908d0864077087454146d5c",
  "matchId": "group-a-match-1",
  "competitionId": "club-world-cup",
  "team1Score": 2,
  "team2Score": 1,
  "realTeam1Score": 2,
  "realTeam2Score": 1,
  "groupIds": [
    "691d5cc672ce370633800343",
    "691fee9cd05a2b08479ce37b"
  ]
}

Respuesta:
{
  "message": "Prediction saved successfully",
  "userId": "6908d0864077087454146d5c",
  "matchId": "group-a-match-1",
  "competitionId": "club-world-cup",
  "team1Score": 2,
  "team2Score": 1,
  "realTeam1Score": 2,
  "realTeam2Score": 1,
  "points": 5,                      // ← 5 puntos (marcador exacto)
  "groupsApplied": [
    {
      "groupId": "691d5cc672ce370633800343",
      "groupName": "Los Mejores",
      "pointsCalculated": true,
      "points": 5,
      "totalScore": 5
    },
    {
      "groupId": "691fee9cd05a2b08479ce37b",
      "groupName": "Grupo de Amigos",
      "pointsCalculated": true,
      "points": 5,
      "totalScore": 5
    }
  ]
}

Ejemplo 2: Predicción con tiempo extra y penales (Fase de Eliminación)
-----------------------------------------------------------------------
POST /football-pool/v1/api/auth/predictions

{
  "userId": "6908d0864077087454146d5c",
  "matchId": "round-of-16-1",
  "competitionId": "club-world-cup",
  "team1Score": 1,
  "team2Score": 1,
  "realTeam1Score": 1,
  "realTeam2Score": 1,
  "extraTime": true,
  "realExtraTime": true,
  "penaltiesteam1Score": 4,
  "penaltiesteam2Score": 3,
  "groupIds": ["691d5cc672ce370633800343"]
}

Respuesta (si los penales fueron 4-3):
{
  "message": "Prediction saved successfully",
  "userId": "6908d0864077087454146d5c",
  "matchId": "round-of-16-1",
  "competitionId": "club-world-cup",
  "team1Score": 1,
  "team2Score": 1,
  "realTeam1Score": 1,
  "realTeam2Score": 1,
  "extraTime": true,
  "realExtraTime": true,
  "penaltiesteam1Score": 4,
  "penaltiesteam2Score": 3,
  "points": 11,                     // ← 5 (exacto) + 1 (extra time) + 2 (penalties) + 3 (exacto penalties) = 11
  "groupsApplied": [
    {
      "groupId": "691d5cc672ce370633800343",
      "groupName": "Los Mejores",
      "pointsCalculated": true,
      "points": 11,
      "totalScore": 11
    }
  ]
}

Ejemplo 3: Predicción para un partido que aún no se ha jugado
--------------------------------------------------------------
POST /football-pool/v1/api/auth/predictions

{
  "userId": "6908d0864077087454146d5c",
  "matchId": "group-b-match-5",
  "competitionId": "club-world-cup",
  "team1Score": 3,
  "team2Score": 2,
  "realTeam1Score": 0,              // ← 0 porque aún no se ha jugado
  "realTeam2Score": 0,               // ← 0 porque aún no se ha jugado
  "groupIds": ["691d5cc672ce370633800343"]
}

Respuesta:
{
  "message": "Prediction saved successfully",
  "userId": "6908d0864077087454146d5c",
  "matchId": "group-b-match-5",
  "competitionId": "club-world-cup",
  "team1Score": 3,
  "team2Score": 2,
  "realTeam1Score": 0,
  "realTeam2Score": 0,
  "points": 0,                       // ← 0 puntos hasta que se actualice el resultado real
  "groupsApplied": [
    {
      "groupId": "691d5cc672ce370633800343",
      "groupName": "Los Mejores",
      "pointsCalculated": true,
      "points": 0,
      "totalScore": 0
    }
  ]
}

NOTA: Cuando el partido se juegue, el frontend debe actualizar la predicción
enviando los resultados reales para que se calculen los puntos.

Ejemplo 4: Múltiples partidos de la misma competencia
------------------------------------------------------
Primera predicción:
POST /football-pool/v1/api/auth/predictions

{
  "userId": "6908d0864077087454146d5c",
  "matchId": "group-a-match-1",
  "competitionId": "club-world-cup",
  "team1Score": 2,
  "team2Score": 1,
  "realTeam1Score": 2,
  "realTeam2Score": 1,
  "groupIds": ["691d5cc672ce370633800343"]
}

Respuesta: points: 5 (marcador exacto)

Segunda predicción (mismo competitionId):
POST /football-pool/v1/api/auth/predictions

{
  "userId": "6908d0864077087454146d5c",
  "matchId": "group-a-match-2",
  "competitionId": "club-world-cup",
  "team1Score": 3,
  "team2Score": 0,
  "realTeam1Score": 2,
  "realTeam2Score": 1,
  "groupIds": ["691d5cc672ce370633800343"]
}

Respuesta: points: 8 (5 del match 1 + 3 del match 2 = 8 puntos acumulados)

================================================================================
FLUJO RECOMENDADO
================================================================================

1. Usuario ingresa marcadores predichos en el formulario
2. Frontend obtiene los resultados reales del partido (si ya se jugó)
3. Frontend obtiene los groupIds de los grupos donde está el usuario
4. Frontend agrupa los groupIds por competitionId
5. Frontend envía la predicción con:
   - matchId, competitionId
   - team1Score, team2Score (predicciones)
   - realTeam1Score, realTeam2Score (resultados reales)
   - groupIds (todos los grupos de esa competencia)
6. Backend guarda la predicción por competitionId
7. Backend CALCULA automáticamente los puntos comparando predicción vs real
8. Backend ACUMULA los puntos de todos los matches de la competencia
9. Backend actualiza cada grupo con los puntos acumulados
10. Frontend recibe respuesta con puntos calculados
11. Frontend muestra confirmación al usuario con los puntos obtenidos

================================================================================
OBTENER PREDICCIONES
================================================================================

GET /football-pool/v1/api/auth/{userId}/predictions?groupId={groupId}

Respuesta:
{
  "userId": "6908d0864077087454146d5c",
  "groupId": "691d5cc672ce370633800343",
  "predictions": {
    "club-world-cup": {
      "matchInfo": [
        {
          "matchId": "group-a-match-1",
          "team1Score": 2,
          "team2Score": 1,
          "realTeam1Score": 2,
          "realTeam2Score": 1,
          "extraTime": false,
          "realExtraTime": false,
          "penaltiesteam1Score": null,
          "penaltiesteam2Score": null,
          "predictedDate": "2025-12-05T16:16:24.919Z"
        }
      ],
      "points": 5
    }
  },
  "count": 1  // Número de competencias con predicciones
}

NOTA: Si no se proporciona groupId, se devuelven todas las predicciones del usuario.

================================================================================
OBTENER INFORMACIÓN DEL GRUPO (CON matchesDetail Y users)
================================================================================

GET /football-pool/v1/api/groups/{groupId}

Headers:
--------
Authorization: Bearer {jwt_token}

Respuesta:
{
  "groupId": "691d5cc672ce370633800343",
  "name": "Chiqui",
  "competitionId": "club-world-cup",
  "matchesDetail": [  // ← Array de matches actualizado
    {
      "matchId": "group-a-match-1",
      "team1Score": 2,
      "team2Score": 1,
      "realTeam1Score": 2,
      "realTeam2Score": 1,
      "predictedDate": "2025-12-05T16:16:24.919Z"
    }
  ],
  "users": [  // ← Lista de usuarios con score y matchesInfo
    {
      "_id": "6908d0864077087454146d5c",
      "userId": "6908d0864077087454146d5c",
      "nombre": "Juan",
      "score": 8,
      "matchesInfo": [
        {
          "matchId": "group-a-match-1",
          "team1Score": 2,
          "team2Score": 1,
          "realTeam1Score": 2,
          "realTeam2Score": 1,
          "predictedDate": "2025-12-05T16:16:24.919Z"
        }
      ]
    }
  ]
}

IMPORTANTE:
-----------
- `matchesDetail`: Se actualiza automáticamente cuando cualquier usuario guarda una predicción
- `users[].matchesInfo`: Contiene las predicciones del usuario específico
- `users[].score`: Puntos acumulados del usuario para la competencia
- Si el usuario no existe en `users[]`, se crea automáticamente al guardar una predicción

================================================================================
VALIDACIONES DEL FRONTEND
================================================================================

1. Campos requeridos:
   - matchId: Debe ser un string válido
   - competitionId: Debe ser un string válido (ej: "club-world-cup")
   - team1Score: Debe ser un número entero >= 0
   - team2Score: Debe ser un número entero >= 0
   - realTeam1Score: Debe ser un número entero >= 0 (0 si no se ha jugado)
   - realTeam2Score: Debe ser un número entero >= 0 (0 si no se ha jugado)
   - groupIds: Debe ser un array con al menos un groupId

2. Campos opcionales:
   - extraTime: Boolean (solo para fases de eliminación)
   - realExtraTime: Boolean (solo para fases de eliminación)
   - penaltiesteam1Score: Number | null (solo para fases de eliminación)
   - penaltiesteam2Score: Number | null (solo para fases de eliminación)

3. Validaciones adicionales:
   - Los marcadores deben ser números enteros no negativos
   - Si el partido ya se jugó, enviar los resultados reales
   - Si el partido no se ha jugado, enviar 0 en realTeam1Score y realTeam2Score
   - Los puntos se recalcularán automáticamente cuando se actualice el resultado

================================================================================
NOTAS IMPORTANTES
================================================================================

1. PREDICCIÓN POR COMPETICIÓN:
   - La predicción se guarda por COMPETICIÓN (competitionId), no por grupo
   - Se aplica a todos los grupos que envíes en groupIds
   - Una misma predicción funciona para TODOS los grupos de la misma competencia
   - No crea múltiples predicciones para el mismo partido

2. ACTUALIZACIÓN:
   - Si ya existe una predicción para ese matchId en esa competencia, se actualiza
   - Si cambias el marcador, se actualiza y se recalculan los puntos
   - Los puntos se recalculan automáticamente al guardar

3. CÁLCULO AUTOMÁTICO DE PUNTOS:
   - El backend CALCULA AUTOMÁTICAMENTE los puntos
   - Compara el marcador predicho vs el marcador real
   - Usa la tabla de puntuación (5 exacto, 3 resultado correcto, 0 incorrecto)
   - Agrega puntos adicionales para fases de eliminación
   - El frontend NO debe calcular puntos manualmente

4. PUNTOS ACUMULADOS:
   - Los puntos se ACUMULAN por competencia (suma de todos los matches)
   - El totalScore por grupo es igual a los puntos acumulados de la competencia
   - Los puntos se actualizan automáticamente cuando se actualiza un match

5. ACTUALIZACIÓN DE GRUPOS:
   - Cuando guardas una predicción, el backend actualiza AUTOMÁTICAMENTE cada grupo:
     * `matchesDetail`: Se actualiza con el matchInfo completo de la competencia
     * `users[].score`: Se actualiza con los puntos acumulados de la competencia
     * `users[].matchesInfo`: Se actualiza con el matchInfo del usuario
   - Si el usuario NO existe en `users[]`, se crea automáticamente
   - Si el usuario YA existe, se actualiza su score y matchesInfo

6. SCOREBOARD:
   - Se actualiza automáticamente después de calcular puntos
   - El score del usuario en el grupo es igual a los puntos acumulados de la competencia
   - El orden se basa en: score (desc), diferencia de goles (desc), goles a favor (desc)

7. FRONTEND:
   - El frontend NO debe calcular puntos
   - El frontend solo debe enviar las predicciones y resultados reales
   - Los puntos se calculan y actualizan automáticamente en el backend
   - El frontend debe mostrar los puntos que recibe en la respuesta
   - El frontend debe enviar TODOS los groupIds donde quiere aplicar la predicción

================================================================================
RESUMEN PARA EL FRONTEND
================================================================================

LO QUE DEBE HACER EL FRONTEND:
-------------------------------
✅ Capturar la predicción del usuario (marcadores predichos)
✅ Obtener los resultados reales del partido (si ya se jugó, sino enviar 0)
✅ Obtener el userId del usuario autenticado
✅ Obtener los groupIds donde el usuario está participando
✅ Agrupar los groupIds por competitionId
✅ Enviar la predicción al endpoint:
   - URL: POST /football-pool/v1/api/auth/predictions
   - Body: {
       userId,                    // ← REQUERIDO: ID del usuario
       matchId, competitionId,
       team1Score, team2Score (predicciones),
       realTeam1Score, realTeam2Score (resultados reales),
       groupIds (todos los grupos de esa competencia)
     }
✅ Mostrar la respuesta al usuario (puntos calculados, grupos actualizados)

LO QUE NO DEBE HACER EL FRONTEND:
----------------------------------
❌ Calcular puntos manualmente
❌ Comparar marcadores predichos con reales
❌ Implementar la lógica de puntuación
❌ Guardar predicciones por separado en cada grupo
❌ Enviar solo un groupId (debe enviar TODOS los grupos de la misma competencia)

LO QUE HACE EL BACKEND AUTOMÁTICAMENTE:
----------------------------------------
✅ Guarda la predicción por competitionId en users.predictions
✅ Compara marcador predicho vs marcador real
✅ Calcula los puntos según la tabla de puntuación
✅ Acumula los puntos de todos los matches de la competencia
✅ Actualiza cada grupo enviado en groupIds con:
   - matchesDetail: Array de matches (igual a matchInfo)
   - users[].score: Puntos acumulados de la competencia
   - users[].matchesInfo: matchInfo del usuario
✅ Crea el usuario en users[] si no existe
✅ Actualiza el usuario en users[] si ya existe
✅ Actualiza los scoreboards

================================================================================
EJEMPLO DE CÓDIGO FRONTEND (TypeScript/JavaScript)
================================================================================

// Función para guardar predicción
// IMPORTANTE: userId y groupIds van en el body
async function savePrediction(
  userId: string,        // ← Va en el body
  matchId: string,
  competitionId: string,
  team1Score: number,
  team2Score: number,
  realTeam1Score: number,
  realTeam2Score: number,
  groupIds: string[],   // ← Va en el body: Array con TODOS los grupos de la misma competencia
  extraTime?: boolean,
  realExtraTime?: boolean,
  penaltiesteam1Score?: number | null,
  penaltiesteam2Score?: number | null
) {
  try {
    // Todos los campos van en el body, incluyendo userId
    const response = await fetch(
      `/football-pool/v1/api/auth/predictions`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({
          userId,        // ← REQUERIDO: ID del usuario (va en el body)
          matchId,
          competitionId,
          team1Score,
          team2Score,
          realTeam1Score,
          realTeam2Score,
          groupIds,  // ← IMPORTANTE: Array con TODOS los grupos de la misma competencia
          ...(extraTime !== undefined && { extraTime }),
          ...(realExtraTime !== undefined && { realExtraTime }),
          ...(penaltiesteam1Score !== null && penaltiesteam1Score !== undefined && { penaltiesteam1Score }),
          ...(penaltiesteam2Score !== null && penaltiesteam2Score !== undefined && { penaltiesteam2Score })
        })
      }
    );

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Error saving prediction');
    }

    const data = await response.json();
    
    // Mostrar confirmación al usuario
    console.log(`✅ Predicción guardada. Puntos: ${data.points}`);
    console.log(`✅ Grupos actualizados: ${data.groupsApplied.length}`);
    
    // data.groupsApplied contiene información de cada grupo actualizado
    data.groupsApplied.forEach((group: any) => {
      console.log(`  - ${group.groupName}: ${group.points} puntos`);
    });
    
    return data;
  } catch (error) {
    console.error('❌ Error saving prediction:', error);
    throw error;
  }
}

// Ejemplo de uso
// IMPORTANTE: Agrupar los groupIds por competitionId antes de enviar
const userId = '6908d0864077087454146d5c';  // ← Obtener del usuario autenticado
const groupIds = ['691d5cc672ce370633800343', '691fee9cd05a2b08479ce37b']; // Todos de la misma competencia

await savePrediction(
  userId,              // ← userId va en el body
  'group-a-match-1',
  'club-world-cup',
  2,  // team1Score
  1,  // team2Score
  2,  // realTeam1Score
  1,  // realTeam2Score
  groupIds
);

// Ejemplo: Obtener información del grupo actualizado
async function getGroupInfo(groupId: string) {
  try {
    const response = await fetch(
      `/football-pool/v1/api/groups/${groupId}`,
      {
        headers: {
          'Authorization': `Bearer ${token}`
        }
      }
    );
    
    if (!response.ok) {
      throw new Error('Error getting group info');
    }
    
    const data = await response.json();
    
    // El grupo ahora tiene:
    // - matchesDetail: Array de matches
    // - users[]: Array de usuarios con score y matchesInfo
    console.log('Matches Detail:', data.matchesDetail);
    console.log('Users:', data.users);
    
    // Encontrar el usuario actual
    const currentUser = data.users?.find((u: any) => u.userId === userId);
    if (currentUser) {
      console.log('Tu score:', currentUser.score);
      console.log('Tus matches:', currentUser.matchesInfo);
    }
    
    return data;
  } catch (error) {
    console.error('Error getting group info:', error);
    throw error;
  }
}

================================================================================
FIN DE DOCUMENTACIÓN
================================================================================

