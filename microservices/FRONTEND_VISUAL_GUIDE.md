# 📊 Guía Visual: Sistema de Predicciones

## 🔄 Flujo del Sistema

```
┌──────────────────┐
│   USUARIO FE     │
│                  │
│ Ingresa scores   │
│  Team1: 2       │
│  Team2: 1       │
└────────┬─────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│       FRONTEND (Tu Aplicación)           │
│                                          │
│  1. Valida datos (scores >= 0)          │
│  2. Obtiene groupIds del usuario        │
│  3. Prepara request body                │
└────────┬─────────────────────────────────┘
         │
         │ POST /auth/{userId}/predictions
         │
         ▼
┌──────────────────────────────────────────────────────────┐
│               BACKEND (API)                              │
│                                                          │
│  1. Guarda predicción UNA vez en users.predictions[]   │
│     {                                                    │
│       matchId: "group-a-match-1",                       │
│       team1Score: 2,                                    │
│       team2Score: 1,                                    │
│       points: 0  // ← Sin calcular aún                 │
│     }                                                    │
│                                                          │
│  2. Busca resultado real del partido                    │
│     ┌─────────────────────────────┐                    │
│     │ ¿Partido ya jugado?        │                     │
│     │ isPlayed: true?            │                     │
│     └────┬──────────────┬─────────┘                    │
│         YES           NO                                 │
│          │             │                                 │
│          ▼             ▼                                 │
│     [CALCULA]    [GUARDA 0]                             │
│      PUNTOS       PUNTOS                                 │
│                                                          │
│  3. Actualiza cada grupo en groupIds[]                  │
│     Grupo1: userPredictions[] ← añade predicción        │
│     Grupo2: userPredictions[] ← añade predicción        │
│     Grupo3: userPredictions[] ← añade predicción        │
│                                                          │
│  4. Actualiza scoreboards (si hay puntos)               │
│                                                          │
│  5. Devuelve respuesta                                  │
└────────┬─────────────────────────────────────────────────┘
         │
         ▼
┌──────────────────────────────────────────┐
│       FRONTEND (Recibe Respuesta)        │
│                                          │
│  {                                       │
│    message: "Success",                   │
│    groupsApplied: [                      │
│      {                                   │
│        groupId: "...",                   │
│        groupName: "Los Mejores",         │
│        pointsCalculated: true,           │
│        points: 5,  ← MOSTRAR AL USUARIO │
│        totalScore: 15                    │
│      }                                   │
│    ]                                     │
│  }                                       │
│                                          │
│  Muestra mensaje de éxito y puntos      │
└──────────────────────────────────────────┘
```

## 🎯 Tabla de Cálculo de Puntos

### Fase de Grupos

| Predicción | Resultado Real | Condición | Puntos | Ejemplo |
|------------|---------------|-----------|---------|---------|
| 2-1 | 2-1 | ✅ Marcador exacto | **5** | Acertaste todo |
| 2-1 | 3-0 | ✅ Resultado correcto (ambos ganó T1) | **3** | Acertaste el ganador |
| 1-1 | 2-2 | ✅ Resultado correcto (ambos empate) | **3** | Acertaste el empate |
| 2-1 | 1-2 | ❌ Resultado incorrecto | **0** | Erraste el ganador |
| 2-1 | 1-1 | ❌ Resultado incorrecto | **0** | Predijiste ganador, fue empate |

### Fase de Eliminación (Puntos Adicionales)

| Predicción Extra | Resultado Real | Puntos Base | Bonus Extra Time | Bonus Penalties | Bonus Exacto Pen | Total |
|------------------|---------------|-------------|------------------|-----------------|------------------|--------|
| 1-1 + ET ✅ + Pen ✅ + 4-3 ✅ | 1-1, ET, Pen 4-3 | 5 | +1 | +2 | +3 | **11** 🔥 |
| 1-1 + ET ✅ + Pen ✅ + 3-2 ❌ | 1-1, ET, Pen 4-3 | 5 | +1 | +2 | 0 | **8** |
| 1-1 + ET ✅ + Pen ❌ | 1-1, ET, no Pen | 5 | +1 | 0 | 0 | **6** |
| 2-1 | 1-1, ET, Pen 4-3 | 0 | 0 | 0 | 0 | **0** |

## 📈 Ejemplos Paso a Paso

### Ejemplo 1: Predicción Perfecta (Fase de Grupos)

```
USUARIO PREDICE:
Team1: 2 ⚽⚽
Team2: 1 ⚽

RESULTADO REAL:
Team1: 2 ⚽⚽  (igual)
Team2: 1 ⚽   (igual)

BACKEND CALCULA:
✅ Marcador exacto → 5 puntos

RESPUESTA AL FRONTEND:
{
  "message": "Prediction saved successfully",
  "groupsApplied": [
    {
      "groupName": "Los Mejores",
      "pointsCalculated": true,
      "points": 5  ← MOSTRAR AL USUARIO
    }
  ]
}
```

### Ejemplo 2: Resultado Correcto sin Marcador Exacto

