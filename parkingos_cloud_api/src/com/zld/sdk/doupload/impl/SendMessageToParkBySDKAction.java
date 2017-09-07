package com.zld.sdk.doupload.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
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

import com.zld.sdk.tcp.NettyChannelMap;
import com.zld.service.DataBaseService;
import com.zld.utils.Check;
import com.zld.utils.StringUtils;

public class SendMessageToParkBySDKAction extends Action {
	private Logger logger = Logger.getLogger(SendMessageToParkBySDKAction.class);
	@Autowired
	DataBaseService daService ;
	
	@Override
	public ActionForward execute(ActionMapping mapping, ActionForm form,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		//��ѯ�����´����ݽӿڣ���������Ҫͬ������ѯ����Ӧ�����ݽ������·����շ�ϵͳ
		//��ѯ��Ҫͬ��������
		List list = new ArrayList();
		list.add(0);
		List dataNeedSyncList = daService.getAllMap("select * from sync_info_pool_tb where state=? ", list);
		if(dataNeedSyncList != null && dataNeedSyncList.size()>0){
			for(int i=0; i<dataNeedSyncList.size(); i++){
				Map mapNeedSync = (Map) dataNeedSyncList.get(i);
				if(mapNeedSync != null && !mapNeedSync.isEmpty()){
					String tableName = String.valueOf(mapNeedSync.get("table_name"));
					Long tableId = Long.valueOf(String.valueOf(mapNeedSync.get("table_id")));
					Long comid = Long.valueOf(String.valueOf(mapNeedSync.get("comid")));
					Integer operate = Integer.valueOf(String.valueOf(mapNeedSync.get("operate")));
					if(tableName.equals("carower_product")){
						String result = sendcardMember(tableName,tableId,comid,operate);
						logger.error(">>>>>>>>>>>>>>>>>>>>>�����¿���Ա��Ϣ���"+result);
					}else if(tableName.equals("product_package_tb")){
						String result = sendcardPackage(tableName,tableId,comid,operate);
						logger.error(">>>>>>>>>>>>>>>>>>>>>�����¿��ײ���Ϣ���"+result);
					}else if(tableName.equals("price_tb")){
						String result = sendPricInfo(tableName,tableId,comid,operate);
						logger.error(">>>>>>>>>>>>>>>>>>>>>���ͼ۸���Ϣ���"+result);
					}
				}
			}
		}else{
			StringUtils.ajaxOutput(response, "û����Ҫͬ��������");
		}
		return null;
	}
	
	private String sendPricInfo(String tableName, Long tableId, Long comid,
			Integer operate) {
		String result="0";
		//��ȡ�´����ݵ�ͨ����Ϣ
		Channel channel = NettyChannelMap.get(getChannel(String.valueOf(comid)));
		//�����װ�´���Ϣ��json����
		JSONObject jsonObj = null;
		//��������
		if(operate == 0){
			operate = 1;
		}else if(operate == 1){
			operate = 2;
		}else if(operate == 2){
			operate = 3;
		}
		//��ѯ����Ӧ����Ҫ�´�������
		Map mapNeed = daService.getMap("select * from "+tableName+" where id=? and comid=?", new Object[]{tableId,comid});
		if(mapNeed != null && !mapNeed.isEmpty()){
			//��ʱ����Ϊ�������ݿ���������´�
			String mapJSONStr = StringUtils.createJson(mapNeed);
			jsonObj = new JSONObject().fromObject(mapJSONStr);
			/*jsonObj.put("card_id", String.valueOf(mapNeed.get("card_id")));
			Long createTime = -1L;
			if(String.valueOf(mapNeed.get("create_time"))!=null && !(String.valueOf(mapNeed.get("create_time"))).equals("null")){
				createTime = Long.valueOf(String.valueOf(mapNeed.get("update_time")));
			}
			jsonObj.put("create_time", createTime);
			Long updateTime = -1L;
			if(String.valueOf(mapNeed.get("update_time"))!=null && !(String.valueOf(mapNeed.get("update_time"))).equals("null")){
				updateTime = Long.valueOf(String.valueOf(mapNeed.get("update_time")));
			}
			jsonObj.put("update_time", updateTime);
			jsonObj.put("describe",String.valueOf(mapNeed.get("describe")));
			jsonObj.put("name", String.valueOf(mapNeed.get("p_name")));*/
			jsonObj.put("operate_type", operate);
		}else{
			logger.error(">>>>>>>>>>>>>û�鵽��Ӧ����Ҫ�´�����Ϣ");
			return result;
		}
		JSONObject jsonMesg = new JSONObject();
		jsonMesg.put("service_name", "price_sync");
		jsonMesg.put("data", jsonObj.toString());
		boolean isSend = doBackMessage(jsonMesg.toString(), channel);
		logger.error(">>>>>>>>>>>>>>�ƶ˷������ݵ�ͣ���շ�ϵͳ�����"+isSend);
		if(isSend){
			result = "1";
		}
		return result;
	}

