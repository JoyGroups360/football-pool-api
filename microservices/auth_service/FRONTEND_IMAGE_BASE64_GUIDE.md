# 📸 Guía Frontend: Enviar Imagen de Perfil en Base64

## 🎯 Resumen Rápido

El frontend debe enviar la imagen como un **string base64** en el campo `profileImage` dentro del body JSON del request.

---

## ✅ Formato Requerido

### Opción 1: Base64 con Data URI (Recomendado)
```javascript
{
  "profileImage": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD..."
}
```

### Opción 2: Base64 Puro
```javascript
{
  "profileImage": "iVBORw0KGgoAAAANSUhEUgAA..."
}
```

---

## 📋 Endpoint para Actualizar Imagen

**PATCH** `/football-pool/v1/api/auth/id?userId={userId}`

**Headers:**
```
Authorization: Bearer {token}
Content-Type: application/json
```

**Body:**
```json
{
  "profileImage": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD..."
}
```

---

## 💻 Ejemplos de Código para Frontend

### 🔵 React / JavaScript (Web)

```javascript
// Función para convertir File a Base64
const fileToBase64 = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file); // Esto ya devuelve el formato data:image/...;base64,...
    reader.onload = () => resolve(reader.result);
    reader.onerror = (error) => reject(error);
  });
};

// Ejemplo de uso en componente
const handleImageUpload = async (event) => {
  const file = event.target.files?.[0];
  
  if (!file) return;
  
  // 1. Validar tamaño (máx 2MB)
  if (file.size > 2 * 1024 * 1024) {
    alert('La imagen debe ser menor a 2MB');
    return;
  }
  
  // 2. Validar que sea una imagen
  if (!file.type.startsWith('image/')) {
    alert('Solo se permiten archivos de imagen');
    return;
  }
  
  try {
    // 3. Convertir a base64 (ya viene en formato data URI)
    const base64Image = await fileToBase64(file);
    
    // 4. Enviar al backend
    const response = await fetch(
      `/football-pool/v1/api/auth/id?userId=${userId}`,
      {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          profileImage: base64Image
        })
      }
    );
    
    const result = await response.json();
    console.log('Imagen actualizada:', result);
  } catch (error) {
    console.error('Error al actualizar imagen:', error);
  }
};

// En tu componente JSX
<input 
  type="file" 
  accept="image/*" 
  onChange={handleImageUpload}
/>
```

---

### 📱 React Native (Expo)

```javascript
import * as ImagePicker from 'expo-image-picker';
import * as FileSystem from 'expo-file-system';
import * as ImageManipulator from 'expo-image-manipulator';

const pickAndUploadImage = async (userId, token) => {
  try {
    // 1. Solicitar permisos
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      alert('Se necesitan permisos para acceder a la galería');
      return;
    }

    // 2. Seleccionar imagen
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [1, 1], // Imagen cuadrada
      quality: 0.8, // Compresión
    });

    if (result.canceled) return;

    // 3. Comprimir imagen (opcional pero recomendado)
    const manipulatedImage = await ImageManipulator.manipulateAsync(
      result.assets[0].uri,
      [{ resize: { width: 400 } }], // Redimensionar a ancho máximo
      { compress: 0.7, format: ImageManipulator.SaveFormat.JPEG }
    );

    // 4. Convertir a base64
    const base64 = await FileSystem.readAsStringAsync(
      manipulatedImage.uri,
      {
        encoding: FileSystem.EncodingType.Base64,
      }
    );

    // 5. Crear data URI
    const mimeType = 'image/jpeg';
    const dataUri = `data:${mimeType};base64,${base64}`;

    // 6. Enviar al backend
    const response = await fetch(
      `${API_URL}/football-pool/v1/api/auth/id?userId=${userId}`,
      {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          profileImage: dataUri
        })
      }
    );

    const result = await response.json();
    console.log('Imagen actualizada:', result);
    return result;
  } catch (error) {
    console.error('Error al subir imagen:', error);
    throw error;
  }
};

// Uso en componente
const ProfileScreen = () => {
  const handleChangeImage = async () => {
    const updatedUser = await pickAndUploadImage(userId, token);
    setUser(updatedUser);
  };

  return (
    <View>
      {user?.profileImage && (
        <Image 
          source={{ uri: user.profileImage }} 
          style={{ width: 100, height: 100, borderRadius: 50 }}
        />
      )}
      <Button title="Cambiar Imagen" onPress={handleChangeImage} />
    </View>
  );
};
```

---

### 🔵 React Native (Sin Expo - FileReader)

