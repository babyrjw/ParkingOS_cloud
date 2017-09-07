package com.zld.wxpublic.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pay.Constants;

import com.zld.AjaxUtil;
import com.zld.weixinpay.utils.util.MD5Util;
import com.zld.weixinpay.utils.util.Sha1Util;
import com.zld.weixinpay.utils.util.TenpayUtil;
import com.zld.weixinpay.utils.util.XMLUtil;



public class PayCommonUtil {
	private static Logger log = LoggerFactory.getLogger(PayCommonUtil.class);
	public static String CreateNoncestr(int length) {
		String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		String res = "";
		for (int i = 0; i < length; i++) {
			Random rd = new Random();
			res += chars.indexOf(rd.nextInt(chars.length() - 1));
		}
		return res;
	}

	public static String CreateNoncestr() {
		String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		String res = "";
		for (int i = 0; i < 16; i++) {
			Random rd = new Random();
			res += chars.charAt(rd.nextInt(chars.length() - 1));
		}
		return res;
	}
	/**
	 * @Description��signǩ��
	 * @param characterEncoding �����ʽ
	 * @param parameters �������
	 * @return
	 */
	public static String createSign(String characterEncoding,SortedMap<Object,Object> parameters){
		StringBuffer sb = new StringBuffer();
		Set es = parameters.entrySet();
		Iterator it = es.iterator();
		while(it.hasNext()) {
			Map.Entry entry = (Map.Entry)it.next();
			String k = (String)entry.getKey();
			Object v = entry.getValue();
			if(null != v && !"".equals(v) 
					&& !"sign".equals(k) && !"key".equals(k)) {
				sb.append(k + "=" + v + "&");
			}
		}
		sb.append("key=" + Constants.WXPUBLIC_APPKEY);
		String sign = MD5Util.MD5Encode(sb.toString(), characterEncoding).toUpperCase();
		return sign;
	}
	/**
	 * @Description�����������ת��Ϊxml��ʽ��string
	 * @param parameters  �������
	 * @return
	 */
	public static String getRequestXml(SortedMap<Object,Object> parameters){
		StringBuffer sb = new StringBuffer();
		sb.append("<xml>");
		Set es = parameters.entrySet();
		Iterator it = es.iterator();
		while(it.hasNext()) {
			Map.Entry entry = (Map.Entry)it.next();
			String k = (String)entry.getKey();
			String v = (String)entry.getValue();
			if ("attach".equalsIgnoreCase(k)||"body".equalsIgnoreCase(k)||"sign".equalsIgnoreCase(k)) {
				sb.append("<"+k+">"+"<![CDATA["+v+"]]></"+k+">");
			}else {
				sb.append("<"+k+">"+v+"</"+k+">");
			}
		}
		sb.append("</xml>");
		return sb.toString();
	}
	/**
	 * @Description�����ظ�΢�ŵĲ���
	 * @param return_code ���ر���
	 * @param return_msg  ������Ϣ
	 * @return
	 */
	public static String setXML(String return_code, String return_msg) {
		return "<xml><return_code><![CDATA[" + return_code
				+ "]]></return_code><return_msg><![CDATA[" + return_msg
				+ "]]></return_msg></xml>";
	}
	
	/**
	 * ���ش��������Ƹ�ͨ��������
	 * 
	 * @param msg
	 * Success or fail
	 * @throws IOException
	 */
	public static void sendToCFT(String msg, HttpServletResponse response) throws IOException {
		String strHtml = msg;
		PrintWriter out = response.getWriter();
		out.println(strHtml);
		out.flush();
		out.close();

	}
	
	/*
     * ��ȡJS-SDKʹ��Ȩ��ǩ��
     */
    public static Map<String, String> sign(String jsapi_ticket, String url) {
        Map<String, String> ret = new HashMap<String, String>();
        String nonce_str = create_nonce_str();
        String timestamp = create_timestamp();
        String string1;
        String signature = "";

        //ע���������������ȫ��Сд���ұ�������
        string1 = "jsapi_ticket=" + jsapi_ticket +
                  "&noncestr=" + nonce_str +
                  "&timestamp=" + timestamp +
                  "&url=" + url;
        System.out.println(string1);

        try
        {
            MessageDigest crypt = MessageDigest.getInstance("SHA-1");
            crypt.reset();
            crypt.update(string1.getBytes("UTF-8"));
            signature = byteToHex(crypt.digest());
        }
        catch (NoSuchAlgorithmException e)
        {
            e.printStackTrace();
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
        }
        
        ret.put("url", url);
        ret.put("jsapi_ticket", jsapi_ticket);
        ret.put("nonceStr", nonce_str);
        ret.put("timestamp", timestamp);
        ret.put("signature", signature);

        return ret;
    }

