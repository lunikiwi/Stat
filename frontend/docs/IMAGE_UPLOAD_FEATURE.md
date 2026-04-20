# Image Upload Feature - Frontend Implementation

## Overview

The chat interface now supports image upload/capture functionality, allowing users to send meal photos to the AI Coach for nutritional analysis.

## Implementation Details

### 1. Type Updates

**File:** [`frontend/src/types/index.ts`](../src/types/index.ts)

Added optional `imageBase64` field to the `ChatRequest` interface:

```typescript
export interface ChatRequest {
  currentMessage: string;
  chatHistory: ChatMessage[];
  imageBase64?: string | null;
}
```

### 2. Chat Component Updates

**File:** [`frontend/src/views/Chat.vue`](../src/views/Chat.vue)

#### New State Variables

- `fileInput`: Reference to the hidden file input element
- `selectedImage`: Stores the Base64-encoded image data
- `imagePreviewUrl`: Stores the object URL for image preview

#### New Functions

**`fileToBase64(file: File): Promise<string>`**
- Converts a File object to a Base64 string
- Removes the data URL prefix to send only the Base64 data
- Returns a Promise that resolves with the Base64 string

**`handleImageSelect(event: Event)`**
- Handles file selection from the input
- Validates file type (must be an image)
- Validates file size (max 5MB)
- Converts the image to Base64 and creates a preview URL

**`removeImage()`**
- Clears the selected image and preview
- Revokes the object URL to free memory
- Resets the file input

**`triggerFileInput()`**
- Programmatically triggers the hidden file input click

#### Updated `handleSubmit()` Function

- Now accepts messages with or without text (image-only messages allowed)
- Displays "📷 Bild gesendet" if no text is provided with the image
- Sends the image as `imageBase64` in the API request
- Clears the image after sending

### 3. UI Components

#### Image Upload Button

- Icon button with camera/image icon
- Located to the left of the text input
- Disabled when loading
- Opens file picker when clicked

#### Image Preview

- Displays selected image as a 80x80px thumbnail
- Shows above the input area
- Has a red "×" button to remove the image
- Border highlights the selected image

#### Updated Send Button

- Now enabled when either text is entered OR an image is selected
- Disabled only when both are empty or when loading

## User Flow

1. **Select Image:**
   - User clicks the image upload button (camera icon)
   - File picker opens (accepts image/* files)
   - User selects an image from their device

2. **Preview:**
   - Selected image appears as a thumbnail above the input
   - User can remove the image by clicking the "×" button

3. **Send:**
   - User can optionally add text to accompany the image
   - User clicks "Senden" button
   - Image is converted to Base64 and sent to the backend

4. **Backend Processing:**
   - Backend receives the image in the `imageBase64` field
   - Gemini analyzes the meal photo
   - Estimates nutritional values
   - Logs the nutrition data to InfluxDB
   - Returns confirmation to the user

## Validation

- **File Type:** Only image files are accepted
- **File Size:** Maximum 5MB per image
- **Error Handling:** User-friendly error messages for validation failures

## Mobile Optimization

- The image button is touch-friendly (adequate size)
- File picker on mobile devices allows camera capture
- Image preview is appropriately sized for mobile screens
- Rounded corners and clean design match the mobile-first approach

## Technical Notes

- Images are converted to Base64 before sending (no multipart/form-data)
- Object URLs are properly revoked to prevent memory leaks
- The file input is hidden and triggered programmatically for better UX
- The implementation is fully compatible with the existing backend API

## Testing Checklist

- [x] Image selection works
- [x] Image preview displays correctly
- [x] Image can be removed before sending
- [x] File type validation works
- [x] File size validation works (5MB limit)
- [x] Base64 conversion works correctly
- [x] API request includes imageBase64 field
- [x] Send button state updates correctly
- [x] Mobile-responsive design
- [x] Dark mode support

## Browser Compatibility

- Modern browsers with FileReader API support
- Mobile browsers with camera access
- Tested on: Chrome, Firefox, Safari, Edge

## Future Enhancements

- Add drag-and-drop support
- Add image compression before upload
- Support multiple images
- Add image editing (crop, rotate)
- Show upload progress indicator
