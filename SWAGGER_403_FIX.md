# 403 Forbidden Error - Troubleshooting Guide

**Last Updated**: 2026-03-03  
**Issue**: Getting 403 Forbidden error when testing APIs in Swagger

---

## Problem Summary

```
Error: 403 Forbidden
Message: Access denied or Unauthorized
```

This error occurs when:
- ❌ JWT token is missing
- ❌ JWT token is invalid or expired
- ❌ User role doesn't have required permissions
- ❌ Authorization header not properly formatted
- ❌ CORS headers missing

---

## Quick Fix (Immediate Solution)

### Step 1: Get a Valid JWT Token

Go to **POST /api/auth/login** in Swagger and test with these credentials:

**Endpoint**: `POST /api/auth/login`

**Request Body**:
```json
{
  "username": "passenger1",
  "password": "Pass@123"
}
```

**Click "Execute"** → Copy the token from response:

```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidXNlciI6InBhc3NlbmdlcjEiLCJyb2xlIjoiUEFTU0VOR0VSIiwiaWF0IjoxNjc4MDExNDQ1LCJleHAiOjE2NzgwOTc4NDV9.abcdefghijklmnop",
    "expiresIn": 86400,
    "tokenType": "Bearer"
  },
  "message": "Login successful"
}
```

**⭐ Copy entire token string** (the long `eyJhbGc...` part)

---

### Step 2: Add Token to Swagger Authorization

1. **Locate the "Authorize" button** in Swagger UI (top-right corner)
   
   ![Authorize Button](button-location)

2. **Click "Authorize"** button

3. **In the popup dialog**, paste your token with "Bearer " prefix:
   ```
   Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxIiwidXNlciI6InBhc3NlbmdlcjEiLCJyb2xlIjoiUEFTU0VOR0VSIiwiaWF0IjoxNjc4MDExNDQ1LCJleHAiOjE2NzgwOTc4NDV9.abcdefghijklmnop
   ```

4. **Click "Authorize"** button in dialog

5. **Click "Close"** to dismiss dialog

---

### Step 3: Now Test Your API

Go to any protected endpoint (e.g., **GET /api/booking/list**) and click "Try it out" → "Execute"

**✅ It should now work with 200 OK response**

---

## Detailed Troubleshooting

### Issue 1: "No Authorization Header" (403)

**Symptom**: 
```json
{
  "success": false,
  "message": "Authentication required",
  "timestamp": "2026-03-03T10:05:00.123Z"
}
```

**Fix**:
1. Follow "Quick Fix" steps above
2. Ensure token starts with "Bearer " (case-sensitive)
3. Clear Swagger cache (F12 → Application → Clear All)
4. Refresh browser page

---

### Issue 2: "Invalid or Expired Token" (401/403)

**Symptom**:
```json
{
  "success": false,
  "message": "Invalid or expired token",
  "timestamp": "2026-03-03T10:06:00.123Z"
}
```

**Fix**:
1. Token expired after 24 hours
2. **Login again** to get a new token
3. **Remove old token**: Click "Authorize" → Clear field → "Authorize"
4. **Add new token** with "Bearer " prefix

**How to check token expiration**:
```
Token expires in: expiresIn (seconds) = 86400 seconds = 24 hours
```

---

### Issue 3: "Insufficient Permissions" (403)

**Symptom**:
```json
{
  "success": false,
  "message": "Access denied. Admin role required.",
  "timestamp": "2026-03-03T10:07:00.123Z"
}
```

**Cause**: You're using a PASSENGER token to access ADMIN-only endpoint

**Fix**:
- **For Admin Endpoints**: Login with admin account
  ```
  Username: admin
  Password: Pass@123
  ```
- **For Passenger Endpoints**: Login with passenger account
  ```
  Username: passenger1
  Password: Pass@123
  ```

**Admin-Only Endpoints**:
- `GET /api/admin/users`
- `PUT /api/admin/users/{userId}`
- `GET /api/admin/audit-logs`

**Passenger-Only Endpoints**:
- `POST /api/booking/initiate`
- `POST /api/booking/confirm`
- `GET /api/booking/list`
- `POST /api/booking/cancel`

---

### Issue 4: "Invalid Token Format" (403)

**Symptom**: Token doesn't start with "Bearer "

**Wrong Format**:
```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ...
```

**Correct Format**:
```
Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ...
```

**Fix**:
1. Click "Authorize"
2. Clear the field
3. Type `Bearer ` (with space after it)
4. Paste your token after "Bearer "
5. Click "Authorize"

---

### Issue 5: Manual Header Addition (Alternative Method)

If Swagger "Authorize" button doesn't work:

**For Each API Call**:
1. Click "Try it out"
2. Scroll down to **Headers** section
3. Add custom header:
   ```
   Header Name: Authorization
   Header Value: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ...
   ```
4. Click "Execute"

---

## Step-by-Step Complete Workflow

### Scenario: Test "Create Booking" Endpoint (Requires Auth)

#### Step 1: Start Fresh
1. Open browser: `http://localhost:8080/swagger-ui.html`
2. Press `F5` to refresh (clear cache)

#### Step 2: Register (if new user)
1. Find **POST /api/auth/register**
2. Click "Try it out"
3. Enter body:
   ```json
   {
     "username": "testuser123",
     "email": "testuser@example.com",
     "password": "Pass@123",
     "role": "PASSENGER"
   }
   ```
4. Click "Execute"
5. Response should be: **201 Created**

#### Step 3: Login to Get Token
1. Find **POST /api/auth/login**
2. Click "Try it out"
3. Enter body:
   ```json
   {
     "username": "testuser123",
     "password": "Pass@123"
   }
   ```
