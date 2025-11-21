# 💰 Guía Frontend: Información de Apuestas en Grupos

## 📋 Resumen

Los grupos ahora incluyen información de apuestas:
- **`totalBetAmount`**: Monto total que el grupo debe pagar (pool de apuestas) - **✅ ES EDITABLE**
- **`paymentDeadline`**: Fecha límite para pagar (misma fecha en que ya no se pueden crear más usuarios)

**Importante:**
- El `totalBetAmount` **puede ser editado en cualquier momento** mediante PATCH/PUT (solo el creador del grupo)
- La fecha límite de pago se calcula automáticamente como **15 días después de la fecha de inicio de la copa** (`poolaAvailableDay`)

---

## ✅ Campos Nuevos en el Modelo Group

### `totalBetAmount` (Double, Requerido)
- **Tipo:** `Number` (Double)
- **Requerido:** Sí (al crear grupo)
- **Descripción:** Monto total que el grupo debe pagar
- **Validación:** Debe ser mayor que 0
- **Ejemplo:** `100.50`, `500`, `1000.00`

### `paymentDeadline` (Date, Calculado)
- **Tipo:** `Date` (timestamp en milisegundos)
- **Requerido:** No (se calcula automáticamente)
- **Descripción:** Fecha límite para pagar (igual a la fecha en que ya no se pueden crear más usuarios)
- **Cálculo:** 15 días después de `poolaAvailableDay` (fecha de inicio de la copa)
- **Ejemplo:** `1735689600000` (timestamp)

---

## 🔧 Endpoint: Crear Grupo

**POST** `/football-pool/v1/api/groups`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Body (campos requeridos agregados):**
```json
{
  "competitionId": "competition-123",
  "category": "fifaNationalTeamCups",
  "name": "Mi Grupo de Apuestas",
  "totalBetAmount": 500.00,
  "invitedEmails": ["user1@example.com", "user2@example.com"],
  "userIds": ["userId1", "userId2"]
}
```

**Respuesta Exitosa:**
```json
{
  "message": "Group created successfully",
  "group": {
    "groupId": "group-123",
    "competitionId": "competition-123",
    "competitionName": "Copa Mundial 2024",
    "name": "Mi Grupo de Apuestas",
    "totalBetAmount": 500.00,
    "paymentDeadline": 1735689600000,
    "creatorUserId": "user-creator-id",
    "userIds": ["user-creator-id", "userId1", "userId2"],
    "invitedEmails": ["user1@example.com"],
    "createdAt": 1733000000000,
    "enabledAt": 1733000000000,
    ...
  },
  "invitedEmails": 1,
  "addedUserIds": 2
}
```

**Errores Posibles:**

1. **Falta `totalBetAmount`:**
```json
{
  "error": "totalBetAmount is required"
}
```

2. **`totalBetAmount` inválido:**
```json
{
  "error": "totalBetAmount must be a valid number"
}
```

3. **`totalBetAmount` menor o igual a 0:**
```json
{
  "error": "totalBetAmount must be greater than 0"
}
```

---

## 🔧 Endpoint: Actualizar Grupo (PATCH) - Editar Monto

**PATCH** `/football-pool/v1/api/groups/{groupId}`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**✅ El monto (`totalBetAmount`) ES EDITABLE** - Puedes actualizarlo en cualquier momento usando PATCH.

**Body (actualizar solo el monto):**
```json
{
  "totalBetAmount": 750.00
}
```

**Respuesta Exitosa:**
```json
{
  "message": "Group patched successfully",
  "group": {
    "groupId": "group-123",
    "totalBetAmount": 750.00,
    "paymentDeadline": 1735689600000,
    ...
  }
}
```

**Actualizar monto y fecha límite:**
```json
{
  "totalBetAmount": 1000.00,
  "paymentDeadline": 1735776000000
}
```

