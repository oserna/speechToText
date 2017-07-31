package hello.old;


import javax.sound.sampled.*;
import java.io.IOException;
import java.util.Arrays;

/*
 * SimpleAudioRecorder2.java
 *
 * This file is part of jsresources.org
 */
/*
 * Copyright (c) 1999 - 2003 by Matthias Pfisterer
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/*
 |<---            this code is formatted to fit into 80 columns             --->|
 */

/**
 * <p>
 * SimpleAudioRecorder2: Recording to an audio file (simple version)
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
 * <li>java SimpleAudioRecorder2 choice="plain" -h
 * <li>java SimpleAudioRecorder2 choice="plain" audiofile
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
 * Source code: <a href="SimpleAudioRecorder2.java.html">SimpleAudioRecorder2.java</a>
 * </p>
 */
public class SimpleAudioRecorder2 extends Thread {

    private TargetDataLine m_line;
    private AudioFileFormat.Type m_targetType;
    private static boolean stopped = false;
    private boolean isCapturing = false;
    private static boolean isTriggered = false;

    public SimpleAudioRecorder2(TargetDataLine line, AudioFileFormat.Type targetType) {
        m_line = line;
        m_targetType = targetType;
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
        System.out.println("Line Buffer: " + m_line.getBufferSize());

        int numBytesRead;
        int currentPosition = 0;
        int offset = 0;
        int chunkSize = m_line.getBufferSize() / 5; //Make this 0.5 seconds worth
        byte[] data = new byte[m_line.getBufferSize() * 10];

        System.out.println(data.length);

        while (!stopped) {

            // Read the next chunk of data from the TargetDataLine.
            numBytesRead = m_line.read(data, offset, chunkSize);

            offset += numBytesRead;

//            System.out.println("Offset: " + offset);
//            System.out.println("Bytes read: " + numBytesRead);

            if (isCapturing) {

                //Check for silence
                int start = offset - 32000;
                if (start < 0) {
                    start = 0;
                }

                if (isSilence(Arrays.copyOfRange(data, start, offset))) {
                    System.out.println("Silence detected");

                    //Reset offset and isCapturing
                    offset = 0;
                    isCapturing = false;

                    //Process Intent
                    System.out.println("Processing intent................................................");

                    continue;
                } else {
                    continue;
                }
            }

            // Check for trigger
            if (isTrigger(data)) {
                System.out.println("Triggered");
                isCapturing = true;
            } else {
                isCapturing = false;
                offset = 0;
            }

        }
//            AudioSystem.write(m_audioInputStream, m_targetType, m_outputFile);
    }

    final static float MAX_8_BITS_SIGNED = Byte.MAX_VALUE;
    final static float MAX_8_BITS_UNSIGNED = 0xff;
    final static float MAX_16_BITS_SIGNED = Short.MAX_VALUE;
    final static float MAX_16_BITS_UNSIGNED = 0xffff;

    private boolean isSilence(byte[] buffer) {
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);

        int max = 0;
        boolean use16Bit = (format.getSampleSizeInBits() == 16);
        boolean signed = (format.getEncoding() ==
                AudioFormat.Encoding.PCM_SIGNED);
        boolean bigEndian = (format.isBigEndian());

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

        System.out.println("Level: " + level);
        return level <= 0.25;
    }

    private boolean isTrigger(byte[] data) {
        return isTriggered;
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
       * Now, we are creating an SimpleAudioRecorder2 object. It contains the logic of starting and stopping the recording,
       * reading audio data from the TargetDataLine and writing the data to a file.
       */
        SimpleAudioRecorder2 recorder = new SimpleAudioRecorder2(targetDataLine, targetType);

        /*
       * Here, the recording is actually started.
       */
        recorder.start();
        out("Recording...");

        int readByte = 10;
      /*
       * We are waiting for the user to press ENTER to start the recording. (You might find it inconvenient if recording starts
       * immediately.)
       */
        out("Press ENTER to toggle. Press Q + ENTER to exit.");
        try {
            while (readByte == 10) {
                readByte = System.in.read();
                if (!isTriggered) {
                } else {
                    recorder.stopRecording();
                }
                isTriggered = !isTriggered;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

      /*
       * Here, the recording is actually stopped.
       */
        recorder.stopRecording();
        out("Recording stopped.");

        stopped = true;
    }

    private static void printUsageAndExit() {
        out("SimpleAudioRecorder2: usage:");
        out("\tjava SimpleAudioRecorder2 -h");
        out("\tjava SimpleAudioRecorder2 <audiofile>");
        System.exit(0);
    }

    private static void out(String strMessage) {
        System.out.println(strMessage);
    }
}

/*** SimpleAudioRecorder2.java ***/