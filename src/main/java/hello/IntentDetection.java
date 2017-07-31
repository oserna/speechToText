package hello;

import com.aldebaran.qi.CallError;
import com.aldebaran.qi.helper.proxies.ALSpeechRecognition;
import com.aldebaran.qi.helper.proxies.ALTextToSpeech;
import com.google.api.gax.grpc.ApiException;
import com.google.cloud.conversation.v1alpha.AudioEncoding;
import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import hello.dto.DetectIntentResponse;
import hello.dto.Fulfillment;
import hello.dto.QueryResult;
import io.grpc.Status;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import javax.sound.sampled.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.*;

//import com.google.cloud.conversation.spi.v1alpha.ConversationServiceClient;
//import com.google.cloud.conversation.v1alpha.*;

public class IntentDetection implements Runnable {

    private final float MAX_8_BITS_SIGNED = Byte.MAX_VALUE;
    private final float MAX_8_BITS_UNSIGNED = 0xff;
    private final float MAX_16_BITS_SIGNED = Short.MAX_VALUE;
    private final float MAX_16_BITS_UNSIGNED = 0xffff;

    private final int sampleRate = 16000;
    private final int sampleSizeInBits = 16;
    private final int channels = 1;
    private final int silenceByteLengthToCheck = (int) (sampleRate * sampleSizeInBits / 8); // 1 Second
    private final int bufferByteSize = (int) (sampleRate * sampleSizeInBits / 8) * 10; // 10 Seconds
    private final boolean signed = true;
    private final boolean bigEndian = false;
    private final double silenceThreshold = 0.25;

    private final AudioFormat audioFormat = new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    private final DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
    private TargetDataLine targetDataLine = null;


    private String sessionId = UUID.randomUUID().toString();

    private boolean isCapturing = false;

    /* Public stuff */

    private boolean stopped = false;

    private boolean isTriggered = false;

    private ALSpeechRecognition sr;
    private ALTextToSpeech tts;

    private String token = "ya29.El-VBEor1WbBeAQQxDR73ZzDJ_9BDENkg108hp4zGlcoV3cxS4VEgq_OYll4RV1_8EteXN_eTc4Q1gAYtmEU3V0R_SwZHQbRePtuUv42vDKL13kf_C5Hvr0IwHTY8b2opA";

    public IntentDetection(ALTextToSpeech tts, ALSpeechRecognition sr, String sessionId) {
        this.tts = tts;
        this.sr = sr;
        this.sessionId = sessionId;

        init();
    }

