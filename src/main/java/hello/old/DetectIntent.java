package hello.old;
/*
  Copyright 2017, Google Inc.
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
      http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

import com.google.api.gax.grpc.ApiStreamObserver;
import com.google.cloud.conversation.spi.v1alpha.ConversationServiceClient;
import com.google.cloud.conversation.v1alpha.AudioEncoding;
import com.google.cloud.conversation.v1alpha.Context;
import com.google.cloud.conversation.v1alpha.DetectIntentRequest;
import com.google.cloud.conversation.v1alpha.EventInput;
import com.google.cloud.conversation.v1alpha.InputAudioConfig;
import com.google.cloud.conversation.v1alpha.QueryInput;
import com.google.cloud.conversation.v1alpha.QueryResult;
import com.google.cloud.conversation.v1alpha.StreamingDetectIntentRequest;
import com.google.cloud.conversation.v1alpha.StreamingDetectIntentResponse;
import com.google.cloud.conversation.v1alpha.StreamingInputAudioConfig;
import com.google.cloud.conversation.v1alpha.StreamingQueryInput;
import com.google.cloud.conversation.v1alpha.StreamingQueryParameters;
import com.google.cloud.conversation.v1alpha.TextInput;
import com.google.protobuf.ByteString;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;

/**
 * Examples for the DetectIntent endpoints of https://cloud.google.com/conversation/docs.
 */
public class DetectIntent implements AutoCloseable {
    public static void main(String... args) throws Exception {
        try (DetectIntent detectIntent = new DetectIntent(args)) {
            detectIntent.run();
        }
    }

    String projectId = "ing-cce";
    String sessionId = UUID.randomUUID().toString();
    String languageCode = "en-US";
    AudioEncoding audioEncoding = AudioEncoding.AUDIO_ENCODING_LINEAR16;
    int sampleRateHertz = 22050;


    /**
     * Parses the given commandline options and creates a Conversation Service client.
     */
    public DetectIntent(String... args) throws Exception {
        try {
            client = ConversationServiceClient.create();
        } catch (IOException e) {
            System.err.format("Could not connect to Conversation API: '%s'.\n", e.getMessage());
            throw e;
        }
    }

    /**
     * Runs the command specified at construction.
     */
    public void run() {
        try {
            QueryResult result= executeCommand("stream", "c:\\dev\\ING\\cce-samples-java\\samples\\resources\\pizza_order.wav");
            printResult(result);

        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private QueryResult executeCommand(String command, String input) throws Throwable {
        if (command.equals("text")) {
            return detectTextIntent(input);
        } else if (command.equals("event")) {
            return detectEventIntent(input);
        } else if (command.equals("audio")) {
            try (FileInputStream stream = new FileInputStream(input)) {
                return detectAudioIntent(ByteString.readFrom(stream));
            }
        } else if (command.equals("stream")) {
            try (FileInputStream stream = new FileInputStream(input)) {
                return detectAudioStream(stream);
            }
        } else {
            throw new IllegalArgumentException(String.format("Unknown command '%s'.", command));
        }
    }

    private void printResult(QueryResult result) {
        System.out.println("Intent detection result:");
        System.out.format("  Query: '%s'\n", result.getQueryText());
        System.out.format("  Response: '%s'\n", result.getFulfillment().getText());
        System.out.format("  Intent: %s\n",
                result.hasIntent() ? "detected '" + result.getIntent().getDisplayName() + "'"
                        : "not detected");
        System.out.format("  Parameters: '%s'\n", result.getParameters().toString().replace("\n", ""));
        if (result.getOutputContextsCount() > 0) {
            System.out.println("  Output contexts: ");
            for (Context context : result.getOutputContextsList()) {
                System.out.format("    %s\n", client.parseContextFromContextName(context.getName()));
                System.out.format("      Lifespan: %s\n", context.getLifespanCount());
                System.out.format(
                        "      Parameters: '%s'\n", context.getParameters().toString().replace("\n", ""));
            }
        }
    }

    // Different types of DetectIntent().
    private QueryResult detectTextIntent(String text) {
        return client
                .detectIntent(
                        ConversationServiceClient.formatSessionName(projectId, projectId, sessionId),
                        QueryInput.newBuilder()
                                .setText(TextInput.newBuilder().setText(text).setLanguageCode(languageCode))
                                .build())
                .getQueryResult();
    }

    private QueryResult detectEventIntent(String eventName) {
        return client
                .detectIntent(
                        ConversationServiceClient.formatSessionName(projectId, projectId, sessionId),
                        QueryInput.newBuilder().setEvent(EventInput.newBuilder().setName(eventName)).build())
                .getQueryResult();
    }

    private QueryResult detectAudioIntent(ByteString audioData) {
        return client
                .detectIntent(
                        DetectIntentRequest.newBuilder()
                                .setSession(
                                        ConversationServiceClient.formatSessionName(projectId, projectId, sessionId))
                                .setQueryInput(
                                        QueryInput.newBuilder()
                                                .setAudioConfig(
                                                        InputAudioConfig.newBuilder()
                                                                .setAudioEncoding(audioEncoding)
                                                                .setLanguageCode(languageCode)
                                                                .setSampleRateHertz(sampleRateHertz)))
                                .setInputAudio(audioData)
                                .build())
                .getQueryResult();
    }

    private QueryResult detectAudioStream(InputStream audioStream) throws Throwable {
        // Start bi-directional StreamingDetectIntent stream. As of 2017-06-26 gRPC only has an
        // asynchronous API in Java, so this is somewhat complicated.
        final CountDownLatch notification = new CountDownLatch(1);
        final List<Throwable> response_throwables = new ArrayList<Throwable>();
        final List<StreamingDetectIntentResponse> responses =
                new ArrayList<StreamingDetectIntentResponse>();
        ApiStreamObserver<StreamingDetectIntentRequest> requestObserver =
                client
                        .streamingDetectIntentCallable()
                        .bidiStreamingCall(
                                new ApiStreamObserver<StreamingDetectIntentResponse>() {
                                    @Override
                                    public void onNext(StreamingDetectIntentResponse response) {
                                        responses.add(response);
                                    }

                                    @Override
                                    public void onError(Throwable throwable) {
                                        response_throwables.add(throwable);
                                        notification.countDown();
                                    }

                                    @Override
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
            while ((bytes = audioStream.read(buffer)) != -1) {
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
                System.out.format(
                        "Intermediate transcript: '%s'\n", response.getRecognitionResult().getTranscript());
            }
        }
        return responses.get(responses.size() - 1).getQueryResult();
    }

    // Conversation API client.
    ConversationServiceClient client;

    @Override
    public void close() throws Exception {
        if (client != null) {
            client.close();
        }
    }
}