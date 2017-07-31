package hello.async;

import hello.async.bus.Event;
import hello.async.bus.EventBuilder;
import hello.async.bus.EventBus;
import hello.async.bus.EventHandler;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class AudioEventLoop {

    private String sessionId = UUID.randomUUID().toString();

    private EventBus<Event> eventBus;

    private ExecutorService service = Executors.newSingleThreadExecutor();

    private boolean capturing = true;
    private boolean recording = false;

    private List<EventHandler> eventHandlers = new ArrayList<>();

    public AudioEventLoop(String sessionId, EventBus<Event> eventBus) {
        this.sessionId = sessionId;
        this.eventBus = eventBus;

        this.eventHandlers.add(new IntentRecognizedHandler());
    }

    public void start() {
        service.submit(new AudioCapturer());
    }

    private class AudioCapturer implements Runnable {

        private final float MAX_8_BITS_SIGNED = Byte.MAX_VALUE;
        private final float MAX_8_BITS_UNSIGNED = 0xff;
        private final float MAX_16_BITS_SIGNED = Short.MAX_VALUE;
        private final float MAX_16_BITS_UNSIGNED = 0xffff;

        private final int sampleRate = 16000;
        private final int sampleSizeInBits = 16;
        private final int channels = 1;
        private final boolean signed = true;
        private final boolean bigEndian = false;

        private final int silenceByteLengthToCheck = (int) (sampleRate * sampleSizeInBits / 8); // 1 Second

        private final int bufferByteSize = (int) (sampleRate * sampleSizeInBits / 8) * 10; // 10 Seconds

        private final AudioFormat audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);

        private final double silenceThreshold = 0.25;

        private final AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;
        private final DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        private TargetDataLine targetDataLine = null;

        public void init() {

            try {
                targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
                targetDataLine.open(audioFormat);
                targetDataLine.start();
                System.out.println(" DataLine initialized ok");

            } catch (LineUnavailableException e) {
                System.out.println("unable to get a recording line");
                e.printStackTrace();
                System.exit(1);
            }
        }

        public void cleanup() {
            targetDataLine.stop();
            targetDataLine.close();
        }

        @Override
        public void run() {

            init();

            int offset = 0;
            byte[] data = new byte[bufferByteSize];

            while (capturing) {

                //available bytes to read
                int availableBytes = targetDataLine.available();

                //TODO buffer overrun protection
                // if (offset + availableBytes) > data.length {...}

                // Read the next chunk of data from the TargetDataLine.
                int numBytesRead = targetDataLine.read(data, offset, availableBytes);

                if (recording) {

                    offset += numBytesRead;

                    //recorded so far
                    byte[] recorded = Arrays.copyOf(data, offset);

                    //Check if there is trailing silence but not all recorded audio is silence
                    if (trailingSilenceDetected(recorded) && !isSilence(recorded)) {

                        eventBus.publish(
                                EventBuilder.create(EventType.VOICE_CHUNK_RECORDED)
                                        .set("AUDIO", recorded)
                                        .build());

                        //switch off recording until the process of the recorded audio finish
                        recording = false;
                        offset = 0;

                    }

                }

                Thread.yield();
            }

            cleanup();
        }

        private boolean trailingSilenceDetected(byte[] recorded) {

            //check for the minimum amount of sound to detect silence
            if (recorded.length < silenceByteLengthToCheck) {
                return false;
            }

            //check for trailing silence at the end of the recorded audio
            if (isSilence(Arrays.copyOfRange(recorded, recorded.length - silenceByteLengthToCheck, recorded.length))) {
                return true;
            }

            return false;
        }

        private boolean isSilence(byte[] buffer) {

            int max = 0;
            boolean use16Bit = (audioFormat.getSampleSizeInBits() == 16);
            boolean signed = (audioFormat.getEncoding() == AudioFormat.Encoding.PCM_SIGNED);
            boolean bigEndian = (audioFormat.isBigEndian());

            if (use16Bit) {
                for (int i = 0; i < buffer.length; i += 2) {
                    int value = 0;
                    // deal with endianness
                    int hiByte = (bigEndian ? buffer[i] : buffer[i + 1]);
                    int loByte = (bigEndian ? buffer[i + 1] : buffer[i]);
                    if (signed) {
                        short shortVal = (short) hiByte;
                        shortVal = (short) ((shortVal << 8) | (byte) loByte);
                        value = shortVal;
                    } else {
                        value = (hiByte << 8) | loByte;
                    }
                    max = Math.max(max, value);
                } // for
            } else {
                // 8 bit - no endianness issues, just sign
                for (int i = 0; i < buffer.length; i++) {
                    int value = 0;
                    if (signed) {
                        value = buffer[i];
                    } else {
                        short shortVal = 0;
                        shortVal = (short) (shortVal | buffer[i]);
                        value = shortVal;
                    }
                    max = Math.max(max, value);
                } // for
            } // 8 bit
            // express max as float of 0.0 to 1.0 of max value
            // of 8 or 16 bits (signed or unsigned)
            float level;

            if (signed) {
                if (use16Bit) {
                    level = (float) max / MAX_16_BITS_SIGNED;
                } else {
                    level = (float) max / MAX_8_BITS_SIGNED;
                }
            } else {
                if (use16Bit) {
                    level = (float) max / MAX_16_BITS_UNSIGNED;
                } else {
                    level = (float) max / MAX_8_BITS_UNSIGNED;
                }
            }

//            System.out.println("Level: " + level);
            return level <= silenceThreshold;
        }

    }

    public class IntentRecognizedHandler implements EventHandler<Event> {

        public EventType getType() {
            return EventType.SPEECH_ENDED;
        }

        public boolean canHandle(EventType eventType) {
            return true;
        }

        public void handle(Event event) {

            System.out.println("AudioEventLoop got an event " + event.getType());

            String textSpeeched = event.get("TEXT", String.class);

            System.out.println("Text speeched: " + textSpeeched);

            setRecording(true);

        }
    }

    public synchronized boolean isCapturing() {
        return capturing;
    }

    public synchronized void setCapturing(boolean capturing) {
        this.capturing = capturing;
    }

    public synchronized boolean isRecording() {
        return recording;
    }

    public synchronized void setRecording(boolean recording) {
        System.out.println("AudioLoop recording: "+recording+" from now on");
        this.recording = recording;
    }

    public List<EventHandler> getEventHandlers() {
        return eventHandlers;
    }
}
