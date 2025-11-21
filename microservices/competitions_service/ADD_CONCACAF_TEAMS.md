# 🇺🇸 Agregar Equipos a Copa Oro CONCACAF

## 📋 Información de la Competencia

- **Category:** `fifaNationalTeamCups`
- **Competition ID:** `gold-cup`
- **Name:** Copa Oro CONCACAF

---

## 🔧 Endpoint para Agregar Equipos

**POST** `/football-pool/v1/api/competitions/{category}/{competitionId}/teams`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

---

## 📝 Equipos CONCACAF para Copa Oro

### Equipos Principales (16 equipos típicos de Copa Oro)

```json
[
  {
    "id": "usa",
    "name": "Estados Unidos",
    "country": "Estados Unidos",
    "flag": "https://flagcdn.com/w320/us.png",
    "group": null,
    "seed": 1
  },
  {
    "id": "mex",
    "name": "México",
    "country": "México",
    "flag": "https://flagcdn.com/w320/mx.png",
    "group": null,
    "seed": 1
  },
  {
    "id": "can",
    "name": "Canadá",
    "country": "Canadá",
    "flag": "https://flagcdn.com/w320/ca.png",
    "group": null,
    "seed": 2
  },
  {
    "id": "crc",
    "name": "Costa Rica",
    "country": "Costa Rica",
    "flag": "https://flagcdn.com/w320/cr.png",
    "group": null,
    "seed": 2
  },
  {
    "id": "pan",
    "name": "Panamá",
    "country": "Panamá",
    "flag": "https://flagcdn.com/w320/pa.png",
    "group": null,
    "seed": 3
  },
  {
    "id": "jam",
    "name": "Jamaica",
    "country": "Jamaica",
    "flag": "https://flagcdn.com/w320/jm.png",
    "group": null,
    "seed": 3
  },
  {
    "id": "hon",
    "name": "Honduras",
    "country": "Honduras",
    "flag": "https://flagcdn.com/w320/hn.png",
    "group": null,
    "seed": 4
  },
  {
    "id": "slv",
    "name": "El Salvador",
    "country": "El Salvador",
    "flag": "https://flagcdn.com/w320/sv.png",
    "group": null,
    "seed": 4
  },
  {
    "id": "gtm",
    "name": "Guatemala",
    "country": "Guatemala",
    "flag": "https://flagcdn.com/w320/gt.png",
    "group": null,
    "seed": 4
  },
  {
    "id": "cub",
    "name": "Cuba",
    "country": "Cuba",
    "flag": "https://flagcdn.com/w320/cu.png",
    "group": null,
    "seed": 4
  },
  {
    "id": "hti",
    "name": "Haití",
    "country": "Haití",
    "flag": "https://flagcdn.com/w320/ht.png",
    "group": null,
    "seed": 4
  },
  {
    "id": "tto",
    "name": "Trinidad y Tobago",
    "country": "Trinidad y Tobago",
    "flag": "https://flagcdn.com/w320/tt.png",
    "group": null,
    "seed": 4
  },
  {
    "id": "cuw",
    "name": "Curazao",
    "country": "Curazao",
    "flag": "https://flagcdn.com/w320/cw.png",
    "group": null,
    "seed": 4
  },
  {
    "id": "mrt",
    "name": "Martinica",
    "country": "Martinica",
    "flag": "https://flagcdn.com/w320/mq.png",
    "group": null,
    "seed": 4
  },
  {
    "id": "nic",
    "name": "Nicaragua",
    "country": "Nicaragua",
    "flag": "https://flagcdn.com/w320/ni.png",
    "group": null,
    "seed": 4
  },
  {
    "id": "blz",
    "name": "Belice",
    "country": "Belice",
    "flag": "https://flagcdn.com/w320/bz.png",
    "group": null,
    "seed": 4
  }
]
```

---

## 💻 Script para Agregar Equipos

### JavaScript/TypeScript (Node.js o Frontend)

