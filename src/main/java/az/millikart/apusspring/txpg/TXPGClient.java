/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package az.millikart.apusspring.txpg;

import az.millikart.apusspring.dto.Service2Pay;
import az.millikart.apusspring.dto.TransactionResult;
import az.millikart.apusspring.dto.Type;
import az.millikart.apusspring.exception.ClientException;
import az.millikart.apusspring.txpg.domain.*;
import az.millikart.apusspring.utils.HttpUtils;
import az.millikart.apusspring.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


@Component
@Slf4j(topic = "service_logger")
public class TXPGClient {

    private final String url;
    private final String typeRid;
    private final String noTdsTypeRid;
    private final String createOrderUri;
    private final String proxyUrl;
    private final String commitUrl;
    private final String orderDetailsUri;
    private final String execTranRefundUri;

    private final HttpUtils httpUtils;

    public TXPGClient(@Value("${txpg.url}") String url,
                      @Value("${txpg.typeRid}") String typeRid,
                      @Value("${txpg.noTdsTypeRid}") String noTdsTypeRid,
                      @Value("${txpg.createOrder}") String createOrderUri,
                      @Value("${txpg.proxy-url}") String proxyUrl,
                      @Value("${txpg.commit-url}") String commitUrl,
                      @Value("${txpg.getOrderDetails}") String orderDetailsUri,
                      @Value("${txpg.execTranRefundUri}") String execTranRefundUri,
                      HttpUtils httpUtils) {
        this.url = url;
        this.typeRid = typeRid;
        this.noTdsTypeRid = noTdsTypeRid;
        this.createOrderUri = createOrderUri;
        this.proxyUrl = proxyUrl;
        this.commitUrl = commitUrl;
        this.orderDetailsUri = orderDetailsUri;
        this.execTranRefundUri = execTranRefundUri;
        this.httpUtils = httpUtils;
    }

    public CreateOrderResponse registerTransaction(
            boolean tds,
            Double amount,
            Double fee,
            String currency,
            String description,
            String tranId,
            String language,
            String login,
            String password,
            String billerListStr,
            String screenMode,
            List<Service2Pay> service2PayList
    ) throws JsonProcessingException {

        Order order = new Order();

        order.setTypeRid(noTdsTypeRid);
        if (tds) {
            order.setTypeRid(typeRid);
        }

        order.setHppRedirectUrl(this.commitUrl);
        order.setAmount(String.valueOf(amount));
        order.setRidByMerchant(tranId);
        order.setCurrency("AZN");
        order.setDescription(description);
        order.setLanguage(language.toLowerCase());

        List<CustAttr> custAttrs = new ArrayList<>();
        if (screenMode.equalsIgnoreCase("DESKTOP")) {
            custAttrs.add(new CustAttr("template", "gpp"));
        } else {
            custAttrs.add(new CustAttr("template", "gpp-mobile"));
        }
        custAttrs.add(new CustAttr("F98", String.valueOf(fee)));
        custAttrs.add(new CustAttr("F104", billerListStr));
        custAttrs.add(new CustAttr("screenMode", screenMode));
        custAttrs.add(new CustAttr("services", Utils.obj2JsonString(service2PayList)));

        order.setCustAttrs(custAttrs);
        order.setMerchant(new TxpgMerchant("https://millikart.az"));

        CreateOrder createOrder = new CreateOrder(order);

        HttpHeaders headers = new HttpHeaders();

        String url = this.proxyUrl + this.createOrderUri;

        log.info("Create order request URL : {}", url);
        log.info("Create order request : {}", Utils.obj2String(createOrder));

        CreateOrderResponse response = httpUtils.sendHttpRequest(
                url,
                HttpMethod.POST,
                this.getHttpEntity(createOrder, headers, login, password),
                CreateOrderResponse.class
        );

        log.info("Create order response : {}", Utils.obj2String(response));
        return response;
    }

