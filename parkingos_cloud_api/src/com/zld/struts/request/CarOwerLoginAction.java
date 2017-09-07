package com.zld.struts.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zld.AjaxUtil;
import com.zld.easemob.apidemo.EasemobIMUsers;
import com.zld.easemob.main.HXHandle;
import com.zld.impl.CommonMethods;
import com.zld.impl.MemcacheUtils;
import com.zld.impl.PublicMethods;
import com.zld.service.DataBaseService;
import com.zld.utils.JsonUtil;
import com.zld.utils.RequestUtil;
import com.zld.utils.SendMessage;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;

public class CarOwerLoginAction extends Action{
	
	@Autowired
	private DataBaseService daService;
	
	private Logger logger = Logger.getLogger(CarOwerLoginAction.class);
	
	@Autowired
	private MemcacheUtils memcacheUtils;
	@Autowired
	private PublicMethods publicMethods;
	@Autowired
	private CommonMethods methods;
	
	
	/**
	 *������¼��ע�ᣬ��֤�봦��
	 * �����룺
	 * 4:ע��ɹ������Ƽ��벻����
	 * 3:���Ʊ���ɹ�
	 * 2:���복�ƺ�
	 * 1:��¼�ɹ���
	 * 0:������֤�룬
	 * -1����֤����Ч��
	 * -2��ע��ʧ�ܣ�
	 * -3�����ֻ�������֤��ʧ�ܣ�
	 * -4����ɫ����
	 * -5:�ֻ��������
	 * -6��ϵͳ��֤�벻����
	 * -7:������֤�����
	 * -8:���Ʊ���ʧ��
	 * -9:�����Ѵ��� 
	 * test : http://s.tingchebao.com/zld/carlogin.do?action=login&mobile=15920157107
	 *http://127.0.0.1/zld/carlogin.do?action=login&mobile=15920157107
	 *http://192.168.0.188/zld//carlogin.do?action=validcode&code=1234&mobile=13641309140&hx=1
	 *http://192.168.0.188/zld//carlogin.do?action=addcar&carnumber=��GHR009&mobile=18101333937
	 *http://s.zhenlaidian.com/zld//carlogin.do?action=validcode&code=6271&mobile=15801682643
	 *�¼Ӻ���������2015-03-10 by LaoYao
	 *��ע���û����¼�û��ϴ�������IMEI�ţ���ע��ʱ����ѯIMEI�Ŵ��ڵ�������������������ϣ���ע����û�ֱ�ӷ��������������ȯ
	 *��¼�û��ϴ�������IMEI�ţ�Ҳ��ѯIMEI�Ŵ��ڵ�������������������ϣ����û�ֱ�ӷ��������
	 *ͬʱ�޸����к�����ע�ἰ�Ƽ���ڣ�����ֱ��ע���³�����������¼ʱ�ٸ����Ƿ��Ǻ��������û��������Ƿ�ȯ
	 *����������ĳ���ͣ��ʱ�����ٷ�ȯ���Ҳ������ڳ������֡�
	 */
	
