# 🎯 Guía Rápida: Guardar Predicciones (Frontend)

## 📋 Resumen Ejecutivo

El backend **HACE TODO AUTOMÁTICAMENTE**:
- Guarda la predicción UNA vez
- Calcula los puntos comparando marcador predicho vs real
- Actualiza todos los grupos enviados
- Actualiza los scoreboards

## 🔌 Endpoint

```
POST /football-pool/v1/api/auth/{userId}/predictions
```

**Headers:**
```json
{
  "Authorization": "Bearer {jwt_token}",
  "Content-Type": "application/json"
}
```

## 📤 Request Body (Lo que el FE envía)

### Fase de Grupos
```json
{
  "matchId": "group-a-match-1",
  "team1Score": 2,
  "team2Score": 1,
  "groupIds": [
    "691d5cc672ce370633800343",
    "691fee9cd05a2b08479ce37b"
  ],
  "competitionId": "club-world-cup",     // OPCIONAL pero recomendado
  "category": "fifaOfficialClubCups"     // OPCIONAL pero recomendado
}
```

### Fase de Eliminación (con tiempo extra y penales)
```json
{
  "matchId": "round-of-16-1",
  "team1Score": 1,
  "team2Score": 1,
  "groupIds": ["691d5cc672ce370633800343"],
  "userExtraTime": true,                 // OPCIONAL
  "userPenalties": true,                 // OPCIONAL
  "userPenaltiesTeam1Score": 4,          // OPCIONAL
  "userPenaltiesTeam2Score": 3,          // OPCIONAL
  "competitionId": "club-world-cup",
  "category": "fifaOfficialClubCups"
}
```

### Campos Requeridos
- `matchId` (string)
- `team1Score` (integer >= 0)
- `team2Score` (integer >= 0)
- `groupIds` (array de strings, mínimo 1)

### Campos Opcionales
- `competitionId` (string) - Ayuda al BE a calcular puntos
- `category` (string) - Ayuda al BE a calcular puntos
- `userExtraTime` (boolean) - Solo para eliminación
- `userPenalties` (boolean) - Solo para eliminación
- `userPenaltiesTeam1Score` (integer) - Solo para eliminación
- `userPenaltiesTeam2Score` (integer) - Solo para eliminación

## 📥 Response (Lo que el BE devuelve)

```json
{
  "message": "Prediction saved successfully",
  "userId": "6908d0864077087454146d5c",
  "matchId": "group-a-match-1",
  "team1Score": 2,
  "team2Score": 1,
  "groupsApplied": [
    {
      "groupId": "691d5cc672ce370633800343",
      "groupName": "Los Mejores",
      "pointsCalculated": true,
      "points": 5,                    // ← CALCULADO AUTOMÁTICAMENTE
      "totalScore": 15
    },
    {
      "groupId": "691fee9cd05a2b08479ce37b",
      "groupName": "Grupo de Amigos",
      "pointsCalculated": true,
      "points": 5,
      "totalScore": 15
    }
  ]
}
```

## 🏆 Tabla de Puntuación (Backend calcula automáticamente)

### Puntos Base

| Condición | Puntos | Ejemplo |
|-----------|--------|---------|
| **Marcador exacto** | **5** | Predices 2-1, resultado 2-1 → 5 pts |
| **Resultado correcto** | **3** | Predices 2-1 (gana T1), resultado 3-0 (gana T1) → 3 pts |
| **Resultado incorrecto** | **0** | Predices 2-1 (gana T1), resultado 1-2 (gana T2) → 0 pts |

### Puntos Adicionales (Solo Eliminación)

| Condición | Puntos | Ejemplo |
|-----------|--------|---------|
| Predijiste tiempo extra correctamente | +1 | userExtraTime: true y ocurrió → +1 |
| Predijiste penales correctamente | +2 | userPenalties: true y ocurrió → +2 |
| Marcador exacto de penales | +3 | 4-3 predicho y 4-3 real → +3 |

### Ejemplo Completo
```
Predicción: 1-1 en tiempo regular
Real: 1-1 en tiempo regular
Predicción: Va a tiempo extra ✓
Real: Fue a tiempo extra ✓
Predicción: Va a penales ✓
Real: Fue a penales ✓
Predicción penales: 4-3
Real penales: 4-3 ✓

Puntos totales: 5 + 1 + 2 + 3 = 11 puntos
```

## ✅ Lo que el FE DEBE hacer

1. **Capturar predicción del usuario**
   - Obtener team1Score y team2Score
   - Si es eliminación, capturar userExtraTime, userPenalties, etc.

2. **Obtener los groupIds**
   - Obtener todos los grupos donde el usuario participa para esa competición

3. **Enviar al endpoint**
   ```javascript
   const response = await fetch(`/football-pool/v1/api/auth/${userId}/predictions`, {
     method: 'POST',
     headers: {
       'Authorization': `Bearer ${token}`,
       'Content-Type': 'application/json'
     },
     body: JSON.stringify({
       matchId: "group-a-match-1",
       team1Score: 2,
       team2Score: 1,
       groupIds: ["groupId1", "groupId2"],
       competitionId: "club-world-cup",
       category: "fifaOfficialClubCups"
     })
   });
   ```

