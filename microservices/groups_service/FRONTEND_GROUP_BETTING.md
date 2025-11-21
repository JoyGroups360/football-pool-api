# 🎲 Sistema de Apuestas Grupales - Guía para Frontend

## 📋 Resumen

El sistema de apuestas funciona **únicamente a nivel de grupo**, donde:
- **Monto mínimo por usuario**: $50 USD (fijo)
- El `totalBetAmount` se divide **equitativamente** entre todos los miembros
- Cada usuario paga su parte equitativa (mínimo $50)
- Solo el creador del grupo puede modificar el `totalBetAmount`

---

## 🎯 Conceptos Clave

### Monto Total del Grupo (`totalBetAmount`)
- Es el **monto total** que debe pagar todo el grupo en conjunto
- Se divide equitativamente entre todos los usuarios
- **Ejemplo**: Si `totalBetAmount = $200` y hay 4 usuarios, cada uno paga $50

### Monto Equitativo por Usuario (`equitableAmountPerUser`)
- Se calcula automáticamente: `totalBetAmount / númeroDeUsuarios`
- Cada usuario debe pagar exactamente este monto
- **Mínimo**: $50 por usuario

### Fórmula de Validación
```
totalBetAmount >= $50 × númeroDeUsuarios
equitableAmountPerUser = totalBetAmount / númeroDeUsuarios
equitableAmountPerUser >= $50
```

---

## 📡 Endpoints Principales

### 1. Crear Grupo con Monto de Apuesta

**POST** `/football-pool/v1/api/groups`

```javascript
const createGroup = async (groupData, token) => {
  try {
    const response = await fetch('/football-pool/v1/api/groups', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        competitionId: 'world-cup',
        category: 'fifaNationalTeamCups',
        name: 'Grupo de la Oficina',
        totalBetAmount: 200, // ⚠️ Mínimo: $50 × número de usuarios
        invitedEmails: ['user1@example.com', 'user2@example.com'],
        userIds: [] // IDs de usuarios existentes (opcional)
      }),
    });

    const result = await response.json();

    if (response.ok) {
      console.log('Grupo creado:', result.group);
      console.log('Monto equitativo por usuario:', result.group.equitableAmountPerUser);
      // Resultado: { equitableAmountPerUser: 50, totalBetAmount: 200, ... }
      return result;
    } else {
      console.error('Error:', result.error);
      throw new Error(result.error);
    }
  } catch (error) {
    console.error('Error al crear grupo:', error);
    throw error;
  }
};
```

**Validaciones del Backend:**
- `totalBetAmount` es **obligatorio**
- `totalBetAmount >= $50 × númeroDeUsuariosEstimados`
- Si no cumple, retorna error con el monto mínimo requerido

**Ejemplo de Error:**
```json
{
  "error": "totalBetAmount must be at least $200 (monto mínimo por usuario es $50 y hay 4 usuarios estimados)",
  "minimumBetPerUser": 50,
  "estimatedUsers": 4,
  "minimumTotalBetAmount": 200
}
```

---

### 2. Obtener Información del Grupo

**GET** `/football-pool/v1/api/groups/{groupId}`

```javascript
const getGroup = async (groupId, token) => {
  try {
    const response = await fetch(`/football-pool/v1/api/groups/${groupId}`, {
      method: 'GET',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });

    const result = await response.json();

    if (response.ok) {
      const group = result.group;
      
      // Información de apuestas
      console.log('Monto total del grupo:', group.totalBetAmount);
      console.log('Monto por usuario:', group.equitableAmountPerUser);
      console.log('Fecha límite de pago:', group.paymentDeadline);
      
      // Estado de pagos de cada usuario
      if (group.userPayments) {
        Object.entries(group.userPayments).forEach(([userId, payment]) => {
          console.log(`Usuario ${userId}:`, {
            monto: payment.paymentAmount,
            haPagado: payment.hasPaid,
            fechaPago: payment.paidDate
          });
        });
      }
      
      return group;
    } else {
      throw new Error(result.error);
    }
  } catch (error) {
    console.error('Error al obtener grupo:', error);
    throw error;
  }
};
```

