package com.zld.struts.request;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.jdom.JDOMException;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import pay.Constants;

import com.zld.CustomDefind;
import com.zld.impl.CommonMethods;
import com.zld.impl.PublicMethods;
import com.zld.service.DataBaseService;
import com.zld.service.LogService;
import com.zld.utils.HttpsProxy;
import com.zld.utils.RequestUtil;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
import com.zld.utils.ZLDType;
import com.zld.weixinpay.utils.util.XMLUtil;
import com.zld.wxpublic.util.PayCommonUtil;

public class WeixinPublicHandle extends HttpServlet {
	private ServletContext servletContext;

	private DataBaseService daService;
	private PublicMethods publicMethods;
	private LogService logService;
	private CommonMethods commonMethods;

	private Logger logger = Logger.getLogger(WeixinPublicHandle.class);
	private static final long serialVersionUID = 4942068508811134127L;

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		doPost(req, resp);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		InputStream inStream = request.getInputStream();
		ByteArrayOutputStream outSteam = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = 0;
		while ((len = inStream.read(buffer)) != -1) {
			outSteam.write(buffer, 0, len);
		}
		outSteam.close();
		inStream.close();
		String result = new String(outSteam.toByteArray(), "utf-8");// ��ȡ΢�ŵ�������notify_url�ķ�����Ϣ
//		System.out.println(result);
		