    public void init() {

        try {
            targetDataLine = (TargetDataLine) AudioSystem.getLine(info);
            targetDataLine.open(audioFormat);
            targetDataLine.start();

//            client = ConversationServiceClient.create();
        } catch (LineUnavailableException e) {
            System.out.println("unable to get a recording line");
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void run() {

        try {

            System.out.println("Line Buffer: " + targetDataLine.getBufferSize());

            int numBytesRead;
            int offset = 0;
            int chunkSize = targetDataLine.getBufferSize() / 2;
            byte[] data = new byte[bufferByteSize];

            System.out.println(data.length);

            int counter = 0;

            while (!stopped) {

                counter++;

                if (counter % 10 == 0) {
                    System.out.println("Offset: " + offset);
                }

                // Read the next chunk of data from the TargetDataLine.
                numBytesRead = targetDataLine.read(data, offset, chunkSize);

                offset += numBytesRead;

                System.out.println("Offset: " + offset);

                Thread.yield();

                if (isCapturing) {

                    //Check for silence
                    int start = offset - silenceByteLengthToCheck;
                    if (start < 0) {
                        continue;
                    }

                    if (isSilence(Arrays.copyOfRange(data, start, offset))) {
                        System.out.println("Silence detected");

                        if (isSilence(Arrays.copyOfRange(data, 0, offset))) {
                            offset = 0;
                            continue;
                        }


                        //Process Intent
                        System.out.println("Processing intent................................................");
                        QueryResult queryResult = null;
                        //String getSpeech = "";


                        queryResult = detectAudioIntent(ByteString.copyFrom(data, 0, offset));
                        if (queryResult == null) {
                            speakAsynch(new ArrayList<String>(){{
                                add("No name");
                                add("Sorry, I didn't get that. Please try again.");
                            }});

                            offset = 0;
                            continue;
                        }

                        final List<String> result = processIntent(queryResult);

                        speakAsynch(result);

                        printResult(queryResult);


                        System.out.println("Processed intent................................................");

                        offset = 0;
                        isTriggered = false;
                        isCapturing = false;

                        //TODO
                        continue;
                    } else {
                        continue;
                    }
                }

                // Check for trigger
                if (isTriggered) {
                    System.out.println("Triggered");
                    isCapturing = true;
                } else {
                    isCapturing = false;
                    offset = 0;
                }

            }
        } finally {
            try {
                cleanup();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void speakAsynch(List<String> result) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tts.say(result.get(1));
                    if (result.get(0).equals("quit")
                            || result.get(0).equals("final.followup.no")) {
                        sr.pause(false);
                    }
                } catch (CallError callError) {
                    callError.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private List<String> processIntent(QueryResult queryResult) {

        if (queryResult.getIntent() != null) {
            switch (queryResult.getIntent().getDisplayName()) {
                case "account.balance":
                    String text = "Here is the list of your accounts. " +
                            "Your Orange Everyday account has $3,234.55. " +
                            "Your Holiday account has $1,134.40. " +
                            "Your Joint account has $933.22. " +
                            queryResult.getFulfillment().getText();
                    return new ArrayList<String>() {{
                        add(queryResult.getIntent().getDisplayName());
                        add(text);
                    }};

                case "account.balance.last.5.yes":
                    String text2 = "Here are the last 5 transactions we have for you. " +
                            "3 weeks ago, you spent $234.34 at Coles. " +
                            "2 weeks ago, you spent $67.76 at Woolworths. " +
                            "3 days ago, you spent $7.80 at Lion Cafe. " +
                            "Yesterday, you spent $12.00 at Seafood Pty Ltd. " +
                            "Moments ago, you spent $60.00 at Opal Travel. " +
                            queryResult.getFulfillment().getText();
                    return new ArrayList<String>() {{
                        add(queryResult.getIntent().getDisplayName());
                        add(text2);
                    }};

                case "quit":
                    setStopped(true);
                    return new ArrayList<String>() {{
                        add(queryResult.getIntent().getDisplayName());
                        add(queryResult.getFulfillment().getText());
                    }};

                case "final.followup.yes":
                case "final.followup.no":
                case "account.balance.last.5.no":

                default:
                    return new ArrayList<String>() {{
                        add(queryResult.getIntent().getDisplayName());
                        add(queryResult.getFulfillment().getText());
                    }};
            }
        }

        return new ArrayList<String>() {{
            add("Null for you");
            add(queryResult.getFulfillment().getText());
        }};
    }


    public void cleanup() throws Exception {
        stopped = false;
        targetDataLine.stop();
        targetDataLine.close();

//        if (client != null) {
//            client.close();
//        }
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

        System.out.println("Level: " + level);
        return level <= silenceThreshold;
    }

    private QueryResult detectAudioIntent(ByteString audioData) {

        String url = "https://conversation.googleapis.com/v1alpha/projects/ing-cce/agents/ing-cce/sessions/123456789:detectIntent";

        HttpPost post = new HttpPost(url);

        post.setHeader("Authorization", "Bearer " + token);
        post.setHeader("Content-Type", "application/json; charset=utf-8");

        try {
            StringEntity entity = new StringEntity("{" +
                    "\"query_input\":{" +
                    "\"audioConfig\": {" +
                    "\"audioEncoding\": \"AUDIO_ENCODING_LINEAR16\"," +
                    "\"sampleRateHertz\": 16000," +
                    "\"languageCode\": \"en-US\"" +
                    "}" +
                    "}," +
                    "\"inputAudio\": \"" + new String(Base64.getEncoder().encode(audioData.toByteArray())) + "\"" +
                    "}");
            post.setEntity(entity);
            //post.setHeader("Content-Length", String.valueOf(entity.getContentLength()));

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        HttpClient httpClient = new DefaultHttpClient();
        QueryResult result = null;

        try {
            HttpResponse httpResponse = httpClient.execute(post);

            BufferedReader br = new BufferedReader(new InputStreamReader((httpResponse.getEntity().getContent())));

            String output;
            StringBuilder stringBuilder = new StringBuilder();

            if (stringBuilder.toString().indexOf("NOT_FOUND") > 0) {
                return null;
            }
            while ((output = br.readLine()) != null) {
                stringBuilder.append(output);
            }

//            FieldNamingStrategy customPolicy = new FieldNamingStrategy() {
//                @Override
//                public String translateName(Field f) {
//                    return f.getName().substring(0, f.getName().length() - 1);
//                }
//            };

            Gson gson = new Gson(); //GsonBuilder().setFieldNamingStrategy(customPolicy).create();
            System.out.println(stringBuilder.toString());
            result = gson.fromJson(stringBuilder.toString(), DetectIntentResponse.class).getQueryResult();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;

//        System.out.println(new String(Base64.getEncoder().encode(audioData.toByteArray())));

//        return client
//                .detectIntent(
//                        DetectIntentRequest.newBuilder()
//                                .setSession(
//                                        ConversationServiceClient.formatSessionName(projectId, projectId, sessionId))
//                                .setQueryInput(
//                                        QueryInput.newBuilder()
//                                                .setAudioConfig(
//                                                        InputAudioConfig.newBuilder()
//                                                                .setAudioEncoding(audioEncoding)
//                                                                .setLanguageCode(languageCode)
//                                                                .setSampleRateHertz(sampleRate)))
//                                .setInputAudio(audioData)
//                                .build())
//                .getQueryResult();
    }

    private void printResult(QueryResult result) {
        System.out.println("Intent detection result:");
        System.out.format("  Query: '%s'", result.getQueryText());
        System.out.format("  Response: '%s'", result.getFulfillment().getText());
//        System.out.format("  Intent: %s\n",
//                result.hasIntent() ? "detected '" + result.getIntent().getDisplayName() + "'"
//                        : "not detected");
        System.out.format("  Parameters: '%s'\n", result.getParameters().toString().replace("\n", ""));
//        if (result.getOutputContextsCount() > 0) {
//            System.out.println("  Output contexts: ");
//            for (Context context : result.getOutputContextsList()) {
//                System.out.format("    %s\n", client.parseContextFromContextName(context.getName()));
//                System.out.format("      Lifespan: %s\n", context.getLifespanCount());
//                System.out.format(
//                        "      Parameters: '%s'\n", context.getParameters().toString().replace("\n", ""));
//            }
//        }
    }

    public synchronized void setStopped(boolean stopped) {
        this.stopped = stopped;
    }

    public synchronized void setIsTriggered(boolean isTriggered) {
        this.isTriggered = isTriggered;
    }

    public synchronized boolean getIsTriggered() {
        return this.isTriggered;
    }

}