4. **Mostrar resultado**
   - Mostrar mensaje de éxito
   - Mostrar puntos obtenidos (si `pointsCalculated: true`)
   - Mostrar en qué grupos se aplicó

## ❌ Lo que el FE NO debe hacer

- ❌ **NO calcular puntos manualmente**
- ❌ **NO comparar marcadores predichos vs reales**
- ❌ **NO implementar la lógica de puntuación**
- ❌ **NO hacer múltiples llamadas (una por grupo)**

## 🔄 Flujo Completo

```
Usuario → Ingresa predicción → FE valida datos → FE obtiene groupIds
    ↓
FE envía al endpoint con todos los groupIds
    ↓
BE guarda predicción UNA vez en users.predictions[]
    ↓
BE busca resultado real del partido automáticamente
    ↓
BE compara marcador predicho vs real
    ↓
BE CALCULA PUNTOS automáticamente (5, 3, 0 + bonos)
    ↓
BE actualiza cada grupo en groups.userPredictions[]
    ↓
BE actualiza scoreboards automáticamente
    ↓
BE devuelve respuesta con puntos calculados
    ↓
FE muestra resultado al usuario
```

## 💡 Puntos Clave

1. **Una sola predicción por partido**
   - La predicción se guarda UNA vez
   - Se aplica a TODOS los grupos enviados en `groupIds`

2. **Cálculo automático**
   - El BE calcula los puntos automáticamente
   - Compara predicción vs resultado real
   - El FE solo muestra los puntos que recibe

3. **Mismos puntos en todos los grupos**
   - Como es la misma predicción, los puntos son iguales en todos los grupos
   - El `totalScore` puede variar por grupo (suma de todas las predicciones de ese grupo)

4. **Actualización de predicción**
   - Si envías la misma predicción de nuevo, se actualiza
   - Los puntos se recalculan automáticamente

## 🚨 Validaciones Frontend

```javascript
// Validar campos requeridos
if (!matchId || team1Score === null || team2Score === null || !groupIds || groupIds.length === 0) {
  throw new Error('Campos requeridos: matchId, team1Score, team2Score, groupIds');
}

// Validar que los scores sean números >= 0
if (team1Score < 0 || team2Score < 0) {
  throw new Error('Los scores deben ser >= 0');
}

// Validar que groupIds sea un array
if (!Array.isArray(groupIds)) {
  throw new Error('groupIds debe ser un array');
}
```

## 📊 Ejemplo de UI Recomendada

```jsx
<PredictionForm>
  <h3>Predicción para {team1Name} vs {team2Name}</h3>
  
  <ScoreInput>
    <label>{team1Name}</label>
    <input type="number" min="0" value={team1Score} />
  </ScoreInput>
  
  <ScoreInput>
    <label>{team2Name}</label>
    <input type="number" min="0" value={team2Score} />
  </ScoreInput>
  
  {isKnockoutStage && (
    <>
      <Checkbox>
        <input type="checkbox" checked={userExtraTime} />
        <label>¿Irá a tiempo extra?</label>
      </Checkbox>
      
      <Checkbox>
        <input type="checkbox" checked={userPenalties} />
        <label>¿Irá a penales?</label>
      </Checkbox>
      
      {userPenalties && (
        <>
          <PenaltiesInput>
            <label>Penales {team1Name}</label>
            <input type="number" min="0" value={userPenaltiesTeam1Score} />
          </PenaltiesInput>
          
          <PenaltiesInput>
            <label>Penales {team2Name}</label>
            <input type="number" min="0" value={userPenaltiesTeam2Score} />
          </PenaltiesInput>
        </>
      )}
    </>
  )}
  
  <Button onClick={handleSubmit}>Guardar Predicción</Button>
</PredictionForm>

{/* Mostrar respuesta */}
{response && (
  <ResultMessage>
    <h4>✅ Predicción guardada exitosamente</h4>
    {response.groupsApplied.map(group => (
      <GroupResult key={group.groupId}>
        <p>Grupo: {group.groupName}</p>
        {group.pointsCalculated ? (
          <p>Puntos obtenidos: {group.points}</p>
        ) : (
          <p>Puntos se calcularán cuando el partido se juegue</p>
        )}
        <p>Score total en este grupo: {group.totalScore}</p>
      </GroupResult>
    ))}
  </ResultMessage>
)}
```

## 🔍 Debugging

Si algo no funciona:

1. **Verifica el token JWT** en los headers
2. **Verifica que groupIds sea un array válido** con ObjectIds de MongoDB
3. **Verifica que matchId exista** en la competición
4. **Revisa la consola del backend** para ver logs de cálculo de puntos
5. **Verifica que competitionId y category sean correctos** (opcional pero ayuda)

## 📞 Endpoints Relacionados

### Obtener predicciones del usuario
```
GET /football-pool/v1/api/auth/{userId}/predictions?groupId={groupId}
```

### Obtener una predicción específica
```
GET /football-pool/v1/api/auth/{userId}/predictions/{groupId}/{matchId}
```

---

**¿Dudas?** Revisa el archivo `FRONTEND_GUARDAR_PREDICCIONES_FINAL.txt` para documentación completa.







