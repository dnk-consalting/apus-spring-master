package az.millikart.apusspring.txpg.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {

    //req
    private String typeRid;
    private String ridByMerchant;
    private String amount;
    private String currency;
    private String description;
    private String language;
    private String hppRedirectUrl;
    private Consumer consumer;
    private List<CustAttr> custAttrs;
    private List<String> hppCofCapturePurposes;

    @JsonProperty("subMerchant")
    private TxpgMerchant merchant;
    //res
    private String hppUrl;
    private Long id;
    private String status;
    private String password;
    private String tdsV1AuthStatus;
    private String tdsV2AuthStatus;

    @JsonProperty("dstToken")
    private DestinationToken destinationToken;

    @JsonProperty("srcToken")
    private SourceToken sourceToken;

    @JsonProperty("lastTran")
    private LastTran lastTran;
    
    @JsonProperty("trans")
    private List<Tran> trans;

}