```
USUARIO PREDICE:
Team1: 2 ⚽⚽
Team2: 1 ⚽

RESULTADO REAL:
Team1: 3 ⚽⚽⚽  (diferente pero también ganó)
Team2: 0        (diferente)

BACKEND CALCULA:
✅ Predijiste que gana Team1 → Correcto
❌ No acertaste el marcador exacto
→ 3 puntos

RESPUESTA AL FRONTEND:
{
  "message": "Prediction saved successfully",
  "groupsApplied": [
    {
      "groupName": "Los Mejores",
      "pointsCalculated": true,
      "points": 3  ← MOSTRAR AL USUARIO
    }
  ]
}
```

### Ejemplo 3: Predicción Incorrecta

```
USUARIO PREDICE:
Team1: 2 ⚽⚽
Team2: 1 ⚽

RESULTADO REAL:
Team1: 0
Team2: 3 ⚽⚽⚽  (ganó el otro equipo)

BACKEND CALCULA:
❌ Predijiste que gana Team1
❌ En realidad ganó Team2
→ 0 puntos

RESPUESTA AL FRONTEND:
{
  "message": "Prediction saved successfully",
  "groupsApplied": [
    {
      "groupName": "Los Mejores",
      "pointsCalculated": true,
      "points": 0  ← MOSTRAR AL USUARIO
    }
  ]
}
```

### Ejemplo 4: Predicción Perfecta con Penales

```
USUARIO PREDICE:
Team1: 1 ⚽
Team2: 1 ⚽
Extra Time: ✅ SÍ
Penalties: ✅ SÍ
Penalties Score: 4-3

RESULTADO REAL:
Team1: 1 ⚽
Team2: 1 ⚽
Extra Time: ✅ SÍ
Penalties: ✅ SÍ
Penalties Score: 4-3 ✅

BACKEND CALCULA:
✅ Marcador exacto (1-1) → 5 puntos
✅ Predijiste tiempo extra → +1 punto
✅ Predijiste penales → +2 puntos
✅ Marcador exacto penales (4-3) → +3 puntos
→ TOTAL: 11 puntos 🔥

RESPUESTA AL FRONTEND:
{
  "message": "Prediction saved successfully",
  "groupsApplied": [
    {
      "groupName": "Los Mejores",
      "pointsCalculated": true,
      "points": 11  ← MOSTRAR AL USUARIO (¡Máximo!)
    }
  ]
}
```

### Ejemplo 5: Partido Aún No Jugado

```
USUARIO PREDICE:
Team1: 3 ⚽⚽⚽
Team2: 2 ⚽⚽

RESULTADO REAL:
⏰ Partido no jugado aún
isPlayed: false

BACKEND:
⏸️ Guarda predicción
⏸️ NO calcula puntos (aún)
→ 0 puntos (temporal)

RESPUESTA AL FRONTEND:
{
  "message": "Prediction saved successfully",
  "groupsApplied": [
    {
      "groupName": "Los Mejores",
      "pointsCalculated": false,  ← ¡Importante!
      "points": 0  ← Temporal, se calculará después
    }
  ]
}

NOTA: Cuando el partido se juegue, el backend
recalculará automáticamente los puntos.
```

## 🎨 Componentes UI Recomendados

### Estado de Predicción

```
┌─────────────────────────────────────────┐
│  🎯 Predicción para Real Madrid vs PSG  │
├─────────────────────────────────────────┤
│                                         │
│  Real Madrid  [  2  ]  ⚽               │
│                                         │
│  PSG          [  1  ]  ⚽               │
│                                         │
│  ☐ ¿Irá a tiempo extra?                │
│  ☐ ¿Irá a penales?                     │
│                                         │
│  [ Guardar Predicción ]                │
│                                         │
└─────────────────────────────────────────┘
```

### Resultado Después de Guardar

#### Caso 1: Partido Ya Jugado (Con Puntos)

```
┌─────────────────────────────────────────┐
│  ✅ Predicción guardada exitosamente   │
├─────────────────────────────────────────┤
│                                         │
│  📊 Grupo: Los Mejores                  │
│                                         │
│  🏆 Puntos obtenidos: 5                 │
│  📈 Score total: 25                     │
│                                         │
│  ─────────────────────────────          │
│                                         │
│  📊 Grupo: Amigos del Fútbol           │
│                                         │
│  🏆 Puntos obtenidos: 5                 │
│  📈 Score total: 18                     │
│                                         │
│  [ Ver Rankings ]                       │
│                                         │
└─────────────────────────────────────────┘
```

#### Caso 2: Partido No Jugado Aún

```
┌─────────────────────────────────────────┐
│  ✅ Predicción guardada exitosamente   │
├─────────────────────────────────────────┤
│                                         │
│  📊 Grupo: Los Mejores                  │
│                                         │
│  ⏰ Los puntos se calcularán cuando     │
│     el partido se juegue                │
│                                         │
│  Tu predicción: Real Madrid 2-1 PSG     │
│                                         │
│  [ OK ]                                 │
│                                         │
└─────────────────────────────────────────┘
```

