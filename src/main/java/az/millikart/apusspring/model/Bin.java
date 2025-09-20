package az.millikart.apusspring.model;

import lombok.Data;

@Data
public class Bin {
    private Integer id;
    private Integer acquirer;
    private String bin;
    private Boolean active;
}
