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

import com.mongodb.BasicDBObject;
import com.zld.AjaxUtil;
import com.zld.impl.MongoDbUtils;
import com.zld.impl.PublicMethods;
import com.zld.service.DataBaseService;
import com.zld.service.LogService;
import com.zld.utils.RequestUtil;
import com.zld.utils.SendMessage;
import com.zld.utils.StringUtils;


/**
 * ע��ͣ�������շ�Ա
 * @author Administrator
 * 2014-12-23 
 */
public class RegisterParkAction extends Action {

	
	@Autowired
	private DataBaseService daService;
	@Autowired
	private MongoDbUtils mongoDbUtils;
	@Autowired
	private LogService logService;
	@Autowired
	private PublicMethods publicMethods;
	
	private Logger logger = Logger.getLogger(ParkCollectorLoginAction.class);

	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String action = RequestUtil.getString(request, "action");
		String mobile = RequestUtil.getString(request, "mobile");
		if(action.equals("getcode")){//��ȡ��֤��
			//��ѯ���ֻ����Ƿ�ע�������Ա
			Map parkadminMap = daService.getMap("select id,state,recom_code from user_info_tb where mobile=? and auth_flag= ? ",
					new Object[]{mobile,1});
			if(parkadminMap != null){
				Integer pstate = (Integer)parkadminMap.get("state");
				Long puin = (Long)parkadminMap.get("id");
				if(pstate==0){//״̬:�����û�
					//ɾ���Ѿ����浫û����֤������֤�루����Ч����֤�룩
					daService.update("delete from verification_code_tb where uin =?",new Object[]{puin});
					SendMessage.sendMultiMessage(mobile, "�����ֻ�����ע����շ�Ա���˺ţ�"+puin+",��ֱ���ô��˺ŵ�¼ ��ͣ������ ");
					AjaxUtil.ajaxOutput(response, "-4");//��ע��,�����û�
					return null;
				}else if(pstate==1){//״̬:����״̬
					//ɾ���Ѿ����浫û����֤������֤�루����Ч����֤�룩
					daService.update("delete from verification_code_tb where uin =?",new Object[]{puin});
					SendMessage.sendMultiMessage(mobile, "�����ֻ�����ע����շ�Ա���˺ţ�"+puin+",Ŀǰ�ѽ��ã�����ϵͣ�����ͷ�010-53618108 ��ͣ������");
					AjaxUtil.ajaxOutput(response, "-5");//��ע�ᣬ���ѽ���
					return null;
				}
			}
			//��ѯ�ֻ��Ŵ��ڵ��շ�Ա
			Long uin= null;
			Map userMap = daService.getMap("select id,state,recom_code from user_info_tb where mobile=? and auth_flag= ? ",
					new Object[]{mobile,2});
			Integer state = 0;
			if(userMap!=null&&userMap.get("id")!=null){
				state=(Integer)userMap.get("state");
				uin = (Long)userMap.get("id");
			}
			int r = 0;
			if(uin==null||uin==-1){//�������շ�Ա��ע���շ�Ա
				uin= daService.getkey("seq_user_info_tb");
				r = createCollectorInfo(request, uin);
				if(r!=1){
					AjaxUtil.ajaxOutput(response, "-1");//ע��ʧ��
					return null;
				}
			}else {
				if(userMap.get("recom_code") ==null){
					if(state==0){//״̬:�����û�
						//ɾ���Ѿ����浫û����֤������֤�루����Ч����֤�룩
						daService.update("delete from verification_code_tb where uin =?",new Object[]{uin});
						SendMessage.sendMultiMessage(mobile, "�����ֻ�����ע����շ�Ա���˺ţ�"+uin+",��ֱ���ô��˺ŵ�¼ ��ͣ������ ");
						AjaxUtil.ajaxOutput(response, "-4");//��ע��,�����û�
						return null;
					}else if(state==1){//״̬:����״̬
						//ɾ���Ѿ����浫û����֤������֤�루����Ч����֤�룩
						daService.update("delete from verification_code_tb where uin =?",new Object[]{uin});
						SendMessage.sendMultiMessage(mobile, "�����ֻ�����ע����շ�Ա���˺ţ�"+uin+",Ŀǰ�ѽ��ã�����ϵͣ�����ͷ�010-53618108 ��ͣ������");
						AjaxUtil.ajaxOutput(response, "-5");//��ע�ᣬ���ѽ���
						return null;
					}else if(state != 0 && state != 1){
						Long count = daService.getLong("select count(*) from parkuser_account_tb where uin=? and type=? and target=? ", new Object[]{uin,0,3});
						if(count > 0){//�ͻ���ע�ᣬ���Ƽ���
							AjaxUtil.ajaxOutput(response, "-6");
							return null;
						}
					}
				}else{
					daService.update("delete from verification_code_tb where uin =?",new Object[]{uin});
					SendMessage.sendMultiMessage(mobile, "�����ֻ�����ע����շ�Ա���˺ţ�"+uin+",��ֱ���ô��˺ŵ�¼  ��ͣ������");
					AjaxUtil.ajaxOutput(response, "-6");//��ע��,�����û�
					return null;
				}
				//�ȴ����״̬�����Լ���������֤��
			}
			//���Ͳ�������֤��
			if(!publicMethods.isCanSendShortMesg(mobile)){
				AjaxUtil.ajaxOutput(response, "-2");
				return null;
			}
			Integer code = new SendMessage().sendMessageToCarOwer(mobile);
			if(code!=null){
				logger.error("code:"+code+",mobile:"+mobile);
				//������֤��
				r =daService.update("insert into verification_code_tb (verification_code,uin,create_time,state)" +
						" values (?,?,?,?)", new Object[]{code,uin,System.currentTimeMillis()/1000,0});
				if(r==1){
					AjaxUtil.ajaxOutput(response, "1");//�ɹ����Ͳ�������֤��
				}else{
					AjaxUtil.ajaxOutput(response, "-2");//��֤�뱣��ʧ��
				}
			}else {
				AjaxUtil.ajaxOutput(response, "-3");//������֤��ʧ��
			}
			//http://192.168.199.240/zld/regparker.do?action=getcode&mobile=15801482643
		}else if(action.equals("validcode")){//��֤���ص���֤��
			String vcode =RequestUtil.processParams(request, "code");
			String sql = "select * from user_info_tb where mobile=? and auth_flag=?";
			Map user = daService.getPojo(sql, new Object[]{mobile,2});
			if(user==null){
				AjaxUtil.ajaxOutput(response, "-2");//�û�������
				return null;
			}
			Long uin = Long.valueOf(user.get("id")+"");
			Map verificationMap = daService.getPojo("select verification_code from verification_code_tb" +
					" where uin=? and state=? ", new Object[]{uin,0});
			if(verificationMap==null){
				AjaxUtil.ajaxOutput(response, "-3");//δ������֤��
				return null;
			}
			String code = verificationMap.get("verification_code").toString();
			if(code.equals(vcode)){//��֤��ƥ��ɹ�
				//ɾ����֤���
				daService.update("delete from verification_code_tb where uin =?",new Object[]{uin});
				//���³���״̬ �����ߣ������¼ʱ��
				daService.update("update user_info_tb set online_flag=? ,logon_time=? where id=?", new Object[]{22,System.currentTimeMillis()/1000,uin});
				AjaxUtil.ajaxOutput(response, "1");//��֤��ƥ��ɹ�
			}else{
				AjaxUtil.ajaxOutput(response, "-1");//��֤��ƥ��ʧ��
			}
			//http://192.168.199.240/zld/regparker.do?action=validcode&code=1580&mobile=15801482643
		}
		else if(action.equals("getmesgcode")){//��ȡ��������֤��ͷ��͵�ַ��
			Map<String,Object> infoMap = new HashMap();//����ֵ
			//��ѯ�ֻ��Ŵ��ڵ��շ�Ա
			Long uin= null;
			if(!mobile.equals("")){
				Map userMap  = daService.getMap("select id  from user_info_tb where mobile=? and auth_flag= ?",
						new Object[]{mobile,2});
				if(userMap!=null&&userMap.get("id")!=null)
					uin = (Long)userMap.get("id");
			}else {
				infoMap.put("mesg", "-1");
				AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));//ע��ʧ��
				return null;
			}
					
			int r = 0;
			if(uin==null||uin==-1){//�������շ�Ա��ע���շ�Ա
				uin= daService.getkey("seq_user_info_tb");
				r = createCollectorInfo(request, uin);
				if(r!=1){
					infoMap.put("mesg", "-1");
					AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));//ע��ʧ��
					return null;
				}
			}else {
				//ɾ���Ѿ����浫û����֤������֤�루����Ч����֤�룩
				daService.update("delete from verification_code_tb where uin =?",new Object[]{uin});
			}
			//������λ��֤��
			Integer code = new Random(System.currentTimeMillis()).nextInt(1000000);
			if(code<99)
				code=code*10000;
			if(code<999)
				code =code*1000;
			if(code<9999)
				code = code*100;
			if(code<99999)
				code = code*10;
			logger.error("code:"+code+",mobile:"+mobile);
			
			//������֤��
			r =daService.update("insert into verification_code_tb (verification_code,uin,create_time,state)" +
					" values (?,?,?,?)", new Object[]{code,uin,System.currentTimeMillis()/1000,0});
			if(r==1){//�ɹ�������֤��
				infoMap.put("mesg", "1");
				infoMap.put("code", code+"00");
				infoMap.put("tomobile", getToMobile(mobile));
			}else{//������֤��ʧ��
				infoMap.put("mesg", "-2");
			}
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
			//http://192.168.199.240/zld/regparker.do?action=getmesgcode&mobile=15801482643
		}else if(action.equals("uploadpic")){
			Long uin= null;
			Map userMap = daService.getMap("select id,state,collector_pics from user_info_tb where mobile=? and auth_flag= ? ",
					new Object[]{mobile,2});
			Integer state = -1;
			if(userMap!=null&&userMap.get("id")!=null){
				state=(Integer)userMap.get("state");
				uin = (Long)userMap.get("id");
			}
			String ret = "0";
			if(uin !=null&&state==2){
				ret=publicMethods.uploadPicToMongodb(request, uin, "parkuser_pics");//uploadParkPics2Mongodb(request, uin);
			}
			if(!ret.equals("-1")){
				Integer collector_pics = (Integer)userMap.get("collector_pics");
				collector_pics += 1;
				ret = daService.update("update user_info_tb set collector_pics=? where id=? ", new Object[]{collector_pics,uin})+"";
			}
			AjaxUtil.ajaxOutput(response, ret + "");
		}else if(action.equals("find")){
			BasicDBObject conditions = new BasicDBObject();
			conditions.put("orderid", 786590L);
			List<String> urls = mongoDbUtils.getPicUrls("car_inout_pics", conditions);
			logger.error(urls);
			//http://127.0.0.1/zld/regparker.do?action=find&mobile=15801482643
		}else if(action.equals("toregpage")){
			String recomcode = RequestUtil.processParams(request, "recomcode");
			request.setAttribute("recomcode", recomcode);
			return mapping.findForward("collectorreg");
		}else if(action.equals("collectorreg")){//���������շ�Ա�Ƽ��շ�Ա
			//���������շ�Ա�˺ţ�����auth_flag����ǳ������շ�Ա��4������2��1�����շ�Ա��
			Long pid = RequestUtil.getLong(request, "recomcode", -1L);//�Ƽ��˵��Ƽ���
			String nickname = AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "nickname"));//�շ�Ա����
			if(pid == -1 || nickname.equals("") || mobile.equals("")){//�����ӳ�,����û���ύ����
				return mapping.findForward("error");
			}
			String sql = "select * from user_info_tb where mobile=? and auth_flag=?";
			Map user = daService.getPojo(sql, new Object[]{mobile,2});
			Long nid = null;
			String oldpass = "";
			if(user != null){
				nid = (Long)user.get("id");
				oldpass = (String)user.get("password");
			}else {
				return mapping.findForward("error");
			}
			//ˢ��ҳ���ֹ�ظ�����
			if(user.get("recom_code") != null){
				//ͣ���ѽ��
				Double nowbalance = Double.valueOf(user.get("balance")+"");
				request.setAttribute("balance", nowbalance);
				String epaysql = "select sum(order_total) epaymoney from order_message_tb where uin=? and state=?";
				Map<String, Object> map = daService.getMap(epaysql, new Object[]{nid,2});
				Double epay = 0.00d;
				if(map.get("epaymoney") != null){
					epay = Double.valueOf(map.get("epaymoney") + "");
				}
				request.setAttribute("epaymoney", epay);
				//���ֽ��
				String backsql = "select sum(amount) backmoney from parkuser_account_tb where uin=? and type=? and target=? ";
				Map<String, Object> backmap = daService.getMap(backsql, new Object[]{nid,0,3});
				Double backmoney = 0.00d;
				if(backmap.get("backmoney") != null){
					backmoney = Double.valueOf(backmap.get("backmoney") + "");
				}
				request.setAttribute("backmoney", backmoney);
				request.setAttribute("nickname", nickname);
				request.setAttribute("uin", nid);
				return mapping.findForward("collect");
			}
			Double nbalance = Double.valueOf(user.get("balance")+"") + 10.00d;
			//4������2��1�����շ�Ա
			Long auth_flag = null;
			String recmobile = null;
			Map recomuser = daService.getPojo("select * from user_info_tb where id=?", new Object[]{pid});
			if(recomuser == null){
				return mapping.findForward("error");
			}else{
				auth_flag = (Long)recomuser.get("auth_flag");
				if(recomuser.get("mobile") != null){
					recmobile = (String)recomuser.get("mobile");
				}
			}
			//2016-09-07
			/*boolean b = false;
			List<Map<String , Object>> sqlMaps = new ArrayList<Map<String,Object>>();
			
			logger.error ("�Ƽ��շ�Աע�ᣬ����...");
			//д�Ƽ���¼
			Map<String, Object> recomsqlMap = new HashMap<String, Object>();
			recomsqlMap.put("sql", "insert into recommend_tb(pid,nid,type,state,create_time) values(?,?,?,?,?)");
			recomsqlMap.put("values", new Object[]{pid,nid,1,0,System.currentTimeMillis()/1000});
			sqlMaps.add(recomsqlMap);
			
			//�շ�Աע��ɹ����ֵ10Ԫ
			Map<String, Object> colAccountsqlMap = new HashMap<String, Object>();
			colAccountsqlMap.put("sql", "insert into parkuser_account_tb(uin,amount,type,create_time,remark,target) values(?,?,?,?,?,?)");
			colAccountsqlMap.put("values", new Object[]{nid,10.00,0,System.currentTimeMillis()/1000,"�շ�Աע��ɹ�,����10Ԫ",3});
			sqlMaps.add(colAccountsqlMap);
			
			//���±��Ƽ����շ�Ա���ֺ��Ƽ���,��ֵ10Ԫ
			Map<String, Object> usersqlMap = new HashMap<String, Object>();
			usersqlMap.put("sql", "update user_info_tb set recom_code=?,nickname=?,balance=? where id=? ");
			usersqlMap.put("values", new Object[]{pid,nickname,nbalance,nid});
			sqlMaps.add(usersqlMap);
			
			//�����Ƽ�����ʱ
			if(auth_flag==4){
				//������5Ԫ
				Double pbalance = Double.valueOf(recomuser.get("balance")+"") + 5.00d;
				
				Map<String, Object> userAccountsqlMap = new HashMap<String, Object>();
				userAccountsqlMap.put("sql", "insert into user_account_tb(uin,amount,type,create_time,remark,pay_type) values(?,?,?,?,?,?)");
				userAccountsqlMap.put("values", new Object[]{pid,5.00,0,System.currentTimeMillis()/1000,"�Ƽ��շ�Ա�ɹ�,����5Ԫ",8});
				sqlMaps.add(userAccountsqlMap);
				
				Map<String, Object> recomusersqlMap = new HashMap<String, Object>();
				recomusersqlMap.put("sql", "update user_info_tb set balance=? where id=? ");
				recomusersqlMap.put("values", new Object[]{pbalance,pid});
				sqlMaps.add(recomusersqlMap);
			}
			try {
				b= daService.bathUpdate(sqlMaps);
				if(b){//��Ϣ����
					if(auth_flag==4)
						logService.insertUserMesg(6, pid, "���Ƽ����շ�Ա"+nickname+"�����5Ԫ��������˳ɹ����ٷ�25Ԫ������", "�Ƽ�����");
					else {
						//�Ƽ��������շ�Ա��1��
						Map userMap = daService.getMap("select comid from user_info_Tb where id = ? ", new Object[]{pid});
						if(userMap!=null){
							logService.updateScroe(5, pid, (Long)userMap.get("comid"));
						}
					}
				}
			} catch (Exception e) {
				// TODO: handle exception
				logger.error("�Ƽ��շ�Աע��,����...", e);
				return mapping.findForward("error");
			}
			if(!b){
				return mapping.findForward("error");
			}*/
			request.setAttribute("uin", nid);
			request.setAttribute("nickname", nickname);
			request.setAttribute("balance", nbalance);
			request.setAttribute("epaymoney", 0.00d);
			request.setAttribute("backmoney", 0.00d);
			String parkappUrl = "http://t.cn/RZ2wZd1 ";
			String message = "����ͣ�����˻��Ѿ�ע��ɹ����˺ţ�"+nid+"����ʼ����Ϊ"+oldpass+"������ͣ����App�и��ྪϲ"+parkappUrl+" ��ͣ������";
			SendMessage.sendMultiMessage(mobile, message);
			/*String msg ="";
			if(auth_flag==4){
				msg="ͣ����С������ã����Ƽ����շ�Ա"+nickname+"�����ע�ᣬ5Ԫ�����ѵ��ˣ���25Ԫ�����շ�Ա�����ɺ��� ��ͣ������";
			}else {
				msg="ͣ����С������ã����Ƽ����շ�Ա"+nickname+"�����ע�ᣬͣ���������ɺ�����30Ԫ�������� ��ͣ������";
			}
			if(recmobile != null){
				SendMessage.sendMultiMessage(recmobile, msg);
			}*/
			return mapping.findForward("collect");
		}else if(action.equals("download")){
			return mapping.findForward("download");
		}else if(action.equals("getbalance")){
			Map<String, Object> infoMap = new HashMap<String, Object>();
			Long uin = RequestUtil.getLong(request, "uin", -1L);
			if(uin == -1){
				AjaxUtil.ajaxOutput(response, "-1");
				return null;
			}
			//��ѯ���
			String sql = "select balance from user_info_tb where id=?";
			Map<String, Object> map = new HashMap<String, Object>();
			map = daService.getMap(sql, new Object[]{uin});
			Double balance = 0.00d;
			if(map != null){
				balance = Double.valueOf(map.get("balance") + "");
			}
			infoMap.put("balance", String.format("%.2f",balance));
			//��ѯ֧���Ľ��
			sql = "select sum(order_total) epaymoney from order_message_tb where uin=? and state=?";
			Map<String, Object> epaymap = new HashMap<String, Object>();
			epaymap = daService.getMap(sql, new Object[]{uin,2});
			Double epay = 0.00d;
			if(epaymap.get("epaymoney") != null){
				epay = Double.valueOf(epaymap.get("epaymoney") + "");
			}
			infoMap.put("epaymoney", String.format("%.2f",epay));
			//��ѯͣ��������
			sql = "select sum(amount) backmoney from parkuser_account_tb where uin=? and type=? and target=? ";
			Map<String, Object> backmap = new HashMap<String, Object>();
			backmap = daService.getMap(sql, new Object[]{uin,0,3});
			Double backmoney = 0.00d;
			if(backmap.get("backmoney") != null){
				backmoney = Double.valueOf(backmap.get("backmoney") + "");
			}
			infoMap.put("backmoney", String.format("%.2f",backmoney));
			AjaxUtil.ajaxOutput(response, StringUtils.createJson(infoMap));
		}
		return null;
	}
	
	private int createCollectorInfo(HttpServletRequest request,Long uin){
		String mobile =RequestUtil.processParams(request, "mobile");
		if(mobile.equals("")) mobile=null;
		Long time = System.currentTimeMillis()/1000;
		
		String strid ="zld"+uin;
		String md5pass = StringUtils.getParkUserPass();//mobile.substring(5);
		String password  = md5pass;
		try {
			md5pass = StringUtils.MD5(md5pass);
			md5pass = StringUtils.MD5(md5pass+"zldtingchebao201410092009");
		} catch (Exception e) {
			e.printStackTrace();
		}
		//�½��շ�Ա��δ���״̬
		String sql= "insert into user_info_tb (id,nickname,password,strid," +
				"reg_time,mobile,auth_flag,comid,md5pass,state) " +
				"values (?,?,?,?,?,?,?,?,?,?)";
		Object[] values= new Object[]{uin,"�շ�Ա",password,strid,
				time,mobile,2,1,md5pass,2};
		
		int r = daService.update(sql,values);
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
	
/*
	private Integer uploadParkPics2Mongodb (HttpServletRequest request,Long uin) throws Exception{
		logger.error("begin upload regist picture....");
		Map<String, String> extMap = new HashMap<String, String>();
	    extMap.put(".jpg", "image/jpeg");
	    extMap.put(".jpeg", "image/jpeg");
	    extMap.put(".png", "image/png");
	    extMap.put(".gif", "image/gif");
		request.setCharacterEncoding("UTF-8"); // ���ô�����������ı����ʽ
		DiskFileItemFactory  factory = new DiskFileItemFactory(); // ����FileItemFactory����
		factory.setSizeThreshold(16*4096*1024);
		ServletFileUpload upload = new ServletFileUpload(factory);
		// �������󣬲��õ��ϴ��ļ���FileItem����
		upload.setSizeMax(16*4096*1024);
		List<FileItem> items = null;
		try {
			items =upload.parseRequest(request);
		} catch (FileUploadException e) {
			e.printStackTrace();
			return -1;
		}
		String filename = ""; // �ϴ��ļ����浽���������ļ���
		InputStream is = null; // ��ǰ�ϴ��ļ���InputStream����
		// ѭ�������ϴ��ļ�
		for (FileItem item : items){
			// ������ͨ�ı���
			if (item.isFormField()){
				if(item.getFieldName().equals("comid")){
					if(!item.getString().equals(""))
						comId = item.getString("UTF-8");
				}
				
			}else if (item.getName() != null && !item.getName().equals("")){// �����ϴ��ļ�
				// �ӿͻ��˷��͹������ϴ��ļ�·���н�ȡ�ļ���
				logger.error(item.getName());
				filename = item.getName().substring(item.getName().lastIndexOf("\\")+1);
				is = item.getInputStream(); // �õ��ϴ��ļ���InputStream����
				
			}
		}
		String file_ext =filename.substring(filename.lastIndexOf(".")).toLowerCase();// ��չ��
		String picurl = uin+"_"+System.currentTimeMillis()+file_ext;
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
			  
		    DBCollection collection = mydb.getCollection("parkuser_pics");
		  //  DBCollection collection = mydb.getCollection("records_test");
			  
			BasicDBObject document = new BasicDBObject();
			document.put("uin",  uin);
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
		} catch (Exception e) {
			e.printStackTrace();
			return -1;
		}finally{
			if(in!=null)
				in.close();
			if(byteout!=null)
				byteout.close();
			if(is!=null)
				is.close();
		}
	  
		return 1;
	}*/
	
	
}
