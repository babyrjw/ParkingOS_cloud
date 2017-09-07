package com.zld.struts.request;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import pay.Constants;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.zld.AjaxUtil;
import com.zld.CustomDefind;
import com.zld.easemob.main.HXHandle;
import com.zld.impl.CommonMethods;
import com.zld.impl.MemcacheUtils;
import com.zld.impl.MongoClientFactory;
import com.zld.impl.PublicMethods;
import com.zld.pojo.ParseJson;
import com.zld.service.DataBaseService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.Check;
import com.zld.utils.HttpProxy;
import com.zld.utils.RequestUtil;
import com.zld.utils.StringUtils;
import com.zld.utils.TimeTools;
import com.zld.utils.ZldDesUtils;
import com.zld.utils.ZldMap;
import com.zld.utils.ZldXMLUtils;
/**
 * ����2.0�ӿ�
 * @author Administrator
 * 20150415
 */
public class CarOwnerInterface extends Action {
	
	@Autowired
	private PgOnlyReadService onlyService;
	@Autowired
	private DataBaseService service;
	@Autowired
	private PublicMethods publicMethods; 
	@Autowired
	private MemcacheUtils memcacheUtils;
	@Autowired
	private CommonMethods commonMethods;
	private Logger logger = Logger.getLogger(CarOwnerInterface.class);

	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String action = RequestUtil.getString(request, "action");
		//System.err.println(request.getParameterMap());
		logger.error("action:"+action);
		if(action.equals("bolinkprepay")){
			publicMethods.prepayToBolink(546045L,0.1,11218L);
			return null;
		}
		if(action.equals("qrtest")){
			logger.error("֧����֧��....");
			Enumeration enumeration = request.getHeaderNames();
			while (enumeration.hasMoreElements()) {
				Object object = (Object) enumeration.nextElement();
				System.err.println(object.toString()+"="+request.getHeader(object.toString()));
			}
			response.sendRedirect("https://");
			return null;
		}
		if(action.equals("parkinfo")){
			String ret = getParkInfo(request);
			logger.error(ret);
			AjaxUtil.ajaxOutput(response, ret);
			//��ȡͣ��������   http://127.0.0.1/zld/carinter.do?action=parkinfo&comid=8689
		}else if(action.equals("getcomment")){//��ȡ����
			String result = getComments(request);
			//��ȡͣ��������   http://127.0.0.1/zld/carinter.do?action=getcomment&comid=1197&page=1&mobile=15801482643
			AjaxUtil.ajaxOutput(response,result);
		}else if(action.equals("uppark")){//�ϴ�����
			String ret = upPark(request);
			AjaxUtil.ajaxOutput(response, ret);
			//�ϴ�����   http://192.168.199.240/zld/carinter.do?action=uppark&mobile=15801482513&parkname=aaa&desc=bbb&lng=116.317514&lat=40.043024&type=0
		}else if(action.equals("preverifypark")){//������˳���
			String 	ret = doPreVerifyPark(request);
			AjaxUtil.ajaxOutput(response, ret);
			//׼����˳���   http://192.168.199.240/zld/carinter.do?action=preverifypark&mobile=15801482643&lat=40.042474&lng=116.306970
		}else if(action.equals("verifypark")){//������˳���
			String 	ret = doVerifyPark(request);
			AjaxUtil.ajaxOutput(response, ret);
			//��˳���   http://192.168.199.240/zld/carinter.do?action=verifypark&mobile=15801482643&id=&isname=&islocal=&ispay=&isresume=
		}else if(action.equals("usetickets")){//ѡ��ͣ��ȯ�����򣺿��ã����Ӵ�С������Ч�ڣ��쵽�ڵ���ǰ��
			String result = useTickets(request);
			AjaxUtil.ajaxOutput(response, result);
			//ʹ��ͣ��ȯ   http://192.168.199.240/zld/carinter.do?action=usetickets&mobile=15801482643&total=5&orderid=&uid=10700&preid=38878&utype=0
		}else if(action.equals("gettickets")){//������ͣ��ȯ
			String result = getallTickets(request);
			AjaxUtil.ajaxOutput(response, result);
			//ʹ��ͣ��ȯ   http://192.168.199.240/zld/carinter.do?action=gettickets&mobile=15801482643&page=10&type=0
		}else if(action.equals("wxaccount")){//ȡ΢��ͣ��ȯ
			String result = getWxAccount(request);
			AjaxUtil.ajaxOutput(response, result);
			//��΢���ۿ۴���ȯ http://127.0.0.1/zld/carinter.do?action=wxaccount&mobile=15801482643&uid=10700&total=15.99
		}else if(action.equals("orderdetail")){//΢��Ԥ֧����������
			doOrderDetail(request);
			request.setAttribute("title", CustomDefind.getValue("TITLE"));
			request.setAttribute("desc", CustomDefind.getValue("DESCRIPTION"));
			return mapping.findForward("orderdetail");
			//��΢���ۿ۴���ȯ http://192.168.199.239/zld/carinter.do?action=orderdetail&orderid=786119&prepay=1000
		}else if(action.equals("getwxbonus")){//��ȡ΢�ź��
			Long id = RequestUtil.getLong(request, "id", -1L);
			int ret = doPreGetWeixinBonus(request,id);
			if(ret==1){
				String location ="https://open.weixin.qq.com/connect/oauth2/authorize?appid="+Constants.WXPUBLIC_APPID+"&redirect_uri=http%3A%2F%2F"+Constants.WXPUBLIC_REDIRECTURL+"%2Fzld%2Fcarowner.do%3Faction%3Dgetobonus%26id%3D"+id+"%26operate%3Dcaibonus&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
				response.sendRedirect(location);
				return null;
			}else {
				request.setAttribute("tpic", "ticket_wx");
				request.setAttribute("mname", "��");
				request.setAttribute("isover", 0);
				return mapping.findForward("caibouns");
			}
//			return mapping.findForward(ret);
		}else if(action.equals("verifyrule")){//��˹���
			return mapping.findForward("verifyrule");
			//AjaxUtil.ajaxOutput(response, "images/verifyrule.png");
			//��˹���  http://192.168.199.240/zld/carinter.do?action=verifyrule
		}else if(action.equals("upfine")){//�ϴ������ô�
			return mapping.findForward("upfine");
			//AjaxUtil.ajaxOutput(response, "images/upfine.png");
			//�ϴ������ô�  http://192.168.199.240/zld/carinter.do?action=upfine
		}else if(action.equals("editmobile")){//����޸��ֻ�����
			String ret = editMobile(request);
			AjaxUtil.ajaxOutput(response, ret);
		}else if(action.equals("puserdetail")){
			String ret = puserDetail(request);
			AjaxUtil.ajaxOutput(response, ret);
			//�շ�Ա����  http://192.168.199.240/zld/carinter.do?action=puserdetail&uid=21654
		}else if(action.equals("pusrcomments")){
			String ret = pcommDetail(request);
			ret = ret.replace("null", "");
			AjaxUtil.ajaxOutput(response, ret);
			//�շ�Ա��������  http://192.168.199.240/zld/carinter.do?action=pusrcomments&uid=10700
		}else if(action.equals("getcarnumbs")){//������ѯ���г��ƺ�
			//http://192.168.199.240/zld/carinter.do?action=getcarnumbs&mobile=13641309140
			String cns = getCarNumbers(request);
			AjaxUtil.ajaxOutput(response, cns);
		}else if(action.equals("deletecarnum")){
			logger.error(">>>>>>>�������Ƴ���");
			String ret = delCarnumber(request);
			AjaxUtil.ajaxOutput(response, ret);
			return null;
		}else if(action.equals("upuserpic")){//�ϴ�������ʻ֤
			String result = uploadCarPics2Mongodb(request);
			logger.error(result);
			AjaxUtil.ajaxOutput(response, result);
			//http://192.168.199.240/zld/carinter.do?action=upuserpic&mobile=13641309140&carnumber=
		}else if(action.equals("getrewardquota")){
			Long uid = RequestUtil.getLong(request, "pid",-1L);
			Double rewardquota = 2.0;
			Map user = onlyService.getMap("select rewardquota from user_info_tb where id = ?", new Object[]{uid});
			if(user!=null&&user.get("rewardquota")!=null)
				rewardquota = StringUtils.formatDouble(user.get("rewardquota"));
			logger.error("�շ�Աuid:"+uid+"�Ĵ�����ȯ��ȣ�"+rewardquota);
			AjaxUtil.ajaxOutput(response, rewardquota+"");
			//�շ�Ա������ȯ��� http://localhost/zld/carinter.do?action=getrewardquota&pid=21732
		}else if(action.equals("getchargewords")){
			AjaxUtil.ajaxOutput(response, "��100���ͳ�ֵ�������֤�û�ר��");
			//��ֵ��ʾ�� http://localhost/zld/carinter.do?action=getchargewords
		}else if(action.equals("prebuyticket")){//׼������ͣ��ȯ
			String result = buyTicket(request);
			AjaxUtil.ajaxOutput(response, result);
			//����ͣ��ȯ�� http://192.168.199.240/zld/carinter.do?action=prebuyticket&mbile=18101333937
		}else if(action.equals("addorder")){//����ɨ��λ��ά�룬���ɶ���
			String ret =addOrder(request);
			AjaxUtil.ajaxOutput(response, ret);
		}else if(action.equals("getuiontickets")){//�鿴�ɺ����ͣ��ȯ
			String result = getUionTickets(request);
			AjaxUtil.ajaxOutput(response, result);
		}else if(action.equals("reqticketuion")){//����ͣ��ȯ����
			String result = reqTikcetUion(request);
			AjaxUtil.ajaxOutput(response, result);
		}else if(action.equals("ticketuioninfo")){//�������Ϣ����������Ӧ����ĳ����ͻ�����ʾ����
			AjaxUtil.ajaxOutput(response, ticketUionInfo(request));
		}else if(action.equals("preresticketuion")){//�����������Ƿ���Ч
			AjaxUtil.ajaxOutput(response, preResTicketUion(request));
		}
		else if(action.equals("resticketuion")){//��Ӧͣ��ȯ����
			String result = resTikcetUion(request);
			AjaxUtil.ajaxOutput(response, result);
		}else if(action.equals("viewticketuion")){//�鿴ͣ��ȯ������
			String result = viewTikcetUion(request);
			AjaxUtil.ajaxOutput(response, result);
		}else if(action.equals("getwxpcartic")){//����΢�Ź��ں����µ�ַ
			String ret = getWxpArtic(request);
			AjaxUtil.ajaxOutput(response, ret);
		}else if(action.equals("quickpay")){//���֧��
			String ret = quickPay(request);
			AjaxUtil.ajaxOutput(response, ret);
			//http://localhost/zld/carinter.do?action=quickpay&mobile=13641309140
		}else if(action.equals("gethxheads")){//ȡ���ź���ͷ��
			AjaxUtil.ajaxOutput(response, getHxHeads(request));
			//http://localhost/zld/carinter.do?action=gethxheads&mobile=18811157723
		}else if(action.equals("preticketuion")){
			AjaxUtil.ajaxOutput(response, preTicketUion(request));
		}else if(action.equals("getparks")){//��ѯȥͣ�����ĳ���
			AjaxUtil.ajaxOutput(response, getParks(request));
			//http://localhost/zld/carinter.do?action=getparks&mobile=13677226466
		}else if(action.equals("parkcars")){//��ѯ�������ͣ���ĳ���
			AjaxUtil.ajaxOutput(response, parkcars(request));
			//http://localhost/zld/carinter.do?action=parkcars&id=3251&mobile=15210932334
		}else if(action.equals("gethxname")){//�����˻��黷���˻�
			AjaxUtil.ajaxOutput(response, getHxName(request));
			//http://localhost/zld/carinter.do?action=gethxname&id=21776
		}else if(action.equals("preaddfriend")){//׼���Ӻ��ѣ������������
			AjaxUtil.ajaxOutput(response, preAddFriend(request));
			//http://localhost/zld/carinter.do?action=preaddfriend&fhxname=hx21776&mobile=15210932334&type=&resume=
		}else if(action.equals("addfriend")){//�ӻ��ź���
			AjaxUtil.ajaxOutput(response, addFriend(request));
			//http://localhost/zld/carinter.do?action=addfriend&id=hx21800&mobile=15801482643
		}else if(action.equals("getfriendhead")){//ȡ�µĻ��ŵĺ���΢��ͷ�񼰳��ƺ�
			AjaxUtil.ajaxOutput(response, getNewFriendhead(request));
			//http://localhost/zld/carinter.do?action=getfriendhead&ids=hx21776,hx21770,hx21783
		}
		/****������Ŀapp�ӿ�***/
//		else if(action.equals("Query_NearbyStatInfo")){//��ѯ��������վ��
//			//http://121.40.130.8/zld/carinter.do?action=Query_NearbyStatInfo&Longitude=119.394150&Latitude=32.387352&Range=2000
//			AjaxUtil.ajaxOutput(response, getNearbyStatInfo(request));
//		}
		else if(action.equals("getbusinfo")){//��ѯ��������վ��
			//http://service.yzjttcgs.com/zld/carinter.do?action=getbusinfo&Longitude=119.394150&Latitude=32.387352&Range=2000
			AjaxUtil.ajaxOutput(response, getNearbyStatInfo(request));
		}
//		else if(action.equals("RouteStatData")){//������·վ��ID��Ϣ
//			AjaxUtil.ajaxOutput(response, getRouteStatData(request));
//			//http://121.40.130.8/zld/carinter.do?action=RouteStatData&RouteID=2023&TimeStamp=2016-03-15+13%3A301%3A11
//		}
		else if(action.equals("stationinfo")){//������·վ��ID��Ϣ
			AjaxUtil.ajaxOutput(response, getRouteStatData(request));
			//http://127.0.0.1/zld/carinter.do?action=stationinfo&RouteID=1021  &timestamp=2016-03-15+13%3A301%3A11
		}else if(action.equals("getstatByName")){//������·վ������Ϣ
			AjaxUtil.ajaxOutput(response, getStatByName(request));
			//http://121.40.130.8/zld/carinter.do?action=getstatByName&stationName=%25E6%259C%2588%25E4%25BA%25AE
		}else if(action.equals("getstatbyname")){//������·վ������Ϣ
			AjaxUtil.ajaxOutput(response, getStatByName(request));
			//http://121.40.130.8/zld/carinter.do?action=getstatByName&stationName=%25E6%259C%2588%25E4%25BA%25AE
		}else if(action.equals("getstatbyid")){//ʵʱ��Ϣ��ѯ_��վ��ID 
			AjaxUtil.ajaxOutput(response, getStatById(request));
			//http://service.yzjttcgs.com/zld/carinter.do?action=getstatbyid&routeID=1021&stationID=    114423
		}else if(action.equals("getchargeinfo")){
			//http://127.0.0.1/zld/carinter.do?action=getchargeinfo&lng=119.394150&lat=32.387352
			AjaxUtil.ajaxOutput(response, getChargeInfo(request));
		}else if(action.equals("chargdetail")){
			//http://127.0.0.1/zld/carinter.do?action=chargdetail&id=3210030010
			AjaxUtil.ajaxOutput(response, chargeDetail(request));
		}else if(action.equals("getbikeall")){
			//http://127.0.0.1/zld/carinter.do?action=getbikeall&lng=119.387583&lat=32.396773
			AjaxUtil.ajaxOutput(response, "{\"bikelist\":"+getBikeAll(request)+"}");
		}else if(action.equals("bikedetail")){
			//http://service.yzjttcgs.com/zld/carinter.do?action=bikedetail&id=118
			AjaxUtil.ajaxOutput(response, bikeDetail(request));
		}else if(action.equals("bikehistory")){//�⳵��¼
			//http://127.0.0.1/zld/carinter.do?action=bikehistory&mobile=18101333937
			AjaxUtil.ajaxOutput(response, bikeHistory(request));
		}else if(action.equals("prereservecar")){//׼��ԤԼ��λ
			//http://127.0.0.1/zld/carinter.do?action=prereservecar&comid=8689&mobile=18101333937
			AjaxUtil.ajaxOutput(response, preReserveCar(request));
		}else if(action.equals("reservecar")){//ԤԼ��λ
			//http://service.yzjttcgs.com/zld/carinter.do?action=reservecar&later=1&comid=8689&mobile=18101333937&car_number=
			AjaxUtil.ajaxOutput(response, reserveCar(request));
		}else if(action.equals("canclereservecar")){//ȡ��ԤԼ��λ
			//http://service.yzjttcgs.com/zld/carinter.do?action=canclereservecar&orderid=112&mobile=18101333937
			AjaxUtil.ajaxOutput(response, cancleReserveCar(request));
		}else if(action.equals("findcar")){//����Ѱ��
			//http://service.yzjttcgs.com/zld/carinter.do?action=findcar&currplot=118&carplot=178&comid=8689&mobile=18560603731
			AjaxUtil.ajaxOutput(response, findCar(request));
		}else if(action.equals("getcarlocal")){//��ѯ��������λ��
			//http://service.yzjttcgs.com/zld/carinter.do?action=getcarlocal&mobile=18560603731
			AjaxUtil.ajaxOutput(response, getCarLocal(request));
		}else if (action.equals("getalltaxi")){
			AjaxUtil.ajaxOutput(response, getAllTaxi(request));
		}else if(action.equals("getrescarorders")){//��ѯ����ԤԼ����
			AjaxUtil.ajaxOutput(response, getReserveCarOrders(request));
		}else if(action.equals("rescarorderdetail")){
			//http://127.0.0.1/zld/carinter.do?action=rescarorderdetail&id=130
			AjaxUtil.ajaxOutput(response,resCarOrderDetail(request));
		}else if(action.equals("calltaxi")){
			AjaxUtil.ajaxOutput(response, callTaxi(request));
		}
		/*else if(action.equals("getalisign")){//ȡ֧����ǩ��
			AjaxUtil.ajaxOutput(response, getAliSign(request));
		}*/
		else if(action.equals("carorderhistory")){
			AjaxUtil.ajaxOutput(response, historyCarOrder(request));
		}else if(action.equals("openbtlock")){
			//http://127.0.0.1/zld/carinter.do?action=openbtlock&plot=177
			AjaxUtil.ajaxOutput(response, openBLLock(request));
		}
		/****������Ŀapp�ӿ�***/
		return null;
	}
	private String openBLLock(HttpServletRequest request) {
		String plotNo = RequestUtil.getString(request, "plot");
		/*KL959907328: A178��λ
		KL940703361��A177��λ*/
		String url = "";
		if(plotNo.equals("177")){
			url="http://www.no1parkinglock.com:8088/task/testAdd.action?numstatus=2&num=KL940703361";
		}else if(plotNo.equals("178")){
			url="http://www.no1parkinglock.com:8088/task/testAdd.action?numstatus=2&num=KL959907328";
		}
		return url;
	}
