# 📱 Facebook OAuth Authentication - Complete Guide

## 🎯 Overview

This system allows users to sign up/login using their Facebook account. The backend validates the Facebook access token, retrieves user information, and creates/updates the user profile in MongoDB.

---

## 🔄 Authentication Flow

```
┌─────────────┐         ┌──────────────┐         ┌──────────────┐         ┌─────────────┐
│   Frontend  │────1───▶│   Facebook   │────2───▶│   Backend    │────3───▶│   MongoDB   │
│ (React Nat.)│◀────6───│   OAuth      │         │   (Spring)   │◀────4───│             │
└─────────────┘         └──────────────┘         └──────────────┘         └─────────────┘
                                                         │
                                                         └────5───▶ Generate JWT tokens
```

### Step-by-Step:

1. **Frontend** initiates Facebook login and receives `accessToken`
2. **Frontend** sends `accessToken` to backend `/auth/social`
3. **Backend** validates token with Facebook Graph API
4. **Backend** creates/updates user in MongoDB
5. **Backend** generates JWT tokens (access + refresh)
6. **Backend** returns user data + tokens to frontend

---

## 📡 API Endpoints

### 1. **Social Authentication** (Facebook Login/Signup)

```http
POST /football-pool/v1/api/auth/social
```

**Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "accessToken": "EAAKx...",  // Facebook access token from frontend
  "provider": "facebook"       // Auth provider (only "facebook" supported now)
}
```

**Response (New User - Profile Incomplete):**
```json
{
  "_id": "673a1234567890abcdef1234",
  "email": "joleogon174@gmail.com",
  "name": "Joel",
  "lastName": "Leon Gonzalez",
  "facebookId": "facebook_user_id_123",
  "authProvider": "facebook",
  "profilePicture": "https://graph.facebook.com/...",
  "preferredTeams": [],
  "preferredLeagues": [],
  "profileIncomplete": true,
  "missingFields": ["preferredTeams", "preferredLeagues"],
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

**Response (Existing User - Profile Complete):**
```json
{
  "_id": "673a1234567890abcdef1234",
  "email": "joleogon174@gmail.com",
  "name": "Joel",
  "lastName": "Leon Gonzalez",
  "birth": "1990-01-01",
  "facebookId": "facebook_user_id_123",
  "authProvider": "facebook",
  "profilePicture": "https://graph.facebook.com/...",
  "preferredTeams": ["Real Madrid", "Barcelona"],
  "preferredLeagues": ["La Liga", "Champions League"],
  "country": "US",
  "state": "CA",
  "city": "LA",
  "phone": "+1234567890",
  "zipcode": "90001",
  "profileIncomplete": false,
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

**Error Responses:**
```json
// Missing access token
{
  "error": "Access token is required"
}

// Invalid provider
{
  "error": "Only Facebook provider is currently supported"
}

// Facebook validation failed
{
  "error": "Facebook authentication failed: Invalid Facebook access token"
}

// Facebook account without email
{
  "error": "Facebook account does not have an email associated"
}
```

---

### 2. **Complete Profile** (Add Required Fields)

```http
PUT /football-pool/v1/api/auth/complete-profile?userId=673a1234567890abcdef1234
```

**Headers:**
```
Content-Type: application/json
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Request Body (Required Fields):**
```json
{
  "preferredTeams": ["Real Madrid", "Barcelona"],
  "preferredLeagues": ["La Liga", "Champions League"]
}
```

**Request Body (With Optional Fields):**
```json
{
  "preferredTeams": ["Real Madrid", "Barcelona"],
  "preferredLeagues": ["La Liga", "Champions League"],
  "birth": "1990-01-01",
  "country": "US",
  "state": "CA",
  "city": "LA",
  "phone": "+1234567890",
  "zipcode": "90001"
}
```

**Response:**
```json
{
  "_id": "673a1234567890abcdef1234",
  "email": "joleogon174@gmail.com",
  "name": "Joel",
  "lastName": "Leon Gonzalez",
  "preferredTeams": ["Real Madrid", "Barcelona"],
  "preferredLeagues": ["La Liga", "Champions League"],
  "birth": "1990-01-01",
  "country": "US",
  "state": "CA",
  "city": "LA",
  "phone": "+1234567890",
  "zipcode": "90001",
  "profileIncomplete": false,
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "expiresIn": 86400
}
```

---

## 🗄️ MongoDB User Schema

### Fields for OAuth Users:

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `_id` | ObjectId | Auto | MongoDB unique identifier |
| `email` | String | ✅ Yes | User's email from Facebook |
| `name` | String | ✅ Yes | First name from Facebook |
| `lastName` | String | ✅ Yes | Last name from Facebook |
| `facebookId` | String | ✅ Yes | Facebook user ID |
| `authProvider` | String | ✅ Yes | "facebook" or "email" |
| `profilePicture` | String | No | URL to profile picture |
| `passwords` | Array | No | Empty for OAuth users |
| `preferredTeams` | Array | ⚠️ Later | Required for complete profile |
| `preferredLeagues` | Array | ⚠️ Later | Required for complete profile |
| `profileIncomplete` | Boolean | Auto | true if teams/leagues missing |
| `birth` | String | No | Optional |
| `country` | String | No | Optional |
| `state` | String | No | Optional |
| `city` | String | No | Optional |
| `phone` | String | No | Optional |
| `zipcode` | String | No | Optional |

### Example Document (New OAuth User):
```json
{
  "_id": ObjectId("673a1234567890abcdef1234"),
  "email": "joleogon174@gmail.com",
  "name": "Joel",
  "lastName": "Leon Gonzalez",
  "facebookId": "facebook_user_id_123",
  "authProvider": "facebook",
  "profilePicture": "https://graph.facebook.com/v12.0/123/picture?type=large",
  "passwords": [],
  "preferredTeams": [],
  "preferredLeagues": [],
  "profileIncomplete": true
}
```

---

## 🔧 Backend Components

### 1. **FacebookAuthService** (`FacebookAuthService.java`)

Validates Facebook access tokens and retrieves user information.

**Key Methods:**
- `validateTokenAndGetUserInfo(accessToken)` - Validates token with Facebook Graph API
- `extractProfilePicture(facebookResponse)` - Extracts profile picture URL
- `splitName(fullName)` - Splits full name into first and last name

**Facebook Graph API Request:**
```
GET https://graph.facebook.com/me?access_token={token}&fields=id,name,email,picture.type(large)
```

---

### 2. **AuthService** (`AuthService.java`)

Handles social authentication logic and profile completion.

**New Methods:**
- `authenticateWithSocial(accessToken, provider)` - Main OAuth login/signup handler
- `completeProfile(body, userId)` - Updates user profile with required fields
- `generateSocialAuthResponse(user, email)` - Helper to generate response with tokens

---

### 3. **AuthController** (`AuthController.java`)

Exposes REST endpoints for OAuth.

**New Endpoints:**
- `POST /auth/social` - Social authentication
- `PUT /auth/complete-profile` - Complete user profile

---

### 4. **SecurityConfig** (`SecurityConfig.java`)

Updated to allow public access to OAuth endpoints.

**Public Endpoints:**
- `/football-pool/v1/api/auth/social` - No authentication required
- `/football-pool/v1/api/auth/complete-profile` - JWT required

---

## 🧪 Testing with Postman

### Test 1: Facebook Login (New User)

```
POST http://localhost:8080/football-pool/v1/api/auth/social

Body:
{
  "accessToken": "YOUR_FACEBOOK_ACCESS_TOKEN",
  "provider": "facebook"
}

Expected Response:
- 200 OK
- User created
- profileIncomplete: true
- missingFields: ["preferredTeams", "preferredLeagues"]
```

### Test 2: Complete Profile

```
PUT http://localhost:8080/football-pool/v1/api/auth/complete-profile?userId=USER_ID

Headers:
Authorization: Bearer YOUR_JWT_TOKEN

Body:
{
  "preferredTeams": ["Real Madrid", "Barcelona"],
  "preferredLeagues": ["La Liga"]
}

Expected Response:
- 200 OK
- profileIncomplete: false
- Updated user data
```

### Test 3: Facebook Login (Existing User)

```
POST http://localhost:8080/football-pool/v1/api/auth/social

Body:
{
  "accessToken": "YOUR_FACEBOOK_ACCESS_TOKEN",
  "provider": "facebook"
}

Expected Response:
- 200 OK
- User found
- profileIncomplete: false (if profile was completed)
```

---

## 🎨 Frontend Integration

Your React Native frontend is already set up correctly! The key parts:

### 1. **Facebook Auth Hook** (`useFacebookAuth`)
```typescript
const { request, response, promptAsync } = useFacebookAuth();
```

### 2. **Handle Facebook Response**
```typescript
useEffect(() => {
  if (response?.type === 'success') {
    const { access_token } = response.params;
    handleFacebookLogin(access_token);
  }
}, [response]);
```

### 3. **Send to Backend**
```typescript
const handleFacebookLogin = async (accessToken: string) => {
  const userInfo = await socialAuthService({
    accessToken,
    provider: 'facebook'
  });
  
  if (userInfo.profileIncomplete) {
    // Show form to complete profile (preferredTeams, preferredLeagues)
  } else {
    // Login successful, navigate to home
    setLocalData({ 
      isAuthenticated: true, 
      username: userInfo.name, 
      token: userInfo.accessToken 
    });
  }
};
```

---

## ⚠️ Important Notes

### Security Considerations:
1. **Never store Facebook access tokens** - Only use them for validation
2. **Always validate tokens server-side** - Never trust frontend tokens
3. **Use HTTPS in production** - Protect tokens in transit
4. **Set short token expiration** - Access tokens expire in 24h

### OAuth vs Email Login:
- **OAuth users** have `passwords: []` (empty array)
- **Email users** have `passwords: ["password"]` (array with password)
- **authProvider** field identifies the login method

### Profile Completion:
- `profileIncomplete: true` → Show form for preferredTeams/Leagues
- `profileIncomplete: false` → Profile is complete, proceed to app
- `missingFields` array shows which fields are required

---

## 🚀 Quick Start Guide

1. **User clicks "Sign in with Facebook"** on frontend
2. **Facebook OAuth dialog** appears
3. **User authorizes app** and Facebook returns `access_token`
4. **Frontend sends** `access_token` to `/auth/social`
5. **Backend validates** token with Facebook
6. **Backend creates/updates** user in MongoDB
7. **Backend returns** user data + JWT tokens
8. **Frontend checks** `profileIncomplete`:
   - If `true` → Show form to add teams/leagues
   - If `false` → Navigate to home screen

---

## 📚 Related Documentation

- `JWT_AUTHENTICATION.md` - JWT token system
- `CORS_CONFIG.md` - CORS configuration for React Native
- `POSTMAN_GUIDE.md` - General API testing guide

---

## 🎯 Summary

✅ **Backend Complete:**
- Facebook OAuth validation
- User creation/update
- JWT token generation
- Profile completion endpoint

✅ **Frontend Ready:**
- Facebook login button configured
- Access token handling
- Profile completion flow

✅ **Security:**
- Server-side token validation
- JWT authentication
- CORS protection

---

🎉 **Everything is ready to test Facebook login!**

