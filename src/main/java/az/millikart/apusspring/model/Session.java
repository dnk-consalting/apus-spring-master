package az.millikart.apusspring.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class Session {

    private Integer id;
    private String value;
    private String bin;
    private Double amount;
    private Double fee;
    private String currency;
    private Date date;
    private String type;
    private String commitUrl;
    private String redirectUrl;
    private String ip;
    private String language;
    private Integer status;
    private String description;
    private Boolean blocked;
    private Boolean closed;

    public Session(String value, Double amount, Double fee, String currency, Date date, String type, String commitUrl, String redirectUrl, String ip, String language) {
        this.value = value;
        this.amount = amount;
        this.fee = fee;
        this.currency = currency;
        this.date = date;
        this.type = type;
        this.commitUrl = commitUrl;
        this.redirectUrl = redirectUrl;
        this.ip = ip;
        this.language = language;
    }
}