		try {
			SortedMap<Object, Object> packageParams = new TreeMap<Object, Object>();
			Map<Object, Object> map = XMLUtil.doXMLParse(result);
			for(Object keyValue : map.keySet()){
	            packageParams.put(keyValue, map.get(keyValue));
	        }
			
			if(map.get("return_code").toString().equalsIgnoreCase("SUCCESS")){
				String sign = PayCommonUtil.createSign("UTF-8", packageParams);//�ص���֤ǩ��
				String signreturn = (String)map.get("sign");
				if (signreturn.equals(sign) && map.get("result_code").toString().equalsIgnoreCase("SUCCESS")) {
					logger.error("=========΢�Ź��ںŸ���ɹ�========");
					//TODO �����߼�
					String openid = (String)map.get("openid");//�˻�
					logger.error("=========΢�Ź��ںŸ���ɹ�=====֧���˻���===:"+openid);
					String total_fee = (String)map.get("total_fee");
					Double wx_total = Double.valueOf(total_fee) * 0.01;//΢��֧���Ľ��
					String attach = (String)map.get("attach");//���Ӳ���
					String out_trade_no = (String)map.get("out_trade_no");//�������
					String wxp_orderid = (String)map.get("transaction_id");
					
					Long ntime = System.currentTimeMillis()/1000;
					Long ticketId = -1L;//ͣ��ȯID
					Double ticketMoney = 0d;//ͣ��ȯ�������
					Long disTicketId = -1L;//����ȯ���
					Double disTicketMoney = 0d;//����ȯ�ֿ۽��
					Double money = 0d;//���ѽ��
					String mobile = "";//�ͻ��ֻ���
					Long uid = -1L;//�շ�Ա���
					Integer type = 0;//�������ͣ� 0:ֱ��ͣ���� 1��������2,ҡһҡ������δ�󶨣�
					Long uin =null;
					Long orderId = -1L;
					Integer bind_flag = 1;//�Ѱ��˻�
					Integer ticketmoney = 0;//����ͣ��ȯ����ֵ
					Integer ticketnum = 0;//����ͣ��ȯ������
					String starttime = "";//�����¿��ĳ�ʼʱ��
					Integer months = 0;//������¿�����
					Long prodid = -1L;//�¿����
					Long end_time = -1L;//��������ʱ��,2016-07-07���
					
					JSONObject jsonObject = JSONObject.fromObject(attach);
					if(jsonObject.get("ticketId") != null){
						ticketId = Long.valueOf(jsonObject.get("ticketId")+"");//ͣ��ȯ
					}
					if(jsonObject.get("ticketMoney") != null){
						ticketMoney = Double.valueOf((String)jsonObject.get("ticketMoney"));
					}
					if(jsonObject.get("money") != null){
						money = Double.valueOf((String)jsonObject.get("money"));
					}
					if(jsonObject.get("type") != null){
						type = Integer.valueOf((String)jsonObject.get("type"));
					}
					
					if(jsonObject.get("uid") != null){
						uid = Long.valueOf((String)jsonObject.get("uid"));
					}
					if(jsonObject.get("mobile") != null){
						mobile = (String)jsonObject.get("mobile");
					}
					if(jsonObject.get("uin") != null){
						uin = Long.valueOf((String)jsonObject.get("uin"));
					}
					if(jsonObject.get("orderid") != null){
						orderId = Long.valueOf((String)jsonObject.get("orderid"));
					}
					if(type == 0){//ֱ����û�ж����������ݿ�Ԥȡһ��
						orderId = daService.getkey("seq_order_tb");
					}
					if(jsonObject.get("ticketmoney") != null){
						ticketmoney = Integer.valueOf((String)jsonObject.get("ticketmoney"));
					}
					if(jsonObject.get("ticketnum") != null){
						ticketnum = Integer.valueOf((String)jsonObject.get("ticketnum"));
					}
					if(jsonObject.get("starttime") != null){
						starttime = (String)jsonObject.get("starttime");
					}
					if(jsonObject.get("months") != null){
						months = Integer.valueOf((String)jsonObject.get("months"));
					}
					if(jsonObject.get("prodid") != null){
						prodid = Long.valueOf((String)jsonObject.get("prodid"));
					}else if(jsonObject.get("cardid") != null){
						prodid = Long.valueOf((String)jsonObject.get("cardid"));
					}
					if(jsonObject.get("end_time") != null){
						end_time = Long.valueOf((String)jsonObject.get("end_time"));
					}
					if(jsonObject.get("disTicketId") != null){
						disTicketId = Long.valueOf((String)jsonObject.get("disTicketId"));
					}
					if(jsonObject.get("disTicketMoney") != null){
						disTicketMoney = Double.valueOf((String)jsonObject.get("disTicketMoney"));
					}
					if(jsonObject.get("pay_time") != null){
						disTicketMoney = Double.valueOf((String)jsonObject.get("pay_time"));
					}
					
					Long count = daService.getLong("select count(*) from alipay_log where notify_no=? and create_time>?",
							new Object[] { out_trade_no ,(System.currentTimeMillis()/1000-(30*60*60))});
					if(count > 0){//�Ѿ�����������ٴ���
						logger.error("����out_trade_no:"+out_trade_no + "�Ѿ�����������أ�");
						response.getWriter().write(PayCommonUtil.setXML("SUCCESS", ""));//����΢�ŷ����������յ���Ϣ�ˣ���Ҫ�ڵ��ûص�action��
						return;
					}
					Map<String, Object> userMap = daService.getMap(
							"select * from user_info_tb where wxp_openid=? ",
							new Object[] { openid });
					//boolean isBolinkUser = false;//�Ƿ��ǲ����û�
					if(userMap == null){//δ���˻�
						bind_flag = 0;
					}
					/*else{
						Integer unionState = (Integer)userMap.get("union_state");//��ͬ��״̬����ͬ����������
						if(unionState>0)
							isBolinkUser=true;
					}
						*/
					Integer ret = -1;
					if(bind_flag == 0){//δ���˻������������˻�
						Map<String, Object> nobindMap= daService.getMap("select * from wxp_user_tb where openid=? ",
								new Object[] { openid });
						logger.error("δ���˻���΢���˻���"+openid + ",attach:"+attach +"type:"+type);
						if(nobindMap != null){
							logger.info("pay monthcard=====΢�ŵ��ô�����uin:"+uin);
							uin = (Long)nobindMap.get("uin");
							logger.error("pay monthcard,δ���˻�������΢���˻���"+openid + "��ѯ����uin:"+uin);
							ret = daService.update("update wxp_user_tb set balance =balance+? where uin=?  ",
									new Object[] { wx_total, uin });
							logger.error("δ���˻����������˻����ֵ��"+wx_total + "Ԫ,openid:"+openid +"type:"+type);
							
						}
					}else{//�Ѱ��˻���������ʵ�ʻ�
						logger.error("�Ѱ��˻���΢���˻���"+openid + ",attach:"+attach +"type:"+type);
						if (uin == null) {
							uin = (Long)userMap.get("id");
							logger.error("pay monthcard,�Ѱ��˻�������΢���˻���"+openid + "��ѯ����uin:"+uin);
						}
						if(uin != null){
							ret = daService.update("update user_info_tb set balance =balance+? where id=?  ",
									new Object[] { wx_total, uin });//�ȳ�ֵ
						}
						
					}
					logger.error("uin:"+uin+",orderid:"+orderId+",type:"+type+",money:"+money+",wx_total:"+wx_total);
					if(uin != null){
						try {
							//begin by zhangq 2017-5-24 �¿�����
							if(type == 11){
								// д���û��˻���--�¿�����
								daService.update("insert into user_account_tb(uin,amount,type,create_time,remark,pay_type,orderid) values(?,?,?,?,?,?,?)",
												new Object[] {uin,wx_total,0,System.currentTimeMillis() / 1000 - 2,"�������¿����ѳ�ֵ����"+CustomDefind.UNIONID+"����"+out_trade_no, 9, orderId });
							}else{
								// д���û��˻���--��ֵ
								daService.update("insert into user_account_tb(uin,amount,type,create_time,remark,pay_type,orderid) values(?,?,?,?,?,?,?)",
												new Object[] {uin,wx_total,0,System.currentTimeMillis() / 1000 - 2,"΢�Ź��ںų�ֵ", 9, orderId });
								
								// �۳�΢��������
								daService.update("insert into tingchebao_account_tb(amount,type,create_time,remark,utype,orderid) values(?,?,?,?,?,?)",
												new Object[] {wx_total * 0.006,1,System.currentTimeMillis() / 1000 - 2,"΢�Ź��ںų�ֵ������", 5, orderId });
							}
							//end by zhangq 2017-5-24 �¿�����
							//д�û�֧���˺���Ϣ��
							logService.insertUserAccountMesg(2, uin, openid);
						} catch (Exception e) {
							// TODO: handle exception
						}
						
						if(ret == 1){
							logger.error("΢�Ź��ںų�ֵ�ɹ���д��΢�Ź��ںų�ֵ��־ ....uin:"+uin+",orderid:"+orderId+",type:"+type);
							daService.update("insert into alipay_log (notify_no,create_time,uin,money,wxp_orderid,orderid) values(?,?,?,?,?,?)",
											new Object[] { out_trade_no,System.currentTimeMillis() / 1000,uin, wx_total, wxp_orderid, orderId });
							
							try {//�����Ƽ��߼�
								Long rcount = daService.getLong(
										"select count(*) from recommend_tb where (nid=? or openid=?) and type=?",
										new Object[] { uin, openid, 0 });
								if(rcount == 0){
									logger.error("΢�Ź��ں�֧�������û�û�б��Ƽ���:openid:"+openid+",uin:"+uin+",uid:"+uid);
								}else{
									logger.error("΢�Ź��ں�֧�������û��Ѿ����Ƽ���������д�Ƽ���¼,openid:"+openid+",uin:"+uin+",uid:"+uid);
								}
								if(wx_total >= 1){//ֱ����Դ�ͽ��㶩����֧��������1Ԫ
									if(rcount == 0 && (type == 0 || type==5 ) && uid != -1){
										logger.error("���û�û�б��Ƽ���������֧�������ڵ���1Ԫ,д�Ƽ���¼openid:"+openid+",uin:"+uin);
										Map usrMap =daService.getMap("select recommendquota from user_info_Tb where id =? ", new Object[]{uid});
										Double recommendquota = 5.00;
										if(usrMap!=null){
											recommendquota = StringUtils.formatDouble(Double.parseDouble(usrMap.get("recommendquota")+""));
											logger.error("���շ�Ա���Ƽ�������ǣ�"+recommendquota);
										}
										int r = daService.update("insert into recommend_tb(pid,nid,type,state,create_time,openid,money) values(?,?,?,?,?,?,?) ",
												new Object[] { uid, uin, 0, 0, System.currentTimeMillis() / 1000 , openid, recommendquota});
									}
								}else{
									logger.error("���û��������Ƽ����򣬲������Ƽ�openid:"+openid+",uin:"+uin);
								}
								if(bind_flag == 1 && (type == 0 || type==5 || type == 4)){
									logger.error("�Ѱ󶨣����̴����Ƽ��ɹ��߼�openid:"+openid+",uin:"+uin);
									//publicMethods.handleWxRecommendCode(uin, 0L);//2016-09-07
								}else{
									logger.error("δ�󶨣�openid:"+openid+",uin:"+uin);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
							
							int ptype = 9;//΢�Ź��ں�
							if(money > wx_total){
								ptype=10;//���+΢�Ź��ں�
							}
							if(type == 0){//ֱ��ͣ���� 
								Long comId = daService.getLong("select comid from user_info_tb where id=? ", new Object[]{uid});
								String carNumber = publicMethods.getCarNumber(uin);
								ret = publicMethods.epay(comId,money, uin, uid, ticketId, carNumber,ptype,bind_flag,orderId,wxp_orderid);
								logger.error(">>>>����ֱ��֧�����շ�Ա:" + ret);
								int _state = -1;// Ĭ��֧�����ɹ�
								if (ret == 5) {
									_state = 2;
									ret = 1;
									// ��֧���ɹ���Ϣ���շ�Ա
									logService.insertParkUserMessage(comId,_state,uid,carNumber,orderId,money, "", 0,ntime,ntime+10, 0, null);
									// ����Ϣ������
									logService.insertMessage(comId,_state, uin, carNumber,orderId,money,"", 0, ntime,ntime+10, 0);
								}else if(ret == -7){
									logService.insertUserMesg(0, uin, "΢�Ź��ں�ֱ��ͣ����"+wx_total+"Ԫ��΢�Ź��ں�֧��ʧ�ܣ���ֵ����ѽ�������˻�", "֧��ʧ������");
								}
							}else if(type==1){//��������
								logger.error(">>>>>weixin pay :type:�����ѣ�������"+uin+"������ID��"+orderId+",ͣ��ȯ��"+ticketId);
								String carNumber = publicMethods.getCarNumber(uin);
								ret = publicMethods.payCarStopOrder(orderId, money, ticketId);
								logger.error(">>>>>weixin pay :type:������,ret="+ret);
								if(ret==5){
									Integer payType = 2;
									String payctype = "΢��";
									if(money>wx_total){
										payctype = "΢��+���";
										payType=3;
									}
									ret = daService.update("update carstop_order_tb set state=? ,pay_type=?, amount=? where id =? ", new Object[]{8,payType,money,orderId});
									logger.error(">>>>>weixin pay :type:������,���¶���:"+ret+" total:"+money+",wx_total:"+wx_total+",paytype:"+payType);
									// ��֧���ɹ���Ϣ���շ�Ա
									logService.insertParkUserMessage(-1L,2,uid,carNumber,-1L,money,payctype, 0,ntime,ntime+10, 0, null);
									logger.error(">>>>>weixin pay :type:������,�ѷ���Ϣ���շ�Ա");
								}
							}else if(type == 2){//��ֵ�������¿�
								logger.error("buy product by wxp>>>uin:"+uin+",starttime:"+starttime+",months:"+months+",prodid:"+prodid);
								if(prodid > 0 && !starttime.equals("") && months > 0){
									Map productMap = daService.getMap("select * from product_package_tb where id=? and state=? and remain_number>? ",
											new Object[] { prodid, 0, 0 });
									if (productMap != null) {
										String cname = (String) daService.getObject("select company_name from com_info_tb where id=?",
														new Object[] { productMap.get("comid") },String.class);
										// д��ֵ��־
										Double total= commonMethods.getProdSum(prodid, months);
										if (total > wx_total)
											ptype = 10;
										int r = daService.update("insert into money_record_tb(comid,create_time,amount,uin,type,pay_type,remark) values (?,?,?,?,?,?,?)",
														new Object[] {-1L,System.currentTimeMillis() / 1000,Double.valueOf(total),uin, ZLDType.MONEY_RECARGE,ptype,productMap.get("p_name")+ "��ֵ - " + cname });
										logger.error("buy product by wxp>>>uin:"+uin+",r:"+r);
										r = publicMethods.buyProducts(uin, productMap, months, starttime,"", ptype);
										logger.error("buy product by wxp>>>uin:"+uin+",prodid:"+prodid+",r:"+r);
										// ���û�������������Ϣ
										logService.insertMessage(-1L, 1, uin, "", 0L,Double.valueOf(total), total + "Ԫ��ֵ������"+ productMap.get("p_name") + "�ɹ�",0, 0L, 0L, 2);
									}
								}
							}else if(type == 3){//����ֵ
								logger.error("����ֵ...");
								publicMethods.syncUserToBolink(uin);
								daService.update("insert into money_record_tb(comid,create_time,amount,uin,type,pay_type,remark) values (?,?,?,?,?,?,?)",
												new Object[] {-1L,System.currentTimeMillis() / 1000,wx_total, uin,ZLDType.MONEY_RECARGE, 9,"��ֵ" });
								
								Integer isAuth = (Integer)userMap.get("is_auth");
								
								String remark = "���������˻���";
								String remark_color = "#000000";
								String url = "http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/wxpaccount.do?action=balance&openid="+openid;
								Long bid=0L;
								/*if((wx_total==100) && isAuth==1){//��ֵ100�������
									//�������
									logger.error("��֤�����û������ҳ�ֵ100Ԫuin:"+uin+",wx_total:"+wx_total);
									String sql = "insert into order_ticket_tb (id,uin,order_id,money,bnum,ctime,exptime,bwords,type) values(?,?,?,?,?,?,?,?,?)";
									Long ctime = System.currentTimeMillis()/1000;
									Long exptime = ctime + 24*60*60;
									bid =daService.getkey("seq_order_ticket_tb");
									Object []values = new Object[]{bid,uin,-1,100,25,ctime,exptime,"����ͣ������ֵ��100Ԫ�����100Ԫͣ��ȯ����������25��С��飬�ֿ��У�������",4};
									int r  = daService.update(sql,values);
									logger.error("���������uin:"+uin+",r:"+r+",bid:"+bid);
									if(r!=1)
										bid=0L;
									logger.error("����"+uin+"΢�Ź��ںų�ֵ��100Ԫ�������25/100������"+r);
									if(bid > 0){
										remark = "��ϲ�����25����100Ԫͣ��ȯ������������ɣ�";
										remark_color = "#FF0000";
										url = "http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/wxpublic.do?action=balancepayinfo&openid="+openid+"&money="+wx_total+"&bonusid="+bid+"&bonus_type=0&notice_type=3";
									}
								}*/
								try {
									logger.error(">>>>>>>>>>>ͣ����΢�Ź��ں��˻���ֵ����ֵ�ɹ����û�΢������Ϣ,uin:"+uin+",openid:"+openid);
									
									Map<String, String> baseinfo = new HashMap<String, String>();
									List<Map<String, String>> orderinfo = new ArrayList<Map<String,String>>();
									String first = "��ϲ������ֵ�ɹ���";
									
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
									keyword2.put("value", "ͣ�����˻���ֵ");
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
								} catch (Exception e) {
									// TODO: handle exception
								}
								logger.error("��ֵ�ɹ���д���ֵ��־ ....");
								// ���û�������������Ϣ
								logService.insertMessage(-1L, 1, uin, "", 0L,wx_total, wx_total + "Ԫ��ֵ�ɹ�", 0, 0L,0L, 2);
							}else if(type == 4){//NFCԤ֧��
								logger.error("Ԥ֧��...");
								Map<String, Object> orderMap = daService.getMap("select * from order_tb where id=? ", new Object[]{orderId});
								publicMethods.prepay(orderMap, money, uin, ticketId, ptype, bind_flag,wxp_orderid);
								logger.error("Ԥ֧���ɹ� ....");
							}else if(type == 5){//��ֵ�����㶩��
								logger.error(">>>>>>>>>>>>>>>΢�Ź��ںŽ��㶩��>>>>>>>>>>>orderid:"+orderId);
								Map orderMap = daService.getMap("select * from order_tb where id=? ",
										new Object[] { orderId });
								if (orderMap != null) {
									String cname = (String) daService.getObject("select company_name from com_info_tb where id=?",
													new Object[] { orderMap.get("comid") },String.class);
									daService.update("insert into money_record_tb(comid,create_time,amount,uin,type,pay_type,remark) values (?,?,?,?,?,?,?)",
													new Object[] {-1L,System.currentTimeMillis() / 1000,wx_total, uin,
															ZLDType.MONEY_RECARGE,9, "ͣ���ѳ�ֵ-" + cname });
									Integer r = publicMethods.payOrder(orderMap, money, uin, 2, ptype, ticketId, wxp_orderid, -1L, uid);
									logger.error("֧������:" + r);
									int _state = -1;// Ĭ��֧�����ɹ�
									String carNumber = publicMethods.getCarNumber(uin);
									if(orderMap.get("car_number") != null){
										carNumber = (String)orderMap.get("car_number");
									}
									if (r == 5) {
										logger.error(">>>>>>>>>>>>>>>΢�Ź��ںŽ��㶩���ɹ�>>>>>>>>>>>>orderid:"+orderId+"comid:"+orderMap.get("comid") );
										_state = 2;
										Long comId = (Long)orderMap.get("comid");
										if(comId==20130){//���͹�˾�Ĳ��Զ������������ǵĽӿڷ��Ͷ���֧��״̬
											String sr = commonMethods.sendOrderState2Baohe(Long.valueOf(orderId), 1, money,ptype);
											logger.error(">>>>>>>>>>>>>baohe sendresult:"+sr+",orderid:"+orderId+",state:1,total:"+money);
										}
										String msg = null;
										Integer orderState = (Integer)orderMap.get("state");
										String car_number = (String)orderMap.get("car_number");
										if(orderState == 2){
											logger.error("�ӵ�׷��");
											msg = car_number + "���Ӳ���" + money + "Ԫ" + "������ʱ�䣺" + TimeTools.getTime_yyyyMMdd_HHmmss(ntime * 1000) + "�������ţ�" + orderId;
										}
										// ��֧���ɹ���Ϣ���շ�Ա
										logService.insertParkUserMessage((Long) orderMap.get("comid"),_state,(Long) orderMap.get("uid"),carNumber,
												Long.valueOf(orderId),money, "", 0,
												(Long) orderMap.get("create_time"),System.currentTimeMillis()/1000, 0, msg);
										// ����Ϣ������
									}
								}
							}else if(type == 6){//�����շ�Ա
								Map orderMap = daService.getMap("select * from order_tb where id=? ",
										new Object[] { orderId });
								String carNumber = publicMethods.getCarNumber(uin);
								if(orderMap.get("car_number") != null){
									String cnum = (String)orderMap.get("car_number");
									if(!carNumber.equals("")){
										carNumber = cnum;
									}
								}
								int r = publicMethods.doparkUserReward(uin, uid, orderId, ticketId, money, ptype,bind_flag);
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
								}
							}else if(type == 7){
								logger.error("buyticket online>>>uin:"+uin+",wx_pay:"+wx_total+",money:"+money);
								int r = publicMethods.buyTickets(uin,ticketmoney,ticketnum,ptype);
								logger.error("buyticket online>>>uin:"+uin+"r:"+r);
							}else if(type==9){//��������Ԥ֧��
								logger.error("��Ҫͬ���������˻���......orderId:"+orderId);
								if(orderId!=null){
									publicMethods.prepayToBolink(uin,money,orderId);
									logger.error("prepay bolink order ,update prepay :"+daService.update("update bolink_order_tb set prepay=?,prepay_time=? where id =? ", new Object[]{money,ntime,orderId}));
								}else
									publicMethods.syncUserToBolink(uin);
								//}
							}else if(type == 11){//begin by zhangq 2017-5-24 �¿�����
								logger.error("�¿���Ա����...");
								Map<String, Object> paramMap = new HashMap<String, Object>();
								String params = jsonObject.getString("params");//���Ӳ���
								logger.info("pay monthcard, ��ȡ������Ϣparams"+params);
								paramMap.put("params", params);//֧���ص���Ϣ 
								paramMap.put("user_id", uin+"");//�������������
								paramMap.put("rand", Math.random());//����� ��������ǩ��
								paramMap.put("union_id", CustomDefind.UNIONID);//֧�����̱��  ��������ǩ��
								//ͬ��������Ϣ������
								publicMethods.syncMothPayToBolink(paramMap);
							}
							//end by zhangq 2017-5-24 �¿�����
						} else {
							logService.insertMessage(-1L, 0, uin, "", 0L,wx_total, "д���û����ʧ��", 0, 0L, 0L, 2);
						}
						logger.error("΢�Ź��ں�֧���ɹ�....����Ϣ������");
					}else {
						logger.error("��ֵ���󣬿ͻ������ڣ��ֻ���" + mobile);
						// resHandler.sendToCFT("Fail");
						logService.insertMessage(-1L, 0, uin, "", 0L,wx_total, "������Ϣ������", 0, 0L, 0L, 2);
						// return;
					}
					//��Ӧ΢�ŷ�����
			        response.getWriter().write(PayCommonUtil.setXML("SUCCESS", ""));//����΢�ŷ����������յ���Ϣ�ˣ���Ҫ�ڵ��ûص�action��
					System.out.println("-------------"+PayCommonUtil.setXML("SUCCESS", ""));
					return;
			    }else{
			    	logger.error("΢�Ź��ں�֪ͨǩ����֤ʧ��");
					logger.error("΢�Ź��ں�֧������....");
			    }
				logger.error("΢�Ź��ں�֧������....");
				response.getWriter().write(PayCommonUtil.setXML("FAIL", ""));//����΢�ŷ����������յ���Ϣ�ˣ���Ҫ�ڵ��ûص�action��
			}
		} catch (JDOMException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Override
	public void init() throws ServletException {
		ApplicationContext ctx = WebApplicationContextUtils
				.getWebApplicationContext(getServletContext());
		daService= (DataBaseService) ctx.getBean("dataBaseService");
		publicMethods = (PublicMethods)ctx.getBean("publicMethods");
		logService = (LogService) ctx.getBean("logService");
		commonMethods = (CommonMethods) ctx.getBean("commonMethods");
	}

	@Override
	public void destroy() {
		// TODO Auto-generated method stub
		super.destroy();
	}
}
