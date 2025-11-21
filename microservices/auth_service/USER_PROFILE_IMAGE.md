# 📸 Guía: Imagen de Perfil del Usuario (Base64)

## 📋 Resumen

El documento `User` en `auth_service` ahora soporta guardar la imagen de perfil del usuario como **base64**. Este campo permite almacenar imágenes directamente en MongoDB sin necesidad de un servicio de almacenamiento externo.

---

## 🗂️ Estructura del Campo

### Campo: `profileImage`

**Tipo:** `String`  
**Formato:** Base64 encoded image string  
**Opcional:** Sí  
**Tamaño máximo recomendado:** ~2MB (imágenes más grandes pueden afectar el rendimiento)

---

## 📝 Formatos Soportados

### Opción 1: Base64 con Data URI (Recomendado)

```
data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...
```

### Opción 2: Base64 Puro

```
iVBORw0KGgoAAAANSUhEUgAA...
```

**Nota:** Ambos formatos son aceptados. El backend guardará el string tal como se envía.

---

## 🔧 Endpoints que Soportan `profileImage`

### 1. Crear Usuario

**POST** `/football-pool/v1/api/auth/create`

**Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "confirmPassword": "password123",
  "name": "John",
  "lastName": "Doe",
  "profileImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
}
```

**Respuesta:**
```json
{
  "_id": "6908d0864077087454146d5c",
  "email": "user@example.com",
  "name": "John",
  "lastName": "Doe",
  "profileImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
  ...
}
```

---

### 2. Actualizar Usuario (PATCH)

**PATCH** `/football-pool/v1/api/auth/id?userId={userId}`

**Body:**
```json
{
  "profileImage": "data:image/jpeg;base64,/9j/4AAQSkZJRgABAQEAYABgAAD..."
}
```

**Para eliminar la imagen:**
```json
{
  "profileImage": null
}
```

O:
```json
{
  "profileImage": ""
}
```

---

### 3. Completar Perfil (Para usuarios de OAuth)

**PUT** `/football-pool/v1/api/auth/complete-profile?userId={userId}`

**Body:**
```json
{
  "preferredTeams": ["Real Madrid", "Barcelona"],
  "preferredLeagues": ["La Liga", "Champions League"],
  "profileImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA..."
}
```

---

## 💻 Ejemplos de Uso desde Frontend

### React Native - Convertir imagen a Base64

```typescript
import * as ImagePicker from 'expo-image-picker';
import * as FileSystem from 'expo-file-system';

export const pickAndConvertImage = async (): Promise<string | null> => {
  try {
    // Solicitar permisos
    const { status } = await ImagePicker.requestMediaLibraryPermissionsAsync();
    if (status !== 'granted') {
      throw new Error('Permission to access media library is required');
    }

    // Abrir selector de imagen
    const result = await ImagePicker.launchImageLibraryAsync({
      mediaTypes: ImagePicker.MediaTypeOptions.Images,
      allowsEditing: true,
      aspect: [1, 1], // Imagen cuadrada para perfil
      quality: 0.8, // Calidad de compresión
    });

    if (result.canceled) {
      return null;
    }

    // Convertir a base64
    const base64 = await FileSystem.readAsStringAsync(result.assets[0].uri, {
      encoding: FileSystem.EncodingType.Base64,
    });

    // Construir data URI
    const mimeType = result.assets[0].mimeType || 'image/jpeg';
    const dataUri = `data:${mimeType};base64,${base64}`;

    return dataUri;
  } catch (error) {
    console.error('Error picking image:', error);
    return null;
  }
};

// Uso en componente
const handleUpdateProfileImage = async () => {
  const base64Image = await pickAndConvertImage();
  if (base64Image) {
    // Actualizar perfil
    await updateUserProfile({
      profileImage: base64Image,
    });
  }
};
```

---

### JavaScript/TypeScript - Convertir File a Base64

```typescript
export const fileToBase64 = (file: File): Promise<string> => {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => {
      const result = reader.result as string;
      resolve(result); // Ya incluye "data:image/..."
    };
    reader.onerror = (error) => reject(error);
  });
};

// Uso en formulario
const handleFileChange = async (event: React.ChangeEvent<HTMLInputElement>) => {
  const file = event.target.files?.[0];
  if (file) {
    // Validar tamaño (máx 2MB)
    if (file.size > 2 * 1024 * 1024) {
      alert('La imagen debe ser menor a 2MB');
      return;
    }

    // Validar tipo
    if (!file.type.startsWith('image/')) {
      alert('Solo se permiten imágenes');
      return;
    }

    // Convertir a base64
    const base64Image = await fileToBase64(file);
    
    // Actualizar perfil
    await updateUserProfile({
      profileImage: base64Image,
    });
  }
};
```

---

## 🔍 Obtener Imagen del Usuario

Cuando obtienes un usuario (login, get user, etc.), el campo `profileImage` viene incluido:

```json
{
  "_id": "6908d0864077087454146d5c",
  "email": "user@example.com",
  "name": "John",
  "profileImage": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...",
  ...
}
```

**Mostrar en React Native:**
```typescript
import { Image } from 'react-native';

