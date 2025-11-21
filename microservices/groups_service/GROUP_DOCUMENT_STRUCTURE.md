# Estructura del Documento Group en MongoDB

Este documento explica la estructura completa del documento `Group` que se almacena en MongoDB.

## Ubicación del Archivo de Ejemplo

El archivo de ejemplo completo está en:
```
groups_service/TOURNAMENT_STRUCTURE_EXAMPLE.json
```

## Estructura Principal del Documento

```json
{
  "_id": "ObjectId",
  "groupId": "String",
  "creatorUserId": "String",
  "competitionId": "String",
  "competitionName": "String",
  "competitionImage": "String",
  "name": "String - Nombre del grupo",
  "teamIds": ["String - IDs de todos los equipos"],
  "userIds": ["String - IDs de usuarios miembros"],
  "invitedEmails": ["String - Emails invitados"],
  
  "scoreboard": {
    "teams": [] // Legacy - para retrocompatibilidad
  },
  
  "tournamentStructure": {
    "tournamentFormat": "String - groups-then-knockout | only-groups | only-knockout | custom",
    "currentStage": "String - ID de la etapa actual",
    "config": {
      "totalTeams": 32,
      "numberOfGroups": 8,
      "teamsPerGroup": 4,
      "teamsQualifyPerGroup": 2,
      "hasGroupStage": true,
      "hasKnockoutStage": true,
      "knockoutRounds": ["round-of-16", "quarter-finals", "semi-finals", "third-place", "final"]
    },
    "stages": {
      "group-stage": { /* Stage object */ },
      "round-of-16": { /* Stage object */ },
      "quarter-finals": { /* Stage object */ },
      "semi-finals": { /* Stage object */ },
      "third-place": { /* Stage object */ },
      "final": { /* Stage object */ }
    }
  },
  
  "createdAt": "Date",
  "updatedAt": "Date",
  "enabledAt": "Date",
  "disabledAt": "Date"
}
```

## Estructura de un Stage (Etapa)

### Stage de Tipo "groups" (Fase de Grupos)

```json
{
  "stageId": "group-stage",
  "stageName": "Fase de Grupos",
  "type": "groups",
  "isActive": true,
  "isCompleted": false,
  "order": 1,
  "groups": [
    {
      "groupLetter": "A",
      "groupName": "Grupo A",
      "teamsPerGroup": 4,
      "teamsQualify": 2,
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
        // ... más equipos
      ],
      "qualifiedTeamIds": ["team1", "team2"],
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
          "team1Score": 2,
          "team2Score": 1,
          "winnerTeamId": "team1",
          "loserTeamId": "team2",
          "isDraw": false,
          "matchDate": "2025-11-20T15:00:00Z",
          "playedDate": "2025-11-20T17:00:00Z",
          "isPlayed": true,
          "venue": "Estadio Santiago Bernabéu",
          "status": "finished",
          "matchday": 1
        }
        // ... más partidos del grupo
      ]
    }
    // ... más grupos (B, C, D, etc.)
  ],
  "qualifiedTeamIds": ["team1", "team2", "team5", "team6", ...] // Top 2 de cada grupo
}
```

### Stage de Tipo "knockout" (Eliminatorias)

```json
{
  "stageId": "round-of-16",
  "stageName": "Octavos de Final",
  "type": "knockout",
  "isActive": false,
  "isCompleted": false,
  "order": 2,
  "matches": [
    {
      "matchId": "r16-1",
      "matchNumber": "1",
      "stageId": "round-of-16",
      "groupLetter": null,
      "team1Id": "team1",
      "team1Name": "Real Madrid",
      "team1Flag": "🇪🇸",
      "team2Id": "team14",
      "team2Name": "Napoli",
      "team2Flag": "🇮🇹",
      "team1Score": null,
      "team2Score": null,
      "winnerTeamId": null,
      "loserTeamId": null,
      "isDraw": null,
      "matchDate": "2025-12-01T20:00:00Z",
      "playedDate": null,
      "isPlayed": false,
      "venue": null,
      "status": "scheduled",
      "nextMatchId": "qf-1",
      "nextStageId": "quarter-finals",
      "matchday": null
    }
    // ... más partidos (r16-2, r16-3, etc.)
  ],
  "qualifiedTeamIds": [] // Se llena cuando terminan los partidos
}
```

## Ejemplo Completo de un Partido

### Partido de Fase de Grupos (con resultado)

```json
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
  "team1Score": 2,
  "team2Score": 1,
  "winnerTeamId": "team1",
  "loserTeamId": "team2",
  "isDraw": false,
  "matchDate": "2025-11-20T15:00:00Z",
  "playedDate": "2025-11-20T17:00:00Z",
  "isPlayed": true,
  "venue": "Estadio Santiago Bernabéu",
  "status": "finished",
  "matchday": 1,
  "nextMatchId": null,
  "nextStageId": null
}
```

### Partido de Eliminatorias (sin resultado aún)

```json
{
  "matchId": "r16-1",
  "matchNumber": "1",
  "stageId": "round-of-16",
  "groupLetter": null,
  "team1Id": "team1",
  "team1Name": "Real Madrid",
  "team1Flag": "🇪🇸",
  "team2Id": "team14",
  "team2Name": "Napoli",
  "team2Flag": "🇮🇹",
  "team1Score": null,
  "team2Score": null,
  "winnerTeamId": null,
  "loserTeamId": null,
  "isDraw": null,
  "matchDate": "2025-12-01T20:00:00Z",
  "playedDate": null,
  "isPlayed": false,
  "venue": null,
  "status": "scheduled",
  "nextMatchId": "qf-1",
  "nextStageId": "quarter-finals",
  "matchday": null
}
```

## Campos Importantes

### Para Fase de Grupos:
- `matches`: Lista de partidos dentro de cada grupo
- `teams`: Estadísticas de equipos en el grupo
- `qualifiedTeamIds`: IDs de equipos que clasifican (top 2 normalmente)
- `matchday`: Número de jornada dentro del grupo (1, 2, 3, etc.)

### Para Eliminatorias:
- `matches`: Lista de partidos de la ronda
- `nextMatchId`: ID del siguiente partido al que avanza el ganador
- `nextStageId`: ID de la siguiente etapa
- `qualifiedTeamIds`: Se llena automáticamente con los ganadores

## Flujo de Datos

1. **Crear Grupo**: Se inicializa `tournamentStructure` con fase de grupos
2. **Registrar Resultado**: 
   - POST `/groups/{groupId}/matches/{matchId}/result`
   - Body: `{ "team1Score": 2, "team2Score": 1 }`
3. **Actualización Automática**:
   - Estadísticas de equipos se actualizan
   - Posiciones se recalculan
   - Equipos clasificados se actualizan
   - Ganadores avanzan a siguiente ronda (si es eliminatoria)

## Endpoints Relacionados

- `GET /groups/{groupId}/matches` - Obtener todos los partidos
- `GET /groups/{groupId}/matches?stageId=group-stage` - Filtrar por etapa
- `GET /groups/{groupId}/matches?groupLetter=A` - Filtrar por grupo
- `GET /groups/{groupId}/matches/{matchId}` - Obtener un partido
- `POST /groups/{groupId}/matches/{matchId}/result` - Registrar resultado
- `PUT /groups/{groupId}/matches/{matchId}/result` - Actualizar resultado
- `DELETE /groups/{groupId}/matches/{matchId}/result` - Limpiar resultado

