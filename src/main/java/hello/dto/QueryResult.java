
package hello.dto;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class QueryResult {

    @SerializedName("queryText")
    @Expose
    private String queryText;
    @SerializedName("parameters")
    @Expose
    private Parameters parameters;
    @SerializedName("allRequiredParamsCollected")
    @Expose
    private Boolean allRequiredParamsCollected;
    @SerializedName("fulfillment")
    @Expose
    private Fulfillment fulfillment;
    @SerializedName("outputContexts")
    @Expose
    private List<OutputContext> outputContexts = null;
    @SerializedName("intent")
    @Expose
    private Intent intent;
    @SerializedName("intentDetectionConfidence")
    @Expose
    private float intentDetectionConfidence;

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public Boolean getAllRequiredParamsCollected() {
        return allRequiredParamsCollected;
    }

    public void setAllRequiredParamsCollected(Boolean allRequiredParamsCollected) {
        this.allRequiredParamsCollected = allRequiredParamsCollected;
    }

    public Fulfillment getFulfillment() {
        return fulfillment;
    }

    public void setFulfillment(Fulfillment fulfillment) {
        this.fulfillment = fulfillment;
    }

    public List<OutputContext> getOutputContexts() {
        return outputContexts;
    }

    public void setOutputContexts(List<OutputContext> outputContexts) {
        this.outputContexts = outputContexts;
    }

    public Intent getIntent() {
        return intent;
    }

    public void setIntent(Intent intent) {
        this.intent = intent;
    }

    public float getIntentDetectionConfidence() {
        return intentDetectionConfidence;
    }

    public void setIntentDetectionConfidence(Integer intentDetectionConfidence) {
        this.intentDetectionConfidence = intentDetectionConfidence;
    }

}
