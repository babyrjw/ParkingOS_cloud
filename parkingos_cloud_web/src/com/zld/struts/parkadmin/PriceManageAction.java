package com.zld.struts.parkadmin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.zld.AjaxUtil;
import com.zld.impl.CommonMethods;
import com.zld.impl.MongoDbUtils;
import com.zld.impl.PublicMethods;
import com.zld.service.DataBaseService;
import com.zld.utils.JsonUtil;
import com.zld.utils.RequestUtil;
import com.zld.utils.SqlInfo;
import com.zld.utils.StringUtils;
/**
 * ͣ������̨����Ա��¼�󣬹����շѼ۸�
 * @author Administrator
 *
 */
public class PriceManageAction extends Action{
	
	@Autowired
	private DataBaseService daService;
	@Autowired
	private PublicMethods publicMethods;
	@Autowired
	private CommonMethods commonMethods;
	@Autowired
	private MongoDbUtils mongoDbUtils;
	
	private Logger logger = Logger.getLogger(PriceManageAction.class);

	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String action = RequestUtil.processParams(request, "action");
		Long comid = (Long)request.getSession().getAttribute("comid");
		String operater = request.getSession().getAttribute("loginuin")+"";
		Integer authId = RequestUtil.getInteger(request, "authid",-1);
		Integer isAdmin =(Integer)request.getSession().getAttribute("isadmin");
		request.setAttribute("authid", authId);
		if(operater==null){
			response.sendRedirect("login.do");
			return null;
		}
		if(comid==0)
			comid = RequestUtil.getLong(request, "comid", 0L);
		if(action.equals("")){
			request.setAttribute("comid", comid);
			Map map = daService.getMap("select * from price_assist_tb where comid = ?", new Object[]{comid});
			if(map!=null&&map.size()>0){
				request.setAttribute("assist_id", map.get("id"));
				request.setAttribute("assist_unit", map.get("assist_unit"));
				request.setAttribute("assist_price", map.get("assist_price"));
			}else{
				request.setAttribute("assist_id", -1);
				request.setAttribute("assist_unit", 0);
				request.setAttribute("assist_price",0);
			}
			return mapping.findForward("list");
		}else if(action.equals("quickquery")){
			String sql = "select * from price_tb where comid=? ";
			String countSql = "select count(*) from price_tb  where comid=? ";
			Long count = daService.getLong(countSql,new Object[]{comid});
			String fieldsstr = RequestUtil.processParams(request, "fieldsstr");
			List list = null;//daService.getPage(sql, null, 1, 20);
			Integer pageNum = RequestUtil.getInteger(request, "page", 1);
			Integer pageSize = RequestUtil.getInteger(request, "rp", 20);
			List<Object> params = new ArrayList<Object>();
			params.add(comid);
			//���ɾ����־
			params.add(0);
			if(count>0){
				try{
					list = daService.getAll(sql+ " and is_delete=? order by id desc ",params, pageNum, pageSize);
				}catch(Exception e){
					logger.error(">>>>>>>>>>>>>>>>>>>>>>>>>>��ѯ�۸��б��쳣"+"sql:"+sql+ " and is_delete=? order by id desc "+"params:"+params);
				}
				
			}
			String json = JsonUtil.Map2Json(list,pageNum,count, fieldsstr,"id");
			AjaxUtil.ajaxOutput(response, json);
			return null;
		}else if(action.equals("query")){
			String sql = "select * from price_tb where comid=?  ";
			String countSql = "select count(*) from price_tb where  comid=?  " ;
			Integer pageNum = RequestUtil.getInteger(request, "page", 1);
			Integer pageSize = RequestUtil.getInteger(request, "rp", 20);
			String fieldsstr = RequestUtil.processParams(request, "fieldsstr");
			SqlInfo base = new SqlInfo("1=1", new Object[]{comid});
			SqlInfo sqlInfo = RequestUtil.customSearch(request,"price_tb");
			Object[] values = null;
			List<Object> params = null;
			if(sqlInfo!=null){
				sqlInfo = SqlInfo.joinSqlInfo(base,sqlInfo, 2);
				countSql+=" and "+ sqlInfo.getSql();
				sql +=" and "+sqlInfo.getSql();
				values = sqlInfo.getValues();
				params = sqlInfo.getParams();
			}else {
				values = base.getValues();
				params= base.getParams();
			}
			//System.out.println(sqlInfo);
			Long count= daService.getLong(countSql, values);
			List list = null;//daService.getPage(sql, null, 1, 20);
			//���ɾ����־
			params.add(0);
			if(count>0){
				try{
					list = daService.getAll(sql + " and is_delete=? order by id desc", params, pageNum, pageSize);
				}catch(Exception e){
					logger.error(">>>>>>>>>>>>>>>>>>>>>>>>>>��ѯ�۸��б��쳣"+"sql:"+sql+ " and is_delete=? order by id desc "+"params:"+params);
				}
			}
			String json = JsonUtil.Map2Json(list,pageNum,count, fieldsstr,"id");
			AjaxUtil.ajaxOutput(response, json);
			return null;
		}else if(action.equals("create")){
			String result = createPrice(request);
			AjaxUtil.ajaxOutput(response, result);
		}else if(action.equals("createassist")){
			Integer assistunit =RequestUtil.getInteger(request, "assist_unit", 0);
			Long assistid =RequestUtil.getLong(request, "assist_id", -1L);
			Double assistprice =RequestUtil.getDouble(request, "assist_price", 0d);
			int result = 0;
			String ret = null;
			if(assistunit!=0&&assistprice!=0){
				Long count = daService.getLong("select count(*) from price_assist_tb where comid=?", new Object[]{comid});
				if(count==0){
					Long nextid = daService.getLong(
							"SELECT nextval('seq_price_assist_tb'::REGCLASS) AS newid", null);
					result = daService.update("insert into  price_assist_tb(id,comid,type,assist_unit,assist_price) values (?,?,?,?,?)", new Object[]{nextid,comid,0,assistunit,assistprice});
					if(result==1){
						ret = "�ɹ���Ӹ����۸�1��,"+assistunit+","+assistprice;
						if(publicMethods.isEtcPark(comid)){
							int re = daService.update("insert into sync_info_pool_tb(comid,table_name,table_id,create_time,operate) values(?,?,?,?,?)", new Object[]{comid,"price_assist_tb",nextid,System.currentTimeMillis()/1000,0});
							logger.error("parkadmin or admin:"+operater+" add comid:"+comid+" assist price ,add sync ret:"+re);
						}else{
							logger.error("parkadmin or admin:"+operater+" add comid:"+comid+" price");
						}
						mongoDbUtils.saveLogs(request, 0, 2, "����˳���("+comid+")�����۸�:"+nextid);
					}
				}else{
					result = daService.update("update price_assist_tb set assist_unit=?, assist_price=? where comid = ?", new Object[]{assistunit,assistprice,comid});
					if(result==1){
						ret = "�ɹ��޸ĸ����۸�,"+assistunit+","+assistprice;
						if(publicMethods.isEtcPark(comid)){
							if(assistid==-1){
								assistid = daService.getLong("select id from price_assist_tb where comid = ?", new Object[]{comid});
							}
							int re = 0;
							if(assistid!=-1)
								re = daService.update("insert into sync_info_pool_tb(comid,table_name,table_id,create_time,operate) values(?,?,?,?,?)", new Object[]{comid,"price_assist_tb",assistid,System.currentTimeMillis()/1000,1});
							logger.error("parkadmin or admin:"+operater+" add comid:"+comid+" assist price ,add sync ret:"+re);
						}else{
							logger.error("parkadmin or admin:"+operater+" add comid:"+comid+" assist price");
						}
						mongoDbUtils.saveLogs(request, 0,3, "�޸��˳���("+comid+")�����۸�:"+assistid);
					}
				}
			}else if(assistunit==0&&assistprice==0){
				Map tempMap = daService.getMap("select * from price_assist_tb where comid=?", new Object[]{comid});
				result = daService.update("delete from price_assist_tb where comid=?", new Object[]{comid});
				if(result==1){
					if(publicMethods.isEtcPark(comid)){
						int re = daService.update("insert into sync_info_pool_tb(comid,table_name,table_id,create_time,operate) values(?,?,?,?,?)", new Object[]{comid,"price_assist_tb",assistid,System.currentTimeMillis()/1000,2});
						logger.error("parkadmin or admin:"+operater+" delete comid:"+comid+" assist price ,add sync ret:"+re);
					}else{
						logger.error("parkadmin or admin:"+operater+" delete comid:"+comid+" assist price");
					}
					ret = "�ɹ�ɾ�������۸�1����,0,0";
					mongoDbUtils.saveLogs(request, 0,4, "ɾ���˳���("+comid+")�����۸�:"+tempMap);
				}else{
					ret = "û�����ø����۸�,0,0";
				}
			}else{
				ret = "���ô��󣨲�������0����,0,0";
			}
			AjaxUtil.ajaxOutput(response, ret);
		}else if(action.equals("edit")){
			Integer b_time =RequestUtil.getInteger(request, "b_time", 0);
			Integer e_time =RequestUtil.getInteger(request, "e_time", 0);
			Integer b_minute =RequestUtil.getInteger(request, "b_minute", 0);
			Integer e_minute =RequestUtil.getInteger(request, "e_minute", 0);
			Integer pay_type =RequestUtil.getInteger(request, "pay_type", 0);
			Integer unit =RequestUtil.getInteger(request, "unit", 0);
			Integer state =RequestUtil.getInteger(request, "state", 0);
			Double price =RequestUtil.getDouble(request, "price", 0d);
			Integer isSale =RequestUtil.getInteger(request, "is_sale", 0);
			Integer first_times =RequestUtil.getInteger(request, "first_times", 0);
			Double fprice =RequestUtil.getDouble(request, "fprice", 0d);
			Double total24 =RequestUtil.getDouble(request, "total24", -1d);
			Integer countless =RequestUtil.getInteger(request, "countless", 0);
			Integer isEdit =RequestUtil.getInteger(request, "isedit", 0);//�Ƿ�ɱ༭�۸�Ŀǰֻ���ռ䰴ʱ�۸���Ч,0��1�ǣ�Ĭ��0
			Integer free_time = RequestUtil.getInteger(request, "free_time", 0);//���ʱ������λ:����
			Integer fpay_type = RequestUtil.getInteger(request, "fpay_type", 0);//�����ʱ���Ʒѷ�ʽ��1:��� ��0:�շ�
			Integer car_type = RequestUtil.getInteger(request, "car_type", 0);//�����ͣ�0��ͨ�ã�1��С����2;��
			Integer isFullDayTime = RequestUtil.getInteger(request, "is_fulldaytime", 0);//�����ͣ�0��ͨ�ã�1��С����2;��
			Long id =RequestUtil.getLong(request, "id", -1l);
			String sql = "update price_tb set b_time=?,e_time=?,b_minute=?,e_minute=?,pay_type=?,unit=?,state=?,price=?," +
					"is_sale=?,first_times=?,fprice=?,countless=?,free_time=?,fpay_type=? ,isedit=?,car_type=?,is_fulldaytime=?,total24=? where id=?";
			Object [] values = new Object[]{b_time,e_time,b_minute,e_minute,pay_type,unit,state,price,isSale,first_times,fprice,countless,free_time,fpay_type,isEdit,car_type,isFullDayTime,total24,id};
			int result = daService.update(sql, values);
			if(result==1){
				if(comid==0)
					comid = daService.getLong("select comid from price_tb where id = ?", new Object[]{id});
				int r = daService.update("update price_tb set total24=? where comid = ? and car_type=? ", new Object[]{total24,comid,car_type});
				logger.error("update comid :"+comid+" total24="+total24+" result :"+r);
				if(publicMethods.isEtcPark(comid)){
					int re = daService.update("insert into sync_info_pool_tb(comid,table_name,table_id,create_time,operate) values(?,?,?,?,?)", new Object[]{comid,"price_tb",id,System.currentTimeMillis()/1000,1});
					logger.error("parkadmin or admin:"+operater+" edit comid:"+comid+" price ,add sync ret:"+re);
				}else{
					logger.error("parkadmin or admin:"+operater+" edit comid:"+comid+" price ");
				}
				mongoDbUtils.saveLogs(request, 0, 3, "�޸��˳���("+comid+")�۸�:"+id);
			}
			//SystemMemcachee.PriceMap.remove(comid);
			logger.error(comid+"�ӻ���������۸�....");
			AjaxUtil.ajaxOutput(response, result+"");
		}else if(action.equals("delete")){
			String id =RequestUtil.processParams(request, "selids");
			Map priceMap = daService.getMap("select * from price_tb where id =?", new Object[]{Long.valueOf(id)});
//			String sql = "delete from price_tb where id =?";
//			Object [] values = new Object[]{Long.valueOf(id)};
			//���ɾ������
			String sql = "update price_tb set is_delete=? where id =?";
			Object [] values = new Object[]{1,Long.valueOf(id)};
			int result = daService.update(sql, values);
			if(result==1){
				if(comid==0)
					comid = daService.getLong("select comid from price_tb where id = ?", new Object[]{Long.valueOf(id)});
				if(publicMethods.isEtcPark(comid)){
					int re = daService.update("insert into sync_info_pool_tb(comid,table_name,table_id,create_time,operate) values(?,?,?,?,?)", new Object[]{comid,"price_tb",Long.valueOf(id),System.currentTimeMillis()/1000,2});
					logger.error("parkadmin or admin:"+operater+" delete comid:"+comid+" price ,add sync ret:"+re);
				}else{
					logger.error("parkadmin or admin:"+operater+" delete comid:"+comid+" price");
				}
				mongoDbUtils.saveLogs(request, 0, 4, "ɾ���˳����۸�:"+priceMap);
			}
			AjaxUtil.ajaxOutput(response, result+"");
		}else if(action.equals("getcartypes")){
			List<Map<String, Object>> resultList = new ArrayList<Map<String, Object>>();
			List<Map<String, Object>> retList = commonMethods.getCarType(comid);
			HashMap<String, Object> hashMap = new HashMap<String, Object>();
			hashMap.put("value_name","ȫ��");
			hashMap.put("value_no", -1);
			resultList.add(hashMap);
			resultList.addAll(retList);
			String result = StringUtils.getJson(resultList);
			AjaxUtil.ajaxOutput(response, result);
		}
		return null;
	}
	
	private String createPrice(HttpServletRequest request){
		Long time = System.currentTimeMillis()/1000;
		Long comid = (Long)request.getSession().getAttribute("comid");
		if(comid == null || comid==0)
			comid = RequestUtil.getLong(request, "comid", 0L);
		Integer b_time =RequestUtil.getInteger(request, "b_time", 0);
		Integer e_time =RequestUtil.getInteger(request, "e_time", 0);
		Integer b_minute =RequestUtil.getInteger(request, "b_minute", 0);
		Integer e_minute =RequestUtil.getInteger(request, "e_minute", 0);
		Integer pay_type =RequestUtil.getInteger(request, "pay_type", 0);
		Integer unit =RequestUtil.getInteger(request, "unit", 0);
		Integer state =RequestUtil.getInteger(request, "state", 0);
		Integer isSale =RequestUtil.getInteger(request, "is_sale", 0);
		String price =RequestUtil.processParams(request, "price");
		Integer first_times =RequestUtil.getInteger(request, "first_times", 0);
		Double fprice =RequestUtil.getDouble(request, "fprice", 0d);
		Double total24 =RequestUtil.getDouble(request, "total24", -1d);
		Integer countless =RequestUtil.getInteger(request, "countless", 0);
		Integer isEdit =RequestUtil.getInteger(request, "isedit", 0);//�Ƿ�ɱ༭�۸�Ŀǰֻ���ռ䰴ʱ�۸���Ч,0��1�ǣ�Ĭ��0
		Integer free_time = RequestUtil.getInteger(request, "free_time", 0);//���ʱ������λ:����
		Integer fpay_type = RequestUtil.getInteger(request, "fpay_type", 0);//�����ʱ���Ʒѷ�ʽ��1:��� ��0:�շ�
		Integer car_type = RequestUtil.getInteger(request, "car_type", 0);//�����ͣ�0��ͨ�ã�1��С����2;��
		Integer isFullDayTime = RequestUtil.getInteger(request, "is_fulldaytime", 0);//�����ͣ�0��ͨ�ã�1��С����2;��

		String message = "";
		if(pay_type==0){
			if(b_time==0&&e_time==0){
				message="����������ʼ�ͽ���ʱ��";
			}
			if(unit==0)
				unit = 60;
		}
		if(!message.equals(""))
			return message;
		Long nextid = daService.getLong(
				"SELECT nextval('seq_price_tb'::REGCLASS) AS newid", null);
		String sql = "insert into  price_tb (id,b_time,e_time,b_minute,e_minute,create_time,price,pay_type," +
				" comid,unit,state,is_sale,first_times,fprice,free_time,fpay_type,countless,isedit,car_type,is_fulldaytime,total24) values" +
				"(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
		Object [] values = new Object[]{nextid,b_time,e_time,b_minute,e_minute,time,Double.valueOf(price),pay_type,
				comid,unit,state,isSale,first_times,fprice,free_time,fpay_type,countless,isEdit,car_type,isFullDayTime,total24};
		int result = daService.update(sql, values);
		if(result == 1){
			int r = daService.update("update price_tb set total24=? where comid = ?", new Object[]{total24,comid});
			logger.error("update comid :"+comid+" total24="+total24+" result :"+r);
			String operater = request.getSession().getAttribute("loginuin")+"";
			if(publicMethods.isEtcPark(comid)){
				int re = daService.update("insert into sync_info_pool_tb(comid,table_name,table_id,create_time,operate) values(?,?,?,?,?)", new Object[]{comid,"price_tb",nextid,System.currentTimeMillis()/1000,0});
				logger.error("parkadmin or admin:"+operater+" add comid:"+comid+" price ,add sync ret:"+re);
			}else{
				logger.error("parkadmin or admin:"+operater+" add comid:"+comid+" price");
			}
			mongoDbUtils.saveLogs(request, 0, 2, "����˳���("+comid+")�۸�:"+nextid);
		}
		return result+"";
	}
	

}