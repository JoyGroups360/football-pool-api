# Ubicación de Matches en el Documento MongoDB

## 📍 Ruta Completa de los Matches

Los matches se guardan en la siguiente ruta dentro del documento `Group`:

```
tournamentStructure
  └── stages
      └── group-stage (o cualquier otra etapa)
          └── groups (solo para fase de grupos)
              └── [grupo]
                  └── matches  ← AQUÍ ESTÁN LOS PARTIDOS
```

## 🔍 Estructura Exacta en MongoDB

### Para Fase de Grupos:

```json
{
  "tournamentStructure": {
    "stages": {
      "group-stage": {
        "groups": [
          {
            "groupLetter": "A",
            "matches": [
              {
                "matchId": "group-a-match-1",
                "team1Id": "team1",
                "team1Name": "Real Madrid",
                "team1Score": 3,  ← RESULTADO AQUÍ
                "team2Id": "team2",
                "team2Name": "Manchester City",
                "team2Score": 1,  ← RESULTADO AQUÍ
                "isPlayed": true,
                "status": "finished"
              }
            ]
          }
        ]
      }
    }
  }
}
```

### Para Eliminatorias:

```json
{
  "tournamentStructure": {
    "stages": {
      "round-of-16": {
        "matches": [
          {
            "matchId": "round-of-16-1",
            "team1Id": "team1",
            "team1Score": 2,  ← RESULTADO AQUÍ
            "team2Id": "team2",
            "team2Score": 1,  ← RESULTADO AQUÍ
            "isPlayed": true
          }
        ]
      }
    }
  }
}
```

## 📝 Ejemplo Completo: Partido con Resultado

### Antes de registrar resultado:

```json
{
  "matchId": "group-a-match-1",
  "team1Id": "team1",
  "team1Name": "Real Madrid",
  "team1Flag": "🇪🇸",
  "team2Id": "team2",
  "team2Name": "Manchester City",
  "team2Flag": "🏴󠁧󠁢󠁥󠁮󠁧󠁿",
  "team1Score": null,  ← Sin resultado
  "team2Score": null,  ← Sin resultado
  "isPlayed": false,
  "status": "scheduled"
}
```

### Después de registrar resultado (POST /groups/{groupId}/matches/{matchId}/result):

```json
{
  "matchId": "group-a-match-1",
  "team1Id": "team1",
  "team1Name": "Real Madrid",
  "team1Flag": "🇪🇸",
  "team2Id": "team2",
  "team2Name": "Manchester City",
  "team2Flag": "🏴󠁧󠁢󠁥󠁮󠁧󠁿",
  "team1Score": 3,  ← RESULTADO GUARDADO
  "team2Score": 1,  ← RESULTADO GUARDADO
  "winnerTeamId": "team1",  ← Ganador automático
  "loserTeamId": "team2",   ← Perdedor automático
  "isDraw": false,
  "isPlayed": true,
  "status": "finished",
  "playedDate": "2025-11-20T15:00:00Z"
}
```

## 🎯 Campos Importantes para Guardar Resultados

Cuando registras un resultado con:
```
POST /groups/{groupId}/matches/{matchId}/result
Body: {
  "team1Score": 3,
  "team2Score": 1,
  "venue": "Estadio Santiago Bernabéu"  // opcional
}
```

Se actualizan automáticamente estos campos en el match:

- ✅ `team1Score`: Marcador del equipo 1
- ✅ `team2Score`: Marcador del equipo 2
- ✅ `winnerTeamId`: ID del ganador (calculado automáticamente)
- ✅ `loserTeamId`: ID del perdedor (calculado automáticamente)
- ✅ `isDraw`: true/false (calculado automáticamente)
- ✅ `isPlayed`: true
- ✅ `status`: "finished"
- ✅ `playedDate`: Fecha actual (o la que envíes)

## 📊 Actualización Automática de Estadísticas

Cuando guardas un resultado, también se actualizan automáticamente las estadísticas de los equipos en:

```
tournamentStructure.stages.group-stage.groups[0].teams[0]
```

Campos que se actualizan:
- `played`: +1
- `won`: +1 (si ganó)
- `drawn`: +1 (si empató)
- `lost`: +1 (si perdió)
- `goalsFor`: +team1Score
- `goalsAgainst`: +team2Score
- `goalDifference`: recalculado
- `points`: +3 (si ganó) o +1 (si empató)
- `position`: recalculado según puntos

## 🔗 Consultar Matches en MongoDB

### Ver todos los matches de un grupo:
```javascript
db.groups.findOne(
  { "groupId": "691a192a53148e413d0e49b0" },
  { "tournamentStructure.stages.group-stage.groups.matches": 1 }
)
```

### Ver matches con resultados:
```javascript
db.groups.findOne(
  { 
    "groupId": "691a192a53148e413d0e49b0",
    "tournamentStructure.stages.group-stage.groups.matches.isPlayed": true
  }
)
```

### Ver un match específico:
```javascript
db.groups.findOne(
  { 
    "groupId": "691a192a53148e413d0e49b0",
    "tournamentStructure.stages.group-stage.groups.matches.matchId": "group-a-match-1"
  },
  { 
    "tournamentStructure.stages.group-stage.groups.$": 1 
  }
)
```

## 📁 Archivo de Ejemplo Completo

Ver: `MONGODB_DOCUMENT_EXAMPLE.json` para un ejemplo completo del documento con matches y resultados guardados.