4. Click "Execute"
5. **Copy the token** from response (the `"token"` value)

#### Step 4: Add Token to Swagger
1. Click **"Authorize"** button (top-right)
2. In dialog, paste:
   ```
   Bearer <YOUR_TOKEN_HERE>
   ```
3. Click "Authorize"
4. Click "Close"

#### Step 5: Test Protected Endpoint
1. Find **POST /api/booking/initiate**
2. Click "Try it out"
3. Enter request body:
   ```json
   {
     "scheduleId": 1,
     "seatIds": [1, 2],
     "passengerDetails": [
       {
         "name": "John Doe",
         "email": "john@example.com",
         "phone": "+1-555-0100",
         "dateOfBirth": "1990-01-15"
       }
     ]
   }
   ```
4. Click "Execute"
5. **✅ Response should be 201 Created** (not 403)

---

## Common Authorization Scenarios

### Scenario A: Testing Search (Public, No Auth Required)

**Endpoint**: `GET /api/search`

**Status**: 🟢 **PUBLIC** - No token needed

**Steps**:
1. Click "Try it out"
2. Enter query parameters (no authorization needed)
3. Click "Execute"
4. Should work without token

---

### Scenario B: Testing Booking (Protected, Auth Required)

**Endpoint**: `POST /api/booking/initiate`

**Status**: 🔒 **PROTECTED** - Requires PASSENGER token

**Steps**:
1. ✅ Get token from login (see Step 2 above)
2. ✅ Add token via "Authorize" button
3. Click "Try it out"
4. Enter request body
5. Click "Execute"
6. Should return 201 Created

---

### Scenario C: Testing Admin (Admin Only)

**Endpoint**: `GET /api/admin/users`

**Status**: 🔴 **ADMIN ONLY** - Requires ADMIN token

**Steps**:
1. ✅ Login with **admin** account:
   ```json
   {
     "username": "admin",
     "password": "Pass@123"
   }
   ```
2. ✅ Copy ADMIN token
3. ✅ Click "Authorize" and paste admin token
4. Click "Try it out"
5. Click "Execute"
6. Should return 200 OK with user list

---

## Token Structure (Reference)

### JWT Token Anatomy

```
eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.
eyJzdWIiOiIxIiwidXNlciI6InBhc3NlbmdlcjEiLCJyb2xlIjoiUEFTU0VOR0VSIiwiaWF0IjoxNjc4MDExNDQ1LCJleXAiOjE2NzgwOTc4NDV9.
abcdefghijklmnop

├─ Part 1: Header (Base64)
│  {
│    "alg": "HS256",
│    "typ": "JWT"
│  }
│
├─ Part 2: Payload (Base64)
│  {
│    "sub": "1",              ← User ID
│    "user": "passenger1",    ← Username
│    "role": "PASSENGER",     ← User role
│    "iat": 1678011445,       ← Issued at
│    "exp": 1678097845        ← Expires at (24 hours later)
│  }
│
└─ Part 3: Signature (HMAC)
   Generated using secret key
```

---

## Browser Console Debugging

If you still see 403, check browser console for details:

**Steps**:
1. Press `F12` to open Developer Tools
2. Go to **Console** tab
3. Look for error messages
4. Go to **Network** tab
5. Click the failed request
6. Check **Headers** tab for:
   - ✅ `Authorization: Bearer ...`
7. Check **Response** tab for exact error

---

## Common Error Messages & Fixes

| Error Message | Cause | Fix |
|---------------|-------|-----|
| "Authentication required" | No token | Add token via Authorize button |
| "Invalid or expired token" | Wrong/expired token | Login again, get new token |
| "Access denied. Admin role required" | Wrong role | Login with admin account |
| "User not found" | Invalid username | Check username spelling |
| "Invalid password" | Wrong password | Check password spelling |
| "Bearer prefix missing" | Malformed token | Add "Bearer " before token |

---

## Quick Reference: Test Accounts

| Account | Username | Password | Role | Use Case |
|---------|----------|----------|------|----------|
| **Passenger** | passenger1 | Pass@123 | PASSENGER | Test booking, search, payment |
| **Passenger** | passenger2 | Pass@123 | PASSENGER | Test multi-user scenarios |
| **Admin** | admin | Pass@123 | ADMIN | Test admin, audit logs, user mgmt |
| **Operator** | operator1 | Pass@123 | OPERATOR | Test operator functions |

---

## Checklist for 403 Error Resolution

- [ ] **Step 1**: Run `POST /api/auth/login` and get token
- [ ] **Step 2**: Copy entire token (including "eyJ..." part)
- [ ] **Step 3**: Click "Authorize" button in Swagger
- [ ] **Step 4**: Paste token with "Bearer " prefix
- [ ] **Step 5**: Click "Authorize" in dialog
- [ ] **Step 6**: Close dialog
- [ ] **Step 7**: Retry your API call
- [ ] **Step 8**: Verify response is 200/201 (not 403)

---

## Additional Resources

- [JWT Documentation](https://tools.ietf.org/html/rfc7519)
- [Spring Security Guide](https://spring.io/guides/gs/securing-web)
- [Swagger/OpenAPI Auth](https://swagger.io/docs/specification/authentication/)

---

**Status**: ✅ Your 403 error should be resolved!

If you still see 403 after following these steps:
1. Check that application is running: `mvn spring-boot:run`
2. Verify database is initialized
3. Try clearing browser cache: `Ctrl+Shift+Delete`
4. Restart the application
5. Get a fresh token and try again

