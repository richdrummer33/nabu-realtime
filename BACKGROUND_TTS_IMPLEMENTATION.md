# Background TTS Implementation - SpeechForegroundService

## Overview

This implementation adds robust background TTS (Text-to-Speech) synthesis and playback capabilities to the Nabu app using a foreground service architecture. The service enables continuous speech processing that persists across screen navigation and when the app is backgrounded.

## Architecture

### Core Components

#### 1. SpeechForegroundService (`com.example.nabu.speech.SpeechForegroundService`)
A foreground service that manages the entire TTS pipeline:

- **Synthesis Worker**: Coroutine-based worker that chunks text and synthesizes audio
- **Playback Worker**: Coroutine-based worker that plays audio chunks in sequence
- **Bounded Buffering**: Channel with capacity 4 prevents memory overflow
- **State Management**: StateFlow exposes current state to UI
- **Audio Focus**: Handles system audio focus events properly

**Key Methods:**
- `speak(request: SpeechRequest)` - Start synthesis and playback
- `stop()` - Stop current operation
- `pause()` - Pause playback (user or focus loss)
- `resume()` - Resume paused playback

#### 2. SpeechState (`com.example.nabu.speech.SpeechState`)
Sealed class hierarchy representing all pipeline states:

```kotlin
sealed class SpeechState {
    object Idle
    object PreparingModels
    data class Chunking(val totalChunks: Int)
    data class Synthesizing(val currentChunk: Int, val totalChunks: Int)
    object Buffering
    data class Playing(val currentChunk: Int, val totalChunks: Int)
    data class Paused(val currentChunk: Int, val totalChunks: Int)
    data class Error(val message: String)
}
```

Each state has a `toStatusString()` method for UI display.

#### 3. TextChunker (`com.example.nabu.speech.TextChunker`)
Simple text chunking utility that splits text into manageable pieces:

- Sentence-based splitting (by `.`, `!`, `?`)
- Word-based fallback for long sentences
- Configurable max chunk length (default 500 chars)

#### 4. AudioFocusManager (`com.example.nabu.speech.AudioFocusManager`)
Handles Android audio focus:

- Requests focus before playback
- Pauses on transient focus loss
- Auto-resumes on focus regain
- Releases focus when done

#### 5. SpeechController (`com.example.nabu.speech.SpeechController`)
Interface for controlling the speech pipeline (implemented by service).

#### 6. SpeechRequest (`com.example.nabu.speech.SpeechRequest`)
Data class for speech requests:
```kotlin
data class SpeechRequest(
    val text: String,
    val style: String,
    val speed: Float,
    val shouldSave: Boolean = false
)
```

## UI Integration

### MainActivity Changes

1. **Service Binding**: MainActivity now binds to SpeechForegroundService on creation
2. **Service Lifecycle**: Properly manages service connection/disconnection
3. **Controller Injection**: Passes service reference to UI components

### BasicScreen Changes

1. **State Persistence**: Uses `rememberSaveable` for text, style, speed, and save flag
   - Text and settings persist across navigation
   - No data loss when switching tabs

2. **Service Integration**: Calls service methods instead of inline audio generation
   - `speechController.speak(SpeechRequest(...))` starts playback
   - Processing state derived from service StateFlow

3. **Fallback Support**: Maintains old behavior if service unavailable

### MainScreen Changes

1. **Global Status Line**: Displays speech state across all tabs
   - Shows current operation (synthesizing, playing, etc.)
   - Shows progress (e.g., "Playing 2/5")
   - Only visible when not idle

2. **State Collection**: Observes service StateFlow for real-time updates

## AndroidManifest Changes

Added necessary permissions and service declaration:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

<service
    android:name=".speech.SpeechForegroundService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="mediaPlayback" />
