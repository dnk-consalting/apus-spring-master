package az.millikart.apusspring.service;

import az.millikart.apusspring.dto.Service2Pay;
import az.millikart.apusspring.exception.ClientException;
import az.millikart.apusspring.model.*;
import az.millikart.apusspring.repository.*;
import az.millikart.apusspring.txpg.TXPGClient;
import az.millikart.apusspring.txpg.domain.CreateOrderResponse;
import az.millikart.apusspring.utils.DecimalUtils;
import az.millikart.apusspring.utils.SOAPResponseBuilder;
import az.millikart.apusspring.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j(topic = "service_logger")
public class RegistrationService {

    private final String txpgEcomRedirectUrl;

    private final TXPGClient txpgClient;
    private final SessionRepository sessionRepository;
    private final BinRepository binRepository;
    private final AcquirerRepository acquirerRepository;
    private final BillerRepository billerRepository;
    private final BillerServiceRepository billerServiceRepository;
    private final SessionServiceRepository sessionServiceRepository;
    private final PaymentRepository paymentRepository;
    private final InternationalTerminalRepository internationalTerminalRepository;

    public RegistrationService(@Value("${txpg.redirect-url}") String txpgEcomRedirectUrl,
                               TXPGClient txpgClient,
                               SessionRepository sessionRepository,
                               BinRepository binRepository,
                               AcquirerRepository acquirerRepository,
                               BillerRepository billerRepository,
                               BillerServiceRepository billerServiceRepository,
                               SessionServiceRepository sessionServiceRepository,
                               PaymentRepository paymentRepository,
                               InternationalTerminalRepository internationalTerminalRepository) {
        this.txpgEcomRedirectUrl = txpgEcomRedirectUrl;
        this.txpgClient = txpgClient;
        this.sessionRepository = sessionRepository;
        this.binRepository = binRepository;
        this.acquirerRepository = acquirerRepository;
        this.billerRepository = billerRepository;
        this.billerServiceRepository = billerServiceRepository;
        this.sessionServiceRepository = sessionServiceRepository;
        this.paymentRepository = paymentRepository;
        this.internationalTerminalRepository = internationalTerminalRepository;
    }

    public String registerPayment(String xml, HttpServletRequest request) throws SOAPException, IOException, SOAPException {
        log.info("Request contents: {}", xml);

        SOAPMessage message = SOAPResponseBuilder.error();
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                    new InputSource(new StringReader(xml))
            );

            //TODO validate
            String language = document.getElementsByTagName("language").item(0).getFirstChild().getNodeValue();
            String transactionID = document.getElementsByTagName("transactionID").item(0).getFirstChild().getNodeValue();

            String cardBin = "";
            String swiftBic = "";

            boolean byBin = false;

            if (document.getElementsByTagName("cardBinCode").getLength() > 0) {
                log.info("Here");
                cardBin = document.getElementsByTagName("cardBinCode").item(0).getFirstChild().getNodeValue();
                log.info("cardBinCode : {}", cardBin);
                byBin = true;
            } else if (document.getElementsByTagName("swiftBic").getLength() > 0) {
                swiftBic = document.getElementsByTagName("swiftBic").item(0).getFirstChild().getNodeValue();
                log.info("swiftBicCode : {}", swiftBic);
            }

            String secType = document.getElementsByTagName("securityType").item(0).getFirstChild().getNodeValue();
            String payComUrl = document.getElementsByTagName("paymentCompletionUrl").item(0).getFirstChild().getNodeValue();
            String screenMode = document.getElementsByTagName("screenMode").item(0).getFirstChild().getNodeValue();
            String reUrl = document.getElementsByTagName("redirectUrl").item(0).getFirstChild().getNodeValue();

            if (document.getElementsByTagName("clearingSystem").getLength() > 0) {
                String clearingSystem = document.getElementsByTagName("clearingSystem").item(0).getFirstChild().getNodeValue();
                log.info("clearingSystem : {}", clearingSystem);
            }


            log.info("language : {}", language);
            log.info("transactionID : {}", transactionID);
            log.info("securityType : {}", secType);
            log.info("paymentCompletionUrl : {}", payComUrl);
            log.info("screenMode : {}", screenMode);
            log.info("redirectUrl : {}", reUrl);


            sessionRepository.getByValue(transactionID).ifPresent(session -> {
                throw new RuntimeException("Using duplicated session!");
            });