```javascript
const API_URL = 'http://localhost:8080/football-pool/v1/api/competitions';
const CATEGORY = 'fifaNationalTeamCups';
const COMPETITION_ID = 'gold-cup';
const TOKEN = 'your-jwt-token-here';

const concacafTeams = [
  {
    id: "usa",
    name: "Estados Unidos",
    country: "Estados Unidos",
    flag: "https://flagcdn.com/w320/us.png",
    group: null,
    seed: 1
  },
  {
    id: "mex",
    name: "México",
    country: "México",
    flag: "https://flagcdn.com/w320/mx.png",
    group: null,
    seed: 1
  },
  {
    id: "can",
    name: "Canadá",
    country: "Canadá",
    flag: "https://flagcdn.com/w320/ca.png",
    group: null,
    seed: 2
  },
  {
    id: "crc",
    name: "Costa Rica",
    country: "Costa Rica",
    flag: "https://flagcdn.com/w320/cr.png",
    group: null,
    seed: 2
  },
  {
    id: "pan",
    name: "Panamá",
    country: "Panamá",
    flag: "https://flagcdn.com/w320/pa.png",
    group: null,
    seed: 3
  },
  {
    id: "jam",
    name: "Jamaica",
    country: "Jamaica",
    flag: "https://flagcdn.com/w320/jm.png",
    group: null,
    seed: 3
  },
  {
    id: "hon",
    name: "Honduras",
    country: "Honduras",
    flag: "https://flagcdn.com/w320/hn.png",
    group: null,
    seed: 4
  },
  {
    id: "slv",
    name: "El Salvador",
    country: "El Salvador",
    flag: "https://flagcdn.com/w320/sv.png",
    group: null,
    seed: 4
  },
  {
    id: "gtm",
    name: "Guatemala",
    country: "Guatemala",
    flag: "https://flagcdn.com/w320/gt.png",
    group: null,
    seed: 4
  },
  {
    id: "cub",
    name: "Cuba",
    country: "Cuba",
    flag: "https://flagcdn.com/w320/cu.png",
    group: null,
    seed: 4
  },
  {
    id: "hti",
    name: "Haití",
    country: "Haití",
    flag: "https://flagcdn.com/w320/ht.png",
    group: null,
    seed: 4
  },
  {
    id: "tto",
    name: "Trinidad y Tobago",
    country: "Trinidad y Tobago",
    flag: "https://flagcdn.com/w320/tt.png",
    group: null,
    seed: 4
  },
  {
    id: "cuw",
    name: "Curazao",
    country: "Curazao",
    flag: "https://flagcdn.com/w320/cw.png",
    group: null,
    seed: 4
  },
  {
    id: "mrt",
    name: "Martinica",
    country: "Martinica",
    flag: "https://flagcdn.com/w320/mq.png",
    group: null,
    seed: 4
  },
  {
    id: "nic",
    name: "Nicaragua",
    country: "Nicaragua",
    flag: "https://flagcdn.com/w320/ni.png",
    group: null,
    seed: 4
  },
  {
    id: "blz",
    name: "Belice",
    country: "Belice",
    flag: "https://flagcdn.com/w320/bz.png",
    group: null,
    seed: 4
  }
];

// Función para agregar un equipo
const addTeam = async (team) => {
  try {
    const response = await fetch(
      `${API_URL}/${CATEGORY}/${COMPETITION_ID}/teams`,
      {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${TOKEN}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(team),
      }
    );

    const result = await response.json();
    
    if (response.ok) {
      console.log(`✅ Equipo agregado: ${team.name}`);
      return result;
    } else {
      console.error(`❌ Error agregando ${team.name}:`, result.error);
      return null;
    }
  } catch (error) {
    console.error(`❌ Error al agregar ${team.name}:`, error);
    return null;
  }
};

// Agregar todos los equipos
const addAllTeams = async () => {
  console.log(`🚀 Agregando ${concacafTeams.length} equipos a Copa Oro CONCACAF...\n`);
  
  let successCount = 0;
  let errorCount = 0;
  
  for (const team of concacafTeams) {
    const result = await addTeam(team);
    if (result) {
      successCount++;
    } else {
      errorCount++;
    }
    
    // Pequeña pausa para no sobrecargar el servidor
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  
  console.log(`\n📊 Resumen:`);
  console.log(`   ✅ Agregados: ${successCount}`);
  console.log(`   ❌ Errores: ${errorCount}`);
};

// Ejecutar
addAllTeams();
```

