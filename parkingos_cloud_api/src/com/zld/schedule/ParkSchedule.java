package com.zld.schedule;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;

import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

import com.zld.sdk.tcp.NettyChannelMap;
import com.zld.service.DataBaseService;
import com.zld.utils.Check;
import com.zld.utils.StringUtils;

public class ParkSchedule extends TimerTask {
	
	DataBaseService daService;
	
	public ParkSchedule(DataBaseService dataBaseService ){
		this.daService = dataBaseService;
	}

	private static Logger logger = Logger.getLogger(ParkSchedule.class);

	@Override
	public void run() {
		logger.error("���ݿ��������:"+daService);
		logger.error("��ʼ�������ݶ�ʱ����....");
		sendMessageToSDK();
	}
	
	private void sendMessageToSDK(){
		
		
		List<Object> list = new ArrayList<Object>();
		list.add(0);
		List<Map<String, Object>> dataNeedSyncList = daService.getAllMap("select * from sync_info_pool_tb where state=? ", list);
		if(dataNeedSyncList != null && dataNeedSyncList.size()>0){
			for(Map<String, Object> mapNeedSync : dataNeedSyncList){
				if(mapNeedSync != null && !mapNeedSync.isEmpty()){
					String tableName = String.valueOf(mapNeedSync.get("table_name"));
					Long tableId = Long.valueOf(String.valueOf(mapNeedSync.get("table_id")));
					Long comid = Long.valueOf(String.valueOf(mapNeedSync.get("comid")));
					Integer operate = Integer.valueOf(String.valueOf(mapNeedSync.get("operate")));
					if(tableName.equals("carower_product")){
						String result = sendcardMember(tableName,tableId,comid,operate);
						logger.error(">>>>>>>>>>>>>>>>>>>>>�����¿���Ա��Ϣ���"+result);
					}else if(tableName.equals("price_tb")){
						String result = sendPriceInfo(tableName,tableId,comid,operate);
						logger.error(">>>>>>>>>>>>>>>>>>>>>���ͼ۸���Ϣ���"+result);
					}else if(tableName.equals("product_package_tb")){
						String result = sendProductPackageInfo(tableName,tableId,comid,operate);
						logger.error(">>>>>>>>>>>>>>>>>>>>>�����¿��ײ���Ϣ���"+result);
					}else {
						logger.error(">>��û�д���ǰͬ��ҵ��:"+tableName);
					}
				}
			}
		}
	}
	/**
	 * ���ƺ�̨�޸ĵ��¿��ײ���Ϣ���͵������շ�ϵͳ
	 * @param tableName
	 * @param tableId
	 * @param comid
	 * @param operate
	 * @return
	 */
	private String sendProductPackageInfo(String tableName, Long tableId,
			Long comid, Integer operate) {
		String result="0";
		//��ȡ�´����ݵ�ͨ����Ϣ
		Channel channel = NettyChannelMap.get(getChannel(String.valueOf(comid)));
		if (channel == null || !channel.isActive()|| !channel.isWritable()) {//ͨ�������ã��ͻ��˵���
			logger.error(comid+"���ͻ��˵���.....");
			return null;
		}
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
		Map mapNeed = daService.getMap("select * from "+tableName+" where id=? ", new Object[]{tableId});
		if(mapNeed != null && !mapNeed.isEmpty()){
			String mapNeedStr = StringUtils.createJson(mapNeed);
			jsonObj = JSONObject.fromObject(mapNeedStr);
			jsonObj.put("operate_type", operate);
		}else{
			logger.error(">>>>>>>>>>>>>û�鵽��Ӧ����Ҫ�´�����Ϣ��������ɾ������");
			return result;
		}
		JSONObject jsonMesg = new JSONObject();
		jsonMesg.put("service_name", "month_card_sync");
		jsonMesg.put("data", jsonObj.toString());
		logger.error(jsonMesg.toString());
		boolean isSend = doBackMessage(jsonMesg.toString(), channel);
		logger.error(">>>>>>>>>>>>>>�ƶ˷������ݵ�ͣ���շ�ϵͳ�����"+isSend);
		if(isSend){
			result = "1";
		}
		return result;
	}

