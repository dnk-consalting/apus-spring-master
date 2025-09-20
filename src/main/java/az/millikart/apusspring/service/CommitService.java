package az.millikart.apusspring.service;

import az.millikart.apusspring.dto.Template;
import az.millikart.apusspring.dto.TransactionResult;
import az.millikart.apusspring.model.*;
import az.millikart.apusspring.repository.*;
import az.millikart.apusspring.txpg.TXPGClient;
import az.millikart.apusspring.utils.DecimalUtils;
import az.millikart.apusspring.utils.SOAPConnectionWrapper;
import az.millikart.apusspring.utils.SOAPResponseBuilder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.soap.SOAPConnectionFactory;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j(topic = "service_logger")
public class CommitService {

    private final String apusUid;
    private final String apusRid;
    private final String apusTid;
    private final String gppsPath;
    private final String gppsPassword;
    private final TXPGClient txpgClient;

    private final PaymentRepository paymentRepository;
    private final SessionRepository sessionRepository;
    private final BinRepository binRepository;
    private final AcquirerRepository acquirerRepository;
    private final SessionServiceRepository sessionServiceRepository;
    private final BillerRepository billerRepository;
    private final BillerServiceRepository billerServiceRepository;


    public CommitService(@Value("${apus.uId}") String apusUid,
                         @Value("${apus.rId}") String apusRid,
                         @Value("${apus.tId}") String apusTid,
                         @Value("${security.gpps.path}") String gppsPath,
                         @Value("${security.gpps.password}") String gppsPassword,
                         TXPGClient txpgClient,
                         PaymentRepository paymentRepository,
                         SessionRepository sessionRepository,
                         BinRepository binRepository,
                         AcquirerRepository acquirerRepository,
                         SessionServiceRepository sessionServiceRepository,
                         BillerRepository billerRepository,
                         BillerServiceRepository billerServiceRepository) {
        this.apusUid = apusUid;
        this.apusRid = apusRid;
        this.apusTid = apusTid;
        this.gppsPath = gppsPath;
        this.gppsPassword = gppsPassword;
        this.txpgClient = txpgClient;
        this.paymentRepository = paymentRepository;
        this.sessionRepository = sessionRepository;
        this.binRepository = binRepository;
        this.acquirerRepository = acquirerRepository;
        this.sessionServiceRepository = sessionServiceRepository;
        this.billerRepository = billerRepository;
        this.billerServiceRepository = billerServiceRepository;
    }


