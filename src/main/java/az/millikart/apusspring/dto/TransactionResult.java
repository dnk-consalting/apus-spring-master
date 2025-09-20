package az.millikart.apusspring.dto;

import lombok.Data;

@Data
public class TransactionResult {
    private String paymentId;
    private String code;
    private String rrn;
    private String approvalCode;
    private Type type;

}