**Nota:** 
- El `totalBetAmount` puede ser editado en cualquier momento por el creador del grupo
- El `paymentDeadline` normalmente se calcula automáticamente, pero puede ser actualizado manualmente si es necesario
- Solo el creador del grupo puede editar estos campos

---

## 🔧 Endpoint: Obtener Grupo

**GET** `/football-pool/v1/api/groups/{groupId}`

**Respuesta:**
```json
{
  "group": {
    "groupId": "group-123",
    "competitionId": "competition-123",
    "name": "Mi Grupo de Apuestas",
    "totalBetAmount": 500.00,
    "paymentDeadline": 1735689600000,
    "creatorUserId": "user-creator-id",
    "userIds": ["user-creator-id", "userId1"],
    "createdAt": 1733000000000,
    ...
  }
}
```

---

## 💻 Ejemplos de Código Frontend

### 🔵 React / JavaScript

```javascript
// Crear grupo con monto de apuesta
const createGroup = async (groupData) => {
  try {
    const response = await fetch('/football-pool/v1/api/groups', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        competitionId: groupData.competitionId,
        category: groupData.category,
        name: groupData.name,
        totalBetAmount: groupData.totalBetAmount, // REQUERIDO
        invitedEmails: groupData.invitedEmails || [],
        userIds: groupData.userIds || [],
      }),
    });

    const result = await response.json();
    
    if (response.ok) {
      console.log('Grupo creado:', result.group);
      console.log('Monto total:', result.group.totalBetAmount);
      console.log('Fecha límite de pago:', new Date(result.group.paymentDeadline));
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

// ✅ EDITAR MONTO DEL GRUPO (El monto es editable)
const updateGroupBetAmount = async (groupId, newAmount) => {
  try {
    const response = await fetch(`/football-pool/v1/api/groups/${groupId}`, {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        totalBetAmount: newAmount
      }),
    });

    const result = await response.json();
    
    if (response.ok) {
      console.log('Monto actualizado:', result.group.totalBetAmount);
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

// Ejemplo de uso - Crear grupo
const handleCreateGroup = async () => {
  const groupData = {
    competitionId: 'competition-123',
    category: 'fifaNationalTeamCups',
    name: 'Mi Grupo de Apuestas',
    totalBetAmount: 500.00, // IMPORTANTE: Incluir este campo
    invitedEmails: ['user1@example.com'],
  };

  try {
    const result = await createGroup(groupData);
    // El paymentDeadline se calcula automáticamente (15 días después de la fecha de inicio)
    console.log('Fecha límite de pago:', new Date(result.group.paymentDeadline));
  } catch (error) {
    alert('Error al crear grupo: ' + error.message);
  }
};

// Ejemplo de uso - Editar monto
const handleUpdateBetAmount = async (groupId) => {
  const newAmount = 750.00; // Nuevo monto
  
  try {
    const result = await updateGroupBetAmount(groupId, newAmount);
    alert(`Monto actualizado a $${result.group.totalBetAmount}`);
  } catch (error) {
    alert('Error al actualizar monto: ' + error.message);
  }
};
```

### 📱 React Native