            Acquirer acquirer;
            String residentStatus;
            if (byBin) {
                Bin bin = binRepository.getByBin(cardBin).get(0);
                acquirer = acquirerRepository.getById(bin.getAcquirer())
                        .<RuntimeException>orElseThrow(() -> new RuntimeException("Acquirer with such BIN not found!"));

                residentStatus = bin.getBin().equals("000000") ? "NON_RESIDENT" : "RESIDENT";
            } else {
                acquirer = acquirerRepository.getByBic(swiftBic)
                        .<RuntimeException>orElseThrow(() -> new RuntimeException("Acquirer with such BIC not found!"));

                residentStatus = "RESIDENT";
            }

            if (Objects.isNull(acquirer)) {
                throw new RuntimeException("Acquirer not found!");
            }

            log.info("Acquirer : {}", acquirer.getName());


            List<String> billerList = new ArrayList<>();

            NodeList paymentList = document.getElementsByTagName("paymentList");

            double payAmount = 0.00;
            double feeAmount = 0.00;

            List<Service2Pay> service2PayList = new ArrayList<>();
            for (int i = 0; i < paymentList.getLength(); i++) {
                Service2Pay service2Pay = new Service2Pay();
                Node node = paymentList.item(i);
                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    for (int j = 0; j < node.getChildNodes().getLength(); j++) {
                        Node item = node.getChildNodes().item(j);
                        switch (item.getNodeName()) {
                            case "scCode":
                                String scCodeTagVal = item.getTextContent().trim();
                                billerList.add(scCodeTagVal);
                                break;
                            case "serviceCode":
                                String serviceCodeTagVal = item.getTextContent().trim();
                                service2Pay.setServiceCode(serviceCodeTagVal);
                                break;
                            case "paymentAmount":
                                double paymentAmountTagVal = Double.parseDouble(item.getTextContent().trim());
                                service2Pay.setAmount(paymentAmountTagVal);

                                payAmount += paymentAmountTagVal;
                                log.info("{}", payAmount);
                                break;
                            case "feeAmount":
                                double feeAmountTagVal = Double.parseDouble(item.getTextContent().trim());
                                service2Pay.setFee(feeAmountTagVal);
                                feeAmount += feeAmountTagVal;
                                log.info("{}", feeAmount);
                                break;
                        }
                    }
                }
                service2PayList.add(service2Pay);
            }

            Session session = new Session(
                    transactionID,
                    payAmount,
                    feeAmount,
                    "944",
                    new Date(),
                    secType,
                    payComUrl,
                    reUrl,
                    request.getRemoteAddr(),
                    language
            );

            if (byBin) {
                session.setBin(cardBin);
            } else {
                session.setBin(swiftBic);
            }

            sessionRepository.save(session);

            log.info("Session : {}", session);

            if (session.getId() == null || session.getId() < 1) {
                throw new RuntimeException("Could not save session to database");
            }

            double billerServiceSurchargeFee = 0;
            double acquirerSurchargeFee = 0;

            List<SessionService> sessionServices = new ArrayList<>();


            for (int i = 0; i < paymentList.getLength(); i++) {
                Node paymentItem = paymentList.item(i);

                if (paymentItem.getNodeType() == Node.ELEMENT_NODE) {

                    String scCode = Utils.getChildText(paymentItem, "scCode");

                    log.info("scCode : {}", scCode);

                    Biller billers = billerRepository.getByCode(scCode)
                            .<RuntimeException>orElseThrow(() -> new RuntimeException(String.format(
                                    "Biller with code '%s' not found",
                                    scCode
                            )));


                    String serviceCode = Utils.getChildText(paymentItem, "serviceCode");
                    log.info("serviceCode : {}", serviceCode);
                    List<BillerService> billerServiceList = billerServiceRepository
                            .getByBillerAndCode(billers.getId(), serviceCode);

                    if (billerServiceList.isEmpty()) {
                        throw new RuntimeException(String.format(
                                "Service with code '%s' not found",
                                serviceCode
                        ));
                    }

                    BillerService billerService = billerServiceList.get(0);


                    for (Service2Pay service2Pay : service2PayList) {
                        if (service2Pay.getServiceCode().equals(serviceCode)) {
                            service2Pay.setName(billerService.getName());
                            break;
                        }
                    }

                    log.info("Biller service : {}", billerService);

                    String amountVal = Utils.getChildText(paymentItem, "paymentAmount");
                    String feeVal = Utils.getChildText(paymentItem, "feeAmount");

                    Double amount = Double.parseDouble(amountVal);
                    Double fee = Double.parseDouble(feeVal);

                    if (acquirer.getSurcharge() != 0) {
                        acquirerSurchargeFee = DecimalUtils.calcAcquirerSurchargeFee(amount, acquirer);

                    } else if (billerService.getSurcharge() != 0) {
                        billerServiceSurchargeFee = DecimalUtils.calcBillerServiceSurchargeFee(amount, billerService);
                    }

                    String fcm = Utils.getChildText(paymentItem, "feeCalculationMethod");

                    sessionServices.add(new SessionService(
                            session.getId(),
                            billerService.getId(),
                            "",
                            Utils.getChildText(paymentItem, "id"),
                            amount,
                            fee,
                            fcm
                    ));
                }
            }

            log.info("{}", sessionServices);

            this.saveSessionServices(sessionServices);

            Payment payment = new Payment();
            payment.setSessionId(session.getId());

            paymentRepository.save(payment);

            String xid = "";

            String description = "";
            if (byBin) {
                description = String.format(
                        "{bin:%s,fee:%s,surcharge:%s}",
                        cardBin,
                        session.getFee() + billerServiceSurchargeFee,
                        acquirerSurchargeFee
                );
            } else {
                description = String.format(
                        "{fee:%s,surcharge:%s}",
                        session.getFee() + billerServiceSurchargeFee,
                        acquirerSurchargeFee
                );
            }

            String billerListStr = this.getBillerListString(billerList);


            Double totalAmount = DecimalUtils.calcTotalAmount(
                    session.getAmount(), session.getFee(), billerServiceSurchargeFee, acquirerSurchargeFee
            );

            Double totalFee = DecimalUtils.calcTotalFee(
                    session.getFee(), billerServiceSurchargeFee, acquirerSurchargeFee
            );

            String login = acquirer.getTxpgLogin();
            String password = acquirer.getTxpgPassword();
            String acquirerName = acquirer.getName().trim();


            if (acquirerName.equals("GPP International")) {
                InternationalTerminal internationalTerminal = internationalTerminalRepository.getByBiller(billerList.get(0))
                        .<RuntimeException>orElseThrow(() -> new RuntimeException("International terminal for biller not found"));

                login = internationalTerminal.getLogin();
                password = internationalTerminal.getPassword();
            }

            log.info("Selected login : {}", login);

            boolean tds = true;
            if (acquirerName.equalsIgnoreCase("ASB")
                    || acquirerName.equalsIgnoreCase("NakhchivanBank")) {
                log.info("NON-3D transaction for {}" , acquirerName);
                tds = false;
            }

                CreateOrderResponse registerTransaction = txpgClient.registerTransaction(
                        tds,
                        totalAmount,
                        totalFee,
                        session.getCurrency(),
                        description,
                        session.getValue(),
                        session.getLanguage(),
                        login,
                        password,
                        billerListStr,
                        screenMode,
                        service2PayList
                );
            payment.setOrderPassword(registerTransaction.getOrder().getPassword());

            xid = String.valueOf(registerTransaction.getOrder().getId());


            log.info("Transaction has been registered. XID: {}", xid);
            payment.setXid(xid);
            paymentRepository.update(payment);

            String redirectUrl = String.format(this.txpgEcomRedirectUrl, xid);


            message = SOAPResponseBuilder.register(redirectUrl, residentStatus);

        } catch (Exception e) {
            String stackTrace = Utils.getStackTrace(e);
            if (e instanceof ClientException) {
                log.warn(e.getMessage() + "\r\n\r\n" + stackTrace);
            } else {
                log.error(e.getMessage() + "\r\n\r\n" + stackTrace);
            }
        }

        String responseXml = Utils.soap2String(message);
        log.info("Response contents: {}", responseXml);

        return responseXml;
    }

    private void saveSessionServices(List<SessionService> sessionServices) {
        sessionServices.forEach(sessionServiceRepository::save);
    }

    private String getBillerListString(List<String> billerList) {
        StringBuilder billerStr = new StringBuilder();
        billerList.forEach(s -> {
            billerStr.append(s).append(", ");
        });
        billerStr.delete(billerStr.lastIndexOf(", "), billerStr.length() - 1);
        return billerStr.toString().trim();
    }


}


