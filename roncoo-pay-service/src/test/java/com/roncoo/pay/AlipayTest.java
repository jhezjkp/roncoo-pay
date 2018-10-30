package com.roncoo.pay;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.domain.AlipayTradeQueryModel;
import com.alipay.api.request.AlipayTradeQueryRequest;
import com.alipay.api.response.AlipayTradeQueryResponse;
import com.roncoo.pay.trade.utils.alipay.config.AlipayConfigUtil;
import org.junit.Test;

import static org.junit.Assert.*;

public class AlipayTest {

    @Test
    public void testTradeQuery() {
        // 一例成功
        String outTradeNo = "66662018102910000053";
        AlipayClient alipayClient = new DefaultAlipayClient(AlipayConfigUtil.gateway, AlipayConfigUtil.app_id, AlipayConfigUtil.mch_private_key, "json", AlipayConfigUtil.charset, AlipayConfigUtil.ali_public_key, AlipayConfigUtil.sign_type);
        AlipayTradeQueryRequest request = new AlipayTradeQueryRequest();
        AlipayTradeQueryModel model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
//        model.setTradeNo(resultMap.get("trade_no"));
        request.setBizModel(model);
        AlipayTradeQueryResponse response = null;
        try {
            response = alipayClient.execute(request);
            System.out.println(response.getBody());
            assertTrue(response.isSuccess());
            assertEquals(outTradeNo, response.getOutTradeNo());
        } catch (AlipayApiException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        // 一例失败(未支付)
        outTradeNo = "66662018102810000014";
        request = new AlipayTradeQueryRequest();
        model = new AlipayTradeQueryModel();
        model.setOutTradeNo(outTradeNo);
//        model.setTradeNo(resultMap.get("trade_no"));
        request.setBizModel(model);
        try {
            response = alipayClient.execute(request);
            System.out.println(response.getBody());
            assertFalse(response.isSuccess());
            assertEquals(outTradeNo, response.getOutTradeNo());
        } catch (AlipayApiException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }
}
