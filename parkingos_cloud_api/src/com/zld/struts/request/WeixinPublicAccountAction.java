package com.zld.struts.request;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import oracle.jdbc.driver.UpdatableResultSet;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;

import pay.Constants;
import pay.PayConfigDefind;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.util.Hash;
import com.zld.AjaxUtil;
import com.zld.CustomDefind;
import com.zld.impl.CommonMethods;
import com.zld.impl.MemcacheUtils;
import com.zld.impl.MongoClientFactory;
import com.zld.impl.PublicMethods;
import com.zld.service.DataBaseService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.HttpProxy;
import com.zld.utils.HttpsProxy;
import com.zld.utils.RequestUtil;
import com.zld.utils.SendMessage;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
import com.zld.wxpublic.util.CommonUtil;
import com.zld.wxpublic.util.PayCommonUtil;

public class WeixinPublicAccountAction extends Action {
	@Autowired
	private DataBaseService daService;
	
	@Autowired
	private PgOnlyReadService pService;
	
	@Autowired
	private PublicMethods publicMethods;
	
	@Autowired
	private CommonMethods commonMethods;
	
	@Autowired
	private MemcacheUtils memcacheUtils;
	
	private Logger logger = Logger.getLogger(WeixinPublicAccountAction.class);
	/**
	 * weixin
	 */
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String action = RequestUtil.processParams(request, "action");
		if(action.equals("")){
			String forward = RequestUtil.processParams(request, "forward");
			String code = RequestUtil.processParams(request, "code");
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>����΢���ҵ��˻�>>>>>>>>>>>>>>>,code:"+code+" forward:"+forward);
			String access_token_url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid="+Constants.WXPUBLIC_APPID+"&secret="+Constants.WXPUBLIC_SECRET+"&code="+code+"&grant_type=authorization_code";
			String result = CommonUtil.httpsRequest(access_token_url, "GET", null);
			JSONObject map = null;
			JSONObject wxuserinfo = null;
			if(result!=null){
				map = JSONObject.fromObject(result);
			}
			if(map ==null || map.get("errcode") != null){
				System.out.println("��ȡopenidʧ��....");
				String redirect_url = "http%3a%2f%2f"+Constants.WXPUBLIC_REDIRECTURL+"%2fzld%2fwxpaccount.do";
				String url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="
						+ Constants.WXPUBLIC_APPID
						+ "&redirect_uri="
						+ redirect_url
						+ "&response_type=code&scope=snsapi_userinfo&state=123#wechat_redirect";
				response.sendRedirect(url);
				return null;
			}
			String openid = (String)map.get("openid");
			String access_token = (String)map.get("access_token");
			String scope = (String)map.get("scope");
			String wxname = "";
			if(scope.equals("snsapi_userinfo")){
				String userinfo_url = "https://api.weixin.qq.com/sns/userinfo?access_token="+access_token+"&openid="+openid+"&lang=zh_CN";
				result = CommonUtil.httpsRequest(userinfo_url, "GET", null);
				if(result != null){
					wxuserinfo = JSONObject.fromObject(result);
				}
				if(wxuserinfo ==null || wxuserinfo.get("errcode") != null){
					System.out.println("��ȡopenidʧ��....");
					String redirect_url = "http%3a%2f%2f"+Constants.WXPUBLIC_REDIRECTURL+"%2fzld%2fwxpaccount.do";
					String url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="
							+ Constants.WXPUBLIC_APPID
							+ "&redirect_uri="
							+ redirect_url
							+ "&response_type=code&scope=snsapi_userinfo&state=123#wechat_redirect";
					response.sendRedirect(url);
					return null;
				}
				wxname = (String)wxuserinfo.get("nickname");
				String wximg = (String)wxuserinfo.get("headimgurl");
				request.setAttribute("wximg", wximg);
				request.setAttribute("wxname", wxname);
			}
			//��������
			
//			String openid = "oRoektybTsv33_vSKKUwLAsJAquc";
//			openid = "oRoekt7uy9abm5hrUBCWYHHDF5sY";
			
			request.setAttribute("openid", openid);
			Map<String, Object> userMap = daService
					.getMap("select * from user_info_tb where auth_flag=? and wxp_openid=? and state=? ",
							new Object[] {4, openid, 0 });
			long uin = -1L;
			if(userMap == null){//δ���û��ʻ��������û���ҳ��
				//request.setAttribute("action", "wxpaccount.do?action=bind");
				//return mapping.findForward("adduser");
				
				//�Զ���openidע��һ��û���ֻ��ŵ��˻�
				Long time = System.currentTimeMillis()/1000;
				uin= daService.getLong("SELECT nextval('seq_user_info_tb'::REGCLASS) AS newid", null);
				String strid = uin+"";
				int update = daService.update("insert into user_info_tb(id,nickname,password,strid," +
				"reg_time,auth_flag,comid,media,cityid,wxp_openid) values(?,?,?,?,?,?,?,?,?,?)", new Object[]{uin,"����:"+wxname,strid,strid,
						time,4,0,11,-1L,openid});
				if(update==1){
					int	eb = daService.update("insert into user_profile_tb (uin,low_recharge,limit_money,auto_cash," +
							"create_time,update_time) values(?,?,?,?,?,?)", 
							new Object[]{uin,10,25,1,time,time});
					logger.error(">>>>���û�ע�ᣬĬ��д��֧������...+"+eb);
				}else{
					logger.error("account>>>ע���쳣,����ע��...");
					return mapping.findForward("account");
				}
				userMap = daService.getMap("select * from user_info_tb where auth_flag=? and wxp_openid=? and state=? ",
						new Object[] {4, openid, 0 });
				logger.error("account>>>�Զ�ע�����û�");
			}
			Long count = daService.getLong(
					"select count(id) from car_info_tb where uin=? and state=? ",
					new Object[] { userMap.get("id"), 1 });
			if(count == 0){//����󶨳���ҳ��
				request.setAttribute("openid", openid);
				request.setAttribute("action", "wxpaccount.do?action=toaccountpage");
				return mapping.findForward("addcarnumber");
			}
			
			Long time = System.currentTimeMillis()/1000;
			Long ticket_count = daService.getLong("select count(id) from ticket_tb where uin = ? and limit_day > ? and state=? ",
					new Object[]{userMap.get("id"),time,0});
			String credit = "0/0";
			Integer is_auth = (Integer)userMap.get("is_auth");
			if(is_auth == 1){
				credit = userMap.get("credit_limit")+"/30";
			}
			request.setAttribute("ticket_count", ticket_count);//ͣ��ȯ��
			request.setAttribute("balance", userMap.get("balance"));//���
			request.setAttribute("mobile", userMap.get("mobile"));
			request.setAttribute("credit", credit);
			if("topresentorderlist".equals(forward)){
				request.setAttribute("domain", Constants.WXPUBLIC_REDIRECTURL);
				return mapping.findForward("presentorderlist");
			}else if("toparkprod".equals(forward)){
				request.setAttribute("domain", Constants.WXPUBLIC_REDIRECTURL);
				return mapping.findForward("parkprod");
			}else{
				return mapping.findForward("account");
			}
		}else if(action.equals("bind")){
			String mobile = RequestUtil.processParams(request, "mobile").trim();
			String openid = RequestUtil.processParams(request, "openid");
			request.setAttribute("openid", openid);
			request.setAttribute("mobile", mobile);
			if(mobile.equals("") || openid.equals("")){
				return mapping.findForward("error");
			}
			Long bind_count = daService.getLong(
					"select count(*) from user_info_tb where wxp_openid is not null and mobile=? ",
					new Object[] { mobile });
			int result = daService
					.update("update user_info_tb set wxp_openid=? where auth_flag=? and state=? and mobile=? ",
							new Object[] { openid, 4, 0, mobile });//΢�Ź��ںŰ󶨳����˻�
			if(result == 1){
				Map<String, Object> userMap = daService
						.getMap("select * from user_info_tb where mobile=? and auth_flag=? ",
								new Object[] { mobile, 4 });
				publicMethods.sharkbinduser(openid, (Long)userMap.get("id"),bind_count);
				
				Long count = daService
						.getLong("select count(c.id) from car_info_tb c,user_info_tb u where c.uin=u.id and u.auth_flag=? and c.state=? and u.mobile=? ",
								new Object[] {4, 1, mobile });
				if(count == 0){//����󶨳����˺�
					request.setAttribute("action", "wxpaccount.do?action=toaccountpage");
					return mapping.findForward("addcarnumber");
				} else {//��ת���˻�ҳ��
					Long time = System.currentTimeMillis()/1000;
					Long ticket_count = daService.getLong("select count(id) from ticket_tb where uin = ? and limit_day > ? and state=? ",
							new Object[]{userMap.get("id"),time,0});
					String credit = "0/0";
					Integer is_auth = (Integer)userMap.get("is_auth");
					if(is_auth == 1){
						credit = userMap.get("credit_limit")+"/30";
					}
					request.setAttribute("credit", credit);
					request.setAttribute("ticket_count", ticket_count);//ͣ��ȯ��
					request.setAttribute("balance", userMap.get("balance"));//���
					return mapping.findForward("account");
				}
			}
			return mapping.findForward("error");
		}else if(action.equals("toaccountpage")){
			String openid = RequestUtil.processParams(request, "openid");
			String wxname = RequestUtil.processParams(request, "wxname");
			String wximg = RequestUtil.processParams(request, "wximg");
			if(openid.equals("")){
				return mapping.findForward("error");
			}
			Map<String, Object> userMap = daService
					.getMap("select * from user_info_tb where auth_flag=? and wxp_openid=? and state=? ",
							new Object[] {4, openid, 0 });
			if(userMap == null){
				return mapping.findForward("error");
			}
			Long time = System.currentTimeMillis()/1000;
			Long ticket_count = daService.getLong("select count(id) from ticket_tb where uin = ? and limit_day > ? and state=? ",
					new Object[]{userMap.get("id"),time,0});
			
			String credit = "0/0";
			Integer is_auth = (Integer)userMap.get("is_auth");
			if(is_auth == 1){
				credit = userMap.get("credit_limit")+"/30";
			}
			request.setAttribute("credit", credit);
			request.setAttribute("wximg", wximg);
			request.setAttribute("wxname", wxname);
			request.setAttribute("ticket_count", ticket_count);//ͣ��ȯ��
			request.setAttribute("balance", userMap.get("balance"));//���
			request.setAttribute("openid", openid);
			request.setAttribute("mobile", userMap.get("mobile"));
			return mapping.findForward("account");
		}else if(action.equals("toticketpage")){
			String openid = RequestUtil.processParams(request, "openid");
			Integer type = RequestUtil.getInteger(request, "type", 0);//0:����ͣ��ȯ��1��ʷͣ��ȯ
			
			if(openid.equals("")){
				return mapping.findForward("error");
			}
			Map<String, Object> userMap = daService
					.getMap("select * from user_info_tb where auth_flag=? and wxp_openid=? and state=? ",
							new Object[] {4, openid, 0 });
			if(userMap == null){
				return mapping.findForward("error");
			}
			String mobile = (String)userMap.get("mobile");
			request.setAttribute("mobile", mobile);
			request.setAttribute("type", type);
			request.setAttribute("openid", openid);
			request.setAttribute("domain", Constants.WXPUBLIC_REDIRECTURL);
			return mapping.findForward("ticket");
		}else if(action.equals("toaccountdetail")){
			String openid = RequestUtil.processParams(request, "openid");
			Long orderid = RequestUtil.getLong(request, "orderid", -1L);
//			openid = "oRoektybTsv33_vSKKUwLAsJAquc";
			Map<String, Object> userMap = daService
					.getMap("select * from user_info_tb where auth_flag=? and wxp_openid=? and state=? ",
							new Object[] {4, openid, 0 });
			if(userMap == null){
				return mapping.findForward("error");
			}
			request.setAttribute("openid", userMap.get("openid"));
			request.setAttribute("orderid", orderid);
			return mapping.findForward("accountdetail");
		}else if(action.equals("regbonus")){
			Long media =RequestUtil.getLong(request, "media",11L);
			Long uid =RequestUtil.getLong(request, "uid",-1L);//�Ƽ��ˣ���id=999ʱ���շ�Ա�Ƽ�������
			request.setAttribute("media", media);
			request.setAttribute("uid", uid);
			
			String code = RequestUtil.processParams(request, "code");
			logger.error(">>>>>>>>>>>>>>>>>>>>>>>>>����΢��ҡһҡ���>>>>>>>>>>>>>>>,code:"+code);
			String access_token_url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid="+Constants.WXPUBLIC_APPID+"&secret="+Constants.WXPUBLIC_SECRET+"&code="+code+"&grant_type=authorization_code";
			String result = CommonUtil.httpsRequest(access_token_url, "GET", null);
			JSONObject map = JSONObject.fromObject(result);
			if(map.get("errcode") != null){
				return mapping.findForward("error");
			}
			String openid = (String)map.get("openid");
			logger.error("openid:"+openid);
//			String openid = "o0ciquFyGzc2DSxh8lxzwMIW6fwg";
			request.setAttribute("openid", openid);
			
			Map<String, Object> userMap = daService.getMap(
					"select * from user_info_tb where wxp_openid=? ",
					new Object[] { openid });
			if(userMap == null){
				return mapping.findForward("regbonus");
			}else{
				Long count = daService.getLong(
						"select count(id) from car_info_tb where uin=? and state=? ",
						new Object[] { userMap.get("id"), 1 });
				request.setAttribute("type", 0);//0���û�
				request.setAttribute("mobile", userMap.get("mobile"));
				if(count == 0){
					return mapping.findForward("addcnum_regbonus");
				}else{
					//΢�Ź��ں�JSSDK��Ȩ��֤
					Map<String, String> ret = new HashMap<String, String>();
					ret = publicMethods.getJssdkApiSign(request);
					//jssdkȨ����֤����
					request.setAttribute("appid", Constants.WXPUBLIC_APPID);
					request.setAttribute("nonceStr", ret.get("nonceStr"));
					request.setAttribute("timestamp", ret.get("timestamp"));
					request.setAttribute("signature", ret.get("signature"));
					return mapping.findForward("nobonus");
				}
			}
		}else if(action.equals("getcode")){
			String mobile = RequestUtil.processParams(request, "mobile");
			Long media =RequestUtil.getLong(request, "media",11L);
			Long uid =RequestUtil.getLong(request, "uid",-1L);//�Ƽ��ˣ���id=999ʱ���շ�Ա�Ƽ�������
			if(mobile.equals("")){
				return mapping.findForward("error");
			}
			Long uin = -1L;
			Integer type = 0;//type=0������ע���û� type=1:��ע���û�
			String sql = "select * from user_info_tb where mobile=? and auth_flag=? ";
			Map<String, Object> user = daService.getPojo(sql, new Object[]{mobile,4});
			if(user == null){
				Double money = 10d;//��ע�᷵10Ԫȯ
				uin = publicMethods.regUser(mobile, media,uid,true);
				if(uin > 0){
					/*int r = daService.update("insert into bonus_record_tb (bid,ctime,mobile,state,amount) values(?,?,?,?,?)",
							new Object[] { media, System.currentTimeMillis()/1000, mobile, 0, money });*/
				}
				type = 1;
				logger.error("ҡһҡ�������ע���û���mobile��"+mobile);
			}else{
				uin = (Long)user.get("id");
			}
			
			//���Ͷ�����֤��
			if(!publicMethods.isCanSendShortMesg(mobile)){
				AjaxUtil.ajaxOutput(response, "-7");
				return null;
			}
			Integer code = new SendMessage().sendMessageToCarOwer(mobile);
			if(code!=null){
				logger.error("code:"+code+",mobile:"+mobile+" ,uin="+uin);
				//������֤��
				//ɾ���Ѿ����浫û����֤������֤�루����Ч����֤�룩
				try {
					daService.update("delete from verification_code_tb where uin =?",new Object[]{uin});
				} catch (Exception e) {
					e.printStackTrace();
				}
				int r =daService.update("insert into verification_code_tb (verification_code,uin,create_time,state)" +
						" values (?,?,?,?)", new Object[]{code,uin,System.currentTimeMillis()/1000,0});
				logger.error("code:"+code+",mobile:"+mobile+" ,uin="+uin+"��������֤�룺"+r);
				Map verificationMap = daService.getPojo("select verification_code from verification_code_tb" +
						" where uin=? and state=? ", new Object[]{uin,0});
				logger.error(verificationMap);
				if(r==1)
					AjaxUtil.ajaxOutput(response, type + "");
				else{
					AjaxUtil.ajaxOutput(response, "-7");
					return null;
				}
			}else {
				AjaxUtil.ajaxOutput(response, "-3");
			}
		}else if(action.equals("regbonusbind")){
			String openid = RequestUtil.processParams(request, "openid");
			String mobile = RequestUtil.processParams(request, "mobile");
			Integer type = RequestUtil.getInteger(request, "type", 0);//type=1��ʾ��ע��
			request.setAttribute("type", type);
			request.setAttribute("openid", openid);
			request.setAttribute("mobile", mobile);
			if(mobile.equals("") || openid.equals("")){
				return mapping.findForward("error");
			}
			Long bind_count = daService.getLong(
					"select count(*) from user_info_tb where wxp_openid is not null and mobile=? ",
					new Object[] { mobile });
			int result = daService
					.update("update user_info_tb set wxp_openid=? where auth_flag=? and state=? and mobile=? ",
							new Object[] { openid, 4, 0, mobile });//΢�Ź��ںŰ󶨳����˻�
			if(result == 1){
				Map<String, Object> userMap = daService
						.getMap("select * from user_info_tb where mobile=? and auth_flag=? ",
								new Object[] { mobile, 4 });
				publicMethods.sharkbinduser(openid, (Long)userMap.get("id"),bind_count);
				
				Long count = daService.getLong(
						"select count(id) from car_info_tb where uin=? and state=? ",
						new Object[] { userMap.get("id"), 1 });
				if(count == 0){//����󶨳����˺�
					return mapping.findForward("addcnum_regbonus");
				}else{
					//΢�Ź��ں�JSSDK��Ȩ��֤
					Map<String, String> ret = new HashMap<String, String>();
					ret = publicMethods.getJssdkApiSign(request);
					//jssdkȨ����֤����
					request.setAttribute("appid", Constants.WXPUBLIC_APPID);
					request.setAttribute("nonceStr", ret.get("nonceStr"));
					request.setAttribute("timestamp", ret.get("timestamp"));
					request.setAttribute("signature", ret.get("signature"));
					return mapping.findForward("nobonus");
				}
			}else{
				return mapping.findForward("error");
			}
		}else if(action.equals("tobonuspage")){
			Integer type = RequestUtil.getInteger(request, "type", 0);
			String mobile = RequestUtil.processParams(request, "mobile");
			
//			type = 1;
//			mobile="18201517240";
			
			if(mobile.equals("")){
				return mapping.findForward("error");
			}
			//΢�Ź��ں�JSSDK��Ȩ��֤
			Map<String, String> ret = new HashMap<String, String>();
			ret = publicMethods.getJssdkApiSign(request);
			//jssdkȨ����֤����
			request.setAttribute("appid", Constants.WXPUBLIC_APPID);
			request.setAttribute("nonceStr", ret.get("nonceStr"));
			request.setAttribute("timestamp", ret.get("timestamp"));
			request.setAttribute("signature", ret.get("signature"));
			if(type == 1){
				request.setAttribute("mobile", mobile);
				Map<String, Object> bonusMap = daService
						.getMap("select * from bonus_record_tb where mobile=? order by ctime limit 1 ",
								new Object[] {mobile});
				request.setAttribute("amount", bonusMap.get("amount"));//�Ѿ���ȡ��
				return mapping.findForward("bonus");
			}else{
				return mapping.findForward("nobonus");
			}
			
		}else if(action.equals("balance")){
			String openid = RequestUtil.processParams(request, "openid");
			
//			openid = "oRoektybTsv33_vSKKUwLAsJAquc";
			logger.error(">>>>>>>>>>>>>>>>΢�Ź��ںŽ����ҵ����:openid"+openid);
			if(openid.equals("")){
				return mapping.findForward("error");
			}
			Map<String, Object> userMap = daService.getMap(
					"select balance from user_info_tb where wxp_openid=? ",
					new Object[] { openid });
			if(userMap == null){
				return mapping.findForward("error");
			}
			request.setAttribute("balance", userMap.get("balance"));
			request.setAttribute("openid", openid);
			return mapping.findForward("balance");
		}else if(action.equals("charge")){
			String openid = RequestUtil.processParams(request, "openid");
			Double money = RequestUtil.getDouble(request, "fee", 0d);
			
			if(openid.equals("") || money <= 0){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			Map<String, Object> userMap = daService.getMap(
					"select * from user_info_tb where wxp_openid=? ",
					new Object[] { openid });
			if(userMap == null){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			String addressip = request.getRemoteAddr();
			Map<String, Object> attachMap = new HashMap<String, Object>();
			attachMap.put("money", money);//��ֵ���
			attachMap.put("mobile", userMap.get("mobile"));//�����ֻ���
			attachMap.put("type", 3);//��ֵ
			attachMap.put("bind_flag", 1);//0:δ�� 1���Ѱ��˻�
			//��������
			String attach = StringUtils.createJson(attachMap);
			Map<String, Object> infoMap = new HashMap<String, Object>();
			try {
				//����֧������
				SortedMap<Object, Object> signParams = new TreeMap<Object, Object>();
				//��ȡJSAPI��ҳ֧������
				signParams = PayCommonUtil.getPayParams(addressip, money, "ͣ�����˻���ֵ", attach, openid);
				
				infoMap.put("appid", signParams.get("appId"));
				infoMap.put("nonceStr", signParams.get("nonceStr"));
				infoMap.put("packagevalue", signParams.get("package"));
				infoMap.put("timestamp", signParams.get("timeStamp"));
				infoMap.put("paySign", signParams.get("paySign"));
				infoMap.put("signType", signParams.get("signType"));
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}
			infoMap.put("openid", openid);
			//��������
			infoMap.put("money", money);
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
		}else if(action.equals("tocarnumber")){
			String openid = RequestUtil.processParams(request, "openid");
			Map<String, Object> userMap = daService
					.getMap("select id,mobile from user_info_tb where wxp_openid=? limit ? ",
							new Object[] { openid, 1 });
			if(userMap != null){
				Long count = daService.getLong("select count(*) from car_info_tb where uin=? and state=? ",
								new Object[] { userMap.get("id"), 1 });
				logger.error("uin:"+userMap.get("id")+",count:"+count);
				request.setAttribute("count", count);
				request.setAttribute("openid", openid);
				//request.setAttribute("mobile", userMap.get("mobile"));
				return mapping.findForward("tocarnumber");
			}
			return mapping.findForward("error");
		}else if(action.equals("toupload")){
			String carnumber = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "carnumber"));
			logger.error("carnumber:"+carnumber);
			String openid = RequestUtil.processParams(request, "openid");
			if(openid.equals("")){
				return mapping.findForward("error");
			}
			if(!carnumber.equals("")){
				Map<String, Object> carMap = daService.getMap("select id from car_info_tb where car_number=? ",
						new Object[] { carnumber });
				if(carMap != null){
					request.setAttribute("carid", carMap.get("id"));
				}
			}
			/*try {
				//΢�Ź��ں�JSSDK��Ȩ��֤
				Map<String, String> ret = new HashMap<String, String>();
				ret = publicMethods.getJssdkApiSign(request);
				//jssdkȨ����֤����
				request.setAttribute("appid", Constants.WXPUBLIC_APPID);
				request.setAttribute("nonceStr", ret.get("nonceStr"));
				request.setAttribute("timestamp", ret.get("timestamp"));
				request.setAttribute("signature", ret.get("signature"));
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();
			}*/
			request.setAttribute("carnumber", carnumber);
			request.setAttribute("action", "wxpaccount.do?action=tocarnumber");
			request.setAttribute("domain", Constants.WXPUBLIC_REDIRECTURL);
			request.setAttribute("openid", openid);
			return mapping.findForward("addcarnumber");
		}else if(action.equals("previewpic")){//Ԥ����֤ͼƬ
			String serverid = RequestUtil.processParams(request, "serverid");//΢�ŷ�����ͼƬserverid
			if(!serverid.equals("")){
				try {
					String accessToken = publicMethods.getWXPAccessToken();
					previewpic(accessToken, serverid, response);
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
		}else if(action.equals("upload")){
			String carnumber = AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "carnumber"));
			//String serverid = RequestUtil.processParams(request, "serverid");
			Long carid = RequestUtil.getLong(request, "carid", -1L);
			String opneid = RequestUtil.processParams(request, "openid");
			//Long curTime = System.currentTimeMillis()/1000;
			logger.error("upload>>>carnumber:"+carnumber+",carid:"+carid);
			if(carnumber.equals("") || opneid.equals("")){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			Map<String, Object> userMap = daService.getMap(
					"select id from user_info_tb where wxp_openid=? limit ? ",
					new Object[] { opneid, 1 });
			if(userMap == null){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			Long uin = (Long)userMap.get("id");
			
			//��ӳ���
			int addCarnum = commonMethods.addCarnum(uin, carnumber);
			
			/*//1.���ݳ��Ʋ�ѯ����
			List<Map> all = daService.getAll("select uin,is_auth,state from car_info_tb where car_number = ?", new Object[]{carnumber});
			for (Map map : all) {
				logger.error(map);
				Long cuin = (Long)map.get("uin");
				Integer state = (Integer) map.get("state");
				Integer isauth = (Integer) map.get("is_auth");
				if(cuin.intValue()==uin&&serverid.equals("")&&state!=0){
					logger.error("�����ѱ��Լ�ע��");
					AjaxUtil.ajaxOutput(response, "-3");
					return null;
				}
				if(isauth>0){//����֤
					logger.error("�����ѱ�����������֤");
					AjaxUtil.ajaxOutput(response, "-2");
					return null;
				}
				//����cuin���¿�,�����δ���ڵ��¿�,�������ֻ���,�򲻿ɰ�
				List<Map> productlist = daService.getAll("select e_time from carower_product where uin = ?", new Object[]{cuin});
				for (Map prod : productlist) {
					Long endTime = (Long) prod.get("e_time");
					if(endTime==null){
						logger.error("ȱʧ�¿�����ʱ��");
						AjaxUtil.ajaxOutput(response, "-1");
						return null;
					}
					Long cTime = TimeTools.getToDayBeginTime();
					logger.error(cTime);
					logger.error("�¿���������:"+TimeTools.getTimeStr_yyyy_MM_dd(endTime*1000)+" ��ǰ����:"+TimeTools.getTimeStr_yyyy_MM_dd(cTime*1000));
					if(cTime<endTime){
						//�������ֻ��� �Ų��ܰ�
						logger.error("�¿�δ����");
						Map uMap = daService.getMap("select mobile from user_info_tb where id = ?", new Object[]{cuin});
						Object obj = uMap.get("mobile");
						if(obj!=null&&StringUtils.isNotNull((String)obj)){
							logger.error("���ֻ���,���ܰ�");
							AjaxUtil.ajaxOutput(response, "-2");
							return null;
						}
						Object obj2 = uMap.get("wxp_openid");
						int result = 0;
						if(obj2!=null&&StringUtils.isNotNull((String)obj2)){
							//��΢��,���¿�ת�����ڵ��û�,ɾ�����û�
							//ɾ��֮ǰ�û�
							//ɾ��֮ǰ�û�
							result = daService.update("delete from user_info_tb where id = ?", new Object[]{cuin});
							logger.error("ɾ�����û�:"+result);
							//���³��ƺ��¿����û�
							result = daService.update("update car_info_tb set uin = ? where car_number = ?", new Object[]{uin,carnumber});
							logger.error("���¸ó����û�:"+result);
							Map map2 = daService.getMap("select id from carower_product where uin = ?", new Object[]{cuin});
							result = daService.update("update carower_product set uin = ? where id = ?", new Object[]{uin,map2.get("id")});
							logger.error("���³����û��¿�:"+result);
							AjaxUtil.ajaxOutput(response, "1");
							return null;
						}
					}
				}
			}
			
			logger.error(">>>>>>>���ƿ��԰�");
			int update = -1;
			if(carid == -1){
				logger.error("����³���carnumber:"+carnumber+",uin:"+uin+",carid:"+carid);
				Map carMap = daService.getMap("select uin from car_info_tb where car_number=? ", new Object[]{carnumber});
				Long count = daService.getLong("select count(id) from car_info_tb where uin=? and state=? ", new Object[]{uin, 1});
				if(count > 3){
					logger.error("�����Ѿ����� ������uin:"+uin+",carnumber:"+carnumber+",count:"+count);
					AjaxUtil.ajaxOutput(response, "-4");
					return null;
				}
				//��ɾ�����иó�������
				Long carcount = daService.getLong("select count(id) from car_info_tb where car_number = ?", new Object[]{carnumber});
				int delCarnum = 1;
				if(carcount>0){
					String delSql = "delete from car_info_tb where car_number= ?";
					delCarnum = daService.update(delSql, new Object[]{carnumber});
				}
				if(delCarnum>0){
					//�¼�һ��
					carid = daService.getkey("seq_ticket_tb");
					String addSql = "insert into car_info_tb(id,car_number,uin,create_time) values(?,?,?,?)";
					update = daService.update(addSql, new Object[]{carid,carnumber,uin,curTime});
					if(update>0){
						logger.error("�󶨳��Ƴɹ�");
						logger.error("ע���³��ƣ�uin:"+uin+",carnumber:"+carnumber+",carid:"+carid);
					}else{
						logger.error("�󶨳���ʧ��");
					}
				}else{
					logger.error("ɾ���ɳ���ʧ��");
				}
				
				
			}else{
				//��ͨ���ѹر�
				logger.error("�޸ĳ���carnumber:"+carnumber+",uin:"+uin+",carid:"+carid);
				//1.ɾ���ó���id!=carid��auth<1��������Ŀ
				Long delcount = daService.getLong("select count(id) from car_info_tb where car_number = ? and is_auth < ?", new Object[]{carnumber,1});
				int update = 1;
				if(delcount>0){//�����,ɾ��
					String delSql = "delete from car_info_tb where car_number= ? and is_auth < ?";
					update = daService.update(delSql, new Object[]{carnumber,1});
				}
				if(update>0){
					//2.���³���
					int r = daService.update("update car_info_tb set car_number=? where id=? ",
							new Object[] { carnumber, carid });
					if(r>0){
						logger.error("�޸ĳ��ƣ�uin:"+uin+",carnumber:"+carnumber+",carid:"+carid);
					}else{
						logger.error("�޸�ʧ��");
					}
				}else{
					logger.error("ɾ����Ч����ʧ��");
				}
			}*/
			//��һ������߼�
/*			if(carid == -1){
				logger.error("����³���carnumber:"+carnumber+",uin:"+uin+",carid:"+carid);
				Map carMap = daService.getMap("select uin from car_info_tb where car_number=? ", new Object[]{carnumber});
				logger.error(carMap);
				if(carMap != null){//�ó�����ע��
					Long cuin = (Long)carMap.get("uin");
					logger.error("�ó����ѱ�ע�ᣬuin:"+uin+",ע��ó��Ƶĳ�����cuin��"+cuin+",carnumber:"+carnumber);
					if(cuin.intValue() != uin.intValue()){//���Ʊ�����ע����
						AjaxUtil.ajaxOutput(response, "-2");
					}else{//���Ʊ��Լ�ע����
						AjaxUtil.ajaxOutput(response, "-3");
					}
					return null;
				}else{
					Long count = daService.getLong("select count(id) from car_info_tb where uin=? and state=? ", new Object[]{uin, 1});
					if(count < 3){
						carid = daService.getkey("seq_ticket_tb");
						int r = daService.update("insert into car_info_tb(id,car_number,uin,create_time) values(?,?,?,?)", new Object[]{carid, carnumber, uin, curTime});
						logger.error("ע���³��ƣ�uin:"+uin+",carnumber:"+carnumber+",carid:"+carid+",r:"+r);
					}else{
						logger.error("�����Ѿ�����������uin:"+uin+",carnumber:"+carnumber+",count:"+count);
						AjaxUtil.ajaxOutput(response, "-4");
						return null;
					}
				}
			}else{
				logger.error("�޸ĳ���carnumber:"+carnumber+",uin:"+uin+",carid:"+carid);
				Map<String, Object> carMap = daService.getMap("select uin from car_info_tb where car_number=? and id != ? ",
								new Object[] { carnumber, carid });
				if(carMap != null){
					Long cuin = (Long)carMap.get("uin");
					if(cuin.intValue() != uin.intValue()){//���Ʊ�����ע����
						AjaxUtil.ajaxOutput(response, "-2");
					}else{//���Ʊ��Լ�ע����
						AjaxUtil.ajaxOutput(response, "-3");
					}
					return null;
				}else{
					int r = daService.update("update car_info_tb set car_number=? where id=? ",
							new Object[] { carnumber, carid });
					logger.error("�޸ĳ��ƣ�uin:"+uin+",carnumber:"+carnumber+",carid:"+carid+",r:"+r);
				}
			}*/
			/*if(carid != -1 && !serverid.equals("")){
				logger.error("upload picture>>>uin:"+uin+",carid:"+carid+",carnumber:"+carnumber+",serverid:"+serverid);
				String meidaId[] = serverid.split(",");
				try {
					Long count = daService.getLong("select count(id) from car_info_tb where car_number=? and is_auth> ?", new Object[]{carnumber, 0});
					logger.error("uin:"+uin+",carid:"+carid+",carnumber:"+carnumber+"count:"+count);
					//�ó���δ�ɹ���֤
					if(count == 0){
						String accessToken = publicMethods.getWXPAccessToken();
						for(int i=0; i< meidaId.length; i++){
							uploadCheck2Mongodb(accessToken, meidaId[i], uin, carnumber, i);
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}*/
			AjaxUtil.ajaxOutput(response, addCarnum+"");
			return null;
		}else if(action.equals("tobuyticket")){
			//******************����ͣ��ȯȥ���� ***************//
			if(true){
				return mapping.findForward("error");
			}
			//******************************************//
			String openid = RequestUtil.processParams(request, "openid");
			if(openid.equals("")){
				return mapping.findForward("error");
			}
			Map<String, Object> userMap = daService
					.getMap("select is_auth from user_info_tb where wxp_openid=? limit ? ",
							new Object[] { openid, 1 });
			if(userMap != null){
				Integer isauth = (Integer)userMap.get("is_auth");
				logger.error("tobuyticket>>>openid:"+openid+",isauth:"+isauth);
				Double authdiscount = StringUtils.formatDouble(CustomDefind.getValue("AUTHDISCOUNT"))*10;
				Double noauthdiscount = StringUtils.formatDouble(CustomDefind.getValue("NOAUTHDISCOUNT"))*10;
				request.setAttribute("isauth", isauth);
				request.setAttribute("openid", openid);
				request.setAttribute("authdisc", authdiscount);
				request.setAttribute("noauthdisc", noauthdiscount);
				return mapping.findForward("tobuyticket");
			}else{
				return mapping.findForward("error");
			}
		}else if(action.equals("ticketprice")){
			String openid = RequestUtil.processParams(request, "openid");
			Integer ticketmoney = RequestUtil.getInteger(request, "ticketmoney", 0);
			Integer num = RequestUtil.getInteger(request, "num", 0);
			logger.error("ticketprice>>>openid:"+openid+",ticketmoney:"+ticketmoney+",num:"+num);
			if(openid.equals("") || ticketmoney == 0 || num == 0){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			Map<String, Object> userMap = daService
					.getMap("select is_auth from user_info_tb where wxp_openid=? limit ? ",
							new Object[] { openid, 1 });
			if(userMap != null){
				Integer isauth = (Integer)userMap.get("is_auth");
				Double discount = Double.valueOf(CustomDefind.getValue("NOAUTHDISCOUNT"));
				Double moneybefore = StringUtils.formatDouble(ticketmoney*num);
				if(isauth == 1){
					discount = Double.valueOf(CustomDefind.getValue("AUTHDISCOUNT"));
				}
				Double moneyafter = StringUtils.formatDouble(ticketmoney*num*discount);
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("moneybefore", moneybefore);
				map.put("moneyafter", moneyafter);
				logger.error("ticketprice>>>openid:"+openid+",isauth:"+isauth+",moneybefore:"+moneybefore+",moneyafter:"+moneyafter);
				AjaxUtil.ajaxOutput(response, StringUtils.createJson(map));
			}else{
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
		}else if(action.equals("buyticket")){
			String openid = RequestUtil.processParams(request, "openid");
			Integer ticketmoney = RequestUtil.getInteger(request, "ticketmoney", 0);
			Integer ticketnum = RequestUtil.getInteger(request, "ticketnum", 0);
			logger.error("buyticket>>>openid:"+openid+",ticketmoney:"+ticketmoney+",ticketnum:"+ticketnum);
			if(openid.equals("") || ticketmoney == 0 || ticketnum == 0){
				return mapping.findForward("error");
			}
			Map<String, Object> userMap = daService
					.getMap("select * from user_info_tb where wxp_openid=? limit ? ",
							new Object[] { openid, 1 });
			if(userMap == null){
				return mapping.findForward("error");
			}else{
				Integer isauth = (Integer)userMap.get("is_auth");
				Double discount = Double.valueOf(CustomDefind.getValue("NOAUTHDISCOUNT"));
				if(isauth == 1){
					discount = Double.valueOf(CustomDefind.getValue("AUTHDISCOUNT"));
				}
				Double total = StringUtils.formatDouble(ticketmoney*ticketnum*discount);
				Double wx_pay = 0d;
				Double balance_pay = 0d;
				logger.error("buyticket>>>openid:"+openid+",isauth:"+isauth+",total:"+total+",uin:"+userMap.get("id"));
				Double balance = Double.valueOf(userMap.get("balance") + "");//�û����
				if(total > balance){
					balance_pay = balance;//���ȫ������֧��
					wx_pay = StringUtils.formatDouble(total - balance_pay);
				}else{
					balance_pay = total;
				}
				if(wx_pay > 0){
					try {
						Map<String, Object> attachMap = new HashMap<String, Object>();
						attachMap.put("money", total);//���ͽ��
						attachMap.put("type", 7);//��ͣ��ȯ
						attachMap.put("ticketmoney", ticketmoney);
						attachMap.put("ticketnum", ticketnum);
						//��������
						String attach = StringUtils.createJson(attachMap);
						//����֧������
						SortedMap<Object, Object> signParams = new TreeMap<Object, Object>();
						//��ȡJSAPI��ҳ֧������
						signParams = PayCommonUtil.getPayParams(request.getRemoteAddr(), wx_pay, "����ͣ��ȯ", attach, openid);
						request.setAttribute("appid", signParams.get("appId"));
						request.setAttribute("nonceStr", signParams.get("nonceStr"));
						request.setAttribute("package", signParams.get("package"));
						request.setAttribute("packagevalue", signParams.get("package"));
						request.setAttribute("timestamp", signParams.get("timeStamp"));
						request.setAttribute("paySign", signParams.get("paySign"));
						request.setAttribute("signType", signParams.get("signType"));
					} catch (Exception e) {
						// TODO: handle exception
						e.printStackTrace();
					}
				}
				request.setAttribute("total", total);
				request.setAttribute("balance_pay", balance_pay);
				request.setAttribute("wx_pay", wx_pay);
				request.setAttribute("ticketmoney", ticketmoney);
				request.setAttribute("ticketnum", ticketnum);
				request.setAttribute("openid", openid);
				request.setAttribute("mobile", userMap.get("mobile"));
				return mapping.findForward("buyticket");
			}
		}else if(action.equals("toorderlist")){
			//String mobile = RequestUtil.processParams(request, "mobile");
			String openid = RequestUtil.processParams(request, "openid");
			if(openid.equals("")){
				return mapping.findForward("error");
			}
			request.setAttribute("domain", Constants.WXPUBLIC_REDIRECTURL);
			request.setAttribute("openid", openid);
			return mapping.findForward("orderlist");
		}else if(action.equals("orderdetail")){
			String mobile = RequestUtil.processParams(request, "mobile");
			Long orderid = RequestUtil.getLong(request, "orderid", -1L);
			if(mobile.equals("") || orderid == -1){
				return mapping.findForward("error");
			}
			
			Map<String, Object> userMap = pService
					.getMap("select id,wxp_openid from user_info_tb where mobile=? and auth_flag=? ",
							new Object[] { mobile, 4 });
			if(userMap == null || userMap.get("wxp_openid") == null){
				return mapping.findForward("error");
			}
			Long etime = System.currentTimeMillis()/1000;
			request.setAttribute("orderid", orderid);
			request.setAttribute("domain", Constants.WXPUBLIC_REDIRECTURL);
			request.setAttribute("bonusid", -1);
			request.setAttribute("openid", userMap.get("wxp_openid"));
			
			Map<String, Object> orderMap = pService.getMap(
					"select * from order_tb where id=? ", new Object[] {orderid});
			if(orderMap == null){
				return mapping.findForward("error");
			}
			Integer state = (Integer)orderMap.get("state");
			Double prepay = Double.valueOf(orderMap.get("prepaid") + "");
			request.setAttribute("state",state);
			request.setAttribute("btime",TimeTools.getTime_yyyyMMdd_HHmm((Long)orderMap.get("create_time")*1000));
			request.setAttribute("prepay", prepay);
			if(state == 2 && prepay == 0){
				request.setAttribute("escapeUrl", "http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/wxpfast.do?action=beginprepay&orderid="+orderid+"&openid="+userMap.get("wxp_openid")+"&paytype=1");
			}
			if(orderMap.get("end_time") != null){
				etime = (Long)orderMap.get("end_time");
				request.setAttribute("etime",TimeTools.getTime_yyyyMMdd_HHmm(etime*1000));
			}
			request.setAttribute("parktime", StringUtils.getTimeString((Long)orderMap.get("create_time"), etime));
			if(state == 0){
				Integer pid = (Integer)orderMap.get("pid");
				Integer car_type = (Integer)orderMap.get("car_type");//0��ͨ�ã�1��С����2����
				if(pid>-1){
					request.setAttribute("total", Double.valueOf(publicMethods.getCustomPrice((Long)orderMap.get("create_time"), System.currentTimeMillis()/1000, pid)));
				}else {
					request.setAttribute("total", Double.valueOf(publicMethods.getPrice((Long)orderMap.get("create_time"), System.currentTimeMillis()/1000, (Long)orderMap.get("comid"), car_type)));
				}
			}else{
				request.setAttribute("total", orderMap.get("total"));
			}
			if(orderMap.get("pay_type") != null){
				Integer pay_type = (Integer)orderMap.get("pay_type");
				String paytype = "";
				if(pay_type == 0 || pay_type == 2){
					paytype = "����֧��";
				}else if(pay_type == 1 || pay_type == 4){
					paytype = "�ֽ�֧��";
				}else if(pay_type == 3){
					paytype = "����";
				}else if(pay_type == 5){
					paytype = "������";
				}else if(pay_type == 6){
					paytype = "�̼ҿ�";
				}else if(pay_type == 8){
					paytype = "���";
				}
				request.setAttribute("paytype", paytype);
			}
			if(orderMap.get("c_type") != null){
				/*//Integer c_type = (Integer)orderMap.get("c_type");
				String intype = "";
				if(c_type == 0){
					intype = "NFCˢ��";
				}else if(c_type == 1){
					intype = "IBeacon";
				}else if(c_type == 2 || c_type == 3 || c_type == 5){
					intype = "ɨ�賵��";
				}else if(c_type == 4){
					intype = "�Ǽ�ʱ";
				}else if(c_type == 6){
					intype = "ɨ��λ��ά��";
				}
				request.setAttribute("intype", intype);*/
			}
			if(orderMap.get("car_number") != null){
				String carnumber = (String)orderMap.get("car_number");
				if(carnumber.length() != carnumber.getBytes().length){
					request.setAttribute("carnumber", carnumber);
				}
			}
			if(orderMap.get("comid") != null){
				Long comid = (Long)orderMap.get("comid");
				request.setAttribute("comid", comid);
				Map comMap = pService.getMap("select company_name from com_info_tb where id=?", new Object[]{comid});
				if(comMap!=null){
					request.setAttribute("cname", comMap.get("company_name"));
				}
				
				//��ͼƬ
				Map<String,Object> picMap = pService.getMap("select picurl from com_picturs_tb where comid=? order by id desc limit ?",
						new Object[]{comid,1});
				String picUrls = "";
				if(picMap!=null&&!picMap.isEmpty()){
					picUrls="http://"+Constants.WXPUBLIC_S_DOMAIN+"/zld/"+(String)picMap.get("picurl");
				}
				request.setAttribute("picUrls", picUrls);
			}
			if(orderMap.get("uid") != null){
				Long uid = (Long)orderMap.get("uid");
				request.setAttribute("uid", uid);
				Map<String, Object> uidMap = pService.getMap(
						"select nickname from user_info_tb where id=? ",
						new Object[] { uid });
				if(uidMap != null){
					request.setAttribute("nickname", uidMap.get("nickname"));
				}
				
				Integer reward_flag = 0;
				Long count = pService.getLong(
						"select count(id) from parkuser_reward_tb where uin=? and order_id=? ",
						new Object[] { userMap.get("id"), orderid });
				if(count > 0){
					reward_flag = 1;
				}else{
					count = pService.getLong("select count(*) from parkuser_reward_tb where uin=? and ctime>=? and uid=? ",
									new Object[] { userMap.get("id"), TimeTools.getToDayBeginTime(), uid });
					if(count > 0){
						reward_flag = 2;
					}
				}
				request.setAttribute("reward_flag", reward_flag);
				if(reward_flag == 0){
					request.setAttribute("url", "http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/wxpfast.do?action=toreward&uid="+uid+"&orderid="+orderid+"&openid="+userMap.get("wxp_openid"));
				}
				logger.error("uid:"+uid+",orderid:"+orderid+",reward_flag:"+reward_flag);
			}
			Map bonusMap = pService.getMap("select * from order_ticket_tb where order_id=? limit ? ", new Object[]{orderid,1});
			if(bonusMap!=null&&!bonusMap.isEmpty()){
				logger.error(">>>��΢�ź��...");
				request.setAttribute("bonusid", bonusMap.get("id"));
				request.setAttribute("bonus_money", bonusMap.get("money"));
				request.setAttribute("bonus_bnum", bonusMap.get("bnum"));
				request.setAttribute("bonus_type", bonusMap.get("type"));
				
				request.setAttribute("title", CustomDefind.getValue("TITLE"));
				request.setAttribute("desc", CustomDefind.getValue("DESCRIPTION"));
				
				try {
					//΢�Ź��ں�JSSDK��Ȩ��֤
					Map<String, String> ret = new HashMap<String, String>();
					ret = publicMethods.getJssdkApiSign(request);
					//jssdkȨ����֤����
					request.setAttribute("appid", Constants.WXPUBLIC_APPID);
					request.setAttribute("nonceStr", ret.get("nonceStr"));
					request.setAttribute("timestamp", ret.get("timestamp"));
					request.setAttribute("signature", ret.get("signature"));
				} catch (Exception e) {
					// TODO: handle exception
					e.printStackTrace();
				}
			}
			return mapping.findForward("horderdetail");
		}else if(action.equals("orderlist")){
			Integer pageNum = RequestUtil.getInteger(request, "page", 1);
			Integer pageSize = RequestUtil.getInteger(request, "size", 20);
			Integer present = RequestUtil.getInteger(request, "present", -1);
			//String mobile = RequestUtil.processParams(request, "mobile");
			String openid = RequestUtil.processParams(request, "openid");
			if(openid.equals("")){
				return mapping.findForward("error");
			}
			List<Map<String, Object>> infoMapList = new ArrayList<Map<String,Object>>();
			Map<String, Object> userMap = pService
					.getMap("select * from user_info_tb where wxp_openid=? and auth_flag=? ",
							new Object[] { openid, 4 });
			if(userMap != null){
				/*if(pageNum == 1){
					String statequery = "";
					Object[] param = null;
					if(present==1){
						logger.error(">>>>>>>��ѯ�û�idΪ:"+userMap.get("id")+"���ڳ�����");
						statequery = "state = ?";
						param = new Object[]{ userMap.get("id"), 0 };
					}else{
						logger.error(">>>>>>>��ѯ�û�idΪ:"+userMap.get("id")+"�����ж���");
						statequery = "state in (?,?)";
						param = new Object[] { userMap.get("id"), 0, 2 };
					}
					String sql = "select * from order_tb where uin=? and "+statequery+" order by state,create_time desc";
					logger.error("sql: "+sql);
					List<Map<String, Object>> orderList = pService.getAll(sql,param);
					if(orderList != null){
						for(Map<String, Object> map : orderList){
							Map<String, Object> info = new HashMap<String, Object>();
							Long comid = (Long)map.get("comid");
							Integer state = (Integer)map.get("state");
							Map<String, Object> comMap = pService.getMap("select company_name from com_info_tb where id=? ",
											new Object[] { comid });
							if(comMap != null){
								info.put("parkname", comMap.get("company_name"));
							}
							Long ctime = (Long)map.get("create_time");
							Integer pid = (Integer)map.get("pid");
							//Integer car_type = (Integer)map.get("car_type");//0��ͨ�ã�1��С����2����
							if(state == 0){
								if(pid>-1){
									info.put("total", Double.valueOf(publicMethods.getCustomPrice(ctime, System.currentTimeMillis()/1000, pid)));
								}else {
									info.put("total", Double.valueOf(publicMethods.getPrice(ctime, System.currentTimeMillis()/1000, (Long)map.get("comid"), car_type)));
								}
							}else if(state == 2){
								info.put("total", map.get("total"));
							}
							info.put("date", TimeTools.getTime_yyyyMMdd_HHmmss(ctime*1000));
							info.put("orderid", map.get("id"));
							info.put("state", state);
							info.put("comid", comid);
							info.put("islocked", map.get("islocked"));
							info.put("lockKey", map.get("lock_key"));
							info.put("openid", userMap.get("wxp_openid"));
							info.put("carnumber", map.get("car_number"));
							logger.error("�������:"+map.get("id")+" ֧��״̬(0δ֧��|1֧��|2�ӵ�):"+state);
							infoMapList.add(info);
						}
					}
				}*/
				if(present!=1){//����1,��ѯ�������ж���,�����߼�Ϊ��ѯ��ʷ����(�ѽ���)
					Map<String, String> params = new HashMap<String, String>();
					params.put("page", pageNum+"");
					params.put("size", pageSize+"");
					params.put("action", "historyroder");
					params.put("openid", openid);
					String result = new HttpProxy().doPost("http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/carowner.do", params);
					if(result != null && !result.equals("")){
						JSONArray jsonArray = JSONArray.fromObject(result);
						for(int i=0;i<jsonArray.size();i++){
							JSONObject jsonObject = (JSONObject)jsonArray.get(i);
							Map<String, Object> info = new HashMap<String, Object>();
							info.put("total", jsonObject.get("total"));
							info.put("date", jsonObject.get("date"));
							info.put("parkname", jsonObject.get("parkname"));
							info.put("orderid", jsonObject.get("orderid"));
							info.put("state", 1);
							infoMapList.add(info);
						}
					}
				}
			}
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMapList));
		}else if(action.equals("presentorderlist")){
			//��ѯ�����ڳ�����
			//mobile=>uin=>����=>ÿ��������ÿ������������δ���㶩��
			Integer pageNum = RequestUtil.getInteger(request, "page", 1);
			Integer pageSize = RequestUtil.getInteger(request, "size", 20);
			//String mobile = RequestUtil.processParams(request, "mobile");
			String openid = RequestUtil.processParams(request, "openid");
			if(openid.equals("")){
				return mapping.findForward("error");
			}
			List<Map<String, Object>> infoMapList = new ArrayList<Map<String,Object>>();
			Map<String, Object> userMap = pService
					.getMap("select * from user_info_tb where wxp_openid=? and auth_flag=? ",
							new Object[] { openid, 4 });
			if(userMap==null){
				return mapping.findForward("error");
			}
			Long uin = (Long) userMap.get("id");
			List<String> allCarnumbers = commonMethods.getAllCarnumbers(uin);
			for (String carnum : allCarnumbers) {
				String sql = "select * from order_tb where car_number = ? and state = ? order by create_time desc";
				List<Map> all = daService.getAll(sql, new Object[]{carnum,0});
				for (Map map : all) {
					Map<String, Object> info = new HashMap<String, Object>();
					Long comid = (Long)map.get("comid");
					Integer state = (Integer)map.get("state");
					Map<String, Object> comMap = pService.getMap("select company_name from com_info_tb where id=? ",
									new Object[] { comid });
					if(comMap != null){
						info.put("parkname", comMap.get("company_name"));
					}
					Long ctime = (Long)map.get("create_time");
					//Integer pid = (Integer)map.get("pid");
					//String car_type = (String)map.get("car_type");//0��ͨ�ã�1��С����2����
					/*if(state == 0){
						if(pid>-1){
							info.put("total", Double.valueOf(publicMethods.getCustomPrice(ctime, System.currentTimeMillis()/1000, pid)));
						}else {
							info.put("total", Double.valueOf(publicMethods.getPrice(ctime, System.currentTimeMillis()/1000, (Long)map.get("comid"), car_type)));
						}
					}else if(state == 2){
						info.put("total", map.get("total"));
					}*/
					
					//ȥ��������ʶ����ļ۸�?
					String orderId = (String) map.get("order_id_local");
					Long id = (Long) map.get("id");
					//ȥ������ѯ�����۸�,�������ݱ�ƽ̨����ת��.ȥ֧��ƽ̨��ѯ�۸�,֧��ƽ̨ȥ������ѯ�۸�,���ڱ�ƽ̨����bolink����
					//Map<String, Object> bolinkOrder = publicMethods.catBolinkOrder(null, orderId, carnum, comid+"", 2, uin);
					Map<String, Object> presentOrder = publicMethods.catPresentOrderPrice(Long.valueOf(CustomDefind.UNIONID), comid+"", carnum, orderId);
					
					/*Integer state = (Integer)orderMap.get("state");//0û���ڳ����� 1���ڳ����� 2��Ԥ����
					if(state==null)
						state = 0;
					if(state>0){
						Integer duration = (Integer)orderMap.get("duration");
						duration = duration==null?1:duration;
						Double prepay = StringUtils.formatDouble(orderMap.get("prepay"));
						Long startTime=Long.valueOf(orderMap.get("start_time")+"");
						request.setAttribute("starttime",TimeTools.getTime_MMdd_HHmm(startTime*1000));
						request.setAttribute("parktime",StringUtils.getTimeString(duration));
						request.setAttribute("beforetotal",prepay);
						request.setAttribute("aftertotal", orderMap.get("money"));
						request.setAttribute("distotal", 0);
						request.setAttribute("prestate", prepay>0?1:0);
						request.setAttribute("pretotal", prepay);
						request.setAttribute("descp", "");
						orderId= orderMap.get("order_id")+"";
						request.setAttribute("isbolink", 1);
						request.setAttribute("park_id", parkId);
					}*/
					
					if(presentOrder!=null){
						logger.error("presentorderlist>>>presentOrder:"+presentOrder);
						int status = (Integer) presentOrder.get("state");
						if(status==1){
							Double money =StringUtils.formatDouble(presentOrder.get("money"));
							info.put("status", status);
							info.put("total", money);
							info.put("prestate", 0);
						}else if(status==2){
							Double money =StringUtils.formatDouble(presentOrder.get("money"));
							Double prepay =StringUtils.formatDouble(presentOrder.get("prepay"));
							info.put("status", status);
							info.put("total", money);
							info.put("prestate", 1);
							info.put("pretotal", prepay);
						}else if(status==3){
							info.put("status", status);
							info.put("total", "δ֪");
						}else if(status==0){
							info.put("status", status);
							info.put("total", "δ֪");
						}
					}
					info.put("isthirdpay", PayConfigDefind.getValue("IS_TO_THIRD_WXPAY"));
					info.put("date", TimeTools.getTime_yyyyMMdd_HHmmss(ctime*1000));
					info.put("orderid", map.get("order_id_local"));
					info.put("id",id);
					info.put("state", state);
					info.put("comid", comid);
					info.put("islocked", map.get("islocked"));
					info.put("lockKey", map.get("lock_key"));
					info.put("openid", userMap.get("wxp_openid"));
					info.put("carnumber", carnum);
					logger.error("presentorderlist>>>"+info);
					logger.error("�������:"+map.get("id")+" ֧��״̬(0δ֧��|1֧��|2�ӵ�):"+state);
					infoMapList.add(info);
				}
			}
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMapList));
		}else if(action.equals("parkprod")){
			String openid = RequestUtil.processParams(request, "openid");
			if(openid.equals("")){
				return mapping.findForward("error");
			}
			Map<String, Object> userMap = commonMethods.getUserByOpenid(openid);
			if(userMap == null){
				return mapping.findForward("error");
			}
			request.setAttribute("domain", Constants.WXPUBLIC_REDIRECTURL);
			request.setAttribute("mobile", userMap.get("mobile"));
			request.setAttribute("openid", openid);
			return mapping.findForward("parkprod");
		}else if(action.equals("toprodlist")){
			String openid = RequestUtil.processParams(request, "openid");
			if(openid.equals("")){
				return mapping.findForward("error");
			}
			Map<String, Object> userMap = commonMethods.getUserByOpenid(openid);
			if(userMap == null){
				return mapping.findForward("error");
			}
			request.setAttribute("openid", openid);
			request.setAttribute("mobile", userMap.get("mobile"));
			return mapping.findForward("prodlist");
		}else if(action.equals("tobuyprod")){
			//�¿������߼�
			String openid = RequestUtil.processParams(request, "openid");
			String cardid = RequestUtil.getString(request, "cardid");
			Long cid = RequestUtil.getLong(request, "cid", -1l);
			Long comid = RequestUtil.getLong(request, "comid", -1l);
			String prodid = RequestUtil.getString(request, "prodid");
			Integer type = RequestUtil.getInteger(request, "type", 0);//0:���� 1������
			logger.error("tobuyprod openid:"+openid);
			if(openid.equals("")){
				return mapping.findForward("error");
			}
			Map<String, Object> userMap = commonMethods.getUserByOpenid(openid);
			logger.error("tobuyprod userMap:"+userMap);
			if(userMap == null){
				return mapping.findForward("error");
			}
			if(!prodid.equals("")&&!prodid.equals("-1")){
				Map<String, Object> pMap = daService.getMap("select p.limitday,p.p_name,c.company_name from product_package_tb p,com_info_tb c where p.comid=c.id and p.card_id=? ", 
						new Object[]{prodid});
				if(pMap == null){
					return mapping.findForward("error");
				}
				/*Object limit = pMap.get("limitday");
				if(limit!=null){
					Long exptime = Long.valueOf(limit + "");
					String expstr = TimeTools.getTimeStr_yyyy_MM_dd(exptime*1000);
					String[] expstrs = expstr.split("-");
					request.setAttribute("exptime", expstr);
					request.setAttribute("maxyear", Integer.valueOf(expstrs[0]));
					request.setAttribute("maxmonth", Integer.valueOf(expstrs[1])-1);
					request.setAttribute("maxday", Integer.valueOf(expstrs[2]));
				}*/
				request.setAttribute("exptime", -1);
				//request.setAttribute("pname", pMap.get("p_name"));
				request.setAttribute("cname", pMap.get("company_name"));
			}else{
				Map<String, Object> pMap = daService.getMap("select t.company_name as name from carower_product c,com_info_tb t where c.com_id=t.id and c.id = ?", 
						new Object[]{cid});
				request.setAttribute("cname", pMap.get("name"));
				request.setAttribute("exptime", -1);
				request.setAttribute("pname", -1);
				request.setAttribute("maxyear", 2020);
				request.setAttribute("maxmonth", 12);
				request.setAttribute("maxday", 31);
			}
			String btime = TimeTools.getDate_YY_MM_DD();
			Long uin = (Long)userMap.get("id");
			String title = "�������";
			if(type == 1){
				Map<String, Object> map = daService.getMap("select e_time from carower_product where id = ?", 
						new Object[]{cid});
				if(map != null){
					Long etime = (Long)map.get("e_time");
					if(etime > TimeTools.getToDayBeginTime()){
						btime = TimeTools.getTimeStr_yyyy_MM_dd(etime*1000);
					}
				}
				title = "��������";
			}
			String[] minstrs = btime.split("-");
			request.setAttribute("btime", btime);
			request.setAttribute("cardid", cardid);
			request.setAttribute("comid", comid);
			request.setAttribute("mobile", userMap.get("mobile"));
			request.setAttribute("prodid", prodid);
			request.setAttribute("openid", openid);
			request.setAttribute("title", title);
			request.setAttribute("minyear", Integer.valueOf(minstrs[0]));
			request.setAttribute("minmonth", Integer.valueOf(minstrs[1])-1);
			request.setAttribute("minday", Integer.valueOf(minstrs[2]));
			
			return mapping.findForward("buyprod");
		}else if(action.equals("getprodprice")){
			Long prodid = RequestUtil.getLong(request, "prodid", -1L);
			String starttime = RequestUtil.processParams(request, "starttime");
			Integer months = RequestUtil.getInteger(request, "months", 1);
			/*if(prodid == -1 || starttime.equals("")){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			Long tondaybegin = TimeTools.getToDayBeginTime();
			Long b =TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(starttime+" 00:00:00");
			if(b < tondaybegin){
				AjaxUtil.ajaxOutput(response, "-2");
				return null;
			}
			Integer r = commonMethods.checkProdExp(prodid, b, months);
			if(r == 1){
				AjaxUtil.ajaxOutput(response, "-3");
				return null;
			}*/
			//δ����,��ѯ�۸�
			Map<String, Object> map = new HashMap<String, Object>();
			//Double total= commonMethods.getProdSum(prodid, months);
			map.put("total", 100);
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(map));
		}else if(action.equals("getlocalprice")){//ͨ��sdk��ѯ�¿����ѽ��
			//Long prodid = RequestUtil.getLong(request, "prodid", -1L);
			Long comid = RequestUtil.getLong(request, "comid", -1L);
			String starttime = RequestUtil.processParams(request, "starttime");
			String cardid = RequestUtil.processParams(request, "cardid");
			Integer months = RequestUtil.getInteger(request, "months", 1);
			//ͨ��sdk��ѯ�¿���¼
			String m = months+"";
			String trade_no = TimeTools.getTimeYYYYMMDDHHMMSS()+comid+m+cardid;
			logger.error("getlocalprice trade_no:"+trade_no);
			//��֯����
			Map<String,String> params = new HashMap<String,String>();
			params.put("service_name", "query_prodprice");
			params.put("card_id", cardid);
			params.put("months",months+"");
			params.put("start_time", starttime);
			params.put("trade_no", trade_no);
			params.put("comid", comid+"");
			//���Ȳ�ѯsdkͨ�� 
			/*Map<String, Object> LoginMap= daService.getMap("select server_ip,local_id from park_token_tb where park_id = ? order by id desc limit 1", 
					new Object[]{comid+""});*/
			List<Map<String, Object>> allLogin = daService.getAll("select server_ip,local_id from park_token_tb where park_id = ?", new Object[]{comid+""});
			int size = allLogin.size();
			logger.error("getlocalprice>>>����"+comid+"��¼�ն�����:"+size);
			if(size==0){
				logger.error("getlocalprice ����δ��һ���ն˵�¼");
				Map<String, Object> map = new HashMap<String, Object>();
				map.put("total", 0.0);
				map.put("trade_no", trade_no);
				map.put("comid", comid);
				AjaxUtil.ajaxOutput(response, StringUtils.createJson(map));
				return null;
			}
			int nextInt =  new Random().nextInt(size);
			logger.error("getlocalprice ���ѡȡ�ն�:"+nextInt);
			//����ͳһ��������ն�,���ѡȡһ��
			Map<String, Object> LoginMap = allLogin.get(nextInt);
			logger.error("getlocalprice loginMap:"+LoginMap);
			logger.error("getlocalprice query order price SDK��¼��Ϣ:"+LoginMap);
			String localId = "";
			boolean isSend = false;
			String money = "0";
			if(LoginMap!=null&&!LoginMap.isEmpty()){
				localId = (String) LoginMap.get("local_id");
				params.put("local_id", localId);
				try {
					//����IP
					String localIp = Inet4Address.getLocalHost().getHostAddress().toString();
					//SDK��¼IP
					String serverIp = (String)LoginMap.get("server_ip");
					if(serverIp!=null){
							//HttpProxy httpProxy = new HttpProxy();
							//String result = httpProxy.doPost("http://"+serverIp+"/zld/sendmesg", params);
							String message = StringUtils.createLinkString(params);
							logger.error("getlocalprice message:"+message);
							String url = "http://"+serverIp+"/zld/sendmsgtopark.do?"+message;
							logger.error(url);
							String doGet = new HttpProxy().doGet(url);
							if(doGet!=null){
								org.json.JSONObject jo = new org.json.JSONObject(doGet);
								isSend = (Boolean) jo.get("state");
								logger.error("getlocalprice ��ѯ������"+isSend);
						}
					}
				} catch (Exception e) {
					logger.error(e.getMessage());
				}
			}else {
				logger.error("getlocalprice û�в鵽SDK��¼��Ϣ....");
			}
			if(isSend){
				try {
					int i = 1;
					long start = System.currentTimeMillis()/1000;
					for(long s=start;s<=start+2;s=System.currentTimeMillis()/1000){
						logger.error("getlocalprice �ӻ����ѯ�۸��"+i+"��");
						money = memcacheUtils.getCache(trade_no);
						if(money!=null){
							break;
						}
						try {
							Thread.currentThread().sleep(250);
						} catch (InterruptedException e) {
							logger.error(e.getMessage());
						}
						i++;
					}
					logger.error("getlocalprice ����sdk��ѯ�۸�,��ʼ��ѯ�����м۸�"+money);
				} catch (Exception e) {
					logger.error("getlocalprice error!!"+e.getMessage());
				}
			}
			if(money==null){
				logger.error("getlocalprice ��ȡ�¿��۸�ʧ��");
				money="0.0";
			}
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("total", money);
			map.put("trade_no", trade_no);
			map.put("comid", comid);
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(map));
			return null;
		}else if(action.equals("checkparkstatus")){
			//***********************ȥbolink��ѯ�����Ƿ���bolink��¼*******************//
			Long comid = RequestUtil.getLong(request, "comid", -1l);
			Double money = RequestUtil.getDouble(request, "money", -1D);
			String checkUrl = "https://s.bolink.club/unionapi/checkparkstatus";
			//String checkUrl = "https://127.0.0.1:8443/api-web/checkparkstatus";
			Map<String, Object> paramMap = new HashMap<String, Object>();
			paramMap.put("park_id", comid+"");
			paramMap.put("money", money);
			paramMap.put("union_id",CustomDefind.UNIONID);
			logger.error("topayprod>>>url:"+checkUrl+">>>params:"+paramMap);
			String doPost = HttpsProxy.doPost(checkUrl, StringUtils.createJson(paramMap), "UTF-8", 20000, 20000);
			JSONObject ret = JSONObject.fromObject(doPost);
			logger.error("topayprod>>>ret:"+ret);
			Map<String, Object> retMap = new HashMap<String, Object>();
			if(ret!=null){
				int state = ret.getInt("state");
				String errmsg = "";
				if(state!=1){
					errmsg = ret.getString("errmsg");
				}
				retMap.put("state", 1);
				retMap.put("errmsg", errmsg);
				//retMap.put("local_id", ret.getString("local_id"));
				logger.error("topayprod>>>����"+comid+"��¼bolink״̬:(1��¼/0����)"+state);
				AjaxUtil.ajaxOutput(response, JSONObject.fromObject(retMap).toString());
				return null;
			}else{
				logger.error("topayprod>>>�����������쳣");
				retMap.put("state", 0);
				retMap.put("errmsg", "�����쳣");
				AjaxUtil.ajaxOutput(response, JSONObject.fromObject(retMap).toString());
				return null;
			}
		}else if(action.equals("topayprod")){
			Long prodid = RequestUtil.getLong(request, "prodid", -1L);
			Long comid = RequestUtil.getLong(request, "comid", -1L);
			String starttime = RequestUtil.processParams(request, "starttime");
			String cardid = RequestUtil.processParams(request, "cardid");
			Integer months = RequestUtil.getInteger(request, "months", 1);
			String openid = RequestUtil.processParams(request, "openid");
			String trade_no = RequestUtil.processParams(request, "trade_no");
			Double thirdprice = RequestUtil.getDouble(request, "thirdprice", -1d);
			
			if(starttime.equals("") || openid.equals("")){//prodid == -1 || 
				return mapping.findForward("error");
			}
			Long tondaybegin = TimeTools.getToDayBeginTime();
			Long b =TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(starttime+" 00:00:00");
			if(b < tondaybegin){
				return mapping.findForward("error");
			}
			Integer r = commonMethods.checkProdExp(prodid, b, months);
			if(r == 1){
				return mapping.findForward("error");
			}
			Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(b*1000);
			calendar.set(Calendar.MONTH, calendar.get(Calendar.MONTH)+months);
			Long endtime = calendar.getTimeInMillis();
			Map<String, Object> userMap = commonMethods.getUserByOpenid(openid);
			if(userMap == null){
				return mapping.findForward("error");
			}
			
			Double total= commonMethods.getProdSum(prodid, months);
			Double wx_pay = 0d;
			Double balance_pay = 0d;
			logger.error("topayprod>>>openid:"+openid+",total:"+total+",uin:"+userMap.get("id")+",starttime:"+starttime+",months:"+months);
			Double balance = Double.valueOf(userMap.get("balance") + "");//�û����
			boolean isThirdPay = PayConfigDefind.getValue("IS_TO_THIRD_WXPAY").equals("1");
			if(isThirdPay){
				logger.error("topayprod>>>��������ѯ�¿��۸�");
				//��bolink�ӿڲ�ѯ�۸�
				wx_pay = thirdprice;
			}else{
				logger.error("topayprod>>>���ز�ѯ�¿��۸�");
				if(total > balance){
					balance_pay = balance;//���ȫ������֧��
					wx_pay = StringUtils.formatDouble(total - balance_pay);
				}else{
					balance_pay = total;
				}
			}
			
			//****************************��ȡ��������************************//
			/*Map parkMap = daService.getMap("select company_name from com_info_tb where id = ?", new Object[]{comid});
			String parkName  = "";
			if(parkMap!=null){
				parkName = (String) parkMap.get("company_name");
			}
			logger.error("parkname:"+parkName);*/
			//***********************************************************//
			String unionid = CustomDefind.UNIONID;
			logger.error("topayprod �¿����ѽ��:"+wx_pay);
			//wx_pay=0.01d;
			if(wx_pay > 0){
				try {
					Map<String, Object> attachMap = new HashMap<String, Object>();
					if(isThirdPay){
						attachMap.put("type", 11);//��������ֵ���������
					}else{
						attachMap.put("type", 2);//���س�ֵ���������
						attachMap.put("prodid", prodid);
					}
					attachMap.put("params", starttime+"__"+months+"__"+trade_no+"__"+wx_pay+"__"+comid+"__"+unionid+"__"+cardid);
					String backurl = "http://"+PayConfigDefind.getValue("WXPUBLIC_S_DOMAIN")+"/zld/wxpfast.do?action=thirdsuccess1";
					//��������
					String attach = StringUtils.createJson(attachMap);
					logger.error("topayprod>>>attch:"+attach+" ����:"+attach.length());
					Map<String, String> paramsMap = new HashMap<String, String>();
					paramsMap.put("attch", attach);
					paramsMap.put("unionid", unionid);
					paramsMap.put("fee", wx_pay+"");
					String params = StringUtils.createLinkString(paramsMap);
					//ǩ��
					String sign =  StringUtils.MD5(params+"key="+CustomDefind.UNIONKEY).toUpperCase();
					
					if(isThirdPay){
						//������֧��,��ת������
						//д��,�¿�֧����ˮ
						Map map = daService.getMap("select car_number,member_id from carower_product where card_id = ?", new Object[]{cardid});
						String carnums = "";
						String memberid = "";
						if(map!=null){
							carnums = (String) map.get("car_number");
							memberid = (String) map.get("member_id");
						}
						int update = daService.update("insert into card_renew_tb(trade_no,card_id,amount_receivable,pay_type,car_number,buy_month,comid,create_time,user_id,collector) values(?,?,?,?,?,?,?,?,?,?)", 
								new Object[]{trade_no,cardid,wx_pay,"΢�Ź��ں�",carnums,months,comid,System.currentTimeMillis()/1000,memberid,"΢�Ź��ں�"});
						logger.error("topayprodд�¿�������ˮ:"+update);
						String url = "https://s.bolink.club/unionapi/prepay?fee="+wx_pay+"&unionid="+unionid+"&sign="+sign+"&backurl="+backurl+"&attch="+attach;
						//String url = "http://jarvisqh.vicp.io/api-web/prepay/?fee="+wx_pay+"&unionid="+unionid+"&sign="+sign+"&park_name="+AjaxUtil.encodeUTF8(parkName)+"&backurl="+backurl+"&attch="+attach;
						logger.error("topayprod>>>�¿�url:"+url);
						response.sendRedirect(url);
						return null;
					}else{
						//��ƽ̨֧��
						//����֧������
						SortedMap<Object, Object> signParams = new TreeMap<Object, Object>();
						//��ȡJSAPI��ҳ֧������
						signParams = PayCommonUtil.getPayParams(request.getRemoteAddr(), wx_pay, "�������", attach, openid);
						request.setAttribute("appid", signParams.get("appId"));
						request.setAttribute("nonceStr", signParams.get("nonceStr"));
						request.setAttribute("package", signParams.get("package"));
						request.setAttribute("packagevalue", signParams.get("package"));
						request.setAttribute("timestamp", signParams.get("timeStamp"));
						request.setAttribute("paySign", signParams.get("paySign"));
						request.setAttribute("signType", signParams.get("signType"));
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			logger.error("topayprod>>>openid:"+openid+",total:"+total+",uin:"+userMap.get("id")+",wx_pay:"+wx_pay+",balance_pay:"+balance_pay);
			request.setAttribute("total", total);
			request.setAttribute("balance_pay", balance_pay);
			request.setAttribute("wx_pay", wx_pay);
			request.setAttribute("starttime", starttime);
			request.setAttribute("endtime", TimeTools.getTimeStr_yyyy_MM_dd(endtime));
			request.setAttribute("months", months);
			request.setAttribute("openid", openid);
			request.setAttribute("mobile", userMap.get("mobile"));
			request.setAttribute("prodid", prodid);
			return mapping.findForward("payprod");
		}else if(action.equals("lockcar")){
			int ret = -2;
			logger.error(">>>>>>>������������");
			Integer lockstatus = RequestUtil.getInteger(request, "lockstatus", -1);
			Long id = RequestUtil.getLong(request, "orderid", -1L);
			logger.error(">>>>>>>��������(1����/0����):"+lockstatus+" ��ƽ̨�������:"+id);
			if(id==-1||lockstatus==-1){
				logger.error(">>>>>>>lockstatus��orderidΪ��");
				return mapping.findForward("error");
			}
			Integer lockKey = -1;
			//0.��ǰ״̬,�����2,����ʾ������,;�����4,����ʾ������
			String ip = "";
			String localId = null;
			String parkid = "";
			String orderid = "";
			Integer islocked = -1;
			//1.��ѯ������Ϣ
			Map<String, Object> orderMap = pService.getMap("select * from order_tb where id = ?", new Object[]{id});
			if(orderMap!=null){
				orderid = (String) orderMap.get("order_id_local");
				if(orderMap.get("islocked")!=null){
					islocked = (Integer) orderMap.get("islocked");
				}
				Object object = orderMap.get("lock_key");
				if(object!=null){
					lockKey = Integer.valueOf((String)object);
				}
				Long comid = (Long) orderMap.get("comid");
				parkid = String.valueOf(comid);
			}else{
				//����������
				logger.error(">>>>>>>����"+id+"������");
				return mapping.findForward("error");
			}
			
			//2.��ѯ��������״̬
			//Map<String, Object> parkTokenMap = pService.getMap("select server_ip,local_id from park_token_tb where park_id = ?", new Object[]{parkid});
			List<Map<String, Object>> allLogin = daService.getAll("select server_ip,local_id from park_token_tb where park_id = ?", new Object[]{parkid});
			int size = allLogin.size();
			if(size==0){
				logger.error("getlocalprice ����δ��һ���ն˵�¼");
				ret = 9;
				String json = "{\"state\":"+ret+",\"lockKey\": "+lockKey+"}";
				logger.error(json);
				AjaxUtil.ajaxOutput(response, json);
				return null;
			}
			int nextInt = new Random().nextInt(size);
			logger.error("���ѡȡ�ն�:"+nextInt);
			//����ͳһ��������ն�,���ѡȡһ��
			Map<String, Object> parkTokenMap = allLogin.get(nextInt);
			if(parkTokenMap!=null){
				if(parkTokenMap.get("server_ip")!=null){
					ip = (String) parkTokenMap.get("server_ip");
					localId = (String) parkTokenMap.get("local_id");
					logger.error("ת�����нӿڵ�ַ:"+ip);
					//ip="localhost";
					//ip="106.75.108.6";
				}else{
					//����������
					ret= 9;
				}
				logger.error("server_ip:"+ip);
			}else{
				ret = 9;
				logger.error("server_ip:"+ip);
			}
			
			if(ret==9){
				//ret=10;
				logger.error(">>>>>>>����"+parkid+"������");
			}
			
			//3.���������
			if(ret!=9){
				if(lockstatus==1){
					//����
					if(islocked==0||islocked==3){
						//��������
						logger.error(">>>>>>>��������,������...");
						lockKey = (int) (Math.random()*9000+1000);
						int lockcar = lockcar(id, lockstatus,lockKey);
						if(lockcar>0){
							//�޸ĳɹ�
							//��������
							//ret=1;
							ret = sendmsgtopark(ip, orderid, lockstatus, parkid, id, lockKey,localId);
						}else{
							//�޸�ʧ��
							logger.error("�޸Ķ�������״̬ʧ��");
							ret = 3;
						}
					}else if(islocked==2){
						//��������
						logger.error(">>>>>>>��������,����������...");
						int lockcar = lockcar(id, lockstatus,lockKey);
						if(lockcar>0){
							//�޸ĳɹ�
							//��������
							//ret=1;
							ret = sendmsgtopark(ip, orderid, lockstatus, parkid, id, lockKey,localId);
						}else{
							//�޸�ʧ��
							logger.error("�޸Ķ�������״̬ʧ��");
							ret = 3;
						}
					}else{
						//����������
						ret = 6;
					}
				}else if(lockstatus==0){
					//����
					if(islocked==1||islocked==4||islocked==5){
						//���Խ���
						logger.error(">>>>>>>���Խ���,������...");
						int lockcar = lockcar(id, lockstatus,null);
						if(lockcar>0){
							//�޸ĳɹ�
							//��������
							//ret=0;
							ret = sendmsgtopark(ip, orderid, lockstatus, parkid, id, null,localId);
						}else{
							//�޸�ʧ��
							logger.error("�޸Ķ�������״̬ʧ��");
							ret = 5;
						}
					}else{
						//����δ����
						ret = 7;
					}
				}
			}
			
			//ret: -2ϵͳ�쳣 -1֪ͨ����ʧ��  0�����ɹ�  1�����ɹ�  3����ʧ�� 5����ʧ�� 6������ 7δ���� 9��������
			String json = "{\"state\":"+ret+",\"lockKey\": "+lockKey+"}";
			logger.error(json);
			AjaxUtil.ajaxOutput(response, json);
			return null;
		}
		return null;
	}
	
	/**
	 * �򳵳�����֪ͨ
	 * @return
	 */
	@SuppressWarnings("static-access")
	private int sendmsgtopark(String ip,String orderid,Integer lockstatus,String parkid,Long id,Integer lockKey,String localId){
		int ret = -1;
		//5.֪ͨ�շѶ�
		String url = "http://"+ip+":8080/zld/sendmsgtopark.do?service_name=lock_car&order_id="+orderid+"&is_locked="+lockstatus+"&comid="+parkid+"&id="+id+"&local_id="+localId;
		if(lockKey!=null){
			url += "&lock_key="+lockKey;
		}
		logger.error(">>>>>>>����֪ͨ:url:"+url);
		String doGet = new HttpProxy().doGet(url);
		logger.error(doGet);
		if(doGet!=null){
			Boolean state = false;
			try {
				org.json.JSONObject jo = new org.json.JSONObject(doGet);
				state = (Boolean) jo.get("state");
			} catch (JSONException e) {
				logger.error(e.getMessage());
			}
			if(state){
				//֪ͨ����ɹ�
				int i = 1;
				long start = System.currentTimeMillis()/1000;
				for(long s=start;s<=start+2;s=System.currentTimeMillis()/1000){
					Map<String, Object> orderMap = pService.getMap("select * from order_tb where id = ?", new Object[]{id});
					if(orderMap!=null){
						//locksatatus=1,���islockedΪ1,�������ɹ�;���islockedֵΪ2,3,������ʧ��"
						//locksatatus=0,���islockedΪ0,������ɹ�;���islockedֵΪ4,5,�����ʧ��
						logger.error(">>>>>>>��ѯ����״̬��"+i+"��");
						Integer islock = (Integer) orderMap.get("islocked");
						if(lockstatus==1){
							if(islock==1){
								//�����ɹ�
								logger.error("�����ɹ�!��������:"+lockKey);
								ret = 1;
								break;
							}else if(islock==2||islock==3){
								//����ʧ��
								logger.error("����ʧ��");
								ret = 3;
							}
						}else if(lockstatus==0){
							if(islock==0){
								//�����ɹ�
								logger.error("�����ɹ�!");
								ret = 0;
								break;
							}else if(islock==4||islock==5){
								//����ʧ��
								logger.error("����ʧ��");
								ret = 5;
							}
						}
					}
					try {
						Thread.currentThread().sleep(300);
					} catch (InterruptedException e) {
						logger.error(e.getMessage());
					}
					i++;
				}
			}else{
				//֪ͨ����ʧ��
				logger.error(">>>>>>>֪ͨ����ʧ��");
			}
		}
		return ret;
	}
	
	/**
	 * �޸ĳ�������״̬
	 * @return
	 */
	private int lockcar(Long id,Integer lockstatus,Integer key){
		int ret = -1;
		String sql = "";
		if(lockstatus==1){
			//����
			sql = "update order_tb set islocked = ?, lock_key = ? where id=?";
			ret = daService.update(sql, new Object[]{2,key,id});
		}else if(lockstatus==0){
			//����
			sql = "update order_tb set islocked = ? where id=?";
			ret = daService.update(sql, new Object[]{4,id});
		}
		return ret;
	}
	
	/*
	 * �ϴ���֤ͼƬ
	 */
	private String uploadCheck2Mongodb (String accessToken, String mediaId,Long uin,String carnumber,int index) throws Exception{
		String requestUrl = "http://file.api.weixin.qq.com/cgi-bin/media/get?access_token=ACCESS_TOKEN&media_id=MEDIA_ID";  
        requestUrl = requestUrl.replace("ACCESS_TOKEN", accessToken).replace("MEDIA_ID", mediaId);  
		logger.error("upload to mongodb>>>uin:"+uin+",mediaId:"+mediaId+",index:"+index);
		Map<String, String> extMap = new HashMap<String, String>();
	    extMap.put("image/jpeg", ".jpg");
	    extMap.put("image/png", ".png");
	    extMap.put("image/gif", ".gif");
	    extMap.put("video/mpeg4", ".mp4");
		URL url = new URL(requestUrl);  
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();  
        conn.setDoInput(true);  
        conn.setRequestMethod("GET");
        String ctype = conn.getHeaderField("Content-Type");
		InputStream is = conn.getInputStream(); // ��ǰ�ϴ��ļ���InputStream����
		String file_ext =extMap.get(ctype);// ��չ��
		logger.error("upload carnumber pics>>>uin:"+uin+",carnumber:"+carnumber+",file_ext:"+file_ext+",index:"+index);
		if(file_ext != null && !file_ext.equals("") && is != null){
			String picurl = uin + "_"+index+"_"+ System.currentTimeMillis()/1000 + file_ext;
			BufferedInputStream in = null;
			ByteArrayOutputStream byteout =null;
		    try {
		    	in = new BufferedInputStream(is);   
		    	byteout = new ByteArrayOutputStream(1024);        	       
			      
		 	    byte[] temp = new byte[1024];        
		 	    int bytesize = 0;        
		 	    while ((bytesize = in.read(temp)) != -1) {        
		 	          byteout.write(temp, 0, bytesize);        
		 	    }        
		 	      
		 	    byte[] content = byteout.toByteArray(); 
		 	    DB mydb = MongoClientFactory.getInstance().getMongoDBBuilder("zld");
			    mydb.requestStart();
				  
			    DBCollection collection = mydb.getCollection("user_dirvier_pics");
				  
				BasicDBObject document = new BasicDBObject();
				document.put("uin", uin);
				document.put("carid", carnumber);
				document.put("ctime",  System.currentTimeMillis()/1000);
				document.put("type", extMap.get(file_ext));
				document.put("content", content);
				document.put("filename", picurl);
				  //��ʼ����
				mydb.requestStart();
				collection.insert(document);
				  //��������
				mydb.requestDone();
				in.close();        
			    is.close();
			    byteout.close();
			    
			    String sql = "update car_info_tb set pic_url1=?,is_auth=?,create_time=?  where uin=? and car_number=?";
				if(index==1){
					sql = "update car_info_tb set pic_url2=?,is_auth=?,create_time=?  where uin=? and car_number=?";
				}
				int ret = daService.update(sql, new Object[]{picurl,2,System.currentTimeMillis()/1000,uin,carnumber});
				logger.error("�ϴ���"+index+"��ͼƬ��ret:"+ret+",uin:"+uin+"carnumber:"+carnumber+",mediaid:"+mediaId);
			} catch (Exception e) {
				e.printStackTrace();
				return "-1";
			}finally{
				if(in!=null)
					in.close();
				if(byteout!=null)
					byteout.close();
				if(is!=null)
					is.close();
			}
		}
		
		return "1";
	}
	
	/** 
     * ��ȡý���ļ� 
     * @param accessToken �ӿڷ���ƾ֤ 
     * @param media_id ý���ļ�id 
     * @param savePath �ļ��ڷ������ϵĴ洢·�� 
     * */  
    public void previewpic(String accessToken, String mediaId, HttpServletResponse response) {
        // ƴ�������ַ  
        String requestUrl = "http://file.api.weixin.qq.com/cgi-bin/media/get?access_token=ACCESS_TOKEN&media_id=MEDIA_ID";  
        requestUrl = requestUrl.replace("ACCESS_TOKEN", accessToken).replace("MEDIA_ID", mediaId);  
        System.out.println(requestUrl);  
        try {  
            URL url = new URL(requestUrl);  
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();  
            conn.setDoInput(true);  
            conn.setRequestMethod("GET");  
            
            BufferedInputStream in = null;  
    		ByteArrayOutputStream byteout =null;
            in = new BufferedInputStream(conn.getInputStream());   
	    	byteout = new ByteArrayOutputStream(1024);        	       
		      
	 	    byte[] temp = new byte[1024];        
	 	    int bytesize = 0;        
	 	    while ((bytesize = in.read(temp)) != -1) {        
	 	          byteout.write(temp, 0, bytesize);        
	 	    }
	 	    byte[] content = byteout.toByteArray(); 
            response.setDateHeader("Expires", System.currentTimeMillis()+4*60*60*1000);
			Calendar c = Calendar.getInstance();
			c.set(1970, 1, 1, 1, 1, 1);
			response.setHeader("Last-Modified", c.getTime().toString());
			response.setContentLength(content.length);
			response.setContentType("image/jpeg");
		    OutputStream o = response.getOutputStream();
		    o.write(content);
		    o.flush();
		    o.close();
		    response.flushBuffer();
        } catch (Exception e) {  
            logger.error("preview picture fail>>>serverid:"+mediaId);
        }  
    } 
    
	/**
	 * ��Ϣ����
	 * 
	 * @param mesg
	 * @param data
	 */
	private boolean doBackMessage(String mesg, Channel channel) {
		if (channel != null && channel.isActive()
				&& channel.isWritable()) {
			try {
				logger.error("����Ϣ��SDK��channel:"+channel+",mesg:" + mesg);
				byte[] req= ("\n" + mesg + "\r").getBytes("utf-8");
				ByteBuf buf = Unpooled.buffer(req.length);
				buf.writeBytes(req);
				ChannelFuture future = channel.writeAndFlush(buf);
				return true;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}else{
			logger.error("�ͻ����ѶϿ�����...");
		}
		return false;
	}
}