**Estructura de Respuesta:**
```json
{
  "group": {
    "groupId": "grupo-123",
    "name": "Grupo de la Oficina",
    "totalBetAmount": 200,
    "equitableAmountPerUser": 50,
    "paymentDeadline": "2026-05-30T00:00:00.000Z",
    "userPayments": {
      "user-id-1": {
        "userId": "user-id-1",
        "userEmail": "user1@example.com",
        "paymentAmount": 50,
        "hasPaid": false,
        "paymentId": null,
        "paidDate": null,
        "isCreator": true
      },
      "user-id-2": {
        "userId": "user-id-2",
        "userEmail": "user2@example.com",
        "paymentAmount": 50,
        "hasPaid": true,
        "paymentId": "payment-stripe-id",
        "paidDate": "2025-11-20T10:30:00.000Z",
        "isCreator": false
      }
    },
    "userIds": ["user-id-1", "user-id-2"],
    "invitedEmails": ["user3@example.com"]
  }
}
```

---

### 3. Actualizar Monto Total del Grupo (Solo Creador)

**PATCH** `/football-pool/v1/api/groups/{groupId}`

```javascript
// ✅ EDITAR MONTO DEL GRUPO (Solo el creador puede hacerlo)
const updateGroupBetAmount = async (groupId, newTotalAmount, token) => {
  try {
    const response = await fetch(`/football-pool/v1/api/groups/${groupId}`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        totalBetAmount: newTotalAmount
      }),
    });

    const result = await response.json();

    if (response.ok) {
      console.log('Monto actualizado:', result.group.totalBetAmount);
      console.log('Nuevo monto por usuario:', result.group.equitableAmountPerUser);
      
      // ⚠️ IMPORTANTE: Los montos de los usuarios que NO han pagado se actualizan automáticamente
      // Los que ya pagaron mantienen su monto original
      
      return result;
    } else {
      console.error('Error:', result.error);
      throw new Error(result.error);
    }
  } catch (error) {
    console.error('Error al actualizar monto:', error);
    throw error;
  }
};

// Ejemplo de uso:
// updateGroupBetAmount('grupo-123', 300, token);
// Resultado: equitableAmountPerUser cambia de $50 a $75 (si hay 4 usuarios)
```

**Validaciones del Backend:**
- Solo el creador puede actualizar
- `totalBetAmount >= $50 × númeroDeUsuariosActuales`
- Se recalcula automáticamente `equitableAmountPerUser`
- Solo se actualizan los montos de usuarios que **aún no han pagado**

---

### 4. Invitar Más Usuarios al Grupo

**POST** `/football-pool/v1/api/groups/{groupId}/invite`

```javascript
const inviteToGroup = async (groupId, email, token) => {
  try {
    const response = await fetch(`/football-pool/v1/api/groups/${groupId}/invite`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        email: email
      }),
    });

    const result = await response.json();

    if (response.ok) {
      // ⚠️ IMPORTANTE: Al invitar usuarios, el equitableAmountPerUser se recalcula automáticamente
      // Ejemplo: Si totalBetAmount = $200 y había 4 usuarios ($50 c/u)
      //          Al invitar 1 más, ahora son 5 usuarios → $40 c/u (pero mínimo $50)
      //          Por lo tanto, el backend validará que totalBetAmount >= $250
      
      return result;
    } else {
      throw new Error(result.error);
    }
  } catch (error) {
    console.error('Error al invitar:', error);
    throw error;
  }
};
```

**Nota Importante:**
Cuando se invita a un nuevo usuario, el sistema recalcula el `equitableAmountPerUser`. Si el monto resultante es menor a $50, el sistema **no permitirá** invitar más usuarios hasta que el creador aumente el `totalBetAmount`.

