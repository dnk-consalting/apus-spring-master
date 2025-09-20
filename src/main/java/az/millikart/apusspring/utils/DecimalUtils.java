package az.millikart.apusspring.utils;

import az.millikart.apusspring.model.Acquirer;
import az.millikart.apusspring.model.BillerService;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class DecimalUtils {

    public static Double calcAcquirerSurchargeFee(Double amount, Acquirer acquirer) {
        BigDecimal decimalAmount = new BigDecimal(amount);
        BigDecimal decimalSurcharge = BigDecimal.valueOf(acquirer.getSurcharge());
        BigDecimal decimalMin = BigDecimal.valueOf(acquirer.getMin());
        BigDecimal decimalMax = BigDecimal.valueOf(acquirer.getMax());

        return calcFee(decimalAmount, decimalSurcharge, decimalMin, decimalMax);
    }

    public static Double calcBillerServiceSurchargeFee(Double amount, BillerService billerService) {
        BigDecimal decimalAmount = new BigDecimal(amount);
        BigDecimal decimalSurcharge = BigDecimal.valueOf(billerService.getSurcharge());
        BigDecimal decimalMin = BigDecimal.valueOf(billerService.getMinFee());
        BigDecimal decimalMax = BigDecimal.valueOf(billerService.getMaxFee());

        return calcFee(decimalAmount, decimalSurcharge, decimalMin, decimalMax);
    }

    public static Double calcTotalAmount(Double amount,
                                         Double fee,
                                         Double billerServiceSurchargeFee,
                                         Double acquirerSurchargeFee) {
        BigDecimal decimalAmount = new BigDecimal(amount);
        BigDecimal decimalFee = new BigDecimal(fee);
        BigDecimal decimalBillFee = new BigDecimal(billerServiceSurchargeFee);
        BigDecimal decimalAcqFee = new BigDecimal(acquirerSurchargeFee);

        return decimalAmount.add(decimalFee)
                .add(decimalBillFee)
                .add(decimalAcqFee)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static Double calcTotalFee(Double fee,
                                      Double billerServiceSurchargeFee,
                                      Double acquirerSurchargeFee) {
        BigDecimal decimalFee = new BigDecimal(fee);
        BigDecimal decimalBillFee = new BigDecimal(billerServiceSurchargeFee);
        BigDecimal decimalAcqFee = new BigDecimal(acquirerSurchargeFee);

        return decimalFee
                .add(decimalBillFee)
                .add(decimalAcqFee)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public static Double calcTotalAmount(Double amount,
                                         Double fee) {
        BigDecimal decimalAmount = new BigDecimal(amount);
        BigDecimal decimalFee = new BigDecimal(fee);

        return decimalAmount.add(decimalFee)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }


    private static Double calcFee(BigDecimal decimalAmount,
                                  BigDecimal decimalSurcharge,
                                  BigDecimal decimalMin,
                                  BigDecimal decimalMax
    ) {
        BigDecimal fee;
        fee = decimalAmount.multiply(decimalSurcharge).divide(new BigDecimal(100), RoundingMode.HALF_UP);
        fee = fee.setScale(2, RoundingMode.HALF_UP);

        if (fee.compareTo(decimalMin) < 0) {
            fee = decimalMin;
        } else if (fee.compareTo(decimalMax) > 0) {
            fee = decimalMax;
        }
        return fee.doubleValue();
    }
}
