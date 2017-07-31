
package hello.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Intent {

    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("displayName")
    @Expose
    private String displayName;
    @SerializedName("webhookState")
    @Expose
    private String webhookState;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getWebhookState() {
        return webhookState;
    }

    public void setWebhookState(String webhookState) {
        this.webhookState = webhookState;
    }

}
