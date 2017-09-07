package com.zld.sdk.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;


import net.sf.json.JSONObject;

import org.apache.log4j.Logger;

public class DownloadUtil {
	
	Logger logger = Logger.getLogger(DownloadUtil.class);
	
	
	/**
	 * ��ȡԤ֧������������
	 * ������Ϣ��������ţ�comid��������Ϣ��order
	 * @param data
	 */
	public void downloadPrepaidOrder(JSONObject data){
		if(data!=null){
			String comId = data.getString("comid");
			Channel clientChannel = NettyChannelMap.get(comId);
			if(clientChannel != null){
				if(clientChannel.isActive()){
					if(clientChannel.isWritable()){
						logger.error("���ص�¼��Ϣ"+data);
						byte[] req = ("\n" + data + "\r").getBytes();
						ByteBuf buf = Unpooled.buffer(req.length);
						buf.writeBytes(req);
						clientChannel.writeAndFlush(buf);
					}else{
						logger.error("tcpͨ����������д��-----do not write");
					}
				}else{
					logger.error("tcpͨ��δ��������------do not active");
				}
			}else{
				logger.error("tcpͨ�������쳣");
			}
		}
	}
	
	/**
	 * ������Ա��Ϣת��������
	 * @param data
	 */
	public void downloadUserInfo(JSONObject data){
		
	}
	
	/**
	 * �����ײ���Ϣת��������
	 * @param data
	 */
	public void downloadProductPackage(JSONObject data){
		
	}
	
	/**
	 * �����۸�ı���¼���Ϣת��������
	 * @param data
	 */
	public void downloadPrice(JSONObject data){
		
	}
	
	/**
	 * ���͸ı���Ϣת��������
	 * @param data
	 */
	public void downloadCarType(JSONObject data){
		
	}
	
	/**
	 * ��������ʼ����Ϣת��������
	 * @param data
	 */
	public void downloadServerInit(JSONObject data){
		
	}
	
}
