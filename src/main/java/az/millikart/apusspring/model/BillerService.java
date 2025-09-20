package az.millikart.apusspring.model;

import lombok.Data;

@Data
public class BillerService {

    private Integer id;
    private String name;
    private String code;
    private Double surcharge;
    private Integer minFee;
    private Integer maxFee;
    private Integer billerId;
}