	/**
	 * ���ƺ�̨�޸ĵļ۸�Ϣ���͵������շ�ϵͳ
	 * @param tableName
	 * @param tableId
	 * @param comid
	 * @param operate
	 * @return
	 */
	private String sendPriceInfo(String tableName, Long tableId, Long comid,
			Integer operate) {
		String result="0";
		//��ȡ�´����ݵ�ͨ����Ϣ
		Channel channel = NettyChannelMap.get(getChannel(String.valueOf(comid)));
		if (channel == null || !channel.isActive()|| !channel.isWritable()) {//ͨ�������ã��ͻ��˵���
			logger.error(comid+"���ͻ��˵���.....");
			return null;
		}
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
		Map mapNeed = daService.getMap("select * from "+tableName+" where id=? ", new Object[]{tableId});
		if(mapNeed != null && !mapNeed.isEmpty()){
			String mapNeedStr = StringUtils.createJson(mapNeed);
			jsonObj = JSONObject.fromObject(mapNeedStr);
			jsonObj.put("operate_type", operate);
		}else{
			logger.error(">>>>>>>>>>>>>û�鵽��Ӧ����Ҫ�´�����Ϣ��������ɾ������");
			return result;
		}
		JSONObject jsonMesg = new JSONObject();
		jsonMesg.put("service_name", "price_sync");
		jsonMesg.put("data", jsonObj.toString());
		logger.error(jsonMesg.toString());
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
		if (channel == null || !channel.isActive()|| !channel.isWritable()) {//ͨ�������ã��ͻ��˵���
			logger.error(comid+"���ͻ��˵���.....");
			return null;
		}
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
		/*String result="0";
		//��ȡ�´����ݵ�ͨ����Ϣ
		Channel channel = NettyChannelMap.get(getChannel(String.valueOf(comid)));
		if (channel == null || !channel.isActive()|| !channel.isWritable()) {//ͨ�������ã��ͻ��˵���
			logger.error(comid+"���ͻ��˵���.....");
			return null;
		}
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
		Map mapNeed = daService.getMap("select * from "+tableName+" where id=? ", new Object[]{tableId});
		if(mapNeed != null && !mapNeed.isEmpty()){
			jsonObj.put("card_id", String.valueOf(mapNeed.get("card_id")));
			jsonObj.put("member_id", mapNeed.get("member_id")==null?"":mapNeed.get("member_id"));
			jsonObj.put("create_time", (Long)mapNeed.get("create_time"));
			jsonObj.put("update_time", (Long)mapNeed.get("update_time"));
			jsonObj.put("begin_time", (Long)mapNeed.get("b_time"));
			jsonObj.put("end_time", (Long)mapNeed.get("e_time"));
			jsonObj.put("pid", Integer.valueOf(String.valueOf(mapNeed.get("pid"))));
			jsonObj.put("name", mapNeed.get("name")==null?"":mapNeed.get("name"));
			jsonObj.put("car_number", mapNeed.get("car_number")==null?"":mapNeed.get("car_number"));
			jsonObj.put("price",StringUtils.formatDouble(mapNeed.get("act_total")));
			jsonObj.put("operate_type", operate);
		}else{
			logger.error(">>>>>>>>>>>>>û�鵽��Ӧ����Ҫ�´�����Ϣ��������ɾ������");
			return result;
		}
		JSONObject jsonMesg = new JSONObject();
		jsonMesg.put("service_name", "month_member_sync");
		jsonMesg.put("data", jsonObj.toString());
		logger.error(jsonMesg.toString());
		boolean isSend = doBackMessage(jsonMesg.toString(), channel);
		logger.error(">>>>>>>>>>>>>>�ƶ˷������ݵ�ͣ���շ�ϵͳ�����"+isSend);
		if(isSend){
			result = "1";
		}
		return result;*/
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
		Map parkMap = daService.getMap("select * from park_token_tb where park_id=? order by id desc ", new Object[]{comid});
		if(parkMap != null && !parkMap.isEmpty()){
			String localId = String.valueOf(parkMap.get("local_id"));
			if(!Check.isEmpty(localId)){
				channelPass = comid+"_"+localId;
			}
		}else{
			channelPass = comid;
		}
		logger.error("sdk comid:"+channelPass);
		return channelPass;
	}
	
}
