package com.zld.struts.request;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.zld.AjaxUtil;
import com.zld.impl.PublicMethods;
import com.zld.service.DataBaseService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.RequestUtil;
import com.zld.utils.StringUtils;
//Localization
public class LocalizationAction extends Action{
	@Autowired
	private DataBaseService daService;
	@Autowired
	private PgOnlyReadService pgOnlyReadService;
	@Autowired
	private PublicMethods publicMethods;
	private Logger logger = Logger.getLogger(LocalizationAction.class);
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String token =RequestUtil.processParams(request, "token");
		String action =RequestUtil.processParams(request, "action");
		Map<String,Object> infoMap = new HashMap<String, Object>();
		long comid =-1;
		Long uin = null;
		if(token.equals("")){
			infoMap.put("info", "no token");
		}else {
			Map comMap = pgOnlyReadService.getPojo("select * from user_session_tb where token=?", new Object[]{token});
			if(comMap!=null&&comMap.get("comid")!=null){
				comid=Long.parseLong(comMap.get("comid")+"");
				uin =(Long) comMap.get("uin");
			}else {
				infoMap.put("info", "token is invalid");
			}
		}
		logger.error("action="+action+",uin="+uin);
		if("synchroTime".equals(action)){//ͬ��ʱ��
			AjaxUtil.ajaxOutput(response,System.currentTimeMillis()+"");
		}else if("synchroPrice".equals(action)){//ͬ���۸�ͳ�����С�Ƽ۵�λ
			List priret = daService.getAll("select * from price_tb where comid=?", new Object []{comid});
			List comret = daService.getAll("select * from com_info_tb where id=?", new Object []{comid});
			String ret = "{\"price_tb\":"+StringUtils.createJson(priret)+",\"com_info_tb\":"+StringUtils.createJson(comret)+"}";
//			priret.addAll(comret);
//			String ret = StringUtils.createJson(priret);
			logger.error(ret);
			AjaxUtil.ajaxOutput(response,ret);
			return null;
		}else if("synchroVip".equals(action)){//ͬ���¿�
//			String maxid = RequestUtil.getString(request, "maxid");
			List list = daService.getAll("select ci.car_number,c.e_time,c.uin from product_package_tb p,carower_product c ,user_info_tb u ,car_info_tb ci " +
		"where c.pid=p.id and p.comid=? and u.id=c.uin and ci.uin = c.uin and c.e_time>?", new Object[]{comid,System.currentTimeMillis()/1000});
			String ret = StringUtils.createJson(list);
			logger.error(ret);
			AjaxUtil.ajaxOutput(response,ret);
			return null;
		}else if("synchroNFC".equals(action)){//ͬ���۸�ͳ�����С�Ƽ۵�λ
			String maxid = RequestUtil.getString(request, "maxid");
			List list = daService.getAll("select * from com_nfc_tb where id >? or update_time > create_time", new Object[]{maxid});
			String ret = StringUtils.createJson(list);
			logger.error(ret);
			AjaxUtil.ajaxOutput(response,ret);
			return null;
		}else if("firstDownloadOrder".equals(action)){//�ͻ��˵�½ʱͬ������  ���ص���δ���㶩���������
			String sql = "select * from order_tb o where o.comid=? and o.state=? order by o.create_time asc ";//order by o.end_time desc
			List result = daService.getAll(sql, new Object[]{comid,0});
			Long maxid = daService.getLong("select max(id) from order_tb", null);
			String ret = "{\"orders\":"+StringUtils.createJson(result)+",\"maxid\":"+maxid+"}";
			AjaxUtil.ajaxOutput(response,ret);
			return null;
		}else if("mergeOrder".equals(action)){//�ϲ�����
			//������Ҫ�ϲ��Ķ���
			//��ѯ������������create_time<end_time and car_number=? and state=0��
			//�еĻ� ���㶩��  update
			//û�еĻ��´κϲ������ύ���Ժϲ�
//			String orders = RequestUtil.getString(request, "orders");//��Ҫ�ϲ��Ķ���
//			long uid = RequestUtil.getLong(request, "uid",-1L);
//			JSONObject jsonObject = JSONObject.fromObject(orders);
//			JSONArray ja = jsonObject.getJSONArray("data");
//			String result ="";
//			for (int i = 0; i < ja.size(); i++) {////������Ҫ�ϲ��Ķ���
//				JSONObject jo =  ja.getJSONObject(i);
//				//��ѯ������������comid=? create_time<? and car_number=? and state=0��
//				Long end = jo.getLong("end_time");
//				Map order = daService.getPojo("select * from order_tb where comid=? create_time<? and car_number=? and state=?", 
//						new Object []{comid,end,jo.getString("car_number"),0});
//				if(order!=null){//update user_info_tb set online_flag=? ,logon_time=? where id=?
//					//����۸�  //�еĻ� ���㶩��  update
//					Integer pid = jo.getInt("pid");
//					Long start = (Long)order.get("create_time");
//					String statol ;
//					if(pid>-1){
//						statol = publicMethods.getCustomPrice(start, end, pid);
//					}else {
//						statol = publicMethods.getPrice(start, end, comid, jo.getInt("car_type"));	
//					}
//					int ret = daService.update("update order_tb set end_time=?,uid=?,total=?,stotal=?,state=1 where id=?", 
//							new Object []{jo.getLong("end_time"),uid,jo.getDouble("total"),Double.parseDouble(statol),(Long)order.get("id")});
//					//�ɹ�����߿ͻ���ɾ�����ݲ���֪ͨ�������շ�Ա�������ٲ���
//					
//					
//				}
//				
//				//û�еĻ��´κϲ������ύ���Ժϲ�
//				 
//			}
//			
			
		}else if("synchroOrder".equals(action)){//ͬ������
//			String ids = RequestUtil.getString(request, "ids");//��Ҫ��ѯ�Ƿ�����id
			Long id = Long.parseLong(RequestUtil.getString(request, "maxid"));
			Long cid = comid;
			String orders = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "orders"));//��Ҫ�ϴ��Ķ���.
			logger.error("sync order maxid:"+id+",order:"+orders);
			StringBuffer ret = new StringBuffer("");
			String querysql = "select * from order_tb o where o.comid=? and o.state=? and id>? order by o.create_time asc ";//order by o.end_time desc
			List result = daService.getAll(querysql, new Object[]{cid,0,id});
			long maxid = daService.getLong("select max(id) from order_tb", null);
			ret.append("{\"orders\":"+StringUtils.createJson(result));//��Ҫͬ���Ķ����������շ�Ա���ɵģ�
			JSONObject jsonObject = JSONObject.fromObject(orders);//��Ҫ�ϴ��Ķ����������������ɲ�����ģ���ȥ���㶩��������ͬ���������󱾵ؽ���Ķ���
			JSONArray ja = jsonObject.getJSONArray("data");
			String relation = ",\"relation\":[";
			String delOrderIds = ",\"delOrderIds\":\"";
			 for (int i = 0; i < ja.size(); i++) {//������Ҫ�ϴ��Ķ�������id��Ӧ��ϵ
				 JSONObject jo =  ja.getJSONObject(i);
				 boolean flag = false;//true������Ҫupdate,false������Ҫinsert
				 try {
					 long j = Long.parseLong(jo.getLong("id")+"");
				 } catch (Exception e) {
					flag = true;
				 }
				 if(flag){//��Ҫִ�в���ļ�¼
					 Long nextid = daService.getLong(
								"SELECT nextval('seq_order_tb'::REGCLASS) AS newid", null);
//					 state,end_time,auto_pay,pay_type,nfc_uuid" +
//				 		",c_type,uid,car_number,imei,pid,car_type,pre_state,in_passid,out_passid)"
					 StringBuffer insertsql = new StringBuffer("insert into order_tb(id,comid,uin,");//order by o.end_time desc
					 StringBuffer valuesql = new StringBuffer("?,?,?,?,?,?,?,?,");
					 ArrayList list = new ArrayList();
					 list.add(nextid);
					 list.add(jo.getLong("comid"));
					 list.add(jo.getLong("uin"));
					 Long createtime = null;
					 String carnumber = null;
					 if(!"null".equals(jo.getString("create_time"))){
						 valuesql.append("?,");
						 insertsql.append("create_time,");
						 createtime = jo.getLong("create_time");
						 list.add(createtime);
					 }
					 if(!"null".equals(jo.getString("car_number"))){
						 valuesql.append("?,");
						 insertsql.append("car_number,");
						 carnumber = jo.getString("car_number");
						 list.add(carnumber);
					 }
					 if(createtime!=null&&carnumber!=null){
						Map count =  daService.getMap("select id from order_tb where car_number=? and create_time = ? and state = ?", new Object[]{carnumber,createtime,0});
						if(count!=null&& count.size()>0){//������Ϸ������ͱ����ϴ��Ķ���ʱ�䳵����ͬ�򱾵�ɾ��
							delOrderIds+=jo.getString("id")+",";
							continue;
						}
					 }
					 if(!"null".equals(jo.getString("total"))&&!"�۸�δ֪".equals(jo.getString("total"))){
						 valuesql.append("?,");
						 insertsql.append("total,");
						 list.add(jo.getDouble("total"));
					 }
					 insertsql.append("state,");
					 list.add(jo.getLong("state"));
					 if(!"null".equals(jo.getString("end_time"))&&!"0".equals(jo.getString("end_time"))){
						 valuesql.append("?,");
						 insertsql.append("end_time,");
						 list.add(jo.getLong("end_time"));
					 }
					 insertsql.append("auto_pay,");
					 list.add(jo.getLong("auto_pay"));
					 insertsql.append("pay_type,");
					 list.add(jo.getLong("pay_type"));
					 if(!"null".equals(jo.getString("nfc_uuid"))){
						 valuesql.append("?,");
						 insertsql.append("nfc_uuid,");
						 list.add(jo.getString("nfc_uuid"));
					 }
					 insertsql.append("c_type,");
					 list.add(jo.getLong("c_type"));
					 insertsql.append("uid,");
					 list.add(jo.getLong("uid"));
					 if(!"null".equals(jo.getString("imei"))){
						 insertsql.append("imei,");
						 valuesql.append("?,");
						 list.add(jo.getString("imei"));
					 }
					 insertsql.append("pid,");
					 insertsql.append("car_type,");
					 insertsql.append("pre_state,");
					 insertsql.append("in_passid,");
					 valuesql.append("?,?,?,?,?,?");
					 insertsql.append("out_passid,type) values ("+valuesql+")");
					
					 list.add(jo.getLong("pid"));
					 list.add(jo.getLong("car_type"));
					 list.add(jo.getLong("pre_state"));
					 list.add(jo.getString("in_passid").equals("null")?-1:jo.getLong("in_passid"));
					 list.add(jo.getString("out_passid").equals("null")?-1:jo.getLong("out_passid"));
					 list.add(0);//type   ���ػ�����
					 int insert = daService.update(insertsql.toString(), list.toArray());
					 logger.error("�������ɶ�������ret:"+insert+",orderid:"+nextid);
					 if(insert==1){
						 relation+="{\"local\":\""+jo.getString("id")+"\",\"line\":\""+nextid+"\"},";//id��Ӧ��ϵ
						 if(jo.getLong("state")>0){
							 delOrderIds+=nextid+",";
							 if(jo.getLong("state")==1){
								 if(!"null".equals(jo.getString("total"))&&!"�۸�δ֪".equals(jo.getString("total"))&&jo.getInt("pay_type")!=8&&jo.getInt("c_type")!=5){
									 Long c = daService.getLong("select count(*) from parkuser_cash_tb where orderid = ?", new Object[]{nextid});
									 if(c!=null&&c<1){
										 int cashret = daService.update("insert into parkuser_cash_tb(uin,amount,orderid,create_time) values(?,?,?,?)", new Object[]{jo.getLong("uid"),jo.getDouble("total"),nextid,jo.getLong("end_time")});
										 logger.error("д�ֽ��շѼ�¼ret��"+cashret+",orderid:"+nextid+",amount:"+jo.getDouble("total"));
									 }else{
										 logger.error("�����ֽ��¼��orderid:"+nextid+",amount:"+jo.getDouble("total"));
									 }
								 }else{
									 logger.error("�۸��ʽ��������¿�������Ѳ�д�ֽ��¼,orderid:"+nextid);
								 }
							 }
						 }
					 }
				 }else{//���ؽ����˸��·������Ĳ���				
					 Long lineid = jo.getLong("id");//update user_info_tb set online_flag=? ,logon_time=? where id=?
					 StringBuffer insertsql = new StringBuffer("update order_tb set");//order by o.end_time desc
					 ArrayList list = new ArrayList();
					 Long createtime = null;
					 String carnumber = null;
					 if(!"null".equals(jo.getString("create_time"))){
						 createtime = jo.getLong("create_time");
						 insertsql.append(" create_time=?,");
						 list.add(createtime);
					 }
					 if(!"null".equals(jo.getString("car_number"))){
						 insertsql.append(" car_number=?,");
						 list.add(jo.getString("car_number"));
						 carnumber = jo.getString("car_number");
					 }
					 if(createtime!=null&&carnumber!=null){
						logger.error("createtime:"+createtime+",car_number:"+carnumber);
						Long count =  daService.getLong("select count(*) from order_tb where car_number=? and create_time = ? and state = ?", new Object[]{carnumber,createtime,1});
						if(count!=null&& count>0){
							delOrderIds+=lineid+",";
							logger.error("���ϸö����ѽ���,����ɾ������:"+lineid);
							continue;
						}
					 }
					 if(!"null".equals(jo.getString("total"))&&!"�۸�δ֪".equals(jo.getString("total"))){
						 insertsql.append(" total=?,");
						 list.add(jo.getDouble("total"));
					 }
					 if(!"null".equals(jo.getString("state"))){
						 insertsql.append(" state=?,");
						 list.add(jo.getLong("state"));
					 }
					 if(!"null".equals(jo.getString("end_time"))){
						 insertsql.append(" end_time=?,");
						 list.add(jo.getLong("end_time"));
					 }
					 if(!"null".equals(jo.getString("pay_type"))){
						 insertsql.append(" pay_type=?,");
						 list.add(jo.getLong("pay_type"));
					 }
					 if(!"null".equals(jo.getString("uid"))){
						 insertsql.append(" uid=?,");
						 list.add(jo.getLong("uid"));
					 }
					 insertsql.append(" out_passid=?");
					 list.add(jo.getString("out_passid").equals("null")?-1:jo.getLong("out_passid"));
					 String sql = insertsql+" where id = ?";
					 list.add(jo.getLong("id"));
					 int update = daService.update(sql, list.toArray());
					 logger.error("���ؽ��㶩������ret:"+update+",orderid:"+lineid);
					 if(update==1){
						 if(jo.getLong("state")>0){
							 delOrderIds+=jo.getLong("id")+",";
							 if(jo.getLong("state")==1){
								 if(!"null".equals(jo.getString("total"))&&!"�۸�δ֪".equals(jo.getString("total"))&&jo.getInt("pay_type")!=8&&jo.getInt("c_type")!=5){
									 Long c = daService.getLong("select count(*) from parkuser_cash_tb where orderid = ?", new Object[]{lineid});
									 if(c!=null&&c<1){
										 int r = daService.update("insert into parkuser_cash_tb(uin,amount,orderid,create_time) values(?,?,?,?)", new Object[]{jo.getLong("uid"),jo.getDouble("total"),jo.getLong("id"),jo.getLong("end_time")});
										 logger.error("д�ֽ��շѼ�¼ret:"+r+",orderid:"+lineid+",amount:"+jo.getDouble("total")+",�����ֽ��շѼ�¼ret:"+r);
									 }else{
										 logger.error("�����ֽ��¼��orderid:"+lineid+",amount:"+jo.getDouble("total"));
									 }
								 }else{
									 logger.error("�۸��ʽ��������¿�������Ѳ�д�ֽ��¼orderid:"+lineid);
								 }
							 }
						 }
					 }
				 }
//				 if(delOrderIds.length()>16)
//						ret.append(delOrderIds);
		     }
			 if(relation.length()>14)
				 ret.append(relation.substring(0, relation.length()-1)+']');
			ret.append(",\"maxid\":\""+(maxid+ja.size())+"\"");
