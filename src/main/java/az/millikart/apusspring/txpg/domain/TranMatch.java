/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package az.millikart.apusspring.txpg.domain;

class TranMatch {

    private String tranActionId;
    private String ridByPmo;

    public TranMatch(String tranActionId, String ridByPmo) {
        this.tranActionId = tranActionId;
        this.ridByPmo = ridByPmo;
    }

    public String getTranActionId() {
        return tranActionId;
    }

    public void setTranActionId(String tranActionId) {
        this.tranActionId = tranActionId;
    }

    public String getRidByPmo() {
        return ridByPmo;
    }

    public void setRidByPmo(String ridByPmo) {
        this.ridByPmo = ridByPmo;
    }

}