---

### 5. Unirse a un Grupo (Usuarios Invitados)

**POST** `/football-pool/v1/api/groups/{groupId}/join`

```javascript
const joinGroup = async (groupId, token) => {
  try {
    const response = await fetch(`/football-pool/v1/api/groups/${groupId}/join`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });

    const result = await response.json();

    if (response.ok) {
      // Al unirse, se crea automáticamente el registro de pago del usuario
      // con el equitableAmountPerUser actual
      
      return result;
    } else {
      throw new Error(result.error);
    }
  } catch (error) {
    console.error('Error al unirse al grupo:', error);
    throw error;
  }
};
```

---

## 💳 Integración con Pagos

### Flujo Completo de Pago

1. **Usuario ve su monto a pagar** (equitableAmountPerUser)
2. **Usuario inicia el pago** (crea PaymentIntent)
3. **Usuario completa el pago** (Stripe procesa)
4. **Se actualiza el estado** (webhook o polling)

```javascript
// 1. Crear PaymentIntent para el monto equitativo
const createPaymentIntent = async (groupId, token) => {
  try {
    const group = await getGroup(groupId, token);
    const equitableAmount = group.equitableAmountPerUser;
    const userId = getUserIdFromToken(token); // Extraer del JWT
    
    const response = await fetch('/football-pool/v1/api/payments/intent', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        amount: equitableAmount,
        currency: 'usd',
        userId: userId,
        groupId: groupId,
        description: `Pago de apuesta - ${group.name}`,
        receiptEmail: getUserEmail(), // Email del usuario
        metadata: {
          groupId: groupId,
          groupName: group.name,
          paymentType: 'group_bet'
        }
      }),
    });

    const result = await response.json();

    if (response.ok) {
      const { clientSecret, paymentIntentId } = result.data;
      
      // 2. Usar clientSecret con Stripe Elements o Stripe Checkout
      // Ejemplo con Stripe Checkout:
      // stripe.redirectToCheckout({ clientSecret: clientSecret });
      
      return { clientSecret, paymentIntentId };
    } else {
      throw new Error(result.error);
    }
  } catch (error) {
    console.error('Error al crear payment intent:', error);
    throw error;
  }
};

// 3. Después de confirmar el pago, actualizar estado en el grupo
const confirmGroupPayment = async (groupId, paymentId, token) => {
  try {
    const response = await fetch(`/football-pool/v1/api/groups/${groupId}/payment/${getUserIdFromToken(token)}`, {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        paymentId: paymentId
      }),
    });

    const result = await response.json();

    if (response.ok) {
      console.log('Pago confirmado en el grupo');
      return result;
    } else {
      throw new Error(result.error);
    }
  } catch (error) {
    console.error('Error al confirmar pago:', error);
    throw error;
  }
};
```

---

## 🎨 Componentes UI Recomendados

### 1. Componente de Creación de Grupo

