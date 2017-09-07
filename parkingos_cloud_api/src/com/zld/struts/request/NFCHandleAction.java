package com.zld.struts.request;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;

import pay.Constants;

import com.zld.AjaxUtil;
import com.zld.CustomDefind;
import com.zld.impl.CommonMethods;
import com.zld.impl.MemcacheUtils;
import com.zld.impl.PublicMethods;
import com.zld.pojo.AutoPayPosOrderReq;
import com.zld.pojo.AutoPayPosOrderResp;
import com.zld.pojo.Order;
import com.zld.service.DataBaseService;
import com.zld.service.LogService;
import com.zld.service.PayPosOrderService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.Check;
import com.zld.utils.CountPrice;
import com.zld.utils.RequestUtil;
import com.zld.utils.SendMessage;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
import com.zld.wxpublic.util.PayCommonUtil;

public class NFCHandleAction extends Action {
	
	
	private Logger logger = Logger.getLogger(NFCHandleAction.class);
	@Autowired
	private DataBaseService daService;
	@Autowired
	private LogService logService;
	@Autowired
	private PgOnlyReadService onlyReadService;
	@Autowired
	private PublicMethods publicMethods;
	@Autowired
	private CommonMethods methods;
	@Autowired
	private MemcacheUtils memcacheUtils;
	@Autowired
	@Resource(name = "bolinkPay")
	private PayPosOrderService payBolinkService;
	/**
	 * ����ֵ��
	 * 0������ȷ��
	 * 1:�����ɶ���
	 * 2��
	 */
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		Long ntime = System.currentTimeMillis()/1000;
		//����
		String action = RequestUtil.getString(request, "action");
		//NFCuuid
		String uuid = RequestUtil.getString(request, "uuid").trim();
		//NFCnid
		Long nid = RequestUtil.getLong(request, "nid",0L);
		//ͣ�������
		Long comId = RequestUtil.getLong(request, "comid", -1L);
		//�շ�Ա���
		Long uid = RequestUtil.getLong(request, "uid", -1L);
		
//		if(action.equals("testfee")){
//			Double fee = RequestUtil.getDouble(request, "total", 0d);
//			AjaxUtil.ajaxOutput(response, StringUtils.createJson(publicMethods.useTickets(21515L, fee)));
//		}
		 
