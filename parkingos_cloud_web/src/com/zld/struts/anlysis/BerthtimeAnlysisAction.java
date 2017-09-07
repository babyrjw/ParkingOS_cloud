package com.zld.struts.anlysis;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.zld.AjaxUtil;
import com.zld.impl.CommonMethods;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.ExportExcelUtil;
import com.zld.utils.JsonUtil;
import com.zld.utils.RequestUtil;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;

public class BerthtimeAnlysisAction extends Action {
	@Autowired
	private PgOnlyReadService pgOnlyReadService;
	@Autowired
	private CommonMethods commonMethods;
	@Override
	public ActionForward execute(ActionMapping mapping,ActionForm form,HttpServletRequest request,HttpServletResponse response) throws Exception{
		String action = RequestUtil.processParams(request, "action");
		Long uin = (Long)request.getSession().getAttribute("loginuin");//登录的用户id
		request.setAttribute("authid", request.getParameter("authid"));
		Long cityid = (Long)request.getSession().getAttribute("cityid");
		Long groupid = (Long)request.getSession().getAttribute("groupid");
		Long comid=(Long)request.getSession().getAttribute("comid");
		Integer pageNum = RequestUtil.getInteger(request, "page", 1);
		Integer pageSize = RequestUtil.getInteger(request, "rp", 20);
		if(uin == null){
			response.sendRedirect("login.do");
			return null;
		}
		
		if(cityid == null) cityid = -1L;
		if(groupid == null) groupid = -1L;
		
		if(action.equals("")){
			//SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
			//request.setAttribute("btime", df2.format(System.currentTimeMillis()));
			//request.setAttribute("etime",  df2.format(System.currentTimeMillis()));
			SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			Long today = TimeTools.getToDayBeginTime();
			request.setAttribute("btime", df2.format(today * 1000 ));
			request.setAttribute("etime",  df2.format(today * 1000 + 24 * 60 * 60 * 1000));
			return mapping.findForward("list");
		}else if(action.equals("query")){
			SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
			String nowtime= df2.format(System.currentTimeMillis());
			String sql = "select c.comid,c.company_name,c.total_time,c.stay_time,round((c.stay_time/(c.stay_time+c.total_time))*100,2) as percent,round(c.stay_time/((c.total_time)/12),2) as berthavghour,round ((c.stay_time/count),2) caravghour from (select a.comid,b.company_name,count(a.id ) as count,b.parking_total*12 as total_time,sum((a.end_time-a.create_time)/60/60) as " +
					"stay_time from order_tb as a left join com_info_tb as b on a.comid=b.id where a.end_time between ? and ? group by a.comid,b.company_name,total_time) as c  where ";
//			String sql = "	select round(c.payment/(c.payment+c.nopayment)*100,2) as percent ,c.company_name as company_name,c.comid as comid ,c.nopayment as nopayment ,c.payment as payment  from ( select sum( case when a.state=1 then a.total  else 0 end) " +
//			" as payment,sum( case when a.state=0 then a.total  else 0 end)   as nopayment,comid ,company_name" +
//			" from no_payment_tb  as a left join com_info_tb as b on a.comid=b.id  where a.create_time < ? group by comid ,company_name  ) as c  where ";
			String fieldsstr = RequestUtil.processParams(request, "fieldsstr");
			List<Map<String, Object>> list = null;
			int count = 0;
			String btime = RequestUtil.processParams(request, "btime");
			String etime = RequestUtil.processParams(request, "etime");
			if(btime.equals(""))
				btime = nowtime;
			if(etime.equals("")){
				etime = nowtime;
			}
			Long s =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(btime+" 00:00:00");
			Long e =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(etime+" 00:00:00");
			List<Object> params = new ArrayList<Object>();
			params.add(s);
			params.add(e);
			List<Object> parks = null;
			if(cityid > 0){
				parks = commonMethods.getparks(cityid);
			}else if(groupid > 0){
				parks = commonMethods.getParks(groupid);
			}else if(comid>0)
			{
				params.add(comid);
			}
			if(parks != null && !parks.isEmpty()){
				String preParams  ="";
				for(Object parkid : parks){
					if(preParams.equals(""))
						preParams ="?";
					else
						preParams += ",?";
				}
				sql += " c.comid in ("+preParams+") ";
				params.addAll(parks);
				list = pgOnlyReadService.getAllMap(sql,params);
			  
				if(list != null && !list.isEmpty()){
					count = list.size();
				}
			}
			
			     
			//setList(list);
			String json = JsonUtil.Map2Json(list,1,count, fieldsstr,"comid");
			
			//json = StringUtils.createJson(list);
			AjaxUtil.ajaxOutput(response, json);
		}else if(action.equals("export")){
			SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
			String nowtime= df2.format(System.currentTimeMillis());
			String sql = "select c.comid,c.company_name,c.total_time,c.stay_time,round((c.stay_time/(c.stay_time+c.total_time))*100,2) as percent,round(c.stay_time/((c.total_time)/12),2) as berthavghour,round ((c.stay_time/count),2) caravghour from (select a.comid,b.company_name,count(a.id ) as count,b.parking_total*12 as total_time,sum((a.end_time-a.create_time)/60/60) as " +
					"stay_time from order_tb as a left join com_info_tb as b on a.comid=b.id where a.end_time between ? and ? group by a.comid,b.company_name,total_time) as c  where ";
//			String sql = "	select round(c.payment/(c.payment+c.nopayment)*100,2) as percent ,c.company_name as company_name,c.comid as comid ,c.nopayment as nopayment ,c.payment as payment  from ( select sum( case when a.state=1 then a.total  else 0 end) " +
//			" as payment,sum( case when a.state=0 then a.total  else 0 end)   as nopayment,comid ,company_name" +
//			" from no_payment_tb  as a left join com_info_tb as b on a.comid=b.id  where a.create_time < ? group by comid ,company_name  ) as c  where ";
			String fieldsstr = RequestUtil.processParams(request, "fieldsstr");
			List<Map<String, Object>> list = null;
			int count = 0;
			String btime = RequestUtil.processParams(request, "btime");
			String etime = RequestUtil.processParams(request, "etime");
			if(btime.equals(""))
				btime = nowtime;
			if(etime.equals("")){
				etime = nowtime;
			}
			Long s =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(btime+" 00:00:00");
			Long e =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(etime+" 00:00:00");
			List<Object> params = new ArrayList<Object>();
			params.add(s);
			params.add(e);
			List<Object> parks = null;
			if(cityid > 0){
				parks = commonMethods.getparks(cityid);
			}else if(groupid > 0){
				parks = commonMethods.getParks(groupid);
			}else if(comid>0)
			{
				params.add(comid);
			}
			if(parks != null && !parks.isEmpty()){
				String preParams  ="";
				for(Object parkid : parks){
					if(preParams.equals(""))
						preParams ="?";
					else
						preParams += ",?";
				}
				sql += " c.comid in ("+preParams+") ";
				params.addAll(parks);
				list = pgOnlyReadService.getAllMap(sql,params);
			  
				if(list != null && !list.isEmpty()){
					count = list.size();
				}
			}
			
			export(response, list,btime,btime);
			     
			//setList(list);
			//String json = JsonUtil.Map2Json(list,1,count, fieldsstr,"comid");
			
			//json = StringUtils.createJson(list);
			//AjaxUtil.ajaxOutput(response, json);
		}
		else if(action.equals("echarts")){
			SimpleDateFormat df2 = new SimpleDateFormat("yyyy-MM-dd");
			String nowtime= df2.format(System.currentTimeMillis());
			String sql = "select c.comid,c.company_name,c.total_time,c.stay_time,round((c.stay_time/(c.stay_time+c.total_time))*100,2) as percent from (select a.comid,b.company_name,b.parking_total*12 as total_time,sum((a.end_time-a.create_time)/60/60) as " +
					"stay_time from order_tb as a left join com_info_tb as b on a.comid=b.id where a.end_time between ? and ? group by a.comid,b.company_name,total_time) as c  where ";
			String fieldsstr = RequestUtil.processParams(request, "fieldsstr");
			List<Map<String, Object>> list = null;
			
			int count = 0;
			String btime = RequestUtil.processParams(request, "btime");
			//String etime = RequestUtil.processParams(request, "etime");
			String operate = RequestUtil.getString(request, "operate");
			if(btime.equals(""))
				btime = nowtime;
			//if(etime.equals(""))
			//	etime = nowtime;
			Long s =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(btime+" 00:00:00");
			Long e =  TimeTools.getLongMilliSecondFrom_HHMMDDHHmmss(btime+" 23:59:59");
			List<Object> params = new ArrayList<Object>();
			params.add(s);
			params.add(e);
			List<Object> parks = null;
			if(cityid > 0){
				parks = commonMethods.getparks(cityid);
			}else if(groupid > 0){
				parks = commonMethods.getParks(groupid);
			}else if(comid>0){
				params.add(comid);
			}
			if(parks != null && !parks.isEmpty()){
				String preParams  ="";
				for(Object parkid : parks){
					if(preParams.equals(""))
						preParams ="?";
					else
						preParams += ",?";
				}
				sql += " c.comid in ("+preParams+") ";
				params.addAll(parks);
				list = pgOnlyReadService.getAllMap(sql,params);
				if(list != null && !list.isEmpty()){
					count = list.size();
				}
			}
			
			String json = StringUtils.createJson(list);
			//AjaxUtil.ajaxOutput(response, json);
			if(operate.equals("")){
				request.setAttribute("btime", df2.format(System.currentTimeMillis()));
	
				//request.setAttribute("etime",  df2.format(System.currentTimeMillis()));
				request.setAttribute("json", json);
				return mapping.findForward("icon");
			}else {
				AjaxUtil.ajaxOutput(response, json);
			}
		}
		
		return null;
	}
	
