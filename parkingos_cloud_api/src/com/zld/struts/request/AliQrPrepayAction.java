package com.zld.struts.request;

import java.io.IOException;
import java.util.Map;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.zld.AjaxUtil;
import com.zld.impl.PublicMethods;
import com.zld.service.DataBaseService;
import com.zld.utils.RequestUtil;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
import com.zld.utils.ToAlipayQrTradePay;

/**
 * ֧����ɨ��Ԥ����������
 * ɨ��-->��������ѯ�������-->����֧����֧��   
 * �ص������-->����Ԥ����Ϣ-->֪ͨSDKԤ�� -->SDKȷ��-->�����������
 * @author Laoyao 20170520
 *
 */
public class AliQrPrepayAction extends Action {
	@Autowired
	private PublicMethods publicMethods;
	@Autowired
	private DataBaseService service;
	@Autowired
	private ToAlipayQrTradePay toAlipayQrTradePay;
	private Logger logger = Logger.getLogger(AliQrPrepayAction.class);
	String log = "aliprepay ";
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String action = RequestUtil.processParams(request, "action");
		String target = null;
		if (action.equals("prepay")) {//����֧����֧��
			target = prepay(request, response);
		}else if (action.equals("sweepcom")) {//֧����ɨ��󣬽����ѯ��������
			target = sweepCom(request, response);
		}else if (action.equals("editcarnumber")) {//��������
			AjaxUtil.ajaxOutput(response, editCarnumber(request, response));
		}
		return mapping.findForward(target);
	}
	/**
	 * ����֧����֧��
	 * @param request
	 * @param response
	 * @return
	 */
	private String prepay (HttpServletRequest request,HttpServletResponse response){
		Long borderId = RequestUtil.getLong(request, "bid", -1L);//bolinkorder id
		String orderId = RequestUtil.getString(request, "orderid");//�������������
		Long unionId = RequestUtil.getLong(request, "unionid", -1L);
		Long uin = RequestUtil.getLong(request, "uin",-1L);
		String parkId = RequestUtil.getString(request, "parkid");
		String carNumber = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "carnumber"));
		logger.error(log+"params:unionid="+unionId+",parkid="+parkId+",orderid="+orderId+",uin="+uin);
		Map<String, Object> orderMap  = publicMethods.catBolinkOrder(borderId, orderId, carNumber,parkId, 0,uin);
		logger.error("orderMap:" + orderMap);
		if (orderMap != null) {
			Double total =StringUtils.formatDouble(orderMap.get("money"));
			if (total > 0) {//����֧��������֧��"http://192.168.199.170/zld/help.jsp";//
				String qr = toAlipayQrTradePay.qrPay(TimeTools.getTimeYYYYMMDDHHMMSS()+orderId, total+"", "Ԥ��ͣ����", uin+"_8_"+orderId, 0L);
				try {
					response.sendRedirect(qr);
					return null;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return "error";
	}
	/**
	 * ֧����ɨ��󣬽����ѯ��������
	 * @param request
	 * @param response
	 * @return
	 */
	private String sweepCom(HttpServletRequest request,HttpServletResponse response){
		logger.error(log+"begin");
		String parkId = RequestUtil.getString(request, "parkid");
		Long unionId = RequestUtil.getLong(request, "unionid", -1L);
		String carNumber = RequestUtil.getString(request, "carnumber");
		request.setAttribute("unionid", unionId);
		request.setAttribute("parkid", parkId);
		request.setAttribute("carnumber", carNumber);
		logger.error(log+"params:unionid="+unionId+",parkid="+parkId+",carNumber="+carNumber);
		if("".equals(carNumber)){//���Ǵ���ӳ���ҳ������ģ��ȴ�cookie��ȡ
			Cookie[] cookies = request.getCookies();//��������Ի�ȡһ��cookie����
			if(cookies!=null){
				for(Cookie cookie : cookies){
					if(cookie!=null&&cookie.getName().equals("lience"))
						carNumber = AjaxUtil.decodeUTF8(cookie.getValue());
				}
			}
		}
		logger.error(log+carNumber);
		if(carNumber==null||"".equals(carNumber)){
			logger.error(log+"�����복��ҳ��");
			return "toaddcnum";
		}else {//���泵�Ƶ�cookie
			Cookie  cookie = new Cookie("lience",AjaxUtil.encodeUTF8(carNumber));
			cookie.setMaxAge(8640000);//�ݶ�100��
			//����·�������·�����ù����¶����Է��ʸ�cookie ���������·������ôֻ�����ø�cookie·��������·�����Է���
			cookie.setPath("/");
			response.addCookie(cookie);
			logger.error(log+"�ѱ��浽cookie,lience="+carNumber);
		}
		Long uin = getUinByCar(carNumber);
		Map<String, Object> orderMap =  publicMethods.catBolinkOrder(null, null, carNumber,parkId, 0, uin);
		request.setAttribute("noorder", 1);
		if (orderMap!=null&&!orderMap.isEmpty()) {
			Long startTime = Long.valueOf(orderMap.get("start_time") + "");
			request.setAttribute("starttime",TimeTools.getTime_MMdd_HHmm(startTime * 1000));
			request.setAttribute("parktime",StringUtils.getTimeString(startTime,System.currentTimeMillis()/1000));
			request.setAttribute("total", orderMap.get("money"));
			request.setAttribute("noorder", 0);
		}
		request.setAttribute("carnumber", carNumber);
		request.setAttribute("uin", uin);
		request.setAttribute("orderid", orderMap.get("id"));//bolinkorder id
		return "prepay";
	}
	
	/**
	 * ���ݳ��Ʋ�ѯ�����˻�
	 * @param carNumber
	 * @return
	 */
	private Long getUinByCar(String carNumber) {
		Map<String, Object> carInfo = service.getMap("select uin from wxp_user_tb where car_number=? ", new Object[]{carNumber});
		Long uin = -1L;
		if(carInfo!=null&&!carInfo.isEmpty()){
			uin = (Long) carInfo.get("uin");
		}
		if(uin==null||uin<0){
			uin = service.getkey("seq_wxp_user_tb");
			int r = service.update("insert into wxp_user_tb (create_time,uin,car_number,openid) values" +
					"(?,?,?,?) ", new Object[]{System.currentTimeMillis()/1000,uin,carNumber,"aliprepayuser"});
			logger.error("aliprepay add user,carnumber:"+carNumber+" ,r="+r);
		}
		return uin;
	}
	/**
	 * ��������
	 * @param request
	 * @param response
	 * @return
	 */
	private String editCarnumber(HttpServletRequest request,HttpServletResponse response){
		String carnumber = AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "carnumber"));
		Cookie  cookie = new Cookie("lience",AjaxUtil.encodeUTF8(carnumber));
		cookie.setMaxAge(8640000);//�ݶ�100��
		//����·�������·�����ù����¶����Է��ʸ�cookie ���������·������ôֻ�����ø�cookie·��������·�����Է���
		cookie.setPath("/");
		response.addCookie(cookie);
		logger.error(log+"�ѱ��浽cookie,lience="+carnumber);
		return "{\"state\":1,\"errmsg\":\"\"}";
	}

}
