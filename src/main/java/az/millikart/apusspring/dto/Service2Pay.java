package az.millikart.apusspring.dto;

import lombok.Data;

@Data
public class Service2Pay {

    private String name;
    private String serviceCode;
    private Double amount;
    private Double fee;
}