		logger.error("action="+action+",NFC:"+uuid+",comid:"+comId+",uid:"+uid+",nid:"+nid);
		if(comId==-1&&!action.equals("regnfc")&&!action.equals("coswipe")&&!action.equals("reguser")&&!action.equals("queryvalidate")){
			logger.error("û�е�¼....");
			AjaxUtil.ajaxOutput(response, "info:���¼��ʹ��!");
			return null;
		}
		if(uuid.equals("")&&!action.equals("completeorder")&&nid.equals("")){
			AjaxUtil.ajaxOutput(response, "info:���Ŵ���������ˢ��");
			return null;
		}
		//ע��NFC��Ϣ
		if(action.equals("regnfc")){
			int result =0;
			String code = RequestUtil.getString(request, "code");
			Long auid = RequestUtil.getLong(request, "uid", -1L);
			logger.error("regnfc---->>>>>qrcode:"+code+",uid:"+auid);
			Long comid = null;
			if(auid!=-1){
				Map usrMap  = daService.getMap("select comid from user_info_tb where id=? ", new Object[]{auid});
				if(usrMap!=null&&usrMap.get("comid")!=null)
					comid  =(Long)usrMap.get("comid");
				if(comid==null||comid==-1){
					AjaxUtil.ajaxOutput(response,  "info:�շ�Ա��Ŵ���!");
					return null;
				}
				uuid = comid+"_"+uuid;
				logger.error("regnfc---->>>>>qrcode:"+code+",uid:"+auid+",uuid:"+uuid);
			}
			try {
				if(!code.equals("")){//ɾ��ԭ��ά��
					int ret = daService.update("delete from com_nfc_tb where qrcode=?", new Object[]{code});
					logger.error("regnfc---->>>>>qrcode:"+code+",uid:"+auid+",uuid:"+uuid+">>>>delete code:"+code+",ret:"+ret);
				}
				result= daService.update("insert into com_nfc_tb (nfc_uuid,create_time,state,nid,qrcode)" +
						" values(?,?,?,?,?)", new Object[]{uuid,System.currentTimeMillis()/1000,0,nid,code});
				logger.error("regnfc---->>>>>qrcode:"+code+",uid:"+auid+",uuid:"+uuid+",д�����ݿ⣺"+result);
			} catch (Exception e) {
				e.printStackTrace();
				logger.error("regnfc---->>>>>qrcode:"+code+",uid:"+auid+",uuid:"+uuid+",д�����"+e.getMessage());
				if(e.getMessage().indexOf("com_nfc_tb_nfc_uuid_key")!=-1){
					if(nid ==0){
						if(code.equals("")){
							logger.error("add 1");
							AjaxUtil.ajaxOutput(response,  "info:NFC����ע���");
							logger.error("NFC����ע���....");
							return null;
						}else {
							logger.error("2");
							result =  daService.update("update com_nfc_tb set state=?,qrcode=? where nfc_uuid = ? ", 
									new Object[]{0,code,uuid});
						}
					}else {
						logger.error("3");
						result = daService.update("update com_nfc_tb set nid=?,state=?,qrcode=? where nfc_uuid = ? ", 
								new Object[]{nid,0,code,uuid});
					}
				}
				//e.printStackTrace();
			}
			if(result==1){
				AjaxUtil.ajaxOutput(response,  "info:NFC��ע��ɹ�!");
				logger.error("regnfc---->>>>>qrcode:"+code+",uid:"+auid+",uuid:"+uuid+",��ע��ɹ�");
			}else {
				AjaxUtil.ajaxOutput(response,  "info:NFC��ע��ʧ�ܣ����Ժ�����");
				logger.error("regnfc---->>>>>qrcode:"+code+",uid:"+auid+",uuid:"+uuid+",��ע��ʧ�ܣ����Ժ�����!");
			}
			//http://127.0.0.1/zld/nfchandle.do?action=regnfc&uuid=0428C302773480
		}else if(action.equals("queryvalidate")){
			
			String code = RequestUtil.getString(request, "code");
			logger.error("queryvalidate---->>>>>code:"+code+",uid:"+uid);
			long ret = 0;
			if(uid!=-1&&code!=null&&!code.equals("")){
				Map usrMap  = daService.getMap("select comid from user_info_tb where id=? ", new Object[]{uid});
				Long comid = -1L;
				if(usrMap!=null&&usrMap.get("comid")!=null)
					comid  =(Long)usrMap.get("comid");
				if(comid==null||comid==-1){
					AjaxUtil.ajaxOutput(response,  "info:�շ�Ա��Ŵ���!");
					return null;
				}
				uuid = comid+"_"+uuid;
				logger.error("queryvalidate---->>>>>code:"+code+",uid:"+uid+",uuid:"+uuid);
				ret = daService.getLong("select count(*) from com_nfc_tb where nfc_uuid = ? and qrcode=? and state = ? ", 
						new Object[]{uuid,code,0});
			}
			logger.error(ret);
			AjaxUtil.ajaxOutput(response, ret+"");
			//���أ�1�ɹ���-1��ʧ�ܣ��복����ע������ӳ��ƺ��ٰ�
			//http://127.0.0.1/zld/nfchandle.do?action=queryvalidate&uuid=041BC402773480&code=&uid=&comid
		}else if(action.equals("reguser")){//NFC���󶨳���
			//http://127.0.0.1/zld/nfchandle.do?action=reguser&uuid=041BC402773480&carnumber=��JAQ216&rgtype=&dtype=
			String carNumber = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "carnumber"));
			carNumber = carNumber.toUpperCase().trim().replace("I", "1").replace("O", "0");
			String mobile = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "mobile"));
			String result = "{\"result\":\"-1\",\"info\":\"����ע���û�����ӳ��ƺ��ٷ�����\"}";
			if(!mobile.equals("")){
				String r = reguser(mobile,carNumber,uid,comId);
				if(!r.equals("0")){
					result= "{\"result\":\"-7\",\"info\":\""+r+"\"}";
					logger.error(result);
					AjaxUtil.ajaxOutput(response, result);
					return null;
				}else {
					//���Ͷ��Ÿ��������𾴵ĳ�����N2211���ã�����2014��9��18��15:35���ڲݷ�ͣ������ͨͣ������ͨ�֣ɣп���ƾ�˿�����������ͣ�����ģΣƣÿ���������ͨ�С�
					Map comMap = daService.getMap("select company_name from com_info_tb where id=?",new Object[]{comId});
					String content = "�𾴵ĳ���"+carNumber+"���ã�����"+TimeTools.getTime_yyyyMMdd_HHmm(System.currentTimeMillis());
					if(comMap!=null&&comMap.get("company_name")!=null)
						content+="��"+comMap.get("company_name");
					content+="��ͨͣ������ͨVIP����ƾ�˿�����������ͣ������NFC����������ͨ�С�ͣ������";
					SendMessage.sendMultiMessage(mobile, content);
				}
			}
			String rgtype = RequestUtil.getString(request, "rgtype");//�Ƿ����°󶨵���������,0��գ���1:��
			String dtype = RequestUtil.getString(request, "dtype");//�Ƿ�ɾ��ԭ�󶨵ĳ���,0��գ���1:��
			//carNumber = carNumber.trim().toUpperCase();
			Map carMap = daService.getMap("select uin from car_info_Tb where car_number=?",new Object[]{carNumber});
			if(carMap!=null&&carMap.get("uin")!=null){//���ƺŴ��ڰ��û�
				//����NFC��ע���
				Map nfcMap = daService.getMap("select * from com_nfc_tb where nfc_uuid=? ", new Object[]{uuid});
				if(nfcMap==null){
					result= "{\"result\":\"-3\",\"info\":\"NFCδע��!\"}";
				}else {
					Long uin = (Long)nfcMap.get("uin");
					if(uin!=null&&uin>0&&!rgtype.equals("1")&&!dtype.equals("1")){//�Ѱ󶨳���,rgtype���Ƿ����°󶨵���������,0��գ���1:��
						//carMap = daService.getMap("select car_number from car_info_tb where uin = ? ", new Object[]{uin});
						String _carNumber = publicMethods.getCarNumber(uin);// carMap.get("car_number")+"";
						if(carNumber.equals(_carNumber)){//��ǰ�����Ѿ�����ЩNFC��
							result= "{\"result\":\"-6\",\"info\":\"NFC�Ѱ󶨵�ǰ������"+carNumber+"\"}";
							publicMethods.updateUinUuidMap(uuid, uin);
						}else {
							result= "{\"result\":\"-4\",\"info\":\"NFC�Ѱ󶨹�������"+_carNumber+",�Ƿ����°��³���\"}";
						}
					}else {//δ�󶨳���
						int ret =0;
						if(dtype.equals("1")){//�Ƿ�ɾ��ԭ�󶨵ĳ���,0��գ���1:��
							try {
								//ɾ��NFC��ԭ�󶨵�ԭ����
								daService.update("update com_nfc_tb set uin =?,state=?  where uin=? ", new Object[]{-1,1,carMap.get("uin")});
								//ɾ���ֳ���ԭ�󶨵�NFC��
								daService.update("update com_nfc_tb set uin =?,state=?  where nfc_uuid=? ", new Object[]{-1,1,uuid});
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						Long count = daService.getLong("select count(id) from com_nfc_tb where uin = ?", new Object[]{carMap.get("uin")});
						if(count>0){
							result= "{\"result\":\"-2\",\"info\":\"�����Ѱ󶨹������°󶨺�ԭ����Ч\"}";
						}else {
							ret = daService.update("update com_nfc_tb set uin =?,state=?,update_time=?,comid=?,uid=? where nfc_uuid=? ", 
									new Object[]{carMap.get("uin"),0,System.currentTimeMillis()/1000,comId,uid,uuid});
							if(ret==1){
								result= "{\"result\":\"1\",\"info\":\"�󶨳ɹ���������"+carNumber+"\"}";
								publicMethods.updateUinUuidMap(uuid,(Long)carMap.get("uin"));
							}
						}
					}
				}
			}else {//���ƺ�δ���û�
				//���ش�����Ϣ����ʾע���û�����ӳ��ƺ��ٷ�����
			}
			logger.error(result);
			AjaxUtil.ajaxOutput(response, result);
			//���أ�1�ɹ���-1��ʧ�ܣ��복����ע������ӳ��ƺ��ٰ�
			//http://127.0.0.1/zld/nfchandle.do?action=reguser&uuid=041BC402773480&carnumber=��JAQ216&rgtype=&dtype=
		}else if(action.equals("nfcincom")){//ˢ��NFC
			if(!memcacheUtils.addLock(uuid+"nfcincom",1)){
				logger.error("lock:"+uuid+",2�����ˢ��.");
				AjaxUtil.ajaxOutput(response, "2�����ˢ��");
				return null;
			}
			String ptype = RequestUtil.getString(request, "ptype");//V1115�汾���ϼ������������ʵ�ְ��²�Ʒ����۸���Ե�֧��
			//logger.error(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>ptype:"+ptype);
			Long count  = daService.getLong("select count(*) from com_nfc_tb where nfc_uuid=? and state=?", 
					new Object[]{uuid,0});
			if(count==0){
				logger.error("NFCˢ��...���ţ�"+uuid+",δע��....");
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			//��ѯ�Ƿ��ж���
			logger.error("NFCˢ��...���ţ�"+uuid);
			
			Map orderMap = onlyReadService.getMap("select * from order_tb where comid=? and nfc_uuid=? and state=?", 
					new Object[]{comId,uuid,0});
			
			//logger.error("NFCˢ��...���ţ�"+uuid);
			if(orderMap!=null&&orderMap.get("comid")!=null){//�ж���������
				Long orderId = (Long)orderMap.get("id");
				String result="-2";
				try {
					//V1115�汾���ϼ������������ʵ�ְ��²�Ʒ����۸���Ե�֧��
					if(ptype.equals("1"))
						result = publicMethods.getOrderPrice(comId, orderMap, ntime);
					else {//�Ͽͻ��˼۸�
						result = publicMethods.handleOrder(comId, orderMap);
					}
				} catch (Exception e) {
					logger.error("NFCˢ��.���㶩�����󣬶�����ţ�"+orderMap.get("id")+",comid:"+comId);
					e.printStackTrace();
				}
				logger.error("NFCˢ��.���㶩����������ţ�"+orderMap.get("id")+",comid:"+comId);
				//{total=2.0, duration=7����, etime=15:14, btime=15:06, uin=-1, orderid=27475, collect=2.0, discount=0.0}
				System.out.print(result);
				AjaxUtil.ajaxOutput(response, result);
			}else {//û�ж������������ɶ�����Ϣ
				logger.error("NFCˢ��.�������ɶ���");
				//���Ƴ������볡ʱ���ؼ۸�
				String pid = CustomDefind.CUSTOMPARKIDS;
				if(pid.equals(comId.toString())){
					AjaxUtil.ajaxOutput(response, "1");
				}else {
					AjaxUtil.ajaxOutput(response, "0");
				}
			}
			return null;
			//ˢ��NFC
			//http://192.168.199.240/zld/nfchandle.do?action=nfcincom&uuid=048D8A4A9A3D81&comid=1197&ptype=1
		}else if(action.equals("incom")){//ˢ���½ӿڣ����Ӳ�ѯ�ӵ������ν���ֱ�ӷ��ؼ۸��� 20150204
			if(!memcacheUtils.addLock(uuid+"nfcincom",1)){
				logger.error("lock:"+uuid+",2�����ˢ��.");
				AjaxUtil.ajaxOutput(response, "2�����ˢ��");
				return null;
			}
			//http://192.168.199.240/zld/nfchandle.do?action=incom&uuid=0468814A9A3D81&comid=3
			logger.error("NFCˢ��...���ţ�"+uuid);
			String from = RequestUtil.getString(request, "from");//��Դ����ɨ�������
			if(from.equals("")){//���Ǵ�ɨ���ά�������
				Long count  = daService.getLong("select count(*) from com_nfc_tb where nfc_uuid=? and state=? ", 
						new Object[]{uuid,0});
				if(count==0){
					logger.error(">>>nfc invalid �������ڡ�������"+uuid);
					AjaxUtil.ajaxOutput(response, "{\"info\":\"-1\"}");
					return null;
				}
			}
			String result = "";
			//�ж����ɻ���㶩��
			
			//��ѯ�Ƿ��ж���
			Map orderMap = onlyReadService.getMap("select * from order_tb where comid=? and nfc_uuid=? and state=?", 
					new Object[]{comId,uuid,0});
			
			try {
				if(orderMap!=null){//�����΢��Ԥ���ѵĶ����������Ƽ���
					Double prefee = StringUtils.formatDouble(orderMap.get("total"));
					logger.error(">>>>>>>>>>>>>>orderid:"+orderMap.get("id")+",prefee:"+prefee);
					if(prefee>0){//������Ԥ֧���������Ƽ���
						Long preUin = (Long)orderMap.get("uin");
						logger.error(">>>>>>>>>>>>����Ԥ֧������uin:"+preUin+",orderid"+orderMap.get("id"));
						if(preUin!=null&&preUin>0){
							//���Ƿ�󶨹�����΢�ź�
							Map wxpMap=daService.getMap("select wxp_openid from user_info_tb  where id =? ", new Object[]{preUin}); 
							String openid = null;
							if(wxpMap!=null&&wxpMap.get("wxp_openid")!=null){
								openid = (String)wxpMap.get("wxp_openid");
							}else{
								Map wxpuserMap = daService.getMap("select openid from wxp_user_tb  where uin =? ", new Object[]{preUin}); 
								if(wxpuserMap!=null)
									openid = (String)wxpuserMap.get("openid");
							}
							List<Object> params = new ArrayList<Object>();
							String recomsql = "select count(ID) from recommend_tb where nid=? ";
							params.add(preUin);
							if(openid != null){
								recomsql += " or openid=? ";
								params.add(openid);
							}
							//�����Ƽ� 
							Long remCount = daService.getCount(recomsql, params);
							if(remCount<1 && false){//û�б��Ƽ���,�鱾�γ���֧������Ҫ��ȥ����ȯ���),����1Ԫ�������շ�Ա2-3Ԫ
								logger.error(">>>>>>>>>>û�б��Ƽ���>>>>>>>>orderid:"+orderMap.get("id"));
								//����֧��
								Long orderId = (Long)orderMap.get("id");
								Map tMap = daService.getMap("select umoney from ticket_tb where uin=? and orderid=? ", new Object[]{preUin,orderId});
								if(tMap!=null&&tMap.get("umoney")!=null){
									Double salefee = StringUtils.formatDouble(tMap.get("umoney"));
									prefee = prefee-salefee;
								}
								if(prefee>=1){
									
									double recommendquota = 5.00;
									Map usrMap1 =daService.getMap("select auth_flag,mobile,recommendquota from user_info_Tb where id =? ", new Object[]{Long.parseLong(orderMap.get("uid")+"")});
									Map usrMap2 =daService.getMap("select auth_flag,mobile,recommendquota from user_info_Tb where id =? ", new Object[]{uid});
									double recommendin = 2;
									double recommendout = 3;
									if(usrMap1!=null){
										recommendquota = Double.parseDouble(usrMap1.get("recommendquota")+"");
										logger.error("�����շ�Ա���Ƽ������Ϊ��"+recommendquota);
										if(usrMap2!=null){
											logger.error("�����շ�Ա���Ƽ������Ϊ��"+Double.parseDouble(usrMap2.get("recommendquota")+""));
											if(Double.parseDouble(usrMap2.get("recommendquota")+"")>recommendquota){
												recommendquota = Double.parseDouble(usrMap2.get("recommendquota")+"");
											}
										}
										logger.error("���շ�Ա���Ƽ�������ǣ�"+recommendquota);
										int recommendquota2 =(int)recommendquota;
										if(recommendquota2%2==0){
											recommendin = recommendquota2/2;
											recommendout = recommendquota2/2;
										}else{
											recommendin = recommendquota2/2+1;
											recommendout = recommendquota2/2;
										}
									}
									int re=  daService.update("insert into recommend_tb(pid,nid,type,state,create_time,money,openid)" +
											" values(?,?,?,?,?,?,?)", new Object[]{(Long)orderMap.get("uid"),preUin,0,0,System.currentTimeMillis()/1000,recommendin,openid});
									logger.error(">>>>΢��Ԥ֧�����Ƽ����������շ�Ա"+recommendin+"Ԫ��pid:"+orderMap.get("uid")+",nid:"+preUin+",ret:"+re);
									re=  daService.update("insert into recommend_tb(pid,nid,type,state,create_time,money,openid)" +
											" values(?,?,?,?,?,?,?)", new Object[]{uid,preUin,0,0,System.currentTimeMillis()/1000,recommendout,openid});
									logger.error(">>>>΢��Ԥ֧�����Ƽ����������շ�Ա"+recommendout+"Ԫ��pid:"+uid+",nid:"+preUin+",ret:"+re);
									if(uid!=null&&uid>-1)
										re = daService.update("update order_tb set uid=? where id=?", new Object[]{uid,orderId});
									logger.error(">>>>>nfc prepay order,���³����շ�Աuid:"+uid+" ,ret="+re);
									if(wxpMap!=null&&wxpMap.get("wxp_openid")!=null){//�ѹ�ע��,ֱ�ӷ��ָ��շ�Ա
										//publicMethods.handleWxRecommendCode(preUin,0L);//2016-09-07
									}
								}
							}
						}
					}
				}
			} catch (Exception e1) {
				logger.error(">>>>>>>NFC,weixin prepay hadale sale to collecter error:"+e1.getMessage());
				e1.printStackTrace();
			}
			//logger.error("NFCˢ��...���ţ�"+uuid);
			if(orderMap!=null&&orderMap.get("comid")!=null){//�ж���������
				try {
					Long orderId = (Long)orderMap.get("id");
					if(from.equals("qr")){//ɨ��ά��������鶩��״̬������ѽ��㣬���ٴ���
						Integer state = (Integer)orderMap.get("state");
						if(state!=null&&state==1){//�ѽ��㣬��ά��ʧЧ
							result = "{\"info\":\"-3\",\"errmsg\":\"�˶����ѽ���\"}";
						}else {
							result = publicMethods.getOrderPrice(comId, orderMap, ntime);
							result = "{\"info\":\"1\","+result.substring(1);
						}
					}else{
						result = publicMethods.getOrderPrice(comId, orderMap, ntime);
						result = "{\"info\":\"1\","+result.substring(1);
					}
				} catch (Exception e) {
					logger.error("NFCˢ��.���㶩�����󣬶�����ţ�"+orderMap.get("id")+",comid:"+comId);
					e.printStackTrace();
				}
				//{total=2.0, duration=7����, etime=15:14, btime=15:06, uin=-1, orderid=27475, collect=2.0, discount=0.0}
			}else if(from.equals("")){//û�ж������������ɶ�����Ϣ
				logger.error("NFCˢ��.�������ɶ���");
				//�Ƿ񲻲��ӵ�,0���ӵ���1����
				Integer esctype = RequestUtil.getInteger(request, "esctype", 0);
				//��ѯ�Ƿ��ǻ�Ա
				Long uin = publicMethods.getUinByUUID(uuid);
				
				String carNumber = "";
				carNumber = publicMethods.getCarNumber(uin);
				if("���ƺ�δ֪".equals(carNumber))
					carNumber="";
				if(uin!=null&&uin>0&&esctype==0&&!"".equals(carNumber)){
					int own = 0;//�ó������Լ��������ӵ�����
					int other=0;//�ó����ڱ�ĳ������ӵ�����
					//��ѯ���������û���ӵ�
					List<Map<String, Object>> escpedList = daService.getAll("select comid from no_payment_tb where state=? and uin=? and car_number=? ",
							new Object[]{0,uin,carNumber});
					if(escpedList!=null){
						for(Map<String, Object> map : escpedList){
							Long cid = (Long)map.get("comid");
							if(cid!=null&&cid.intValue()==comId.intValue())
								own++;
							else
								other++;
						}
					}
					if(own!=0||other!=0){//���ӵ�
						result = "{\"info\":\"-2\",\"own\":\""+own+"\",\"other\":\""+other+"\",\"carnumber\":\""+carNumber+"\"}";
						AjaxUtil.ajaxOutput(response, result);
						logger.error("NFCˢ��.���㶩����result:"+result);
						return null;
					}
				}
				//��ѯ�۸�,�ǲ��ǽ������շ�
				List<Map<String ,Object>> priceList=daService.getAll("select * from price_tb where comid=? " +
						"and state=? ", new Object[]{comId,0});
				if(priceList!=null&&priceList.size()==1){
					Integer pay_type = (Integer)priceList.get(0).get("pay_type");
					Integer unit = (Integer)priceList.get(0).get("unit");
					
					if(pay_type==1&&unit!=null&&unit==0){//�������շѣ��շѵ�λΪ0������ʱ����
						Object price = priceList.get(0).get("price");
						String curtime = TimeTools.gettime();
						//�೵�ƴ���
						List<Map<String, Object>> cardList = daService.getAll("select car_number from car_info_Tb where uin=? ", new Object[]{uin});
						String cards = "[]";
						if(cardList!=null&&cardList.size()>0){
							cards = "";
							for(Map<String, Object> cMap: cardList){
								cards +=",\""+cMap.get("car_number")+"\"";
							}
							cards = cards.substring(1);
							cards = "["+cards+"]";
						}
						result = "{\"info\":\"2\",\"carnumber\":\""+carNumber+"\",\"cards\":"+cards+",\"ctime\":\""+curtime+"\",\"total\":\""+price+"\",\"uin\":\""+uin+"\",\"uuid\":\""+uuid+"\"}";
						AjaxUtil.ajaxOutput(response, result);
						logger.error("NFCˢ��.���㶩����result:"+result); 
						return null;
					}
				}
				//���Ƴ������볡ʱ���ؼ۸�
				String pid = CustomDefind.CUSTOMPARKIDS;
				if(pid.equals(comId.toString())){
					result = "{\"info\":\"3\"}";
				}else {
					result = "{\"info\":\"0\"}";
				}
			}else {
				result = "{\"info\":\"-3\",\"errmsg\":\"�˶����ѽ���\"}";
			}
			if(from.equals("qr")){
				result="{\"type\":\"0\",\"info\":"+result+"}";
			}
			logger.error("NFCˢ��.result:"+result);
			AjaxUtil.ajaxOutput(response, result);
			return null;
			
		}else if(action.equals("coswipe")){//����ˢNFC����δ����ʱ���󶨶������ѽ���ʱ��֧��
			String mobile = RequestUtil.processParams(request, "mobile");
			String from = RequestUtil.getString(request, "from");//�¶�ά��ɨ�����
			if(nid!=-1&&uuid.equals("")){//����ɨ�賵�ƣ�����nidʱ���Ȳ��Ӧ��uuid
				uuid = getUUIDByNid(nid);
			}
			if(uuid.equals("")){//û��uuidʱ�����ؿ�
				AjaxUtil.ajaxOutput(response, "info:����(nid)��"+nid+"������ !");
				return null;
			}
			if(mobile.equals("")||!Check.checkMobile(mobile)){
				AjaxUtil.ajaxOutput(response, "info:�ֻ����벻�Ϸ�!");
				return null;
			}
			//��ѯ����
			Map<String, Object> userMap = daService.getPojo("select * from user_info_Tb where mobile=? and auth_flag=?", new Object[]{mobile,4});
			if(userMap==null||userMap.isEmpty()){
				AjaxUtil.ajaxOutput(response, "info:�ֻ�����δע��!");
				return null;
			}else {
				//�����˺�
				Long uin = (Long)userMap.get("id");
				//����UUID��ѯ��ǰ����
				Map<String, Object> orderMap = daService.getPojo("select * from order_tb where nfc_uuid=?  ", new Object[]{uuid});
				//�ж��Ƿ���ͬһ���������ɹ�����
				if(orderMap==null||orderMap.isEmpty()){
					AjaxUtil.ajaxOutput(response, "info:δ��ѯ��������Ϣ!");
					return null;
				}
				Long ocount = onlyReadService.getLong("select count(id) from order_tb where comid=? and state=? and uin=? and nfc_uuid!=? ", 
						new Object[]{orderMap.get("comid"),0,uin,uuid});
				//�ж���������Ƿ��Ѱ󶨹�����
				Long _uin = (Long)orderMap.get("uin");
				Integer state = (Integer)orderMap.get("state");
				System.out.println(">>>ocount:"+ocount);
				if(ocount>0){//������ͬһ���������ɹ������򶩵��Ƿ��Ѱ󶨹�����
					AjaxUtil.ajaxOutput(response, "info:�����ڸó��������ɹ�����!");
					return null;
				}
				System.out.println(">>>_uin:"+_uin+",state:"+state+",uin:"+uin);
				if(_uin>0&&state<1&&uin.intValue()!=_uin.intValue()){
					AjaxUtil.ajaxOutput(response, "info:��ͣ�����Ѱ󶨹�"+orderMap.get("car_number")+"�ĳ���!");
					return null;
				}
				String carNumber = "";
				//��������
				Map<String, Object> carinfoMap = daService.getPojo("select car_number from car_info_tb where uin=?", new Object[]{uin});
				if(carinfoMap!=null)
					carNumber = (String)carinfoMap.get("car_number");
				Long oid = null;//
				String result = "{}";
				if(orderMap!=null&&orderMap.get("id")!=null){
					oid = (Long)orderMap.get("id");
					Integer ptype = (Integer)orderMap.get("pay_type");
					try {
						daService.update("insert into lottery_tb(uin,orderid,create_time,lottery_result) values(?,?,?,?)",
								new Object[]{uin,oid,System.currentTimeMillis()/1000,-1});
					} catch (Exception e) {
						logger.error(">>>>>>�ѵ�¼���齱��Ϣ....");
					}
					
					if(state==0){//δ���㣬�󶨶���
						int reslut = daService.update("update order_tb set uin=?,car_number=? where id=?", new Object[]{uin,carNumber,oid});
						if(reslut==1){
							Map<String, Object> infoMap = new HashMap<String, Object>();
							Map _orderMap = daService.getPojo("select o.create_time,o.id,o.comid,c.company_name," +
									"c.address,o.state,o.pid from order_tb o,com_info_tb c where o.comid=c.id and o.id=? and o.state=?",
									new Object[]{oid,0});
							if(orderMap!=null){
								Long btime = (Long)_orderMap.get("create_time");
								Long end = System.currentTimeMillis()/1000;
								Long _comId = (Long)orderMap.get("comid");
								Integer car_type = (Integer)orderMap.get("car_type");//0��ͨ�ã�1��С����2����
								Integer pid = (Integer)orderMap.get("pid");
								if(pid>-1){
									infoMap.put("total",publicMethods.getCustomPrice(btime, end, pid));
								}else {
									infoMap.put("total",publicMethods.getPrice(btime, end, comId, car_type));	
								}
								infoMap.put("btime", btime);
								infoMap.put("etime",end);
								infoMap.put("parkname", _orderMap.get("company_name"));
								infoMap.put("address", _orderMap.get("address"));
								infoMap.put("orderid", _orderMap.get("id"));
								infoMap.put("state",_orderMap.get("state"));
								infoMap.put("parkid", _comId);
							}
							result= StringUtils.createJson(infoMap);
						}else {
							AjaxUtil.ajaxOutput(response, "info:������ʧ��!");
						}
					}else if(state==1){//�ѽ���
						if(ptype==1){//�ѽ��㣬�ֽ�֧�������ظ��û����û������ֽ�֧��ʱ�������ֻ�֧��
							Map<String, Object> infomMap = new HashMap<String, Object>();
							comId = (Long)orderMap.get("comid");
							Long btime = (Long)orderMap.get("create_time");
							Long etime = (Long)orderMap.get("end_time");
							String cname = (String)daService.getObject("select company_name from com_info_tb where id=?",new Object[]{comId}, String.class);
							infomMap.put("parkname",cname);
							infomMap.put("btime", btime);
							infomMap.put("etime", etime);
							infomMap.put("total", StringUtils.formatDouble(orderMap.get("total")));
							infomMap.put("state",orderMap.get("pay_type"));// -- 0:δ���㣬1����֧����2��֧�����
							infomMap.put("orderid",oid);
							result = StringUtils.createJson(infomMap);
						}
					}
				}else{
					result ="info:û�ж�Ӧ�Ķ���!";
				}
				if(from.equals("qr")){
					result="{\"type\":\"2\",\"info\":"+result+"}";
				}
				AjaxUtil.ajaxOutput(response, result);
				return null;
			}
			//http://127.0.0.1/zld/nfchandle.do?action=coswipe&uuid=0428C302773480&mobile=15801482643
		}
		//�������ɶ�������
		else if(action.equals("addorder")){
			if(!memcacheUtils.addLock(uuid+"nfcincom",1)){
				logger.error("lock:"+uuid+",2�����ˢ��.");
				AjaxUtil.ajaxOutput(response, "2�����ˢ��");
				return null;
			}
			//logger.error("NFCˢ��.���ɶ�����NFC�ţ�"+uuid+",comid="+comId);
			String imei  =  RequestUtil.getString(request, "imei");
			///////��ֹ������������ͬ�Ľ���������ϵͳ��Ҫ˯��10-300����
			logger.error("NFCˢ��.���ɶ�����uid:"+uid+",NFC�ţ�"+uuid+",comid="+comId);
//			try {
//				Integer sleepMillons = new Random().nextInt(300);
//				if(sleepMillons<10)
//					sleepMillons = sleepMillons*10;
//				Thread.sleep(sleepMillons);
//				logger.error("nfc������˯����"+sleepMillons+"����");
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
			Long ctime =System.currentTimeMillis()/1000;
			Long uin = publicMethods.getUinByUUID(uuid);
			logger.error(">>>��ͨ���û���uin="+uin);
		    ///////��ֹ������������ͬ�Ľ���������ϵͳ��Ҫ˯��10-300����
			Integer pid  =  RequestUtil.getInteger(request, "ctype",-1);//�Ʒѷ�ʽ,���ų���ר��--�Ʒѷ�ʽ��0����(0.5/h)��1��ʱ��12Сʱ��10Ԫ����ÿСʱ1Ԫ��
			Integer count =0;
					//daService.getLong("select  create_time from order_tb where state=? and nfc_uuid=? and comid=? and uin =? ", 
					//new Object[]{0,uuid,comId,uin});
			/*Map orderMap = daService.getMap("select * from order_tb where nfc_uuid=? and comid=? and create_time =?  ",
					new Object[]{uuid,comId,ctime});
			logger.error("nfc:"+uuid+",before insert ,db has record:"+orderMap);
			
			if(orderMap!=null&&orderMap.get("state")!=null){
				Integer state = (Integer)orderMap.get("state");
				if(state==0){
					count=1;
					logger.error("nfc:"+uuid+",before insert ,�Ѵ���δ���㶩�����������ɶ���....");
				}
			}*/
			
			//��ѯ�Ƿ��ж���
			String qsql = "select * from order_tb where comid=? and nfc_uuid=? and state=? ";
			Object [] values = new Object[]{comId,uuid,0};
			Map orderMap = onlyReadService.getMap(qsql,values);
			if(orderMap!=null&&!orderMap.isEmpty()){
				count=1;
				logger.error(">>>> add nfc order error,exists order :"+orderMap);
			}
			if(count==0){
				try {
					if(uin!=null&&uin!=-1){//��ͨ��,����ʱ���Զ�����һ������,��5�������г�������ʱ���������ɶ���
						qsql =" select count(id) count from order_tb where comid=? and nfc_uuid=? and uin =? and end_time > ?  ";
						values = new Object[]{comId,uuid,uin,System.currentTimeMillis()/1000-5*60};
					}
					Map cou = onlyReadService.getMap(qsql, values);
					if(cou!=null&&!cou.isEmpty()){
						Long c = (Long)cou.get("count");
						//c=0L;
						if(c!=null&&c>0){
							logger.error(">>>> add nfc order error,nfc_user("+uin+") has exists in 5 min order :"+cou);
							AjaxUtil.ajaxOutput(response, "-2");//��ͨ����������ڲ�����ͬһ������һ����������
							return null;
						}
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				//��ѯNFC���Ƿ��Ѱ󶨳���
				//ParkingMap.getNfcUid(uuid, daService);// -1L;
//				Map nfcMap = daService.getMap("select uin from com_nfc_tb where nfc_uuid=?",new Object[]{uuid});
//				if(nfcMap!=null&&nfcMap.get("uin")!=null)
//					uin = (Long)nfcMap.get("uin");
//				uin = uin==0?-1L:uin;
				
				String carNumber = "";
				if(uin!=null&&uin!=-1){
					carNumber = publicMethods.getCarNumber(uin);
//					Map carMap = daService.getPojo("select car_number from car_info_Tb where uin=?", new Object[]{uin});
//					if(carMap!=null&&carMap.get("car_number")!=null)
//						carNumber = (String)carMap.get("car_number");
				}
				if(uin==null)
					uin = -1L;//? -1L:uin;
				int result = daService.update("insert into order_tb (comid,uin,state,create_time,nfc_uuid,c_type,uid,imei,car_number,pid) " +
						"values(?,?,?,?,?,?,?,?,?,?)",
						new Object[]{comId,uin,0,ctime,uuid,0,uid,imei,carNumber,pid});
				String cname = (String) daService.getObject("select company_name from com_info_tb where id=?",
						new Object[] { comId},String.class);
				//String ntime = TimeTools.getTime_yyyyMMdd_HHmm(System.currentTimeMillis());
				//logService.insertParkUserMesg(1, uid, "", "");
				if(uin!=null&&uin!=-1)
					logService.insertUserMesg(4, uin, "���ѽ���"+cname+"���볡��ʽ��NFCˢ���볡��", "�볡����");
				logger.error("NFCˢ��.���ɶ�����NFC�ţ�"+uuid+",comid="+comId+",��� ��"+result);
				AjaxUtil.ajaxOutput(response, ""+result);
			}else {
				logger.error("NFCˢ��.���ɶ�����NFC�ţ�"+uuid+",comid="+comId+",��� �������Ѵ��� ���������� !");
				AjaxUtil.ajaxOutput(response, "-1");
			}
			//http://127.0.0.1/zld/nfchandle.do?action=addorder&uuid=0458F902422D80&comid=3
		}
		//��ɶ���
		else if(action.equals("completeorder")){
			//http://wang151068941.oicp.net/zld/nfchandle.do?action=completeorder&orderid=786532&collect=10&comid=1197&carnumber=&uid=
			Long orderId = RequestUtil.getLong(request, "orderid", -1L);//�������
			Double money = RequestUtil.getDouble(request, "collect", 0d);//ʵ�ս��
			Long out_passid = RequestUtil.getLong(request, "passid", -1L);//����ͨ��id
			Integer pay_flag = RequestUtil.getInteger(request, "pay", -1);//-1:Ĭ�ϣ�0��Ԥ֧���°汾1:Ԥ֧���°汾�ֽ����
			String isclick = RequestUtil.getString(request, "isclick");
			System.err.println("isclick:"+isclick+",orderid:"+orderId);
			String imei  =  RequestUtil.getString(request, "imei");//�ֻ�����
			
			/**����֧��****/
			if(orderId>0&&money>0){
				Object orderObject = daService.getPOJO("select * from order_tb where id=? ", 
						new Object[]{orderId}, Order.class);
				if(orderObject!=null){
					Order order = (Order)orderObject;
					double prePay = order.getPrepaid();
					Integer cType =order.getC_type();
					Long comid = order.getComid();
					Long userId = order.getUin();
					if(prePay<=0&&cType!=5){
						AutoPayPosOrderResp autoPayResp = null;
						AutoPayPosOrderReq autoPayReq = new AutoPayPosOrderReq();
						autoPayReq.setOrder(order);
						autoPayReq.setMoney(money);
						autoPayReq.setImei(imei);
						autoPayReq.setUid(uid);
						autoPayReq.setUserId(userId);
						autoPayReq.setEndTime(System.currentTimeMillis()/1000);
						autoPayReq.setGroupId(publicMethods.getGroup(comid));
						String bolinkPayRes =publicMethods.sendPayOrderToBolink(orderId,
								System.currentTimeMillis()/1000,money,order.getComid());
						if(bolinkPayRes.equals("app")){//����ƽ̨�Ѿ�����֧�������ﴦ��Ϊ����֧��
							logger.error("��������>>>"+autoPayReq.toString());
							autoPayResp = payBolinkService.autoPayPosOrder(autoPayReq);
							logger.error("��������>>>"+autoPayResp.toString());
							if(autoPayResp.getResult() == 1){
								logService.insertParkUserMessage(comId, 2, uid, order.getCar_number(), orderId, money,StringUtils.getTimeString(ntime-order.getCreate_time()),0, order.getCreate_time(), ntime,0, null);
								AjaxUtil.ajaxOutput(response, "1");
								return null;
							}
						}
					}
				}
			}
			/**����֧��****/
			
			Integer isClick = isclick.equals("true")?1:0;//0�Զ����� 1�ֶ�����
			Map orderMap = null;
			String carnumber =AjaxUtil.decodeUTF8(RequestUtil.getString(request, "carnumber"));
			carnumber = carnumber.toUpperCase().trim();
			carnumber = carnumber.replace("I", "1").replace("O", "0");
			boolean isetc = publicMethods.isEtcPark(comId);
			if(isetc){
//				Integer local = RequestUtil.getInteger(request, "local", -1);//����-1   ��������������֧��Ϊ1
//				if(local<0){
					int ret = daService.update("update order_tb set need_sync=? where id=? and (need_sync=? or need_sync=?)",new Object[]{4,orderId,2,3});
					logger.error("���ػ������л������Ͻ��㶩����ǣ����ط���������ʱͬ�����㱾�أ�result��"+ret+",orderid:"+orderId);
//				}
			}
			logger.error("completeorder orderid:"+orderId+",carnumber:"+carnumber+",money"+money);
			if("���ƺ�δ֪".equals(carnumber))
				carnumber="";
			
			Long neworderId = -1L;//����ʱ����޶�����ţ������¶���	
			if(orderId==-1){//����ֱ�ӽ���ʱ��û�ж�����ţ���Ҫ�����ɶ��� 20150204
				Long uin = RequestUtil.getLong(request, "uin", -1L);
				if(uin==-1){//���ǻ�Ա
					if(!carnumber.equals("")){//�г���ʱ����ѯ��Ա�˺�
						Map carMap = daService.getMap("select uin from car_info_tb where car_number=?", new Object[]{carnumber});
						if(carMap!=null&&carMap.get("uin")!=null){
							uin = (Long)carMap.get("uin");
						}
					}
				}
				if(!uuid.equals("")){
					orderId = daService.getkey("seq_order_tb");
					int result = daService.update("insert into order_tb(id,create_time,uin,comid,c_type,uid,car_number,state,imei,nfc_uuid,total,in_passid) values" +
							"(?,?,?,?,?,?,?,?,?,?,?,?)", new Object[]{orderId,System.currentTimeMillis()/1000,uin,comId,0,uid,carnumber,0,imei,uuid,money,out_passid});
					logger.error("completeorder>>>>���ɶ�����orderid��"+orderId+",uin:"+uin);
					if(result!=1){//���ɶ������ɹ�
						AjaxUtil.ajaxOutput(response, "-1");
						return null;
					}else {
						neworderId = orderId;//�����ɵĶ����������ٴ����복��
					}
				}
			}
			//Long _uid = RequestUtil.getLong(request, "uid", -1L);//�շ�Ա�ʺ�
			Long count =0L;
			Long _uin=-1L;
			Long band_uin = null;
			if(orderId!=-1)
				orderMap = daService.getMap("select * from order_tb where id=?",new Object[]{orderId});
			
			//neworderid ==-1ʱ����ʾ���ж������
			Integer preState =(Integer)orderMap.get("pre_state");//Ԥ֧��״̬ ,1����Ԥ֧��,2�ȴ��������Ԥ֧��
			if(preState==1 && pay_flag == 0 && neworderId == -1){
				logger.error(">>>>����Ԥ֧���У��ȴ��������Ԥ֧����orderid:"+orderId+",uin:"+orderMap.get("uin")+",money:"+money);
				AjaxUtil.ajaxOutput(response, "-5");//��������Ԥ֧�����շ�Ա
				return null;
			}
			if(neworderId == -1 && orderMap.get("total")!= null && Double.valueOf(orderMap.get("total")+"")>0 && (Integer)orderMap.get("state") ==0){
				Integer local = RequestUtil.getInteger(request, "local", -1);//����-1   ��������������֧��Ϊ1
				if(local>0&&isetc){
					//��Ԥ֧���Ľӿ�
					String result = doprepayorder(request,comId,uid);
					AjaxUtil.ajaxOutput(response,result);
					return null;
				}
				logger.error(">>>>�����ո�Ԥ֧���꣬�շ�Ա��Ҫ����ˢ������action=doprepayorder�ӿڣ�orderid:"+orderId+",uin:"+orderMap.get("uin")+",money:"+money);
				AjaxUtil.ajaxOutput(response, "-6");//��������Ԥ֧�����շ�Ա
				return null; 
			}
			
			//�ж���ͨ��
			boolean isShuTong = false;
			if(uuid==null||uuid.equals("null")||uuid.equals(""))
				uuid = (String)orderMap.get("nfc_uuid");
			if(uuid!=null&&!uuid.equals(""))
				band_uin = publicMethods.getUinByUUID(uuid);
			if(band_uin!=null&&band_uin>1)
				isShuTong=true;
			//ԭ�����еĳ���
			String orderCarNumber = (String) orderMap.get("car_number");
			logger.error(">>>>>>>pay_order....isShuTong:"+isShuTong+",band_uin:"+band_uin+",carnumber:"+orderCarNumber);
			//���޳��ƶ�����
			boolean isNoCarNumber = orderCarNumber==null||"".equals(orderCarNumber)||"���ƺ�δ֪".equals(orderCarNumber);
			if(isNoCarNumber&&neworderId==-1&&!carnumber.equals("")&&!isShuTong){//ԭ������û�г��ƣ�����ʱ�������³��ƣ�����ͨ���û�
				if(!carnumber.equals(""))
					count=onlyReadService.getLong("select count(id) from order_tb where comid=? and car_number=? and state=? ", new Object[]{comId,carnumber,0});
				if(count>0){
					//��ͬ�ĳ������ڱ��������ڶ���
					AjaxUtil.ajaxOutput(response, "-2");
					return null;
				}else {
					Map carMap = daService.getMap("select uin from car_info_tb where car_number=?", new Object[]{carnumber});
					if(carMap!=null&&carMap.get("uin")!=null){
						_uin = (Long)carMap.get("uin");
					}
					int ret = daService.update("update order_tb set uin=? ,car_number=?,out_passid=? where id=?",new Object[]{_uin,carnumber,out_passid,orderId});
					if(ret==1)
						orderCarNumber=carnumber;
				}
			}
			//ԭ�������г��ƣ����봫��ĳ��Ʋ�һ��ʱ���޸Ķ����еĳ���
			if(!isNoCarNumber&&!carnumber.equals("")&&!carnumber.equals(orderCarNumber)){
				int ret = daService.update("update order_tb set car_number=?  where id=?",new Object[]{carnumber,orderId});
				logger.error(">>>>>ԭ�������г��ƣ����봫��ĳ��Ʋ�һ��ʱ���޸Ķ����еĳ���,ԭ���ƣ� "+orderCarNumber+"���ֳ���"+carnumber+"����� ��"+ret);
			}
			//logger.error("NFCˢ��.��ɶ�����NFC�ţ�"+uuid+",comid="+comId);
			if(orderId==-1||comId==-1){
				AjaxUtil.ajaxOutput(response, "-1");
			}else {
				Long etime = System.currentTimeMillis()/1000;
				
				Integer pay_type = 1;
				if(_uin==null||_uin==-1){
					_uin = (Long)orderMap.get("uin");
				}
				Integer _state = (Integer)orderMap.get("state");
				pay_type= (Integer)orderMap.get("pay_type");
				if(pay_type!=null&&pay_type>1&&_state!=null&&_state==1){//��֧����������
					logger.error(">>>>����֧������֧����....����...");
					AjaxUtil.ajaxOutput(response, "1");
					return null;
				}
				//20160229������Ԥ֧��ֻ������ ��������ǰpay_type = 4  ��ֹ���ĳ�pay_type= 1
				if(pay_type!=4){
					pay_type = 1;//�ֽ�֧�� ;
				}
				Integer cType = Integer.valueOf(String.valueOf(orderMap.get("c_type")));//������ʽ ��0:NFC��2:����    3:ͨ�����ƽ��� 4ֱ�� 5�¿��û�
				logger.error("completeorder>>>>orderid:"+orderId+",money:"+money+",_uin"+_uin+",cType:"+cType);
				if(money==0&&_uin!=null&&_uin!=-1){//�ж��¿��û�
					if(cType==5){//�¿��û�
					//if(isMonthUser(comId, _uin)){//�¿��û�
						logger.error("completeorder>>>>���¿��û�,_uin:"+_uin+",orderid:"+orderId);
						pay_type = 3;
					}
				}
				if(cType==3||cType==2){//ͨ�����ֻ�����
					if(cType==3)//ͨ��
						isShuTong=true;
					if(_uin==null||_uin==-1){//����ʱ����ע���û�
						carnumber = (String)orderMap.get("car_number");
						if(carnumber!=null){
							Map carMap = daService.getMap("select uin from car_info_tb where car_number=?", new Object[]{carnumber});
							if(carMap!=null&&carMap.get("uin")!=null){
								_uin = (Long)carMap.get("uin");
							}
//							daService.update("update order_tb set uin=? ,car_number=? where id=?",
//									new Object[]{_uin,carnumber,orderId});
						}
					}
				}
				
				//���¶���Ϊ��֧��,�ֽ�(pay_type:1)���¿�(pay_type:3)
//				Long intime = (Long)orderMap.get("create_time");
//				//Long endtime = TimeTools.getOrderTime();
//				if(etime==intime)
//					etime = etime+60;
				int result = daService.update("update order_tb set end_time=?,total=?,state=?,uin=?," +
						"pay_type=?,uid=?,imei=?,out_passid=?,isclick=?  where  id=?", 
						new Object[]{etime,money,1,_uin,pay_type,uid,imei,out_passid,isClick,orderId});
				logger.error("NFCˢ��.��ɶ�����NFC�ţ�"+uuid+",comid="+comId+",������"+_uin+",�����"+result+",orderid:"+orderId);
				if(result == 1){
					try {
						methods.updateRemainBerth(comId, 1);
					} catch (Exception e) {
						// TODO: handle exception
						logger.error("update remain error>>>orderid:"+orderId+",comid:"+comId, e);
					}
				}
				//���¼���ȯ״̬
				publicMethods.updateShopTicket(orderId, _uin);
				
				Long btime = null;
				orderMap = daService.getPojo("select * from order_tb where id=?",new Object[]{orderId});
				btime = (Long)orderMap.get("create_time");
				//System.out.println(">>>>>>>>"+orderMap);
				if(pay_type==3&&money==0){//�¿��û�֧����� ������֧���ɹ���Ϣ
//					logService.insertParkUserMessage(comId, 2, uid, (String)orderMap.get("car_number"), 
//							orderId, money,StringUtils.getTimeString(btime, etime), 0, btime, etime,0);
					AjaxUtil.ajaxOutput(response, ""+result);
					logger.error("completeorder,�¿��û�,orderid:"+orderId);
					logger.error(">>>>����֧����������Ϣ�������������յ�֧�������Ϣ..����......");
					return null ;
				}
				
				
				/*
				if(cType!=null&&btime!=null&&(etime-btime>=15*60)){//����ʱ������15���ӣ����Լ�һ��0.2�Ļ���
					if(cType==0)//NFC����  ( ˢNFC������ɨ��������Ч������������֧����0.01�֣�����֧������һԪ����2�֡�)
						logService.updateScroe(6, uid,comId);
					else if(cType==2||cType==3)//ɨ�ƻ����ƻ��� 
						logService.updateScroe(7, uid,comId);
				}
				*/
				Long uin = (Long)orderMap.get("uin");
				//Long parkcomId = (Long)orderMap.get("comid");
				Map comMap = daService.getMap("select isautopay from com_info_tb where id = ? ", new Object[]{Long.parseLong(orderMap.get("comid")+"")});
				Integer epay = 0;
				if(comMap!=null&&comMap.get("isautopay")!=null){
					epay = Integer.parseInt(comMap.get("isautopay")+"");
				}
				//�������ͨ���û�  ���ҳ���֧�ֵ���֧��
				if(isShuTong&&uin!=null&&uin!=-1&&epay==1&&cType!=null&&(cType==0||cType==3)){
					logger.error(">>>>>>�г�����Ϣ���ж��Զ�֧��....");
					//���û����
					Double balance =0d;
					Map userMap = daService.getMap("select balance,wxp_openid from user_info_tb where id=?",new Object[]{uin});
					
					if(userMap!=null&&userMap.get("balance")!=null){
						balance=Double.valueOf(userMap.get("balance")+"");
					}
					//�鳵�����ã��Ƿ��������Զ�֧����û������ʱ��Ĭ��25Ԫ�����Զ�֧�� 
					Integer autoCash=1;
					Map upMap = daService.getPojo("select auto_cash,limit_money from user_profile_tb where uin =?", new Object[]{uin});
					Integer limitMoney =25;
					if(upMap!=null&&upMap.get("auto_cash")!=null){
						autoCash= (Integer)upMap.get("auto_cash");
						limitMoney = (Integer)upMap.get("limit_money");
					}
					
					//String carNumber=orderCarNumber;
					if(carnumber==null||carnumber.equals("")){//����ʱû�д��복��
						if(orderCarNumber!=null&&!orderCarNumber.equals(""))//�������г���ʱ����Ϊ�����еĳ���
							carnumber = orderCarNumber;
						else
							carnumber=publicMethods.getCarNumber(uin);
					}
					
					String duration = StringUtils.getTimeString(btime, etime);
					int state = 1;//������Ϣ״̬��0:δ���㣬1����֧����2��֧�����, -1:֧��ʧ��   Ĭ��1�ȴ�֧��
					if(autoCash!=null&&autoCash==1){//�������Զ�֧��
						//�鳵���Ƿ��п��õ�ͣ��ȯ
						
						boolean isupmoney=true;//�Ƿ�ɳ����Զ�֧���޶�
						if(limitMoney!=null){
							if(limitMoney==-1||limitMoney>=money)//����ǲ��޻����֧�������Զ�֧�� 
								isupmoney=false;
						}
						if(isupmoney){//�����Զ�֧���޶�
							if(pay_type == 1 && _state == 0){//д�ֽ���ϸ
								int r = daService.update("insert into parkuser_cash_tb(uin,amount,type,orderid,create_time) values(?,?,?,?,?)",
												new Object[] { uid, money, 0, orderId, System.currentTimeMillis() / 1000 });
								logger.error("completeorder>>>>�����Զ�֧���޶дһ���ֽ�֧����ϸorderid:"+orderId+",money:"+money+",uid:"+uid+"r:"+r);
							}
							AjaxUtil.ajaxOutput(response, "4");
							logService.insertMessage(comId, state, uin,carnumber, orderId, money, duration,0, btime, etime,0);
							return null;
						}
						//==============�Զ�ѡȯ�߼�begin===============//
						Map<String, Object> ticketMap = null;
						boolean isAuth = publicMethods.isAuthUser(uin);
						List<Map<String, Object>> ticketlList = methods.chooseTicket(uin, money, 2, uid, isAuth, 2, comId, orderId, 0);
						if(ticketlList != null && !ticketlList.isEmpty()){
							Map<String, Object> map = ticketlList.get(0);
							if(map.get("iscanuse") != null && (Integer)map.get("iscanuse") == 1){
								ticketMap = map;
							}
						}
						Double tickMoney = 0d;//����ͣ��ȯ���
						Long ticketId = null;//ͣ��ȯID
						if(ticketMap!=null){
							tickMoney = StringUtils.formatDouble(ticketMap.get("limit"));
							ticketId = (Long)ticketMap.get("id");
						}
						logger.error(">>>>>>>>>>>>le $30 auto cash: true,total:"+money+",limitmoney:"+limitMoney+",balance:"+balance+"," +
								"ticketid:"+ticketId+",ticketMoney:"+tickMoney+",isupmoney:"+isupmoney);
						//==============�Զ�ѡȯ�߼�end===============//
						//�������ö�ȿ��Զ�֧���������� 20150721
						//�����Ƿ���֤ͨ�������ö���Ƿ����꣬����ʱ�����ö�ȵֿ�
						Double creditLimit=0.0;//�������ö�ȳ�ֵ��֧��ʧ��ʱ��Ҫ����ֵ
						if((balance+tickMoney)<money){//����ʱ���鳵���Ƿ���֤ͨ��
							creditLimit = money-(balance+tickMoney);
							Map tMap = daService.getMap("select is_auth from car_info_Tb where car_number=? ", new Object[]{carnumber});
							Integer is_auth =0;
							if(tMap!=null)
								is_auth=(Integer)tMap.get("is_auth");
							if(is_auth==1){//����֤�ĳ��ƣ��鳵���Ƿ��п������ö��
								tMap = daService.getMap("select is_auth,credit_limit from user_info_tb where id =? ", new Object[]{uin});
								if(tMap!=null){
									is_auth = (Integer)tMap.get("is_auth");
									Double climit = StringUtils.formatDouble(tMap.get("credit_limit"));
									if(is_auth==1&&climit>=creditLimit){
										int ret = daService.update("update user_info_tb set balance=balance+?,credit_limit=credit_limit-? where id =? ", 
												new Object[]{creditLimit,creditLimit,uin});
										logger.error(">>>>>>>auto pay ,�������ö�ȵֿۣ�"+creditLimit+",ret��"+ret);
										if(ret==1){//���ö�ȳ�ֵ�ɹ�
											balance = money-tickMoney;
										}else {
											creditLimit=0.0;
										}
									}else {
										logger.error(">>>>>>>auto pay ,����δ��֤�����ö�Ȳ���,������֤״̬��"+is_auth+",������"+money+",ticketmoney:"+tickMoney+",���ö�ȣ�"+climit);
									}
								}
							}else {
								logger.error(">>>>>>>auto pay ,����δ��֤");
							}
						}
						
						if((balance+tickMoney)>=money){//������֧��
							int re = publicMethods.payOrder(orderMap, money, uin, 2,0,ticketId,null,-1L, uid);//����֧������
							if(re==5){//�ɹ�֧��
								if(comId==20130){//���͹�˾�Ĳ��Զ������������ǵĽӿڷ��Ͷ���֧��״̬
									String sr = methods.sendOrderState2Baohe(orderId, result, money, 0);
									logger.error(">>>>>>>>>>>>>baohe sendresult:"+sr+",orderid:"+orderId+",state:"+result+",money:"+money);
								}
								pay_type = 2;
								//�ֱ���������շ�Ա���ͳɹ�֧����Ϣ
								logger.error(">>>>>>>>>>>>auto cash: success,����Ϣ�������������շ�Ա....");
								state=2;
								//���շ�Ա����Ϣ
								String cname = (String) daService.getObject("select company_name from com_info_tb where id=?",
										new Object[] { comId },String.class);
								logService.insertUserMesg(2, uin,cname+"��ͣ����"+money+"Ԫ���Զ�֧���ɹ���", "�Զ�֧������");
								logService.insertParkUserMessage(comId, state, uid, carnumber, orderId, money,duration,0, btime, etime,0, null);
								
								if(userMap.get("wxp_openid") != null){
									String openid = (String)userMap.get("wxp_openid");
									Map<String, String> baseinfo = new HashMap<String, String>();
									List<Map<String, String>> orderinfo = new ArrayList<Map<String,String>>();
									String url = "http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/wxpaccount.do?action=toaccountdetail&openid="+openid;
									String remark = "�������鿴�˻���ϸ��";
									String remark_color = "#000000";
									Map bMap  =daService.getMap("select * from order_ticket_tb where uin=? and  order_id=? and ctime>? order by ctime desc limit ?",
											new Object[]{uin,orderId,System.currentTimeMillis()/1000-5*60, 1});//�����ǰ�ĺ��
									
									if(bMap!=null&&bMap.get("id")!=null){
										Integer bonus_type = 0;//0:��ͨ���������1��΢���ۿۺ��
										if(bMap.get("type")!= null && (Integer)bMap.get("type") == 1){
											bonus_type = 1;//΢�Ŵ��ۺ��
										}
										if(bonus_type == 1){
											remark = "��ϲ�����"+bMap.get("bnum")+"��΢��"+bMap.get("money")+"��ȯ������������ɣ�";
										}else{
											remark = "��ϲ�����"+bMap.get("bnum")+"��ͣ��ȯ������������ɣ�";
										}
										remark_color = "#FF0000";
										url = "http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/wxpublic.do?action=balancepayinfo&openid="+openid+"&money="+money+"&bonusid="+bMap.get("id")+"&bonus_type="+bonus_type+"&orderid="+orderId+"&paytype=1";
									}
									Map uidMap = daService.getMap("select nickname from user_info_tb where id=? ", new Object[]{uid});
									String first = "����"+cname+"���շ�Ա"+uidMap.get("nickname")+"���ѳɹ���";
									baseinfo.put("url", url);
									baseinfo.put("openid", openid);
									baseinfo.put("top_color", "#000000");
									baseinfo.put("templeteid", Constants.WXPUBLIC_SUCCESS_NOTIFYMSG_ID);
									Map<String, String> keyword1 = new HashMap<String, String>();
									keyword1.put("keyword", "orderMoneySum");
									keyword1.put("value", money+"Ԫ");
									keyword1.put("color", "#000000");
									orderinfo.add(keyword1);
									Map<String, String> keyword2 = new HashMap<String, String>();
									keyword2.put("keyword", "orderProductName");
									keyword2.put("value", "ͣ����");
									keyword2.put("color", "#000000");
									orderinfo.add(keyword2);
									Map<String, String> keyword3 = new HashMap<String, String>();
									keyword3.put("keyword", "Remark");
									keyword3.put("value", remark);
									keyword3.put("color", remark_color);
									orderinfo.add(keyword3);
									Map<String, String> keyword4 = new HashMap<String, String>();
									keyword4.put("keyword", "first");
									keyword4.put("value", first);
									keyword4.put("color", "#000000");
									orderinfo.add(keyword4);
									publicMethods.sendWXTempleteMsg(baseinfo, orderinfo);
									
									publicMethods.sendBounsMessage(openid,uid,2d,orderId, uin);//��������Ϣ
								}
								//logService.insertMessage(comId, state, uin, carNumber, orderId, money,duration,0, btime, etime,0);
							}else if(re==-7){
								String cname = (String) daService.getObject("select company_name from com_info_tb where id=?",
										new Object[] { comId },String.class);
								logService.insertUserMesg(0, uin, "��������ԭ��"+cname+"��ͣ����"+money+"Ԫ���Զ�֧��ʧ�ܡ�", "֧��ʧ������");
							}else {
								logger.error(">>>>>>>>>>>>auto cash: fail,���أ�"+re+"....");
								state=-1;
							}
							
							if(re!=5&&creditLimit>0){//֧��ʧ��ʱ�� ���ö��Ҫ����ֵ
								int ret = daService.update("update user_info_tb set balance=balance-?,credit_limit=credit_limit+? where id =? ", 
										new Object[]{creditLimit,creditLimit,uin});
								logger.error(">>>>>>>auto pay ,�������ö�ȵֿ�֧��ʧ�ܣ� ���ö�ȷ���ֵ����"+creditLimit+",ret��"+ret);
							}
							
						}else {
							//д����Ϣ������������Ϣ
							result=2;//��ʾ����
							logService.insertMessage(comId, state, uin,carnumber, orderId, money, duration,0, btime, etime,0);
						}
					}else {
						logger.error(">>>>>>�г�����Ϣ��δ�Զ�֧��������Ϣ������....");
						result=3;//��ͨ���û�û�������Զ�֧��
						//д����Ϣ������������Ϣ
						logService.insertMessage(comId, state, uin,carnumber, orderId, money, duration,0, btime, etime,0);
					}
				}else{// if(cType==2){
					logger.error(">>>>>>�г�����Ϣ������Ϣ������....");
					if(uin!=-1)
						logService.insertMessage(comId, 1, uin,(String)orderMap.get("car_number"), orderId, money, StringUtils.getTimeString(btime, etime),0, btime, etime,0);
				}
				//20160301pay_type = 4Ҳд�ֽ��¼
				if((pay_type == 1|| pay_type ==4) && _state == 0){//д�ֽ���ϸ
					Long c = onlyReadService.getLong("select count (*) from parkuser_cash_tb where orderid=? ", new Object[]{orderId}) ;
					if(c>0){//�Ѿ����ڣ����¼�¼
						int r = daService.update("update parkuser_cash_tb set uin =?,amount=?,type=?,create_time=? where orderid=? ",
								new Object[] { uid, money, 0, System.currentTimeMillis() / 1000 , orderId});
						logger.error("completeorder>>>>дһ���ֽ�֧����ϸorderid:"+orderId+",money:"+money+",uid:"+uid+"r:"+r);
					}else {
						int r = daService.update("insert into parkuser_cash_tb(uin,amount,type,orderid,create_time) values(?,?,?,?,?)",
								new Object[] { uid, money, 0, orderId, System.currentTimeMillis() / 1000 });
						logger.error("completeorder>>>>дһ���ֽ�֧����ϸorderid:"+orderId+",money:"+money+",uid:"+uid+"r:"+r);
					}
				}
				if(!isShuTong && uin!=-1){//���ͽ��㶩������Ϣ������΢�Ź��ں�
					Integer state = (Integer)orderMap.get("state");
					pay_type = (Integer)orderMap.get("pay_type");
					if(pay_type != 2&& state == 1 && money > 0 && orderMap.get("end_time") != null){
						String openid = null;
						Map<String, Object> userMap = onlyReadService.getMap(
								"select wxp_openid from user_info_tb where id=? and wxp_openid is not null ",
								new Object[] { uin });
						if(userMap != null){
							openid = (String)userMap.get("wxp_openid");
						}else{
							userMap = onlyReadService
									.getMap("select openid from wxp_user_tb where uin=? ",
											new Object[] { uin });
							if(userMap != null){
								openid = (String)userMap.get("openid");
							}
						}
//						if(openid != null){
//							//sendWxpMsg(orderMap, openid, money);
//						}
					}
				}
				logger.error(">>>>>>NFC �������� ....��� ��"+result);
				AjaxUtil.ajaxOutput(response, ""+result);
			}
			//http://127.0.0.1/zld/nfchandle.do?action=completeorder&orderid=78&collect=20&comid=3&carnumber=
		}else if(action.equals("doprepayorder")){//����Ԥ���Ѷ���
			String result = doprepayorder(request,comId,uid);
			//http://127.0.0.1/zld/nfchandle.do?action=doprepayorder&orderid=829931&collect=0.03&comid=3251&uid=20551
			AjaxUtil.ajaxOutput(response, result);
			return null;
		}else if(action.equals("toprepay")){
			//http://127.0.0.1/zld/nfchandle.do?action=toprepay&orderid=829931&money=0.03&comid=3251&uid=20551
			Map<String, Object> infoMap = new HashMap<String, Object>();
			Long orderId = RequestUtil.getLong(request, "orderid", -1L);
			Double money = RequestUtil.getDouble(request, "money", 0d);
			Long uin = -1L;
			logger.error("toprepay>>>orderid:"+orderId+",money:"+money+",comid:"+comId);
			int result = 0;
			int auto = 0;
			if(orderId > 0){
				//�鳵�����ã��Ƿ��������Զ�֧����û������ʱ��Ĭ��25Ԫ�����Զ�֧�� 
				Map<String, Object> orderMap = daService.getMap("select * from order_tb where id=? and state=? ", new Object[]{orderId, 0});
				if(orderMap != null){
					Double prefee = 0d;
					if(orderMap.get("total") != null){
						prefee = Double.valueOf(orderMap.get("total") + "");
					}
					if(orderMap.get("uin") != null){
						uin = (Long)orderMap.get("uin");
					}
					logger.error("toprepay>>>orderid:"+orderId+",prefee:"+prefee+",uin:"+uin);
					if(uin != null && uin > 0){
						Map<String, Object> userMap = daService.getMap("select * from user_info_tb where id=? ", 
								new Object[]{uin});
						if(userMap != null && prefee == 0){
							Double balance = 0d;
							if(userMap.get("balance") != null){
								balance = Double.valueOf(userMap.get("balance") + "");
							}
							Long btime = (Long)orderMap.get("create_time");
							Long etime = System.currentTimeMillis()/1000;
							String duration = StringUtils.getTimeString(btime, etime);
							String carnumber = null;
							if(orderMap.get("car_number") != null){
								carnumber = (String)orderMap.get("car_number");
							}
							if(uid == -1){
								if(orderMap.get("uid") != null){
									uid = (Long)orderMap.get("uid");
								}
							}
							Integer autoCash=1;
							Map upMap = daService.getPojo("select auto_cash,limit_money from user_profile_tb where uin =?", new Object[]{uin});
							Integer limitMoney =25;
							if(upMap!=null&&upMap.get("auto_cash")!=null){
								autoCash= (Integer)upMap.get("auto_cash");
								limitMoney = (Integer)upMap.get("limit_money");
							}
							logger.error("toprepay>>>orderid:"+orderId+",uin:"+uin+",autoCash:"+autoCash+",balance:"+balance+",uid:"+uid);
							if(autoCash!=null&&autoCash==1){//�������Զ�֧��
								boolean isupmoney=true;//�Ƿ�ɳ����Զ�֧���޶�
								if(limitMoney!=null){
									if(limitMoney==-1||limitMoney>=money)//����ǲ��޻����֧�������Զ�֧�� 
										isupmoney=false;
								}
								logger.error("toprepay>>>orderid:"+orderId+",uin:"+uin+",isupmoney:"+isupmoney);
								if(!isupmoney){
									//==============�Զ�ѡȯ�߼�begin===============//
									Map<String, Object> ticketMap = null;
									boolean isAuth = publicMethods.isAuthUser(uin);
									List<Map<String, Object>> ticketlList = methods.chooseTicket(uin, money, 2, uid, isAuth, 2, comId, orderId, 0);
									if(ticketlList != null && !ticketlList.isEmpty()){
										Map<String, Object> map = ticketlList.get(0);
										if(map.get("iscanuse") != null && (Integer)map.get("iscanuse") == 1){
											ticketMap = map;
										}
									}
									Double tickMoney = 0d;//����ͣ��ȯ���
									Long ticketId = -1L;//ͣ��ȯID
									if(ticketMap!=null){
										tickMoney = StringUtils.formatDouble(ticketMap.get("limit"));
										ticketId = (Long)ticketMap.get("id");
									}
									logger.error("toprepay>>>orderid:"+orderId+",uin:"+uin+",ticketid:"+ticketId+",ticketmoney:"+tickMoney+",balance:"+balance);
									//==============�Զ�ѡȯ�߼�end===============//
									if((balance+tickMoney) >= money){
										auto = 1;
									}
								}
							}
							//����������Ϣ��֧��
							result = 1;
							logService.insertMessage(comId, 1, uin,carnumber, orderId, money, duration,0, btime, etime,10);
						}
					}
				}
			}
			infoMap.put("auto", auto);
			infoMap.put("result", result);
			infoMap.put("orderid", orderId);
			infoMap.put("money", money);
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap)) ;
		}else if(action.equals("autoprepay")){
			//http://127.0.0.1/zld/nfchandle.do?action=autoprepay&orderid=829931&money=0.03&comid=3251&uid=20551
			Map<String, Object> infoMap = new HashMap<String, Object>();
			Long orderId = RequestUtil.getLong(request, "orderid", -1L);
			Double money = RequestUtil.getDouble(request, "money", 0d);
			Long uin = -1L;
			logger.error("autoprepay>>>orderid:"+orderId+",money:"+money+",comid:"+comId);
			int result = 0;
			if(orderId > 0){
				//�鳵�����ã��Ƿ��������Զ�֧����û������ʱ��Ĭ��25Ԫ�����Զ�֧�� 
				Map<String, Object> orderMap = daService.getMap("select * from order_tb where id=? and state=? ", new Object[]{orderId, 0});
				if(orderMap != null){
					Double prefee = 0d;
					if(orderMap.get("total") != null){
						prefee = Double.valueOf(orderMap.get("total") + "");
					}
					if(orderMap.get("uin") != null){
						uin = (Long)orderMap.get("uin");
					}
					logger.error("autoprepay>>>orderid:"+orderId+",prefee:"+prefee+",uin:"+uin);
					if(uin != null && uin > 0){
						Map<String, Object> userMap = daService.getMap("select * from user_info_tb where id=? ", 
								new Object[]{uin});
						if(userMap != null && prefee == 0){
							Double balance = 0d;
							if(userMap.get("balance") != null){
								balance = Double.valueOf(userMap.get("balance") + "");
							}
							Long btime = (Long)orderMap.get("create_time");
							Long etime = System.currentTimeMillis()/1000;
							String duration = StringUtils.getTimeString(btime, etime);
							String carnumber = null;
							if(orderMap.get("car_number") != null){
								carnumber = (String)orderMap.get("car_number");
							}
							if(uid == -1){
								if(orderMap.get("uid") != null){
									uid = (Long)orderMap.get("uid");
								}
							}
							Integer autoCash=1;
							Map upMap = daService.getPojo("select auto_cash,limit_money from user_profile_tb where uin =?", new Object[]{uin});
							Integer limitMoney =25;
							if(upMap!=null&&upMap.get("auto_cash")!=null){
								autoCash= (Integer)upMap.get("auto_cash");
								limitMoney = (Integer)upMap.get("limit_money");
							}
							logger.error("autoprepay>>>orderid:"+orderId+",uin:"+uin+",autoCash:"+autoCash+",balance:"+balance+",uid:"+uid);
							if(autoCash!=null&&autoCash==1){//�������Զ�֧��
								boolean isupmoney=true;//�Ƿ�ɳ����Զ�֧���޶�
								if(limitMoney!=null){
									if(limitMoney==-1||limitMoney>=money)//����ǲ��޻����֧�������Զ�֧�� 
										isupmoney=false;
								}
								logger.error("autoprepay>>>orderid:"+orderId+",uin:"+uin+",isupmoney:"+isupmoney);
								if(!isupmoney){
									//==============�Զ�ѡȯ�߼�begin===============//
									Map<String, Object> ticketMap = null;
									boolean isAuth = publicMethods.isAuthUser(uin);
									List<Map<String, Object>> ticketlList = methods.chooseTicket(uin, money, 2, uid, isAuth, 2, comId, orderId, 0);
									if(ticketlList != null && !ticketlList.isEmpty()){
										Map<String, Object> map = ticketlList.get(0);
										if(map.get("iscanuse") != null && (Integer)map.get("iscanuse") == 1){
											ticketMap = map;
										}
									}
									Double tickMoney = 0d;//����ͣ��ȯ���
									Long ticketId = -1L;//ͣ��ȯID
									if(ticketMap!=null){
										tickMoney = StringUtils.formatDouble(ticketMap.get("limit"));
										ticketId = (Long)ticketMap.get("id");
									}
									logger.error("autoprepay>>>orderid:"+orderId+",uin:"+uin+",ticketid:"+ticketId+",ticketmoney:"+tickMoney+",balance:"+balance);
									//==============�Զ�ѡȯ�߼�end===============//
									if((balance+tickMoney) >= money){
										int r = publicMethods.prepay(orderMap, money, uin, ticketId, 0, 1, null);
										result = r;
										logger.error("autoprepay>>>orderid:"+orderId+",uin:"+uin+",r:"+r);
										if(r == 1){
											logService.insertUserMesg(2, uin,"Ԥ��ͣ����"+money+"Ԫ���Զ�֧���ɹ���", "�Զ�֧������");
										}else{
											logService.insertUserMesg(0, uin, "��������ԭ��Ԥ��ͣ����"+money+"Ԫ���Զ�֧��ʧ�ܡ�", "֧��ʧ������");
										}
//										if(comId == 20130){
//											String re = methods.sendPrepay2Baohe(orderId, r, money, 0);
//											logger.error("autoprepay>>>orderid:"+orderId+",uin:"+uin+",re:"+re);
//										}
									}
								}
							}
						}
					}
				}
			}
			infoMap.put("result", result);
			infoMap.put("orderid", orderId);
			infoMap.put("money", money);
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap)) ;
			return null;
		}else if(action.equals("hdderate")){
			Long orderid = RequestUtil.getLong(request, "orderid", -1L);
			Integer time = RequestUtil.getInteger(request, "time", 0);
			Integer type = RequestUtil.getInteger(request, "type", 3);
			logger.error("orderid:"+orderid+",time:"+time+",type:"+type);
			
			Map<String, Object> rMap = new HashMap<String, Object>();
			if(orderid == -1){
				rMap.put("result", "-1");
				AjaxUtil.ajaxOutput(response, StringUtils.createJson(rMap));
				return null;
			}
			Map<String, Object> orderMap = daService.getMap("select pay_type,end_time from order_tb where pay_type=? and state=? and id=? ", new Object[]{1, 1, orderid});
			if(orderMap == null){
				rMap.put("result", "-1");
				AjaxUtil.ajaxOutput(response, StringUtils.createJson(rMap));
				return null;
			}
			
			Long count = daService.getLong("select count(id) from ticket_tb where orderid=? and (type=? or type=?) ", 
					new Object[]{orderid, 3, 4});
			if(count > 0){
				rMap.put("result", "-2");
				AjaxUtil.ajaxOutput(response, StringUtils.createJson(rMap));
				return null;
			}
			
			SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
			String nowtime= df2.format(System.currentTimeMillis());
			Long etime =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(nowtime+" 23:59:59");
			Long ticketid = daService.getkey("seq_ticket_tb");
			int r = daService.update("insert into ticket_tb(id,create_time,utime,limit_day,money,state,comid,type) values(?,?,?,?,?,?,?,?)", 
					new Object[]{ticketid, ntime, ntime, etime, time, 0, comId,type});
			if(r == 1){
				Long endtime = (Long)orderMap.get("end_time");
				Map<String, Object> map = methods.getOrderInfo(orderid, ticketid, endtime);
				logger.error("orderid:"+orderid+",map:"+map);
				daService.update("update ticket_tb set state=? where id=? ", 
						new Object[]{1, ticketid});
				
				Double afttotal = Double.valueOf(map.get("aftertotal") + "");
				Double beftotal = Double.valueOf(map.get("beforetotal") + "");
				
				Double distotal = 0d;
				if(beftotal > afttotal){
					int res = daService.update("update order_tb set total=? where id=? ", new Object[]{afttotal, orderid});
					daService.update("update parkuser_cash_tb set amount=? where orderid =? ", new Object[]{afttotal,orderid});
					distotal = StringUtils.formatDouble(beftotal - afttotal);
				}
				rMap.put("result", "1");
				rMap.put("collect", afttotal);
				rMap.put("befcollect", beftotal);
				rMap.put("distotal", distotal);
				rMap.put("shopticketid", ticketid);
				rMap.put("tickettype", map.get("tickettype"));
				rMap.put("tickettime", map.get("tickettime"));
				logger.error( StringUtils.createJson(rMap));
				AjaxUtil.ajaxOutput(response, StringUtils.createJson(rMap));
			}
			return null;
			//http://192.168.199.239/zld/nfchandle.do?action=hdderate&orderid=14363028&type=3&comid=15989&time=1&uid=375361
		}else if(action.equals("test")){
			String btime = RequestUtil.processParams(request, "btime");
			String etime = RequestUtil.processParams(request, "etime");
			//String uid = RequestUtil.processParams(request, "uuid");
			Long comid = RequestUtil.getLong(request, "comid",-1L);
			//��ѯ�Ƿ��ж���
			Map orderMap = null;//daService.getPojo("select * from order_tb where comid=? and nfc_uuid=? and state=?", 
				//	new Object[]{comid,uuid,0});
			Long start = TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(btime);
			Long end = TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(etime);
			AjaxUtil.ajaxOutput(response, doOrderTest(comid,orderMap,start,end));
		}
		
		return null;
	}

	/**
	 * Ԥ֧������
	 * @param request
	 * @param comId
	 * @return
	 */
	private String doprepayorder(HttpServletRequest request,Long comId,Long uid){
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);//�������
		Double money = RequestUtil.getDouble(request, "collect", 0d);//ʵ�ս��
		Long out_passid = RequestUtil.getLong(request, "passid", -1L);//����ͨ��id
		Long _uid = RequestUtil.getLong(request, "uid", -1L);
		logger.error(">>>>>>>>>>>>����Ԥ֧������,orderid:"+orderId);
		Map orderMap = null;
		String result = "{\"result\":\"-1\"}";
		if(orderId!=-1){
			if(out_passid!=-1){
				int ret = daService.update("update order_tb set out_passid=?,uid=? where id =? ",
						new Object[]{out_passid,_uid,orderId});
				logger.error(">>>>nfchandle->doprepayorder->add out_passid="+out_passid+",uid="+_uid+",orderid="+orderId+", ret:"+ret+",orderid:"+orderId);
			}
			orderMap = daService.getMap("select * from order_tb where id=? and state=? ",new Object[]{orderId, 0});
			if(orderMap!=null){
				DecimalFormat dFormat = new DecimalFormat("#.00");
				Integer pay_type = (Integer)orderMap.get("pay_type");
				Double prefee =StringUtils.formatDouble(orderMap.get("total"));
				logger.error(">>>>>>>>>>����Ԥ֧����orderid��"+orderId+",prefee:"+prefee+",pay_type:"+pay_type);
				if(prefee>0){//������Ԥ����
					if(pay_type ==4|| pay_type == 5 || pay_type== 6){//����Ԥ֧��
						Map<String, Object> resultMap = publicMethods.doMidPayOrder(orderMap, money,uid);
						logger.error("middoprepay>>>>:orderid:"+orderId+",������:"+resultMap.toString());
						if(resultMap != null){
							Integer r = (Integer)resultMap.get("result");
							if(r == 1){
								prefee = Double.valueOf(resultMap.get("prefee") + "");//ʵ��Ԥ֧���Ľ��(���¼���ֿۺ�)
								if(prefee >= money){
									result="{\"result\":\"1\"}";
								}else{
									result="{\"result\":\"2\",\"prefee\":\""+prefee+"\",\"total\":\""+money+"\",\"collect\":\""+Double.valueOf(dFormat.format(money-prefee))+"\"}";
								}
							}
							if(comId==20130){//���͹�˾�Ĳ��Զ������������ǵĽӿڷ��Ͷ���֧��״̬
								String sr = methods.sendOrderState2Baohe(Long.valueOf(orderId), r,money, pay_type);
								logger.error(">>>>>>>>>>>>>baohe sendresult:"+sr+",orderid:"+orderId+",state:"+r+",total:"+money+",paytype:"+pay_type);
							}
						}
					}else{
						Integer ret = publicMethods.doPrePayOrder(orderMap, money);
						if(ret==1){
							if(prefee>=money){
								result="{\"result\":\"1\"}";
							}else {
								result="{\"result\":\"2\",\"prefee\":\""+prefee+"\",\"total\":\""+money+"\",\"collect\":\""+Double.valueOf(dFormat.format(money-prefee))+"\"}";
							}

							try {
								methods.updateRemainBerth(comId, 1);
							} catch (Exception e) {
								// TODO: handle exception
								logger.error("update remain error>>>orderid:"+orderId+",comid:"+comId, e);
							}
						}
						if(comId==20130){//���͹�˾�Ĳ��Զ������������ǵĽӿڷ��Ͷ���֧��״̬
							String sr = methods.sendOrderState2Baohe(Long.valueOf(orderId), ret,money, pay_type);
							logger.error(">>>>>>>>>>>>>baohe sendresult:"+sr+",orderid:"+orderId+",state:"+ret+",total:"+money+",paytype:"+pay_type);
						}
					}
				}
			}
		}
		if(publicMethods.isEtcPark(comId)){
//				Integer local = RequestUtil.getInteger(request, "local", -1);//����-1   ��������������֧��Ϊ1
//				if(local<0){
			int ret = daService.update("update order_tb set type=? where id=? and type=?",new Object[]{5,orderId,4});
			logger.error("���ػ������л������Ͻ��㶩����ǣ����ط���������ʱͬ�����㱾�أ�result��"+ret+",orderid:"+orderId);
//				}
		}
		return result;
	}

	private String getUUIDByNid(Long nid) {
		Map nfcMap = daService.getMap("select nfc_uuid from com_nfc_tb where nid=? ", new Object[]{nid});
		String uuid = "";
		if(nfcMap!=null&&nfcMap.get("nfc_uuid")!=null)
			uuid=(String)nfcMap.get("nfc_uuid");
		logger.error(">>>>>>>nid:"+nid+",nid->uuid:"+uuid);
		if(uuid!=null&&uuid.length()>10)
			return uuid.trim();
		return "";
	}
	
	private void sendWxpMsg(Map<String, Object> orderMap, String openid, Double money){
		Map<String, Object> comMap = onlyReadService.getMap(
				"select company_name from com_info_tb where id=? ",
				new Object[] { orderMap.get("comid") });
		String create_time  = TimeTools.getTime_yyyyMMdd_HHmm((Long)orderMap.get("create_time") * 1000);
		if(comMap != null){
			//����ģ����Ϣ
			JSONObject msgObject = new JSONObject();
			JSONObject dataObject = new JSONObject();
			JSONObject firstObject = new JSONObject();
			firstObject.put("value", "����"+comMap.get("company_name")+"��δ֧������,����15����֮��֧������");
			firstObject.put("color", "#000000");
			JSONObject keynote1Object = new JSONObject();
			JSONObject keynote2Object = new JSONObject();
			JSONObject keynote3Object = new JSONObject();
			keynote1Object.put("value", money+"Ԫ");
			keynote1Object.put("color", "#000000");
			keynote2Object.put("value", create_time);
			keynote2Object.put("color", "#000000");
			keynote3Object.put("value", orderMap.get("id"));
			keynote3Object.put("color", "#000000");
			JSONObject remarkObject = new JSONObject();
			remarkObject.put("value", "���ȥ֧������");
			remarkObject.put("color", "#FF0000");
			dataObject.put("first", firstObject);
			dataObject.put("o_money", keynote1Object);
			dataObject.put("order_date", keynote2Object);
			dataObject.put("o_id", keynote3Object);
			dataObject.put("Remark", remarkObject);
			
			msgObject.put("touser", openid);
			msgObject.put("template_id", Constants.WXPUBLIC_ORDER_NOTIFYMSG_ID);
			msgObject.put("url", "http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/wxpfast.do?action=topayorder&openid="+openid+"&orderid="+orderMap.get("id")+"&total="+money);
			msgObject.put("topcolor", "#000000");
			msgObject.put("data", dataObject);
			String accesstoken = publicMethods.getWXPAccessToken();
			String msg = msgObject.toString();
			PayCommonUtil.sendMessage(msg, accesstoken);
		}
	}

	/**
	 * ע���û�
	 * @param mobile
	 * @param carNumber
	 */
	private String reguser(String mobile, String carNumber,Long uid,Long comId) {
		String sql = "select count(id) from user_info_tb where mobile=? and auth_flag=? ";
		Long ucount = daService.getLong(sql, new Object[]{mobile,4});
		String result = "0";
		Long ntime = System.currentTimeMillis()/1000;
		if(ucount>0){//�ֻ���ע���
			result="�ֻ���ע���";
		}else {//ע�ᳵ�� 
			Long uin= daService.getLong("SELECT nextval('seq_user_info_tb'::REGCLASS) AS newid", null);
			String strid = uin+"zld";
			int r = daService.update("insert into user_info_tb (id,nickname,password,strid," +
						"reg_time,mobile,auth_flag,comid,recom_code,media) values (?,?,?,?,?,?,?,?,?,?)",
						new Object[]{uin,"����",strid,strid,ntime,mobile,4,0,uid,999});
			if(r==1){//ע��ɹ�
				//д���ƺ�
				Long ccount = daService.getLong("select count(*) from car_info_tb where car_number=? ", new Object[]{carNumber});
				if(ccount>0){//���ƺ��Ѿ���ע���
					return "���ƺ��Ѿ���ע���";
				}
				daService.update("insert into car_info_tb (uin,car_number,create_time) values (?,?,?)",
						new Object[]{uin,carNumber,ntime});
				Long time = TimeTools.getToDayBeginTime();
				time = time + 16*24*60*60-1;
//				int e = daService.update("insert into ticket_tb (uin,create_time,limit_day,money,state) values(?,?,?,?,?)",
//						new Object[]{uin,System.currentTimeMillis()/1000,time,3,0});
//				String tsql = "insert into ticket_tb (create_time,limit_day,money,state,uin) values(?,?,?,?,?) ";
//				//��30Ԫͣ��ȯ�����ţ�20,5,3,2��
//				List<Object[]> values = new ArrayList<Object[]>();
//				Long ntime = System.currentTimeMillis()/1000;
//				Object[] v1 = new Object[]{ntime,time,2,0,uin};
//				Object[] v2 = new Object[]{ntime,time,3,0,uin};
//				Object[] v3 = new Object[]{ntime,time,5,0,uin};
//				Object[] v4 = new Object[]{ntime,time,20,0,uin};
//				values.add(v1);values.add(v2);values.add(v3);values.add(v4);
				int e=publicMethods.backNewUserTickets(ntime, uin);//daService.bathInsert(tsql, values, new int[]{4,4,4,4,4});
				
				if(e==0){
					String bsql = "insert into bonus_record_tb (bid,ctime,mobile,state,amount) values(?,?,?,?,?) ";
					Object [] values = new Object[]{999,ntime,mobile,0,10};//�Ǽ�Ϊδ��ȡ�������¼ʱд��ͣ��ȯ���ж��Ƿ��Ǻ�������
					logger.error(">>>>>>>>�շ�Ա�Ƽ����������û�������10Ԫȯ����д��ͣ��ȯ���û���¼�󷵻أ�"+daService.update(bsql,values));	
				}
				
				int	eb = daService.update("insert into user_profile_tb (uin,low_recharge,limit_money,auto_cash," +
						"create_time,update_time) values(?,?,?,?,?,?)", 
						new Object[]{uin,10,25,1,ntime,ntime});
				//д���Ƽ���¼
				int rt = daService.update("insert into recommend_tb (pid,nid,type,state,create_time) values(?,?,?,?,?)",
						new Object[]{uid,uin,0,0,ntime});
//				if(uid!=null&&comId!=null)
//					logService.updateScroe(5, uid, comId);//�Ƽ���������1���� 
				System.out.println(">>>>>>>>>����ȯ�����"+e+"��(20,5,3,2)���Ƽ���¼��"+rt+",��Ч������"+TimeTools.getTime_yyyyMMdd_HHmmss(ntime*1000)+",����Ĭ��֧��:"+eb);
			}else {//ע��ʧ��
				return "ע��ʧ��";
			}
		}
		return result;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String doOrderTest(Long comId,Map orderMap,Long start,Long end){
		String pid = CustomDefind.CUSTOMPARKIDS;
	//	System.out.println(">>>>>>>>>>>>>custom park price:parkid:"+pid);
		if(comId.intValue()==Long.valueOf(pid).intValue()){//���Ƽ۸����
			return StringUtils.getTimeString(start, end)+":"+publicMethods.getCustomPrice(start, end, 1);
		}
		Map dayMap=null;//�ռ����
		Map nigthMap=null;//ҹ�����
		//��ʱ�μ۸����
		List<Map<String,Object>> priceList=daService.getAll("select * from price_tb where comid=? " +
				"and state=? and pay_type=? order by id desc", new Object[]{comId,0,0});
		if(priceList==null||priceList.size()==0){//û�а�ʱ�β���
			//�鰴�β���
			priceList=daService.getAll("select * from price_tb where comid=? " +
					"and state=? and pay_type=? order by id desc", new Object[]{comId,0,1});
			//Long btLong = (Long)orderMap.get("create_time");
			//Long et = System.currentTimeMillis();
			String btime = TimeTools.getTime_MMdd_HHmm(start*1000).substring(6);
			String etime = TimeTools.getTime_MMdd_HHmm(end).substring(6);
			Map<String, Object> orMap=new HashMap<String, Object>();
			if(priceList==null||priceList.size()==0){//û�а��β��ԣ�������ʾ
				orMap.put("collect", "δ���ü۸�");
				orMap.put("btime", btime);
				orMap.put("etime", etime);
				//return StringUtils.createJson(orMap);//;
			}else {//�а��β��ԣ�ֱ�ӷ���һ�ε��շ�
				Map timeMap =priceList.get(0);
				Object ounit  = timeMap.get("unit");
				if(ounit!=null){
					Integer unit = Integer.valueOf(ounit.toString());
					if(unit>0){
						//Long start = (Long)orderMap.get("create_time");
						//Long end = System.currentTimeMillis() / 1000;
						Long du = (end-start)/60;//ʱ������
						int times = du.intValue()/unit;
						if(du%unit!=0)
							times +=1;
						double total = times*Double.valueOf(timeMap.get("price")+"");
						orMap.put("collect", total);
						orMap.put("btime", btime);
						orMap.put("etime", etime);
						orMap.put("total", total);
						orMap.put("duration", StringUtils.getTimeString(start, end));
						orMap.put("orderid", "111");
						//��{total=0.0, duration=0����, 
						//etime=17:18, btime=17:18, uin=-1, orderid=17468, collect=0.0, discount=0.0}
						return orMap.get("collect")+"";//timeMap.get("price")+"";
					}
				}
				orMap.put("collect", timeMap.get("price"));
				orMap.put("btime", btime);
				orMap.put("etime", etime);
				return StringUtils.createJson(orMap);//timeMap.get("price")+"";
			}
			//�����Ÿ�����Ա��ͨ�����úü۸�
		}else {//�Ӱ�ʱ�μ۸�����зּ���ռ��ҹ���շѲ���
			dayMap= priceList.get(0);
			boolean pm1 = false;//�ҵ�map1,�����ǽ���ʱ����ڿ�ʼʱ��
			boolean pm2 = false;//�ҵ�map2
			if(priceList.size()>1){
				for(Map map : priceList){
					if(pm1&&pm2)
						break;
					Integer btime = (Integer)map.get("b_time");
					Integer etime = (Integer)map.get("e_time");
					if(btime==null||etime==null)
						continue;
					if(etime>btime){
						if(!pm1){
							dayMap = map;
							pm1=true;
						}
					}else {
						if(!pm2){
							nigthMap=map;
							pm2=true;
						}
					}
				}
			}
		}
//		System.out.println("�ռ䣺"+dayMap);
//		System.err.println("ҹ�䣺"+nigthMap);
		//test
//		Long startLong = 1405872000L;
//		Long endLong =1406340000L; countPrice
		Map com =daService.getPojo("select * from com_info_tb where id=? "
				, new Object[]{comId});
		double minPriceUnit = Double.valueOf(com.get("minprice_unit")+"");
		Map assistMap = daService.getMap("select * from price_assist_tb where comid = ? and type = ?", new Object[]{comId,0});
		Map<String, Object> orMap=CountPrice.getAccount(start,end, dayMap, nigthMap,minPriceUnit,assistMap);
//		Map<String, Object> orMap=CountPrice.getAccount((Long)orderMap.get("create_time"),
//				System.currentTimeMillis() / 1000, dayMap, nigthMap);
		//orMap.put("orderid", orderMap.get("id"));
//		System.out.println(orMap);
		List<Map<String, Object>>  list = new ArrayList<Map<String,Object>>();
		list.add(orMap);
//		Map<String, Object> orMap1=CountPrice.getAccount(start,end, dayMap, nigthMap);
//		list.add(orMap1);
		return orMap.get("collect")+"";//StringUtils.createJson(list);	
	}
	
	//���ж��¿�
		/*@SuppressWarnings("unchecked")
		private boolean isMonthUser(Long comId,Long uin){
			Long ntime = System.currentTimeMillis()/1000;
			boolean isVip = false;
			Map<String, Object> pMap = daService.getMap("select p.b_time,p.e_time,p.type from product_package_tb p," +
					"carower_product c where c.pid=p.id and p.comid=? and c.uin=? and c.e_time>? order by c.id desc limit ?", 
					new Object[]{comId,uin,ntime,1});
			if(pMap!=null&&!pMap.isEmpty()){
				System.out.println(pMap);
				Integer b_time = (Integer)pMap.get("b_time");
				Integer e_time = (Integer)pMap.get("e_time");
				Calendar c = Calendar.getInstance();
				Integer hour = c.get(Calendar.HOUR_OF_DAY);
				Integer type = (Integer)pMap.get("type");//0:ȫ�� 1ҹ�� 2�ռ�
				if(type==0){//0:ȫ�� 1ҹ�� 2�ռ�
					isVip = true;
				}else if(type==2){//0:ȫ�� 1ҹ�� 2�ռ�
					if(hour>=b_time&&hour<=e_time){
						isVip = true;
					}
				}else if(type==1){//0:ȫ�� 1ҹ�� 2�ռ�
					if(hour<=e_time||hour>=b_time){
						isVip = true;
					}
				}
			}
			return isVip;
		}*/
}
