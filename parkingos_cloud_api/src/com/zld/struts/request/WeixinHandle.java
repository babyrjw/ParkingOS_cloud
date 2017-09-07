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

import pay.Constants;

import com.zld.AjaxUtil;
import com.zld.impl.CommonMethods;
import com.zld.impl.PublicMethods;
import com.zld.service.DataBaseService;
import com.zld.service.LogService;
import com.zld.utils.Check;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
import com.zld.utils.ZLDType;
import com.zld.weixinpay.utils.ResponseHandler;

public class WeixinHandle extends HttpServlet {
	private ServletContext servletContext;

	private DataBaseService daService;
	private PublicMethods publicMethods;
	private LogService logService;
	private CommonMethods commonMethods;

	private Logger logger = Logger.getLogger(WeixinHandle.class);
	/**
	 * AppID��wx73454d7f61f862a5 AppSecret��b3e563822a872e5a37eb692a856ed4ba
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

		logger.error("weixin callback begin....");
		/**
		 * ���΢���յ���Ӧ����success,��Ϊ֪ͨʧ�ܣ���ͨ��һ���Ĳ��ԣ���30���ӹ�8�Σ��������·���֪ͨ�����������֪ͨ�ĳɹ���
		 * ������֤֪ͨ�����ܳɹ�
		 * ���ڴ������·��ͺ�̨֪ͨ����������ͬ����֪ͨ��Ӧҵ�����ݵ�״̬���жϸ�֪ͨ�Ƿ��Ѿ�����������û�д�����ٽ��д���
		 * ����������ֱ�ӷ���success. �ڶ�ҵ�����ݽ���״̬���ʹ���֮ǰ��Ҫ�������������в������ƣ��Ա��⺯��������ɵ����ݻ��ҡ�
		 * Ŀǰ�������Ƶļ��ʱ��Ϊ8s,10s,10s,30s,60s,120s,360s,1000s
		 */
		// �̻���
		//String partner = Constants.WXPAY_PARTNERID;// "1900000109";
		// ��Կ
		String key = Constants.WXPAY_PARTNERKEY;// "8934e7d15453e97507ef794cf7b0519d";
		// ����֧��Ӧ�����
		ResponseHandler resHandler = new ResponseHandler(req, resp);
		resHandler.setKey(key);
		//�û�֧���˻�
		String userPayAccount = resHandler.getUserPayAccount();
		// �ж�ǩ��
		if (resHandler.isTenpaySign()) {

			// ֪ͨid
			//String notify_id = resHandler.getParameter("notify_id");

			// ��֤�Ƿ��ѳ�ֵ:
			servletContext = getServletContext();
			ApplicationContext ctx = WebApplicationContextUtils
					.getWebApplicationContext(servletContext);
			daService = (DataBaseService) ctx.getBean("dataBaseService");
			publicMethods = (PublicMethods) ctx.getBean("publicMethods");
			logService = (LogService) ctx.getBean("logService");
			commonMethods = (CommonMethods) ctx.getBean("commonMethods");

			// // �����������
			// RequestHandler queryReq = new RequestHandler(req, resp);
			// // ͨ�Ŷ���
			// TenpayHttpClient httpClient = new TenpayHttpClient();
			// // Ӧ�����
			// ClientResponseHandler queryRes = new ClientResponseHandler();

			/**
			 * weixin callback request paramaters: {
			 * attach=15375242041_1_25_1_20140925, bank_billno=201409257727769,
			 * bank_type=3006, discount=0, fee_type=1, input_charset=UTF-8,
			 * notify_id=UfkifSJxOiiL-WgfF1XVsNYYhKFfffy46cJI4V9_-
			 * _YluNFK44LjE4o4qFuUnAGdDqo1ZHkKZW4TNMEb4TE5guQz54K8AoKc,
			 * out_trade_no=dc36f18a9a0a776671d4879cae69b551,
			 * partner=1220886701, product_fee=2,
			 * sign=459604EA471EFE10CC7016189E30E86E, sign_type=MD5,
			 * time_end=20140925104544, total_fee=2, trade_mode=1,
			 * trade_state=0, transaction_id=1220886701201409253369167825,
			 * transport_fee=0 }
			 * 
			 * {attach=13860132164_0, bank_type=2022, discount=0, fee_type=1,
			 * input_charset=UTF-8,
			 * notify_id=UfkifSJxOiiTDJOoFUp2shlKb6uZYLChWD1PH9gmvfCoAf67
			 * -2kndxjTm6H4n2DLgdHUJMqXCmzlYBK-4isWJdWwm0GK4WHB,
			 * out_trade_no=49c9adb18e44be0711a94e827042f630,
			 * partner=1220886701, product_fee=100,
			 * sign=9619653CA169A1067F10C67BD0B63D76, sign_type=MD5,
			 * time_end=20141113112126, total_fee=100, trade_mode=1,
			 * trade_state=0, transaction_id=1220886701201411136069869971,
			 * transport_fee=0}
			 */

			// ͨ��֪ͨID��ѯ��ȷ��֪ͨ�����Ƹ�ͨ
			// queryReq.init();
			// queryReq.setKey(key);
			// queryReq.setGateUrl("https://gw.tenpay.com/gateway/verifynotifyid.xml");
			// queryReq.setParameter("partner", partner);
			// queryReq.setParameter("notify_id", notify_id);
			//
			// // ͨ�Ŷ���
			// httpClient.setTimeOut(5);
			// // ������������
			// httpClient.setReqContent(queryReq.getRequestURL());
			// System.out.println("queryReq:" + queryReq.getRequestURL());
			// ��̨����
			// if (httpClient.call()) {
			// ���ý������
			// try {
			// queryRes.setContent(httpClient.getResContent());
			// } catch (Exception e) {
			// e.printStackTrace();
			// }
			// System.out.println("queryRes:" + httpClient.getResContent());
			// queryRes.setKey(key);
			//
			// // ��ȡ���ز���
			// String retcode = queryRes.getParameter("retcode");
			// String trade_state = queryRes.getParameter("trade_state");
			//
			// String trade_mode = queryRes.getParameter("trade_mode");
			String total_fee = resHandler.getParameter("total_fee");
			Double total = Double.valueOf(total_fee) * 0.01;
			String attach = resHandler.getParameter("attach");
			/*
			 * Э��˵���� attach�� �û��ֻ���_type; type��ֵ���ͣ� 0���ʺų�ֵ��body�����Ϊ�����֣��û��ֻ���_0
			 * ��:15801582643_0 1����ֵ������ͣ�������²�Ʒ,body�����Ϊ������֣�
			 * �û��ֻ���_1_����Ĳ�Ʒ���_��������_��ʼ���� ��:15801482643_1_1022_3_20140815
			 * 2:��ֵ��֧������,body������ �û��ֻ���_2_�������_�Ż�ȯ��� �磺15801482643_2_1011_1123
			 * 3:��ֵ��ֱ��֧�����շ�Ա,body�岿�� �û��ֻ���_3_�շ�Ա�˺�_֧�����_�Ż�ȯ��� �磺15801482643_3_10700_15.0_1123
			 * 4:��ֵ�����͸��շ�Ա,body������ 
				�û��ֻ���_4_�շ�Ա�˺�_���ͽ��_�������_�Ż�ȯ���
				�磺15801482643_4_10700_15.0_2590099_1123
				5:��ֵ������ͣ��ȯ,body�Ĳ��� 
				�û��ֻ���_5_ͣ��ȯ���_��������
				�磺15801482643_5_10_2
			 */
			// System.err.println(attach);
			logger.error("attach:" + attach);
			Integer payType = 2;// - 0��1֧������2΢�ţ�3������4���+֧����,5���+΢��,6���+����
			// �������ݿ��߼�
			if (attach == null || "".equals(attach)) {
				logger.error("attach�գ�û�п�����Ϣ");
				resHandler.sendToCFT("Fail");
				return;
			}
			Long ntime = System.currentTimeMillis()/1000;
			String mobile = "";// �û��ֻ��ţ�������־�û�
			String type = "";// ��ֵ���ͣ�0���ʺų�ֵ��1����ֵ������ͣ�������²�Ʒ,2:��ֵ��֧������
			String pid = "";// ����Ĳ�Ʒ��ţ�������һ�����Ŀ�ʼλ��ʶ��0������1������²�Ʒ��2....
			String number = "";// ��������
			String start = "";// ��ʼ����
			String orderId = "-1";// �������
			Long uid=-1L;//�շ�Ա�˺�
			Double money=0d;//ֱ��֧�����
			Integer ticketNumber =0;//��������
			Integer ticketPrice =0;//ͣ��ȯ���
			Long ticketId = null;
			Integer bind_flag = 1;//0:δ���˻���1���Ѱ��˻�
			logger.error("attach:"+attach);
			if (attach.indexOf("_") != -1) {
				String[] info = attach.split("_");
				if (info.length > 1) {
					mobile = info[0];
					type = info[1];
					if (type != null && type.equals("1") && info.length == 5) {
						pid = info[2];
						number = info[3];
						start = info[4];
					} else if (type.equals("2")) {
						orderId = info[2];
						if (info.length == 4 && Check.isLong(info[3]))
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
						try {//15801482643_5_10_2 �û��ֻ���_5_ͣ��ȯ���_��������
							orderId = info[2];
							money = StringUtils.formatDouble(info[3]);
							ticketId = Long.valueOf(info[4]);
						} catch (NumberFormatException e) {
							e.printStackTrace();
						}
					}
				}
			}
			logger.error(">>>>>>body=["+attach+"],���� ��,mobile=" + mobile + ",type=" + type + ",pid="
					+ pid + ",number=" + number + ",start=" + start+ ",orderid=" + orderId+",uid="+uid+",total="+money+",ticketid="+ticketId);
			Long uin = (Long) daService.getObject("select id from user_info_Tb where mobile=? and auth_flag=? ",
							new Object[] { mobile, 4 }, Long.class);
			logger.error("�ͻ���� ,uin=" + uin);
			// �ж�ǩ�������
			// if (queryRes.isTenpaySign() && "0".equals(retcode)
			// && "0".equals(trade_state) && "1".equals(trade_mode)) {
			// logger.error("΢����֤ͨ��");
			// ȡ���������ҵ����
			/*
			 * System.out.println("out_trade_no:"+
			 * queryRes.getParameter("out_trade_no") + " transaction_id:"+
			 * queryRes.getParameter("transaction_id"));
			 * System.out.println("trade_state:"+
			 * queryRes.getParameter("trade_state") + " total_fee:"+
			 * queryRes.getParameter("total_fee")); //
			 * �����ʹ���ۿ�ȯ��discount��ֵ��total_fee+discount=ԭ�����total_fee
			 * System.out.println("discount:"+ queryRes.getParameter("discount")
			 * + " time_end:" + queryRes.getParameter("time_end"));
			 */// ------------------------------
				// ����ҵ��ʼ
				// ------------------------------
				// ���ط���ÿ�λص�ʱ��out_trade_no��ͬ
			String out_trade_no = resHandler.getParameter("out_trade_no");
			logger.error("out_trade_no:" + out_trade_no);
			Long count = daService.getLong("select count(*) from alipay_log where notify_no=? and create_time>?",
					new Object[] { out_trade_no ,(System.currentTimeMillis()/1000-(12*60*60))});
			if (count > 0) {// �ѳ�ֵ�������ٴ���
				logger.error("�Ѵ������ֵ,���� ��");
				AjaxUtil.ajaxOutput(resp, "Success");
				return;
			}
			logger.error("δ�������ֵ....����....out_trade_no=" + out_trade_no);
			// ע�⽻�׵���Ҫ�ظ�����
			// ע���жϷ��ؽ��

			if (uin != null) {// �ͻ����ڡ�
				int result = daService.update("update user_info_tb set balance =balance+? where id=?  ",
								new Object[] { Double.valueOf(total), uin });

				try {
					List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
					Map<String, Object> userSqlMap = new HashMap<String, Object>();
					Map<String, Object> tcbFeeSqlMap = new HashMap<String, Object>();
					Map<String, Object> tcbSqlMap = new HashMap<String, Object>();
					userSqlMap.put("sql", "insert into user_account_tb(uin,amount,type,create_time,remark,pay_type,orderid) values(?,?,?,?,?,?,?)");
					userSqlMap.put("values", new Object[]{uin,Double.valueOf(total),0,System.currentTimeMillis() / 1000 - 2,"΢�ų�ֵ", 2, Long.valueOf(orderId) });
					bathSql.add(userSqlMap);
					tcbSqlMap.put("sql", "insert into tingchebao_account_tb(amount,type,create_time,remark,utype,orderid,online_orderid,uin) values(?,?,?,?,?,?,?,?)");
					tcbSqlMap.put("values", new Object[]{total,0,System.currentTimeMillis() / 1000 - 2,"΢�ų�ֵ", 6, Long.valueOf(orderId), out_trade_no, uin });
					bathSql.add(tcbSqlMap);
					tcbFeeSqlMap.put("sql", "insert into tingchebao_account_tb(amount,type,create_time,remark,utype,orderid,online_orderid,uin) values(?,?,?,?,?,?,?,?)");
					tcbFeeSqlMap.put("values", new Object[]{Double.valueOf(total) * 0.006,1,System.currentTimeMillis() / 1000 - 2,"΢�ų�ֵ������", 2, Long.valueOf(orderId), out_trade_no, uin });
					bathSql.add(tcbFeeSqlMap);
					
					boolean b = daService.bathUpdate(bathSql);
					logger.error("orderid:"+orderId+",uin:"+uin+",b:"+b);
					//д�û�֧���˺���Ϣ��
					logService.insertUserAccountMesg(1, uin, userPayAccount);
					
				} catch (Exception e) {
					e.printStackTrace();
				}

				logger.error("��ֵ,д��ͻ��ʻ�:money=" + total + ",uin=" + uin+ ",result=" + result);
				if (result == 1) {
					// д�����ֵ��־
					logger.error("��ֵ�ɹ���д��΢�ų�ֵ��־ ....");
					daService.update("insert into alipay_log (notify_no,create_time,uin,money,orderid) values(?,?,?,?,?)",
									new Object[] { out_trade_no,System.currentTimeMillis() / 1000,uin, Double.valueOf(total),Long.valueOf(orderId) });
					logger.error("��ֵ�����" + result + "���ֻ��ţ�" + mobile + " ����� ��"+ total);
					// �ж���һ��������0��������1������²�Ʒ 2֧������
					if (type.equals("0")) {// ����ֵ
						logger.error("����ֵ...");
						daService.update("insert into money_record_tb(comid,create_time,amount,uin,type,pay_type,remark) values (?,?,?,?,?,?,?)",
										new Object[] {-1L,System.currentTimeMillis() / 1000,Double.valueOf(total), uin,ZLDType.MONEY_RECARGE, payType,"��ֵ" });
						logger.error("��ֵ�ɹ���д���ֵ��־ ....");
						//����ֵʱ����֤������Ӧ���Ȳ�ȫ���ö��30Ԫ����ȫ�����Ľ��ſ��Գ䵽����С�
						Map userMap = daService.getMap("select is_auth,credit_limit from user_info_Tb where id=? ", new Object[]{uin});
						Integer isAuth =0;
						if(userMap!=null){
							isAuth= (Integer)userMap.get("is_auth");
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
						Long bid=0L;
						/*if(total==100&&isAuth==1){//��ֵ100�������
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
						publicMethods.syncUserToBolink(uin);
						// ���û�������������Ϣ
						logService.insertMessage(-1L, 1, uin, "", bid,Double.valueOf(total), total + "Ԫ��ֵ�ɹ�", 0, 0L,0L, 2);
						//publicMethods.sendMessageToThird(uin, Integer.valueOf(total_fee), null, null, null, 1);
					} else if (type.equals("1")) {// ��ֵ��������²�Ʒ
						logger.error("��ֵ��������²�Ʒ...");
						Map productMap = daService.getMap("select * from product_package_tb where id=? and state=? and remain_number>? ",
										new Object[] { Long.valueOf(pid), 0, 0 });
						if (productMap != null) {
							String cname = (String) daService.getObject("select company_name from com_info_tb where id=?",
											new Object[] { productMap.get("comid") },String.class);
							// д��ֵ��־
							Double price = Double.valueOf(productMap.get("price") + "");
							price = price * Integer.valueOf(number);
							if (price > Double.valueOf(total))
								payType = 5;
							daService.update("insert into money_record_tb(comid,create_time,amount,uin,type,pay_type,remark) values (?,?,?,?,?,?,?)",
											new Object[] {-1L,System.currentTimeMillis() / 1000,Double.valueOf(total),uin,
													ZLDType.MONEY_RECARGE,payType,productMap.get("p_name")+ "��ֵ - " + cname });
							logger.error("��ֵ�ɹ���д���ֵ��־ ....");
							result = publicMethods.buyProducts(uin, productMap,
									Integer.valueOf(number), start,"", payType);
							logger.error(productMap.get("p_name") + "��ֵ - "
									+ cname + ":" + total + ",result:" + result);
							// ���û�������������Ϣ
							logService.insertMessage(-1L, 1, uin, "", 0L,Double.valueOf(total), total + "Ԫ��ֵ������"+ productMap.get("p_name") + "�ɹ�",0, 0L, 0L, 2);

						}
					} else if (type.equals("2")) {// ��ֵ��֧������...
						logger.error("��ֵ��֧������...");
						// д��ֵ��־
						Map orderMap = daService.getMap("select * from order_tb where id=? ",
								new Object[] { Long.valueOf(orderId) });
						logger.error("total:"+total+"ordermap:"+orderMap);
						if (orderMap != null) {
							String cname = (String) daService.getObject("select company_name from com_info_tb where id=?",
											new Object[] { orderMap.get("comid") },String.class);
							Long comId = (Long)orderMap.get("comid");
							Double ordermoney =  0d;
							if(orderMap.get("total") != null){
								ordermoney = StringUtils.formatDouble(orderMap.get("total"));
							}
							if(ordermoney==0.0)
								ordermoney =  StringUtils.formatDouble(total);
							//Double price = Double.valueOf(orderMap.get("total")+ "");
							if (Double.valueOf(total) < ordermoney) {
								payType = 5;
							}
							daService.update("insert into money_record_tb(comid,create_time,amount,uin,type,pay_type,remark) values (?,?,?,?,?,?,?)",
											new Object[] {-1L,System.currentTimeMillis() / 1000,Double.valueOf(total), uin,
													ZLDType.MONEY_RECARGE,payType, "ͣ���ѳ�ֵ-" + cname });
							logger.error("��ֵ�ɹ���д���ֵ��־ ....");
							
							result = publicMethods.payOrder(orderMap,ordermoney,
									uin, 2, payType, ticketId, null,-1L, uid);
							logger.error("֧������:" + result+",orderid:"+orderId+",uin:"+uin+",comid:"+orderMap.get("comid"));
							int _state = -1;// Ĭ��֧�����ɹ�
							String carNumber = (String)orderMap.get("car_number");
							if(carNumber==null||"".equals(carNumber)||"���ƺ�δ֪".equals(carNumber))
									carNumber = publicMethods.getCarNumber(uin);
							Long endTime =(Long)orderMap.get("end_time"); 
							if(endTime==null)
								endTime=System.currentTimeMillis()/1000;
							if (result == 5) {
								_state = 2;
								result = 1; 
								// ��֧���ɹ���Ϣ���շ�Ա
								logService.insertParkUserMessage((Long) orderMap.get("comid"),_state,(Long) orderMap.get("uid"),carNumber,
										Long.valueOf(orderId),ordermoney, "", 0,
										(Long) orderMap.get("create_time"),endTime, 0, null);
								// ����Ϣ������
							}else if(result==-7){
								logService.insertUserMesg(0, uin, "��������ԭ��"+cname+"��ͣ����"+total+"Ԫ��΢��֧��ʧ��", "֧��ʧ������");
							}
							if(comId==20130){//���͹�˾�Ĳ��Զ������������ǵĽӿڷ��Ͷ���֧��״̬
								String sr = commonMethods.sendOrderState2Baohe(Long.valueOf(orderId), result,ordermoney, payType);
								logger.error(">>>>>>>>>>>>>baohe sendresult:"+sr+",orderid:"+orderId+",state:"+result+",total:"+ordermoney+",paytype:"+payType);
							}
							logService.insertMessage((Long) orderMap.get("comid"),_state, uin, carNumber,Long.valueOf(orderId),
									ordermoney,"", 0, (Long) orderMap.get("create_time"),
									endTime, 0);
						}
					}else if(type.equals("3")) {//��ֵ��ֱ��֧�����շ�Ա...
						Long comId = daService.getLong("select comid from user_info_tb where id=? ", new Object[]{uid});
						String carNumber = publicMethods.getCarNumber(uin);
						result = publicMethods.epay(comId,money, uin, uid, ticketId, carNumber,5, bind_flag,Long.valueOf(orderId),null);
						logger.error(">>>>����ֱ��֧�����շ�Ա:" + result);
						int _state = -1;// Ĭ��֧�����ɹ�
						if (result == 5) {
							_state = 2;
							result = 1;
							// ��֧���ɹ���Ϣ���շ�Ա
							logService.insertParkUserMessage(comId,_state,uid,carNumber,Long.valueOf(orderId),money, "", 0,ntime,ntime+10, 0, null);
							// ����Ϣ������
						}
						//logService.doMessage(comId, _state, uin, carNumber,-1L,money, "֧���ɹ�",0, ntime,ntime+10, 2);
						logService.insertMessage(comId,_state, uin, carNumber,Long.valueOf(orderId),money,"", 0, ntime,ntime+10, 0);
					}else if(type.equals("4")){//��ֵ������
						int ptype = 2;//΢��
						if(money>Double.valueOf(total)){
							ptype=5;//���+΢��
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
							logService.insertMessage(-1L, 1, uin,carNumber, 0L,StringUtils.formatDouble(total),"֧���ɹ�!", 0,ntime,ntime+10,2);
						}else if(ret==-2){
							logService.insertMessage(-1L, 0, uin,carNumber, 0L,StringUtils.formatDouble(total),"�Ѵ��͹�", 0,ntime,ntime+10,2);
						}else {
							logService.insertMessage(-1L, 0, uin,carNumber, 0L,StringUtils.formatDouble(total),"֧��ʧ��", 0,ntime,ntime+10,2);
						}
					}else if(type.equals("5")){//��ֵ������ͣ��ȯ
						boolean isAuth = publicMethods.isAuthUser(uin);
						Integer discount = 9;
						if(isAuth)
							discount=7;
						Double ttotal =  StringUtils.formatDouble(ticketNumber*ticketPrice*discount*0.1);
						int pttype=2;
						if(ttotal>StringUtils.formatDouble(total))
							pttype=5;
						int ret = publicMethods.buyTickets(uin, ticketPrice, ticketNumber, pttype);
						logger.error(mobile+"΢�Ź���ͣ��ȯ��val:"+ticketPrice+",num:"+ticketNumber+",ret:" + ret);
						// ���û�������������Ϣ
						logService.insertMessage(-1L, 1, uin, "", 0L,Double.valueOf(total), total + "Ԫ��ֵ������ͣ��ȯ�ɹ�",0, 0L, 0L, 2);
					}else if(type.equals("6")){
						logger.error("weixin prepay>>>orderid:"+orderId+",uin:"+uin+",wx_total:"+total+",money:"+money+",tickid:"+ticketId);
						Map orderMap = daService.getMap("select * from order_tb where id=? ",
								new Object[] { Long.valueOf(orderId) });
						Long comId = (Long)orderMap.get("comid");
						String carNumber = (String)orderMap.get("car_number");
						if(carNumber==null||"".equals(carNumber)||"���ƺ�δ֪".equals(carNumber))
								carNumber = publicMethods.getCarNumber(uin);
						int ret = publicMethods.prepay(orderMap, money, uin, ticketId, payType, bind_flag, null);
						logger.error("weixin prepay>>>orderid:"+orderId+",uin:"+uin+",ret:"+ret+",comId:"+comId);
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
					}else if(result==-7){
						logService.insertUserMesg(0, uin, "ֱ��ͣ����"+total+"Ԫ��΢��֧��ʧ�ܣ���ֵ����ѽ�������˻�", "֧��ʧ������");
					}
				} else {
					logService.insertMessage(-1L, 0, uin, "", 0L,Double.valueOf(total), "д���û����ʧ��", 0, 0L, 0L, 2);
				}
			} else {
				logger.error("��ֵ���󣬿ͻ������ڣ��ֻ���" + mobile);
				// resHandler.sendToCFT("Fail");
				logService.insertMessage(-1L, 0, uin, "", 0L,Double.valueOf(total), "������Ϣ������", 0, 0L, 0L, 2);
				// return;
			}
			// ------------------------------
			// ����ҵ�����
			// ------------------------------
			logger.error("΢��֧���ɹ�....����Ϣ������");
			resHandler.sendToCFT("Success");
			return;
			/*
			 * } else { // ����ʱ�����ؽ��δǩ������¼retcode��retmsg��ʧ�����顣
			 * System.out.println("��ѯ��֤ǩ��ʧ�ܻ�ҵ�����");
			 * System.out.println("retcode:"+ queryRes.getParameter("retcode") +
			 * " retmsg:" + queryRes.getParameter("retmsg"));
			 * logService.doMessage(-1L, 0, uin,"",
			 * 0L,Double.valueOf(total),"΢����֤��ͨ��", 0,0L,0L,2);
			 * logger.error("΢��֧������...."); }
			 */

			/*
			 * } else {
			 * 
			 * System.out.println("��̨����ͨ��ʧ��"); //logService.doMessage(-1L, 0,
			 * uin,"", 0L,Double.valueOf(total),"΢����֤��ͨ��", 0,0L,0L,2);
			 * System.out.println(httpClient.getResponseCode());
			 * System.out.println(httpClient.getErrInfo()); //
			 * �п�����Ϊ����ԭ�������Ѿ�������δ�յ�Ӧ�� logger.error("΢��֧������...."); }
			 */
		} else {
			System.out.println("֪ͨǩ����֤ʧ��");
			logger.error("΢��֧������....");
		}
		logger.error("΢��֧������....");
		resHandler.sendToCFT("Fail");
	}

}
