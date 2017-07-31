
package hello.dto;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Parameters_ {

    @SerializedName("account-type")
    @Expose
    private String accountType;
    @SerializedName("account-type.original")
    @Expose
    private String accountTypeOriginal;

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getAccountTypeOriginal() {
        return accountTypeOriginal;
    }

    public void setAccountTypeOriginal(String accountTypeOriginal) {
        this.accountTypeOriginal = accountTypeOriginal;
    }

}