```jsx
import React, { useState } from 'react';

const CreateGroupForm = ({ competition, onSuccess }) => {
  const [totalBetAmount, setTotalBetAmount] = useState(200);
  const [invitedEmails, setInvitedEmails] = useState([]);
  const [estimatedUsers, setEstimatedUsers] = useState(1); // Creator + invited
  
  const minimumTotalAmount = 50 * estimatedUsers; // $50 × número de usuarios
  
  const handleInviteUser = (email) => {
    setInvitedEmails([...invitedEmails, email]);
    setEstimatedUsers(estimatedUsers + 1);
    
    // Recalcular mínimo requerido
    const newMinimum = 50 * (estimatedUsers + 1);
    if (totalBetAmount < newMinimum) {
      setTotalBetAmount(newMinimum); // Auto-ajustar si es necesario
    }
  };
  
  const handleCreateGroup = async () => {
    // Validar que totalBetAmount >= minimumTotalAmount
    if (totalBetAmount < minimumTotalAmount) {
      alert(`El monto total debe ser al menos $${minimumTotalAmount} ($$50 por usuario × ${estimatedUsers} usuarios)`);
      return;
    }
    
    const groupData = {
      competitionId: competition.id,
      category: competition.category,
      name: 'Mi Grupo',
      totalBetAmount: totalBetAmount,
      invitedEmails: invitedEmails
    };
    
    // Llamar a createGroup...
  };
  
  return (
    <div>
      <h2>Crear Grupo de Apuesta</h2>
      
      <div>
        <label>Monto Total del Grupo:</label>
        <input
          type="number"
          min={minimumTotalAmount}
          step="0.01"
          value={totalBetAmount}
          onChange={(e) => setTotalBetAmount(parseFloat(e.target.value))}
        />
        <small>Mínimo: ${minimumTotalAmount} ($$50 × {estimatedUsers} usuarios)</small>
      </div>
      
      <div>
        <p>Monto por usuario: <strong>${(totalBetAmount / estimatedUsers).toFixed(2)}</strong></p>
        {totalBetAmount / estimatedUsers < 50 && (
          <p style={{color: 'red'}}>⚠️ El monto por usuario debe ser al menos $50</p>
        )}
      </div>
      
      {/* Resto del formulario... */}
    </div>
  );
};
```

### 2. Componente de Visualización de Pagos

```jsx
const GroupBettingInfo = ({ group, currentUserId }) => {
  const equitableAmount = group.equitableAmountPerUser;
  const userPayment = group.userPayments?.[currentUserId];
  const hasPaid = userPayment?.hasPaid || false;
  const isCreator = group.creatorUserId === currentUserId;
  
  // Calcular estadísticas de pagos
  const totalUsers = Object.keys(group.userPayments || {}).length;
  const paidUsers = Object.values(group.userPayments || {}).filter(p => p.hasPaid).length;
  const pendingPayments = totalUsers - paidUsers;
  const totalCollected = Object.values(group.userPayments || {})
    .filter(p => p.hasPaid)
    .reduce((sum, p) => sum + p.paymentAmount, 0);
  
  return (
    <div className="betting-info">
      <h3>Información de Apuestas</h3>
      
      <div className="betting-summary">
        <p><strong>Monto Total del Grupo:</strong> ${group.totalBetAmount}</p>
        <p><strong>Monto por Usuario:</strong> ${equitableAmount}</p>
        <p><strong>Fecha Límite de Pago:</strong> {new Date(group.paymentDeadline).toLocaleDateString()}</p>
      </div>
      
      <div className="payment-status">
        <p>Usuarios que han pagado: {paidUsers} / {totalUsers}</p>
        <p>Total recaudado: ${totalCollected} / ${group.totalBetAmount}</p>
      </div>
      
      <div className="user-payment">
        <h4>Tu Pago</h4>
        <p><strong>Monto a Pagar:</strong> ${equitableAmount}</p>
        
        {hasPaid ? (
          <div className="paid-status">
            <p>✅ Ya has pagado</p>
            <p>Fecha de pago: {new Date(userPayment.paidDate).toLocaleDateString()}</p>
          </div>
        ) : (
          <button onClick={() => handlePay(equitableAmount)}>
            Pagar ${equitableAmount}
          </button>
        )}
      </div>
      
      {isCreator && (
        <div className="creator-actions">
          <button onClick={() => handleEditAmount()}>
            Editar Monto Total
          </button>
        </div>
      )}
    </div>
  );
};
```

---

## ✅ Validaciones en el Frontend

### Antes de Crear un Grupo

