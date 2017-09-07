package com.zld.struts.request;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import pay.AlipayUtil;

import com.zld.AjaxUtil;
import com.zld.impl.CommonMethods;
import com.zld.impl.PublicMethods;
import com.zld.service.DataBaseService;
import com.zld.service.LogService;
import com.zld.utils.Check;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
import com.zld.utils.ZLDType;

public class ZhifubaoHandle extends HttpServlet{
	private ServletContext servletContext;

	private DataBaseService daService;
	private PublicMethods publicMethods;
	private LogService logService;
	private CommonMethods commonMethods;
	
	private Logger logger = Logger.getLogger(ZhifubaoHandle.class);
	/**
	 * 
	 */
	private static final long serialVersionUID = 4942068508811134127L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		servletContext = getServletContext();
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(servletContext);
		daService = (DataBaseService)ctx.getBean("dataBaseService");
		publicMethods =(PublicMethods)ctx.getBean("publicMethods");
		logService=(LogService)ctx.getBean("logService");
		commonMethods = (CommonMethods) ctx.getBean("commonMethods");
		
		Map map = req.getParameterMap();
		/**
		 * ֧�����ص�˵�� �������������ص�
		 * 
		 * ��֧ͨ����type=1-6
		 * body=18101333937_0&buyer_email=15801482643&buyer_id=2088112611027102&discount=0.00
		 * &gmt_create=2016-11-28 17:02:42&is_total_fee_adjust=Y&notify_id=f5bd4caea603a135b2b53d0ede66885gru
		 * &notify_time=2016-11-28 17:02:42&notify_type=trade_status_sync&out_trade_no=112817023414043
		 * &payment_type=1&price=0.10&quantity=1&seller_email=caiwu@zhenlaidian.com&seller_id=2088411488582814
		 * &subject=�˻���ֵ&total_fee=0.10&trade_no=2016112821001004100200777631&trade_status=WAIT_BUYER_PAY
		 * &use_coupon=N
		 * 
		 * ɨ��ص���type=7
		 *  {body=-1_7_401723, open_id=20880058437367510024045431418146, subject=������BBBBBB֧��ͣ����, 
		 *  sign_type=RSA, buyer_logon_id=185****2450, auth_app_id=2016080801718601, 
		 *  notify_type=trade_status_sync, out_trade_no=24093932, version=1.0, point_amount=0.00, 
		 *  fund_bill_list=[{"amount":"0.01","fundChannel":"ALIPAYACCOUNT"}], total_amount=0.01, 
		 *  buyer_id=2088912086349460, trade_no=2016113021001004460276581016, 
		 *  notify_time=2016-11-30 13:07:30, charset=utf-8, invoice_amount=0.01, 
		 *  trade_status=TRADE_SUCCESS, gmt_payment=2016-11-30 13:01:18, 
		 *  sign=b+L7vZa+wMUJ+3DsM4qZAxXu2gNpaoR6aNJkCw69MyDm1NPQBAlAWd+Ep+jVgGvr7VppUco3O8N5i+ef/hpWDJysrTpz0tfk91LcG4+hl8KL566PbaBG/LxhB4JL8m8TMWf4QBRGLrWVHJghaT9HO2087LBRO9dOWRr1D1Fe/lI=, 
		 *  gmt_create=2016-11-30 13:00:58, buyer_pay_amount=0.01, receipt_amount=0.01, app_id=2016080801718601, 
		 *  seller_id=2088411488582814, notify_id=38fed5f14dd9ebaf883df1f422ff53ejju, seller_email=caiwu@zhenlaidian.com}
		 *  
		 *  ��Ҫ�����ǹ����ˣ�buyer_email -- buyer_logon_id����� ��total_fee --- total_amount
		 *  
		 *   ɨ��Ԥ�� uin_8_bolinkorderid
		 */
		Map<String, String> parMap = new HashMap<String, String>();
		String body = req.getParameter("body");//������Ϣ�Ĳ���
		String notify  =req.getParameter("notify_id");
		String userPayAccount = req.getParameter("buyer_id");
		logger.error(">>>>>alipay buyer_id:"+userPayAccount);
		/*
		 * Э��˵����
		 * body��
			�û��ֻ���_type; ��ǰ����0,1,2,3,4
			type��ֵ���ͣ�
			0���ʺų�ֵ��body�����Ϊ�����֣��û��ֻ���_0
			��:15801582643_0
			1����ֵ������ͣ�������²�Ʒ,body�����Ϊ������֣�
			�û��ֻ���_1_����Ĳ�Ʒ���_��������_��ʼ����
			��:15801482643_1_1022_3_20140815
			2:��ֵ��֧������,body������
			�û��ֻ���_2_�������_�Ż�ȯ��� 
			�磺15801482643_2_1011_1123
			3:��ֵ��ֱ��֧�����շ�Ա,body�岿�� 
			�û��ֻ���_3_�շ�Ա�˺�_֧�����_�Ż�ȯ���
			�磺15801482643_3_10700_15.0_1123
			4:��ֵ�����͸��շ�Ա,body������ 
			�û��ֻ���_4_�շ�Ա�˺�_���ͽ��_�������_�Ż�ȯ���
			�磺15801482643_4_10700_15.0_2590099_1123
			5:��ֵ������ͣ��ȯ,body�Ĳ��� 
			�û��ֻ���_5_ͣ��ȯ���_��������
			�磺15801482643_5_10_2
			6:��ֵ��֧������,body�岿�� 
			�û��ֻ���_6_������_���_ͣ��ȯ���
			�磺15801482643_6_333333_3.0_3344
			7:ɨ���ֵ��֧������,body�Ĳ��� 
			�û��˻�_7_�շ�Ա�˻������������out_trade_no�ش�������
			�磺21192_7_10999
		 */
		Long ntime = System.currentTimeMillis()/1000;
		String mobile="";//�û��ֻ��ţ�������־�û�
		String type = "";//��ֵ���ͣ�0���ʺų�ֵ��1����ֵ������ͣ�������²�Ʒ,2:��ֵ��֧������
		String pid = "";//����Ĳ�Ʒ��ţ�������һ�����Ŀ�ʼλ��ʶ��0������1������²�Ʒ��2....
		String number = "";//��������
		String start = "";//��ʼ����
		String orderId="-1";//�������
		Long ticketId = null;
		Long uid=-1L;//�շ�Ա�˺�
		Double money=0d;//ֱ��֧�����
		Integer bind_flag = 1;//0:δ���˻���1���Ѱ��˻�
		Integer ticketNumber =0;//��������
		Integer ticketPrice =0;//ͣ��ȯ���
		String tradeId = "";//req.getParameter("");
		logger.error("body:"+body);
		Long uin =null;
		if(body.indexOf("_")!=-1){
			String [] info = body.split("_");
			if(info.length>1){
				mobile = info[0];
				type = info[1];
				if(type!=null&&type.equals("1")&&info.length==5){
					pid = info[2];
					number = info[3];
					start = info[4];
				}else if(type.equals("2")){
					orderId=info[2];
					if(info.length==4&&Check.isLong(info[3]))
						ticketId = Long.valueOf(info[3]);
				}else if(type.equals("3")){
					try {
						orderId = daService.getkey("seq_order_tb") + "";//ֱ��û�ж�����Ԥȡһ��
						uid = Long.valueOf(info[2]);
						money = StringUtils.formatDouble(info[3]);
						ticketId = Long.valueOf(info[4]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}else if(type.equals("4")){
					try {//15801482643_4_10700_15.0_2590099_1123
						uid = Long.valueOf(info[2]);
						money = StringUtils.formatDouble(info[3]);
						orderId = info[4];
						ticketId = Long.valueOf(info[5]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}else if(type.equals("5")){
					try {//15801482643_5_10_2 �û��ֻ���_5_ͣ��ȯ���_��������
						ticketPrice = Integer.valueOf(info[2]);
						ticketNumber = Integer.valueOf(info[3]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}else if(type.equals("6")){
					try {//15801482643_6_333333_3.0_99999 //15801482643_type_������_���_ͣ��ȯ���
						orderId = info[2];
						money = StringUtils.formatDouble(info[3]);
						ticketId = Long.valueOf(info[4]);
					} catch (NumberFormatException e) {
						e.printStackTrace();
					}
				}else if(type.equals("7")){
					orderId = req.getParameter("out_trade_no");
					uin = Long.valueOf(mobile);
					uid = Long.valueOf(info[2]);
				}else if(type.equals("8")){
					tradeId = req.getParameter("out_trade_no");
					uin = Long.valueOf(mobile);
					orderId =info[2];
				}
			}
		}
		logger.error("���� ��,mobile="+mobile+",type="+type+",pid="+pid+",number="+number+",start="+start+",orderid="+orderId);
		
		if(!type.equals("7")){//ֻ��ɨ��֧��ʱ�������ǲ���ע�ᳵ��
			Map userMap = daService.getMap("select id from user_info_tb where mobile=? and auth_flag=? ", new Object[]{mobile,4});
			if(userMap!=null&&!userMap.isEmpty()){
				uin =(Long)userMap.get("id");
			}
		}
		logger.error("�ͻ���� ,uin="+uin);
		for(Object key :map.keySet()){
			parMap.put(key.toString(), req.getParameter(key.toString()));
		}
		String sign = req.getParameter("sign");
		//String buyer_email=req.getParameter("buyer_email");
		//String buyer_id=req.getParameter("buyer_id");
		
		boolean isalipay = false;
		logger.error("zhifubao callback:"+parMap);
		if(type.equals("7")||type.equals("8"))
			isalipay=AlipayUtil.getQrSignVeryfy(parMap,sign);
		else {
			isalipay=AlipayUtil.getSignVeryfy(parMap,sign);
		}
		logger.error("zhifubao callback,veryfy:"+isalipay);
		//System.out.println("user mobile:"+mobile);
		if(isalipay){//��֤�ɹ�
			logger.error("ali��֤�ɹ���");
			String state  = req.getParameter("trade_status");
			String total = req.getParameter("total_fee");
			if(total==null)
				total = req.getParameter("total_amount");
			Integer payType = 1;//- 0��1֧������2΢�ţ�3������4���+֧����,5���+΢��,6���+����
			if(state.equals("TRADE_FINISHED")|| state.equals("TRADE_SUCCESS")){
				logger.error("״̬����"+state+",ali������� ��");
				
				//��֤�Ƿ��ѳ�ֵ:
				Long count = daService.getLong("select count(*) from alipay_log where notify_no=? and create_time>? ", 
						new Object[]{notify,(System.currentTimeMillis()/1000-(12*60*60))});
				if(count>0){//�ѳ�ֵ�������ٴ���
					logger.error("�Ѵ������ֵ,���� ��");
					AjaxUtil.ajaxOutput(resp, "success");
					return ;
				}
				logger.error("δ�������ֵ....����....notify_id="+notify);
				boolean isbind=true;//�Ƿ�Ϊע�ᳵ����falseΪ��ʱ�˻�
				if(type.equals("7")&&uin==-1){//��ɨ��֧��,�ͻ������� 
					if(Check.isLong(orderId)){//�����ż��
						Map orderMap = daService.getMap("select car_number from order_tb where id =? ",new Object[]{ Long.valueOf(orderId)});
						if(orderMap!=null&&!orderMap.isEmpty()){
							String carNumber=(String)orderMap.get("car_number");
							if(Check.checkPhone(userPayAccount,"m")){//�����������ֻ���ע��ģ�����ע��һ���û������󶨺ó���
								logger.error("֧����ɨ��֧�����û�δע������û�֧����ʹ�õ��ֻ�ע�ᣬ�˻���"+userPayAccount);
								if(userPayAccount.startsWith("0"))//���˵�ǰ���0
									userPayAccount = userPayAccount.substring(1);
								//����û��Ƿ�ע���
								if(userPayAccount.length()==11){//�ֻ���11λ
									Map userMap = daService.getMap("select id from user_info_tb where mobile=? and auth_flag=? ", new Object[]{userPayAccount,4});
									if(userMap!=null&&!userMap.isEmpty()){//�û��Ѵ��� ��ʹ�õ�ǰ���˺�
										uin =(Long)userMap.get("id");
										logger.error("֧����ɨ��֧�����û���ע������˻���"+uin+",�����ܳ��Ʋ��Ǳ������ĳ��ƣ�֧����¼�˳�����");
									}else{//������ע��һ���µ��˻�
										uin = daService.getkey("seq_user_info_tb");
										String sql= "insert into user_info_tb (id,nickname,password,strid," +
												"reg_time,mobile,auth_flag,comid,media,recom_code) " +
												"values (?,?,?,?,?,?,?,?,?,?)";
										Object[] values= new Object[]{uin,"����","zlduser"+uin,"zlduser"+uin,ntime,userPayAccount,4,0,0,uid};
										int r = daService.update(sql,values);
										logger.error("֧����ɨ��֧�����û�δע�������֧����ע���ֻ�ע����һ���û����ֻ���"+userPayAccount+",uin="+uin+",r="+r);
										if(r==1){//����ע��ɹ����󶨳��ƺ�д�û�����
											//��ѯ�����Ƿ��Ѱ�
											Map carMap = daService.getMap("select uin from car_info_tb where car_number=? ",  new Object[]{carNumber});
											if(carMap==null||carMap.isEmpty()){
												r = daService.update("insert into car_info_tb(uin,car_number,create_time,remark) values(?,?,?,?)",
														new Object[]{uin,carNumber,ntime,"֧����ɨ��֧��ʱע��"});
												logger.error("֧����ɨ��֧�����û�δע������󶨳��ƣ�"+carNumber+",uin="+uin+",r="+r);
											}else {
												logger.error("֧����ɨ��֧����������ע�����"+carNumber+",���ٰ� ��ԭ�󶨳���Ϊ��uin="+carMap.get("uin"));
											}
											r= daService.update("insert into user_profile_tb (uin,low_recharge,limit_money,auto_cash," +
													"create_time,update_time) values(?,?,?,?,?,?)", 
													new Object[]{uin,10,25,1,ntime,ntime});
											logger.error("֧����ɨ��֧�����û�δע�������֧����ע���ֻ�ע����һ���û�,д�û�ͨ�����ã�r="+r);
										}
									}
								}
							}else {//�����߲������ֻ���ע��ģ��ѳ���д�������˻����û�appע��ʱ���������ݵ��û��˻���
								logger.error("֧����ɨ��֧�����û�δע������û�֧����û��ʹ�õ��ֻ�ע�ᣬ�˻���"+userPayAccount+",ֻ�ܸ��ݳ���дһ����ʱ�˻�");
								isbind=false;//��ʱ�˻�
								//����Ƿ�������ʱ�˻�
								Map wxpUserMap= daService.getMap("select uin,car_number from wxp_user_tb where car_number=? ", new Object[]{carNumber});
								if(wxpUserMap!=null&&wxpUserMap.get("uin")!=null){
									uin = (Long)wxpUserMap.get("uin");
									logger.error("֧����ɨ��֧��,������ʱ�˻���"+wxpUserMap);
								}
								if(uin==-1){
									uin = daService.getkey("seq_user_info_tb");
									int r = daService.update("insert into wxp_user_tb(openid,create_time,uin,car_number) values(?,?,?,?) ",
											new Object[] { "zhifubao", System.currentTimeMillis() / 1000, uin,orderMap.get("car_number")});
									logger.error("û����ʱ�˻�������һ��uin:"+uin+",car_number:"+orderMap.get("car_number")+",r:"+r);
								}
							}
						}else {
							logger.error("֧����ɨ��֧�����󣺶�����Ŵ��󣬲鲻������+"+orderId);
						}
					}else {
						logger.error("֧����ɨ��֧�����󣺶�����Ŵ��󣬸�ʽ���󣬲������֣�"+orderId);
					}
				}
				if(uin!=null&&uin>0){//�ͻ����ڡ�
					int result =0;
					if(type.equals("8")||!isbind){//֧����Ԥ������ע���û�;��ֵ����ʱ�˻�
						result=result=daService.update("update wxp_user_tb set balance =balance+? where uin=?  ", 
								new Object[]{Double.valueOf(total),uin});
					}else {
						result=daService.update("update user_info_tb set balance =balance+? where id=?  ", 
								new Object[]{Double.valueOf(total),uin});
					}
					try {
						List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
						Map<String, Object> userSqlMap = new HashMap<String, Object>();
						//Map<String, Object> tcbFeeSqlMap = new HashMap<String, Object>();
						Map<String, Object> tcbSqlMap = new HashMap<String, Object>();
						userSqlMap.put("sql", "insert into user_account_tb(uin,amount,type,create_time,remark,pay_type,orderid) values(?,?,?,?,?,?,?)");
						userSqlMap.put("values", new Object[]{uin,Double.valueOf(total),0,System.currentTimeMillis()/1000-2,"֧������ֵ",1,Long.valueOf(orderId)});
						bathSql.add(userSqlMap);
						tcbSqlMap.put("sql", "insert into tingchebao_account_tb(amount,type,create_time,remark,utype,orderid,online_orderid,uin) values(?,?,?,?,?,?,?,?)");
						tcbSqlMap.put("values", new Object[]{Double.valueOf(total),0,System.currentTimeMillis() / 1000 - 2,"֧������ֵ", 7, Long.valueOf(orderId), notify, uin });
						bathSql.add(tcbSqlMap);
//						tcbFeeSqlMap.put("sql", "insert into tingchebao_account_tb(amount,type,create_time,remark,utype,orderid,online_orderid,uin) values(?,?,?,?,?,?,?,?)");
//						tcbFeeSqlMap.put("values", new Object[]{Double.valueOf(total)*0.025,1,System.currentTimeMillis() / 1000 - 2,"֧������ֵ������", 1, Long.valueOf(orderId), notify, uin });
//						bathSql.add(tcbFeeSqlMap);
						
						boolean b = daService.bathUpdate(bathSql);
						logger.error("orderid:"+orderId+",uin:"+uin+",b:"+b);
						//д�û�֧���˺���Ϣ��
						logService.insertUserAccountMesg(0, uin, userPayAccount);
						
					} catch (Exception e) {
						e.printStackTrace();
					}
					
					
					logger.error("��ֵ,д��ͻ��ʻ�:money="+total+",uin="+uin+",result="+result);
					if(result==1){
						//д�����ֵ��־ 
						logger.error("��ֵ�ɹ���д�밢���ֵ��־ ....");
						daService.update("insert into alipay_log (notify_no,create_time,uin,money,orderid,wxp_orderid) values(?,?,?,?,?,?)",
								new Object[]{notify,System.currentTimeMillis()/1000,uin,Double.valueOf(total),Long.valueOf(orderId),req.getParameter("trade_no")});
						logger.error("��ֵ�����"+result+"���ֻ��ţ�"+mobile+" ����� ��"+total);
						//�ж���һ��������0��������1������²�Ʒ 2֧������
						if(type.equals("0")){//����ֵ
							logger.error("����ֵ...");
							daService.update( "insert into money_record_tb(comid,create_time,amount,uin,type,pay_type,remark) values (?,?,?,?,?,?,?)", 
									new Object[]{-1L,System.currentTimeMillis()/1000,Double.valueOf(total),uin,ZLDType.MONEY_RECARGE,payType,"��ֵ"});
							logger.error("��ֵ�ɹ���д���ֵ��־ ....");
							logService.insertMessage(-1L, 1, uin,"", 0L,Double.valueOf(total),"", 0,0L,0L,2);
							//����ֵʱ����֤������Ӧ���Ȳ�ȫ���ö��30Ԫ����ȫ�����Ľ��ſ��Գ䵽����С�
							Map userMap = daService.getMap("select is_auth,credit_limit from user_info_Tb where id=? ", new Object[]{uin});
							Integer isAuth = 0;
							if(userMap!=null){
								isAuth=(Integer)userMap.get("is_auth");
								if(isAuth!=null&&isAuth==1){
									Double limit = StringUtils.formatDouble(userMap.get("credit_limit"));
									if(limit!=null){
										if(limit<30){
											int r = daService.update("update user_info_tb set balance = balance-? ,credit_limit=? where id =? ", new Object[]{StringUtils.formatDouble(30-limit),30.0,uin});
											logger.error("��֤������Ӧ���Ȳ�ȫ���ö��30Ԫ,��ȫ��"+(30-limit)+",ret:"+r);
										}
									}
								}
							}
							Double tot= Double.valueOf(total)*100;
							//publicMethods.sendMessageToThird(uin,tot.intValue(), null, null, null, 1);
							logger.error("��ֵ�ɹ�������������Ϣ....");
							Long bid=0L;
							/*if(Double.valueOf(total)==100&&isAuth==1){//��ֵ100�������
								//�������
								String sql = "insert into order_ticket_tb (id,uin,order_id,money,bnum,ctime,exptime,bwords,type) values(?,?,?,?,?,?,?,?,?)";
								Long ctime = System.currentTimeMillis()/1000;
								Long exptime = ctime + 24*60*60;
								bid =daService.getkey("seq_order_ticket_tb");
								Object []values = new Object[]{bid,uin,-1,100,25,ctime,exptime,"����ͣ������ֵ��100Ԫ�����100Ԫͣ��ȯ����������25��С��飬�ֿ��У�������",4};
								int ret  = daService.update(sql,values);
								if(ret!=1)
									bid=0L;
								logger.error("����"+uin+"ͣ������ֵ��100Ԫ�������25/100������"+ret);
								logService.insertUserMesg(1, uin, "��ϲ����ó�ֵ�����", "�������");
							}*/
							// ���û�������������Ϣ
							publicMethods.syncUserToBolink(uin);
							logService.insertMessage(-1L, 1, uin, "", bid,Double.valueOf(total), total + "Ԫ��ֵ�ɹ�", 0, 0L,0L, 2);
							//publicMethods.sendMessageToThird(uin, Integer.valueOf(total), null, null, null, 1);
						}else if(type.equals("1")){//��ֵ��������²�Ʒ
							logger.error("��ֵ��������²�Ʒ...");
							Map productMap = daService.getMap("select * from product_package_tb where id=? and state=? and remain_number>? ", 
									new Object[]{Long.valueOf(pid),0,0});
							if(productMap!=null){
								String cname = (String)daService.getObject("select company_name from com_info_tb where id=?",new Object[]{productMap.get("comid")}, String.class);
								//д��ֵ��־
								Double price = Double.valueOf(productMap.get("price")+"");
								price = price*Integer.valueOf(number);
								if(price>Double.valueOf(total))
									payType=4;
								daService.update( "insert into money_record_tb(comid,create_time,amount,uin,type,pay_type,remark) values (?,?,?,?,?,?,?)", 
										new Object[]{-1L,System.currentTimeMillis()/1000,Double.valueOf(total),uin,ZLDType.MONEY_RECARGE,payType,productMap.get("p_name")+"��ֵ - "+cname});
								logger.error("��ֵ�ɹ���д���ֵ��־ ....");
								result=publicMethods.buyProducts(uin,productMap, Integer.valueOf(number), start,"",payType);
								logger.error(productMap.get("p_name")+"��ֵ - "+cname+":"+total+",result:"+result);
								//����������Ϣ
								logService.insertMessage(-1L, 1, uin,"", 0L,Double.valueOf(total),"", 0,0L,0L,2);
							}
						}else if(type.equals("2")){//��ֵ��֧������...
							logger.error("��ֵ��֧������...");
							//д��ֵ��־
							Map orderMap = daService.getMap("select * from order_tb where id=? ", new Object[]{Long.valueOf(orderId)});
							logger.error("total:"+total+"ordermap:"+orderMap);
							String cname = (String)daService.getObject("select company_name from com_info_tb where id=?",new Object[]{orderMap.get("comid")}, String.class);
							if(orderMap!=null){
								Long comId = (Long)orderMap.get("comid");
								Double ordermoney = 0d;
								if(orderMap.get("total") != null){
									ordermoney = StringUtils.formatDouble(orderMap.get("total"));
								}
								if(ordermoney==0.0)
									ordermoney =  StringUtils.formatDouble(total);
								//Double price = Double.valueOf(orderMap.get("total")+"");
								if(Double.valueOf(total)<ordermoney){
									payType=4;
								}
								daService.update( "insert into money_record_tb(comid,create_time,amount,uin,type,pay_type,remark) values (?,?,?,?,?,?,?)", 
										new Object[]{-1L,System.currentTimeMillis()/1000,Double.valueOf(total),uin,ZLDType.MONEY_RECARGE,payType,"ͣ���ѳ�ֵ-"+cname});
								logger.error("��ֵ�ɹ���д���ֵ��־ ....");
								
								result=publicMethods.payOrder(orderMap,ordermoney,uin,2,payType,ticketId,null,-1L,uid);
								logger.error("֧������:"+result+",orderid:"+orderId+",uin:"+uin+",comid:"+orderMap.get("comid"));
								int _state =-1;//Ĭ��֧�����ɹ�
								String carNumber = (String)orderMap.get("car_number");
								if(carNumber==null||"".equals(carNumber)||"���ƺ�δ֪".equals(carNumber))
										carNumber = publicMethods.getCarNumber(uin);
								Long endTime =(Long)orderMap.get("end_time"); 
								if(endTime==null)
									endTime=System.currentTimeMillis()/1000;
								if(result==5){
									_state  = 2;
									result = 1;
									//��֧���ɹ���Ϣ���շ�Ա
									logService.insertParkUserMessage((Long)orderMap.get("comid"), _state, (Long)orderMap.get("uid"),carNumber, Long.valueOf(orderId),ordermoney,"", 0, (Long)orderMap.get("create_time"), endTime,0, null);
									//����Ϣ������
								}else if(result==-7){
									logService.insertUserMesg(0, uin, "��������ԭ��"+cname+"��ͣ����"+total+"Ԫ��֧����֧��ʧ��", "֧��ʧ������");
								}
								if(comId==20130){//���͹�˾�Ĳ��Զ������������ǵĽӿڷ��Ͷ���֧��״̬
									String sr = commonMethods.sendOrderState2Baohe(Long.valueOf(orderId), result, ordermoney, payType);
									logger.error(">>>>>>>>>>>>>baohe sendresult:"+sr+",orderid:"+orderId+",state:"+result+",total:"+ordermoney+",paytype:"+payType);
								}
								logService.insertMessage((Long)orderMap.get("comid"), _state, uin,carNumber, Long.valueOf(orderId),ordermoney,"", 0,  (Long)orderMap.get("create_time"), endTime,0);
							}
						}else if(type.equals("3")) {//��ֵ��ֱ��֧�����շ�Ա...
							Long comId = daService.getLong("select comid from user_info_tb where id=? ", new Object[]{uid});
							String carNumber = publicMethods.getCarNumber(uin);
							result = publicMethods.epay(comId,money, uin, uid, ticketId, carNumber,4, bind_flag,Long.valueOf(orderId),null);
							logger.error(">>>>����ֱ��֧�����շ�Ա:" + result);
							int _state = -1;// Ĭ��֧�����ɹ�
							if (result == 5) {
								_state = 2;
								result = 1;
								// ��֧���ɹ���Ϣ���շ�Ա
								logService.insertParkUserMessage(comId,_state,uid,carNumber,Long.valueOf(orderId),money, "", 0,ntime,ntime+10, 0, null);
								// ����Ϣ������
							}else if(result==-7){
								logService.insertUserMesg(0, uin, "ֱ��ͣ����"+total+"Ԫ��֧����֧��ʧ�ܣ���ֵ����ѽ�������˻�", "֧��ʧ������");
							}
							//logService.doMessage(comId, _state, uin, carNumber,-1L,money, "֧���ɹ�",0, ntime,ntime+10, 2);
							logService.insertMessage(comId,_state, uin, carNumber,Long.valueOf(orderId),money,"", 0, ntime,ntime+10, 0);
						}else if(type.equals("4")){//��ֵ������
							int ptype = 1;//֧����
							if(money>Double.valueOf(total)){
								ptype=4;//���+֧����
							}
							String carNumber = publicMethods.getCarNumber(uin);
							int ret = publicMethods.doparkUserReward(uin, uid, Long.valueOf(orderId), ticketId, money, ptype,1);
							if(ret==1){
								Long btime = TimeTools.getToDayBeginTime();
								Long recount = daService.getLong("select count(id) from parkuser_reward_tb where uid =? and ctime >? ",
										new Object[]{uid,btime});
								Map<String, Object> tscoreMap = daService.getMap("select sum(score) tscore from reward_account_tb where type=? and create_time>? and uin=? ",
										new Object[] { 0, btime, uid });
								Long comid = -1L;
								if(tscoreMap != null && tscoreMap.get("tscore") != null){
									Double tscore = Double.valueOf(tscoreMap.get("tscore") + "");
									if(tscore >= 5000){
										comid = -2L;
										logger.error("���մ����Ѵ�����uid:"+uid+",tscore:"+tscore+",uin:"+uin);
									}
								}
								logService.insertParkUserMessage(comid,2,uid,carNumber,uin,money, ""+recount, 0,ntime,ntime+10,5, null);
								logService.insertMessage(-1L, 1, uin,carNumber,  0L,StringUtils.formatDouble(total),"", 0,ntime,ntime+10,2);
							}else if(ret==-2){
								logService.insertMessage(-1L, 0, uin,carNumber,  0L,StringUtils.formatDouble(total),"�Ѵ��͹�", 0,ntime,ntime+10,2);
							}else
								logService.insertMessage(-1L, 0, uin,carNumber,  0L,StringUtils.formatDouble(total),"", 0,ntime,ntime+10,2);
						}else if(type.equals("5")){//��ֵ������ͣ��ȯ
							boolean isAuth = publicMethods.isAuthUser(uin);
							Integer discount = 9;
							if(isAuth)
								discount=7;
							Double ttotal =  StringUtils.formatDouble(ticketNumber*ticketPrice*discount*0.1);
							int pttype=1;
							if(ttotal>StringUtils.formatDouble(total))
								pttype=4;
							int ret = publicMethods.buyTickets(uin, ticketPrice, ticketNumber, pttype);
							logger.error(mobile+"΢�Ź���ͣ��ȯ��val:"+ticketPrice+",num:"+ticketNumber+",ret:" + ret);
							// ���û�������������Ϣ
							logService.insertMessage(-1L, 1, uin, "", 0L,Double.valueOf(total), total + "Ԫ��ֵ������ͣ��ȯ�ɹ�",0, 0L, 0L, 2);
						}else if(type.equals("6")){//Ԥ��ͣ����
							logger.error("zhifubao prepay>>>orderid:"+orderId+",uin:"+uin+",wx_total:"+total+",money:"+money+",tickid:"+ticketId);
							Map orderMap = daService.getMap("select * from order_tb where id=? ",
									new Object[] { Long.valueOf(orderId) });
							Long comId = (Long)orderMap.get("comid");
							String carNumber = (String)orderMap.get("car_number");
							if(carNumber==null||"".equals(carNumber)||"���ƺ�δ֪".equals(carNumber))
									carNumber = publicMethods.getCarNumber(uin);
							int ret = publicMethods.prepay(orderMap, money, uin, ticketId, payType, bind_flag, null);
							logger.error("zhifubao prepay>>>orderid:"+orderId+",uin:"+uin+",ret:"+ret+",comId:"+comId);
							int _state = -1;// Ĭ��֧�����ɹ�
							if (ret == 1) {
								_state = 2;
							}else if(ret == -7){
								logService.insertUserMesg(0, uin, "��������ԭ��Ԥ��ͣ����"+total+"Ԫ��΢��֧��ʧ��", "֧��ʧ������");
							}
							if(comId==20130){//���͹�˾�Ĳ��Զ������������ǵĽӿڷ��Ͷ���֧��״̬
								String sr = commonMethods.sendPrepay2Baohe(Long.valueOf(orderId), ret,money, payType);
								logger.error(">>>>>>>>>>>>>baohe sendresult:"+sr+",orderid:"+orderId+",state:"+ret+",money:"+money+",paytype:"+payType);
							}
							logService.insertMessage((Long) orderMap.get("comid"),_state, uin, carNumber,Long.valueOf(orderId),
									money,"", 0, (Long) orderMap.get("create_time"), System.currentTimeMillis()/1000, 0);
						}else if(type.equals("7")){//֧����ɨ��֧��
							logger.error("֧����ɨ��֧������...");
							//д��ֵ��־
							Map orderMap = daService.getMap("select * from order_tb where id=? ", new Object[]{Long.valueOf(orderId)});
							logger.error("total:"+total+"ordermap:"+orderMap);
							String cname = (String)daService.getObject("select company_name from com_info_tb where id=?",new Object[]{orderMap.get("comid")}, String.class);
							if(orderMap!=null){
								//Double ordermoney = 0d;
								daService.update( "insert into money_record_tb(comid,create_time,amount,uin,type,pay_type,remark) values (?,?,?,?,?,?,?)", 
										new Object[]{-1L,System.currentTimeMillis()/1000,Double.valueOf(total),uin,ZLDType.MONEY_RECARGE,payType,"ͣ���ѳ�ֵ-"+cname});
								logger.error("֧����ɨ��ֵ�ɹ���д���ֵ��־ ....");
								
								result=publicMethods.payOrder(orderMap,Double.valueOf(total),uin,2,payType,ticketId,null,-1L,uid);
								logger.error("֧������:"+result+",orderid:"+orderId+",uin:"+uin+",comid:"+orderMap.get("comid"));
								int _state =-1;//Ĭ��֧�����ɹ�
								String carNumber = (String)orderMap.get("car_number");
								Long endTime =(Long)orderMap.get("end_time"); 
								if(endTime==null)
									endTime=System.currentTimeMillis()/1000;
								if(result==5){
									_state  = 2;
									result = 1;
								}
								//��֧���ɹ���Ϣ���շ�Ա
								logService.insertParkUserMessage((Long)orderMap.get("comid"), _state, uid,carNumber, Long.valueOf(orderId),Double.valueOf(total),"", 0, (Long)orderMap.get("create_time"), endTime,0, null);
								//���³����˻�������
								daService.update("update order_tb set uin=? where id = ? ", new Object[]{uin,Long.valueOf(orderId)});
							}
						}else if(type.equals("8")){
							logger.error("prepay bolink order ,update prepay :"+daService.update("update bolink_order_tb set prepay=?,prepay_time=? where id =? ", new Object[]{money,ntime,Long.valueOf(orderId)}));
							publicMethods.prepayToBolink(uin,money,Long.valueOf(orderId));
						}
					}else {
						logService.insertMessage(-1L, 0, uin,"", 0L,Double.valueOf(total),"������д��ʧ��", 0,0L,0L,2);
					}
				}else{
					logService.insertMessage(-1L, 0, uin,"", 0L,Double.valueOf(total),"��ֵ���󣬿ͻ������ڣ��ֻ���"+mobile, 0,0L,0L,2);
					logger.error("��ֵ���󣬿ͻ������ڣ��ֻ���"+mobile);
				}
			}else {
				logger.error("״̬WAIT_PAY������������... trade_status:"+state);
			}
			AjaxUtil.ajaxOutput(resp, "success");
			logger.error("�����ɹ������ظ�����success...");
		}else {//��֤ʧ��
			logService.insertMessage(-1L, 0, uin,"", 0L,0d,"֧����������֤ʧ��", 0,0L,0L,2);
			AjaxUtil.ajaxOutput(resp, "fail");
			logger.error("����ʧ�ܣ����ظ�����fail...");
		}
	}

}