//	private String getAliSign(HttpServletRequest request) {
//		//http://service.yzjttcgs.com/zld/carinter.do?action=getalisign&content=
//		//http://127.0.0.1/zld/carinter.do?action=getalisign&partner=2088411488582814&seller_id=caiwu@zhenlaidian.com&out_trade_no=041616213922193&subject=���Ե���Ʒ&body=�ò�����Ʒ����ϸ����&total_fee=0.01&notify_url=http://service.yzjttcgs.com/zld/rechage&service=mobile.securitypay.pay&payment_type=1&_input_charset=utf-8&it_b_pay=30m&return_url=m.alipay.com
//		Map map = request.getParameterMap();
//		Map<String, String> parMap = new HashMap<String, String>();
//		for(Object key :map.keySet()){
//			parMap.put(key.toString(),"\""+request.getParameter(key.toString())+"\"");
//		}
//		parMap.remove("action");
//		String sign = AlipayUtil.sign(parMap);
//		logger.error("ali pay origin content:"+parMap+",sign:"+sign);
//		return sign;
//	}
	private String callTaxi(HttpServletRequest request) {
		Double mylng =RequestUtil.getDouble(request, "mylng", 0.0);
		Double mylat =RequestUtil.getDouble(request, "mylat", 0.0);
		String mystation  = RequestUtil.getString(request, "mystation");
		
		Double desclng =RequestUtil.getDouble(request, "desclng", 0.0);
		Double desclat =RequestUtil.getDouble(request, "desclat", 0.0);
		String descstation =RequestUtil.getString(request, "descstation");
		String mobile = RequestUtil.getString(request, "mobile");
		
		String url = "http://192.168.6.189:8080/OrderSrv/example/callCarMyself?" +
				"action.mLongitude="+mylng+"&action.mLatitude="+mylat+"&action.addr="+mystation+"" +
				"&action.mobilenumber="+mobile+"&action.findRadius=1000&action.dest="+descstation+"" +
				"&action.destlng="+desclng+"&action.destlat="+desclat+"&action.callfee=0" +
				"&action.ddtj=3&action.tip=0&action.carpool=0&action.veltype=4";
		String result = new HttpProxy().doGet(url);
		
		return null;
	}
	private String resCarOrderDetail(HttpServletRequest request) {
		Long id= RequestUtil.getLong(request, "id", -1L);
		logger.error("reservecar order detail id:"+id);
		String result ="{}";
		if(id!=-1){
			Map<String, Object> map = service.getMap("select o.create_time,o.state,o.id,o.arrive_time," +
				"o.limit_time,o.car_number,o.plot_no,c.company_name,c.id parkid,c.parking_type from order_reserve_tb o left join" +
				" com_info_tb c on o.comid=c.id where o.id=? ", new Object[]{id});
			if(map!=null&&!map.isEmpty()){
				Map<String, Object> info = new HashMap<String, Object>();
				Long ctime = (Long)map.get("create_time");
				info.put("date", TimeTools.getTime_yyyyMMdd_HHmm(ctime*1000));
				Long atime = (Long)map.get("arrive_time");
				info.put("arriver_time", TimeTools.getTime_yyyyMMdd_HHmm(atime*1000).substring(11));
				Long limittime = (Long)map.get("limit_time");
				info.put("limit_time", TimeTools.getTime_yyyyMMdd_HHmm(limittime*1000).substring(11));
				Integer state = (Integer)map.get("state");
				String s = "δ�볡";//0:Ƿ�� 1:�Ѳ��� 2:δ�볡 3:��ȡ��
				if(state==0){
					s="Ƿ�� ";
				}else if(state==1){
					s="�Ѳ���";
				}else if(state==3){
					s="��ȡ��";
				}
				info.put("state", s);
				info.put("plotno",  map.get("plot_no"));
				info.put("parkname", map.get("company_name"));
				info.put("orderid", map.get("id"));
				//��ͼƬ
				Map<String,Object> picMap = onlyService.getMap("select picurl from com_picturs_tb where comid=? order by id desc limit ?",
						new Object[]{map.get("parkid"),1});
				String picUrls = "";
				if(picMap!=null&&!picMap.isEmpty()){
					picUrls=(String)picMap.get("picurl");//"http://121.40.130.8/zld/parkpics/"+
				}else{
					Integer parkType = (Integer)map.get("parking_type");
					if(parkType==4){
						picUrls="8674_1460623804316.png";
					}else {
						picUrls="8694_1460616855017.png";
					}
				}
				info.put("imgurl", picUrls);
				result = StringUtils.createJson(info);
				logger.error("reservecar order detail id:"+id+",result:"+result);
			}
		}
		return result;
	}
	private String getReserveCarOrders(HttpServletRequest request) {
		//http://121.40.130.8/zld/carinter.do?action=getrescarorders&mobile=18101333937
		//http://127.0.0.1/zld/carinter.do?action=getrescarorders&mobile=18101333937
		String mobile = RequestUtil.getString(request, "mobile");
		Integer pageNum = RequestUtil.getInteger(request, "page", 1);
		Integer pageSize = RequestUtil.getInteger(request, "size", 20);
		Long uin = getUinByMobile(mobile);
		List<Map<String, Object>> infoMaps = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> list = onlyService.getPage("select o.create_time,o.state,o.id,o.arrive_time," +
				"o.limit_time,o.car_number,o.plot_no,c.company_name from order_reserve_tb o left join" +
				" com_info_tb c on o.comid=c.id where o.uin =? and o.type=? order by o.id desc",
				new Object[]{uin,0}, pageNum, pageSize);
		if(list!=null&&list.size()>0){
			for(Map map : list){
				Map<String, Object> info = new HashMap<String, Object>();
				Long ctime = (Long)map.get("create_time");
				info.put("date", TimeTools.getTime_yyyyMMdd_HHmm(ctime*1000));
				Long atime = (Long)map.get("arrive_time");
				info.put("arriver_time", TimeTools.getTime_yyyyMMdd_HHmm(atime*1000).substring(11));
				Long limittime = (Long)map.get("limit_time");
				info.put("limit_time", TimeTools.getTime_yyyyMMdd_HHmm(limittime*1000).substring(11));
				Integer state = (Integer)map.get("state");
				String s = "δ�볡";//0:Ƿ�� 1:�Ѳ��� 2:δ�볡 3:��ȡ��
				if(state==0){
					s="Ƿ�� ";
				}else if(state==1){
					s="�Ѳ���";
				}else if(state==3){
					s="��ȡ��";
				}
				info.put("state", s);
				info.put("plotno",  map.get("plot_no"));
				info.put("parkname", map.get("company_name"));
				info.put("orderid", map.get("id"));
				infoMaps.add(info);
			}
		}
		if(!infoMaps.isEmpty()){
			return "{\"booklist\":"+StringUtils.createJson(infoMaps)+"}";
		}
		return "[]";
	}
	private String getAllTaxi(HttpServletRequest request) {
		//http://service.yzjttcgs.com/zld/carinter.do?action=getalltaxi&mobile=18101333937&lng=119.387583&lat=32.396773&range=200
		String mobile =RequestUtil.getString(request, "mobile");
		Double lng =RequestUtil.getDouble(request, "lng", 0.0);
		Double lat =RequestUtil.getDouble(request, "lat", 0.0);
//		lng = lng-0.0073;
//		lat = lat-0.00575 ;
		Integer range = RequestUtil.getInteger(request, "range", 600);
		String result = new HttpProxy().doGet(CustomDefind.getValue("WEBSERVICECLIENTURL")+"?type=taxi&action=getalltaxi" +
		//String result = new HttpProxy().doGet("http://172.16.230.2:8080/wsclient/tsclient?type=taxi&action=getalltaxi" +
								"&mobile="+mobile+"&lng="+lng+"" +
				"&lat="+lat+"&range="+range);
//		System.out.println(result);
		String ret = "{\"total\":4,\"footer\":[],\"success\":true,\"msg\":\"�����ɹ���\",\"rows\":[{\"sex\":\"��\",\"empcode\":\"4110246812062617\",\"tel\":\"15052525285\",\"carno\":\"��KV9048\",\"mtype\":\"�ݴ�\",\"starlevel\":0,\"veltype\":1,\"id\":1,\"simno\":\"15396774156\",\"distance\":0.422,\"ownername\":\"���ڳ��⹫˾\",\"name\":\"����\",\"longitude\":119.38902,\"latitude\":32.396557},{\"sex\":\"��\",\"empcode\":\"3210026702272437\",\"tel\":\"13813199080\",\"carno\":\"��KBV147\",\"mtype\":\"ɣ����2��\",\"starlevel\":0,\"veltype\":1,\"id\":2,\"simno\":\"18051446414\",\"distance\":0.427,\"ownername\":\"�������\",\"name\":\"����\",\"longitude\":119.388862,\"latitude\":32.396363},{\"sex\":\"Ů\",\"empcode\":\"3210026807011540\",\"tel\":\"13815835067\",\"carno\":\"��KAW815\",\"mtype\":\"ɣ����2��\",\"starlevel\":0,\"veltype\":1,\"id\":3,\"simno\":\"18051447747\",\"distance\":0.514,\"ownername\":\"�������\",\"name\":\"�º���\",\"longitude\":119.387823,\"latitude\":32.396238},{\"sex\":\"Ů\",\"empcode\":\"3206117408312648\",\"tel\":\"13004315738\",\"carno\":\"��KBV867\",\"mtype\":\"ɣ����3000\",\"starlevel\":0,\"veltype\":1,\"id\":4,\"simno\":\"18051446454\",\"distance\":0.69,\"ownername\":\"�������\",\"name\":\"�⺣��\",\"longitude\":119.385927,\"latitude\":32.39639}]}";
		if(result!=null)
			return result;
		return ret;//"{\"total\":2,\"success\":true,\"msg\":\"�����ɹ���\",\"rows\":[{\"id\":1,\"longitude\":118.888888,\"latitude\":32.222222,\"carno\":\"��K66666\",\"simno\":\"13073222222\",\"tel\":\"13075555555\",\"name\":\"����\",\"sex\":\"��\",\"starlevel\":\"5��\",\"ownername\":\"����\",\"empcode\":\"A2334\",\"veltype\":\"����\",\"mtype\":\"ADEEE\",\"ordercount\":50,\"cancelcount\":20,\"praiserate\":27.1},{\"id\":2,\"longitude\":118.8444888,\"latitude\":32.222222,\"carno\":\"��A12345\",\"simno\":\"13075555555\",\"tel\":\"13075555555\",\"name\":\"����\",\"sex\":\"��\",\"starlevel\":\"4��\",\"ownername\":\"����\",\"empcode\":\"AAASA\",\"veltype\":\"�ݴ�\",\"mtype\":\"126585\",\"ordercount\":50,\"cancelcount\":20,\"praiserate\":27.1}]}";
	}
	private String getCarLocal(HttpServletRequest request) {
		//http://127.0.0.1/zld/carinter.do?action=getcarlocal&mobile=18101333937
		String mobile = RequestUtil.getString(request, "mobile");
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String carNumber = "";
		Map carMap = onlyService.getMap("select car_number from car_info_tb where uin=" +
				" (select id from user_info_tb where mobile=? and auth_flag=? )", new Object[]{mobile,4});
		if(carMap!=null&&!carMap.isEmpty()){
			carNumber = (String)carMap.get("car_number");
		}
		if(carNumber==null||carNumber.trim().length()!=7){
			resultMap.put("state", 0);
			resultMap.put("errmsg", "���Ʋ��Ϸ�");
		}else{
			//carNumber="��KA7406";
			logger.error("get cal car_number:"+carNumber);
			Map<String, Object> carLocalInfo =getCarPlot(carNumber);
			if(!carLocalInfo.isEmpty()){
				String plot =  (String)carLocalInfo.get("plot");
				resultMap.put("comid",carLocalInfo.get("comid"));
				resultMap.put("parkname", carLocalInfo.get("parkname"));
				resultMap.put("plot", carLocalInfo.get("plot"));
				if(plot!=null&&!"".equals(plot.trim()))
					resultMap.put("state", 1);
				else {
					resultMap.put("state", 1);
				}
				resultMap.put("errmsg", "");
			}else {
				resultMap.put("comid","");
				resultMap.put("parkname", "");
				resultMap.put("plot", "");
				resultMap.put("state", -1);
				resultMap.put("errmsg", "");
			}
		}
		logger.error("get cal local:"+resultMap);
		return StringUtils.createJson(resultMap);
	}
	private String bikeHistory(HttpServletRequest request) {
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String mobile = RequestUtil.getString(request, "mobile");
		//��ѯ�Ƿ�ע������֤������
		Map<String, Object> userMap = getUserByMobile(mobile);
		String certNo =(String)userMap.get("certno");//���֤����
		String actNo = "9616900760075187";//(String)userMap.get("actno");//���񿨺�
		if(certNo==null&&actNo==null){
			resultMap.put("state", -1);
			resultMap.put("errmsg", "���������񿨺Ż����֤�Ų�ѯ");
			resultMap.put("data", "[]");
		}else {
			String url ="http://yxiudongyeahnet.vicp.cc/wsclient/tsclient?type=bike&action=history&pageno=0&pagesize=100&mobile="+mobile;
			if(actNo!=null)
				url +="&actno="+actNo;
			else 
				url +="&certno="+certNo;
			Long etime = System.currentTimeMillis();
			Long btime = etime-15*24*3600*1000;//Ĭ�ϲ�15���
			String bstr = TimeTools.getTime_yyyyMMdd_HHmmss(btime);
			String estr = TimeTools.getTime_yyyyMMdd_HHmmss(etime);
			bstr = bstr.replaceAll("-", "").replaceAll(":", "").replaceAll(" ","").trim();
			estr = estr.replaceAll("-", "").replaceAll(":", "").replaceAll(" ","").trim();
			url +="&btime="+bstr+"&etime="+estr;
			String result = new HttpProxy().doGet(url);
			Map<String, Object> retMap = null;
			if(result!=null){
				retMap = ZldXMLUtils.parserStrXml(result);
			}
			if(retMap!=null&&!retMap.isEmpty()){
				resultMap.put("state", 1);
				resultMap.put("count", retMap.get("reccount"));
				String data="[]";
				String retInfo =(String) retMap.get("retinfo");
				if(retInfo!=null&&retInfo.indexOf("|")!=-1){
					if(retInfo.startsWith("|"))
						retInfo = retInfo.substring(1);
					String [] info  = retInfo.split("\\|");
					List<Map<String, Object>> dataList = new ArrayList<Map<String,Object>>();
					for(int i=0;i<info.length;i+=5){
						Map<String, Object> dataMap = new HashMap<String, Object>();
						//dataMap.put("id", info[i]);
						dataMap.put("start", info[1]);
						dataMap.put("borrlocation", info[2]);
						dataMap.put("end", info[3]);
						dataMap.put("droplocation", info[4]);
						dataList.add(dataMap);
					}
					resultMap.put("data", StringUtils.createJson(dataList));
				}
			}else {
				resultMap.put("state", 0);
				resultMap.put("errmsg", "δ��ѯ����¼");
				resultMap.put("data", "[]");
			}
		}
		
		return StringUtils.createJson(resultMap);
	}
	private String cancleReserveCar(HttpServletRequest request) {
		//http://127.0.0.1/zld/carinter.do?action=canclereservecar&orderid=101&mobile=18101333937

		Long id  = RequestUtil.getLong(request, "orderid", -1L);
		String mobile = RequestUtil.getString(request, "mobile");
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Map<String, Object> user = getUserByMobile(mobile);
		Map<String, Object> orderMap = service.getMap("select * from order_reserve_tb where id= ? and uin =? and state=?  ",
				new Object[]{id,user.get("id"),2});
		if(orderMap!=null){
			String car_number = (String)orderMap.get("car_number");
			Long endTime = (Long)orderMap.get("limit_time");
			Long ntime = System.currentTimeMillis()/1000;
//			if(ntime>endTime){//�ѹ�ԤԼ�������ʱ��
//				resultMap.put("satae", -2);
//				resultMap.put("errmsg", "�ѳ���ȡ��ʱ��");
//			}else {
				//�����ƶ��ӿ�
				String url = CustomDefind.getValue("WEBSERVICECLIENTURL") +
						"?type=mobilews&action=canclereserve&car_number="+AjaxUtil.encodeUTF8(AjaxUtil.encodeUTF8(car_number));
				String outXml = new HttpProxy().doGet(url);
				String code =null;
				try {
					JSONObject object = new JSONObject(outXml);
					code = object.getString("Code");
				} catch (JSONException e) {
					e.printStackTrace();
				}
				if(code!=null&&code.equals("1000")){
					int ret = service.update("update order_reserve_tb set state =? where id =? ", new Object[]{3,orderMap.get("id")});
					if(ret==1){
						resultMap.put("satae", 1);
						resultMap.put("errmsg", "��ȡ����λԤԼ");
					}else {
						resultMap.put("satae", -1);
						resultMap.put("errmsg", "��ȡ����λԤԼ");
					}
				}
//			}
		}else {
			resultMap.put("satae", 0);
			resultMap.put("errmsg", "����������");
		}
		return StringUtils.createJson(resultMap);
	}
	//����Ѱ��
	private String findCar(HttpServletRequest request) {
		//http://127.0.0.1/zld/carinter.do?action=findcar&currplot=A15&carplot=A1&comid=8551&mobile=18101333937
		
		Map<String, Object> resultMap = new HashMap<String, Object>();	
		//������ǰ���ڳ�λ
		String currPlot = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "currplot"));
		String carPlot = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "carplot"));
		Long comId = RequestUtil.getLong(request, "comid",-1L);
		String mobile = RequestUtil.getString(request, "mobile");
		Map carMap = onlyService.getMap("select car_number from car_info_tb where uin =" +
				"(select id from user_info_tb where mobile=? and auth_flag=?)", new Object[]{mobile,4});
		String carNumber = null;
		logger.error("find car,currplot:"+currPlot+",carplot:"+carPlot+",comid:"+comId);
		if(carMap!=null)
			carNumber = (String)carMap.get("car_number");
		if(carNumber==null||carNumber.trim().equals("")){
			resultMap.put("state", -1);
			resultMap.put("errmsg", "��δע�ᳵ��");
			resultMap.put("imgurl", "");
			return StringUtils.createJson(resultMap);
		}
		///Map<String, Object> carPlotResult = getCarPlot(carNumber);
			//String ParkingId = (String)carPlotResult.get("comid");
		if(carPlot==null){
			resultMap.put("state", -2);
			resultMap.put("errmsg", "û�в鵽ͣ����Ϣ");
			resultMap.put("imgurl", "");
		}else {
			String imgurl= getFindCarImg(comId, currPlot, carPlot);
			if(imgurl!=null&&!"".equals(imgurl)){
				resultMap.put("state", 1);
				resultMap.put("errmsg", "");
				resultMap.put("imgurl", imgurl);
			}else {
				resultMap.put("state", -2);
				resultMap.put("errmsg", "û�в鵽ͣ����Ϣ");
				resultMap.put("imgurl", "");
			}
		}
		logger.error("findcar image:"+resultMap);
		return StringUtils.createJson(resultMap);
	}
	//���ú��Žӿ�
	private String getFindCarImg(Long comid, String currPlot, String carPlot) {
		String url = new HttpProxy().doGet(CustomDefind.getValue("WEBSERVICECLIENTURL") +"?type=findcar&parkid="+comid+"&sid="+currPlot+"&eid="+carPlot);
		return url;
	}
	//�ƶ��ӿڵ��ó���ͣ������λ��
	private Map<String, Object> getCarPlot(String carNumber) {
		String reslut = new HttpProxy().doGet(CustomDefind.getValue("WEBSERVICECLIENTURL") +"" +
				"?type=mobilews&action=findcar&car_number="+AjaxUtil.encodeUTF8(AjaxUtil.encodeUTF8(carNumber)));
		Map<String, Object> retMap = new HashMap<String, Object>();
		try {
			JSONObject object = new JSONObject(reslut);
			String prarId = object.getString("ParkingId");
			if(prarId!=null){
				Map comMap = onlyService.getMap("select company_name,id from com_info_tb where park_uuid=? ", new Object[]{prarId+"0000000000000000000000000000000"});
				if(comMap!=null){
					retMap.put("parkname", comMap.get("company_name"));
					retMap.put("comid",comMap.get("id"));
				}
			}
			retMap.put("plot",object.getString("CarportNo") );
		} catch (JSONException e) {
			logger.error("����Ѱ��,�û����ƣ�"+carNumber+"���ӿڷ��أ�"+reslut+"����������"+e.getMessage());
			e.printStackTrace();
		}
		logger.error("get cal loacl interface return:"+retMap);
		return retMap;
	}
	//��ʼԤԼ��λ 
	private String reserveCar(HttpServletRequest request) {
		//http://127.0.0.1/zld/carinter.do?action=reservecar&later=1&comid=8689&mobile=18101333937
		String mobile = RequestUtil.getString(request, "mobile");
		String comid = RequestUtil.getString(request, "comid");
		String carNumber =AjaxUtil.decodeUTF8(AjaxUtil.decodeUTF8(RequestUtil.getString(request, "car_number")));
		//��Сʱ�󵽴�
		Integer later = RequestUtil.getInteger(request, "later", 1);
		Map<String, Object> resultMap = new HashMap<String, Object>();
		Double p1 = 6.0;//1Сʱ�۸�
		Double p2 = 8.0;//2Сʱ�۸�
		Double balance = 7.0;//���
		//String carNumber = "";
		Map<String, Object> userMap= getUserByMobile(mobile);
		if(userMap!=null){
			balance = StringUtils.formatDouble(userMap.get("balance"));
//			Map<String, Object> carMap = onlyService.getMap("select car_number from car_info_tb where uin = ? ", new Object[]{userMap.get("id")});
//			if(carMap!=null)
//				carNumber = (String) carMap.get("car_number");
		}
		logger.error("ԤԼͣ����Ԥ��"+later+"Сʱ�󵽴�,�û���"+ balance+",���ƣ�"+carNumber );
		/*if(carNumber==null||"".equals(carNumber.trim())){
			resultMap.put("state", -1);
			resultMap.put("plot", "");
			resultMap.put("errmsg", "����ע�ᳵ��");
			return StringUtils.createJson(resultMap);
		}else if(carNumber.length()!=7){
			resultMap.put("state", -2);
			resultMap.put("plot", "");
			resultMap.put("errmsg", "���Ʋ��Ϸ�:"+carNumber);
			return StringUtils.createJson(resultMap);
		}*/
		//����ԤԼ���ѣ������Ա�
		boolean isCanDate=false;
		if(balance>0){//����ԤԼʱ���ж�����Ƿ����
			if(later==1)
				isCanDate = (balance>=p1);
			else if(later==2){
				isCanDate = (balance>=p2);
			}
		}
		if(carNumber.length()!=7){
			resultMap.put("state", 0);
			resultMap.put("errmsg", "��ѡ����");
		}else if(isCanDate){//����ԤԼ
			
			//���������ƶ���ԤԼ�ӿ�
			Long ntime =System.currentTimeMillis()/1000;
			Map comMap = service.getMap("select park_uuid from com_info_tb where id =? ", new Object[]{Long.valueOf(comid)});
			String plot = null;
			if(comMap!=null&&comMap.get("park_uuid")!=null){
				String comNo = (String)comMap.get("park_uuid");
				plot = getPlot(carNumber,comNo.substring(0,5),TimeTools.getTime_yyyyMMdd_HHmmss((ntime+later*3600)*1000),TimeTools.getTime_yyyyMMdd_HHmmss((ntime+(later+1)*3600)*1000));
			}
			if(plot!=null&&!"".equals(plot)){//ԤԼ�ɹ�
				
				Long id = service.getkey("seq_order_reserve_tb");
				int ret = service.update("insert into order_reserve_tb(id,comid,uin,create_time,arrive_time,limit_time,state,car_number,type,plot_no)" +
						"values(?,?,?,?,?,?,?,?,?,?)", new Object[]{id,Long.valueOf(comid),userMap.get("id"),ntime,ntime+later*3600,ntime+(later+1)*3600,2,carNumber,0,plot});
				if(ret==1){
					resultMap.put("state", 1);
					resultMap.put("plot", plot);
					resultMap.put("orderid", id);
					resultMap.put("errmsg", "ԤԼ�ɹ���");
				}else {
					resultMap.put("state", 0);
					resultMap.put("errmsg", "��λ���������Ժ����ԣ�");
				}
			}else {//ԤԼʧ��
				resultMap.put("state", 0);
				resultMap.put("errmsg", "��λ���������Ժ����ԣ�");
			}
		}else {//����
			resultMap.put("state", -3);
			resultMap.put("errmsg", "���㣡");
		}
		return StringUtils.createJson(resultMap);
	}
	private String getPlot(String car_number,String comid,String btime,String etime) {
		String url = CustomDefind.getValue("WEBSERVICECLIENTURL") +
				"?type=mobilews&action=reservecar&parkid="+comid+"&car_number="+AjaxUtil.encodeUTF8(AjaxUtil.encodeUTF8(car_number))+"&btime="+AjaxUtil.encodeUTF8(btime)+"&etime="+AjaxUtil.encodeUTF8(etime);
		logger.error("resvercar wsurl :"+url);
		String outXml = new HttpProxy().doGet(url);
		logger.error("resvercar ws result :"+outXml);
		String plot = "";
		try {
			JSONObject object = new JSONObject(outXml);
			String code = object.getString("Code");
			if(code!=null&&code.equals("1000")){
				plot = object.getString("BerthNo");
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return plot;
	}
	//׼��ԤԼ��λ
	private String preReserveCar(HttpServletRequest request) {
		//http://127.0.0.1/zld/carinter.do?action=prereservecar&comid=8689&mobile=18101333937

		Long parkId = RequestUtil.getLong(request, "comid", -1L);
		String mobile = RequestUtil.getString(request, "mobile");
		Map<String, Object> resultMap = new HashMap<String, Object>();
		//��֤����
		String carNumbers ="[";
		List<Map<String, Object>> carList = onlyService.getAll("select car_number from car_info_tb where uin =(" +
				"select id  from user_info_tb where mobile=? ) ", new Object[]{mobile});
		if(carList!=null&&!carList.isEmpty()){
			for(Map<String, Object> map : carList){
				carNumbers += "\""+(String)map.get("car_number")+"\",";
			}
		}
		if(carNumbers.endsWith(","))
			carNumbers = carNumbers.substring(0,carNumbers.length()-1);
		carNumbers +="]";
		resultMap.put("reserve_max_hour", 2);
		Map<String, Object> comMap = onlyService.getMap("select book from com_info_tb where id =? ", new Object[]{parkId});
		if(comMap==null||comMap.get("book").toString().equals("0")){
			resultMap.put("state", 0);
			resultMap.put("total", 0);
			resultMap.put("free", 0);
			resultMap.put("errmsg", "");
			resultMap.put("orderid", "");
			resultMap.put("car_numbers", carNumbers);
			return StringUtils.createJson(resultMap);
		}
		resultMap.put("price", "��Сʱ5Ԫ����Сʱ��6Ԫ");
		
		if(carNumbers.equals("[]")){
			resultMap.put("state", -1);
			resultMap.put("total", 0);
			resultMap.put("free", 0);
			resultMap.put("orderid", "");
			resultMap.put("car_numbers", carNumbers);
			resultMap.put("errmsg", "����ע�ᳵ��");
			return StringUtils.createJson(resultMap);
		}
		//��ѯ��ԤԼ��λ
		Map oMap= onlyService.getMap("select id,limit_time from order_reserve_tb where state=? and  uin =(select id from " +
				"user_info_tb where mobile=? and auth_flag=? ) ",new Object[]{2,mobile,4});
		if(oMap!=null&&!oMap.isEmpty()){//����ԤԼ������δ����
			resultMap.put("state", -2);
			resultMap.put("total", 0);
			resultMap.put("free", 0);
			resultMap.put("orderid", oMap.get("id"));
			Long limitTime = (Long)oMap.get("limit_time");
			if(limitTime!=null&&limitTime>0){
				resultMap.put("limit_time",TimeTools.getTime_yyyyMMdd_HHmm(limitTime*1000) );
			}
			resultMap.put("car_numbers", carNumbers);
			resultMap.put("errmsg", "����ԤԼ����");
			return StringUtils.createJson(resultMap);
		}
		Long count = onlyService.getLong("select count(id) from order_reserve_tb where comid=? and state=? ", new Object[]{parkId,2});
		if(count>1){//��λ����
			resultMap.put("state", -3);
			resultMap.put("total", 0);
			resultMap.put("free", 0);
			resultMap.put("orderid", "");
			resultMap.put("car_numbers", carNumbers);
			resultMap.put("errmsg", "ԤԼ��λ����");
			return StringUtils.createJson(resultMap);
		}
		resultMap.put("state", 1);
		resultMap.put("total", 2);
		resultMap.put("free", 2-count);
		resultMap.put("errmsg", "");
		resultMap.put("car_numbers", carNumbers);
		resultMap.put("orderid", "");
		return StringUtils.createJson(resultMap);
	}
	//��ѯ���׮����
	private String chargeDetail(HttpServletRequest request) {
		Long id = RequestUtil.getLong(request, "id", -1L);
		List<Map<String, Object>> chargeList = getChargeSite();
		Map<String, Object> retMap = new HashMap<String, Object>();
		String result = "{}";
		if(!chargeList.isEmpty()){
			for(Map<String, Object> map :chargeList){
				if(!retMap.isEmpty())
					break;
				//�������
				Long idLong= Long.valueOf(map.get("staCode")+"");
				if(idLong==null||id.intValue()!=idLong.intValue()){
					continue;
				}
				//ת���ֶ�����
				retMap.put("addr", map.get("staAddress"));
				retMap.put("id", idLong);
				retMap.put("lng", map.get("lng"));
				retMap.put("lat", map.get("lat"));
				retMap.put("name", map.get("staName"));
				Integer ac = Integer.valueOf(map.get("acNum")+"");
				Integer acable = Integer.valueOf(map.get("acableNum")+"");
				Integer dc = Integer.valueOf(map.get("dcNum")+"");
				Integer dcable = Integer.valueOf(map.get("dcableNum")+"");
				retMap.put("total", ac+dc);
				retMap.put("free", acable+dcable);
				retMap.put("price", map.get("price")+"Ԫ/ǧ��ʱ");
				retMap.put("desc", ac+"���������׮��"+acable+"�����ã�"+dc+"��ֱ�����׮��"+dcable+"������");
			}
		}
		//http://127.0.0.1/zld/carinter.do?action=chargdetail&id=3210030009

//		String c1 = "{\"id\":\"1\",\"name\":\"�³�������������\",\"lng\":\"119.390696\",\"lat\":\"32.395396\"," +
//				"\"total\":\"12\",\"addr\":\"�Ĳ���·525��\",\"desc\":\"10���������׮��2���������׮\",\"price\":\"1.47Ԫ/KW(�����)\",\"free\":\"12\"}" ;
//		String c2= "{\"id\":\"2\",\"name\":\"����վ����ͣ����\",\"lng\":\"119.356177\",\"lat\":\"32.392036\"," +
//				"\"total\":\"6\",\"addr\":\"���������ݻ�վ�Ա�\",\"desc\":\"6��ֱ�����׮\",\"price\":\"1.47Ԫ/KW(�����)\",\"free\":\"6\"}" ;
//		String result ="{}";
//		if(id==1)
//			result =c1;
//		else if(id==2){
//			result=c2;
//		}
		result = StringUtils.createJson(retMap);
		return result;
	}
	//��ѯ�������׮վ�㣬Ŀǰ��������
	private String getChargeInfo(HttpServletRequest request) {
		//{\"pageCount\":\"1\",\"itemCount\":\"3\",\"staList\":[{\"staCode\":\"3210030010\",\"staName\":\"���ݽ�ͨ��������վ\",\"staType\":\"����վ\",\"staOpstate\":\"��Ӫ��\",\"province\":\"����ʡ\",\"city\":\"������\",\"region\":\"������\",\"staAddress\":\"����ʡ�������������������Ĳ���·525��\",\"lng\":\"119.390196\",\"lat\":\"32.395320\",\"price\":\"1.63\",\"acNum\":\"2\",\"dcNum\":\"10\",\"acableNum\":\"2\",\"dcableNum\":\"5\"},{\"staCode\":\"3210030011\",\"staName\":\"��������������Ŧ����ͣ�������վ\",\"staType\":\"����վ\",\"staOpstate\":\"��Ӫ��\",\"province\":\"����ʡ\",\"city\":\"������\",\"region\":\"������\",\"staAddress\":\"����ʡ�����������������л�վ����\",\"lng\":\"119.356392\",\"lat\":\"32.392111\",\"price\":\"1.63\",\"acNum\":\"0\",\"dcNum\":\"5\",\"acableNum\":\"0\",\"dcableNum\":\"0\"},{\"staCode\":\"3210030009\",\"staName\":\"������������ջ���վ\",\"staType\":\"����վ\",\"staOpstate\":\"��Ӫ��\",\"province\":\"����ʡ\",\"city\":\"������\",\"region\":\"������\",\"staAddress\":\"����ʡ��������������·���õ��԰����\",\"lng\":\"119.413693\",\"lat\":\"32.420896\",\"price\":\"1.63\",\"acNum\":\"1\",\"dcNum\":\"2\",\"acableNum\":\"0\",\"dcableNum\":\"0\"}]}
		Double lng =RequestUtil.getDouble(request, "lng", 0.0);
		Double lat =RequestUtil.getDouble(request, "lat", 0.0);
		List<Map<String, Object>> chargeList = getChargeSite();
		String reslut = "{}";
		if(!chargeList.isEmpty()){
			List<Map<String, Object>> retList = new ArrayList<Map<String,Object>>();
			for(Map<String, Object> map :chargeList){
				//�������
				Map<String, Object> retMap = new HashMap<String, Object>();
				Double lng1 =Double.valueOf(map.get("lng")+"");
				Double lat1 =Double.valueOf(map.get("lat")+"");
				double distance = StringUtils.distanceByLnglat(lng,lat,lng1,lat1);
				Integer d=  StringUtils.formatDouble(distance*1000).intValue();
				retMap.put("distance", d);
				//ת���ֶ�����
				retMap.put("addr", map.get("staAddress"));
				retMap.put("id", map.get("staCode"));
				retMap.put("lng", map.get("lng"));
				retMap.put("lat", map.get("lat"));
				retMap.put("name", map.get("staName"));
				Integer ac = Integer.valueOf(map.get("acNum")+"");
				Integer acable = Integer.valueOf(map.get("acableNum")+"");
				Integer dc = Integer.valueOf(map.get("dcNum")+"");
				Integer dcable = Integer.valueOf(map.get("dcableNum")+"");
				retMap.put("total", ac+dc);
				retMap.put("price", map.get("price")+"Ԫ/ǧ��ʱ");
				retMap.put("free", acable+dcable);
				retMap.put("desc", ac+"���������׮��"+acable+"�����ã�"+dc+"��ֱ�����׮��"+dcable+"������");
				retList.add(retMap);
			}
			reslut = "{\"suggid\":\"\",\"data\":"+StringUtils.createJson(retList)+"}";
		}
		//http://127.0.0.1/zld/carinter.do?action=getchargeinfo&lng=119.394150&lat=32.387352

//		String reslut = "{\"suggid\":\"\",\"data\":[{\"id\":\"1\",\"name\":\"�³�������������\",\"lng\":\"119.390696\",\"lat\":\"32.395396\",\"total\":\"12\",\"addr\":\"�Ĳ���·525��\",\"desc\":\"10���������׮��2���������׮\",\"distance\":\""+d1+"\"}," +
//				"{\"id\":\"2\",\"name\":\"����վ����ͣ����\",\"lng\":\"119.356177\",\"lat\":\"32.392036\",\"total\":\"6\",\"addr\":\"���������ݻ�վ�Ա�\",\"desc\":\"6��ֱ�����׮\",\"distance\":\""+d2+"\"}]}" ;
		//logger.error("���׮���أ�"+reslut);
		return reslut;
	}
	//���г�վ������
	private String bikeDetail(HttpServletRequest request) {
		Long id =RequestUtil.getLong(request, "id",-1L);
		//http://127.0.0.1/zld/carinter.do?action=bikedetail&id=120
		//http://service.yzjttcgs.com/zld/carinter.do?action=bikedetail&id=120
		//String url ="http://172.16.220.32:8080/wsclient/tsclient?type=bike&action=detail&id="+id ;
		//String result = new HttpProxy().doGet(url);
		//{ret=0, zdaddr=���··��, wdinfo=32.396822, totalcws=32, zdname=ְ����, jdinfo=119.382417, leftcws=29}
		Map<String, Object> retMap = new HashMap<String, Object>();
		/*if(result!=null){
			Map<String, Object> resultMap = ZldXMLUtils.parserStrXml(result);
			if(resultMap!=null){
				retMap.put("name", resultMap.get("zdname"));
				retMap.put("address", resultMap.get("zdaddr"));
				retMap.put("lng", resultMap.get("jdinfo"));
				retMap.put("lat", resultMap.get("wdinfo"));
				retMap.put("price", "��Сʱ��ѣ���1Ԫ/Сʱ�����20Ԫÿ��");
				retMap.put("id", id);
				retMap.put("total_count", resultMap.get("totalcws"));
				retMap.put("left_count", resultMap.get("leftcws"));
			}
		}*/
		if(retMap.isEmpty()){
			Map bikeMap = onlyService.getMap("select * from city_bike_tb where id =? ", new Object[]{id});
			if(bikeMap!=null&&!bikeMap.isEmpty()){
				retMap.put("name", bikeMap.get("name"));
				retMap.put("address", bikeMap.get("address"));
				retMap.put("lng", bikeMap.get("longitude"));
				retMap.put("lat", bikeMap.get("latitude"));
				retMap.put("price", "��Сʱ��ѣ���1Ԫ/Сʱ�����20Ԫÿ��");
				retMap.put("id", id);
				retMap.put("total_count", bikeMap.get("plot_count"));
				Integer free = (Integer)bikeMap.get("surplus");
//				free = free-new Random().nextInt(2);
//				free=free<=0?5:free;
				retMap.put("left_count",free);
			}
		}
		return StringUtils.createJson(retMap);
	}
	//��ѯ���������ﷶΧ�ڵ����г�����վ��
	private String getBikeAll(HttpServletRequest request) {
		//http://127.0.0.1/zld/carinter.do?action=getbikeall&lng=119.387583&lat=32.396773
		updateBikeInfo();
		//double lon1 = 0.023482756;//��������
		//double lat1 = 0.017978752;
		double lon1 = 0.015482756/2;//һ������
		double lat1 = 0.014978752/2;
		Double lng =RequestUtil.getDouble(request, "lng", 0.0);
		Double lat =RequestUtil.getDouble(request, "lat", 0.0);
//		lng = lng-0.0053;
//		lat = lat-0.00575 ;
		String sql = "select id,name,address,longitude lng,latitude lat,plot_count,surplus" +
				" from city_bike_tb where longitude between ? and ? " +
				"and latitude between ? and ?  ";
		List<Object> params = new ArrayList<Object>();
		params.add(lng-lon1);
		params.add(lng+lon1);
		params.add(lat-lat1);
		params.add(lat+lat1);
		List<Map<String, Object>> result = service.getAll(sql, params,0,0);
		logger.error("get bike all :lng:"+lng+",lat:"+lat);
		if(result!=null){
			for(Map<String, Object> map:result){
				double lon2 = Double.valueOf(map.get("lng")+"");
				double lat2 = Double.valueOf(map.get("lat")+"");
				double distance = StringUtils.distanceByLnglat(lng,lat,lon2,lat2);
				map.put("distance", StringUtils.formatDouble(distance*1000).intValue());
			}
			return StringUtils.createJson(result);
		}
		else
			return "[]";
	}
	//��ѯ����վ������
	private String getStatById(HttpServletRequest request) {
		Long routeId = RequestUtil.getLong(request, "routeID", -1L);
		Long stationId = RequestUtil.getLong(request, "stationID", -1L);
		HttpProxy httpProxy = new HttpProxy();
		String url =CustomDefind.getValue("BUSURL") +"Query_ByStationID/?routeID="+routeId+"&stationID="+stationId;
		String result =httpProxy.doGet(url);
		logger.error("url:"+url+",result��"+result);
		if(result==null)
			result="[]";
		else {
			result="{\"data\":"+result+"}";
		}
		return result;
	}
	//ģ����ѯ����վ��
	private String getStatByName(HttpServletRequest request){
		String StationName =RequestUtil.getString(request, "stationName");
		
		HttpProxy httpProxy = new HttpProxy();
		String url =CustomDefind.getValue("BUSURL") +"Query_ByStaNameNE/?StationName="+StationName;
		String result =httpProxy.doGet(url);
		logger.error("url:"+url+",result��"+result);
		if(result==null)
			result="[]";
		else {
			result="{\"data\":"+result+"}";
		}
		return result;
		
	}
	//������·��ѯվ����Ϣ
	private String getRouteStatData(HttpServletRequest request) {
		Long routeID = RequestUtil.getLong(request, "RouteID", -1L);
		Long time = RequestUtil.getLong(request, "time", -1L);
		///carinter.do?action=stationinfo&routeid=2023&time=2016-03-15+13%3A301%3A11
		if(time==-1)
			time = System.currentTimeMillis()/1000;
		String timeStamp = TimeTools.getTime_yyyyMMdd_HHmmss(time*1000);
		
		HttpProxy httpProxy = new HttpProxy();
		String url =CustomDefind.getValue("BUSURL") +"Require_RouteStatData/?RouteID="+routeID+"&TimeStamp="+AjaxUtil.encodeUTF8(timeStamp);
		String result =httpProxy.doGet(url);
		//logger.error("url:"+url+",result��"+result);
		if(result==null)
			result="[]";
		else {
			/**
			 * {"RouteID":1021,"RunBusNum":0,"RStaRealTInfoList":[{"StationID":"102002","RStanum":1,"ExpArriveBusStaNum":1,"StopBusStaNum":0,"BusType":0},{"StationID":"101414","RStanum":5,"ExpArriveBusStaNum":1,"StopBusStaNum":0,"BusType":0},{"StationID":"105184","RStanum":12,"ExpArriveBusStaNum":1,"StopBusStaNum":0,"BusType":0},{"StationID":"100241","RStanum":14,"ExpArriveBusStaNum":1,"StopBusStaNum":0,"BusType":0},{"StationID":"104544","RStanum":19,"ExpArriveBusStaNum":0,"StopBusStaNum":1,"BusType":0}],"IsEnd":null}
			* {"RouteID":1021,"RunBusNum":0,"RStaRealTInfoList":[{"StationID":"100262","RStanum":3,"ExpArriveBusStaNum":0,"StopBusStaNum":1,"BusType":0},{"StationID":"100243","RStanum":11,"ExpArriveBusStaNum":1,"StopBusStaNum":0,"BusType":0},{"StationID":"114373","RStanum":16,"ExpArriveBusStaNum":1,"StopBusStaNum":0,"BusType":0},{"StationID":"101982","RStanum":18,"ExpArriveBusStaNum":1,"StopBusStaNum":0,"BusType":0},{"StationID":"105972","RStanum":22,"ExpArriveBusStaNum":1,"StopBusStaNum":0,"BusType":0}],"IsEnd":null}
			 */
			List<Map<String, Object>> info = ParseJson.jsonToList1(result);
			try {
				for(Map<String, Object> map : info){
					List<Map<String, Object>> segmentList=(List<Map<String, Object>>)map.get("SegmentList");
					for(Map<String, Object> map1: segmentList){
						String url1  = "http://218.91.52.117:8999/BusService/Query_ByRouteID?RouteID="+routeID+"&Segmentid="+map1.get("SegmentID");
						String result1 =httpProxy.doGet(url1);
						List<Map<String, Object>> StationList =(List<Map<String, Object>>)map1.get("StationList");
						Map<String, Object> SegmentMap = ParseJson.jsonToMap1(result1);
						for(Map<String, Object> map3 : StationList){
							String sid = (String)map3.get("StationID");
							List<Map<String, Object>> RStaRealTInfoList = (List<Map<String, Object>>)SegmentMap.get("RStaRealTInfoList");
							map3.put("expArriveBusStaNum","0");
							map3.put("stopBusStaNum","0");
							for(Map<String, Object> map4:RStaRealTInfoList ){
								String sid1 = (String)map4.get("StationID");
								if(sid.equals(sid1)){
									map3.put("expArriveBusStaNum", map4.get("ExpArriveBusStaNum"));
									map3.put("stopBusStaNum", map4.get("StopBusStaNum"));
									break;
								}
							}
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			String json = ParseJson.createJson(info);
			result="{\"data\":"+json+"}";
			logger.error("result��"+result);
		}
		return result;
	}
	//��ѯ����ָ����Χ�ڵĹ�����վ��
	private String getNearbyStatInfo(HttpServletRequest request) {
		Double lng = RequestUtil.getDouble(request, "Longitude", 0d);
		Double lat = RequestUtil.getDouble(request, "Latitude", 0d);
		Integer range =700;// RequestUtil.getInteger(request, "Rang", 1800);
//		lng = lng-0.0053;
//		lat = lat-0.00575 ;
		///carinter.do?action=getbusinfo&lng=119.394150&lat=32.387352&rang=200
		if(range<1000)
			range=1000;
		HttpProxy httpProxy = new HttpProxy();
		String url =CustomDefind.getValue("BUSURL") +"Query_NearbyStatInfo/?Longitude="+lng+"&Latitude="+lat+"&Range="+range;
		String result = httpProxy.doGet(url);
		if(result==null)
			result="[]";
		else {
			//result="{\"buslist\":"+result+"}";
		}
		logger.error("�������ݳɹ����أ�lng:"+lng+",lat:"+lat);
		//Long count = onlyService.getCount("select count(id) from bus_station_tb ", null);
		//if(count==0){
			JSONArray array=null;
			List<Map<String, Object>> busList = new ArrayList<Map<String,Object>>();
			try {
				array = new JSONArray(result);
				if(array!=null&&array.length()>0){
					for(int i=0;i<array.length();i++){
						Map<String, Object> busMap = new HashMap<String, Object>();
						JSONObject object = array.getJSONObject(i);
						for (Iterator<String> iter = object.keys(); iter.hasNext();) { 
					       String key = iter.next();
					       Object value = object.get(key);
					       if(value==null||value.toString().toLowerCase().trim().equals("null"))
					    	   value="";
					       if(key.equals("StationPostion")){
					    	   JSONObject subObject = new JSONObject(value.toString());
					    	   busMap.put("Longitude", subObject.get("Longitude"));
					    	   busMap.put("Latitude", subObject.get("Latitude"));
					       }else if(key.equals("Distance")){
					    	   busMap.put(key, StringUtils.formatDouble(value));
					       }else {
					    		   busMap.put(key, value);
//					    	   if(key.equals("Distance"))
//					    		   busMap.put(key, StringUtils.formatDouble(value));
//					    	   else {
//					    	   }
					       }
						 }
						busList.add(busMap);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			result = "{\"buslist\":"+StringUtils.createJson(busList)+"}";
//			if(!busList.isEmpty()){
//				logger.error("��ʼд��....");
//				String sql = "insert into bus_station_tb (station_id,station_name,longitude,latitude," +
//						"station_memo,create_time,update_time) values(?,?,?,?,?,?,?)";
//				List<Object[]> values = new ArrayList<Object[]>();
//				Long ntime = System.currentTimeMillis()/1000;
//				for(Map<String, Object> map : busList){
//					Object []params= new Object[]{map.get("StationID"),map.get("StationName"),map.get("Longitude"),
//							map.get("Latitude"),map.get("StationMemo"),ntime,ntime};
//					values.add(params);
//				}
//				int ret = service.bathInsert(sql, values, new int[]{4,12,3,3,12,4,4});
//				logger.error("д�빫�����ݣ�"+ret+"��");
//			}
		//}
			//logger.error("�ɹ����أ�"+result);
		return result;
	}


	//׼���Ӻ��ѣ������������
	private String preAddFriend(HttpServletRequest request) {
		String mobile = RequestUtil.getString(request, "mobile");
		String fname = RequestUtil.getString(request, "fhxname");
		Long buin = getUinByMobile(mobile);
		Long euin =-1L;
		if(!fname.equals("")&&Check.isLong(fname.substring(2)))
			euin = Long.valueOf(fname.substring(2));
		//"user_preaddfriend_tb_buin_euin_key"
		String result = "{\"result\":\"0\",\"errmsg\":\"�˺��쳣\"}";
		Integer type = RequestUtil.getInteger(request, "type", 1);
		String resume = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "resume"));
		System.err.println("<<<<<<<<<<<resume:"+resume);
		Long ntime = System.currentTimeMillis()/1000;
		int ret = 0;
		if(buin>1&&euin>1){
			try {
				ret=service.update("insert into user_preaddfriend_tb(buin,euin,ctime,utime,atype,resume)" +
						" values(?,?,?,?,?,?)", new Object[]{buin,euin,ntime,ntime,type,resume});
				logger.error(mobile+",preaddhxfriend,euin:"+euin+",ret:"+ret);
				result = "{\"result\":\"1\",\"errmsg\":\"����ɹ�\"}";
			} catch (Exception e) {
				if(e.getMessage().indexOf("user_preaddfriend_tb_buin_euin_key")!=-1){//�ظ�����ˣ�����һ�¾�����
					ret = service.update("update user_preaddfriend_tb set atype=?,resume=?,utime=? where " +
							"buin=? and euin=? ", new Object[]{type,resume,ntime,buin,euin});
					logger.error(mobile+",preaddhxfriend error ,update data,euin:"+euin+",ret:"+ret);
					result = "{\"result\":\"1\",\"errmsg\":\"�Ѿ�����,���³ɹ�\"}";
				}
			}
			
		}
		logger.error(mobile+",preaddhxfriend,result:"+result);
		return result;
	}
	//ȡ�µĻ��ŵĺ���΢��ͷ�񼰳��ƺ�
	private String getNewFriendhead(HttpServletRequest request) {
		String _ids = RequestUtil.getString(request, "ids");
		String result = "[]";
		if(!"".equals(_ids)){
			List<Object> params = new ArrayList<Object>();
			String perParams = "";
			if(_ids.indexOf(",")!=-1){
				String []ids = _ids.split(",");
				if(ids.length>0){
					for(String id : ids){
						if(Check.isLong(id.substring(2))){
							perParams +=",?";
							params.add(Long.valueOf(id.substring(2)));
						}
					}
				}
			}else {
				String id =_ids.substring(2);
				if(Check.isLong(id)){
					perParams = ",?";
					params.add(Long.valueOf(id));
				}
			}
			if(!params.isEmpty()){
				List<Map<String, Object>> userList = onlyService.getAllMap("select u.id,u.hx_name,wx_imgurl,car_number from user_info_tb u left join car_info_tb c on c.uin = u.id " +
						" where u.id in("+perParams.substring(1)+")", params);
				List<Map<String, Object>> ulList = new ArrayList<Map<String,Object>>();
				List<Long> uList = new ArrayList<Long>();
				if(userList!=null&&!userList.isEmpty()){
					for(Map<String, Object> map: userList){
						Long uin =(Long)map.get("id");
						if(uList.contains(uin))
							continue;
						else {
							uList.add(uin);
						}
						String carNumber = (String)map.get("car_number");
						if(carNumber==null||carNumber.equals(""))
							carNumber = "���ƺ�δ֪";
						else if(carNumber.length()==7)
							carNumber = carNumber.substring(0,4)+"***"+carNumber.substring(6);
						map.put("car_number", carNumber);
						map.remove("id");
						ulList.add(map);
					}
					result= StringUtils.createJson(ulList);
				}
			}
		}
		logger.error("result:"+result);
		return result;
	}
	//�����˻��黷���˻�
	private String getHxName(HttpServletRequest request) {
		Long euin = RequestUtil.getLong(request, "id", -1L);
		String ret  ="{\"result\":\"-1\",\"errmsg\":\"���������ڣ�\",\"hxname\":\"\"}";
		if(euin>0){
			boolean isFriendAddHx =false;
			//��ѯ���ӳ����Ƿ���ע�ỷ���˻�
			Map userMap = onlyService.getMap("select id,hx_name from user_info_tb where id =? ", new Object[]{euin});
			if(userMap!=null&&!userMap.isEmpty()){
				String hxName = (String)userMap.get("hx_name");
				if(hxName==null||hxName.equals("")){
					String hxPass = publicMethods.getHXpass(euin);
					isFriendAddHx= HXHandle.reg("hx"+euin,hxPass);
					if(isFriendAddHx){
						int r = service.update("update user_info_tb set hx_name=?,hx_pass=? where id = ?", new Object[]{"hx"+euin,hxPass,euin});
						isFriendAddHx = r==1?true:false;
						if(r==1)
							ret  ="{\"result\":\"1\",\"errmsg\":\"\",\"hxname\":\""+hxName+"\"}";
					}
				}else {
					ret  ="{\"result\":\"1\",\"errmsg\":\"\",\"hxname\":\""+hxName+"\"}";
				}
			}
		}
		return ret;
	}
	//�ӻ��ź���
	private String addFriend(HttpServletRequest request) {
		String ret  ="{\"result\":\"0\",\"errmsg\":\"���ʧ��\"}";
		//���ӵĳ���
		String fid = RequestUtil.getString(request, "id");
		String mobile = RequestUtil.getString(request, "mobile");
		Long buin =-1L;
		//����
		if(!"".equals(fid)&&!mobile.equals("")){
			boolean isFriendAddHx =false;
			Long euin = -1L;
			if(Check.isLong(fid.substring(2)))
				euin = Long.valueOf(fid.substring(2));
			else {
				ret  ="{\"result\":\"-1\",\"errmsg\":\"���������ڣ�\"}";
				return ret;
			}
			//��ѯ���ӳ����Ƿ���ע�ỷ���˻�
			Map userMap = onlyService.getMap("select id,hx_name from user_info_tb where id =? ", new Object[]{euin});
			if(userMap!=null&&!userMap.isEmpty()){
				String hxName = (String)userMap.get("hx_name");
				if(hxName==null||hxName.equals("")){
					String hxPass = publicMethods.getHXpass(euin);
					isFriendAddHx= HXHandle.reg("hx"+euin,hxPass);
					if(isFriendAddHx){
						int r = service.update("update user_info_tb set hx_name=?,hx_pass=? where id = ?", new Object[]{"hx"+euin,hxPass,euin});
						isFriendAddHx = r==1?true:false;
					}
				}else {
					isFriendAddHx = true;
				}
			}else {
				ret  ="{\"result\":\"-1\",\"errmsg\":\"���������ڣ�\"}";
			}
			boolean isOwnAddHx=false;
			if(isFriendAddHx){
				userMap = onlyService.getMap("select id,hx_name from user_info_tb where mobile=? and auth_flag=?  ", new Object[]{mobile,4});
				if(userMap!=null&&!userMap.isEmpty()){
					String hxName = (String)userMap.get("hx_name");
					buin = (Long)userMap.get("id");
					if(hxName==null||hxName.equals("")){
						String hxPass = publicMethods.getHXpass(buin);
						isOwnAddHx= HXHandle.reg("hx"+euin,publicMethods.getHXpass(buin));
						if(isOwnAddHx){
							int r = service.update("update user_info_tb set hx_name=?,hx_pass=? where id = ?", new Object[]{"hx"+buin,hxPass,buin});
							isOwnAddHx = r==1?true:false;
						}
					}else {
						isOwnAddHx=true;
					}
				}else {
					ret  ="{\"result\":\"-2\",\"errmsg\":\"�����˻��쳣��\"}";
				}
			}
			if(isOwnAddHx&&isFriendAddHx){//˫�����Ѽ��뻷���˻����Ǽǲ�zld���ѱ�
				Long count = onlyService.getLong("select count(ID) from user_friend_tb where buin=? and euin=? ", new Object[]{buin,euin});
				if(count<1){
					Map preAddMap = onlyService.getMap("select atype from user_preaddfriend_tb where buin=? and euin=? ", new Object[]{euin,buin});
					Integer atype=1;
					logger.error(mobile+",addfriend,preAddMap:"+preAddMap);
					if(preAddMap!=null&&!preAddMap.isEmpty()){
						atype = (Integer)preAddMap.get("atype");
						int r = service.update("delete from user_preaddfriend_tb where buin=? and euin=?", new Object[]{euin,buin});
						logger.error(mobile+",addfriend,preAddMap deleted:"+r);
					}
					int r1 = service.update("insert into user_friend_tb (buin,euin,ctime,atype,is_add_hx)" +
							"values(?,?,?,?,?)", new Object[]{buin,euin,System.currentTimeMillis()/1000,atype,1});
					logger.error(mobile+",�ӳ������ź��ѣ�"+euin+",ret:"+r1);
					if(r1==1){
						int r2=service.update("insert into user_friend_tb (buin,euin,ctime,atype,is_add_hx)" +
								"values(?,?,?,?,?)", new Object[]{euin,buin,System.currentTimeMillis()/1000,atype,1});
						logger.error(euin+",�������ź��ѣ�"+mobile+",ret:"+r2);
						if(r2==1){
							ret  ="{\"result\":\"1\",\"errmsg\":\"��ӳɹ���\"}";
							//��˫������Ϣ
							logger.error(mobile+",buin:"+buin+",euin:"+euin);
							logger.error("buin-euin sendmessage:"+HXHandle.sendMsg("hx"+buin, "hx"+euin, "�Է�ͨ������ĺ������������Ѿ��Ǻ����ˡ�"));
							logger.error("euin-buin sendmessage:"+HXHandle.sendMsg("hx"+euin, "hx"+buin, "��ͨ���˶Է��ĺ������������Ѿ��Ǻ����ˡ�" ));
						}
					}
				}else {
					ret  ="{\"result\":\"1\",\"errmsg\":\"�����Ѿ��Ǻ�����\"}";
				}
			}
		}else {
			ret  ="{\"result\":\"-3\",\"errmsg\":\"���ʧ�ܣ����Ժ����ԣ�\"}";
		}
		logger.error(mobile+",buin:"+buin+",euin:"+fid+",ret:"+ret);
		return ret;
	}

	//��ѯ�������ͣ���ĳ���
	private String parkcars(HttpServletRequest request) {
		Long comId = RequestUtil.getLong(request, "id", -1L);
		String mobile = RequestUtil.getString(request, "mobile");
		Long uin = getUinByMobile(mobile);
		logger.error(uin+",mobile:"+mobile+",comId:"+comId);
		if(comId>0){
			List<Map<String, Object>> userList = onlyService.getAll("select u.id,wx_imgurl,car_number from user_info_tb u left join car_info_tb c on c.uin = u.id " +
					" where u.id in(select uin from order_tb where comid =? and uin not in(?,?) order by id desc) ", new Object[]{comId,uin,-1L});
			if(userList!=null&&!userList.isEmpty()){
				List<Map<String, Object>> friendList = onlyService.getAll("select euin from user_friend_tb where buin=?  ", new Object[]{uin});
				List<Map<String, Object>> preFriendList= onlyService.getAll("select buin,euin from user_preaddfriend_tb where buin=? or euin=?  ", new Object[]{uin,uin});
				logger.error("preFriendList:"+preFriendList);
				List<Long> friends = new ArrayList<Long>();
				if(friendList!=null&&!friendList.isEmpty()){
					for(Map<String, Object> fmap: friendList){
						friends.add((Long)fmap.get("euin"));
					}
				}
				for(Map<String, Object> map: userList){
					String carNumber = (String)map.get("car_number");
					if(carNumber==null||carNumber.equals(""))
						carNumber = "���ƺ�δ֪";
					else if(carNumber.length()==7)
						carNumber = carNumber.substring(0,4)+"***"+carNumber.substring(6);
					map.put("car_number", carNumber);
//					String wxImgUrl = (String)map.get("wx_imgurl");
//					if(wxImgUrl==null||"".equals(wxImgUrl)){
//						wxImgUrl="images/bunusimg/logo.png";
//						map.put("wx_imgurl", wxImgUrl);
//					}
					map.put("isfriend", "0");
					Long cid = (Long)map.get("id");
					if(friends.contains(cid)){
						map.put("isfriend", "1");
					}else if(preFriendList!=null&&!preFriendList.isEmpty()){
						for(Map<String, Object> fMap : preFriendList){
							Long buin = (Long)fMap.get("buin");
							Long euin = (Long)fMap.get("euin");
							if(buin.equals(uin)&&euin.equals(cid)){
								map.put("isfriend", "2");
								break;
							}else if(buin.equals(cid)&&euin.equals(uin)){
								map.put("isfriend", "3");
								break;
							}
						}
					}
					//map.remove("id");
				}
				String ret = StringUtils.createJson(userList);
				ret = ret.replace("null", "");
				logger.error(ret);
				return ret;
			}
		}
		return "[]";
	}

	//��ѯȥͣ�����ĳ���
	private String getParks(HttpServletRequest request) {
		String mobile = RequestUtil.getString(request, "mobile");
		Long uin = getUinByMobile(mobile);
		if(uin>0){
			List parkList = onlyService.getAll("select id,company_name as name from com_info_tb where id in(" +
					"select comid  from order_tb where uin = ? order by id desc) limit ? ", new Object[]{uin,10});
			if(parkList!=null&&!parkList.isEmpty())
				return StringUtils.createJson(parkList);
		}
		return "[]";
	}

	private String preResTicketUion(HttpServletRequest request) {
		//http://localhost/zld/carinter.do?action=preresticketuion&mobile=18811157723&id=426
		String mobile = RequestUtil.getString(request, "mobile");
		Long id = RequestUtil.getLong(request, "id", -1L);
		logger.error(mobile+",id:"+id);
		Map<String, Object> retMap = ZldMap.getMap(new String[]{"result","errmsg"} , new Object[]{1,"���Ժ���"});
		Map uionMap = service.getMap("select * from ticket_uion_tb where id =?  ", new Object[]{id});
		
		if(uionMap!=null){
			Integer s = (Integer)uionMap.get("state");
			if(s==0){
				
				String _touin = RequestUtil.getString(request, "touin");
				String _uin = RequestUtil.getString(request, "uin");
				Long touin=-1L;
				if(_touin.indexOf("hx")!=-1)
					touin= Long.valueOf(_touin.substring(2));
				Long uin =-1L;
				if(_uin.indexOf("hx")!=-1)
					uin= Long.valueOf(_uin.substring(2));
				Long count  = service.getLong("select count(id) from ticket_uion_tb where ((req_uin =? and res_uin=?) or (res_uin =? and req_uin=?) ) and req_time > ?", 
						new Object[]{uin,touin,uin,touin,TimeTools.getToDayBeginTime()});
				logger.error(uin+".....resuin:"+uin+",requin:"+touin+",���պ��������"+count);
				if(count>0){
					retMap.put("result", "-2");
					retMap.put("errmsg", "���������Ѿ�������������ٺ����ˣ�Ҫע������Ӵ��");
					logger.error("preResTicketUion>>>>>>>>info:"+retMap);
					return StringUtils.createJson(retMap);
				}
				
				Map<String, Object> reqMap= service.getMap("select id,state,limit_day,money,resources,limit_day,pmoney from ticket_tb" +
						" where id = ? ", new Object[]{uionMap.get("req_tid")});
				Integer state = (Integer)reqMap.get("state");
				if(state!=null&&state==1){
					retMap.put("result", "-1");
					retMap.put("errmsg", "ͣ��ȯ��ʹ��");
				}else {
					Long limitDay = (Long)reqMap.get("limit_day");
					if(limitDay<(System.currentTimeMillis()/1000)){
						retMap.put("result", "-1");
						retMap.put("errmsg", "ͣ��ȯ�ѹ���");
					}
				}
			}
		}else {
			retMap = ZldMap.getMap(new String[]{"result","errmsg"} , new Object[]{-1,"���������Ѳ�����"});
		}
		logger.error("preResTicketUion>>>>>>>>info:"+retMap);
		return StringUtils.createJson(retMap);
	}

	/**
	 * ����ǰ������Ϣ....
	 * @param request
	 * @return
	 */
	private String ticketUionInfo(HttpServletRequest request) {
		//http://192.168.199.240/zld/carinter.do?action=ticketuioninfo&mobile=13641309140&tid=46401&id=461
		String mobile = RequestUtil.getString(request, "mobile");
		Long tid = RequestUtil.getLong(request, "tid", -1L);
		Long id = RequestUtil.getLong(request, "id", -1L);
		logger.error(mobile+",id:"+id+",tid:"+tid);
		Long uin = getUinByMobile(mobile);
		//�����������ĺ���ֵ
		Map reqTicketMap = null;
		Map uionMap = service.getMap("select * from ticket_uion_tb where id =? and state=? ", new Object[]{id,0});
		Long pretid =-1L;
		Long reqNumber=0L;
		Long ntime = TimeTools.getToDayBeginTime();
		int ret =1;
		String mesg ="";
		Integer reqmoney= 0;//�������ĺ���ֵ
		String friend="{}";
		String own ="{}";
		if(uionMap!=null&&!uionMap.isEmpty()){
			pretid =(Long)uionMap.get("req_tid");
			reqTicketMap= service.getMap("select id,money,resources,limit_day,pmoney from ticket_tb where id = ? and state=? ", new Object[]{pretid,0});
			if(reqTicketMap!=null&&!reqTicketMap.isEmpty()){
				reqmoney=(Integer)reqTicketMap.get("money");
				Integer res =  (Integer)reqTicketMap.get("resources");
				Long limitDay = (Long)reqTicketMap.get("limit_day");
				reqNumber = (reqmoney*2+(limitDay-ntime)/(24*60*60))*(res+1);
				friend ="{\"uiontotal\":\""+reqNumber+"\"}";
			}else {
				ret=-4;
				mesg = "��������ʧ��,ͣ��ȯ��ʹ��";
			}
		}else {
			ret=-3;
			mesg = "��������ʧ�ܣ�������ȡ��";
		}
		
		//������Ӧ����ĺ���ֵ
		Long resNumber=0L;//��Ӧ����ĺ���ֵ
		Integer resmoney =0;
		Map resTicketMap = null;
		if(reqNumber>0){
			resTicketMap = service.getMap("select id,money,resources,limit_day,pmoney from ticket_tb where id = ? and state=? ", new Object[]{tid,0});
			if(resTicketMap!=null&&!resTicketMap.isEmpty()){
				resmoney = (Integer)resTicketMap.get("money");
				if((resmoney+reqmoney)%2==0){
					if(reqmoney%2==0){
						mesg = "����ʧ��,˫��ͬΪż��";
						ret =-2;
					}else{
						mesg = "����ʧ��,˫��ͬΪ����";
						ret=-1;
						
					}
					logger.error(mobile+">>>>>����ʧ�ܣ�"+mesg);
				}else {
					Integer res =  (Integer)resTicketMap.get("resources");
					Long limitDay = (Long)resTicketMap.get("limit_day");
					resNumber = (resmoney*2+(limitDay-ntime)/(24*60*60))*(res+1);
					own="{\"ticketvalue\":\""+resmoney*2+"\",\"expvalue\":\""+(limitDay-ntime)/(24*60*60)+"\",\"buyvalue\":\""+(res+1)+"\",\"uiontotal\":\""+resNumber+"\"}";
				}
			}else {
				ret =-5;
				mesg = "��Ӧ�����ͣ��ȯ�ѹ���";
			}
		}
		Map<String, Object> resultMap = ZldMap.getMap(new String[]{"result","errmsg","own","friend","winrate"}, 
				new Object[]{ret,mesg,own,friend,resNumber+"/"+(resNumber+reqNumber)});
		String result = StringUtils.createJson(resultMap);
		logger.error(uin+",��Ӧ���壬������Ϣ��"+result);
		return result;
	}

	/**
	 * ȡ���ź���ͷ��
	 * @param request
	 * @return ΢������΢��ͷ���ַ���������ԭ�򣬳��ƺ�
	 */
	private String getHxHeads(HttpServletRequest request) {
		String mobile = RequestUtil.getString(request, "mobile");
		String hxName = RequestUtil.getString(request, "hxname");
		String ret  ="{}";
		Long uin = getUinByMobile(mobile);
		if("".equals(hxName)){//ֻ��mobil����ʱ��ȡ���к�����Ϣ
			//����΢������ͷ���ַ
			List friends = onlyService.getAll("select id,wx_name,wx_imgurl from user_info_tb where  id in " +
					"(select euin from user_friend_tb where buin=? and is_add_hx=?)", new Object[]{uin,1});
			//������Դ
			List fList = onlyService.getAll("select euin,atype from user_friend_tb where buin=?   and is_add_hx=? ",
					new Object[]{uin,1});
			//���ѳ���
			List carList = onlyService.getAll("select uin,car_number from car_info_tb where uin in " +
					"(select euin from user_friend_tb where buin=? and is_add_hx=?) and state=? order by id desc", new Object[]{uin,1,1});
			if(friends!=null&&!friends.isEmpty()){
				for(int i=0;i<friends.size();i++){
					Map map = (Map)friends.get(i);
					Long id = (Long)map.get("id");
					//������Դ
					for(int j=0;i<fList.size();j++){
						Map map2 = (Map)fList.get(j);
						Long fuin = (Long)map2.get("euin");
						if(id.equals(fuin)){
							Integer atype = (Integer)map2.get("atype");
							if(atype!=null){
								if(atype==0)
									map.put("source","��һ�");
								else if(atype==1)
									map.put("source","ͬ��������");
								else {
									map.put("source","δ֪");
								}
							}
							break;
						}
					}
					//���ó���
					for(int k=0;k<carList.size();k++){
						Map map2 = (Map)carList.get(k);
						Long fuin = (Long)map2.get("uin");
						if(id.equals(fuin)){
							String carNumber = (String)map2.get("car_number");
							if(carNumber!=null&&carNumber.length()==7)
								carNumber=carNumber.substring(0,4)+"***"+carNumber.substring(6);
							map.put("carnumber",carNumber);
							break;
						}
					}
					map.put("id", "hx"+map.get("id"));
				}
			}
			ret = StringUtils.createJson(friends);
		}else {//ȡ����������Ϣ
			Long fuin = Long.valueOf(hxName.substring(2));
			Map userMap = onlyService.getMap("select id,wx_name,wx_imgurl from user_info_tb where id =? ", new Object[]{uin});
			Map fMap = onlyService.getMap("select euin,atype from user_friend_tb where buin=? and euin=?  and is_add_hx=? ",
					new Object[]{uin,fuin,1});
			Integer atype = (Integer)fMap.get("atype");
			if(atype!=null&&atype==0)
				userMap.put("source","��һ�");
			else {
				userMap.put("source","δ֪");
			}
			userMap.put("id", "hx"+userMap.get("id"));
			userMap.put("carnumber", publicMethods.getCarNumber(fuin));
			ret = StringUtils.createJson(userMap);
		}
		ret = ret.replace("null", "");
		//logger.error(">>>>friends:"+ret);
		return ret;
	}
	private String quickPay(HttpServletRequest request) {
		String mobile = RequestUtil.getString(request, "mobile");
		Long uin = getUinByMobile(mobile);
		List<Object> params = new ArrayList<Object>();
		params.add(uin);
		params.add(1);
		params.add(-1);
		Long time = System.currentTimeMillis()/1000-30*24*60*60;
		params.add(time);
		List<Map<String, Object>> list = onlyService.getAllMap("select uid as id,max(end_time)paytime from order_tb where uin = ?  and state = ? and  uid<>? and end_time >? group by uid order by paytime desc",
				params);
		if(list!=null&&list.size()>0){
			for(Map map:list){
				long uid = Long.parseLong(map.get("id")+"");
				params.clear();
				params.add(uid);
				params.add(0);
				Map<String, Object> usermap = onlyService.getMap("select u.nickname as name ,u.online_flag  online,c.company_name parkname from user_info_tb u,com_info_tb c where u.id=? and u.state=? and u.comid=c.id ",params);
				map.putAll(usermap);
			}
		}
		String ret = StringUtils.createJson(list);
		logger.error(">>>uin:"+uin+"���֧���Ľ��:"+ret);
		return ret ;
	}
	/**
	 * ����΢�Ź��ں����µ�ַ
	 * @param request
	 * @return
	 */
	private String getWxpArtic(HttpServletRequest request) {
		//uoinrule,useticket,credit,backbalance
		//http://localhost/zld/carinter.do?action=getwxpcartic&mobile=13641309140&artictype=backbalance
		String atype = RequestUtil.getString(request, "artictype");
		if(atype.equals("uoinrule"))//ͣ��ȯ�ϲ�����˵��
			return "http://mp.weixin.qq.com/s?__biz=MzA4MTAxMzA2Mg==&mid=209338628&idx=1&sn=d40db1b84727c85eb6e557113ec44cb1#rd";
		else if(atype.equals("useticket"))//ͣ����ͣ��ȯʹ��˵��
			return "http://mp.weixin.qq.com/s?__biz=MzA4MTAxMzA2Mg==&amp;mid=208427587&amp;idx=1&amp;sn=6cec3794e585e4d31b5079f919b01614#rd";
		else if(atype.equals("credit"))//ͣ�������ö��˵��
			return "http://mp.weixin.qq.com/s?__biz=MzA4MTAxMzA2Mg==&amp;mid=208427120&amp;idx=1&amp;sn=6cb6719bf1520ef5a72097fe5c7fe56a#rd";
		else if(atype.equals("backbalance"))//���ڳ����˻�����˵��
			return "http://mp.weixin.qq.com/s?__biz=MzA4MTAxMzA2Mg==&amp;mid=209376960&amp;idx=1&amp;sn=369c4bea18d70d656c4f3e30b86cc843#rd";
		return "";
	}
	/**
	 * �鿴��������
	 * @param request
	 * @return
	 */
	private String viewTikcetUion(HttpServletRequest request) {
		Long id = RequestUtil.getLong(request, "id", -1L);
		String mobile = RequestUtil.getString(request, "mobile");
		Long uin = getUinByMobile(mobile);
		logger.error(mobile+",id:"+id);
		Map uionMap = service.getMap("select * from ticket_uion_tb where id =? ", new Object[]{id});
		Map<String, Object> retMap = ZldMap.getMap(new String[]{"result","errmsg"} , new Object[]{-1,""});
		if(uionMap!=null&&!uionMap.isEmpty()){
			Long winUin = (Long)uionMap.get("win_uin");
			Long reqUin = (Long)uionMap.get("req_uin");
			Long resUin = (Long)uionMap.get("res_uin");
			
			//�������ĳ���ͣ��ȯ
			Map reqMap = service.getMap("select t.resources,t.money,t.state,t.limit_day,wx_imgurl from ticket_tb t left join user_info_tb u on u.id =t.uin where t.id=?", new Object[]{uionMap.get("req_tid")});
			//��Ӧ����ĳ���ͣ��ȯ
			Map resMap = service.getMap("select resources,money,wx_imgurl from ticket_tb t left join user_info_tb u on u.id =t.uin where t.id=?", new Object[]{uionMap.get("res_tid")});
			boolean isOwnWin=false;//�Ƿ����Լ�Ӯ��
			boolean isReqOwn=false;//�������Ƿ����Լ�
			if(winUin!=null){
				Integer reqMoney = (Integer)uionMap.get("req_money");
				Integer resMoney = (Integer)uionMap.get("res_money");
				if(reqUin.equals(uin))
					isReqOwn=true;
				if(winUin.equals(uin))//�������ĳ������Լ�
					isOwnWin=true;
				String friendCar ="";
				if(isReqOwn){//�������ĳ����Ƿ����Լ�
					friendCar = publicMethods.getCarNumber(resUin);
					if(friendCar.length()==7){
						friendCar = friendCar.substring(0,4)+"***"+friendCar.substring(6);
					}
					if(winUin==-1){
						retMap = ZldMap.getMap(new String[]{"ownticket"},new Object[]{"{\"name\":\"�ҵ�ͣ��ȯ\",\"money\":\""+reqMoney+"\",\"isbuy\":\""+reqMap.get("resources")+"\"}"});
						retMap = ZldMap.getAppendMap(retMap,new String[]{"friendticket"}, new Object[]{"{\"name\":\"���Գ���"+friendCar+"��ͣ��ȯ\",\"money\":\""+resMoney+"\",\"isbuy\":\""+resMap.get("resources")+"\"}"});
					}else {
						if(isOwnWin){
							retMap = ZldMap.getMap(new String[]{"ownticket"}, new Object[]{"{\"name\":\"�ҵ�ͣ��ȯ\",\"money\":\""+(reqMoney)+"\",\"isbuy\":\""+reqMap.get("resources")+"\"}"});
							retMap = ZldMap.getAppendMap(retMap,new String[]{"friendticket"}, new Object[]{"{\"name\":\"���Գ���"+friendCar+"��ͣ��ȯ\",\"money\":\""+resMoney+"\",\"isbuy\":\""+resMap.get("resources")+"\"}"});
						}else {
							retMap = ZldMap.getMap(new String[]{"ownticket"}, new Object[]{"{\"name\":\"�ҵ�ͣ��ȯ\",\"money\":\""+reqMoney+"\",\"isbuy\":\""+reqMap.get("resources")+"\"}"});
							retMap = ZldMap.getAppendMap(retMap,new String[]{"friendticket"}, new Object[]{"{\"name\":\"���Գ���"+friendCar+"��ͣ��ȯ\",\"money\":\""+(resMoney)+"\",\"isbuy\":\""+resMap.get("resources")+"\"}"});
						}
					}
				}else {
					friendCar = publicMethods.getCarNumber(reqUin);
					if(friendCar.length()==7){
						friendCar = friendCar.substring(0,4)+"***"+friendCar.substring(6);
					}
					if(winUin==-1){
						retMap = ZldMap.getMap(new String[]{"ownticket"},new Object[]{"{\"name\":\"�ҵ�ͣ��ȯ\",\"money\":\""+resMoney+"\",\"isbuy\":\""+resMap.get("resources")+"\"}"});
						retMap = ZldMap.getAppendMap(retMap,new String[]{"friendticket"}, new Object[]{"{\"name\":\"���Գ���"+friendCar+"��ͣ��ȯ\",\"money\":\""+reqMoney+"\",\"isbuy\":\""+reqMap.get("resources")+"\"}"});
					}else {
						if(isOwnWin){
							retMap = ZldMap.getMap(new String[]{"ownticket"},new Object[]{"{\"name\":\"�ҵ�ͣ��ȯ\",\"money\":\""+(resMoney)+"\",\"isbuy\":\""+resMap.get("resources")+"\"}"});
							retMap = ZldMap.getAppendMap(retMap,new String[]{"friendticket"}, new Object[]{"{\"name\":\"���Գ���"+friendCar+"��ͣ��ȯ\",\"money\":\""+reqMoney+"\",\"isbuy\":\""+reqMap.get("resources")+"\"}"});
						}else {
							retMap = ZldMap.getMap(new String[]{"ownticket"},new Object[]{"{\"name\":\"�ҵ�ͣ��ȯ\",\"money\":\""+resMoney+"\",\"isbuy\":\""+resMap.get("resources")+"\"}"});
							retMap = ZldMap.getAppendMap(retMap,new String[]{"friendticket"}, new Object[]{"{\"name\":\"���Գ���"+friendCar+"��ͣ��ȯ\",\"money\":\""+(reqMoney)+"\",\"isbuy\":\""+reqMap.get("resources")+"\"}"});
							
						}
					}
				}
				if(winUin==-1){//����ʧ��
					retMap= ZldMap.getAppendMap(retMap,new String[]{"result","errmsg"},new Object[]{0,"����ʧ��"});
					retMap= ZldMap.getAppendMap(retMap,new String[]{"ownret","friendret"},new Object[]{
							"{\"imgurl\":\""+(isReqOwn?reqMap.get("wx_imgurl"):resMap.get("wx_imgurl"))+"\",\"toptip\":\"ʧȥ"+(isReqOwn?reqMap.get("money"):resMap.get("money"))+"Ԫͣ��ȯ\",\"righttip\":\"ʧ��\"}",
							"{\"imgurl\":\""+(isReqOwn?resMap.get("wx_imgurl"):reqMap.get("wx_imgurl"))+"\",\"toptip\":\"ʧȥ"+(isReqOwn?resMap.get("money"):reqMap.get("money"))+"Ԫͣ��ȯ\",\"righttip\":\"ʧ��\"}"});
				}else{//����ɹ�
					retMap= ZldMap.getAppendMap(retMap,new String[]{"result","errmsg"},new Object[]{1,"����ɹ�"});
					String ownTip1 = "";
					String ownTip2 = "";
					String ownRightTip = "";
					String friendTip1 = "";
					String friendTip2 = "";
					String friendRightTip = "";
					reqMoney = (Integer)reqMap.get("money");
					resMoney = (Integer)resMap.get("money");
					if(isOwnWin){
						ownTip1="���"+(isReqOwn?reqMoney:resMoney)+"Ԫͣ��ȯ";
						ownTip2="������ŵڶ�����Ϸ�ʸ�";
						ownRightTip = "��ʤ";
						friendTip1="ʧȥ"+(isReqOwn?resMoney:reqMoney)+"Ԫͣ��ȯ";
						friendTip2="���һ�ŵڶ�����Ϸ�ʸ�";
						friendRightTip = "ʧ��";
					}else{
						friendTip1="���"+(isReqOwn?resMoney:reqMoney)+"Ԫͣ��ȯ";
						friendTip2="������ŵڶ�����Ϸ�ʸ�";
						friendRightTip = "��ʤ";
						ownTip1="ʧȥ"+(isReqOwn?reqMoney:resMoney)+"Ԫͣ��ȯ";
						ownTip2="���һ�ŵڶ�����Ϸ�ʸ�";
						ownRightTip = "ʧ��";
					}
					retMap= ZldMap.getAppendMap(retMap,new String[]{"ownret","friendret"},new Object[]{
							"{\"win\":\""+(isOwnWin?1:0)+"\",\"imgurl\":\""+(isReqOwn?reqMap.get("wx_imgurl"):resMap.get("wx_imgurl"))+"\",\"toptip\":\""+ownTip1+"\",\"buttip\":\""+ownTip2+"\",\"righttip\":\""+ownRightTip+"\"}",
							"{\"win\":\""+(isOwnWin?0:1)+"\",\"imgurl\":\""+(isReqOwn?resMap.get("wx_imgurl"):reqMap.get("wx_imgurl"))+"\",\"toptip\":\""+friendTip1+"\",\"buttip\":\""+friendTip2+"\",\"righttip\":\""+friendRightTip+"\"}"});
				}
			}else {
				Integer state = (Integer)reqMap.get("state");
				if(state!=null&&state==1){
					retMap.put("errmsg", "ͣ��ȯ��ʹ��");
				}else {
					Long limitDay = (Long)reqMap.get("limit_day");
					if(limitDay<(System.currentTimeMillis()/1000))
						retMap.put("errmsg", "ͣ��ȯ�ѹ���");
				}
				Integer reqMoney = (Integer)reqMap.get("money");
				retMap = ZldMap.getAppendMap(retMap, new String[]{"ownticket"}, new Object[]{"{\"money\":\""+reqMoney+"\",\"isbuy\":\""+reqMap.get("resources")+"\",\"name\":\"�ҵ�ͣ��ȯ\"}"});
			}
		}
		//http://192.168.199.240/zld/carinter.do?action=viewticketuion&mobile=15801482643&id=125
		//http://192.168.199.240/zld/carinter.do?action=viewticketuion&mobile=18811157723&id=8
		logger.error(retMap);
		return StringUtils.createJson(retMap);
	}

	/**
	 * ��ɺ����ͣ��ȯ
	 * @param request
	 * @return
	 */
	private String getUionTickets(HttpServletRequest request) {
		 //http://192.168.199.240/zld/carinter.do?action=getuiontickets&mobile=15801482643&page=1
		Integer page = RequestUtil.getInteger(request, "page", 1);
		String mobile = RequestUtil.getString(request, "mobile");
		List<Object> params = new ArrayList<Object>();
		Long uin = getUinByMobile(mobile);
		String sql = "select id,money,resources,type,state,pmoney,limit_day as limitday from ticket_tb  where uin = ?" +
				" and type=? and limit_day>? and state=? and money<? and id not in" +
				"(select req_tid from ticket_uion_tb where req_uin=? and state=? ) order by money ,limit_day";//order by limit_day";
		Long ntime =TimeTools.getToDayBeginTime();
		params.add(uin);
		params.add(0);
		params.add(ntime);
		params.add(0);
		params.add(12);
		params.add(uin);
		params.add(0);
		List<Map<String, Object>> ticketMap = onlyService.getAll(sql, params, page, 50);
		//����û�з��ص��˻��е�ͣ��ȯ
		//logger.error(">>>>");
		String result = "[]";
		if(ticketMap!=null&&!ticketMap.isEmpty()){
			for(Map<String, Object> tMap : ticketMap){
				Long limitDay = (Long)tMap.get("limitday");
				if(ntime >limitDay)
					tMap.put("exp", 0);
				else {
					tMap.put("exp", 1);
				}
				Integer res = (Integer)tMap.get("resources");
				tMap.put("isbuy",res);
				tMap.put("iscanuse",1);
				tMap.remove("resources");
				Integer money = (Integer)tMap.get("money");
				Integer topMoney = CustomDefind.getUseMoney(money.doubleValue(), 1);
				tMap.put("desc", "��"+topMoney+"Ԫ���Եֿ�ȫ��");
				if(res==1){
					tMap.put("desc", "��"+(money+1)+"Ԫ���Եֿ�ȫ��,<br/>���ں��˻� <font color='#32a669'>"+StringUtils.formatDouble(tMap.get("pmoney"))+"</font> Ԫ�������˻�");
				}
			}
			result= StringUtils.createJson(ticketMap).replace("null", "");
		}
		return result;
	}
	/**
	 * ��Ӧ���壬���������
	 * @param request
	 * @return
	 */
	private String resTikcetUion(HttpServletRequest request) {
		String mobile = RequestUtil.getString(request, "mobile");
		Long tid = RequestUtil.getLong(request, "tid", -1L);
		Long id = RequestUtil.getLong(request, "id", -1L);
		logger.error(mobile+",id:"+id+",tid:"+tid);
		Long uin = getUinByMobile(mobile);
		/**
		 * �������
			ֻ��һ���˳��˵�������һ���˳���˫������ȯ�����ܺ���ɹ���
			����ʧ�ܺ����˵�ͣ��ȯ����ʧȥ��
			����ɹ��󣬽�����ˣ��õ�����ͣ��ȯ֮�ͣ����12Ԫ�����õ����ŵڶ�����Ϸ�ʸ񿨡�
			���С���ˣ�ͣ��ȯʧȥ�����õ�һ�ŵڶ�����Ϸ�ʸ񿨡�
			��Ϸ�ʸ񿨲�����ڡ�
			�̶��������ˣ�ÿ��ֻ�ܺ���һ�Σ�Ҫע�����彡����
			�����ͣ��ȯ�ͷǹ����ͣ��ȯ����󣬹����ͣ��ȯ
			����ɹ���ͣ��ȯ������Ч�ڰ����̵��㡣
			������ݺ���ֵ���жϡ�

			1����           ��Ԫͣ��ȯ�õ����*2�ĺ���ֵ��
			2����Ч��           ÿ����1����Ч�ڣ�����1�ֺ���ֵ��
			3���Ƿ���        ����ĺ���ֱֵ�ӳ���5.
				����ֵ����Ӯ�ú���ĸ��ʡ�
				���磬���1Ԫͣ��ȯ������2�쵽�ڣ��ǹ������� ��ͣ��ȯ����ֵΪ4
				���2Ԫȯ������3�쵽�ڣ��������ͣ��ȯ����ֵΪ35.
				�����ǵ���Ӯ���ʱ�Ϊ35:4
				����ɹ���ͣ��ȯ���Ϊ����ͣ��ȯ���֮�ͣ�����12����Ч�ڰ�Ӯ���㣬˭Ӯ�ˣ���Ч�ڱ��˭�ģ�������˿���䡣
		 */
		String mesg = "��������ʧ��";
		int ret = 0;//������ -4��0 ʧ�ܣ�1�ɹ�,������峵��Ӯ��2�ɹ�����Ӧ����Ӯ
		Long reqNumber=0L;
		Long ntime = System.currentTimeMillis()/1000;
		Integer reqmoney =0;
		Map reqTicketMap = null;
		Map uionMap = service.getMap("select * from ticket_uion_tb where id =? and state=? ", new Object[]{id,0});
		Long pretid =-1L;
		Long preUin = -1L;
		Long reqtime = null;
		if(uionMap!=null&&!uionMap.isEmpty()){
			pretid =(Long)uionMap.get("req_tid");
			preUin = (Long)uionMap.get("req_uin");
			reqtime =(Long)uionMap.get("req_time");
			reqTicketMap= service.getMap("select id,money,resources,limit_day,pmoney from ticket_tb where id = ? and state=? ", new Object[]{pretid,0});
			if(reqTicketMap!=null&&!reqTicketMap.isEmpty()){
				reqmoney=(Integer)reqTicketMap.get("money");
				Integer res =  (Integer)reqTicketMap.get("resources");
				Long limitDay = (Long)reqTicketMap.get("limit_day");
				if(limitDay<ntime){
					ret=-4;
					mesg = "��������ʧ�ܣ��Է�ͣ��ȯ�ѹ���";
				}else
					reqNumber = (reqmoney*2+(limitDay-ntime)/(24*60*60))*(res+1);
			}else {
				ret=-2;
				mesg = "��������ʧ��,ͣ��ȯ��ʹ��";
			}
		}else {
			ret=-3;
			mesg = "��������ʧ�ܣ�������ȡ��";
		}
		
		Long resNumber=0L;
		Integer resmoney =0;
		Long winUin=-1L;
		
		Map resTicketMap = null;
		Long winTicketId=-1L;
		Long loseTicketId=-1L;//ʧ�ܳ�����ͣ��ȯ���
		Long limitAddtime = 0L;//Ӯȯ����Ч���ۼ�ֵ,��ǰʱ���ȥ�������Ӧ�ϲ�ʱ��ʱ�� 
		boolean isBuy = false;
		if(reqNumber>0){
			resTicketMap = service.getMap("select id,money,resources,limit_day,pmoney from ticket_tb where id = ? and state=? ", new Object[]{tid,0});
			if(resTicketMap!=null&&!resTicketMap.isEmpty()){
				resmoney = (Integer)resTicketMap.get("money");
				if((resmoney+reqmoney)%2==0){
					int r = service.update("update ticket_tb set state=? where id in(?,?)", new Object[]{1,pretid,tid});
					if(reqmoney%2==0)
						mesg = "����ʧ��,˫��ͬΪż��";
					else
						mesg = "����ʧ��,˫��ͬΪ����";
					ret=-1;
					logger.error(mobile+">>>>>����ʧ��,��������ͣ��ȯ��"+r);
				}else {
					Integer res =  (Integer)resTicketMap.get("resources");
					Long limitDay = (Long)resTicketMap.get("limit_day");
					resNumber = (resmoney*2+(limitDay-ntime)/(24*60*60))*(res+1);
					Integer rand = new Random().nextInt(reqNumber.intValue()+resNumber.intValue());
					logger.error(preUin+",�������ĺ���ֵ ��"+reqNumber+"��"+uin+",��Ӧ����ĺ���ֵ ��"+resNumber+",rand:"+rand);
					if(rand+1<=reqNumber){
						winUin=preUin;
						ret=1;
						winTicketId=pretid;
						loseTicketId=tid;
						res =  (Integer)reqTicketMap.get("resources");
						mesg = "����ɹ���������峵��Ӯ";
						limitAddtime  = ntime-reqtime;
						addSecondGameCard(preUin,2);
						addSecondGameCard(uin,1);
					}else {
						winUin=uin;
						ret=2;
						winTicketId=tid;
						loseTicketId=pretid;
						mesg = "����ɹ�����Ӧ���峵��Ӯ";
						addSecondGameCard(preUin,1);
						addSecondGameCard(uin,2);
					}
					if(res==1)
						isBuy =true;
					logger.error("���壺win:"+winUin);
					
				}
			}
		}
		
		if(ret>0){
			Integer total =reqmoney+resmoney;
			if(total>12)
				total=12;
			Double utotal = StringUtils.formatDouble(reqTicketMap.get("pmoney"))+StringUtils.formatDouble(resTicketMap.get("pmoney"));
			if(utotal>12)
				utotal=12.0;
			if(!isBuy)
				utotal=0.0;
			int r = service.update("update ticket_tb set money =?,pmoney=?,limit_day=limit_day + ? where id=? ",
					new Object[]{total,utotal,limitAddtime,winTicketId});
			logger.error("reqticket:"+reqTicketMap);
			logger.error("resticket:"+resTicketMap);
			logger.error("nowtime:"+ntime+",limitAddtime:"+limitAddtime);
			logger.error("win:"+winTicketId+",�ϲ���ȯ��"+total+",�˿��� ��"+utotal);
			logger.error(mobile+",����ɹ���Ӯ����"+ret+"(1:������峵��Ӯ,2:��Ӧ����Ӯ),����ͣ��ȯ��"+r);
			
			r=service.update("update ticket_uion_tb set res_tid=?, res_uin=?,res_time=?, win_uin=?,state=?,res_money=? where id=? ",
					new Object[]{tid,uin,System.currentTimeMillis()/1000,winUin,1,resmoney,id});
			r = service.update("update ticket_tb set state=? where id =? ", new Object[]{1,loseTicketId});
		}else {
			int r=service.update("update ticket_uion_tb set res_tid=?, res_uin=?,res_time=?,win_uin=?,state=?,res_money=? where id=?",
					new Object[]{tid,uin,System.currentTimeMillis()/1000,winUin,1,resmoney,id});
		}
		if(ret==1){
			Map userMap=onlyService.getMap("select mobile from user_info_tb where id =? ", new Object[]{preUin});
			mobile= userMap==null?"":userMap.get("mobile")+"";
		}else if(ret!=2){
			mobile="";
		}
		Map<String, Object> resultMap = ZldMap.getMap(new String[]{"result","errmsg","id","winner"}, new Object[]{ret,mesg,id,mobile});
		return StringUtils.createJson(resultMap);
		//http://192.168.199.240/zld/carinter.do?action=resticketuion&mobile=15801482643&tid=44878&id=8
	}
	/**
	 * �ӵڶ����볡ȯ
	 * @param uin
	 * @param number
	 */
	private void addSecondGameCard(Long uin,Integer number){
		int ret =0;
		for(int i=0;i<number;i++){
			ret +=service.update("insert into flygame_score_tb (uin,fgid,remark,ptype,money,ctime) values(?,?,?,?,?,?)",
						new Object[]{uin,-1,"�ڶ������",6,0,System.currentTimeMillis()/1000});
		}
		logger.error(uin+"������"+number+"�ŵڶ����볡ȯ.ret:"+ret);
	}
	
	/**
	 * ��ѯ�Ƿ���Ժ��壬��ͬ����������һ��ֻ�ܺ���һ��
	 * @param request
	 * @return
	 */
	private String preTicketUion(HttpServletRequest request) {
		//http://192.168.199.240/zld/carinter.do?action=preticketuion&uin=hx21766&touin=hx21691
		String _touin = RequestUtil.getString(request, "touin");
		String _uin = RequestUtil.getString(request, "uin");
		String result = "{\"result\":\"0\",\"errmsg\":\"���������Ѿ�������������ٺ����ˣ�Ҫע������Ӵ��\"}";
		Long touin =-1L;
		if(_touin.indexOf("hx")!=-1)
			touin= Long.valueOf(_touin.substring(2));
		Long uin =-1L;
		if(_uin.indexOf("hx")!=-1)
			uin= Long.valueOf(_uin.substring(2));
		if(uin>0&&touin>0){
			Long ttime = TimeTools.getToDayBeginTime();
			Long count  = service.getLong("select count(id) from ticket_uion_tb where ((req_uin =? and res_uin=?) or (res_uin =? and req_uin=?) )  and req_time > ?", 
					new Object[]{uin,touin,uin,touin,ttime});
			logger.error(uin+".....resuin:"+uin+",requin:"+touin+",���պ��������"+count);
			if(count<1)
				result = "{\"result\":\"1\",\"errmsg\":\"���Ժ���\"}";
		}
		logger.error("preticketuion,uin:"+uin+",touin:"+touin+", result:"+result);
		return result;
	}
	
	/**
	 * �����������
	 * @param request
	 * @return
	 */
	private String reqTikcetUion(HttpServletRequest request) {
		
		String mobile = RequestUtil.getString(request, "mobile");
		//��Ӧ������
		String _touin = RequestUtil.getString(request, "touin");
		Long touin =-1L;
		if(_touin.indexOf("hx")!=-1)
			touin= Long.valueOf(_touin.substring(2));
		Long uin = getUinByMobile(mobile);
		Long tid = RequestUtil.getLong(request, "tid", -1L);
		int ret = 0;
		Long key =-1L;
		logger.error("ticket uion:resuin:"+uin+",requin:"+touin);
		logger.error(mobile+",tid:"+tid);
		String mesg = "��������ʧ��";
		if(uin>0&&tid>0&&touin>0){
			Map ticketMap = onlyService.getMap("select id,money from ticket_tb where id =? ", new Object[]{tid});
			if(ticketMap!=null){
				Long ttime = TimeTools.getToDayBeginTime();
				Long count  = service.getLong("select count(id) from ticket_uion_tb where ((req_uin =? and res_uin=?) or (res_uin =? and req_uin=?) ) and req_time > ?", 
						new Object[]{uin,touin,uin,touin,ttime});
				logger.error(uin+".....resuin:"+uin+",requin:"+touin+",���պ��������"+count);
				if(count<1){
					key = service.getkey("seq_ticket_uion_tb");
					ret = service.update("insert into ticket_uion_tb(id,req_uin,req_tid,req_time,req_money) values(?,?,?,?,?)", 
							new Object[]{key,uin,tid,System.currentTimeMillis()/1000,ticketMap.get("money")});
				}else {
					ret =-1;
					mesg = "���������Ѿ�������������ٺ����ˣ�Ҫע������Ӵ��";
				}
			}
		}
		if(ret==1)
			mesg = "��������ɹ�";
		Map userMap = onlyService.getMap("select wx_name,wx_imgurl,car_number from " +
				"user_info_tb u left join car_info_tb c on c.uin=u.id where u.id=?", new Object[]{uin});
		Map<String, Object> resultMap = ZldMap.getMap(new String[]{"result","errmsg","wxname","wximgurl","carnumber","id"}, 
				new Object[]{ret,mesg,userMap.get("wx_name"),userMap.get("wx_imgurl"),userMap.get("car_number"),key});
		//http://192.168.199.240/zld/carinter.do?action=reqticketuion&mobile=15210932334&tid=44807&touin=
		String result = StringUtils.createJson(resultMap);
		logger.error("reqticketuion,uin:"+uin+",touin:"+touin+", result:"+result);
		return result;
	}

	/**
	 * ����ɨ�賵λ��ά�����ɶ���
	 * @param request
	 * @return
	 */
	private String addOrder(HttpServletRequest request) {
		//���ݶ�ά��鳵λ��Ϣ
		String mobile =RequestUtil.getString(request, "mobile");
		Long uin = getUinByMobile(mobile);
		String carNumber = publicMethods.getCarNumber(uin);
		Long cid = RequestUtil.getLong(request, "cid", -1L);
		String result = "{\"result\":\"0\",\"errmsg\":\"���ɶ���ʧ��\"}";
		Map parkMap = onlyService.getMap("select * from com_park_tb where qid=?", new Object[]{cid});
		if(parkMap!=null&&!parkMap.isEmpty()){//�鵽�˳�λ
			Long count = service.getLong("select count(id) from com_park_tb where state=? " +
					" and order_id>? and id=? ", new Object[]{1, 0, parkMap.get("id")});
			logger.error("count:"+count);
			if(count == 0){
				Long key = service.getkey("seq_order_tb");
				Map uidMap = service.getMap("select id from user_info_tb where comid=? ", new Object[]{parkMap.get("comid")});
				int ret = service.update("insert into order_tb (id,create_time,comid,uin,c_type,car_number,state,uid) values(?,?,?,?,?,?,?,?)", 
						new Object[]{key,System.currentTimeMillis()/1000,parkMap.get("comid"),uin,6,carNumber,0,uidMap.get("id")});
				if(ret==1){
					ret = service.update("update com_park_tb set state=? ,order_id=? where id=?", new Object[]{1,key,parkMap.get("id")});
					logger.error("(update com_park_tb orderid):"+key+",id:"+parkMap.get("id"));
					if(ret==1){
						result = "{\"result\":\"1\",\"errmsg\":\"�����ɹ�\"}";
					}
				}
			}
		}
		logger.error(mobile+",ɨ��ά�����ɶ���cid:"+cid+",result :"+result);
		return result;
	}
	
	/**
	 * ����������ͣ��ȯ
	 * @param request
	 * @return
	 */
	private String buyTicket(HttpServletRequest request) {
		String mobile  = RequestUtil.getString(request, "mobile");
		Map userMap = onlyService.getPojo("select is_auth from user_info_Tb where mobile=? and auth_flag=? ", new Object[]{mobile,4});
		Integer isAuth = 0;
		if(userMap!=null){
			isAuth=(Integer) userMap.get("is_auth");
		}
		Double authdiscount = StringUtils.formatDouble(CustomDefind.getValue("AUTHDISCOUNT"))*10;
		Double noauthdiscount = StringUtils.formatDouble(CustomDefind.getValue("NOAUTHDISCOUNT"))*10;
		return "{\"isauth\":\""+isAuth+"\",\"auth\":\""+authdiscount+"\",\"notauth\":\""+noauthdiscount+"\"}";
	}
	/**
	 * ��ѯ���г���
	 * @param request
	 * @return
	 */
	private String getCarNumbers(HttpServletRequest request) {
		String openid  = RequestUtil.getString(request, "openid");
		//Long uin = getUinByMobile(mobile);
		Long uin = getUinByOpenid(openid);
		List<Map<String, Object>> carList = onlyService.getAll("select id,car_number,is_auth " +
				"from car_info_tb where uin=? and state =? ", new Object[]{uin,1});
		Integer state=0;
		if(carList!=null&&!carList.isEmpty()){
			List<Integer> sList = new ArrayList<Integer>();
			for(Map<String, Object> car : carList){
				sList.add((Integer)car.get("is_auth"));
			}
			if(sList.size()==1)
				state = sList.get(0);
			else {
				if(sList.contains(-1))
					state=-1;
				else if (sList.contains(2)){
					state=2;
				}else if(sList.contains(0)){
					state=0;
				}else if(sList.contains(1))
					state=1;
			}
			for(Map<String, Object> car : carList){
				Integer isAuth = (Integer)car.get("is_auth");
				//����carnumber��ѯ�¿�,����Ч�����¿��û�,���hasprod 1,����0
				if(isAuth.equals(state))
					car.put("is_default", "1");
				else {
					car.put("is_default", "0");
				}
			}
			logger.error(carList);
			return StringUtils.createJson(carList);
		}
		return "[]";
	}

	/**
	 * �ϴ�������ʻ֤
	 * @param request
	 * @return
	 * @throws Exception
	 */
	private String uploadCarPics2Mongodb (HttpServletRequest request) throws Exception{
		//logger.error("begin upload user picture....");
		String mobile =RequestUtil.getString(request, "mobile");
		String carid =AjaxUtil.decodeUTF8(RequestUtil.getString(request, "carnumber"));
		String oldcarid =AjaxUtil.decodeUTF8(RequestUtil.getString(request, "old_carnumber"));
		logger.error("begin upload user picture....mobile:"+mobile+",carnumber:"+carid);
		Map<String, String> extMap = new HashMap<String, String>();
	    extMap.put(".jpg", "image/jpeg");
	    extMap.put(".jpeg", "image/jpeg");
	    extMap.put(".png", "image/png");
	    extMap.put(".gif", "image/gif");
	    extMap.put(".webp", "image/webp");
		if(mobile.equals("")){
			return  "{\"result\":\"-3\",\"errmsg\":\"�ֻ���Ϊ�գ�\"}";
		}
		if(carid.equals("")){
			return  "{\"result\":\"-4\",\"errmsg\":\"���ƺ�Ϊ�գ�\"}";
		}
		
		logger.error("mobile:"+mobile+",carnumber:"+carid+",oldcarnumber"+oldcarid);
		Map userMap = getUserByMobile(mobile);
		Long uin = getUinByMobile(mobile);
		if(uin==null||uin==-1){
			return  "{\"result\":\"-5\",\"errmsg\":\"�ֻ���δע�ᣡ\"}";
		}
		carid = carid.toUpperCase();
		oldcarid = oldcarid.toUpperCase();
		List<Map<String, Object>> carList  = service.getAll("select car_number from car_info_Tb where uin=? ", new Object[]{uin}); 
		boolean isHasCarId = false;
		boolean isUpdate = false;
		int cnum=0;
		if(carList!=null&&!carList.isEmpty()){
			cnum=carList.size();
			for(Map<String, Object> cMap: carList){
				String carNumber = (String)cMap.get("car_number");
				if(carid.equals(carNumber)){//�����Ѵ���
					isHasCarId = true;
				}
				if(carNumber.equals(oldcarid))
					isUpdate=true;
			}
		}
		if(!isUpdate){
			if(!oldcarid.equals(""))
				return "{\"result\":\"-5\",\"errmsg\":\"ԭ���Ʋ����ڣ����ܸ��£�\"}";
			else if(isHasCarId&&oldcarid.equals(""))
				return "{\"result\":\"-6\",\"errmsg\":\"���Ʋ����ظ�ע�ᣡ\"}";
		}
		if(!isUpdate&&!oldcarid.equals(""))
			return "{\"result\":\"-5\",\"errmsg\":\"ԭ���Ʋ����ڣ����ܸ��£�\"}";
		
		
		if(isUpdate&&!oldcarid.equals("")&&!carid.equals(oldcarid)){//��Ҫ����ԭ����
			Long ccount = service.getLong("select count(id) from car_info_Tb where car_number=? ", new Object[]{carid});
			if(ccount>0){
				return "{\"result\":\"-6\",\"errmsg\":\"������ע��������������룡\"}";
			}
			int rs = service.update("update car_info_tb set car_number=? where uin=? and car_number=? ", new Object[]{carid,uin,oldcarid});
			logger.error("carowner:"+mobile+",update car_number,old:"+oldcarid+",new:"+carid+",ret:"+rs);
			if(rs==1){
				publicMethods.syncUserAddPlateNumber(uin, oldcarid, carid);
			}
		}else {
			if(!isHasCarId){
				if(cnum<3){
					Long ccount = service.getLong("select count(id) from car_info_Tb where car_number=? ", new Object[]{carid});
					if(ccount>0){
						return "{\"result\":\"-6\",\"errmsg\":\"������ע��������������룡\"}";
					}
					int ret = service.update("insert into car_info_Tb (uin,car_number,state,create_time) values(?,?,?,?) ",
							new Object[]{uin,carid,1,System.currentTimeMillis()/1000});
					if(ret==1){
						isHasCarId = true;
						publicMethods.syncUserAddPlateNumber(uin, carid, "");
					}
					logger.error("carowner:"+mobile+",add car_number:"+carid+",ret:"+ret+",curr carnumber count:"+(cnum+1));
				}else {
					return "{\"result\":\"-2\",\"errmsg\":\"�����ѳ���������\"}";
				}
			}
		}
		
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
			return "{\"result\":\"1\",\"errmsg\":\"���Ʊ���ɹ���\"}";
		}
		String filename = ""; // �ϴ��ļ����浽���������ļ���
		InputStream is = null; // ��ǰ�ϴ��ļ���InputStream����
		// ѭ�������ϴ��ļ�
		int index =0;
		for (FileItem item : items){
			// ������ͨ�ı���
			if (!item.isFormField()){
				// �ӿͻ��˷��͹������ϴ��ļ�·���н�ȡ�ļ���
				filename = item.getName().substring(item.getName().lastIndexOf("\\")+1);
				is = item.getInputStream(); // �õ��ϴ��ļ���InputStream����
				logger.error("index:"+index+",filename:"+item.getName()+",stream:"+is);
				System.err.println("index:"+index+",1========="+is.available());
				System.err.println("index:"+index+",2========="+item.getContentType());
			}else
				continue;
			String file_ext =filename.substring(filename.lastIndexOf(".")).toLowerCase();// ��չ��
			String picurl = uin + "_"+index+"_"+ System.currentTimeMillis()/1000 + file_ext;
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
				
				DBCollection collection = mydb.getCollection("user_dirvier_pics");
				//  DBCollection collection = mydb.getCollection("records_test");
				
				BasicDBObject document = new BasicDBObject();
				document.put("uin", uin);
				document.put("carid", carid);
				document.put("ctime",  System.currentTimeMillis()/1000);
				document.put("type", extMap.get(file_ext));
				document.put("content", content);
				document.put("filename", picurl);
				//��ʼ����
				System.out.println("index:"+index+",��ʼд��dB");
				//��������
				mydb.requestStart();
				System.out.println(collection.insert(document));
				//��������
				mydb.requestDone();
				System.out.println("index:"+index+",д����dB");
				in.close();        
				is.close();
				byteout.close();
				
				String sql = "update car_info_tb set pic_url1=?,is_auth=?,create_time=?  where uin=? and car_number=?";
				if(index==1){
					sql = "update car_info_tb set pic_url2=?,is_auth=?,create_time=?  where uin=? and car_number=?";
				}
				int ret = service.update(sql, new Object[]{picurl,2,System.currentTimeMillis()/1000,uin,carid});
				logger.error("��"+index+"��ͼƬ��ret:"+ret);
				index++;
			} catch (Exception e) {
				e.printStackTrace();
				return "{\"result\":\"0\",\"errmsg\":\"ͼƬ�ϴ�ʧ�ܣ�\"}";
			}finally{
				if(in!=null)
					in.close();
				if(byteout!=null)
					byteout.close();
				if(is!=null)
					is.close();
			}
		}
		return "{\"result\":\"1\",\"errmsg\":\"�ϴ��ɹ���\"}";
	}
	
	/**
	 * ��������
	 * @param request
	 * @return
	 */
	private String pcommDetail(HttpServletRequest request) {
		Long uid = RequestUtil.getLong(request, "uid", -1L);
		Integer page = RequestUtil.getInteger(request, "page", 1);
		Integer size = RequestUtil.getInteger(request, "size", 20);
		String ret = "[]";
		if(uid!=-1){
			List<Map<String, Object>> cList = onlyService.getPage("select comments as info,ctime,uin from parkuser_comment_tb  " +
					" where uid=? order by ctime desc ", new Object[]{uid},page,size);
			if(cList!=null&&!cList.isEmpty()){
				for(Map<String, Object> map : cList){
					Long uin = (Long)map.get("uin");
					if(uin!=null){
						String carNumber = publicMethods.getCarNumber(uin);
						if(!carNumber.equals("���ƺ�δ֪"))
							map.put("user", carNumber);
						else {
							map.put("user", "");
						}
					}
				}
				ret = StringUtils.createJson(cList);
			}
		}
		return ret;
	}
	/***�շ�Ա����,���أ�������������һ�ܷ���������յ��������������յ���������**/
	private String puserDetail(HttpServletRequest request) {
		Long uid = RequestUtil.getLong(request, "uid", -1L);
		String ret = "{}";
		if(uid!=-1){
			//���з������
			Long scount= onlyService.getLong("select count(ID) from order_tb where uid=? and state=? ", new Object[]{uid,1});
			//һ�ܷ������
			Long wcount= onlyService.getLong("select count(ID) from order_tb where uid=? and state=? and end_time >? ",
					new Object[]{uid,1,TimeTools.getToDayBeginTime()-7*24*60*60});
			Map<String,Object> rMap = onlyService.getMap("select count(ID) rcount,sum(money) money,uid from parkuser_reward_tb" +
					" where uid =?  group by uid ", new Object[]{uid});
			//������
			Long ccount = onlyService.getLong("select count(ID) from parkuser_comment_tb where uid=? ", new Object[]{uid});
			Map userMap = onlyService.getMap("select mobile from user_info_tb where id =?", new Object[]{uid});
			if(rMap==null){
				rMap = new HashMap<String,Object>();
				rMap.put("pcount", 0);
				rMap.put("money", 0);
			}else {
				rMap.remove("uid");
			}
			rMap.put("scount", scount);
			rMap.put("wcount", wcount);
			rMap.put("ccount", ccount);
			if(userMap!=null)
				rMap.put("mobile", userMap.get("mobile"));
			else {
				rMap.put("mobile", "");
			}
			ret = StringUtils.createJson(rMap);
		}
		ret = ret.replace("null", "");
		return ret;
	}
	//����޸��ֻ�����
	private String editMobile(HttpServletRequest request) {
		String omobile = RequestUtil.getString(request, "omobile");
		String nmobile =RequestUtil.getString(request, "nmobile");
		Map nuserMap = service.getMap("select id,wxp_openid from user_info_tb where mobile=? and auth_flag=? ", new Object[]{nmobile,4});
		int ret = 0;
		Long uin = -1L;
		if(nuserMap!=null){
			String openid = (String)nuserMap.get("wxp_openid");
			uin = (Long)nuserMap.get("id");
			if(openid!=null&&openid.length()>2)
				return "-1";
		}else {//�������˻��������˻�
			uin = publicMethods.regUser(nmobile, 1000L,-1L,false);//ע�Ტ��Ԫͣ��ȯ
		}
		Map userMap = service.getMap("select id,wxp_openid,wx_name,wx_imgurl from user_info_tb where mobile=? and auth_flag=? ", new Object[]{omobile,4});
		if(uin!=null&&uin!=-1&&userMap!=null&&userMap.get("wxp_openid")!=null){
			ret = service.update("update user_info_tb set wxp_openid=?,wx_name=?,wx_imgurl=? where id=?  ",  
					new Object[]{userMap.get("wxp_openid"),userMap.get("wx_name"),userMap.get("wx_imgurl"),uin});
			if(ret==1){
				ZldMap.removeUser(uin);
				ret = service.update("update user_info_tb set wxp_openid=?,wx_name=?,wx_imgurl=? where id=? ", new Object[]{null,null,null,userMap.get("id")});
				logger.error(">>>>>������޸��ֻ��ţ� ԭ�ֻ� ��"+omobile+"�����ֻ� ��"+nmobile);
			}
		}
		return  ret+"";
	}

	private int doPreGetWeixinBonus(HttpServletRequest request,Long id) {
		//�Ƿ���ں�����Ƿ�������
		String words = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "words"));
		Map bMap = service.getMap("select etime,bnum,bwords,money,exptime,uin from order_ticket_tb where id =? ", new Object[]{id});
		Long count= service.getLong("select count(id) from order_ticket_detail_tb where otid =? and uin is not null", new Object[]{id});
		int ret = 1;
		Integer bnum=0;
		Integer money = 0;
		if(bMap!=null){
			String bwords = (String)bMap.get("bwords");
			if(!words.equals("")){//����ף����
				service.update("update order_ticket_tb set bwords=? where id=? ", new Object[]{id});
				bwords = words;
			}
			if(bwords!=null&&bwords.length()>14)
				bwords = bwords.substring(0,11)+"...";
			Long exptime = (Long)bMap.get("exptime");
			bnum = (Integer)bMap.get("bnum");
			money = (Integer)bMap.get("money");
			Long ntime = System.currentTimeMillis()/1000;
			Map userMap = service.getMap("select wx_imgurl from user_info_tb where id=? ", new Object[]{bMap.get("uin")});
			if(userMap!=null&&userMap.get("wx_imgurl")!=null)
				request.setAttribute("carowenurl",userMap.get("wx_imgurl"));
			request.setAttribute("bwords",bwords);
			request.setAttribute("bnum",bnum);
			request.setAttribute("totalmoney",money);
			if(ntime>exptime&&count==0){//�ѹ���
				request.setAttribute("tipwords", "�ۿ�ȯ�ѹ���");
				ret =-1;
			}else {
				if(count==0){//û���۹��������ʼ�ۺ��
					if(bnum>0&&money>0){
						String insertSql = " insert into order_ticket_detail_tb (otid,amount,btype) values(?,?,?)";
						List<Object[]> values = new ArrayList<Object[]>();
						for(int i=0;i<bnum;i++){
							Object[] objects = new Object[]{id,money,1};
							values.add(objects);
						}
						int _ret = service.bathInsert(insertSql, values, new int[]{4,4,4});
						logger.error("����΢�������д����....."+_ret);
					}
				}else if(count.intValue()==bnum){//������
					getBonusList(request,id);
					request.setAttribute("tipwords", "�ۿ�ȯ������");
					ret = -2;
				}
			}
		}else {//������
			request.setAttribute("tipwords", "�ۿ�ȯ�ѹ���");
			ret =-3;
		}
		
		return ret;
	}
	/**�������*/
	private void getBonusList(HttpServletRequest request,Long bid){
		//���������ȡ�ĺ��
		List<Map<String, Object>> blList = service.getAll("select o.id,amount,ttime,wx_name,wx_imgurl,wxp_openid,o.ticketid,u.mobile from order_ticket_detail_tb o left join user_info_tb u on o.uin=u.id where otid=? and ttime is not null ", new Object[]{bid});
		String data = "[]";
		if(blList!=null&&!blList.isEmpty()){
			data = "[";
			for(Map<String, Object> map : blList){
				Long time = (Long)map.get("ttime");
				String wxname = (String)map.get("wx_name");
				String wxurl = (String)map.get("wx_imgurl");
				if(wxname==null){
					wxname = map.get("mobile")+"";
					if(wxname.length()>10){
						wxname = wxname.substring(0,3)+"****"+wxname.substring(7);
					}
				}
				if(wxurl==null){
					wxurl ="images/bunusimg/logo.png";
				}
				data +="{\"amount\":\""+StringUtils.formatDouble(map.get("amount"))+"\"," +
						"\"ttime\":\""+TimeTools.getTime_yyMMdd_HHmm(time*1000).substring(3)+"\"," +
						"\"wxname\":\""+wxname+"\",\"wxurl\":\""+wxurl+"\"},";
			}
			data = data.substring(0,data.length()-1);
			data +="]";
			request.setAttribute("haveget", blList.size());
		}
		request.setAttribute("havegetpic", "wxhaveget");
		request.setAttribute("data", data);
	}
	/**
	 * ��ѯ����ͣ��ȯ
	 * @param request
	 * @return
	 */
	private String getallTickets(HttpServletRequest request) {
		 //http://192.168.199.240/zld/carinter.do?action=gettickets&mobile=15801482643&type=0&page=1
		Integer page = RequestUtil.getInteger(request, "page", 1);
		Integer type = RequestUtil.getInteger(request, "type", 0);//0��ǰδʹ����δ���ڵ�ȯ 1����ʹ�û��ѹ��ڵ�ȯ
		String mobile = RequestUtil.getString(request, "mobile");
		String from = RequestUtil.processParams(request, "from");//����΢�Ź��ںźͿͻ���
		List<Object> params = new ArrayList<Object>();
		Long uin = getUinByMobile(mobile);
		String sql = "select * from ticket_tb where uin=? ";
		params.add(uin);
		if(!from.equals("wxpublic")){
			sql += " and type<? ";
			params.add(2);
		}
		
		Long btime =TimeTools.getToDayBeginTime();
		if(type==0){//��ǰδʹ����δ���ڵ�ȯ
			sql+=" and state=? and limit_day>=? order by limit_day ";
			params.add(0);
			params.add(btime);
		}else if(type==1){//����ʹ�û��ѹ��ڵ�ȯ
			sql+=" and ( state=? or limit_day<? ) order by id desc";
			params.add(1);
			params.add(btime);
		}
		List<Map<String, Object>> list = onlyService.getAll(sql, params, page, 20);
		String result = "[]";
		if(list != null){
			list = commonMethods.getTicketInfo(list, 2, -1L, 2);
			result= StringUtils.createJson(list).replace("null", "");
		}
		return result;
	}
	
	private String doVerifyPark(HttpServletRequest request) {
		String mobile = RequestUtil.getString(request, "mobile");
		Long uin = getUinByMobile(mobile);
		Long id = RequestUtil.getLong(request, "id", -1L);
		
		Integer isname = RequestUtil.getInteger(request, "isname", -1);
		Integer islocal = RequestUtil.getInteger(request, "islocal", -1);
		Integer ispay = RequestUtil.getInteger(request, "ispay", -1);
		Integer isresume = RequestUtil.getInteger(request, "isresume", -1);
		int ret = 0;//0���ʧ�� 1��˳ɹ� -1�ظ���� 
		if(id!=-1){
			Long count = service.getLong("select count(id) from park_verify_tb where uin =? and comid =?", new Object[]{uin,id});
			if(count>0)
				return "-1";
			ret = service.update("insert into park_verify_tb (isname,islocal,ispay,isresume,comid,uin,ctime) values(?,?,?,?,?,?,?)", 
					new Object[]{isname,islocal,ispay,isresume,id,uin,System.currentTimeMillis()/1000});
		}
		return ""+ret;
	}
	
	private String doPreVerifyPark(HttpServletRequest request) {
		String mobile = RequestUtil.getString(request, "mobile");
		String ids = AjaxUtil.decodeUTF8(RequestUtil.getString(request, "ids"));
		logger.error(">>>>VerifyPark,ids:"+ids);
		Long uin = getUinByMobile(mobile);
		Double lng = RequestUtil.getDouble(request, "lng", 0d);
		Double lat = RequestUtil.getDouble(request, "lat", 0d);
		double d1 = 0.382346;//20��������
		double d2 = 0.291792;//20��������
		//ÿ��ֻ���������δ��˵ĳ���
		Long count = onlyService.getLong("select count(ID) from park_verify_tb where uin =? and ctime >? ", new Object[]{uin,TimeTools.getToDayBeginTime()});
		if(count>2)
			return "{\"id\":\"-1\"}" ;
		String sql = "select id, company_name as name,resume as desc,type," +
				"longitude as lng ,latitude as lat from com_info_tb where longitude between ? and ? and latitude between ? and ? and  state =? " +
				"and upload_uin is not null and upload_uin !=? and id not in" +
				"(select comid from park_verify_tb where uin =? )  ";
		//Object [] values = new Object[]{lng-d1,lng+d1,lat-d2,lat+d2,2,uin,uin,1};
		List<Object> params = new ArrayList<Object>();
		params.add(lng-d1);
		params.add(lng+d1);
		params.add(lat-d2);
		params.add(lat+d2);
		params.add(2);
		params.add(uin);
		params.add(uin);
		if(!ids.equals("")){
//			if(ids.endsWith(","))
//				ids = ids.substring(0,ids.length()-1);
			if(ids.startsWith(","))
				ids = ids.substring(1);
			if(ids.indexOf(",")!=-1){
				String []_ids = ids.split(",");
				String preParams = "";
				
				for(int i=0;i<_ids.length;i++){
					if(_ids[i]==null||!Check.isNumber(_ids[i]))
						continue;
					if(i==0)
						preParams ="?";
					else {
						preParams +=",?";
					}
					params.add(Long.valueOf(_ids[i]));
				}
				sql +=" and id not in("+preParams+") ";
			}else {
				if(Check.isNumber(ids)){
					sql +=" and id not in(?) ";
					params.add(Long.valueOf(ids));
				}
			}
		}
		sql +=" order by id desc limit ?";
		params.add(1);
		Map uploadedCom = service.getMap(sql,params );
		logger.error("sql:"+sql+",/rparams"+params+" ,/rret:"+uploadedCom);
		if(uploadedCom!=null)
			return StringUtils.createJson(uploadedCom);
		
		return "{\"id\":\"-2\"}";
	}
	
	/*
	 * ��������
	 */
	private void doOrderDetail(HttpServletRequest request) {
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);
		Double prePay = RequestUtil.getDouble(request, "prepay",0d);
		Double back = RequestUtil.getDouble(request, "back",0d);
		Integer first_flag = RequestUtil.getInteger(request, "first_flag", 0);//0:���ױ�֧���� 1���ױ�֧��
		logger.error("doOrderDetail>>>>orderid:"+orderId+",prePay:"+prePay+",back:"+back+",first_flag:"+first_flag);
		if(orderId!=-1){
			Map orderMap = service.getMap("select * from order_tb where id =? ", new Object[]{orderId});
			request.setAttribute("prepay",prePay);
			Double total = StringUtils.formatDouble(orderMap.get("total"));
			request.setAttribute("total",total);
			if(prePay<total){
				request.setAttribute("addmoney",total-prePay);
				request.setAttribute("back_dp", "�������");
			}else if(prePay>total){
				request.setAttribute("back_dp", "�˻����");
				request.setAttribute("addmoney",back);
			}
			request.setAttribute("orderid",orderMap.get("id"));
			request.setAttribute("state",orderMap.get("state"));
			request.setAttribute("btime",TimeTools.getTime_yyyyMMdd_HHmm((Long)orderMap.get("create_time")*1000));
			request.setAttribute("etime",TimeTools.getTime_yyyyMMdd_HHmm((Long)orderMap.get("end_time")*1000));
			Long comid = (Long)orderMap.get("comid");
			if(comid!=null){
				Map comMap = onlyService.getMap("select company_name from com_info_tb where id=?", new Object[]{comid});
				if(comMap!=null)
					request.setAttribute("comname", comMap.get("company_name"));
			}
			Map bonusMap = onlyService.getMap("select * from order_ticket_tb where order_id=? limit ? ", new Object[]{orderId,1});
			if(bonusMap!=null&&!bonusMap.isEmpty()){
				logger.error(">>>��΢�ź��...");
				request.setAttribute("bonusid", bonusMap.get("id"));
				request.setAttribute("bonus_money", bonusMap.get("money"));
				request.setAttribute("bonus_bnum", bonusMap.get("bnum"));
				request.setAttribute("bonus_type", bonusMap.get("type"));
				request.setAttribute("first_flag", first_flag);
			}else{
				request.setAttribute("bonusid", -1);
			}
			//΢�Ź��ں�JSSDK��Ȩ��֤
			Map<String, String> result = new HashMap<String, String>();
			try {
				result = publicMethods.getJssdkApiSign(request);
			}catch (Exception e) {
				e.printStackTrace();
				
			}
			System.out.println(result);
			//jssdkȨ����֤����
			request.setAttribute("appid", Constants.WXPUBLIC_APPID);
			request.setAttribute("nonceStr", result.get("nonceStr"));
			request.setAttribute("timestamp", result.get("timestamp"));
			request.setAttribute("signature", result.get("signature"));
		}
	}

	private String getWxAccount(HttpServletRequest request) {
		//����������ͣ��ȯ
		String mobile = RequestUtil.getString(request, "mobile");
		Long uin = RequestUtil.getLong(request, "wxp_uin", -1L);
		Double total = RequestUtil.getDouble(request, "total", 0d);
		Long uid = RequestUtil.getLong(request, "uid", -1L);//ֱ��ʱ�����շ�Ա��� 
		logger.error("choose ditotal ticket of weixin>>>mobile:"+mobile+",uin:"+uin+",total:"+total+",uid:"+uid);
		Map<String, Object> userMap = null;
		if(!mobile.equals("")){
			userMap  = getUserByMobile(mobile);
			uin  = (Long)userMap.get("id");
		}else{
			userMap = getWxUserByUin(uin);
		}
		Object balance = userMap.get("balance");
		Map<String, Object> ticketMap = null;
		
		boolean parkuserblack = publicMethods.isBlackParkUser(uid, true);
		boolean userblack = publicMethods.isBlackUser(uin);
		boolean isCanuseCache = memcacheUtils.readUseTicketCache(uin);
		
		logger.error("uin:"+uin+",parkuserblack:"+parkuserblack+",parkuserblack:"+parkuserblack+",userblack:"+userblack+",isCanuseCache:"+isCanuseCache);
		if(uid > 0 && isCanuseCache && !parkuserblack && !userblack){
			Long count = onlyService.getLong("select count(*) from user_account_tb where uin=? and type=? ",
					new Object[] { uin, 1 });//�ж��Ƿ����ױ�֧��
			if(count == 0){
				logger.error("is first pay>>>uin:"+uin+",count:"+count);
				ticketMap = commonMethods.chooseDistotalTicket(uin, uid, total);
			}
		}
		
		//����û�з��ص��˻��е�ͣ��ȯ
		if(!mobile.equals("")){
			commonMethods.checkBonus(mobile, uin);
		}
		String ret = "{\"balance\":\""+balance+"\",\"tickets\":[]}";
		String tickets = "[";
		if(ticketMap!=null)
			tickets +=StringUtils.createJson(ticketMap);//"{\"id\":\""+ticketMap.get("id")+"\",\"money\":\""+ticketMap.get("money")+"\"}";
		tickets +="]";
		ret = ret.replace("[]", tickets);
		return ret;
		//���������ȯ http://127.0.0.1/zld/carowner.do?action=&mobile=15801270154&total=5
	}
	private String useTickets(HttpServletRequest request) {
		//http://192.168.199.240/zld/carinter.do?action=usetickets&mobile=15801482643&total=5&orderid=&uid=10700&preid=38878&utype=0
		String mobile = RequestUtil.getString(request, "mobile");
		Long preId = RequestUtil.getLong(request, "preid", -1L);
		Double total = RequestUtil.getDouble(request, "total", 0d);
		Long orderId = RequestUtil.getLong(request, "orderid", -1L);//��ͨ���������������
		Long uid = RequestUtil.getLong(request, "uid", -1L);//ֱ��ʱ�����շ�Ա��� 
		Integer utype = RequestUtil.getInteger(request, "utype", 0);//0����汾�������ȶ��������ȯ��1�ϰ汾���Զ�ѡȯʱ��������ͨȯ����ߵֿ۽�2�°汾������ȯ���ͷ�����ߵֿ۽��
		Integer ptype = RequestUtil.getInteger(request, "ptype", -1);//0�˻���ֵ��1���²�Ʒ��2ͣ���ѽ��㣻3ֱ��;4���� 5����ͣ��ȯ
		Integer source = RequestUtil.getInteger(request, "source", 0);//0�ͻ��� 1��΢�Ź��ں�
		
		Long parkId = null;
		Long uin = getUinByMobile(mobile);
		logger.error("userTickets>>uin:"+uin+",orderid:"+orderId+",uid:"+uid+"preid:"+preId+",total:"+total+",utype:"+utype+",ptype:"+ptype+",uid:"+uid);
		if(orderId != -1){
			Map<String, Object> orderMap = onlyService.getMap("select comid,uid from order_tb where id=?", new Object[]{orderId});
			if(orderMap!=null){
				parkId = (Long)orderMap.get("comid");
				uid = (Long)orderMap.get("uid");
			}
		}else if(uid != -1){
			Map<String, Object> userMap2 = onlyService.getMap("select comid from user_info_tb where id = ? ", new Object[]{uid});
			if(userMap2!=null)
				parkId = (Long)userMap2.get("comid");
		}
		boolean isAuth = publicMethods.isAuthUser(uin);
		logger.error("userTickets>>uin:"+uin+",orderid:"+orderId+",parkId:"+parkId+",uid:"+uid+",isAuth:"+isAuth);
		List<Map<String, Object>> userTikcets = new ArrayList<Map<String,Object>>();
		if(uid != -1 && parkId != null && total > 0){
			List<Map<String, Object>> list = commonMethods.chooseTicket(uin, total, utype, uid, isAuth, ptype, parkId, orderId, source);
			if(list != null){
				userTikcets = list;
			}
		}
		String result = StringUtils.createJson(userTikcets);
		logger.error("userTickets>>uin:"+uin+",orderid:"+orderId+",size:"+userTikcets.size());
		return result.replace("null", "");
	}

	private String upPark(HttpServletRequest request) {
		//ÿ��ֻ���ϴ���������
		String mobile = RequestUtil.getString(request, "mobile");
		Long uin = getUinByMobile(mobile);
		String parkname  = AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "parkname"));
		String desc  =  AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "desc"));
		String address  =  AjaxUtil.decodeUTF8(RequestUtil.processParams(request, "addr"));
		//System.out.println(parkname);
		Double	 lng = RequestUtil.getDouble(request, "lng", 0D);
		Double	 lat = RequestUtil.getDouble(request, "lat", 0D);
		Integer type = RequestUtil.getInteger(request, "type", 1);
		Long ntime = System.currentTimeMillis()/1000;
		int ret = 0;
		Long count = onlyService.getLong("select count(ID) from com_info_Tb where upload_uin=? and create_time >? ", 
				new Object[]{uin,TimeTools.getToDayBeginTime()});
		if(count>2)
			return "-1";
		if(lng!=0&&lat!=0){
			try {
				ret = service.update("insert into com_info_tb (company_name,resume,address,longitude,latitude,state," +
						"create_time,upload_uin,type)values(?,?,?,?,?,?,?,?,?)", new Object[]{parkname,desc,address,lng,lat,2,ntime,uin,type});
			} catch (Exception e) {
				ret=-2;
			}
		}
		return ret+"";
	}

	private String getComments(HttpServletRequest request) {
		Integer page = RequestUtil.getInteger(request, "page", 1);
		Long comId= RequestUtil.getLong(request, "comid", -1L);
		String mobile = RequestUtil.getString(request, "mobile");
		Long userId = getUinByMobile(mobile);
		List<Map<String, Object>> comList =onlyService.getPage("select * from com_comment_tb where comid=? order by id desc",
				new Object[]{comId},page,20);
		List<Map<String, Object>> resultMap = new ArrayList<Map<String,Object>>();
		boolean ishave=false;
		if(comList!=null&&comList.size()>0){
			for(Map<String, Object> map : comList){
				Map<String, Object> iMap = new HashMap<String, Object>();
				Long createTime = (Long)map.get("create_time");
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(createTime*1000);
				String times = TimeTools.getTime_MMdd_HHmm(createTime*1000);
				Long uin = (Long)map.get("uin");
				iMap.put("parkId",comId);// ���۵ĳ���ID
				iMap.put("date", times.substring(0,5));// �������ڣ�7-24
				iMap.put("week", "����"+StringUtils.getWeek(calendar.get(Calendar.DAY_OF_WEEK)));//�������������ڼ���������
				iMap.put("time", times.substring(6));// ���۵ĳ���ID
				iMap.put("info",  map.get("comment"));//�������ݣ���������һ�󴮷ϻ�������
				iMap.put("ctime",  createTime);
				iMap.put("user", publicMethods.getCarNumber(uin));// �����ߣ����������ƺţ���A***A111��
				if(uin.equals(userId)){
					ishave = true;
					resultMap.add(0,iMap);
				}else {
					resultMap.add(iMap);
				}
			}
			if(!ishave&&page==1&&userId!=-1){
				Map tMap = service.getMap("select * from com_comment_tb where comid=? and uin=? ", new Object[]{comId,userId});
				if(tMap!=null&&!tMap.isEmpty()){
					Map<String, Object> iMap = new HashMap<String, Object>();
					Long createTime = (Long)tMap.get("create_time");
					Calendar calendar = Calendar.getInstance();
					calendar.setTimeInMillis(createTime*1000);
					String times = TimeTools.getTime_MMdd_HHmm(createTime*1000);
					Long uin = (Long)tMap.get("uin");
					if(uin.equals(userId))
						ishave = true;
					iMap.put("parkId",comId);// ���۵ĳ���ID
					iMap.put("date", times.substring(0,5));// �������ڣ�7-24
					iMap.put("week", "����"+StringUtils.getWeek(calendar.get(Calendar.DAY_OF_WEEK)));//�������������ڼ���������
					iMap.put("time", times.substring(6));// ���۵ĳ���ID
					iMap.put("info",  tMap.get("comment"));//�������ݣ���������һ�󴮷ϻ�������
					iMap.put("ctime",  createTime);
					iMap.put("user", publicMethods.getCarNumber(uin));// �����ߣ����������ƺţ���A***A111��
					resultMap.add(0,iMap);
				}
			}
			return StringUtils.createJson(resultMap);
		}
		return "[]";
	}

	private String getParkInfo(HttpServletRequest request) {
		Long pid = RequestUtil.getLong(request, "comid", -1L);
		logger.error("comid:"+pid);
		String result = "{}";
		/*
		 * 
		 * id;// ͣ����ID
			 name;// ͣ��������
			 lng;// γ��
			 lat;// ����
			 free;// ���г�λ������ͬ��freespace��
			 price;// ��ǰ�۸�Ԫ/ÿСʱ��������ʾ��ѣ�������ʾ�м۸�0�򡰡���ʾû�м۸���Ϣ��
			 total;// �ܳ�λ
			 addr;// ͣ������ַ
			 phone;// ͣ�����绰
			 monthlypay;// �Ƿ�֧���¿�
			 epay;// �Ƿ�֧���ֻ�֧��
			 desc;// ��������
 			[photo_url];// ������Ƭ��url��ַ����
	["3","ͣ�������Գ������������Ʒ��","116.317565","40.043024","50","","50","�����б����к������ϵ���Ϣ·26��","","1","",["parkpics/3_1421131961.jpeg"]]
		 */
		//���������绰������/��λ������ͼƬ����ַ���۸�����
		if(pid!=null&&pid>0){
			
			Map<String,Object> comMap =null;
			if(pid<800000)
				comMap=onlyService.getMap("select id,longitude lng,latitude lat,epay,share_number,company_name as name,mobile phone," +
					"parking_total as total,address addr,remarks as desc ,type ,parking_type,empty " +
					"from com_info_tb where id =?", new Object[]{pid});
			else {
				comMap=onlyService.getMap("select id,longitude lng,latitude lat,epay,share_number,company_name as name,phone," +
						"parking_total as total,address addr,remarks as desc ,resume as price,type ,parking_type,empty " +
						"from union_park_tb where id =?", new Object[]{pid});
			}
			
			//��ͼƬ
			Map<String,Object> picMap = onlyService.getMap("select picurl from com_picturs_tb where comid=? order by id desc limit ?",
					new Object[]{pid,1});
			String picUrls = "";
			if(picMap!=null&&!picMap.isEmpty()){
				picUrls=(String)picMap.get("picurl");//"http://121.40.130.8/zld/parkpics/"+
			}
			/**ͣ������Ŀ***/
			if(pid<800000){
				Long free = 0L;
				
				Map<String, Object> map = onlyService.getMap("select sum(amount) free from remain_berth_tb where comid=? and state=? ", 
						new Object[]{pid, 0});
				if(map != null && map.get("free") != null){
					free = Long.valueOf(map.get("free") + "");
				}
				if(free>0)
					comMap.put("free", free);
				else {
					comMap.put("free", comMap.get("share_number"));
				}
				logger.error("get free lots>>>comid:"+pid+",free:"+free);
			}else {
				comMap.put("free", comMap.get("empty"));
			}
			/**ͣ������Ŀ***/
			//��۸�
			Integer type =(Integer)comMap.get("type");
			String price ="";
			if(type==0&&pid<800000){
				price = getPrice(pid);
				comMap.put("price", price);
			}
			comMap.put("photo_url", "[\""+picUrls+"\"]");
			comMap.remove("share_number");
			result = StringUtils.createJson(comMap);
		}
		return result.replace("null", "");
	}
	/**
	 * ��ͣ���۸�
	 * @param parkId
	 * @return
	 */
	private String getPrice(Long parkId){
		Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+8"));
		//��ʼСʱ
		int bhour = calendar.get(Calendar.HOUR_OF_DAY);
		List<Map<String,Object>> priceList=onlyService.getAll("select * from price_tb where comid=? " +
				"and state=? and pay_type=? order by id desc", new Object[]{parkId,0,0});
		if(priceList==null||priceList.size()==0){//û�а�ʱ�β���
			//�鰴�β���
			priceList=onlyService.getAll("select * from price_tb where comid=? " +
					"and state=? and pay_type=? order by id desc", new Object[]{parkId,0,1});
			if(priceList==null||priceList.size()==0){//û�а��β��ԣ�������ʾ
				return "0Ԫ/��";
			}else {//�а��β��ԣ�ֱ�ӷ���һ�ε��շ�
				Map<String,Object> timeMap =priceList.get(0);
				Integer unit = (Integer)timeMap.get("unit");
				if(unit!=null&&unit>0){
					if(unit>60){
						String t = "";
						if(unit%60==0)
							t = unit/60+"Сʱ";
						else
							t = unit/60+"Сʱ "+unit%60+"����";
						return timeMap.get("price")+"Ԫ/"+t;
					}else {
						return timeMap.get("price")+"Ԫ/"+unit+"����";
					}
				}else {
					return timeMap.get("price")+"Ԫ/��";
				}
				//return timeMap.get("price")+"Ԫ/��";
			}
			//�����Ÿ�����Ա��ͨ�����úü۸�
		}else {//�Ӱ�ʱ�μ۸�����зּ���ռ��ҹ���շѲ���
			if(priceList.size()>0){
				logger.info(priceList);
				for(Map<String,Object> map : priceList){
					Integer btime = (Integer)map.get("b_time");
					Integer etime = (Integer)map.get("e_time");
					Double price = Double.valueOf(map.get("price")+"");
					Double fprice = Double.valueOf(map.get("fprice")+"");
					Integer ftime = (Integer)map.get("first_times");
					if(ftime!=null&&ftime>0){
						if(fprice>0)
							price = fprice;
					}
					if(btime<etime){//�ռ� 
						if(bhour>=btime&&bhour<etime){
							return price+"Ԫ/"+map.get("unit")+"����";
						}
					}else {
						if(bhour>=btime||bhour<etime){
							return price+"Ԫ/"+map.get("unit")+"����";
						}
					}
				}
			}
		}
		return "0.0Ԫ/Сʱ";
	}
	private Long getUinByMobile(String mobile){
		if(!"".equals(mobile)){
			Map userMap = onlyService.getPojo("select id from user_info_Tb where mobile=? and auth_flag=? ", new Object[]{mobile,4});
			if(userMap!=null&&!userMap.isEmpty()){
				return (Long) userMap.get("id");
			}
		}
		return -1L;
	}
	
	private Long getUinByOpenid(String openid){
		if(!"".equals(openid)){
			Map userMap = onlyService.getPojo("select id from user_info_Tb where wxp_openid=? and auth_flag=? ", new Object[]{openid,4});
			if(userMap!=null&&!userMap.isEmpty()){
				return (Long) userMap.get("id");
			}
		}
		
		
		return -1l;
	}
	
	private String getParkNameByComid(Long comid){
		Map<String, Object> comMap = onlyService.getMap("select company_name from com_info_tb where id =? ",new Object[]{comid});
		if(comMap!=null)
			return (String)comMap.get("company_name");
		return "";
				
	}
	private Map<String, Object> getUserByMobile(String mobile){
		if(!"".equals(mobile)){
			Map userMap = onlyService.getPojo("select * from user_info_Tb where mobile=? and auth_flag=? ", new Object[]{mobile,4});
			if(userMap!=null){
				return userMap;
			}
		}
		return null;
	}
	private Map<String, Object> getWxUserByUin(Long uin){
		if(uin != -1){
			Map<String, Object> userMap = onlyService.getMap(
					"select * from wxp_user_tb where uin=? ",
					new Object[] { uin });
			if(userMap != null){
				return userMap;
			}
		}
		return null;
	}
	
	//��ѯ���׮��Ϣ
	private  List<Map<String, Object>> getChargeSite(){
		List<Map<String, Object>> resList = new ArrayList<Map<String,Object>>();
		//String url ="http://api.wyqcd.cn:8004/api/Sta/PostSta";
		String url =CustomDefind.getValue("RECHARGEURL");//"http://open.teld.cn/api/Sta/PostSta";
		//String url ="http://127.0.0.1/zld/carinter.do?action=RouteStatData";
		String signKey = "15JBfEs6QnRPiLMlN3SXZrLq9UvXXdY7";
		String token =getChargeAccToken();
		//String token ="VbuEV8c0pUXOlxPDS6bTnRPXe_4opVooEQyf1NR61FUM_kUWhBfljvjqChGTpc3ZoZPEMvNgDfq9U5QTLE3JPkOfRBVbGIHSvF-ff2VbuhPqp8vMi_V2nxkbPQP-3EpwstgJTemH5vrT-I9o1PN1sDlc3ngjmrZOQm1kzACoh_Kync1aiGnLsJWKDYjQvMzKZVXtA7a6m-Wa3tSMxF7-bG-rM-zliBsOQ2Q5FMi6DwDTER2u";
		Map<String, String> paramsMap = new HashMap<String, String>();
		paramsMap.put("province","����");
		paramsMap.put("city","����");
		paramsMap.put("region", "");
		paramsMap.put("type","" );
		paramsMap.put("opState", "");
		paramsMap.put("pageNo", "1");
		paramsMap.put("pageSize", "100");
		String linkedParam=StringUtils.createLinkedJson(paramsMap,1);
		//System.out.println(linkedParam);
		String sign =null;
		try {
			String signedStr = "requestMsg="+linkedParam+signKey;
			//System.out.println(signedStr);
			sign= StringUtils.MD5(new String(signedStr.getBytes("utf-8"))).toLowerCase();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//System.out.println("sign:"+sign);
		String queryDes="";
		try {
			queryDes = new String(ZldDesUtils.encrypt(linkedParam));
		} catch (Exception e) {
			e.printStackTrace();
		}
		//System.out.println("queryDes:"+queryDes);
		String queryParam = "{\"requestMsg\":\""+queryDes+"\",\"sign\":\""+sign+"\"}";
		//System.out.println("queryparams:"+queryParam);
		String result = new HttpProxy().doTeldPost(url, token, queryParam);
	//	logger.error("ȡ���׮����:"+result);
		try {
			JSONObject jsonObject = new JSONObject(result);
			String resultValue  = jsonObject.getString("resultValue");
			String res = ZldDesUtils.decrypt(resultValue);
			jsonObject = new JSONObject(res);
			JSONArray array= jsonObject.getJSONArray("staList");
			for(int i=0;i<array.length();i++){
				jsonObject = array.getJSONObject(i);
				Map<String, Object> retMap = new HashMap<String, Object>();
				for (Iterator<String> iter = jsonObject.keys(); iter.hasNext();) { 
				     String key = (String)iter.next();
				     retMap.put(key, jsonObject.get(key));
				}
				resList.add(retMap);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return resList;
	}
		//ȡ���׮�ӿ�TOKEN��7200��ʧЧ 
	private  String getChargeAccToken(){
		String  cachetoken = memcacheUtils.doStringCache("recharge_acctoken", null, null);
		String token = null;
		Long ntime = System.currentTimeMillis()/1000;
		if(cachetoken!=null&&cachetoken.length()>11){
			String time = cachetoken.substring(0,10);
			if(Check.isLong(time)){
				Long lastTime  = Long.valueOf(time);
				if(lastTime+7200>ntime){
					token =cachetoken.substring(10);
				}
			}
			logger.error("���׮����token:"+token);
		}
		if(token==null||token.trim().equals("")){
			String url ="http://open.teld.cn/OAuth/Token";
			Map<String, String> paramsMap = new HashMap<String, String>();
			paramsMap.put("grant_type", "client_credentials");
			paramsMap.put("client_id", "teldhh20cdbpb2umuocw");
			paramsMap.put("client_secret", "Y0iFg61V12");
			String re = new HttpProxy().doPost(url,paramsMap);
			//{"access_token":"7YheTLsEhZpUx_GNvvcjxMeYaKOuumoYnYZSbzBzIDqh6vSkYwMs7TejVby-Jt9Isb8waxehy6L7Qe4AsxQSiY8BTV2qlg1cPwCsy-dSx0YqX1ZyLMhQqlj4voCOwxF5ACM3EXngJNu5HmcJXQc1qfPEIuMllf2z99W5yooil03969rP615jQHjKJnKOPasoZ_oEohQBP3RzXPt0Vhu8OBp20yeFpJdDx8JrmAjZwuO_oLbv","token_type":"bearer","expires_in":7199}
			try {
				logger.error("���׮ȡtoken:"+re);
				JSONObject jsonObject = new JSONObject(re);
				token = jsonObject.getString("access_token");
				if(token!=null)
					memcacheUtils.doStringCache("recharge_acctoken", ntime+token, "update");
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return token;
	}
	
	//�����������г����ݣ�������ԭ������������ͬʱ�����£���ͬʱ��ɾ��������
	private void updateBikeInfo(){
		String url ="http://172.16.220.32:8080/wsclient/tsclient?type=bike&action=queryall";
		//String url ="http://127.0.0.1/wsclient/tsclient?type=bike&action=queryall";
		String result = new HttpProxy().doGet(url);
		//{ret=0, zdaddr=���··��, wdinfo=32.396822, totalcws=32, zdname=ְ����, jdinfo=119.382417, leftcws=29}
		Map<String, Object> retMap = new HashMap<String, Object>();
		if(result!=null){
			Map<String, Object> resultMap = ZldXMLUtils.parserStrXml(result);
			//<RET>0</RET><INFO></INFO><RECCOUNT>412</RECCOUNT><TOTALREC>412</TOTALREC><RETINFO>|
			if(resultMap!=null){
				String ret = (String)resultMap.get("ret");
				if(ret!=null&&ret.equals("0")){
					String total = (String)resultMap.get("totalrec");
					if(total!=null&&Check.isNumber(total)){
						Integer t = Integer.valueOf(total);
						if(t>0){//update
							String sql = "update city_bike_tb set plot_count=?,surplus=? where id=? ";
							String data = (String)resultMap.get("retinfo");
							if(data!=null){
								if(data.startsWith("|"))
									data = data.substring(1);
								String d[] = data.split("\\|");
								List<Object[]> values = new ArrayList<Object[]>();
								for(int i=0;i<d.length;i+=7){
									Integer count = 0;
									if(Check.isNumber(d[i+5]))
											count = Integer.valueOf(d[i+5]);
									Integer surplus = 0;
									if(Check.isNumber(d[i+6]))
										surplus = Integer.valueOf(d[i+6]);
									values.add( new Object[]{count,surplus,Long.valueOf(d[i])});
								}
								int todb = service.bathInsert(sql, values, new int[]{4,4,4});
								logger.error("bikeall update "+todb);
							}
						}
						/*else{//����д���¼
							String sql = "insert into city_bike_tb (id,name,address,longitude,latitude,plot_count,surplus)" +
									" values(?,?,?,?,?,?,?);";
							String data = (String)resultMap.get("retinfo");
							if(data!=null){
								if(data.startsWith("|"))
									data = data.substring(1);
								String d[] = data.split("\\|");
								if(d.length>100){
									int s = service.update("delete from city_bike_tb where id>?",new Object[]{0});
									if(s>0){
										List<Object[]> values = new ArrayList<Object[]>();
										for(int i=0;i<d.length;i+=7){
											String lng = d[i+3];
											String lat = d[i+4];
											Double lg = 0.0;
											double la = 0.0;
											if(Check.isDouble(lng))
												lg = ZldUploadUtils.formatDouble(lng, 6);
											if(Check.isDouble(lat)){
												la=ZldUploadUtils.formatDouble(lat, 6);
											}
											Integer count = 0;
											if(Check.isNumber(d[i+5]))
												count = Integer.valueOf(d[i+5]);
											Integer surplus = 0;
											if(Check.isNumber(d[i+6]))
												surplus = Integer.valueOf(d[i+6]);
											values.add( new Object[]{Long.valueOf(d[i]),d[i+1],d[i+2],lg,la,count,surplus});
										}
										int todb = service.bathInsert(sql, values, new int[]{4,12,12,3,3,4,4});
										logger.error("bikeall insert "+todb);
									}
								}
							}
						}*/
					}
				}
			}
		}
	}
	private String historyCarOrder(HttpServletRequest request){
		//http://service.yzjttcgs.com/zld/carinter.do?action=carorderhistory&page=1&size=10&mobile=18101333937
		//���ڣ�ͣ�������ƣ��ܼ�
		Integer pageNum = RequestUtil.getInteger(request, "page", 1);
		Integer pageSize = RequestUtil.getInteger(request, "size", 200);
		List<Object> params = new ArrayList<Object>();
		String mobile = RequestUtil.getString(request, "mobile");
		Long uin = getUinByMobile(mobile);
		params.add(1);
		params.add(uin);
		params.add(1);
		//Long time = TimeTools.getToDayBeginTime();
		List<Map<String, Object>> infoMaps = new ArrayList<Map<String,Object>>();
		List<Map<String, Object>> list = onlyService.getAll("select t.id, t.create_time,t.total,c.company_name from order_tb t,com_info_tb c " +
				"where t.comid=c.id and  t.state=? and t.uin=? and t.pay_type>? order by t.end_time desc",// and create_time>?",
				params, pageNum, pageSize);
		params.clear();
		params.add(uin);
		params.add(8);
	
		if(list!=null&&list.size()>0){
			for(Map map : list){
				Map<String, Object> info = new HashMap<String, Object>();
				Long ctime = (Long)map.get("create_time");
				info.put("date", TimeTools.getTimeStr_yyyy_MM_dd(ctime*1000));
				info.put("total", StringUtils.formatDouble(map.get("total")));
				info.put("parkname", map.get("company_name"));
				info.put("orderid", map.get("id"));
				infoMaps.add(info);
			}
		}else {
			Map<String, Object> info = new HashMap<String, Object>();
//			info.put("info", "û�м�¼");
//			infoMaps.add(info);
		}
		//System.err.println(infoMaps);
		String result = "{\"parklist\":"+StringUtils.createJson(infoMaps)+"}";
		logger.error("car order history:"+result);
		return result;
	}
	
	/**
	 * �����
	 * @param request
	 * @return
	 */
	private String delCarnumber(HttpServletRequest request){
		Long carid = RequestUtil.getLong(request, "carid", -1l);
		String mobile = RequestUtil.getString(request, "mobile");
		Long uin = getUinByMobile(mobile);
		logger.error("carid:"+carid+" uin:"+uin);
		String update = "-1";
		if(carid==-1){
			return update;
		}
		//����uin���¿�,�����δ���ڵ��¿�,�򲻿ɽ��
		Map carmap = service.getMap("select car_number from car_info_tb where id = ?", new Object[]{carid});
		//Map<String, Object> ret = commonMethods.isProdBeOverdue((String)carmap.get("car_number"),uin);
		/*List<Map> productlist = service.getAll("select e_time from carower_product where uin = ?", new Object[]{uin});
		for (Map prod : productlist) {
			Long endTime = (Long) prod.get("e_time");
			if(endTime==null){
				logger.error("ȱʧ�¿�����ʱ��");
				return "-3";
			}
			Long cTime = TimeTools.getToDayBeginTime();
			logger.error(cTime);
			logger.error("�¿���������:"+TimeTools.getTimeStr_yyyy_MM_dd(endTime*1000)+" ��ǰ����:"+TimeTools.getTimeStr_yyyy_MM_dd(cTime*1000));
			if(cTime<endTime){
				logger.error("�¿�δ����,���ɰ�");
				return "-2";
			}
		}*/
		//�����
		//Integer overdued = (Integer)ret.get("overdued");
		/*if(overdued!=2){
			//ȫ����
			logger.error("���Խ��");
			update = service.update("delete from car_info_tb where id = ?", new Object[]{carid})+"";
			return update;
		}else{
			logger.error("���ɽ��");
			return "-2";
		}*/
		
		//�ó����ڳ�����������,���ɽ��
		Long curorder = service.getLong("select count(id) from order_tb where car_number = ? and state = ? and islocked = ? ", 
				new Object[]{carmap.get("car_number"),0,1});
		if(curorder.intValue()<1){
			//���Խ��
			logger.error("���Խ��");
			update = service.update("delete from car_info_tb where id = ?", new Object[]{carid})+"";
			return update;
		}else{
			return "-2";
		}
	}
}
