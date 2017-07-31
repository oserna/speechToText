package hello.async;

public enum EventType {

    VOICE_CHUNK_RECORDED,   // [AudioLoop]      ->  [AudioProcessor]        to get the intent
    VOICE_CHUNK_ANALYZED,   // [AudioProcessor] ->  [SpeakerImpl]           to tell the customer the info
    CONVERSATION_ENDED,     // [AudioProcessor] ->  [SpeechRecognitionImpl] to pause the native speech recognition
                            // [AudioProcessor] ->  [AudioLoop]             to not record audio anymore
                            // [AudioProcessor] ->  [SpeakerImpl]           to tell the customer the info

    SPEECH_ENDED            // [SpeakerImpl]    ->  [AudioLoop]             to record audio again
}