```javascript
const validateGroupCreation = (totalBetAmount, invitedEmails, existingUserIds) => {
  const minimumBetPerUser = 50; // $50 fijo
  const totalUsers = 1 + (invitedEmails?.length || 0) + (existingUserIds?.length || 0);
  const minimumTotalBetAmount = minimumBetPerUser * totalUsers;
  const equitableAmount = totalBetAmount / totalUsers;
  
  const errors = [];
  
  if (totalBetAmount < minimumTotalBetAmount) {
    errors.push(`El monto total debe ser al menos $${minimumTotalBetAmount} ($$50 × ${totalUsers} usuarios)`);
  }
  
  if (equitableAmount < minimumBetPerUser) {
    errors.push(`El monto por usuario debe ser al menos $${minimumBetPerUser}`);
  }
  
  if (totalBetAmount <= 0) {
    errors.push('El monto total debe ser mayor a 0');
  }
  
  return {
    isValid: errors.length === 0,
    errors: errors,
    minimumTotalBetAmount: minimumTotalBetAmount,
    equitableAmount: equitableAmount
  };
};

// Uso:
const validation = validateGroupCreation(200, ['user1@email.com'], []);
if (!validation.isValid) {
  console.error('Errores:', validation.errors);
  // Mostrar errores al usuario
}
```

### Antes de Actualizar el Monto Total

```javascript
const validateAmountUpdate = (newTotalAmount, group) => {
  const minimumBetPerUser = 50;
  const totalUsers = group.userIds.length + (group.invitedEmails?.length || 0);
  const minimumTotalBetAmount = minimumBetPerUser * totalUsers;
  const newEquitableAmount = newTotalAmount / totalUsers;
  
  if (newTotalAmount < minimumTotalBetAmount) {
    return {
      isValid: false,
      error: `El monto total debe ser al menos $${minimumTotalBetAmount} ($$50 × ${totalUsers} usuarios)`
    };
  }
  
  if (newEquitableAmount < minimumBetPerUser) {
    return {
      isValid: false,
      error: `El monto por usuario resultante ($${newEquitableAmount.toFixed(2)}) es menor a $${minimumBetPerUser}`
    };
  }
  
  // ⚠️ ADVERTENCIA: Los usuarios que ya pagaron mantendrán su monto original
  const paidUsers = Object.values(group.userPayments || {}).filter(p => p.hasPaid).length;
  if (paidUsers > 0) {
    return {
      isValid: true,
      warning: `⚠️ ${paidUsers} usuario(s) ya pagaron. Sus montos no cambiarán, solo los pendientes.`
    };
  }
  
  return { isValid: true };
};
```

---

## 📊 Ejemplos de Flujo Completo

### Ejemplo 1: Crear Grupo con 4 Usuarios

```javascript
// 1. Usuario crea grupo
const group = await createGroup({
  competitionId: 'world-cup',
  category: 'fifaNationalTeamCups',
  name: 'Grupo Mundial 2026',
  totalBetAmount: 200, // $200 total
  invitedEmails: ['user1@email.com', 'user2@email.com', 'user3@email.com']
}, token);

// Resultado:
// - equitableAmountPerUser: $50 ($200 / 4 usuarios)
// - userPayments creados para el creador y los 3 invitados
// - Cada uno debe pagar $50

// 2. Los usuarios se unen
// Al unirse, se crea automáticamente su registro de pago con $50

// 3. Cada usuario paga su $50
// Se crea un PaymentIntent por $50
// Se actualiza userPayments[userId].hasPaid = true
```

### Ejemplo 2: Creador Cambia el Monto Total

```javascript
// Estado inicial:
// - totalBetAmount: $200
// - 4 usuarios
// - equitableAmountPerUser: $50
// - 2 usuarios ya pagaron $50 cada uno

// Creador cambia a $300
await updateGroupBetAmount('grupo-123', 300, token);

// Resultado:
// - totalBetAmount: $300
// - equitableAmountPerUser: $75 ($300 / 4 usuarios)
// - Usuarios que YA pagaron: mantienen $50 (no cambia)
// - Usuarios pendientes: ahora deben pagar $75
```

### Ejemplo 3: Invitar Más Usuarios