### Usando Axios

```javascript
import axios from 'axios';

const API_URL = 'http://localhost:8080/football-pool/v1/api/competitions';
const CATEGORY = 'fifaNationalTeamCups';
const COMPETITION_ID = 'gold-cup';
const TOKEN = 'your-jwt-token-here';

const concacafTeams = [
  // ... (mismo array de equipos de arriba)
];

const addAllTeams = async () => {
  console.log(`🚀 Agregando ${concacafTeams.length} equipos...\n`);
  
  let successCount = 0;
  let errorCount = 0;
  
  for (const team of concacafTeams) {
    try {
      const response = await axios.post(
        `${API_URL}/${CATEGORY}/${COMPETITION_ID}/teams`,
        team,
        {
          headers: {
            'Authorization': `Bearer ${TOKEN}`,
            'Content-Type': 'application/json',
          },
        }
      );
      
      console.log(`✅ ${team.name} agregado`);
      successCount++;
    } catch (error) {
      console.error(`❌ Error con ${team.name}:`, error.response?.data || error.message);
      errorCount++;
    }
    
    // Pausa entre requests
    await new Promise(resolve => setTimeout(resolve, 100));
  }
  
  console.log(`\n📊 Resumen: ${successCount} exitosos, ${errorCount} errores`);
};

addAllTeams();
```

---

## 📋 Ejemplo de Request Individual

Para agregar un equipo individualmente:

**POST** `/football-pool/v1/api/competitions/fifaNationalTeamCups/gold-cup/teams`

**Body:**
```json
{
  "id": "usa",
  "name": "Estados Unidos",
  "country": "Estados Unidos",
  "flag": "https://flagcdn.com/w320/us.png",
  "group": null,
  "seed": 1
}
```

---

## ✅ Verificar Equipos Agregados

**GET** `/football-pool/v1/api/competitions/fifaNationalTeamCups/gold-cup/teams`

Este endpoint devuelve todos los equipos calificados de la competencia.

---

## 🔍 Notas

1. **IDs de Equipos:** Usa códigos ISO de 3 letras cuando sea posible (usa, mex, can, etc.)

2. **Flags:** Las URLs de flags usan `flagcdn.com` que es un servicio gratuito y confiable

3. **Seeds:** 
   - Seed 1: Potencias (USA, México)
   - Seed 2: Equipos fuertes (Canadá, Costa Rica)
   - Seed 3: Equipos medios (Panamá, Jamaica)
   - Seed 4: Resto de equipos

4. **Group:** Se puede dejar como `null` inicialmente y asignarse después cuando se formen los grupos

5. **Errores Comunes:**
   - Si un equipo ya existe, el endpoint puede devolver un error
   - Verifica que el `competitionId` y `category` sean correctos
   - Asegúrate de tener un token válido

---

## 🚀 Ejecución Rápida

Si tienes Node.js instalado, puedes crear un archivo `add-concacaf-teams.js` con el código de arriba y ejecutarlo:

```bash
node add-concacaf-teams.js
```

O si prefieres usar curl desde la terminal:

```bash
# Ejemplo para agregar Estados Unidos
curl -X POST "http://localhost:8080/football-pool/v1/api/competitions/fifaNationalTeamCups/gold-cup/teams" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "id": "usa",
    "name": "Estados Unidos",
    "country": "Estados Unidos",
    "flag": "https://flagcdn.com/w320/us.png",
    "group": null,
    "seed": 1
  }'
```