```javascript
import axios from 'axios';

// Crear grupo
const createGroup = async (groupData, token) => {
  try {
    const response = await axios.post(
      `${API_URL}/football-pool/v1/api/groups`,
      {
        competitionId: groupData.competitionId,
        category: groupData.category,
        name: groupData.name,
        totalBetAmount: groupData.totalBetAmount, // REQUERIDO
        invitedEmails: groupData.invitedEmails || [],
        userIds: groupData.userIds || [],
      },
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      }
    );

    return response.data;
  } catch (error) {
    console.error('Error creating group:', error.response?.data || error.message);
    throw error;
  }
};

// ✅ EDITAR MONTO DEL GRUPO
const updateGroupBetAmount = async (groupId, newAmount, token) => {
  try {
    const response = await axios.patch(
      `${API_URL}/football-pool/v1/api/groups/${groupId}`,
      {
        totalBetAmount: newAmount
      },
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
      }
    );

    return response.data;
  } catch (error) {
    console.error('Error updating bet amount:', error.response?.data || error.message);
    throw error;
  }
};

// Componente de formulario
const CreateGroupScreen = () => {
  const [groupName, setGroupName] = useState('');
  const [totalBetAmount, setTotalBetAmount] = useState('');
  const [competitionId, setCompetitionId] = useState('');
  
  const handleSubmit = async () => {
    if (!totalBetAmount || parseFloat(totalBetAmount) <= 0) {
      alert('El monto debe ser mayor a 0');
      return;
    }

    try {
      const result = await createGroup({
        competitionId,
        category: 'fifaNationalTeamCups',
        name: groupName,
        totalBetAmount: parseFloat(totalBetAmount),
      }, token);

      const deadline = new Date(result.group.paymentDeadline);
      alert(`Grupo creado! Fecha límite de pago: ${deadline.toLocaleDateString()}`);
    } catch (error) {
      alert('Error: ' + (error.response?.data?.error || error.message));
    }
  };

  return (
    <View>
      <TextInput
        placeholder="Nombre del grupo"
        value={groupName}
        onChangeText={setGroupName}
      />
      <TextInput
        placeholder="Monto total (ej: 500.00)"
        value={totalBetAmount}
        onChangeText={setTotalBetAmount}
        keyboardType="numeric"
      />
      <Button title="Crear Grupo" onPress={handleSubmit} />
    </View>
  );
};
```

---

## 📅 Trabajar con Fechas

### Convertir Timestamp a Fecha

```javascript
// Obtener grupo
const group = await getGroup(groupId);

// Convertir paymentDeadline (timestamp) a Date
const paymentDeadline = new Date(group.paymentDeadline);

// Formatear fecha
const formattedDate = paymentDeadline.toLocaleDateString('es-ES', {
  year: 'numeric',
  month: 'long',
  day: 'numeric'
});

console.log(`Fecha límite: ${formattedDate}`);
```

### Validar si ya pasó la fecha límite

```javascript
const isPaymentDeadlinePassed = (group) => {
  if (!group.paymentDeadline) {
    return false; // No hay fecha límite configurada
  }
  
  const deadline = new Date(group.paymentDeadline);
  const now = new Date();
  
  return now > deadline;
};

// Uso
if (isPaymentDeadlinePassed(group)) {
  // Ya no se pueden agregar usuarios
  console.log('La fecha límite de pago ya pasó');
} else {
  // Aún se pueden agregar usuarios
  const daysLeft = Math.ceil((new Date(group.paymentDeadline) - new Date()) / (1000 * 60 * 60 * 24));
  console.log(`Quedan ${daysLeft} días para la fecha límite`);
}
```

---

## 💰 Calcular Monto por Usuario

```javascript
// Calcular cuánto debe pagar cada usuario
const calculateAmountPerUser = (group) => {
  if (!group.totalBetAmount) {
    return 0;
  }
  
  const totalUsers = group.userIds?.length || 1; // Incluir al creador
  const amountPerUser = group.totalBetAmount / totalUsers;
  
  return Math.round(amountPerUser * 100) / 100; // Redondear a 2 decimales
};

// Ejemplo
const group = {
  totalBetAmount: 500.00,
  userIds: ['user1', 'user2', 'user3', 'user4']
};

const amountPerUser = calculateAmountPerUser(group);
console.log(`Cada usuario debe pagar: $${amountPerUser}`); // $125.00
```

---

## ⚠️ Validaciones Importantes

### Frontend debe validar:

1. **`totalBetAmount` es requerido:**
```javascript
if (!totalBetAmount || totalBetAmount === '') {
  alert('El monto total es requerido');
  return;
}
```

