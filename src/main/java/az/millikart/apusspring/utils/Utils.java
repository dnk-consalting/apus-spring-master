package az.millikart.apusspring.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.tomcat.util.codec.binary.Base64;
import org.w3c.dom.Node;
import jakarta.xml.soap.SOAPException;
import jakarta.xml.soap.SOAPMessage;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class Utils {

    private static final String BASIC_AUTH_PREFIX = "Basic ";


    public static String getBasicAuthString(String username, String password) {
        String credentials = username + ":" + password;
        String encoded = java.util.Base64.getEncoder()
                .encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return BASIC_AUTH_PREFIX + encoded;

    }

    public static String getStackTrace(Throwable throwable) {
        Writer buffer = new StringWriter();
        PrintWriter pw = new PrintWriter(buffer);
        throwable.printStackTrace(pw);
        return buffer.toString();
    }

    public static String obj2String(Object ob) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ObjectWriter objectWriter = mapper.writerWithDefaultPrettyPrinter();
        return objectWriter.writeValueAsString(ob);
    }

    public static String obj2JsonString(Object object) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(object);
    }

    public static <T> T convert2Object(String json, Class<T> tClass) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return mapper.readValue(json, tClass);
    }

    public static String soap2String(SOAPMessage message) throws SOAPException, IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        message.writeTo(stream);
        return new String(stream.toByteArray(), StandardCharsets.UTF_8);
    }

    public static String getChildText(Node parent, String childNodeName) {
        for (int j = 0; j < parent.getChildNodes().getLength(); j++) {
            Node item = parent.getChildNodes().item(j);
            if (childNodeName.equals(item.getNodeName())) {
                if (Objects.nonNull(item.getTextContent())) {
                    return item.getTextContent().trim();
                }
            }
        }
        return "";
    }
}
