package com.zld.struts.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import com.zld.AjaxUtil;
import com.zld.CustomDefind;
import com.zld.impl.PublicMethods;
import com.zld.service.DataBaseService;
import com.zld.utils.Check;
import com.zld.utils.StringUtils;
import com.zld.utils.ZldUploadUtils;


/**
 * ���������ݽӿ�
 * @author Administrator
 * 20160322
 */
@Path("third")
public class ZldThirdApi {
	

	Logger logger = Logger.getLogger(ZldThirdApi.class);
	
	@POST
	@Path("/uploadorder")//����ͣ������֪ͨ
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)       
	public void uploadOrder(String params,
			@Context ServletContext context,
			@Context HttpServletResponse response)throws Exception {
		String operate = "bolink uploadorder �����볡,";
		String result ="{\"state\":\"1\",\"errmsg\":\"\"}"; 
		Map<String, String> paramMap = ZldUploadUtils.stringToMap(params);
		String sign = paramMap.get("sign");
		paramMap.remove("sign");
		String signStr= StringUtils.createLinkString(paramMap)+"key="+CustomDefind.getValue("UNIONKEY");;
		logger.error(operate+signStr);
		String _sign = StringUtils.MD5(signStr,"utf-8").toUpperCase();
		logger.error(operate+sign+":"+_sign);
		if(!sign.equals(_sign)){
			result ="{\"state\":\"0\",\"errmsg\":\"����У��ʧ��\"}"; 
		}else {
			logger.error(AjaxUtil.decodeUTF8(paramMap+""));
			ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
			DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
			String plateNumber = AjaxUtil.decodeUTF8(paramMap.get("plate_number"));
			Long uin = -1L;
			if(plateNumber!=null&&!"".equals(paramMap)){
				Map userMap = daService.getMap("select uin from car_info_tb where car_number=? ", new Object[]{plateNumber});
				if(userMap==null||userMap.isEmpty()){//��΢�������˻��и��ݳ��Ʋ�ѯ
					userMap = daService.getMap("select uin from wxp_user_tb where car_number=? ",  new Object[]{plateNumber});
				}
				if(userMap!=null)
					uin = (Long)userMap.get("uin");
			}
			logger.error("api �ϴ�������ɾ��ͬһ����ͬһ���ƶ�����"+
					daService.update("delete from bolink_order_tb where union_id=? " +
							"and park_id=? and plate_number=? and state=? ", 
							new Object[]{Long.valueOf(paramMap.get("union_id")),
							paramMap.get("park_id"),plateNumber,0}));
			String sql ="insert into bolink_order_tb (union_id,start_time,state," +
					"in_time,update_time,union_name,park_name,plate_number,order_id,uin,park_id) values(?,?,?,?,?,?,?,?,?,?,?)";
			Object[] parmas = new Object[]{
					Long.valueOf(paramMap.get("union_id")),
					Long.valueOf(paramMap.get("start_time")),
					0,Long.valueOf(paramMap.get("time_temp")),
					Long.valueOf(paramMap.get("time_temp")),
					AjaxUtil.decodeUTF8(paramMap.get("union_name")),
					AjaxUtil.decodeUTF8(paramMap.get("park_name")),
					plateNumber,
					paramMap.get("order_id"),uin,paramMap.get("park_id")};
			int ret = daService.update(sql, parmas);
			if(ret!=1){
				result ="{\"state\":\"0\",\"errmsg\":\"����д��ʧ��\"}"; 
			}else if(uin>0){
				PublicMethods publicMethods = (PublicMethods) ctx.getBean("publicMethods");
				publicMethods.syncUserToBolink(uin);
			}
		}
		logger.error(operate+result);
		AjaxUtil.ajaxOutput(response, result);
	}
	
	@POST
	@Path("/completeorder")//����ͣ����������֪ͨ
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void completeOrder(String params,
			@Context ServletContext context,
			@Context HttpServletResponse response)throws Exception {
		String operate = "bolink completeorder, ��������";
		String result ="{\"state\":\"1\",\"errmsg\":\"\"}"; 
		Map<String, String> paramMap = ZldUploadUtils.stringToMap(params);
		String sign = paramMap.get("sign");
		paramMap.remove("sign");
		String key = CustomDefind.getValue("UNIONKEY");
		String signStr= StringUtils.createLinkString(paramMap)+"key="+key;
		logger.error(operate+signStr);
		String _sign = StringUtils.MD5(signStr,"utf-8").toUpperCase();
		logger.error(operate+sign+":"+_sign);
		//end_time=1498008206&money=0.01&operate_time=1498008221&order_id=753a8002-24aa-4ec7-a9d5-35f0ae975b8d&pay_type=0
		//&pay_union_id=200001&plate_number=%E4%BA%ACT99999&rand=0.9189926304140231&time_temp=1498008221&union_id=200103
		//key=DEEFE9094535JUJF
		if(!sign.equals(_sign)){
			result ="{\"state\":\"0\",\"errmsg\":\"����У��ʧ��\"}"; 
		}else {
			logger.error(operate+AjaxUtil.decodeUTF8(paramMap+""));
			ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
			DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
			Double money = Double.valueOf(paramMap.get("money"));
			if(money==null||money<0){
				AjaxUtil.ajaxOutput(response, "{\"state\":\"0\",\"errmsg\":\"��������쳣:"+money+"\"}");
				return ;
			}
			Map orderMap = daService.getMap("select * from bolink_order_tb where union_id=? and order_id=? ",
					new Object[]{Long.valueOf(paramMap.get("union_id")),paramMap.get("order_id")});
			Long orderId = -1L;
			int isPrePay = 0;
			if(orderMap==null||orderMap.isEmpty()){
				AjaxUtil.ajaxOutput(response, "{\"state\":\"0\",\"errmsg\":\"����������\"}");
				return ;
			}else {
				orderId = (Long)orderMap.get("id");
				Integer state = (Integer)orderMap.get("state");
				if(state!=null&&state==1){
//					AjaxUtil.ajaxOutput(response, "{\"state\":\"0\",\"errmsg\":\"�����ѽ���\"}");
//					return ;	
					isPrePay=1;
				}
			}
			if(orderId==null||orderId<0){
				AjaxUtil.ajaxOutput(response, "{\"state\":\"0\",\"errmsg\":\"����������\"}");
				return ;
			}
			//0�ֽ�֧����1����֧����2����������֧����������������ڱ�ƽ̨�п��û����
			Integer payType=Integer.valueOf(paramMap.get("pay_type"));
			Integer pay_type = payType==0?0:1;
			String sql ="update  bolink_order_tb set money=?,out_time=?,end_time=?,state=?,pay_type=?,pay_union_id=? where id=? ";
			if(isPrePay==1)
				sql ="update  bolink_order_tb set money=money+?,out_time=?,end_time=?,state=?,pay_type=?,pay_union_id=? where id=? ";
			Object[] parmas = new Object[]{money,Long.valueOf(paramMap.get("operate_time")),
					Long.valueOf(paramMap.get("end_time")),1,pay_type,Long.valueOf(paramMap.get("pay_union_id")),orderId};
			int ret = daService.update(sql, parmas);
			if(ret!=1){
				result ="{\"state\":\"0\",\"errmsg\":\"���ݸ���ʧ��\"}"; 
			}else {
				if(payType==1){//�ǵ���֧������
					//�ۻ�Աͣ����
					//String plateNumber =AjaxUtil.decodeUTF8(paramMap.get("plate_number"));
					String userId = paramMap.get("user_id");
					String payUnionId = paramMap.get("pay_union_id");
					logger.error(operate+"payUnionId="+payUnionId+",payUser:"+userId+",user:"+userId+",money:"+money);
					String ownUnionId = CustomDefind.UNIONID;
					if(!payUnionId.equals(ownUnionId)){
						logger.error("����֧������ƽ̨�˻�����payUnionId :"+payUnionId+",own unionId :"+ownUnionId);
						AjaxUtil.ajaxOutput(response, "{\"state\":\"0\",\"errmsg\":\"����֧������ƽ̨�˻�����\"}" );
						return ;
					}
					Map userMap = daService.getMap("select uin from car_info_tb where car_number=? ", new Object[]{orderMap.get("plate_number")});
					boolean isBindUser = true;//�Ƿ�����ע�ᳵ��
					if(userMap==null||userMap.isEmpty()){//��΢�������˻��и��ݳ��Ʋ�ѯ
						isBindUser=false;
					}
					//Map userMap =daService.getMap("select uin from car_info_tb where car_number=? ", new Object[]{plateNumber});
					//if(userMap!=null&&!userMap.isEmpty()){
					if(Check.isLong(userId)){
						Long uin =Long.valueOf(userId);
						if(uin!=null&&uin>0){
							Long ntime = System.currentTimeMillis()/1000;
							List<Map<String, Object>> bathSql = new ArrayList<Map<String,Object>>();
							//�����˻�
							Map<String, Object> usersqlMap =new HashMap<String, Object>();
							//�����˻���ϸ
							Map<String, Object> userAccountsqlMap =new HashMap<String, Object>();
							//����������ϸ
							Map<String, Object> bolinkSqlMap =new HashMap<String, Object>();
							
							if(isBindUser){//��ʽ�û��۷�
								usersqlMap.put("sql", "update user_info_tb  set balance =balance-? where id=? ");
								usersqlMap.put("values", new Object[]{money,uin});
								bathSql.add(usersqlMap);
							}else {//΢�������û��۷�
								usersqlMap.put("sql", "update wxp_user_tb  set balance =balance-? where uin=? ");
								usersqlMap.put("values", new Object[]{money,uin});
								bathSql.add(usersqlMap);
							}
							
							userAccountsqlMap.put("sql", "insert into user_account_tb(uin,amount,type,create_time,remark,pay_type,orderid) values(?,?,?,?,?,?,?)");
							userAccountsqlMap.put("values", new Object[]{uin,money,1,ntime,"ͣ����-"+orderMap.get("union_name")+"-"+orderMap.get("park_name"),0,orderMap.get("id")});
							bathSql.add(userAccountsqlMap);
							
							bolinkSqlMap.put("sql", "insert into bolink_ccount_tb(money,ctime,orderid,giveto,uin,type) values(?,?,?,?,?,?)");
							bolinkSqlMap.put("values",  new Object[]{money, ntime,paramMap.get("order_id"), 3,uin, 1});
							bathSql.add(bolinkSqlMap);
							
							boolean b = daService.bathUpdate(bathSql);
							logger.error(operate+"�����û���"+b);
							if(!b){
								result ="{\"state\":\"0\",\"errmsg\":\"�˻������쳣\"}"; 
							}else if(uin>0){
								PublicMethods publicMethods = (PublicMethods) ctx.getBean("publicMethods");
								publicMethods.syncUserToBolink(uin);
							}
						}
					}
				}
			}
		}
		logger.error(operate+result);
		AjaxUtil.ajaxOutput(response, result);
	}
	
	
	
	/**
	 *  1���û�����ʱ���ǲ����û���������ע��Ϊĳ������ƽ̨�û�������˳��ƺ󣬳���ƽ̨ͬ��������������д˳���δ����Ķ�����������֪ͨ����ͣ���ĳ���ƽ̨���޸ĳ���Ϊ�����û�����������ʱ��Ҫ�ϴ����������������û��ͳ���ƽ̨������������������֧��ҵ��
		2�����û��޸ĳ��ơ�ɾ�����ơ���������ʱ��ͬ�����˲�����
		(1)ɾ������ʱ���������˳���δ����Ķ�����������֪ͨ����ͣ���ĳ���ƽ̨���޸Ĵ˶���Ϊ�ǲ�����������������ʱ������Ҫ�ϴ���������
		(2)��ӳ���ʱ������д˳���δ����Ķ�����������֪ͨ����ͣ���ĳ���ƽ̨���޸ĳ���Ϊ�����û�����������ʱ��Ҫ�ϴ����������������û��ͳ���ƽ̨������������������֧��ҵ��
		(3)�޸ĳ���ʱ���������ԭ����δ����Ķ�����������֪ͨ����ͣ���ĳ���ƽ̨���޸Ĵ˶���Ϊ�ǲ�����������������ʱ������Ҫ�ϴ�������������³�����δ����Ķ�����������֪ͨ����ͣ���ĳ���ƽ̨���޸ĳ���Ϊ�����û�����������ʱ��Ҫ�ϴ����������������û��ͳ���ƽ̨������������������֧��ҵ��
	 * @param params
	 * @param context
	 * @param response
	 * @throws Exception
	 */
	
	@POST
	@Path("/editorder")//���������������޸ġ�ɾ�����ƺ��¼�
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void editOrder(String params,
			@Context ServletContext context,
			@Context HttpServletResponse response)throws Exception {
		String operate = "editplatenumber, �޸ĳ��ƺ󣬸��Ķ���";
		String result ="{\"state\":\"1\",\"errmsg\":\"\"}"; 
		Map<String, String> paramMap = ZldUploadUtils.stringToMap(params);
		String sign = paramMap.get("sign");
		paramMap.remove("sign");
		String signStr= StringUtils.createLinkString(paramMap)+"key="+CustomDefind.getValue("UNIONKEY");
		logger.error(operate+signStr);
		String _sign = StringUtils.MD5(signStr,"utf-8").toUpperCase();
		logger.error(operate+sign+":"+_sign);
		if(!sign.equals(_sign)){
			result ="{\"state\":\"0\",\"errmsg\":\"����У��ʧ��\"}"; 
		}else {
			logger.error(operate+AjaxUtil.decodeUTF8(paramMap+""));
			ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
			DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
			String orderId = paramMap.get("order_id");
			String isUnionUser = paramMap.get("is_union_user");
			if(Check.isLong(orderId)){
				Long id = Long.valueOf(orderId);
				int ret = 0;
				if(id>0){
					if(isUnionUser.equals("1")){
						ret = daService.update("update order_tb set is_union_user=? where id =? ", new Object[]{1,id});
					}else {
						ret = daService.update("update order_tb set is_union_user=? where id =? ", new Object[]{0,id});
					}
				}
				if(ret!=1){
					result ="{\"state\":\"0\",\"errmsg\":\"����������\"}"; 
				}
			}
			
		}
		logger.error(operate+result);
		AjaxUtil.ajaxOutput(response, result);
	}
	
	
	
	@POST
	@Path("/getparkqr")//����ƽ̨��ѯ������ά��
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)       
	public void getParkQR(String params,
			@Context ServletContext context,
			@Context HttpServletResponse response)throws Exception {
		String operate="get park qrurl,";
		String result ="{\"state\":0,\"errmsg\":\"ȡ��ά��ʧ��\",\"codeurl\":\"\"}"; 
		Map<String, String> paramMap = ZldUploadUtils.stringToMap(params);
		String sign = paramMap.get("sign");
		paramMap.remove("sign");
		String signStr= StringUtils.createLinkString(paramMap)+"key="+CustomDefind.getValue("UNIONKEY");
		logger.error(operate+signStr);
		String _sign = StringUtils.MD5(signStr,"utf-8").toUpperCase();
		logger.error(operate+sign+":"+_sign);
		if(!sign.equals(_sign)){
			result ="{\"state\":0,\"codeurl\":\"\",\"errmsg\":\"����У��ʧ��\"}"; 
		}else {
			logger.error(AjaxUtil.decodeUTF8(paramMap+""));
			Long unionId = Check.isLong(paramMap.get("union_id"))?Long.valueOf(paramMap.get("union_id")):-1L;
			String park_id = paramMap.get("park_id");
			if(unionId>0&&park_id!=null&&!"".equals(park_id)){
				ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
				DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
				Map<String, Object> qrMap = daService.getMap("select code from qr_thirdpark_code where park_id=? and union_id=? ", new Object[]{park_id,unionId});
				if(qrMap!=null&&qrMap.get("code")!=null){
					String qrUrl = "http://s.tingchebao.com/zld/qr/c/"+qrMap.get("code");
					result ="{\"state\":1,\"codeurl\":\""+qrUrl+"\",\"errmsg\":\"\"}"; 
				}else {
					String qrCode = "B0liNk"+StringUtils.MD5("z545335B544D8372321"+park_id+unionId).substring(0,16).toUpperCase();
					int r = daService.update("insert into qr_thirdpark_code(park_id,code,union_id) values(?,?,?)",
							new Object[]{park_id,qrCode,unionId});
					logger.error(operate+"add new qr park_id:"+park_id+",unionId:"+unionId+"��qrcode:"+qrCode+",ret:"+r);
					if(r==1){
						String qrUrl = "http://s.tingchebao.com/zld/qr/c/"+qrCode;
						result ="{\"state\":1,\"codeurl\":\""+qrUrl+"\",\"errmsg\":\"\"}"; 
					}
				}
			}
		}
		logger.error(operate+result);
		AjaxUtil.ajaxOutput(response, result);
	}
	
	@POST
	@Path("/catorder")//����ƽ̨��ѯδ����������ͣ��ʵʱ���
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)       
	public void catOrder(String params,
			@Context ServletContext context,
			@Context HttpServletResponse response)throws Exception {
		String operate="bolink catorder, ��ѯ�������";
		String result ="{\"state\":0,\"money\":0.0,\"errmsg\":\"\"}"; 
		Map<String, String> paramMap = ZldUploadUtils.stringToMap(params);
		String sign = paramMap.get("sign");
		paramMap.remove("sign");
		String signStr= StringUtils.createLinkString(paramMap)+"key="+CustomDefind.getValue("UNIONKEY");
		logger.error(operate+signStr);
		String _sign = StringUtils.MD5(signStr,"utf-8").toUpperCase();
		logger.error(operate+sign+":"+_sign);
		if(!sign.equals(_sign)){
			result ="{\"state\":\"0\",\"money\":0.0,\"errmsg\":\"����У��ʧ��\"}"; 
		}else {
			logger.error(AjaxUtil.decodeUTF8(paramMap+""));
			ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
			DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
			ZldUploadUtils zldUploadUtils = new ZldUploadUtils();
			if(Check.isLong(paramMap.get("order_id"))){
				Long orderId = Long.valueOf(paramMap.get("order_id"));
				Integer delayTime = Integer.valueOf(paramMap.get("delay_time"));//��ѯ���ٷ��Ӻ�Ķ������
				Map orderMap = daService.getMap("select * from order_tb where create_time >? and id =? ",
						new Object[]{System.currentTimeMillis()/1000-2*86400,orderId});
					if(orderMap==null||orderMap.isEmpty()){
						AjaxUtil.ajaxOutput(response, "{\"state\":\"0\",\"errmsg\":\"����������\"}");
						return ;
					}else {
						Long start = (Long)orderMap.get("create_time");
						Long comId = (Long)orderMap.get("comid");
						Integer car_type = (Integer)orderMap.get("car_type");
						String price = zldUploadUtils.getPrice(start, System.currentTimeMillis()/1000+delayTime*60, 
								comId, car_type, daService);
						result ="{\"state\":\"1\",\"money\":"+price+",\"errmsg\":\"\",\"order_id\":\""+orderId+"\",\"delay_time\":"+delayTime+"}"; 
					}
			}
		}
		logger.error(operate+result);
		AjaxUtil.ajaxOutput(response, result);
	}
}