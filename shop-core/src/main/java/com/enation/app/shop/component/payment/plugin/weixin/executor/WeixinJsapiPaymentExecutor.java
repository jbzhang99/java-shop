package com.enation.app.shop.component.payment.plugin.weixin.executor;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.enation.app.shop.component.payment.plugin.weixin.WeixinPayPlugin;
import com.enation.app.shop.component.payment.plugin.weixin.WeixinPuginConfig;
import com.enation.app.shop.component.payment.plugin.weixin.WeixinUtil;
import com.enation.app.shop.payment.model.vo.PayBill;
import com.enation.app.shop.trade.model.enums.TradeType;
import com.enation.eop.processor.core.freemarker.FreeMarkerPaser;
import com.enation.framework.cache.ICache;
import com.enation.framework.context.webcontext.ThreadContextHolder;
import com.enation.framework.util.DateUtil;
import com.enation.framework.util.StringUtil;
import com.enation.framework.validator.ErrorCode;
import com.enation.framework.validator.ResourceNotFoundException;

/**
 * 微信公众号支付
 * @author fk
 * @version v6.4
 * @since v6.4
 * 2017年10月17日 上午10:52:08
 */
@Service
public class WeixinJsapiPaymentExecutor extends WeixinPuginConfig{

	@Autowired
	private ICache cache;
	
	/**
	 * 支付
	 * @param bill
	 * @return
	 */
	public String onPay(PayBill bill) {
		
		Map<String, String> cfgparams = this.getConfig();
		if (cfgparams == null) {
			throw new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND,"请设置微信支付商户参数!");
		}
		String mchid = cfgparams.get("mchid");// cfgparams.get("mchid");
		String appid = cfgparams.get("appid");// cfgparams.get("appid");
		String key = cfgparams.get("key"); // cfgparams.get("key");

		String original_sn = bill.getTrade_sn();
		String body = "网店订单[" + original_sn + "]";

		HttpServletRequest request = ThreadContextHolder.getHttpRequest();
		Map<String,String> params = new TreeMap();
		params.put("appid", appid);
		params.put("mch_id", mchid);
		params.put("nonce_str", StringUtil.getRandStr(10));
		params.put("body", body);
		params.put("out_trade_no", original_sn);

		// 应付转为分
		Double money = bill.getOrder_price();
		params.put("total_fee", toFen(money));
		params.put("spbill_create_ip", request.getRemoteAddr());
		params.put("notify_url",this.getCallBackUrl(bill.getTradeType()));
		params.put("trade_type", "JSAPI");
		String openid = (String) cache.get(WeixinPayPlugin.OPENID_SESSION_KEY+request.getSession().getId());
		params.put("openid", openid);
		String sign = WeixinUtil.createSign(params, key);
		params.put("sign", sign);
		
		String result = "";
		try {
			String xml = WeixinUtil.mapToXml(params);
			Document resultDoc = WeixinUtil.post("https://api.mch.weixin.qq.com/pay/unifiedorder", xml);
			Element rootEl = resultDoc.getRootElement();

			// 返回结果
			String return_code = rootEl.element("return_code").getText(); // 返回码
			if ("SUCCESS".equals(return_code)) {

				String result_code = rootEl.element("result_code").getText(); // 业务码

				if ("SUCCESS".equals(result_code)) {
					String prepay_id = rootEl.element("prepay_id").getText(); // 预支付订单id
					
					result = this.getPayScript(prepay_id, appid, key,original_sn,bill.getTradeType());

				} else {
					
					String err_code = rootEl.element("err_code").getText();
					String err_code_des = rootEl.element("err_code_des").getText();
					result = "<script>alert('支付意外错误，请联系技术人员:"
							+ err_code + "【"+err_code_des+"】')</script>";
				}
			} else {
				result = "<script>alert('支付意外错误，请联系技术人员:" + return_code + "')</script>";
				if ("FAIL".equals(return_code)) {
					String return_msg = resultDoc.getRootElement()
							.element("return_msg").getText(); // 错误信息
					this.logger.error("微信端返回错误" + return_code + "["
							+ return_msg + "]");
					this.logger.debug("-----------post xml-----------");
					this.logger.debug(xml);
				}
			}
		} catch (Exception e) {
			this.logger.error("微信生成支付二维码错误", e);
			return "二维码生成错误";
		}

		return result;
	}
	
	/**
	 * 生成支付的脚本
	 * 
	 * @param prepay_id
	 *            预支付订单id
	 * @return
	 */
	private String getPayScript(String prepay_id, String appid, String weixinkey,String original_sn,TradeType tradeType) {

		Map<String, String> params = new TreeMap();
		params.put("appId", appid);
		params.put("nonceStr", StringUtil.getRandStr(10));
		params.put("timeStamp", String.valueOf(DateUtil.getDateline()));
		params.put("package", "prepay_id=" + prepay_id);
		params.put("signType", "MD5");
		String sign = WeixinUtil.createSign(params, weixinkey);
		params.put("paySign", sign);

		FreeMarkerPaser fp = FreeMarkerPaser.getInstance();
		fp.setClz(this.getClass());
		fp.setPageName("payscript");

		StringBuffer payStr = new StringBuffer();
		payStr.append("WeixinJSBridge.invoke('getBrandWCPayRequest',{");

		int i = 0;
		for (String key : params.keySet()) {
			String value = params.get(key);

			if (i != 0)
				payStr.append(",");
			payStr.append("'" + key + "':'" + value + "'");
			i++;

		}

		payStr.append("}");
		
		payStr.append(",function(res){  if( 'get_brand_wcpay_request:ok'==res.err_msg ) { "
				+ "alert('支付成功'); "
				+ "location.href='"+getPay_wap_success_url(tradeType.name(),original_sn)+"?operation=success';"
				+ "}else{ alert('支付失败'); "
				+ "location.href='"+getPay_wap_success_url(tradeType.name(),original_sn)+"?operation=fail';"
				+ "} "
				+ "}");

		payStr.append(");");
		fp.putData("payStr", payStr);

		return fp.proessPageContent();
	}
	
	private String httpget(String uri) {
		HttpClient httpclient = new DefaultHttpClient();
		HttpGet httpget = new HttpGet(uri);
		HttpResponse response;
		try {
			response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			String content = EntityUtils.toString(entity, "utf-8");
		 
			return content;
		} catch (ClientProtocolException e) {
			e.printStackTrace();
			
		} catch (IOException e) {
			e.printStackTrace();
			
		}
		return "error";
	}
	
}