```

## Features

### ✅ Background Processing
- Speech synthesis continues when switching tabs
- Playback continues when app is backgrounded
- Notification shows current status

### ✅ State Persistence
- Text input persists across navigation
- Style and speed settings persist
- No model reloading on tab switches

### ✅ Audio Focus Handling
- Pauses when phone call or other audio starts
- Resumes automatically when focus returns
- Releases focus when done

### ✅ Progress Tracking
- Chunking progress
- Synthesis progress (chunk X of Y)
- Playback progress
- Error states

### ✅ Bounded Buffering
- Maximum 4 chunks buffered
- Prevents memory overflow
- Synthesis blocks when buffer full
- Playback waits when buffer empty (no crashes)

### ✅ Logging
- DebugLogger statements throughout
- State transitions logged
- Audio focus events logged
- Error conditions logged

## Testing

### Unit Tests

1. **TextChunkerTest**: Tests text chunking logic
   - Empty/blank text handling
   - Single vs multiple sentences
   - Long sentence splitting
   - Whitespace handling

2. **SpeechStateTest**: Tests state formatting
   - All state types
   - Status string generation
   - Progress display

### Manual Testing Required

Due to Android SDK dependencies, full build testing requires:

1. **Navigation Testing**:
   - Start playback on Basic screen
   - Switch to Mixer/Book tabs
   - Verify playback continues
   - Verify state persists when returning to Basic

2. **Background Testing**:
   - Start playback
   - Press home button
   - Verify notification shows
   - Verify playback continues
   - Return to app

3. **Audio Focus Testing**:
   - Start playback
   - Trigger phone call or play music
   - Verify automatic pause
   - End call/music
   - Verify automatic resume

4. **State Persistence Testing**:
   - Enter text and adjust settings
   - Switch tabs
   - Return to Basic screen
   - Verify text and settings preserved

## Usage Example

```kotlin
// In BasicScreen
val speechController: SpeechController? = ... // from service binding

// Start speech
speechController?.speak(
    SpeechRequest(
        text = "Hello world! This is a test.",
        style = "af_sky",
        speed = 1.0f,
        shouldSave = false
    )
)

// Observe state
val speechState by speechController.state.collectAsState()
when (speechState) {
    is SpeechState.Playing -> { /* Show playing UI */ }
    is SpeechState.Error -> { /* Show error */ }
    // ...
}

// Control playback
speechController?.pause()
speechController?.resume()
speechController?.stop()
```

## Logging

All significant events are logged via DebugLogger:

```
SpeechService: speak() called with text: Hello world...
SpeechService: Starting synthesis
SpeechService: Chunked text into 3 chunks
SpeechService: Synthesizing chunk 1/3
SpeechService: Chunk 1 synthesized in 245ms
AudioFocus: Request result = GRANTED
SpeechService: Received chunk 1/3 for playback
SpeechService: Finished playing chunk 1/3
AudioFocus: Change event = AUDIOFOCUS_LOSS_TRANSIENT
AudioFocus: Transient loss, pausing
AudioFocus: Change event = AUDIOFOCUS_GAIN
AudioFocus: Gained audio focus
AudioFocus: Resuming playback after focus gain
```

## Future Enhancements

Possible improvements (not in current scope):

1. **Pause/Resume UI Controls**: Add buttons in global status line
2. **Seek/Skip**: Allow skipping chunks or seeking position
3. **Queue Management**: Support multiple speech requests in queue
4. **Voice Mixing**: Integrate with existing mixer settings
5. **Concurrent Synthesis**: Allow configurable synthesis concurrency
6. **Persistent State**: Save/restore across app restarts
7. **Media Session**: Integrate with Android MediaSession for system controls

## Files Modified/Added

### New Files
- `app/src/main/java/com/example/nabu/speech/SpeechState.kt`
- `app/src/main/java/com/example/nabu/speech/SpeechRequest.kt`
- `app/src/main/java/com/example/nabu/speech/SpeechController.kt`
- `app/src/main/java/com/example/nabu/speech/AudioFocusManager.kt`
- `app/src/main/java/com/example/nabu/speech/TextChunker.kt`
- `app/src/main/java/com/example/nabu/speech/SpeechForegroundService.kt`
- `app/src/test/java/com/example/nabu/speech/TextChunkerTest.kt`
- `app/src/test/java/com/example/nabu/speech/SpeechStateTest.kt`

### Modified Files
- `app/src/main/AndroidManifest.xml` - Added service and permissions
- `app/src/main/java/com/example/nabu/MainActivity.kt` - Service binding and UI integration

## Notes

- Service uses Channel for bounded buffering (safer than unlimited queue)
- Playback and synthesis run in separate coroutines for clean separation
- Service properly handles lifecycle (onDestroy cleans up)
- Mutex protects AudioTrack access for thread safety
- Global status line only shows when state is not Idle (clean UI)
- rememberSaveable ensures no data loss on configuration changes
