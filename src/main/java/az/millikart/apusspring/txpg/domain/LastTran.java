package az.millikart.apusspring.txpg.domain;

public class LastTran {

    private String approvalCode;
    private String ridByAcquirer;
    private String declineReason;

    public String getApprovalCode() {
        return approvalCode;
    }

    public void setApprovalCode(String approvalCode) {
        this.approvalCode = approvalCode;
    }

    public String getRidByAcquirer() {
        return ridByAcquirer;
    }

    public void setRidByAcquirer(String ridByAcquirer) {
        this.ridByAcquirer = ridByAcquirer;
    }

    public String getDeclineReason() {
        return declineReason;
    }

    public void setDeclineReason(String declineReason) {
        this.declineReason = declineReason;
    }

}