const ProfileImage = ({ profileImage }) => {
  if (profileImage) {
    return (
      <Image
        source={{ uri: profileImage }} // Funciona directamente con data URI
        style={{ width: 100, height: 100, borderRadius: 50 }}
      />
    );
  }
  return <DefaultAvatar />;
};
```

**Mostrar en Web (HTML):**
```html
<img 
  src="${user.profileImage}" 
  alt="Profile" 
  style="width: 100px; height: 100px; border-radius: 50%;"
/>
```

---

## ⚠️ Consideraciones Importantes

### 1. Tamaño de la Imagen

- **Recomendado:** Máximo 500KB - 1MB
- **Máximo:** 2MB (aunque MongoDB soporta más, no es recomendable)
- **Impacto:** Imágenes grandes aumentan el tamaño del documento y pueden afectar el rendimiento

### 2. Compresión

Siempre comprime las imágenes antes de convertirlas a base64:

```typescript
// React Native con expo-image-manipulator
import * as ImageManipulator from 'expo-image-manipulator';

const compressImage = async (uri: string) => {
  const manipulatedImage = await ImageManipulator.manipulateAsync(
    uri,
    [{ resize: { width: 400 } }], // Redimensionar a ancho máximo
    { compress: 0.7, format: ImageManipulator.SaveFormat.JPEG }
  );
  return manipulatedImage.uri;
};
```

### 3. Formatos Soportados

- PNG (recomendado para transparencias)
- JPEG (recomendado para fotos, mejor compresión)
- GIF
- WebP (soportado en navegadores modernos)

### 4. Seguridad

- El backend **NO valida** el contenido de la imagen, solo acepta el string base64
- Es recomendable validar en el frontend antes de enviar:
  - Tamaño del archivo
  - Tipo MIME
  - Dimensiones (opcional)

---

## 🔄 Diferencia entre `profileImage` y `profilePicture`

- **`profileImage`**: Base64 string (guardado por el usuario)
- **`profilePicture`**: URL string (viene de Facebook OAuth)

Ambos campos pueden coexistir. Si ambos están presentes, el frontend puede priorizar `profileImage`.

---

## 📊 Ejemplo Completo de Actualización

```typescript
// Servicio
export const updateUserProfileImage = async (
  userId: string,
  profileImageBase64: string
) => {
  const response = await apiClient.patch(
    `/auth/id?userId=${userId}`,
    {
      profileImage: profileImageBase64,
    },
    {
      headers: {
        'Authorization': `Bearer ${getToken()}`,
        'Content-Type': 'application/json',
      },
    }
  );
  return response.data;
};

// Componente
const ProfileScreen = () => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleImagePicker = async () => {
    setLoading(true);
    try {
      // 1. Seleccionar imagen
      const imageUri = await pickImage();
      if (!imageUri) return;

      // 2. Comprimir
      const compressedUri = await compressImage(imageUri);

      // 3. Convertir a base64
      const base64 = await FileSystem.readAsStringAsync(compressedUri, {
        encoding: FileSystem.EncodingType.Base64,
      });
      const mimeType = 'image/jpeg';
      const dataUri = `data:${mimeType};base64,${base64}`;

      // 4. Actualizar en backend
      const updatedUser = await updateUserProfileImage(user._id, dataUri);
      setUser(updatedUser);

      alert('Imagen actualizada exitosamente');
    } catch (error) {
      console.error('Error updating profile image:', error);
      alert('Error al actualizar la imagen');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View>
      {user?.profileImage && (
        <Image source={{ uri: user.profileImage }} style={styles.avatar} />
      )}
      <Button title="Cambiar Imagen" onPress={handleImagePicker} />
    </View>
  );
};
```

---

## ✅ Checklist de Implementación Frontend

- [ ] Función para convertir imagen a base64
- [ ] Validación de tamaño de archivo (máx 2MB)
- [ ] Validación de tipo MIME (solo imágenes)
- [ ] Compresión de imagen antes de convertir
- [ ] Manejo de errores al actualizar
- [ ] Mostrar imagen en componentes de perfil
- [ ] Fallback si no hay imagen (avatar por defecto)

---

## 🚀 Próximos Pasos

- [ ] Agregar endpoint específico para subir imagen (si se decide usar almacenamiento externo)
- [ ] Agregar validación de dimensiones en el backend
- [ ] Implementar redimensionamiento automático en el backend
- [ ] Considerar usar GridFS de MongoDB para imágenes muy grandes (>2MB)

