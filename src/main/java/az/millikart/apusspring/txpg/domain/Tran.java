/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package az.millikart.apusspring.txpg.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tran {

    private String phase;
    private String type;
    private String voidKind;
    private String amount;
    private Authentication authentication;

    //res
    private String approvalCode;
    private String billingStatus;
    private String rrn;
    private String ridByPmo;
    private String declineReason;
    private String description;
    private String regTime;

    private BigDecimal storedTokenId;

    @JsonProperty("match")
    private TranMatch tranMatch;

}