	private void export(HttpServletResponse response, List<Map<String, Object>> list, String btime, String etime){
		try {
			if(list != null && !list.isEmpty()){
				String heards[] = new String[]{"车场编号","停车场","总可停时长","停车时长","泊位平均停车时长","车辆平均停车时长","停车占比"};
				List<List<String>> bodyList = new ArrayList<List<String>>();
				for(Map<String, Object> map : list){
					List<String> valueList = new ArrayList<String>();
					valueList.add(map.get("comid") + "");
					valueList.add(map.get("company_name") + "");
					valueList.add(map.get("total_time") + "");
					valueList.add(map.get("stay_time") + "");
					valueList.add(map.get("percent") + "");
					valueList.add(map.get("berthavghour") + "");
					valueList.add(map.get("caravghour") + "");
					
					bodyList.add(valueList);
				}
				String fname = "停车时长分析报表" + btime + "至" + etime;
				java.io.OutputStream os = response.getOutputStream();
				response.reset();
				response.setHeader("Content-disposition", "attachment; filename="
						+ StringUtils.encodingFileName(fname) + ".xls");
				ExportExcelUtil importExcel = new ExportExcelUtil("停车时长分析报表",
						heards, bodyList);
				Map<String, String> headInfoMap=new HashMap<String, String>();
				headInfoMap.put("length", heards.length - 1 + "");
				headInfoMap.put("content", fname);
				importExcel.headInfo = headInfoMap;
				importExcel.createExcelFile(os);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
