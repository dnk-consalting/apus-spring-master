package az.millikart.apusspring.model;

import lombok.Data;

@Data
public class Acquirer {

    private Integer id;
    private String name;
    private Double surcharge;
    private Double min;
    private Double max;
    private Boolean active;
    private String txpgLogin;
    private String txpgPassword;
}
