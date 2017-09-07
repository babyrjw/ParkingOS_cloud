package com.zld.struts.request;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.beans.factory.annotation.Autowired;

import com.zld.AjaxUtil;
import com.zld.sdk.tcp.NettyChannelMap;
import com.zld.service.DataBaseService;
import com.zld.utils.Check;
import com.zld.utils.StringUtils;

public class SendMessageByTCPAction extends Action {
	
	private Logger logger = Logger.getLogger(SendMessageByTCPAction.class);
	@Autowired
	DataBaseService dataBaseService ;
	
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String action = request.getParameter("action");
		if(action == null){
			action = "";
		}else if(action.equals("sendliftrodmsg")){
			//����̧����Ϣ���ݵ��շ�ϵͳ
			String comid = request.getParameter("comid");
			//��ȡ�´����ݵ�ͨ����Ϣ
			Channel channel = NettyChannelMap.get(getChannel(comid));
			//��װ̧������
			JSONObject jsonObj = new JSONObject();
			JSONObject json = new JSONObject();
			//ͨ������
			String channelName = request.getParameter("channelName");
			//ͨ����
			String channelId = request.getParameter("channelId");
			//��բָ��
			Integer operate = Integer.valueOf(request.getParameter("operate"));
			json.put("channel_name", URLDecoder.decode(channelName,"UTF-8"));
			json.put("channel_id", URLDecoder.decode(channelId,"UTF-8"));
			json.put("operate", operate);
			json.put("service_name", "operate_liftrod");
			doBackMessage(json.toString(), channel);
			StringUtils.ajaxOutput(response,"1");
		}
		Map<String,Object> params = new HashMap<String,Object>();
		Map<String,String[]> requestParams = request.getParameterMap();
		for (Iterator<String> iter = requestParams.keySet().iterator(); iter.hasNext();) {
			String name = (String) iter.next();
			String[] values = (String[]) requestParams.get(name);
			String valueStr = "";
			for (int i = 0; i < values.length; i++) {
				valueStr = (i == values.length - 1) ? valueStr + values[i]
						: valueStr + values[i] + ",";
			}
			params.put(name,AjaxUtil.decodeUTF8(valueStr));
		}
		logger.error(">>>>>>>>>>>>>>����tcpת����Ϣ�����ݣ�"+params);
		String message = StringUtils.createJson(params);
		String pass = (String)params.get("comid");
		String localId = (String)params.get("local_id");
		if(!Check.isEmpty(localId)){
			pass += "_"+localId;
		}
		Channel channel = NettyChannelMap.get(pass);
		boolean isSend = doBackMessage(message, channel);
		doBackBusiness(params,isSend);
		logger.error(">>>>>>>>>>>>>>����tcpת����Ϣ�������"+isSend);
		StringUtils.ajaxOutput(response,"{\"state\":"+isSend+"}");
		