    private static String byteToHex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash)
        {
            formatter.format("%02x", b);
        }
        String result = formatter.toString();
        formatter.close();
        return result;
    }

    private static String create_nonce_str() {
        return UUID.randomUUID().toString();
    }

    private static String create_timestamp() {
        return Long.toString(System.currentTimeMillis() / 1000);
    }
    
    /*
	 * @Description����ȡJSAPI֧������
	 * @param addressip �������ɵĻ���IP
	 * @param fee ���
	 * @param body ��Ʒ����
	 * @param attach ��������
	 * @param openid �û���ʶ
	 */
	public static SortedMap<Object, Object> getPayParams(String addressip,
			Double fee, String body, String attach, String openid)
			throws Exception {
		// ��ǰʱ�� yyyyMMddHHmmss
		String currTime = TenpayUtil.getCurrTime();
		// 8λ����
		String strTime = currTime.substring(8, currTime.length());
		// ��λ�����
		String strRandom = TenpayUtil.buildRandom(4) + "";
		// 10λ���к�,�������е�����
		String strReq = strTime + strRandom;
		// �����ţ��˴���ʱ�����������ɣ��̻������Լ����������ֻҪ����ȫ��Ψһ����
		String out_trade_no = strReq;
		// ����package��������
		SortedMap<Object, Object> packageParams = new TreeMap<Object, Object>();
		packageParams.put("appid", Constants.WXPUBLIC_APPID);
		packageParams.put("mch_id", Constants.WXPUBLIC_MCH_ID); // �����̻���
		packageParams.put("nonce_str", PayCommonUtil.CreateNoncestr());
		body = AjaxUtil.decodeUTF8(body);
		packageParams.put("body", body); // ��Ʒ����
		packageParams.put("out_trade_no", out_trade_no); // �̻�������
		packageParams.put("total_fee", String.valueOf(Integer.parseInt(new java.text.DecimalFormat("0").format(fee*100)))); // ��Ʒ�ܽ��,�Է�Ϊ��λ
		packageParams.put("spbill_create_ip",addressip);
		packageParams.put("notify_url",Constants.WXPUBLIC_NOTIFY_URL); // ����֪ͨ��ַ
		packageParams.put("trade_type", "JSAPI");
		packageParams.put("openid", openid);
		
		packageParams.put("attach", attach);
		String sign = PayCommonUtil.createSign("UTF-8", packageParams);
		packageParams.put("sign", sign);
		
		String requestXML = PayCommonUtil.getRequestXml(packageParams);
		String result =CommonUtil.httpsRequest(Constants.WXPUBLIC_UNIFIEDORDER, "POST", requestXML);

		Map<String, String> map = XMLUtil.doXMLParse(result);//����΢�ŷ��ص���Ϣ����Map��ʽ�洢����ȡֵ
		SortedMap<Object,Object> params = new TreeMap<Object,Object>();
		String timestamp = Sha1Util.getTimeStamp();
		params.put("appId", Constants.WXPUBLIC_APPID);
        params.put("timeStamp", timestamp);
        params.put("nonceStr", PayCommonUtil.CreateNoncestr());
        params.put("package", "prepay_id="+map.get("prepay_id"));
        params.put("signType", "MD5");
        String paySign = PayCommonUtil.createSign("UTF-8", params);
        params.put("packageValue", "prepay_id="+map.get("prepay_id"));//������packageValue��Ԥ��package�ǹؼ�����js��ȡֵ����
        params.put("paySign", paySign);//paySign�����ɹ����Sign�����ɹ���һ��
		return params;
	}
	
	/*
	 * @Description������ģ����Ϣ
	 * @param msg ��Ϣ
	 * @param accesstoken
	 */
	public static void sendMessage(String msg, String accesstoken){
		String sendUrl = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + accesstoken;
		String result = CommonUtil.httpsRequest(sendUrl, "POST", msg);
	}
}