```javascript
// Estado inicial:
// - totalBetAmount: $200
// - 4 usuarios ($50 c/u)

// Invitar 1 usuario más
await inviteToGroup('grupo-123', 'newuser@email.com', token);

// El sistema recalcula automáticamente:
// - Ahora son 5 usuarios
// - equitableAmountPerUser = $200 / 5 = $40
// ⚠️ PERO: El mínimo es $50, entonces el sistema VALIDARÁ que:
// - $200 >= $50 × 5 = $250
// - Como no cumple, el backend rechazará la invitación o requerirá aumentar el totalBetAmount
```

---

## ⚠️ Consideraciones Importantes

### 1. Monto Mínimo Fijo
- El monto mínimo por usuario es **siempre $50** (no se puede cambiar)
- No hay opción de apuestas individuales personalizadas

### 2. División Equitativa
- Todos los usuarios pagan **exactamente** el mismo monto
- Si el `totalBetAmount` no es divisible equitativamente, el cálculo usa decimales

### 3. Actualización de Montos
- Solo el **creador** puede cambiar `totalBetAmount`
- Los usuarios que **ya pagaron** mantienen su monto original
- Solo se actualizan los montos de usuarios **pendientes**

### 4. Validación de Invitaciones
- Al invitar usuarios, el sistema valida que el monto equitativo resultante sea >= $50
- Si no cumple, el backend puede rechazar la invitación

### 5. Fecha Límite de Pago
- Se calcula automáticamente: **15 días después** de la fecha de inicio de la competencia (`poolaAvailableDay`)
- Es la misma fecha límite para crear nuevos usuarios en el grupo

---

## 🎯 Resumen de Reglas de Negocio

| Regla | Descripción |
|-------|-------------|
| **Monto mínimo por usuario** | $50 USD (fijo, no configurable) |
| **Tipo de apuesta** | Solo grupal (no individual) |
| **División** | Equitativa (totalBetAmount / númeroUsuarios) |
| **Quien puede cambiar monto** | Solo el creador del grupo |
| **Validación de monto total** | totalBetAmount >= $50 × númeroUsuarios |
| **Actualización de montos** | Solo afecta a usuarios que no han pagado |

---

## 📝 Ejemplo de Respuesta del Grupo

```json
{
  "group": {
    "groupId": "grupo-abc123",
    "name": "Grupo Mundial 2026",
    "creatorUserId": "user-123",
    "competitionId": "world-cup",
    "competitionName": "Copa Mundial FIFA",
    "totalBetAmount": 200,
    "equitableAmountPerUser": 50,
    "paymentDeadline": "2026-05-30T23:59:59.999Z",
    "userPayments": {
      "user-123": {
        "userId": "user-123",
        "userEmail": "creator@email.com",
        "paymentAmount": 50,
        "hasPaid": true,
        "paymentId": "pi_stripe123",
        "paidDate": "2025-11-20T10:00:00.000Z",
        "isCreator": true
      },
      "user-456": {
        "userId": "user-456",
        "userEmail": "member1@email.com",
        "paymentAmount": 50,
        "hasPaid": false,
        "paymentId": null,
        "paidDate": null,
        "isCreator": false
      }
    },
    "userIds": ["user-123", "user-456"],
    "invitedEmails": ["pending@email.com"]
  }
}
```

---

## 🔗 Integración con Payments Service

Para procesar los pagos, usa el **Payments Service** que creamos:

```javascript
// Crear PaymentIntent para el monto equitativo
const response = await fetch('/football-pool/v1/api/payments/intent', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json',
  },
  body: JSON.stringify({
    amount: group.equitableAmountPerUser,
    currency: 'usd',
    userId: currentUserId,
    groupId: group.groupId,
    description: `Pago de apuesta grupal - ${group.name}`,
    metadata: {
      groupId: group.groupId,
      groupName: group.name
    }
  }),
});
```

---

¿Necesitas más información o algún ejemplo específico para tu frontend?

