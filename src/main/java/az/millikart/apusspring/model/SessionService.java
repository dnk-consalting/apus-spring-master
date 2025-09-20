package az.millikart.apusspring.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SessionService {

    private Integer id;
    private Integer sessionId;
    private Integer serviceId;
    private String destination;
    private Double amount;
    private Double fee;
    private String fcm;
    private String invoice;



    public SessionService(Integer sessionId, Integer serviceId, String destination, String invoice, Double amount, Double fee, String fcm) {
        this.sessionId = sessionId;
        this.serviceId = serviceId;
        this.amount = amount;
        this.fee = fee;
        this.destination = destination;
        this.invoice = invoice;
        this.fcm = fcm;
    }

}