## 📱 Estados del UI

### Loading

```javascript
if (loading) {
  return (
    <div>
      <Spinner />
      <p>Guardando predicción...</p>
    </div>
  );
}
```

### Error

```javascript
if (error) {
  return (
    <Alert type="error">
      ❌ {error}
      <Button onClick={retry}>Reintentar</Button>
    </Alert>
  );
}
```

### Success

```javascript
if (response) {
  return (
    <Alert type="success">
      ✅ {response.message}
      
      {response.groupsApplied.map(group => (
        <GroupResult key={group.groupId}>
          <h4>{group.groupName}</h4>
          
          {group.pointsCalculated ? (
            <>
              <Points>🏆 {group.points} puntos</Points>
              <TotalScore>Total: {group.totalScore}</TotalScore>
            </>
          ) : (
            <Pending>
              ⏰ Puntos pendientes
              <small>Se calcularán cuando el partido se juegue</small>
            </Pending>
          )}
        </GroupResult>
      ))}
    </Alert>
  );
}
```

## 🔍 Debugging Guide

### Problema: No se calculan puntos

**Causa posible:**
- El partido no se ha jugado aún (isPlayed: false)
- No enviaste competitionId y category

**Solución:**
```javascript
// Asegúrate de enviar estos campos
{
  matchId: "...",
  team1Score: 2,
  team2Score: 1,
  groupIds: [...],
  competitionId: "club-world-cup",  // ← IMPORTANTE
  category: "fifaOfficialClubCups"  // ← IMPORTANTE
}
```

### Problema: Error 400 "groupIds required"

**Causa:**
- groupIds no es un array
- groupIds está vacío

**Solución:**
```javascript
// MAL ❌
groupIds: "691d5cc672ce370633800343"

// BIEN ✅
groupIds: ["691d5cc672ce370633800343"]
```

### Problema: Los puntos no se muestran

**Causa:**
- La respuesta tiene `pointsCalculated: false`

**Solución:**
```javascript
// Verificar antes de mostrar
if (group.pointsCalculated) {
  showPoints(group.points);
} else {
  showMessage("Puntos pendientes");
}
```

## 📊 Matriz de Resultados

| Tu Predicción | Resultado Real | Win/Draw/Loss Tuyo | Win/Draw/Loss Real | Puntos |
|---------------|----------------|--------------------|--------------------|--------|
| 2-1 | 2-1 | Team1 Win | Team1 Win | 5 (exacto) |
| 3-0 | 2-1 | Team1 Win | Team1 Win | 3 (resultado) |
| 1-1 | 2-2 | Draw | Draw | 3 (resultado) |
| 0-3 | 1-2 | Team2 Win | Team2 Win | 3 (resultado) |
| 2-1 | 1-2 | Team1 Win | Team2 Win | 0 (incorrecto) |
| 2-1 | 1-1 | Team1 Win | Draw | 0 (incorrecto) |
| 1-1 | 2-1 | Draw | Team1 Win | 0 (incorrecto) |

## 🎯 Checklist Frontend

- [ ] Validar que team1Score >= 0
- [ ] Validar que team2Score >= 0
- [ ] Validar que groupIds sea un array con al menos 1 elemento
- [ ] Enviar competitionId y category (recomendado)
- [ ] Manejar estado de loading
- [ ] Manejar errores con mensajes claros
- [ ] Mostrar puntos solo si pointsCalculated: true
- [ ] Mostrar mensaje "pendiente" si pointsCalculated: false
- [ ] Permitir actualizar predicción
- [ ] Deshabilitar inputs durante loading

## 🚀 Testing Scenarios

### Test 1: Predicción Correcta con Puntos
```javascript
test('should save prediction and calculate points', async () => {
  const response = await savePrediction({
    matchId: 'match-1',
    team1Score: 2,
    team2Score: 1,
    groupIds: ['group1'],
    competitionId: 'club-world-cup',
    category: 'fifaOfficialClubCups'
  });

  expect(response.groupsApplied[0].pointsCalculated).toBe(true);
  expect(response.groupsApplied[0].points).toBeGreaterThan(0);
});
```

### Test 2: Predicción sin Puntos (Partido No Jugado)
```javascript
test('should save prediction without points if match not played', async () => {
  const response = await savePrediction({
    matchId: 'future-match-1',
    team1Score: 3,
    team2Score: 2,
    groupIds: ['group1']
  });

  expect(response.groupsApplied[0].pointsCalculated).toBe(false);
  expect(response.groupsApplied[0].points).toBe(0);
});
```

### Test 3: Validación de Campos Requeridos
```javascript
test('should fail if groupIds is missing', async () => {
  await expect(
    savePrediction({
      matchId: 'match-1',
      team1Score: 2,
      team2Score: 1,
      // groupIds missing
    })
  ).rejects.toThrow('groupIds array is required');
});
```

---

**🎉 ¡Listo!** Con esta guía visual tienes todo lo necesario para implementar el sistema de predicciones en el frontend.







