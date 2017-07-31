package hello.old;

import com.google.api.gax.grpc.ApiStreamObserver;
import com.google.cloud.conversation.spi.v1alpha.ConversationServiceClient;
import com.google.cloud.conversation.v1alpha.*;
import com.google.protobuf.ByteString;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * <p>
 * SimpleAudioRecorder: Recording to an audio file (simple version)
 * </p>
 * <p>
 * <p>
 * Purpose: Records audio data and stores it in a file. The data is recorded in CD quality (44.1 kHz, 16 bit linear, stereo) and
 * stored in a .wav file.
 * </p>
 * <p>
 * <p>
 * Usage:
 * <ul>
 * <li>java SimpleAudioRecorder choice="plain" -h
 * <li>java SimpleAudioRecorder choice="plain" audiofile
 * </ul>
 * <p>
 * <p>
 * Parameters
 * <ul>
 * <li>-h: print usage information, then exit
 * <li>audiofile: the file name of the audio file that should be produced from the recorded data
 * </ul>
 * <p>
 * <p>
 * Bugs, limitations: You cannot select audio formats and the audio file type on the command line. See AudioRecorder for a version
 * that has more advanced options. Due to a bug in the Sun jdk1.3/1.4, this program does not work with it.
 * </p>
 * <p>
 * <p>
 * Source code: <a href="SimpleAudioRecorder.java.html">SimpleAudioRecorder.java</a>
 * </p>
 */
public class SimpleAudioRecorder extends Thread {

    private List<String> command = new ArrayList<String>();

    private TargetDataLine m_line;
    private AudioFileFormat.Type m_targetType;
    private AudioInputStream m_audioInputStream;
    private OutputStream m_outputStream;
    private static float level;

    public SimpleAudioRecorder(TargetDataLine line, AudioFileFormat.Type targetType, OutputStream outputStream) {
        m_line = line;
        m_audioInputStream = new AudioInputStream(line);
        m_targetType = targetType;
        m_outputStream = outputStream;
    }

    /**
     * Starts the recording. To accomplish this, (i) the line is started and (ii) the thread is started.
     */
    public void start() {
      /*
       * Starting the TargetDataLine. It tells the line that we now want to read data from it. If this method isn't called, we
       * won't be able to read data from the line at all.
       */
        m_line.start();

      /*
       * Starting the thread. This call results in the method 'run()' (see below) being called. There, the data is actually read
       * from the line.
       */
        super.start();
    }

    /**
     * Stops the recording.
     * <p>
     * Note that stopping the thread explicitely is not necessary. Once no more data can be read from the TargetDataLine, no more
     * data be read from our AudioInputStream. And if there is no more data from the AudioInputStream, the method
     * 'AudioSystem.write()' (called in 'run()' returns. Returning from 'AudioSystem.write()' is followed by returning from
     * 'run()', and thus, the thread is terminated automatically.
     * <p>
     * It's not a good idea to call this method just 'stop()' because stop() is a (deprecated) method of the class 'Thread'. And
     * we don't want to override this method.
     */
    public void stopRecording() {
        m_line.stop();
        m_line.close();
    }

