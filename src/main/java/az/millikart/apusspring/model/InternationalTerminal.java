package az.millikart.apusspring.model;

import lombok.Data;

@Data
public class InternationalTerminal {

    private Long id;
    private String biller;
    private String login;
    private String password;

    private String type;
}
