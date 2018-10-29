package com.roncoo.pay.trade.utils.alipay;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.roncoo.pay.common.core.utils.StringUtil;
import com.roncoo.pay.reconciliation.utils.alipay.httpClient.HttpProtocolHandler;
import com.roncoo.pay.reconciliation.utils.alipay.httpClient.HttpRequest;
import com.roncoo.pay.reconciliation.utils.alipay.httpClient.HttpResponse;
import com.roncoo.pay.reconciliation.utils.alipay.httpClient.HttpResultType;
import com.roncoo.pay.trade.entity.RoncooPayGoodsDetails;
import com.roncoo.pay.trade.utils.alipay.config.AlipayConfigUtil;
import com.roncoo.pay.trade.utils.alipay.sign.MD5;
import org.apache.commons.httpclient.NameValuePair;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.*;

public class AliPayUtil {

    private static final Logger logger = LoggerFactory.getLogger(AliPayUtil.class);

    private AliPayUtil() {

    }

    /**
     * 支付宝被扫(扫码设备)
     *
     * @param outTradeNo
     * @param authCode
     * @param subject
     * @param amount
     * @param body
     * @param roncooPayGoodsDetailses
     * @return
     */
    public static Map<String, Object> tradePay(String outTradeNo, String authCode, String subject, BigDecimal amount, String body, List<RoncooPayGoodsDetails> roncooPayGoodsDetailses) {
        logger.info("======>支付宝被扫");
        String charset = "UTF-8";
        String format = "json";
        String signType = "RSA2";
        String scene = "bar_code";//支付场景--条码支付
        String totalAmount = amount.toString();//订单金额
        String discountableAmount = "0.0";//默认折扣金额为0,建议由业务系统记录折扣金额,值传递给支付宝实际支付金额
        String storeId = "ykt_pay_store_id"; // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String timeExpress = "5m";// 支付超时，线下扫码交易定义为5分钟

        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfigUtil.trade_pay_url, AlipayConfigUtil.app_id, AlipayConfigUtil.mch_private_key, format, charset, AlipayConfigUtil.ali_public_key, signType);

        SortedMap<String, Object> paramMap = new TreeMap<>();
        paramMap.put("out_trade_no", outTradeNo);
        paramMap.put("scene", scene);
        paramMap.put("auth_code", authCode);
        paramMap.put("subject", subject);
        paramMap.put("total_amount", totalAmount);
        paramMap.put("discountable_amount", discountableAmount);
        paramMap.put("body", body);
        paramMap.put("store_id", storeId);
        paramMap.put("timeout_express", timeExpress);

        // 商品明细列表，需填写购买商品详细信息，
        if (roncooPayGoodsDetailses != null && roncooPayGoodsDetailses.size() > 0) {
            List<SortedMap<String, Object>> goodsList = new ArrayList<>();
            for (RoncooPayGoodsDetails roncooPayGoodsDetails : roncooPayGoodsDetailses) {
                SortedMap<String, Object> goodsMap = new TreeMap<>();
                goodsMap.put("goods_id", roncooPayGoodsDetails.getGoodsId());
                goodsMap.put("goods_name", roncooPayGoodsDetails.getGoodsName());
                goodsMap.put("quantity", roncooPayGoodsDetails.getNums());
                goodsMap.put("price", roncooPayGoodsDetails.getSinglePrice());
                goodsList.add(goodsMap);
            }
            paramMap.put("goods_detail", goodsList);
        }

        SortedMap<String, Object> extendParamsMap = new TreeMap<>();
        extendParamsMap.put("sys_service_provider_id", AlipayConfigUtil.partner);
        paramMap.put("extend_params", extendParamsMap);

        AlipayTradePayRequest request = new AlipayTradePayRequest();
        System.out.println(JSONObject.toJSONString(paramMap));
        request.setBizContent(JSONObject.toJSONString(paramMap));
        try {
            AlipayTradePayResponse response = alipayClient.execute(request);
            JSONObject responseJSON = JSONObject.parseObject(JSONObject.toJSONString(response));
            logger.info("支付宝返回结果:{}", responseJSON);
            return responseJSON;
        } catch (AlipayApiException e) {
            logger.error("支付宝扫码，支付异常:{}", e);
            JSONObject resultJSON = new JSONObject();
            resultJSON.put("outTradeNo", outTradeNo);
            resultJSON.put("totalAmount", amount);
            resultJSON.put("errorCode", "9999");
            return resultJSON;
        }
    }

    public static AlipayTradeQueryResponse tradeQuery(String outTradeNo) {
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfigUtil.gateway, AlipayConfigUtil.app_id, AlipayConfigUtil.mch_private_key, "json", AlipayConfigUtil.charset, AlipayConfigUtil.ali_public_key, AlipayConfigUtil.sign_type);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
//        model.setTradeNo(resultMap.get("trade_no"));
        request.setBizModel(model);
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
            return response;
        } catch (AlipayApiException e) {
            logger.error("支付宝收单线下交易查询发生异常", e);
        }
        return null;
    }


    /**
     * MAP类型数组转换成NameValuePair类型
     *
     * @param properties MAP类型数组
     * @return NameValuePair类型数组
     */
    private static NameValuePair[] generatNameValuePair(SortedMap<String, String> properties) {
        NameValuePair[] nameValuePair = new NameValuePair[properties.size()];
        int i = 0;
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            nameValuePair[i++] = new NameValuePair(entry.getKey(), entry.getValue());
        }

        return nameValuePair;
    }

    private static String getSign(SortedMap<String, String> paramMap, String key) {
        StringBuilder signBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : paramMap.entrySet()) {
            if (!"sign".equals(entry.getKey()) && !"sign_type".equals(entry.getKey()) && !StringUtil.isEmpty(entry.getValue())) {
                signBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
            }
        }
        return MD5.sign(signBuilder.substring(0, signBuilder.length() - 1), key, "UTF-8");
    }
}
