package az.millikart.apusspring.txpg.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;


public class Conditions {

    public Conditions(List<String> cofCapturePurposes) {
        this.cofCapturePurposes = cofCapturePurposes;
    }

    @JsonProperty("cofCapturePurposes")
    private List<String> cofCapturePurposes;

    public List<String> getCofCapturePurposes() {
        return cofCapturePurposes;
    }

    public void setCofCapturePurposes(List<String> cofCapturePurposes) {
        this.cofCapturePurposes = cofCapturePurposes;
    }

    public Conditions() {
    }
    
   
}
