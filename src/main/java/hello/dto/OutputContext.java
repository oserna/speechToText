
package hello.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class OutputContext {

    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("lifespanCount")
    @Expose
    private Integer lifespanCount;
    @SerializedName("parameters")
    @Expose
    private Parameters_ parameters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLifespanCount() {
        return lifespanCount;
    }

    public void setLifespanCount(Integer lifespanCount) {
        this.lifespanCount = lifespanCount;
    }

    public Parameters_ getParameters() {
        return parameters;
    }

    public void setParameters(Parameters_ parameters) {
        this.parameters = parameters;
    }

}