	@SuppressWarnings({ "rawtypes"})
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String action = RequestUtil.processParams(request, "action");
		String mobile =RequestUtil.processParams(request, "mobile");
		String openid =RequestUtil.processParams(request, "openid");
		//logger.error("action:"+action+",mobile:"+mobile);
		/*if(mobile==null||"".equals(mobile)){
			AjaxUtil.ajaxOutput(response, "-5");
			return null;
		}*/
		if(action.equals("dologin")){//������¼���ʺŲ�����ʱ��ע���ϣ�������֤��,������ȡ��֤�루���Ż�ȡ��֤�룩
			String sql = "select * from user_info_tb where mobile=? and auth_flag=? ";
			Map user = daService.getPojo(sql, new Object[]{mobile,4});
			String imei  =  RequestUtil.getString(request, "imei").trim();//�ֻ�����
			Map<String,Object> infoMap = new HashMap();//����ֵ
			if(!publicMethods.isCanSendShortMesg(mobile)){
				infoMap.put("mesg", "-2");
				AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
				return null;
			}
			Long uin = null;
			if(user==null){
				//ע�ᳵ�� 
				uin= daService.getLong("SELECT nextval('seq_user_info_tb'::REGCLASS) AS newid", null);
				int r = createCarOwerInfo(request, uin);
				if(r==1){
					boolean isBlack = doValidateBlackByImei(imei,uin,mobile);//�Ƿ��Ǻ���������
					user = daService.getPojo("select * from user_info_tb where id = ?", new Object[]{uin});
					//���û�ע�ᣬ��3Ԫͣ��ȯ,��Ч��15��
					Long time = TimeTools.getToDayBeginTime();
					Long etime = time + 6*24*60*60-1;
					Long ntime = System.currentTimeMillis()/1000;
					if(!isBlack){//���Ǻ����������ŷ�ȯ
						if(!methods.checkBonus(mobile,uin)){//û�к��ʱ����ʮԪȯ
							try {
								daService.update("insert into ticket_tb (uin,create_time,limit_day,money,state) values(?,?,?,?,?)",
										new Object[]{uin,time,etime,1,0});
								daService.update("insert into ticket_tb (uin,create_time,limit_day,money,state) values(?,?,?,?,?)",
										new Object[]{uin,time,etime,2,0});
								daService.update("insert into ticket_tb (uin,create_time,limit_day,money,state) values(?,?,?,?,?)",
										new Object[]{uin,time,etime,3,0});
								daService.update("insert into ticket_tb (uin,create_time,limit_day,money,state) values(?,?,?,?,?)",
										new Object[]{uin,time,etime,4,0});
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}else {
						logger.error(">>>>�ֻ����ţ�"+imei+",�Ѵ��������û����ϣ��ֻ��ţ�"+mobile+",�Ǻ������û���....����ȯ");
					}
					int	eb = daService.update("insert into user_profile_tb (uin,low_recharge,limit_money,auto_cash," +
							"create_time,update_time) values(?,?,?,?,?,?)", 
							new Object[]{uin,10,25,1,ntime,ntime});
					
					//System.out.println(">>>>>>>>>��3Ԫͣ��ȯ�����"+e+"����Ч������"+TimeTools.getTime_yyyyMMdd_HHmmss(time*1000)+",����Ĭ��֧��:"+eb);
				}else {
					infoMap.put("mesg", "-2");
					AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
					return null;
				}
			}else {
				uin = (Long)user.get("id");
			}
			Long role = (Long)user.get("auth_flag");
			if(user!=null&&role==4){
				//���Ͷ�����֤��
				Integer code = new Random(System.currentTimeMillis()).nextInt(1000000);
				if(code<99)
					code=code*10000;
				if(code<999)
					code =code*1000;
				if(code<9999)
					code = code*100;
				if(code<99999)
					code = code*10;
				if(code!=null){
					logger.error("code:"+code+",mobile:"+mobile);
					//������֤��
					//ɾ���Ѿ����浫û����֤������֤�루����Ч����֤�룩
					daService.update("delete from verification_code_tb where uin =?",new Object[]{uin});
					int r =daService.update("insert into verification_code_tb (verification_code,uin,create_time,state)" +
							" values (?,?,?,?)", new Object[]{code,uin,System.currentTimeMillis()/1000,0});
					if(r==1){
						infoMap.put("mesg", "0");
						infoMap.put("code", code);
						infoMap.put("tomobile", getToMobile(mobile));
						//�ƶ� ��ͨ  106901336275
						//������1069004270441
						AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
					}else{
						infoMap.put("mesg", "-7");
						AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
					}
				}else {
					infoMap.put("mesg", "-3");
					AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
				}
			}else {
				infoMap.put("mesg", "-4");
				AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
			}
		}else if(action.equals("login")){//������¼���ʺŲ�����ʱ��ע���ϣ�������֤��
			String ip = StringUtils.getIpAddr(request);
			//logger.error("car user login ip:"+ip);
			boolean isTruck = publicMethods.isTruck(ip);
			if(isTruck){
				AjaxUtil.ajaxOutput(response, "-1");
				logger.error("login action,mobile:"+mobile+",ip="+ip+",���ڹ���");
				return null;
			}
				
			String sql = "select * from user_info_tb where mobile=? and auth_flag=? ";
			Map user = daService.getPojo(sql, new Object[]{mobile,4});
//			if(!memcacheUtils.addLock("login_action_"+ip, 1)){//1���ظ����󲻴���
//				AjaxUtil.ajaxOutput(response, "-1");
//				logger.error("login action,mobile="+mobile+",С�������󣬲�����");
//				return null;
//			}
			
			if(!memcacheUtils.addLock("login_action_"+mobile, 1)){//1���ظ����󲻴���
				AjaxUtil.ajaxOutput(response, "-1");
				logger.error("login action,mobile="+mobile+",С�������󣬲�����");
				return null;
			}
			Long uin = null;
			if(user==null){
				//ע�ᳵ�� 
				uin= daService.getLong("SELECT nextval('seq_user_info_tb'::REGCLASS) AS newid", null);
				int r = createCarOwerInfo(request, uin);
				if(r!=1){//ע��ʧ��
					AjaxUtil.ajaxOutput(response, "-2");
					return null;
				}
				user = daService.getPojo("select * from user_info_tb where id = ?", new Object[]{uin});
				/*if(r==1){
					//���û�ע�ᣬ��10Ԫͣ��ȯ,��Ч��15��
					Long time = TimeTools.getToDayBeginTime();
					time = time + 16*24*60*60-1;
					Long ntime = System.currentTimeMillis()/1000;
					if(!checkBonus(mobile,uin))//û�к��ʱ����һ��10Ԫȯ
						daService.update("insert into ticket_tb (uin,create_time,limit_day,money,state) values(?,?,?,?,?)",
							new Object[]{uin,System.currentTimeMillis()/1000,time,10,0});
					int	eb = daService.update("insert into user_profile_tb (uin,low_recharge,limit_money,auto_cash," +
							"create_time,update_time) values(?,?,?,?,?,?)", 
							new Object[]{uin,10,25,1,ntime,ntime});
					int ret =daService.update("insert into user_message_tb(type,ctime,uin,content,title) values(?,?,?,?,?)",
							new Object[]{1,System.currentTimeMillis()/1000,uin,"��ϲ�����һ��10Ԫͣ��ȯ!", "�������"} );
					System.out.println(">>>>>>>>>��10Ԫͣ��ȯ�����"+ret+"����Ч������"+TimeTools.getTime_yyyyMMdd_HHmmss(time*1000)+",����Ĭ��֧��:"+eb);
				}else {
					AjaxUtil.ajaxOutput(response, "-2");
					return null;
				}*/
			}else {
				uin = (Long)user.get("id");
			}
			Long role = (Long)user.get("auth_flag");
			if(user!=null&&role==4){
				//���Ͷ�����֤��
				if(!publicMethods.isCanSendShortMesg(mobile)){
					AjaxUtil.ajaxOutput(response, "-7");
					return null;
				}
				Map userCode = daService.getMap("select verification_code from " +
						"verification_code_tb where create_time >? and  uin =? ", new Object[]{TimeTools.getToDayBeginTime(),uin});
				Integer code = 0;
				if(userCode!=null){
					code=(Integer)userCode.get("verification_code");
				}
				if(code==null||code==0){
					code = new SendMessage().sendMessageToCarOwer(mobile);
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
//						Map verificationMap = daService.getPojo("select verification_code from verification_code_tb" +
//								" where uin=? and state=? ", new Object[]{uin,0});
//						logger.error(verificationMap);
						if(r==1)
							AjaxUtil.ajaxOutput(response, "0");
						else{
							AjaxUtil.ajaxOutput(response, "-7");
							return null;
						}
					}
				}else {
					AjaxUtil.ajaxOutput(response, "-3");
				}
			}else {
				AjaxUtil.ajaxOutput(response, "-4");
			}
		}else if(action.equals("validcode")){//��֤���ص���֤��
			String vcode =RequestUtil.processParams(request, "code");
			//hx=1,��¼�󷵻ػ����˻������룬json��ʽ
			Integer hx = RequestUtil.getInteger(request, "hx", 0);
			String sql = "select * from user_info_tb where mobile=? and auth_flag=?";
			Map user = daService.getPojo(sql, new Object[]{mobile,4});
			String result = "-1";
			if(hx==1){
				result="{\"result\":\"-1\",\"errmsg\":\"��¼ʧ��\",\"hxname\":\"\",\"hxpass\":\"\"}";
			}
			if(user==null){
				result="-5";
				if(hx==1)
					result="{\"result\":\"-5\",\"errmsg\":\"�ֻ���δע��\",\"hxname\":\"\",\"hxpass\":\"\"}";
				AjaxUtil.ajaxOutput(response, result);
				return null;
			}
			Long uin = Long.valueOf(user.get("id")+"");
			logger.error("hx��"+hx+",code:"+vcode+",mobile:"+mobile+" ,uin="+uin);
			Map verificationMap = daService.getPojo("select verification_code from verification_code_tb" +
					" where uin=? and state=? ", new Object[]{uin,0});
			logger.error(verificationMap);
			if(verificationMap==null){
				result="-6";
				if(hx==1)
					result="{\"result\":\"-6\",\"errmsg\":\"��֤�����\",\"hxname\":\"\",\"hxpass\":\"\"}";
				AjaxUtil.ajaxOutput(response, result);
				return null;
			}
			String code = verificationMap.get("verification_code").toString();
			logger.error(code+":"+code.equals(vcode));
			if(code.equals(vcode)){//��֤��ƥ��ɹ�
				String imei  =  RequestUtil.getString(request, "imei").trim();
				//ɾ����֤���
				daService.update("delete from verification_code_tb where uin =?",new Object[]{uin});
				//���³���״̬ �����ߣ������¼ʱ��
				String updateSql = "update user_info_tb set online_flag=? ,logon_time=? where id=?";
				Object[] values = new Object[]{22,System.currentTimeMillis()/1000,uin};
				daService.update(updateSql, values);
				//�Ƿ��Ǻ������û�
				boolean isBack=doValidateBlackByImei(imei,uin,mobile);
				/*if(!isBack){//���ں������ڣ���������¼������У�д����û���ͣ��ȯ����
					if(!methods.checkBonus(mobile,uin)){//û��ͣ��ȯ��Ĭ����һ��10Ԫ
						Long count = daService.getLong("select count(id) from ticket_tb where uin =? ", new Object[]{uin});
						if(count==0)
							try {
								Long time = TimeTools.getToDayBeginTime();
								Long etime = time + 6*24*60*60-1;
								daService.update("insert into ticket_tb (uin,create_time,limit_day,money,state) values(?,?,?,?,?)",
										new Object[]{uin,time,etime,1,0});
								daService.update("insert into ticket_tb (uin,create_time,limit_day,money,state) values(?,?,?,?,?)",
										new Object[]{uin,time,etime,2,0});
								daService.update("insert into ticket_tb (uin,create_time,limit_day,money,state) values(?,?,?,?,?)",
										new Object[]{uin,time,etime,3,0});
								daService.update("insert into ticket_tb (uin,create_time,limit_day,money,state) values(?,?,?,?,?)",
										new Object[]{uin,time,etime,4,0});
							} catch (Exception e) {
								e.printStackTrace();
							}
					}
				}*/
				
				
				//��ѯ�Ƿ�����д������
				String  carNumber =null;
				Map carInfo = daService.getPojo("select car_number from car_info_Tb where uin=? and state=?", new Object[]{uin,1});
				if(carInfo!=null&&carInfo.get("car_number")!=null)
					carNumber = (String)carInfo.get("car_number");
				logger.error(carNumber);
				if(carNumber!=null){
					//�����Ƽ�����
					/*if(!isBack){//���Ǻ�����ʱ�������Ƽ�����
						Long recomCode = (Long)user.get("recom_code");
						//handleRecommendCode(uin, recomCode,mobile);
						//�û�ͨ��ע���¿���Աע�ᳵ��������������
						//handleVipRegister(uin);//2016-09-07
					}*/
					result="1";
				}else{
					//result="2";
					result="1";//��ʱ��ǿ�����복��
				}
			}
			logger.error(mobile+",login,result:"+result);
			AjaxUtil.ajaxOutput(response, result.replace("null", ""));
			return null;
			
		}else if(action.equals("addcar")){//��ӳ��ƺ�
			 //http://192.168.0.188/zld//carlogin.do?action=addcar&carnumber=��GHR009&mobile=18101333937
			//publicMethods.sendMessageToThird(21816L, 2000, null, null, null, 1);
			String cn = RequestUtil.processParams(request, "carnumber");//
			String carNumber  = AjaxUtil.decodeUTF8(cn).toUpperCase().trim();
			carNumber = carNumber.replace("I", "1").replace("O", "0");
			Long curTime = System.currentTimeMillis()/1000;
			//Long recomCode = RequestUtil.getLong(request, "recom_code", 0L);
			Map userMap = daService.getMap("select id,strid,recom_code,logon_time,cityid from user_info_Tb where wxp_openid=? and auth_flag=?", new Object[]{openid,4});
			//Object oid = daService.getObject("select id from user_info_Tb where mobile=? and auth_flag=? ", new Object[]{mobile,4}, Long.class);
			Long uin= null;
			Long cityId = null;
			if(userMap!=null&&userMap.get("id")!=null){
				uin = (Long)userMap.get("id");
				cityId=(Long)userMap.get("cityid");
			}
			if(uin==null){
				AjaxUtil.ajaxOutput(response, "-5");
			}else {
				int result = 0;
				
				result = methods.addCarnum(uin, carNumber);
				/*//���ݳ��ƺŲ��û�
				Map carmap = daService.getMap("select uin from car_info_tb where car_number = ?", new Object[]{carNumber});
				if(carmap!=null){
					Long cuin = (Long) carmap.get("uin");
					Map cusermap = daService.getMap("select wxp_openid from user_info_tb where id = ?", new Object[]{cuin});
					if(cusermap!=null){
						String wxpopenid = (String) cusermap.get("wxp_openid");
						if(wxpopenid==null){
							//ɾ��֮ǰ�û�
							result = daService.update("delete from user_info_tb where id = ?", new Object[]{cuin});
							logger.error("ɾ�����û�:"+result);
							//���³��ƺ��¿����û�
							result = daService.update("update car_info_tb set uin = ? where car_number = ?", new Object[]{uin,carNumber});
							logger.error("���¸ó����û�:"+result);
							Map map = daService.getMap("select id from carower_product where uin = ?", new Object[]{cuin});
							result = daService.update("update carower_product set uin = ? where id = ?", new Object[]{uin,map.get("id")});
							logger.error("���³����û��¿�:"+result);
						}else{
							//��ʵ�û�,�鿴�¿�
							Map prod = daService.getMap("select e_time from carower_product where uin = ?", new Object[]{cuin});
							if(prod!=null){
								Long endTime = (Long) prod.get("e_time");
								Long cTime = TimeTools.getToDayBeginTime();
								logger.error(cTime);
								logger.error("�¿���������:"+TimeTools.getTimeStr_yyyy_MM_dd(endTime*1000)+" ��ǰ����:"+TimeTools.getTimeStr_yyyy_MM_dd(cTime*1000));
								if(cTime<endTime){
									//�¿�δ����,���ܰ�
									//���ܰ󶨸ó���
									AjaxUtil.ajaxOutput(response, "-9");
									return null;
								}
							}
							//���԰�
							result = daService.update("update car_info_tb set uin = ? where car_number = ?", new Object[]{uin,carNumber});
							logger.error("�����¿�����:"+result);
							if(result>0){
								result=1;
							}
						}
					}
				}else{
					//�½�����
					try {
						result=daService.update("insert into car_info_Tb (uin,car_number,create_time)" +
							"values(?,?,?)", new Object[]{uin,carNumber,curTime});
					} catch (Exception e) {
						e.printStackTrace();
						if(e.getMessage().indexOf("car_info_tb_car_number_key")!=-1){
							AjaxUtil.ajaxOutput(response, "-9");
							return null;
						}
					}
				}*/
				
				if(result==1){//����д��ɹ��������Ƽ�
					//methods.checkBonus(mobile,uin);
					//Long recomCode = (Long)userMap.get("recom_code");
					//�����¼ʱ�䣬���Ϊ�գ�Ϊ�µ�¼�û�����һ��ͣ��ȯ������������ȫ�����ڣ���һ����Ԫͣ��ȯ
					/*Long logon_time = (Long) userMap.get("logon_time");
					if(logon_time==null){//δ��¼��
						Long ntime = System.currentTimeMillis()/1000;
						Long countLong = daService.getLong("select count(id) from ticket_tb where uin=? and limit_day>? ", 
								new Object[]{uin,ntime+24*60*60});
						if(countLong==0){//��һ��ͣ��ȯ
							int ret = daService.update("insert into ticket_tb (uin,create_time,limit_day,money,state) values(?,?,?,?,?)",
									new Object[]{uin,ntime,ntime+15*24*60*60,3,0});
							logger.error(">>>>>>first login,ticket is invalid,add a 10 yuan ticket :"+ret);
						}
					}*/
					//logger.error(recomCode);
					//if(recomCode!=null&&recomCode>0){
						//�Ƽ��� 
						//logger.error(recomCode);
						//List<Long> blackUserList = memcacheUtils.doListLongCache("zld_black_users", null, null);
						//�����Ƽ�
//						if(blackUserList==null||!blackUserList.contains(uin))//���ں������п��Դ����Ƽ�����
//							handleRecommendCode(uin, recomCode,mobile);
					//}
					AjaxUtil.ajaxOutput(response, result+"");
				}else 
					AjaxUtil.ajaxOutput(response, "-8");
			}
		}else if(action.equals("addcid")){//�ͻ��˵�¼���ϴ�CID��������Ϣ���͸���"����"
			//http://192.168.199.240/zld/carlogin.do?action=addcid&cid=123456789&mobile=15801482643&hx=1
			String cid = RequestUtil.getString(request, "cid");
			Integer hx = RequestUtil.getInteger(request, "hx",0);
			System.out.println(">>>>>>>>>>>>cid:"+cid+",mobile:"+mobile);
			//�����˻�������
			String sql = "select * from user_info_tb where mobile=? and auth_flag=?";
			Map user = daService.getPojo(sql, new Object[]{mobile,4});
			String hxName  = (String)user.get("hx_name");
			String hxPass  = (String)user.get("hx_pass");
			boolean ishas = false;//getHxNamePass(user,hxPass);
			if(ishas){
				hxName  = (String)user.get("hx_name");
				hxPass  = (String)user.get("hx_pass");
			}
			Integer ctype = 0;//�ͻ�������  0:android,1:ios
			if(cid.length()>32)
				ctype=1;
			int result = 0;
			if(!cid.equals(""))
				result =daService.update("update user_info_Tb set cid = ?,client_type=?  where mobile=? and auth_flag=?",
						new Object[]{cid,ctype,mobile,4});
			else {
				result =daService.update("update user_info_Tb set cid = ?  where mobile=? and auth_flag=?",
						new Object[]{cid,mobile,4});
			}
			String ret=result+"";
			if(hx==1)//
				ret ="{\"result\":\""+result+"\",\"hxname\":\""+hxName+"\",\"hxpass\":\""+hxPass+"\",\"wximgurl\":\""+(user.get("wx_imgurl")==null?"":user.get("wx_imgurl"))+"\"}";
			logger.error(ret);
			AjaxUtil.ajaxOutput(response, ret);
		}
		return null;
	}
	