	/**
	 * �����¿��ײ���Ϣ��ͣ���շ�ϵͳ
	 * @param tableName
	 * @param tableId
	 * @param comid
	 * @param operate
	 * @return
	 */
	private String sendcardPackage(String tableName, Long tableId, Long comid,
			Integer operate) {
		String result="0";
		//��ȡ�´����ݵ�ͨ����Ϣ
		Channel channel = NettyChannelMap.get(getChannel(String.valueOf(comid)));
		//�����װ�´���Ϣ��json����
		JSONObject jsonObj = null;
		//��������
		if(operate == 0){
			operate = 1;
		}else if(operate == 1){
			operate = 2;
		}else if(operate == 2){
			operate = 3;
		}
		//��ѯ����Ӧ����Ҫ�´�������
		Map mapNeed = daService.getMap("select * from "+tableName+" where id=? and comid=?", new Object[]{tableId,comid});
		if(mapNeed != null && !mapNeed.isEmpty()){
			//��ʱ����Ϊ�������ݿ��ֶζ����ݽ����´�����
			String mapNeedStr = StringUtils.createJson(mapNeed);
			jsonObj = new JSONObject().fromObject(mapNeedStr);
			
			/*jsonObj.put("card_id", String.valueOf(mapNeed.get("card_id")));
			Long createTime = -1L;
			if(String.valueOf(mapNeed.get("create_time"))!=null && !(String.valueOf(mapNeed.get("create_time"))).equals("null")){
				createTime = Long.valueOf(String.valueOf(mapNeed.get("update_time")));
			}
			jsonObj.put("create_time", createTime);
			Long updateTime = -1L;
			if(String.valueOf(mapNeed.get("update_time"))!=null && !(String.valueOf(mapNeed.get("update_time"))).equals("null")){
				updateTime = Long.valueOf(String.valueOf(mapNeed.get("update_time")));
			}
			jsonObj.put("update_time", updateTime);
			jsonObj.put("describe",String.valueOf(mapNeed.get("describe")));
			jsonObj.put("name", String.valueOf(mapNeed.get("p_name")));*/
			jsonObj.put("operate_type", operate);
		}else{
			logger.error(">>>>>>>>>>>>>û�鵽��Ӧ����Ҫ�´�����Ϣ");
			return result;
		}
		JSONObject jsonMesg = new JSONObject();
		jsonMesg.put("service_name", "month_card_sync");
		jsonMesg.put("data", jsonObj.toString());
		boolean isSend = doBackMessage(jsonMesg.toString(), channel);
		logger.error(">>>>>>>>>>>>>>�ƶ˷������ݵ�ͣ���շ�ϵͳ�����"+isSend);
		if(isSend){
			result = "1";
		}
		return result;
	}

	/**
	 * ���ƺ�̨�޸ĵ��¿���Ա����Ϣ���͵������շ�ϵͳ
	 * @param tableName
	 * @param tableId
	 * @param comid
	 * @param operate
	 * @return
	 */
	private String sendcardMember(String tableName, Long tableId, Long comid,
			Integer operate) {
		String result="0";
		//��ȡ�´����ݵ�ͨ����Ϣ
		Channel channel = NettyChannelMap.get(getChannel(String.valueOf(comid)));
		//�����װ�´���Ϣ��json����
		JSONObject jsonObj = new JSONObject();
		//��������
		if(operate == 0){
			operate = 1;
		}else if(operate == 1){
			operate = 2;
		}else if(operate == 2){
			operate = 3;
		}
		//��ѯ����Ӧ����Ҫ�´�������
		Map mapNeed = daService.getMap("select * from "+tableName+" where id=? and com_id=?", new Object[]{tableId,comid});
		if(mapNeed != null && !mapNeed.isEmpty()){
			logger.error(">>>>>>>��ѯ��Ҫͬ�����¿���Ա��Ϣ��"+mapNeed.toString());
			Long beginTime = -1L;
			if(String.valueOf(mapNeed.get("b_time"))!=null && !(String.valueOf(mapNeed.get("b_time"))).equals("null")){
				beginTime = Long.valueOf(String.valueOf(mapNeed.get("b_time")));
			}
			Long endTime = -1L;
			if(String.valueOf(mapNeed.get("e_time"))!=null && !(String.valueOf(mapNeed.get("e_time"))).equals("null")){
				endTime = Long.valueOf(String.valueOf(mapNeed.get("e_time")));
			}
			//�޸��´����ݵ����ݣ������ĵ����󣬽����е����ݶ��´����շ�ϵͳ
			String mapNeedStr = StringUtils.createJson(mapNeed);
			jsonObj = new JSONObject().fromObject(mapNeedStr);
			jsonObj.put("begin_time", beginTime);
			jsonObj.put("end_time", endTime);
			jsonObj.put("price", String.valueOf(mapNeed.get("act_total")));
			jsonObj.put("operate_type", operate);
			logger.error(">>>>>>>>>>>>>>>>>>>>>>>>>>�������������Ϊ��"+jsonObj.toString());
		}else{
			logger.error(">>>>>>>>>>>>>û�鵽��Ӧ����Ҫ�´�����Ϣ��������ɾ������");
			return result;
		}
		JSONObject jsonMesg = new JSONObject();
		jsonMesg.put("service_name", "month_member_sync");
		jsonMesg.put("data", jsonObj.toString());
		boolean isSend = doBackMessage(jsonMesg.toString(), channel);
		logger.error(">>>>>>>>>>>>>>>>>>>>ͬ���¿���Ա���ݵ��շ�ϵͳ��"+jsonMesg.toString());
		logger.error(">>>>>>>>>>>>>>�ƶ˷������ݵ�ͣ���շ�ϵͳ�����"+isSend);
		if(isSend){
			result = "1";
		}
		return result;
	}

	/**
	 * �´���Ϣ���շ�ϵͳ�ķ���
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
		Map parkMap = daService.getMap("select * from park_token_tb where park_id=?", new Object[]{comid});
		if(parkMap != null && !parkMap.isEmpty()){
			String localId = String.valueOf(parkMap.get("local_id"));
			if(!Check.isEmpty(localId)){
				channelPass += "_"+localId;
			}
		}else{
			channelPass = comid;
		}
		return channelPass;
	}
}