2. **`totalBetAmount` debe ser un número positivo:**
```javascript
const amount = parseFloat(totalBetAmount);
if (isNaN(amount) || amount <= 0) {
  alert('El monto debe ser un número mayor a 0');
  return;
}
```

3. **Mostrar fecha límite de pago:**
```javascript
if (group.paymentDeadline) {
  const deadline = new Date(group.paymentDeadline);
  console.log(`Fecha límite de pago: ${deadline.toLocaleDateString()}`);
} else {
  console.warn('No hay fecha límite configurada (competencia sin fecha de inicio)');
}
```

---

## 📊 Ejemplo Completo de UI con Edición de Monto

```javascript
// Componente que muestra información de apuestas con opción de editar
const GroupBettingInfo = ({ group, currentUserId, onUpdate }) => {
  const [isEditing, setIsEditing] = useState(false);
  const [newAmount, setNewAmount] = useState(group.totalBetAmount?.toString() || '');
  const [loading, setLoading] = useState(false);
  
  const isCreator = group.creatorUserId === currentUserId;
  const paymentDeadline = group.paymentDeadline 
    ? new Date(group.paymentDeadline)
    : null;
  
  const isDeadlinePassed = paymentDeadline 
    ? new Date() > paymentDeadline
    : false;
  
  const daysLeft = paymentDeadline
    ? Math.ceil((paymentDeadline - new Date()) / (1000 * 60 * 60 * 24))
    : null;
  
  const amountPerUser = group.totalBetAmount && group.userIds
    ? (group.totalBetAmount / (group.userIds.length || 1)).toFixed(2)
    : '0.00';

  const handleSaveAmount = async () => {
    const amount = parseFloat(newAmount);
    if (isNaN(amount) || amount <= 0) {
      alert('El monto debe ser mayor a 0');
      return;
    }

    setLoading(true);
    try {
      await updateGroupBetAmount(group.groupId, amount);
      setIsEditing(false);
      onUpdate?.(); // Refrescar datos del grupo
      alert('Monto actualizado exitosamente');
    } catch (error) {
      alert('Error al actualizar monto: ' + error.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>💰 Información de Apuestas</Text>
        {isCreator && !isEditing && (
          <TouchableOpacity 
            onPress={() => setIsEditing(true)}
            style={styles.editButton}
          >
            <Text style={styles.editButtonText}>✏️ Editar</Text>
          </TouchableOpacity>
        )}
      </View>
      
      <View style={styles.infoRow}>
        <Text style={styles.label}>Monto Total:</Text>
        {isEditing ? (
          <View style={styles.editContainer}>
            <TextInput
              style={styles.amountInput}
              value={newAmount}
              onChangeText={setNewAmount}
              keyboardType="numeric"
              placeholder="0.00"
            />
            <TouchableOpacity 
              onPress={handleSaveAmount}
              disabled={loading}
              style={styles.saveButton}
            >
              <Text style={styles.saveButtonText}>
                {loading ? 'Guardando...' : '✓'}
              </Text>
            </TouchableOpacity>
            <TouchableOpacity 
              onPress={() => {
                setIsEditing(false);
                setNewAmount(group.totalBetAmount?.toString() || '');
              }}
              style={styles.cancelButton}
            >
              <Text style={styles.cancelButtonText}>✕</Text>
            </TouchableOpacity>
          </View>
        ) : (
          <Text style={styles.value}>
            ${group.totalBetAmount?.toFixed(2) || '0.00'}
          </Text>
        )}
      </View>
      
      <View style={styles.infoRow}>
        <Text style={styles.label}>Monto por Usuario:</Text>
        <Text style={styles.value}>${amountPerUser}</Text>
      </View>
      
      {paymentDeadline && (
        <>
          <View style={styles.infoRow}>
            <Text style={styles.label}>Fecha Límite de Pago:</Text>
            <Text style={[styles.value, isDeadlinePassed && styles.expired]}>
              {paymentDeadline.toLocaleDateString('es-ES', {
                year: 'numeric',
                month: 'long',
                day: 'numeric'
              })}
            </Text>
          </View>
          
          {!isDeadlinePassed && daysLeft !== null && (
            <View style={styles.warning}>
              <Text style={styles.warningText}>
                {daysLeft > 0 
                  ? `⏰ Quedan ${daysLeft} día${daysLeft > 1 ? 's' : ''} para la fecha límite`
                  : '⚠️ Hoy es la fecha límite de pago'}
              </Text>
            </View>
          )}
          
          {isDeadlinePassed && (
            <View style={styles.error}>
              <Text style={styles.errorText}>
                ❌ La fecha límite de pago ya pasó. No se pueden agregar más usuarios.
              </Text>
            </View>
          )}
        </>
      )}
      
      {!paymentDeadline && (
        <View style={styles.warning}>
          <Text style={styles.warningText}>
            ⚠️ No hay fecha límite configurada (la competencia no tiene fecha de inicio)
          </Text>
        </View>
      )}
    </View>
  );
};
```