//			if(StringUtils.isNotNull(ids)){//�����ϴ�ͬ���Ķ����Ƿ��ѽ���
//				String[] idArr = ids.split(",");
////				String delIds = "";
//				for (int i = 0; i < idArr.length; i++) {
//					Long state = daService.getLong("select o.state from order_tb o where o.comid=? and id=?  ",new Object[]{cid,Long.parseLong(idArr[i])});
//					if(state!=null&&state==1){
//						delOrderIds+=idArr[i]+",";
//					}
//				}
//			}
			if(delOrderIds.length()>16){
				ret.append(""+delOrderIds.substring(0, delOrderIds.length()-1)+"\"");
			}
			ret.append("}");
			logger.error("sync order return result��"+ret.toString());
			AjaxUtil.ajaxOutput(response,ret.toString());
			return null;
//			����:http://localhost:8080/zld/local.do?action=synchroOrder&token=?&maxid=2&ids=786835&orders={%22data%22:[{%22id%22:%22786491%22,%22
//			create_time%22:%221431431340%22,%22comid%22:%221197%22,%22uin%22:%2219614%22,%22total%22:%22null%22,%22state%22:%221%22,%22end_time
//			%22:%22null%22,%22auto_pay%22:%220%22,%22pay_type%22:%220%22,%22nfc_uuid
//			%22:%220468904A9A3D80%22,%22c_type%22:%220%22,%22uid%22:%2211802%22,%22car_number
//			%22:%22%E9%9D%92S12348%22,%22imei%22:%22359776056347380%22,%22pid%22:%22-1%22,%22car_type%22:%220%22,
//			%22pre_state%22:%220%22,%22in_passid%22:%22-1%22,%22out_passid%22:%22-1%22}]}
		}
		AjaxUtil.ajaxOutput(response,"");
		return null;
	}
//	{"orders":[],
//		"maxid":"790251","delOrderIds":"790220,790221,790222,790223,790224,790225,790226,790227,3c856c6b-3636-4448-905f-694e97277dcd," +
//				"b328342b-fd53-4e04-8058-9424f541ccbf,e0dc3805-3cf0-45d4-992e-7a74b3de13a6," +
//				"76f96608-1ab3-495b-af3a-16601d06e605,1d3552a2-4406-4b76-b5ba-20d23d3c43b9"}
	 
	 
	
}