    public TransactionResult getTransactionResult(String xid,
                                                  String orderPassword,
                                                  String login,
                                                  String password)
            throws ClientException {

        try {
            TransactionResult result = new TransactionResult();
            result.setPaymentId(xid);

            String url = String.format(this.url + this.orderDetailsUri, xid, orderPassword);
            log.info("Get order details request to TXPG : {}", url);

            GetOrderDetailsResponse response = httpUtils.sendHttpRequest(
                    url,
                    HttpMethod.GET,
                    this.getHttpEntity(null, login, password),
                    GetOrderDetailsResponse.class);

            log.info("Get order details response : {}", Utils.obj2String(response));
            String status = response.getOrder().getStatus();

            switch (status) {
                case "FullyPaid": {
                    Tran tran = response.getOrder().getTrans().getFirst();

                    result.setType(Type.OK);
                    result.setCode("0");
                    if (Objects.nonNull(tran.getRrn())) {
                        result.setRrn(tran.getRrn());
                    } else {
                        result.setRrn(tran.getRidByPmo());
                    }

                    result.setApprovalCode(tran.getApprovalCode());
                    break;
                }
                case "Closed": {
                    if (response.getOrder().getTrans() != null) {
                        List<Tran> tranList = response.getOrder().getTrans();
                        if (tranList.size() == 1) {
                            Tran purchaseTran = tranList.getFirst();
                            if (purchaseTran.getBillingStatus().equals("Normal")
                                    && purchaseTran.getDescription().equals("Purchase")
                                    && Objects.nonNull(purchaseTran.getRidByPmo())) {
                                result.setType(Type.OK);
                                result.setCode("0");
                                result.setRrn(purchaseTran.getRidByPmo());
                                result.setApprovalCode(purchaseTran.getApprovalCode());
                                break;
                            } else {
                                result.setType(Type.Declined);
                                result.setCode("4");
                            }
                        } else {
                            Tran lastTran = tranList.stream()
                                    .sorted((tran1, tran2) -> {
                                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                                        LocalDateTime tran1RegTime = LocalDateTime.parse(tran1.getRegTime(), formatter);
                                        LocalDateTime tran2RegTime = LocalDateTime.parse(tran2.getRegTime(), formatter);

                                        if (tran1RegTime.isAfter(tran2RegTime)) {
                                            return 1;
                                        }
                                        if (tran1RegTime.isBefore(tran2RegTime)) {
                                            return -1;
                                        }
                                        return 0;
                                    }).toList()
                                    .getFirst();

                            if ((lastTran.getDescription().equals("Purchase - Void") || lastTran.getDescription().equals("Refund"))
                                    && lastTran.getBillingStatus().equals("Normal")
                                    && Objects.nonNull(lastTran.getRidByPmo())) {
                                result.setType(Type.Reversed);
                                result.setCode("5");
                                result.setRrn(lastTran.getRidByPmo());
                                if (lastTran.getApprovalCode() != null) {
                                    result.setApprovalCode(lastTran.getApprovalCode());
                                }
                                break;
                            }

                            if (lastTran.getBillingStatus().equals("Normal")
                                    && lastTran.getDescription().equals("Purchase")
                                    && Objects.nonNull(lastTran.getRidByPmo())) {
                                result.setType(Type.OK);
                                result.setCode("0");
                                result.setRrn(lastTran.getRidByPmo());
                                result.setApprovalCode(lastTran.getApprovalCode());
                                break;
                            }

                        }
                    } else {
                        result.setType(Type.Declined);
                        result.setCode("4");
                    }
                }
                case "Rejected" , "Preparing": {
                    result.setType(Type.Declined);
                    result.setCode("4");
                    break;
                }
                case "Expired": {
                    result.setType(Type.Timeout);
                    result.setCode("6");
                    break;
                }
                default: {
                    break;
                }
            }
            return result;
        } catch (Exception e) {
            throw new ClientException(e);
        }

    }

    public Boolean reverseTransaction(String xid,
                                      Double amount,
                                      String login,
                                      String password
    ) {
        String url = String.format(this.url + this.execTranRefundUri, xid);
        try {

            log.info("Exec tran reversal request URL : {}", url);
            Tran tran = new Tran();

            tran.setPhase("Single");
            tran.setAmount(String.valueOf(amount));
            tran.setVoidKind("Full");

            ExecTran execTran = new ExecTran(tran);

            log.info("Exec tran reversal request to TXPG : {}", Utils.obj2String(execTran));

            ExecTranResponse response = httpUtils.sendHttpRequest(
                    url,
                    HttpMethod.POST,
                    this.getHttpEntity(execTran, login, password),
                    ExecTranResponse.class
            );

            log.info("Exec tran refund response : {}", response);

            return true;

        } catch (Exception e) {
            log.warn("Error occurred : {}", e.getMessage());
            return false;
        }
    }

    private <T> HttpEntity<T> getHttpEntity(T object, HttpHeaders headers, String txpgUsername, String txpgPassword) {
        headers.add(HttpHeaders.AUTHORIZATION, Utils.getBasicAuthString(txpgUsername, txpgPassword));
        return new HttpEntity<>(object, headers);
    }

    private <T> HttpEntity<T> getHttpEntity(T object, String txpgUsername, String txpgPassword) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, Utils.getBasicAuthString(txpgUsername, txpgPassword));
        return new HttpEntity<>(object, headers);
    }
}