    public Template commitPayment(HttpServletRequest request,
                                  String transId,
                                  String error,
                                  String order,
                                  String password) {
        log.info("Commit request. Request URL : {}", request.getRequestURL());
        Template template = new Template();
        template.setType("0");
        try {
            log.info("Order : {} ", order);


            if (Objects.nonNull(error)) {
                throw new RuntimeException("E-commerce reported an error: " + error);
            }
            if (Objects.isNull(order) || order.isEmpty()) {
                throw new RuntimeException("Order not supplied!");
            }

            Payment payment = paymentRepository.getByOrder(order)
                    .<RuntimeException>orElseThrow(() -> new RuntimeException("Order not found!"));

            log.info("{}", payment);

            Session session = sessionRepository.getById(payment.getSessionId())
                    .<RuntimeException>orElseThrow(() -> new RuntimeException("Session not found!"));

            log.info("{}", session);

            if (Objects.nonNull(session.getClosed()) && session.getClosed()) {
                throw new RuntimeException("Session is closed!");
            }
            if (Objects.nonNull(session.getClosed()) && session.getBlocked()) {
                throw new RuntimeException("Session is blocked!");
            }

            template.setSession(session.getValue());
            template.setUrl(session.getRedirectUrl());
            template.setLang(session.getLanguage());

            session.setBlocked(true);
            sessionRepository.update(session);

            try {
                Bin bin = binRepository.getByBin(session.getBin()).get(0);


                Acquirer acquirer = acquirerRepository.getById(bin.getAcquirer())
                        .<RuntimeException>orElseThrow(() -> new RuntimeException("Certificate for BIN not found! BIN : " + session.getBin()));


                List<SessionService> sessionServices = sessionServiceRepository.getBySessionId(session.getId());

                List<String> billerList = new ArrayList<>();
                for (SessionService sessionService : sessionServices) {
                    Optional<BillerService> optionalBillerService =
                            billerServiceRepository.getById(sessionService.getServiceId());

                    optionalBillerService
                            .flatMap(service -> billerRepository.getById(service.getId()))
                            .ifPresent(biller -> billerList.add(biller.getCode()));
                }

                TransactionResult result = txpgClient.getTransactionResult(
                        order,
                        payment.getOrderPassword(),
                        acquirer.getTxpgLogin(),
                        acquirer.getTxpgPassword()
                );


                payment.setRrn("0");

                SOAPMessage message = SOAPResponseBuilder.commitError(
                        this.apusUid,
                        this.apusRid,
                        session.getValue()
                );

                switch (result.getType()) {
                    case OK: {
                        payment.setRrn(result.getRrn());
                        payment.setStatus(0);

                        message = SOAPResponseBuilder.commit(
                                this.apusUid,
                                this.apusRid,
                                session.getValue(),
                                sessionServices,
                                payment
                        );
                        break;
                    }
                    case Failed: {
                        payment.setStatus(1);
                        break;
                    }
                    case Created: {
                        payment.setStatus(2);
                        break;
                    }
                    case Pending: {
                        payment.setStatus(3);
                        break;
                    }
                    case Declined: {
                        payment.setStatus(4);
                        break;
                    }
                    case Reversed: {
                        payment.setStatus(5);
                        break;
                    }
                    case AutoReversed: {
                        payment.setStatus(6);
                        break;
                    }
                    case Timeout: {
                        payment.setStatus(7);
                        break;
                    }
                    default: {
                        throw new Exception("Could not get transaction result");
                    }
                }
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                message.writeTo(stream);
                String xml = stream.toString(StandardCharsets.UTF_8);

                log.info("Transaction {} status is: {}", order, result.getType());
                log.info("Sending answer...");
                log.info("Answer contents:\r\n {}", xml);

                SOAPConnectionWrapper wrapper = new SOAPConnectionWrapper(
                        SOAPConnectionFactory.newInstance().createConnection()
                );
                KeyManagerFactory factory
                        = KeyManagerFactory.getInstance("SunX509");
                KeyStore store
                        = KeyStore.getInstance("JKS");
                InputStream input
                        = Files.newInputStream(new File(this.gppsPath).toPath());
                store.load(input, this.gppsPassword.toCharArray());
                input.close();
                factory.init(store, this.gppsPassword.toCharArray());
                SSLContext context
                        = SSLContext.getInstance("TLS");
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm()
                );
                tmf.init(store);
                context.init(
                        factory.getKeyManagers(),
                        tmf.getTrustManagers(),
                        new SecureRandom()
                );

                wrapper.setSSLContext(context);
                payment.setCode(result.getCode());
                payment.setPid(result.getPaymentId());
                payment.setAac(result.getApprovalCode());
                paymentRepository.update(payment);

                log.info("CommitURL {}", session.getCommitUrl());

                for (int i = 0; i < 3; i++) {
                    try {
                        SOAPMessage msg = wrapper.call(
                                message, session.getCommitUrl()
                        );
                        stream.reset();
                        log.info("RESPONSE MSG : {}", msg);

                        msg.writeTo(stream);
                        xml = stream.toString(StandardCharsets.UTF_8);
                        break;
                    } catch (SOAPException x) {
                        log.warn("Failed to commit payment. Error: {}", x.getMessage());

                        try {
                            if (i != 2) {
                                Thread.sleep(5000);
                                continue;
                            }
                        } catch (InterruptedException ignored) {
                        }
                        throw x;
                    }
                }

                if (!xml.isEmpty()) {
                    log.info("Response contents:\r\n {}", xml);

                    Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                            new InputSource(new StringReader(xml)
                            ));

                    if (payment.getStatus().equals(0)) {
                        if (document.getElementsByTagName("faultcode").getLength() > 0) {
                            // Reverse
                            log.info("Error response from APUS.Trying to reverse");

                            boolean reversed = txpgClient.reverseTransaction(order,
                                    DecimalUtils.calcTotalAmount(session.getAmount(), session.getFee()),
                                    acquirer.getTxpgLogin(),
                                    acquirer.getTxpgPassword()
                            );

                            if (!reversed) {
                                // System failed
                                throw new Exception("Reversal has failed");
                            } else {
                                // Apus failed
                                template.setType("2");
                                log.info("Transaction '{}' has been reversed successfully", order);
                            }
                            session.setStatus(1);
                        } else {
                            // OK
                            session.setStatus(0);
                            log.info("Transaction '{}' has been committed successfully", order);
                        }
                    } else {
                        log.warn("Payment status was not successful");
                        session.setStatus(1);
                        template.setType("3");
                    }

                } else {
                    log.warn("APUS response is empty");
                    session.setStatus(-1);
                    if (payment.getStatus().equals(0)) {
                        template.setType("0");
                    } else {
                        template.setType("3");
                    }
                    log.warn("Failed to commit payment");
                }

            } finally {
                session.setClosed(true);
                session.setBlocked(false);
                sessionRepository.update(session);
            }
        } catch (Exception e) {
            StringWriter trace
                    = new StringWriter();
            e.printStackTrace(new PrintWriter(trace));
            // System failed
            template.setType("1");
            log.error(e.getMessage() + "\r\n" + trace + "\r\n");
        }
        return template;
    }
}

