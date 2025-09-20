package az.millikart.apusspring.service;

import az.millikart.apusspring.dto.Template;
import az.millikart.apusspring.model.Payment;
import az.millikart.apusspring.model.Session;
import az.millikart.apusspring.repository.PaymentRepository;
import az.millikart.apusspring.repository.SessionRepository;
import az.millikart.apusspring.utils.SOAPConnectionWrapper;
import az.millikart.apusspring.utils.Utils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.xml.soap.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

@Service
@Slf4j(topic = "service_logger")
public class RedirectService {

    private final String apusUid;
    private final String apusRid;
    private final String apusTid;
    private final String gppsPath;
    private final String gppsPassword;
    private final String tietoRedirectString;
    private final String applicationHost;

    private final PaymentRepository paymentRepository;
    private final SessionRepository sessionRepository;

    public RedirectService(@Value("${apus.uId}") String apusUid,
                           @Value("${apus.rId}") String apusRid,
                           @Value("${apus.tId}") String apusTid,
                           @Value("${security.gpps.path}") String gppsPath,
                           @Value("${security.gpps.password}") String gppsPassword,
                           @Value("${e-commerce.redirect}") String tietoRedirectString,
                           @Value("${application.address}") String applicationHost,
                           PaymentRepository paymentRepository,
                           SessionRepository sessionRepository) {
        this.apusUid = apusUid;
        this.apusRid = apusRid;
        this.apusTid = apusTid;
        this.gppsPath = gppsPath;
        this.gppsPassword = gppsPassword;
        this.tietoRedirectString = tietoRedirectString;
        this.applicationHost = applicationHost;
        this.paymentRepository = paymentRepository;
        this.sessionRepository = sessionRepository;
    }

    public Object redirect(HttpServletRequest request, String referer, String order) {
        Template template = new Template();
        template.setType("1");
        try {
            log.info("Redirect request");
            log.info("Refer : {}", referer);
            log.info("Request URL:{}", request.getRequestURL().toString());
            log.info("Request Query params :{}", request.getQueryString());

            if (Objects.isNull(order)) {
                throw new RuntimeException("XID not provided!");
            }

            log.info("Order(xid) : {}", order);

            Payment payment = paymentRepository.getByOrder(order)
                    .<RuntimeException>orElseThrow(() -> {
                        throw new RuntimeException("Order not found!");
                    });


            Session session = sessionRepository.getById(payment.getSessionId())
                    .<RuntimeException>orElseThrow(() -> {
                        throw new RuntimeException("Session not found!");
                    });

            template.setSession(session.getValue());
            template.setUrl(session.getRedirectUrl());
            template.setLang(session.getLanguage());

            if (Objects.nonNull(session.getClosed()) && session.getClosed()) {
                throw new RuntimeException("Session is closed!");
            }


            String redirect = "https://gpp.az/";

            if (referer == null || new URL(referer).getHost().equals(new URL(this.applicationHost).getHost())) {
                redirect = session.getRedirectUrl();
            }

            SOAPMessage message = MessageFactory.newInstance().createMessage();
            SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();

            envelope.addNamespaceDeclaration("ser", "http://services.ws.gpp.sinam.net/");
            envelope.addNamespaceDeclaration("pac", "http://pac.portal.ws.gpp.sinam.net/");

            SOAPElement header = envelope.getHeader().addHeaderElement(envelope.createQName("messageHeader", "ser"));
            header.addChildElement("userID").setValue(this.apusUid);
            header.addChildElement("receiverID").setValue(this.apusRid);
            header.addChildElement("transactionID").setValue(session.getValue());
            header.addChildElement("messageDateTime").setValue(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date()));
            header.addChildElement("messageTimestamp").setValue(Long.toString(System.currentTimeMillis() / 1000));

            SOAPElement body = envelope.getBody().addBodyElement(envelope.createQName(
                    "CompletePaymentWithError", "pac"
            )).addChildElement(
                    "Reason"
            );
            body.addChildElement("errorObjectType").setValue("SYSTEM");
            body.addChildElement("errorCode").setValue("FAILURE");
            body.addChildElement("errorData").setValue("USER");
            body.addChildElement("errorDateTime").setValue(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date()));


            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            message.writeTo(stream);
            String xml = new String(stream.toByteArray(), StandardCharsets.UTF_8);

            log.info("Sending answer...");
            log.info("Answer contents:\r\n {}", xml);

            SOAPConnectionWrapper wrapper = new SOAPConnectionWrapper(
                    SOAPConnectionFactory.newInstance().createConnection()
            );
            KeyManagerFactory factory
                    = KeyManagerFactory.getInstance("SunX509");
            KeyStore store
                    = KeyStore.getInstance("JKS");
            InputStream input = Files.newInputStream(new File(this.gppsPath).toPath());
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

            for (int i = 0; i < 3; i++) {
                try {
                    SOAPMessage msg = wrapper.call(
                            message, "https://secure.test.apus.az/WEBAPUS/GPPWebCardProcessWS?wsdl"
                    );
                    stream.reset();

                    msg.writeTo(stream);
                    xml = new String(stream.toByteArray(), StandardCharsets.UTF_8);
                    log.info("RESPONSE MSG {}", xml);
                    break;
                } catch (SOAPException x) {
                    log.warn("Failed to commit payment. Error: {}", x.getMessage()
                    );

                    try {
                        if (i != 2) {
                            Thread.sleep(5000);
                            continue;
                        }
                    } catch (InterruptedException e) {
                    }
                    throw x;
                }
            }

            return redirect;
        } catch (Exception e) {
            String trace = Utils.getStackTrace(e);
            log.error(e.getMessage() + "\r\n\r\n" + trace + "\r\n");

            return template;
        }
    }
}
