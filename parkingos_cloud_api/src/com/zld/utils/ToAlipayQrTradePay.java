/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2014 All Rights Reserved.
 */
package com.zld.utils;

import java.text.SimpleDateFormat;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import pay.AlipayAPIClientFactory;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePrecreateRequest;
import com.alipay.api.response.AlipayTradePrecreateResponse;

@Component
public class ToAlipayQrTradePay {
	Logger logger = Logger.getLogger(ToAlipayQrTradePay.class);
	/**
	 * 
	 * @param args
	 */
	/*public static void main(String[] args) {
		//201504210011041195
		String out_trade_no="20150528207426"; //�̻�Ψһ������
		String total_amount="0.01";
		String subject = "����ɨ�븶����";
		qrPay(out_trade_no,total_amount,subject,"֧������",1029L);
	}
	*/
	
	/**
	 *  ��ά���µ�֧��
	 * @param out_trade_no
	 * @param auth_code
	 * @author jinlong.rhj
	 * @date 2015��4��28��
	 * @version 1.0
	 * @return 
	 */
	public String qrPay(String out_trade_no,String total_amount,
			String title,String body,Long uid) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String time_expire= sdf.format(System.currentTimeMillis()+3600*1000);
		
		StringBuilder sb = new StringBuilder();
		sb.append("{\"out_trade_no\":\"" + out_trade_no + "\",");
		sb.append("\"total_amount\":\""+total_amount+"\",\"discountable_amount\":\"0.00\",");
		sb.append("\"subject\":\""+title+"\",\"body\":\""+body+"\",");
		//sb.append("\"goods_detail\":[{\"goods_id\":\"apple-01\",\"goods_name\":\"ipad\",\"goods_category\":\"7788230\",\"price\":\"88.00\",\"quantity\":\"1\"}],");
		sb.append("\"goods_detail\":[],");
		sb.append("\"operator_id\":\""+uid+"\",\"store_id\":\"zld1117\",\"terminal_id\":\"t_001\",");
		sb.append("\"time_expire\":\""+time_expire+"\"}");
		logger.error("֧����֧����ά�����������"+sb.toString());

		AlipayClient alipayClient = AlipayAPIClientFactory.getAlipayClient();

		// ʹ��SDK������Ⱥ������ģ��
		AlipayTradePrecreateRequest request = new AlipayTradePrecreateRequest();
		request.setBizContent(sb.toString());
		//�ص���ַ������
		request.setNotifyUrl("http://yxiudongyeahnet.vicp.cc/zld/rechage");
		//�ص���ַ����ʽ
		//request.setNotifyUrl("http://s.tingchebao.com/zld/rechage");
//		request.putOtherTextParam("ws_service_url", "http://unitradeprod.t15032aqcn.alipay.net:8080");
		String qr = "";
		try {
			AlipayTradePrecreateResponse response = null;
			// ʹ��SDK�����ý����µ��ӿ�
			response = alipayClient.execute(request);
			if(response!=null){
				logger.error("֧����֧����ά�뷵���Ƿ�ɹ���"+response.isSuccess());
				logger.error("֧����֧����ά�뷵�أ�"+response.getMsg());
				logger.error("֧����֧����ά�뷵�ؽ����"+response.getBody());
				if( response.isSuccess()){
					qr=response.getQrCode();
				}else {
					logger.error("�����룺"+response.getSubCode());
					logger.error("����������"+response.getSubMsg());
				}
			}else {
				logger.error("֧����֧����ά�뷵�ش���");
			}
		} catch (AlipayApiException e) {
			e.printStackTrace();
		}
		return qr;
	}
	


}
