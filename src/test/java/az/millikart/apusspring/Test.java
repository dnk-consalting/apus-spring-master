package az.millikart.apusspring;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Test {

    public static void main(String[] args) {
        Double amount  = 35.4;
        Double fee  = 0.8;

        BigDecimal decA = new BigDecimal(amount);
        BigDecimal decF = new BigDecimal(fee);

        System.out.println(amount + fee);
        System.out.println(decA.add(decF).setScale(2 , RoundingMode.HALF_UP).doubleValue());
    }
}
