package az.millikart.apusspring.utils;

import az.millikart.apusspring.model.Payment;
import az.millikart.apusspring.model.SessionService;
import jakarta.xml.soap.*;

import javax.xml.namespace.QName;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public final class SOAPResponseBuilder {


    public static SOAPMessage error() throws SOAPException {
        return error(null);
    }

    public static SOAPMessage error(String faultString) throws SOAPException {
        SOAPMessage message = MessageFactory.newInstance().createMessage();
        SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();
        envelope.addNamespaceDeclaration("pac", "http://pac.ws.gpp.sinam.net/");
        SOAPFault fault = envelope.getBody().addFault();
        QName faultName = new QName(SOAPConstants.URI_NS_SOAP_ENVELOPE,
                "Server");

        fault.setFaultCode(faultName);

        if (Objects.nonNull(faultString)) {
            fault.setFaultString(faultString);
        }

        Detail detail = fault.addDetail();

        detail.addChildElement("errorCode").setValue("SYSTEM_ERROR");
        detail.addChildElement("errorObjectType").setValue("SYSTEM");
        detail.addChildElement("errorData").setValue("UNKNOWN ERROR");
        detail.addChildElement("errorDateTime").setValue(
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date())
        );

        return message;
    }


    public static SOAPMessage register(String redirectUrl, String residentStatus) throws SOAPException {
        SOAPMessage message = MessageFactory.newInstance().createMessage();
        SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();
        envelope.addNamespaceDeclaration("pac", "http://pac.ws.gpp.sinam.net/");
        SOAPElement body = envelope.getBody().addChildElement("pac:InitiatePaymentResponse");
        SOAPElement element = body.addChildElement("return");

        element.addChildElement("redirectUrl").setValue(redirectUrl);
        element.addChildElement("cardIssuerType").setValue(residentStatus);

        return message;
    }

    public static SOAPMessage commitError(String apusUid,
                                          String apusRid,
                                          String sessionId) throws SOAPException {
        SOAPMessage message = MessageFactory.newInstance().createMessage();
        SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();

        envelope.addNamespaceDeclaration("ser", "http://services.ws.gpp.sinam.net/");
        envelope.addNamespaceDeclaration("pac", "http://pac.portal.ws.gpp.sinam.net/");

        SOAPElement header = envelope.getHeader().addHeaderElement(envelope.createQName("messageHeader", "ser"));
        header.addChildElement("userID").setValue(apusUid);
        header.addChildElement("receiverID").setValue(apusRid);
        header.addChildElement("transactionID").setValue(sessionId);
        header.addChildElement("messageDateTime").setValue(
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date())
        );
        header.addChildElement("messageTimestamp").setValue(Long.toString(System.currentTimeMillis() / 1000));
        SOAPElement body = envelope.getBody().addBodyElement(envelope.createQName(
                "CompletePaymentWithError", "pac"
        )).addChildElement(
                "Reason"
        );
        body.addChildElement("errorObjectType").setValue("AUTHENTICATION");
        body.addChildElement("errorCode").setValue("FAILURE");
        body.addChildElement("errorData").setValue(sessionId);
        body.addChildElement("errorDateTime").setValue(
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date())
        );


        return message;
    }

    public static SOAPMessage commit(String apusUid,
                                     String apusRid,
                                     String sessionId,
                                     List<SessionService> sessionServices,
                                     Payment payment) throws SOAPException {
        SOAPMessage message = MessageFactory.newInstance().createMessage();
        SOAPEnvelope envelope = message.getSOAPPart().getEnvelope();

        envelope.addNamespaceDeclaration("ser", "http://services.ws.gpp.sinam.net/");
        envelope.addNamespaceDeclaration("pac", "http://pac.portal.ws.gpp.sinam.net/");

        SOAPElement header = envelope.getHeader().addHeaderElement(envelope.createQName("messageHeader", "ser"));
        header.addChildElement("userID").setValue(apusUid);
        header.addChildElement("receiverID").setValue(apusRid);
        header.addChildElement("transactionID").setValue(sessionId);
        header.addChildElement("messageDateTime").setValue(
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date())
        );
        header.addChildElement("messageTimestamp").setValue(Long.toString(System.currentTimeMillis() / 1000));
        SOAPElement body = envelope.getBody().addBodyElement(envelope.createQName(
                "CompletePayment", "pac"
        )).addChildElement(
                "PaymentCompletionRequest"
        );

        body.addChildElement("retry").setValue("false");
        body.addChildElement("terminalId").setValue("APUS");

        for (SessionService sessionService : sessionServices) {

            SOAPElement element = body.addChildElement(envelope.createName("paymentList"));
            element.addChildElement("id").setValue(sessionService.getInvoice());
            element.addChildElement("pacTransactionId").setValue(payment.getRrn());
            element.addChildElement("bankAccountNumber");
            element.addChildElement("pacDateTime").setValue(
                    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(new Date())
            );
            element.addChildElement("paymentAmount").setValue(String.valueOf(sessionService.getAmount()));
            element.addChildElement("feeCalculationMethod").setValue(sessionService.getFcm());
            element.addChildElement("feeAmount").setValue(String.valueOf(sessionService.getFee()));
        }

        return message;
    }
}
