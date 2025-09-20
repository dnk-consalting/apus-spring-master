package az.millikart.apusspring.model;

import lombok.Data;

@Data
public class Payment {

    private Integer id;
    private String pid;
    private String xid;
    private String aac;
    private String rrn;
    private String code;
    private Integer status;
    private Integer sessionId;
    private String orderPassword;

}
