package com.roncoo.pay.trade.utils.alipay;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradePayModel;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.domain.ExtendParams;
import com.alipay.api.domain.GoodsDetail;
import com.alipay.api.internal.util.AlipaySignature;
import com.alipay.api.request.AlipayTradePayRequest;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradePayResponse;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.roncoo.pay.common.core.utils.StringUtil;
import com.roncoo.pay.trade.entity.RoncooPayGoodsDetails;
import com.roncoo.pay.trade.utils.alipay.config.AlipayConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 支付宝工具类
 */
public class AliPayUtil {

    private static final Logger logger = LoggerFactory.getLogger(AliPayUtil.class);

    private AliPayUtil() {

    }

    /**
     * 支付宝被扫(扫码设备)->统一收音交易支付接口: https://docs.open.alipay.com/api_1/alipay.trade.pay/
     *
     * @param outTradeNo
     * @param authCode
     * @param subject
     * @param amount
     * @param body
     * @param roncooPayGoodsDetailses
     * @return
     */
    public static AlipayTradePayResponse tradePay(String outTradeNo, String authCode, String subject, BigDecimal amount, String body, List<RoncooPayGoodsDetails> roncooPayGoodsDetailses) {
        logger.info("======>支付宝被扫");
        String charset = "UTF-8";
        String scene = "bar_code";//支付场景--条码支付
        String totalAmount = amount.toString();//订单金额
        String discountableAmount = "0.0";//默认折扣金额为0,建议由业务系统记录折扣金额,值传递给支付宝实际支付金额
        String storeId = "ykt_pay_store_id"; // (必填) 商户门店编号，通过门店号和商家后台可以配置精准到门店的折扣信息，详询支付宝技术支持
        String timeExpress = "5m";// 支付超时，线下扫码交易定义为5分钟
        String operatorId = ""; //  商户操作员编号
        String terminalId = ""; // 终端编号

        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfigUtil.trade_pay_url, AlipayConfigUtil.app_id, AlipayConfigUtil.mch_private_key, AlipayConfigUtil.format, charset, AlipayConfigUtil.ali_public_key, AlipayConfigUtil.sign_type);


        // 商品明细列表，需填写购买商品详细信息，
        List<GoodsDetail> details = new ArrayList<>();
        if (roncooPayGoodsDetailses != null && roncooPayGoodsDetailses.size() > 0) {
            GoodsDetail detail = new GoodsDetail();
            for (RoncooPayGoodsDetails roncooPayGoodsDetails : roncooPayGoodsDetailses) {
                detail.setAlipayGoodsId(roncooPayGoodsDetails.getGoodsId());
                detail.setGoodsName(roncooPayGoodsDetails.getGoodsName());
                detail.setPrice(BigDecimal.valueOf(roncooPayGoodsDetails.getSinglePrice()).toString());
                detail.setQuantity(new Long(roncooPayGoodsDetails.getNums()));

                details.add(detail);
            }
        }


        ExtendParams extendParams = new ExtendParams();
        extendParams.setSysServiceProviderId(AlipayConfigUtil.seller_id); // 系统供应商直接为平台收单账号

        AlipayTradePayRequest request = new AlipayTradePayRequest();
        AlipayTradePayModel model = new AlipayTradePayModel();
        model.setOutTradeNo(outTradeNo);
        model.setSubject(subject);
        model.setBody(body);
        model.setTimeoutExpress(timeExpress);
        model.setTotalAmount(totalAmount);
        model.setScene(scene);
        model.setAuthCode(authCode);    // 授权码
        model.setDiscountableAmount(discountableAmount);
        model.setStoreId(storeId);
        model.setOperatorId(operatorId);
        model.setTerminalId(terminalId);
        model.setExtendParams(extendParams);
        model.setGoodsDetail(details);


        System.out.println(JSONObject.toJSONString(model));
        request.setBizModel(model);
        try {
            AlipayTradePayResponse response = alipayClient.execute(request);
            JSONObject responseJSON = JSONObject.parseObject(JSONObject.toJSONString(response));
            logger.info("支付宝返回结果:{}", JSON.toJSON(response));
            return response;
        } catch (AlipayApiException e) {
            logger.error("支付宝扫码，支付异常", e);
//            JSONObject resultJSON = new JSONObject();
//            resultJSON.put("outTradeNo", outTradeNo);
//            resultJSON.put("totalAmount", amount);
//            resultJSON.put("errorCode", "9999");
            return null;
        }
    }

    /**
     * 根据商户订单号查询订单情况
     *
     * @param outTradeNo
     * @return
     */
    public static AlipayTradeQueryResponse tradeQuery(String outTradeNo) {
        return tradeQuery(outTradeNo, null);
    }

    /**
     * 根据商户订单号、支付宝方订单号查询订单情况(其实支付宝在商户订单号和自己的订单号都存在的情况下是优先根据自己的订单号去查的)
     *
     * @param outTradeNo
     * @param tradeNo
     * @return
     */
    public static AlipayTradeQueryResponse tradeQuery(String outTradeNo, String tradeNo) {
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfigUtil.gateway, AlipayConfigUtil.app_id, AlipayConfigUtil.mch_private_key, "json", AlipayConfigUtil.charset, AlipayConfigUtil.ali_public_key, AlipayConfigUtil.sign_type);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
        if (!StringUtil.isEmpty(tradeNo)) {
            model.setTradeNo(tradeNo);
        }
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
     * 签名检查
     *
     * @param resultMap
     * @return
     */
    public static boolean checkSign(Map<String, String> resultMap) {
        try {
            return AlipaySignature.rsaCheckV1(resultMap, AlipayConfigUtil.ali_public_key, AlipayConfigUtil.charset, AlipayConfigUtil.sign_type);
        } catch (AlipayApiException e) {
            logger.error("签名检查发生异常", e);
        }
        return false;
    }


}