```javascript
import { launchImageLibrary } from 'react-native-image-picker';
import RNFS from 'react-native-fs';

const pickAndUploadImage = async (userId, token) => {
  try {
    // 1. Seleccionar imagen
    const result = await new Promise((resolve, reject) => {
      launchImageLibrary(
        {
          mediaType: 'photo',
          quality: 0.8,
          maxWidth: 400,
          maxHeight: 400,
        },
        (response) => {
          if (response.didCancel) reject(new Error('User cancelled'));
          else if (response.errorMessage) reject(new Error(response.errorMessage));
          else resolve(response);
        }
      );
    });

    if (!result.assets || result.assets.length === 0) return;

    const imageUri = result.assets[0].uri;

    // 2. Convertir a base64
    const base64 = await RNFS.readFile(imageUri, 'base64');
    
    // 3. Crear data URI
    const mimeType = result.assets[0].type || 'image/jpeg';
    const dataUri = `data:${mimeType};base64,${base64}`;

    // 4. Enviar al backend
    const response = await fetch(
      `${API_URL}/football-pool/v1/api/auth/id?userId=${userId}`,
      {
        method: 'PATCH',
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          profileImage: dataUri
        })
      }
    );

    const result = await response.json();
    return result;
  } catch (error) {
    console.error('Error al subir imagen:', error);
    throw error;
  }
};
```

---

## 🔧 Usando Axios / Fetch con Async/Await

```javascript
// Función helper
const updateProfileImage = async (userId, base64Image, token) => {
  try {
    const response = await axios.patch(
      `/football-pool/v1/api/auth/id?userId=${userId}`,
      {
        profileImage: base64Image
      },
      {
        headers: {
          'Authorization': `Bearer ${token}`,
          'Content-Type': 'application/json',
        }
      }
    );
    return response.data;
  } catch (error) {
    console.error('Error updating profile image:', error);
    throw error;
  }
};

// Uso
const handleImageChange = async (file) => {
  const base64Image = await fileToBase64(file);
  const updatedUser = await updateProfileImage(userId, base64Image, token);
  setUser(updatedUser);
};
```

---

## ⚠️ Validaciones Importantes en Frontend

### ✅ Antes de Enviar

1. **Tamaño máximo:** 2MB (recomendado: 500KB - 1MB)
2. **Tipo de archivo:** Solo imágenes (`image/*`)
3. **Formato:** PNG, JPEG, GIF, WebP
4. **Compresión:** Siempre comprimir antes de convertir a base64

### 📝 Código de Validación

```javascript
const validateImage = (file) => {
  // Validar tamaño
  const maxSize = 2 * 1024 * 1024; // 2MB
  if (file.size > maxSize) {
    throw new Error('La imagen debe ser menor a 2MB');
  }

  // Validar tipo
  if (!file.type.startsWith('image/')) {
    throw new Error('Solo se permiten archivos de imagen');
  }

  // Validar formato
  const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
  if (!allowedTypes.includes(file.type)) {
    throw new Error('Formato de imagen no soportado');
  }

  return true;
};
```

---

## 🗑️ Eliminar Imagen de Perfil

Para eliminar la imagen, envía `null` o `""`:

```javascript
await fetch(
  `/football-pool/v1/api/auth/id?userId=${userId}`,
  {
    method: 'PATCH',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      profileImage: null  // o ""
    })
  }
);
```

---

## 📊 Mostrar Imagen en Frontend

La imagen viene en el mismo formato base64 que enviaste:

```javascript
// React Native
<Image 
  source={{ uri: user.profileImage }} 
  style={styles.avatar}
/>

// Web (HTML)
<img 
  src={user.profileImage} 
  alt="Profile"
  style={{ width: 100, height: 100, borderRadius: '50%' }}
/>

// React Web
<img 
  src={user.profileImage || defaultAvatar} 
  alt="Profile"
/>
```

---

## ✅ Checklist de Implementación

- [ ] Función para convertir imagen a base64
- [ ] Validar tamaño (máx 2MB) antes de convertir
- [ ] Validar tipo MIME (solo imágenes)
- [ ] Comprimir imagen antes de convertir (opcional pero recomendado)
- [ ] Crear data URI con formato `data:image/type;base64,{base64String}`
- [ ] Enviar en campo `profileImage` del body JSON
- [ ] Incluir header `Authorization: Bearer {token}`
- [ ] Incluir header `Content-Type: application/json`
- [ ] Manejar errores de red/API
- [ ] Actualizar UI después de actualización exitosa

---

## 🚀 Ejemplo Completo Mínimo

```javascript
// 1. Convertir File a Base64
const fileToBase64 = (file) => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result); // Ya viene como data:image/...;base64,...
    reader.onerror = reject;
  });
};

// 2. Actualizar imagen
const updateProfileImage = async (file) => {
  // Validar
  if (file.size > 2 * 1024 * 1024) {
    alert('Imagen demasiado grande (máx 2MB)');
    return;
  }

  // Convertir
  const base64Image = await fileToBase64(file);

  // Enviar
  const response = await fetch(
    `/football-pool/v1/api/auth/id?userId=${userId}`,
    {
      method: 'PATCH',
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({ profileImage: base64Image })
    }
  );

  return await response.json();
};

// 3. Usar
<input 
  type="file" 
  accept="image/*" 
  onChange={(e) => updateProfileImage(e.target.files[0])}
/>
```

---

## 📝 Notas Importantes

1. **FileReader.readAsDataURL()** ya devuelve el formato completo `data:image/...;base64,...` - **NO necesitas agregarlo manualmente**
2. El backend acepta tanto el formato completo (`data:image/...`) como base64 puro
3. **Siempre comprime** las imágenes antes de enviar para reducir el tamaño
4. Para React Native, usa librerías como `expo-image-manipulator` o `react-native-image-picker` con compresión

