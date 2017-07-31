package hello.async;

import com.google.gson.Gson;
import com.google.protobuf.ByteString;
import hello.dto.DetectIntentResponse;
import hello.dto.QueryResult;
import hello.async.bus.Event;
import hello.async.bus.EventBuilder;
import hello.async.bus.EventBus;
import hello.async.bus.EventHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class AudioProcessor {

    private final EventBus<Event> eventBus;

    private List<EventHandler> eventHandlers = new ArrayList<>();

    private String token = "ya29.El-VBEor1WbBeAQQxDR73ZzDJ_9BDENkg108hp4zGlcoV3cxS4VEgq_OYll4RV1_8EteXN_eTc4Q1gAYtmEU3V0R_SwZHQbRePtuUv42vDKL13kf_C5Hvr0IwHTY8b2opA";

    public AudioProcessor(String token, EventBus<Event> eventBus) {
        this.token = token;
        this.eventBus = eventBus;

        this.eventHandlers.add(new VoiceCapturedHandler());
    }

    public void intentRecognition(byte [] recorded){

        //Process Intent
        System.out.println("Processing intent................................................");

        //Audio to Text -> text -> text to Intent
        QueryResult queryResult = detectAudioIntent(ByteString.copyFrom(recorded));

        printResult(queryResult);

        //There is no intent, probably silence or not a clear match to an intent
        if (queryResult == null) {
            eventBus.publish(
                    EventBuilder.create(EventType.VOICE_CHUNK_ANALYZED)
                            .set("TEXT", "Sorry, I didn't get that. Please try again.")
                            .build());

            return;
        }

        //Intent recognized
        String [] result = processIntent(queryResult);

        //If the customer is trying to end the service
        if (result[0].equals("quit")
                || result[0].equals("final.followup.no")) {
            eventBus.publish(
                    EventBuilder.create(EventType.CONVERSATION_ENDED)
                            .set("TEXT", result[1])
                            .build());

            return;
        }

        eventBus.publish(
                EventBuilder.create(EventType.VOICE_CHUNK_ANALYZED)
                        .set("TEXT", result[1])
                        .build());

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

            Gson gson = new Gson(); //GsonBuilder().setFieldNamingStrategy(customPolicy).create();
            System.out.println(stringBuilder.toString());
            result = gson.fromJson(stringBuilder.toString(), DetectIntentResponse.class).getQueryResult();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;

    }

    private String [] processIntent(QueryResult queryResult) {

        if (queryResult.getIntent() != null) {
            switch (queryResult.getIntent().getDisplayName()) {
                case "account.balance":
                    String text = "Here is the list of your accounts. " +
                            "Your Orange Everyday account has $3,234.55. " +
                            "Your Holiday account has $1,134.40. " +
                            "Your Joint account has $933.22. " +
                            queryResult.getFulfillment().getText();
                    return new String[] {queryResult.getIntent().getDisplayName(), text};

                case "account.balance.last.5.yes":
                    String text2 = "Here are the last 5 transactions we have for you. " +
                            "3 weeks ago, you spent $234.34 at Coles. " +
                            "2 weeks ago, you spent $67.76 at Woolworths. " +
                            "3 days ago, you spent $7.80 at Lion Cafe. " +
                            "Yesterday, you spent $12.00 at Seafood Pty Ltd. " +
                            "Moments ago, you spent $60.00 at Opal Travel. " +
                            queryResult.getFulfillment().getText();
                    return new String[] {queryResult.getIntent().getDisplayName(), text2};

                case "quit":
                    return new String[]{queryResult.getIntent().getDisplayName(), queryResult.getFulfillment().getText()};

                case "final.followup.yes":
                case "final.followup.no":
                case "account.balance.last.5.no":

                default:
                    return new String[]{queryResult.getIntent().getDisplayName(), queryResult.getFulfillment().getText()};
            }
        }

        return new String[]{"Null for you",queryResult.getFulfillment().getText()};
    }

    private void printResult(QueryResult result) {
        System.out.println("Intent detection result:");
        System.out.format("  Query: '%s'", result.getQueryText() != null ? result.getQueryText() : "No text received");
        System.out.format("  Response: '%s'", result.getFulfillment().getText() != null ? result.getFulfillment().getText() : "No fulfillment text received");
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

    public class VoiceCapturedHandler implements EventHandler<Event> {

        public EventType getType() {
            return EventType.VOICE_CHUNK_RECORDED;
        }
        public boolean canHandle(EventType eventType) {
            return true; // As long as getType() return not null this method is not called at all
        }
        public void handle(Event event) {

            System.out.println("AudioProcessor got an event: " + event.getType());

            byte[] recorded = event.get("AUDIO", byte[].class);

            intentRecognition(recorded);
        }
    }

    public List<EventHandler> getEventHandlers() {
        return eventHandlers;
    }

}