		/*if("".equals(action)){
			String park_id = request.getParameter("park_id");
			String data = request.getParameter("data");
			String serviceName = request.getParameter("service_name");
			Channel clientChannel = NettyChannelMap.get(park_id.toString());
			JSONObject jsonObj = new JSONObject();
			jsonObj.put("service_name", serviceName);
			jsonObj.put("data", data);
			String sendMessage = jsonObj.toString();
			logger.error("ͬ����Ϣ������" + sendMessage + ",channel:" + clientChannel);
			if (clientChannel != null && clientChannel.isActive()
					&& clientChannel.isWritable()) {
				byte[] req = ("\n" + sendMessage + "\r").getBytes("UTF-8");
				ByteBuf buf = Unpooled.buffer(req.length);
				buf.writeBytes(req);
				clientChannel.writeAndFlush(buf);
				logger.error("�Ѿ�ͬ����Ϣ��ͣ����SDK...");
				String message = "Send Successed";
				try {
					AjaxUtil.ajaxOutput(response, message);
				} catch (IOException e) {
					logger.error("���ͷ���ֵ��Ϣ�����쳣������̨����");
					e.printStackTrace();
				}
			} else {
				logger.error("��ͣ�����������쳣���ͻ���SDK�ѵ���...");
			}
		}
		if("sendData".equals(action)){
			String park_id = request.getParameter("park_id");
			String data = request.getParameter("data");
			Channel clientChannel = NettyChannelMap.get(park_id.toString());
			logger.error("ͬ����Ϣ������" + data + ",channel:" + clientChannel);
			if (clientChannel != null && clientChannel.isActive()
					&& clientChannel.isWritable()) {
				byte[] req = ("\n" + data + "\r").getBytes("UTF-8");
				ByteBuf buf = Unpooled.buffer(req.length);
				buf.writeBytes(req);
				clientChannel.writeAndFlush(buf);
				logger.error("�Ѿ�ͬ����Ϣ��ͣ����SDK...");
				String message = "{\"state\":1,\"message\":\"Send Successed\"}";
				try {
					AjaxUtil.ajaxOutput(response, message);
				} catch (IOException e) {
					logger.error("���ͷ���ֵ��Ϣ�����쳣������̨����");
					e.printStackTrace();
				}
			} else {
				logger.error("��ͣ�����������쳣���ͻ���SDK�ѵ���...");
				String message = "{\"state\":0,\"message\":\"Send fail\"}";
				try {
					AjaxUtil.ajaxOutput(response, message);
				} catch (IOException e) {
					logger.error("���ͷ���ֵ��Ϣ�����쳣������̨����");
					e.printStackTrace();
				}
			}
		}*/
		return null;
	}
	
	/**
	 * ����������Ϣ��ʵ������ҵ��
	 * @param params
	 * @param isSend
	 */
	private void doBackBusiness(Map<String,Object> params, boolean isSend) {
		String serviceName = (String)params.get("service_name");
		if(serviceName!=null&&serviceName.equals("lock_car")){//����ҵ��
			if(params.containsKey("id")&&Check.isLong(params.get("id")+"")){
				Long orderId = Long.valueOf(params.get("id")+"");
				if(params.containsKey("is_locked")){
					Integer state = -1;
					String isLocked = (String) params.get("is_locked");
					if(isLocked!=null){
						if(isLocked.equals("1")){//����
							if(!isSend)
								state=3;
						}else if(isLocked.equals("0")){//����
							if(!isSend)
								state=5;
						}
						if(state>0){
							int re = dataBaseService.update("update order_tb set islocked=? where id =? ", new Object[]{state,orderId});
							logger.error("����ҵ�񣬲���ʧ�ܣ������"+re);
						}
					}
				}
			}
		}
	}

	/**
	 * ��Ϣ����
	 * 
	 * @param mesg
	 * @param data
	 */
	private boolean doBackMessage(String mesg, Channel channel) {
		if (channel != null && channel.isActive()
				&& channel.isWritable()) {
			try {
				logger.error("����Ϣ��SDK��channel:"+channel+",mesg:" + mesg);
				byte[] req= ("\n" + mesg + "\r").getBytes("utf-8");
				ByteBuf buf = Unpooled.buffer(req.length);
				buf.writeBytes(req);
				ChannelFuture future = channel.writeAndFlush(buf);
				return true;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}else{
			logger.error("�ͻ����ѶϿ�����...");
		}
		return false;
	}
	
	/**
	 * ��ȡ�·����ݵ�TCPͨ��
	 * @param comid
	 * @return
	 */
	private String getChannel(String comid){
		String channelPass = "";
		Map parkMap = dataBaseService.getMap("select * from park_token_tb where park_id=?", new Object[]{comid});
		if(parkMap != null && !parkMap.isEmpty()){
			String localId = String.valueOf(parkMap.get("local_id"));
			if(!Check.isEmpty(localId)){
				channelPass += comid+"_"+localId;
			}
		}else{
			channelPass = comid;
		}
		return channelPass;
	}
}
