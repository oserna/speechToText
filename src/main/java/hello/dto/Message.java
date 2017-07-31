
package hello.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Message {

    @SerializedName("text")
    @Expose
    private Text text;
    @SerializedName("payload")
    @Expose
    private Payload payload;

    public Text getText() {
        return text;
    }

    public void setText(Text text) {
        this.text = text;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

}