---

## ✅ Checklist de Implementación Frontend

- [ ] Validar que `totalBetAmount` sea requerido al crear grupo
- [ ] Validar que `totalBetAmount` sea un número positivo
- [ ] Mostrar `totalBetAmount` en la UI del grupo
- [ ] **Implementar funcionalidad para EDITAR el monto (PATCH)**
- [ ] Mostrar botón/formulario para editar monto (solo para creador del grupo)
- [ ] Mostrar `paymentDeadline` formateado en la UI
- [ ] Calcular y mostrar monto por usuario
- [ ] Validar si la fecha límite ya pasó antes de permitir agregar usuarios
- [ ] Mostrar contador de días restantes hasta la fecha límite
- [ ] Manejar casos donde `paymentDeadline` sea `null` (competencia sin fecha de inicio)

---

## 🔍 Notas Importantes

1. **✅ El Monto ES EDITABLE:** El `totalBetAmount` puede ser actualizado en cualquier momento mediante PATCH o PUT. Solo el creador del grupo puede editarlo.

2. **Cálculo Automático:** El `paymentDeadline` se calcula automáticamente como 15 días después de la fecha de inicio de la copa (`poolaAvailableDay`)

3. **Fecha Límite = Fecha de Cierre:** La misma fecha límite de pago es cuando ya no se pueden crear/agregar más usuarios al grupo

4. **Valores Nulos:** Si la competencia no tiene `poolaAvailableDay`, el `paymentDeadline` será `null`. El frontend debe manejar este caso

5. **Actualización Manual:** Aunque `paymentDeadline` se calcula automáticamente, puede ser actualizado manualmente mediante PATCH si es necesario

6. **Moneda:** El sistema no maneja moneda específica. El frontend debe mostrar el símbolo de moneda adecuado (ej: `$`, `€`, etc.)

7. **Permisos:** Solo el creador del grupo (`creatorUserId`) puede editar el `totalBetAmount` y `paymentDeadline`

---

## 📝 Ejemplo de Request Completo

```json
POST /football-pool/v1/api/groups
Content-Type: application/json
Authorization: Bearer {token}

{
  "competitionId": "copa-mundial-2024",
  "category": "fifaNationalTeamCups",
  "name": "Grupo de Apuestas - Copa Mundial",
  "totalBetAmount": 1000.00,
  "invitedEmails": [
    "friend1@example.com",
    "friend2@example.com"
  ],
  "userIds": [
    "user-id-1",
    "user-id-2"
  ]
}
```

---

## 🚀 Próximos Pasos

- [ ] Implementar validación en frontend para no permitir agregar usuarios después de `paymentDeadline`
- [ ] Implementar notificaciones cuando se acerque la fecha límite
- [ ] Implementar sistema de pagos (cuando esté listo)
- [ ] Agregar historial de pagos por usuario