	/**
	 * ������IM,ע�ᳵ���˻������ºû��ѵ�����ϵͳ������Ϣ���û���
	 * @param user
	 * @return
	 * @throws Exception
	 */
	private boolean getHxNamePass(Map user,String pass) throws Exception {
		Long uin = (Long)user.get("id");
		if(pass==null||"".equals(pass))
			pass = publicMethods.getHXpass(uin);
		if(HXHandle.reg("hx"+uin,pass)){
			user.put("hx_name", "hx"+uin);
			user.put("hx_pass", pass);
			int ret = daService.update("update user_info_tb set hx_name=? ,hx_pass=? where id =? ", new Object[]{"hx"+uin,pass,uin});
			logger.error(user.get("mobile")+">>>>>��¼��ע�ỷ�Ž����"+ret);
			if(ret==1){
				//ע��û��ѵ�����ϵͳ
				List friends = daService.getAll("select euin from user_friend_tb where buin=? and is_add_hx=? ", new Object[]{uin,0});
				List regFriends = daService.getAll("select id from user_info_tb where  id in" +
						"(select euin from user_friend_tb where buin=? and is_add_hx=?) and (hx_name is  null or hx_pass is null)", new Object[]{uin,0});
				logger.error(user.get("mobile")+">>>>>>������Ҫ���µ�����ϵͳ��number:"+(friends==null?"0":friends.size()));
				logger.error(user.get("mobile")+">>>>>>����ע�ᵽ����ϵͳ��number:"+(regFriends==null?"0":regFriends.size()));
				List<Long> regUinList = new ArrayList<Long>();
				if(regFriends!=null&&!regFriends.isEmpty()){
					for(int i=0;i<regFriends.size();i++){
						Map uMap = (Map)regFriends.get(i);
						regUinList.add((Long)uMap.get("id"));
					}
				}
				//http://192.168.199.240/zld//carlogin.do?action=validcode&code=1234&mobile=13641309140&hx=1
				if(friends!=null&&!friends.isEmpty()){
					JsonNodeFactory factory = new JsonNodeFactory(false);
					//����ע��Ļ���
					ArrayNode arrayNode = new ArrayNode(factory);
					//�Ӻ����б�
					List<Long> addFriends = new ArrayList<Long>();
					String sql = "update user_info_tb set hx_name=? ,hx_pass=? where id =? ";
					List<Object[]> params = new ArrayList<Object[]>();
					for(int i=0;i<friends.size();i++){
						Map uMap = (Map)friends.get(i);
						Long _uin = (Long)uMap.get("euin");
						if(regUinList.contains(_uin)){//ȥ���Ѿ�ע��Ļ���
							ObjectNode jsonNode = factory.objectNode();
							jsonNode.put("username", "hx"+_uin);
							jsonNode.put("password",  publicMethods.getHXpass(_uin));
							arrayNode.add(jsonNode);
							params.add(new Object[]{"hx"+_uin, publicMethods.getHXpass(_uin),_uin});
						}
						addFriends.add(_uin);
					}
					//����ע�����
					ObjectNode objectNode = null;
					if(arrayNode.size()>0)
						objectNode=EasemobIMUsers.createNewIMUserBatch(arrayNode);
					if(null!=objectNode){
						String statusCode = JsonUtil.getJsonValue(objectNode.toString(), "statusCode");
						if(statusCode.equals("200")){
							logger.error(user.get("mobile")+"������ע����ѳɹ���"+objectNode.toString());
							if(!params.isEmpty()){
								int r = daService.bathInsert(sql, params, new int[]{12,12,4});
								logger.error(user.get("mobile")+"������ע����ѳɹ�,���µ����ݿ⣺ret:"+r);
							}
						}else {
							logger.error(user.get("mobile")+"������ע�����ʧ�ܣ�"+objectNode.toString());
						}
					}
					if(!addFriends.isEmpty()){
					  	//ѭ���������
						for(Long touin : addFriends){
							objectNode = EasemobIMUsers.addFriendSingle("hx"+uin, "hx"+touin);
							String statusCode = JsonUtil.getJsonValue(objectNode.toString(), "statusCode");
							if(statusCode.equals("200")){
								logger.error(user.get("mobile")+"���Ӻ��ѳɹ������ѣ�"+"hx"+touin+","+objectNode.toString());
								int r = daService.update("update user_friend_tb set is_add_hx=? where  buin=? or euin=?  ", new Object[]{1,uin,uin});
								logger.error(user.get("mobile")+">>>>>>����zld���ѱ�ret:"+r);
							}else {
								logger.error(user.get("mobile")+"���Ӻ���ʧ�ܣ����ѣ�"+"hx"+touin+","+objectNode.toString());
							}
						}
					}
				}
			}
			return true;
		}
		return false;
	}
	/**
	 * ��֤�ֻ����ţ��ж��Ƿ��Ǻ������û�
	 * ��ѯ�ֻ������Ƿ�ע�������������������û�ʹ�ù��ô��ţ����� true;
	 * @param uin
	 * @param imei
	 */
	private boolean doValidateBlackByImei( String imei,Long uin,String mobile) {
		// TODO Auto-generated method stub
		logger.error(">>>>>mobile:"+mobile+",imei:"+imei+",uin:"+uin);
		if("000000000000000".equals(imei))//ˢ������ֻ����ţ������ں�����
			return false;
		boolean isBlack = publicMethods.isBlackUser(uin);
		if(isBlack)
			return isBlack;
		if(imei==null||"".equals(imei)||imei.indexOf("null")!=-1)
			return false;
		String sql = "select mobile from user_info_Tb where imei=? and auth_flag=? and id <>?  ";
		Object [] values = new Object[]{imei,4,uin};
		List uList = daService.getAll(sql, values);
		if(uList!=null&&uList.size()>1){
			if(!publicMethods.isAuthUser(uin)){
				//д���������
				Long ntime = System.currentTimeMillis()/1000;
				try {
					List<Long> whiteUsers = memcacheUtils.doListLongCache("zld_white_users", null, null);
					if(whiteUsers==null||!whiteUsers.contains(uin)){
						int ret = daService.update("insert into zld_black_tb(ctime,utime,uin,state,remark) values(?,?,?,?,?)",
								new Object[]{ntime,ntime,uin,0,"��ǰ�ֻ���"+mobile+",�ֻ������ظ���"+imei+",�Ѵ����ֻ���"+uList});
						logger.error(">>>���������,uin:"+uin+",imei:"+imei+"����� ��"+ret);
						if(ret==1){
							//�������������
							List<Long> blackUsers = memcacheUtils.doListLongCache("zld_black_users", null, null);
							if(blackUsers==null){
								blackUsers = new ArrayList<Long>();
								//blackUsers.clear();
								blackUsers.add(uin);
								memcacheUtils.doListLongCache("zld_black_users", blackUsers, "update");
							}else {
								if(!blackUsers.contains(uin)){
									//blackUsers.clear();
									blackUsers.add(uin);
									System.err.println(blackUsers);
									memcacheUtils.doListLongCache("zld_black_users", blackUsers, "update");
								}
							}
							return true;
						}
					}else {
						logger.error(">>>>>>>zld_white_tb>,uin:"+uin+",imei:"+imei+"���ڰ������� ��"+whiteUsers);
					}
				} catch (Exception e) {
					logger.error(">>>�������������,uin:"+uin+",imei:"+imei+"���Ѿ����ڣ�");
					e.printStackTrace();
				}
			}else{
				logger.error(">>>���������û�ʹ�ù��ô���,����������֤�����������,uin:"+uin+",imei:"+imei);
			}
		}else {//���³����˻��ֻ�����
			int ret = daService.update("update user_info_Tb set imei=? where id =? ", new Object[]{imei,uin});
			logger.error(">>>���ں�������uin:"+uin+",imei:"+imei+"�����³����ֻ����Ž����"+ret);
		}
		return false;
	}
	//ע�ᳵ����Ϣ
	@SuppressWarnings({ "rawtypes" })
	private int createCarOwerInfo(HttpServletRequest request,Long uin){
		Integer media = RequestUtil.getInteger(request, "media", 0);
		String mobile =RequestUtil.processParams(request, "mobile");
		if(mobile.equals("")) mobile=null;
		Map adminMap = daService.getPojo("select * from user_info_tb where comid = ?", new Object[]{0});
		Long time = System.currentTimeMillis()/1000;
		
		String strid = uin+"";
		//�û���
		String sql= "insert into user_info_tb (id,nickname,password,strid," +
				"address,reg_time,mobile,auth_flag,comid,media,cityid) " +
				"values (?,?,?,?,?,?,?,?,?,?,?)";
		Object[] values= new Object[]{uin,"����",strid,strid,
				adminMap.get("address"),time,mobile,4,0,media,-1L};
		
		int r = daService.update(sql,values);
		if(r==1){
			int	eb = daService.update("insert into user_profile_tb (uin,low_recharge,limit_money,auto_cash," +
					"create_time,update_time) values(?,?,?,?,?,?)", 
					new Object[]{uin,10,25,1,time,time});
			logger.error(">>>>���û�ע�ᣬĬ��д��֧������...+"+eb);
			
		}
		return r;
	}	
	private String  getToMobile(String mobile){
		//�ƶ� ��ͨ  106901336275
		//������1069004270441
		//�й����ţ�133,153,177,180,181,189
		if(mobile.startsWith("133")||mobile.startsWith("153")
				||mobile.startsWith("177")||mobile.startsWith("180")
				||mobile.startsWith("181")||mobile.startsWith("189")
				||mobile.startsWith("170"))
			return "1069004270441";
		return "106901336275";
	}
	
	
	//�û�ͨ��ע���¿���Աע��ĳ������ڳ�����һ�ε�¼��ʱ�����Ӧ�ĳ�����5Ԫ
	private void handleVipRegister(Long uin){
		Map<String, Object> map = daService.getMap("select * from recommend_tb where nid=? and type=? and state=? ", new Object[]{uin,0,0});
		List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
		if(map != null){
			Long comid = (Long)map.get("comid");
			Long count = daService.getLong("select count(*) from com_info_tb where id=? and state !=?", new Object[]{comid,1});
			if(count > 0){
				Map<String, Object> parkaccountMap = new HashMap<String, Object>();
				parkaccountMap.put("sql", "insert into park_account_tb(comid,amount,type,create_time,remark,source) values(?,?,?,?,?,?) ");
				parkaccountMap.put("values", new Object[]{comid,5,0,System.currentTimeMillis()/1000,"ͨ��ע���¿���Աע�ᳵ��_"+uin,3});
				bathSql.add(parkaccountMap);
				
				Map<String, Object> cominfoMap = new HashMap<String, Object>();
				cominfoMap.put("sql", "update com_info_tb set money=money+?,total_money=total_money+? where id=?");
				cominfoMap.put("values", new Object[]{5,5,comid});
				bathSql.add(cominfoMap);
				
				Map<String, Object> recomSqlMap = new HashMap<String, Object>();
				recomSqlMap.put("sql", "update recommend_tb set state=? where nid=? and type=? and state=?");
				recomSqlMap.put("values", new Object[]{1,uin,0,0});
				bathSql.add(recomSqlMap);
				
				daService.bathUpdate(bathSql);
			}
		}
	}
	
/*	private void handleRecommendCode(Long uin,Long recomCode,String mobile){
		//�Ƽ���
		//logger.error(recomCode);
		Map usrMap = daService.getMap("select auth_flag from user_info_tb where id=?", new Object[]{recomCode});
		//�Ƽ��˽�ɫ
		Long auth_flag = null;
		if(usrMap!=null)
			auth_flag = (Long) usrMap.get("auth_flag");
		if(auth_flag!=null&&(auth_flag==1||auth_flag==2)){//���շ�Ա�Ƽ��ĳ�����Ŀǰû�г����Ƽ������ļ�¼
			Long count  = daService.getLong("select count(ID) from recommend_tb where nid=? and pid=? and state=? and type=?", new Object[]{uin,recomCode,0,0});
			//�Ƽ�����.0��������1:����
			logger.error("is recom:"+count);
			if(count!=null&&count>0){//���Ƽ������Ƽ��˵Ľ���û��֧��//���������շ�Ա�˺�5Ԫ
				
				List<Map<String , Object>> sqlMaps = new ArrayList<Map<String,Object>>();
				Map<String, Object> usersqlMap = new HashMap<String, Object>();
				//�շ�Ա�˻���5Ԫ
				usersqlMap.put("sql", "update user_info_tb set balance=balance+? where id=? ");
				usersqlMap.put("values", new Object[]{5,recomCode});
				sqlMaps.add(usersqlMap);
			
				//д���շ�Ա�˻���ϸ
				Map<String, Object> parkuserAccountMap = new HashMap<String, Object>();
				parkuserAccountMap.put("sql", "insert into parkuser_account_tb(uin,amount,type,create_time,remark,target) " +
						"values(?,?,?,?,?,?)");
				parkuserAccountMap.put("values", new Object[]{recomCode,5,0,System.currentTimeMillis()/1000,"�Ƽ�����",3});
				sqlMaps.add(parkuserAccountMap);
				
				//�����Ƽ���¼
				Map<String, Object> recomsqlMap = new HashMap<String, Object>();
				recomsqlMap.put("sql", "update recommend_tb set state=? where nid=? and pid=?");
				recomsqlMap.put("values", new Object[]{1,uin,recomCode});
				sqlMaps.add(recomsqlMap);
				
				
				
				logger.error(count);
				boolean ret = daService.bathUpdate(sqlMaps);
				if(ret){//д���շ�Ա��Ϣ��
					String mobile_end = mobile.substring(7);
					int result =daService.update("insert into parkuser_message_tb(type,ctime,uin,title,content) values(?,?,?,?,?)",
							new Object[]{0,System.currentTimeMillis()/1000, recomCode, "�Ƽ�����", "���Ƽ��ĳ������ֻ�β��"+mobile_end+"��ע��ɹ������ã�Ԫ������"} );
					logger.error(">>>>>>>>>���������շ�Ա�Ƽ�����5Ԫ��Ϣ:"+result);
				}
				logger.error(">>>>>>>>>���������շ�Ա�Ƽ�����5Ԫ��"+ret);
			}
		}else {
			logger.error(recomCode);
		}
	}*/
/*	public static void main(String[] args) {
		Integer code = new Random(System.currentTimeMillis()).nextInt(1000000);
		if(code<99)
			code=code*10000;
		if(code<999)
			code =code*1000;
		if(code<9999)
			code = code*100;
		if(code<99999)
			code = code*10;
		System.out.println(code);
	}*/
}