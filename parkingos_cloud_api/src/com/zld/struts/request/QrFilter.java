package com.zld.struts.request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import pay.Constants;

import com.zld.AjaxUtil;
import com.zld.service.DataBaseService;
import com.zld.service.PgOnlyReadService;
import com.zld.utils.StringUtils;


/**
 * ��������Ƭ����mongodb��ȡ
 * @author Administrator
 *
 */
@Path("c")
public class QrFilter {
	
	Logger logger  = Logger.getLogger(QrFilter.class);
	/**NFC��ά��
	 * http://127.0.0.1/zld/qr/c/d41A501D0501460255b
	 *�շ�Ա��ά��
	 * http://127.0.0.1/zld/qr/c/d41A501D0501460255b&mobile=15801482643&total=5.0
	 */
	@GET
	@Path("/{code}")
	@Produces(MediaType.TEXT_HTML)
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)       
	public void getLogoFile(@PathParam("code") String code,
			@Context HttpServletRequest request, 
			@Context HttpServletResponse response,
			@Context ServletContext context)throws IOException {
			
		System.err.println("code:"+code);
		if(!code.startsWith("z")&&!code.startsWith("d")&&!code.startsWith("B0liNk"))
			return ;
		boolean isclient = false;
		Map<String, String> map = getparam(code);
		code = map.get("code");
		String mobile = map.get("mobile");
		if(mobile != null){
			isclient = true;
		}
		String total = map.get("total");
		String stype = map.get("type");
		System.err.println("isclient:"+isclient+",mobile:"+mobile+",code:"+code+",total:"+total+",map:"+map+",stype:"+stype);
		
		ApplicationContext ctx = WebApplicationContextUtils.getWebApplicationContext(context);
		DataBaseService daService = (DataBaseService) ctx.getBean("dataBaseService");
		PgOnlyReadService pgOnlyReadService = (PgOnlyReadService) ctx.getBean("pgOnlyReadService");
		//System.err.println(daService);
		Map<String, Object> codeMap = null;
		String from ="";
		if(code.startsWith("B0liNk")){//������ά��
			codeMap=daService.getMap("select * from qr_thirdpark_code where code=?", new Object[]{code});
			if(codeMap!=null){
				from = "bolink";
				codeMap.put("type", 4);
			}
		}else {
			codeMap=daService.getMap("select * from qr_code_tb where code=?", new Object[]{code});
		}
		System.out.println(codeMap);
		Long orderId = -1L;
		if(codeMap!=null){
			Integer type = (Integer)codeMap.get("type");
			if(type!=null){
				switch (type) {
				case 0://NFC
					Map nfcMap = daService.getMap("select nfc_uuid from com_nfc_tb where qrcode=? ", new Object[]{code});
					String uuid  = "";
					if(nfcMap!=null){
						uuid = (String)nfcMap.get("nfc_uuid");
					}
					if(uuid!=null&&!"".equals(uuid)){
						Map orderMap = daService.getMap("select id from order_tb where nfc_uuid=? and state= ? order by create_time desc limit ?", new Object[]{uuid,0,1});
						if(orderMap!=null)
							orderId = (Long)orderMap.get("id");
					}
					if(!isclient){//΢��ɨ��,���������
						String redirectUrl = "http%3A%2F%2F"+Constants.WXPUBLIC_REDIRECTURL+"%2Fzld%2Fwxpfast.do%3Faction%3Dprepay%26type%3Dnfc%26orderid%3D"+orderId;
						String url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="+Constants.WXPUBLIC_APPID+"&redirect_uri="+redirectUrl+
								"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
						System.err.println(url);
						response.sendRedirect(url);
					}else {//�ͻ���ɨ��
						String url = "http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/nfchandle.do?action=coswipe&uuid="+uuid+"&mobile="+mobile+"&from=qr";
						System.err.println(url);
						response.sendRedirect(url);
					}
					return;
				case 1://�շ�Ա��ά��,client:�ж����������ѣ��޶�����ֱ��,  ΢�ţ�ֱ��
					Long uid = (Long)codeMap.get("uid");
					String redirectUrl = "http%3A%2F%2F"+Constants.WXPUBLIC_REDIRECTURL+"%2Fzld%2Fwxpfast.do%3Faction%3Depay%26type%3Dsweepparker%26uid%3D"+uid;
					if(total != null){
						redirectUrl += "%26total%3D"+total;
					}
					String url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="+Constants.WXPUBLIC_APPID+"&redirect_uri="+redirectUrl+
							"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
					if(!isclient){
						response.sendRedirect(url);
					}else {
						//�鳵������������Ƿ��ж���
						//1�鳵����� 
						Long uin = null;
						Map tempMap = daService.getMap("select id from user_info_tb where auth_flag=? and mobile=? ", new Object[]{4,mobile});
						if(tempMap!=null)
							uin  = (Long)tempMap.get("id");
						//2���շ�Ա���ڵĳ���
						Long comid = null;
						tempMap = daService.getMap("select comid from user_info_tb where id =?", new Object[]{uid});
						if(tempMap!=null)
							comid = (Long)tempMap.get("comid");
						//3��û�н��� ����
						if(uin!=null&comid!=null&&uin>0&&comid>0){
							tempMap = pgOnlyReadService.getMap("select id from order_tb where comid=? and uin=? and state=? ", new Object[]{comid,uin,0});
						}
						System.out.println(">>>>>>>>>>>>>uin:"+uin+",comid:"+comid+",order"+tempMap);
						if(tempMap!=null&&tempMap.get("id")!=null){//�ж���
							orderId =(Long)tempMap.get("id");
							url ="http://"+Constants.WXPUBLIC_S_DOMAIN+"/zld/carowner.do?action=currentorder&mobile="+mobile+"&uid="+uid+"&comid="+comid+"&orderid="+orderId+"&from=qr";
							response.sendRedirect(url);
						}else {
							//�����շ�Ա�ı�ż���������������
							Map userMap = daService.getMap("select nickname as name,u.id ,company_name as parkname ,online_flag as online" +
									" from user_info_Tb u left join com_info_tb c on u.comid=c.id where u.id=?", new Object[]{uid});
							if(total != null && userMap != null && Double.valueOf(total + "") > 0){
								userMap.put("total", total);
							}
							AjaxUtil.ajaxOutput(response, "{\"type\":\"1\",\"info\":"+StringUtils.createJson(userMap)+"}");
						}
					}
					return ;
				case 2://��λ��ά��
					redirectUrl = "http%3A%2F%2F"+Constants.WXPUBLIC_REDIRECTURL+"%2Fzld%2Fwxpfast.do%3Faction%3Dsweepspace%26codeid%3D"+codeMap.get("id");
					url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="+Constants.WXPUBLIC_APPID+"&redirect_uri="+redirectUrl+
							"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
					if(!isclient){
						response.sendRedirect(url);
					}else {
						Map parkMap = daService.getMap("select * from com_park_tb where qid=?", new Object[]{codeMap.get("id")});
						if(parkMap!=null&&!parkMap.isEmpty()){//�鵽�˳�λ
							Integer state = (Integer)parkMap.get("state");
							Long oid  = (Long)parkMap.get("order_id");
							if(oid!=null&&oid>0){
								parkMap = daService.getMap("select state from order_tb where id=? ", new Object[]{oid});
								if(parkMap!=null&&!parkMap.isEmpty()){
									state = (Integer)parkMap.get("state");
									if(state==0)
										state=1;
									else {
										state=0;
									}
								}
							}
							if(state==0){//���У����ɶ���
								AjaxUtil.ajaxOutput(response, "{\"type\":\"4\",\"info\":{\"cid\":\""+codeMap.get("id")+"\"}}");
							}else if(state==1){//��ռ�ý��㶩��
								orderId =(Long)parkMap.get("order_id");
								Object comid = parkMap.get("comid");
								//url ="http://"+Constants.WXPUBLIC_S_DOMAIN+"/zld/carowner.do?action=currentorder&mobile="+mobile+"&comid="+comid+"&orderid="+orderId+"&from=qr";
								url ="http://s.tingchebao.com/zld/carowner.do?action=currentorder&mobile="+mobile+"&comid="+comid+"&orderid="+orderId+"&from=qrpark";
								response.sendRedirect(url);
							}
						}
					}
					break;
				case 3://����Ա��ά��
					Long _uid = (Long)codeMap.get("uid");
					String _redirectUrl = "http%3A%2F%2F"+Constants.WXPUBLIC_REDIRECTURL+"%2Fzld%2Fattendant.do%3Faction%3Dhandleqr%26uid%3D"+_uid;
					String _url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="+Constants.WXPUBLIC_APPID+"&redirect_uri="+_redirectUrl+
							"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
					System.err.println(_url);
					response.sendRedirect(_url);
					break;
				case 4://������ά��
					/*String _eUrl = "http://s.tingchebao.com/zld/wxpublic.do?action=error&errorcode=1";
					response.sendRedirect(_eUrl);
					break;*/
				case 5://����ȯ��ά��
					String userAgent = request.getHeader("user-agent");
					logger.error("user-agent:"+userAgent);
					String thirdOrderId = map.get("orderid");
					String carNumber = map.get("licence");
					if(userAgent.indexOf("AlipayClient")!=-1){//֧����ɨ��
						response.sendRedirect("http://" + Constants.WXPUBLIC_REDIRECTURL + "/zld/aliprepay.do?action=sweepcom&parkid="+
								codeMap.get("park_id")+"&unionid="+codeMap.get("union_id")+"&orderid="+thirdOrderId+"&licence="+carNumber);
					}else {
						String rUrl = "http%3a%2f%2f"+Constants.WXPUBLIC_REDIRECTURL+"%2fzld%2fwxpfast.do%3faction%3dsweepcom%26from%3d"+from
								+"%26codeid%3d"+codeMap.get("id");
						if((thirdOrderId!=null&&!"".equals(thirdOrderId))||from.equals("bolink"))//���ж�����ŵ�Ԥ����Ϣ������֧����ά��
							rUrl = "http%3a%2f%2f"+Constants.WXPUBLIC_REDIRECTURL+"%2fzld%2fwxpfast.do%3faction%3dhandlethirdprepay%26comid%3d"+codeMap.get("park_id")+"%26orderid%3d"+thirdOrderId+"%26carnumber%3d"+carNumber;
						
						String _rUrl = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="+Constants.WXPUBLIC_APPID+"&redirect_uri="+rUrl+
								"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
						logger.error(rUrl);
						response.sendRedirect(_rUrl);
					}
					break;
				case 6://ͣ��ȯ��ά��
					if(!isclient){
						String _rUrl = "http%3A%2F%2F"+Constants.WXPUBLIC_REDIRECTURL+"%2Fzld%2Fwxpfast.do%3Faction%3Dsweepticket%26codeid%3D"+codeMap.get("id");
						if(stype != null){
							_rUrl += "%26type%3D"+stype;
						}
						String _tUrl = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="+Constants.WXPUBLIC_APPID+"&redirect_uri="+_rUrl+
								"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
						System.out.println(_tUrl);
						response.sendRedirect(_tUrl);
					}else{
						String  _rUrl = "http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/carowner.do?action=sweepticket&codeid="+codeMap.get("id")+"&mobile="+mobile;
						
						System.out.println(_rUrl);
						response.sendRedirect(_rUrl);
					}
					break;
				
				default:
					break;
				}
			}
		}else {//����pos����������ʱ��ά��
			Map orderMap = daService.getMap("select * from order_tb where nfc_uuid=?",new Object[]{code});
			String uid = map.get("uid");
			String comid =map.get("comid");
			if(orderMap!=null){
				//Integer state = (Integer)orderMap.get("state");//״̬
				orderId = (Long)orderMap.get("id");
				if(uid==null){//����ɨ��ά��
					//΢�ţ�Ԥ֧�����ѯ�������ͻ��ˣ�����Ԥ֧����δ������
					if(!isclient){//΢��ɨ��,���������
						String redirectUrl = "http%3A%2F%2F"+Constants.WXPUBLIC_REDIRECTURL+"%2Fzld%2Fwxpfast.do%3Faction%3Dprepay%26type%3Dnfc%26orderid%3D"+orderId;
						String url = "https://open.weixin.qq.com/connect/oauth2/authorize?appid="+Constants.WXPUBLIC_APPID+"&redirect_uri="+redirectUrl+
								"&response_type=code&scope=snsapi_base&state=123#wechat_redirect";
						System.err.println(url);
						response.sendRedirect(url);
					}else {//�ͻ���ɨ��
						String url = "http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/nfchandle.do?action=coswipe&uuid="+code+"&mobile="+mobile+"&from=qr";
						System.err.println(url);
						response.sendRedirect(url);
					}
				}else {//�շ�Աɨ��ά��
					//��NFCˢ������
					String reUrl ="http://"+Constants.WXPUBLIC_REDIRECTURL+"/zld/nfchandle.do?action=incom&from=qr&uuid="+code+"&uid="+uid+"&comid="+comid;
					System.err.println(reUrl);
					response.sendRedirect(reUrl);
				}
			}			
		}

		AjaxUtil.ajaxOutput(response, "code:"+code);
	}
	
	private Map<String, String> getparam(String code){
		Map<String, String> map = new HashMap<String, String>();
		if(code != null){
			code = "code="+code;
			String param[] = code.split("&");
			for(int i=0;i<param.length;i++){
				String pString = param[i];
				String p[] = pString.split("=");
				map.put(p[0], p[1]);
			}
		}
		return map;
	}
}