    /**
     * Main working method. You may be surprised that here, just 'AudioSystem.write()' is called. But internally, it works like
     * this: AudioSystem.write() contains a loop that is trying to read from the passed AudioInputStream. Since we have a special
     * AudioInputStream that gets its data from a TargetDataLine, reading from the AudioInputStream leads to reading from the
     * TargetDataLine. The data read this way is then written to the passed File. Before writing of audio data starts, a header is
     * written according to the desired audio file type. Reading continues untill no more data can be read from the
     * AudioInputStream. In our case, this happens if no more data can be read from the TargetDataLine. This, in turn, happens if
     * the TargetDataLine is stopped or closed (which implies stopping). (Also see the comment above.) Then, the file is closed
     * and 'AudioSystem.write()' returns.
     */
    public void run() {
        try {
            AudioSystem.write(m_audioInputStream, m_targetType, m_outputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

      /*
       * For simplicity, the audio data format used for recording is hardcoded here. We use PCM 44.1 kHz, 16 bit signed, stereo.
       */
//        AudioFormat audioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100.0F, 16, 2, 4, 44100.0F, false);
        int sampleRate = 16000;
        AudioFormat audioFormat = new AudioFormat(sampleRate, 16, 1, true, false);

      /*
       * Now, we are trying to get a TargetDataLine. The TargetDataLine is used later to read audio data from it. If requesting
       * the line was successful, we are opening it (important!).
       */
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
        TargetDataLine targetDataLine = null;
        try {
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
        } catch (LineUnavailableException e) {
            out("unable to get a recording line");
            e.printStackTrace();
            System.exit(1);
        }

      /*
       * Again for simplicity, we've hardcoded the audio file type, too.
       */
        AudioFileFormat.Type targetType = AudioFileFormat.Type.WAVE;

      /*
       * Now, we are creating an SimpleAudioRecorder object. It contains the logic of starting and stopping the recording,
       * reading audio data from the TargetDataLine and writing the data to a file.
       */

        final AudioInputStream audioInputStream = new AudioInputStream(targetDataLine);
        targetDataLine.start();

        final CountDownLatch notification = new CountDownLatch(1);

        new Thread(new Runnable() {
            public void run() {
                try {
                    QueryResult queryResult = detectAudioStream(audioInputStream, notification);
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }
            }
        }).start();

        level = 1;
        System.out.println(level);

        while (level > 0.1) {
            System.out.println(level);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        targetDataLine.stop();

        try {
            notification.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }

    private static QueryResult detectAudioStream(InputStream audioStream, final CountDownLatch notification) throws Throwable {

        ConversationServiceClient client = ConversationServiceClient.create();

        String projectId = "ing-cce";
        String sessionId = UUID.randomUUID().toString();
        String languageCode = "en-US";
        AudioEncoding audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR16;
        int sampleRateHertz = 16000;

        // Start bi-directional StreamingDetectIntent stream. As of 2017-06-26 gRPC only has an
        // asynchronous API in Java, so this is somewhat complicated.
        final List<Throwable> response_throwables = new ArrayList<Throwable>();
        final List<StreamingDetectIntentResponse> responses = new ArrayList<StreamingDetectIntentResponse>();

        ApiStreamObserver<StreamingDetectIntentRequest> requestObserver =
                client
                        .streamingDetectIntentCallable()
                        .bidiStreamingCall(
                                new ApiStreamObserver<StreamingDetectIntentResponse>() {
                                    //                                    @Override
                                    public void onNext(StreamingDetectIntentResponse response) {
                                        responses.add(response);
                                    }

                                    //                                    @Override
                                    public void onError(Throwable throwable) {
                                        response_throwables.add(throwable);
                                        notification.countDown();
                                    }

                                    //                                    @Override
                                    public void onCompleted() {
                                        notification.countDown();
                                    }
                                });

        // Send messages.
        try {
            // 1st message: config.
            requestObserver.onNext(
                    StreamingDetectIntentRequest.newBuilder()
                            .setQueryParams(
                                    StreamingQueryParameters.newBuilder()
                                            .setSession(
                                                    ConversationServiceClient.formatSessionName(
                                                            projectId, projectId, sessionId)))
                            .setQueryInput(
                                    StreamingQueryInput.newBuilder()
                                            .setAudioConfig(
                                                    StreamingInputAudioConfig.newBuilder()
                                                            .setConfig(
                                                                    InputAudioConfig.newBuilder()
                                                                            .setAudioEncoding(audioEncoding)
                                                                            .setLanguageCode(languageCode)
                                                                            .setSampleRateHertz(sampleRateHertz))))
                            .build());

            // Following messages: audio chunks. We just read the file in fixed-size chunks. In reality
            // you would split the user input by time.
            byte[] buffer = new byte[4096];
            int bytes = 0;
            while ((bytes = audioStream.read(buffer)) != 0) {

                calculateLevel(buffer, 0, buffer.length - bytes);

                requestObserver.onNext(
                        StreamingDetectIntentRequest.newBuilder()
                                .setInputAudio(ByteString.copyFrom(buffer, 0, bytes))
                                .build());
            }

        } catch (RuntimeException e) {
            // Cancel stream.
            requestObserver.onError(e);
        }

        // Half-close the stream.
        requestObserver.onCompleted();
        // Wait for the final response (without explicit timeout).
        notification.await();
        // Process errors/responses.
        if (!response_throwables.isEmpty()) {
            throw response_throwables.get(0);
        }
        if (responses.isEmpty()) {
            throw new RuntimeException("No response from CCE.");
        }
        for (StreamingDetectIntentResponse response : responses) {
            if (response.hasRecognitionResult()) {
                System.out.println(response.getRecognitionResult().getTranscript());
            }
        }

        System.out.println(responses.get(responses.size() - 1).getQueryResult().getIntent().getDisplayName());
        System.out.println(responses.get(responses.size() - 1).getQueryResult().getFulfillment().getText());

        return responses.get(responses.size() - 1).getQueryResult();
    }

    final static float MAX_8_BITS_SIGNED = Byte.MAX_VALUE;
    final static float MAX_8_BITS_UNSIGNED = 0xff;
    final static float MAX_16_BITS_SIGNED = Short.MAX_VALUE;
    final static float MAX_16_BITS_UNSIGNED = 0xffff;

    private static void calculateLevel(byte[] buffer,
                                       int readPoint,
                                       int leftOver) {

        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);

        int max = 0;
        boolean use16Bit = (format.getSampleSizeInBits() == 16);
        boolean signed = (format.getEncoding() ==
                AudioFormat.Encoding.PCM_SIGNED);
        boolean bigEndian = (format.isBigEndian());
        if (use16Bit) {
            for (int i = readPoint; i < buffer.length - leftOver; i += 2) {
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
            for (int i = readPoint; i < buffer.length - leftOver; i++) {
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

        System.out.println("Level: " + level);

    } // calculateLevel
}