
package hello.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class DetectIntentResponse {

    @SerializedName("responseId")
    @Expose
    private String responseId;
    @SerializedName("queryResult")
    @Expose
    private QueryResult queryResult;
    @SerializedName("webhookStatus")
    @Expose
    private WebhookStatus webhookStatus;

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public QueryResult getQueryResult() {
        return queryResult;
    }

    public void setQueryResult(QueryResult queryResult) {
        this.queryResult = queryResult;
    }

    public WebhookStatus getWebhookStatus() {
        return webhookStatus;
    }

    public void setWebhookStatus(WebhookStatus webhookStatus) {
        this.webhookStatus = webhookStatus;
    }

}
