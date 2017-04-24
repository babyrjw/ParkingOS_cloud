package com.zld.pcloud_xunlang.handler.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.zld.pcloud_xunlang.Constants;
import com.zld.pcloud_xunlang.pojo.SensorCar;
import com.zld.pcloud_xunlang.pojo.SensorCar.Dici;
import com.zld.pcloud_xunlang.pojo.SensorCar.HeartBeat;
import com.zld.pcloud_xunlang.pojo.XunLangAttach;
import com.zld.pcloud_xunlang.pojo.XunLangAttach.Attach;
import com.zld.pcloud_xunlang.pojo.XunLangAttach.DiciAttach;
import com.zld.pcloud_xunlang.pojo.XunLangAttach.TranAttach;
import com.zld.pcloud_xunlang.util.HttpAsyncProxy;
import com.zld.pcloud_xunlang.util.TimeTools;
import com.zld.pcloud_xunlang.pojo.XunLangAttaches;
import com.zld.pcloud_xunlang.pojo.XunLangBase;
import com.zld.pcloud_xunlang.pojo.XunLangTimeCommand;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

public class XunLangHandler extends ChannelHandlerAdapter{

	Logger logger = Logger.getLogger(SensorServerHandler.class);

	@Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
		logger.error("一个新的连接");
        ctx.fireChannelActive();
    }
	
	@Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		logger.error("channelRead:"+msg);
		XunLangBase base = (XunLangBase)msg;
		if(base.getCommand() == Constants.UPLOAD_TIME){
			logger.error("时间请求");
			ctx.writeAndFlush(XunLangTimeCommand.getNowTimeCommand(base));
		}else if(base.getCommand() == Constants.UPLOAD_DICI){
			SensorCar sensor = (SensorCar)msg;
			Long curTime = System.currentTimeMillis()/1000;
			for(Dici dici :sensor.getCars()){
				String sensorID = sensor.getNid() + dici.getDiciId();
				if(dici.getStatus() == 0){
					Map<String, String> param = new HashMap<String, String>();
					param.put("sensornumber", sensorID);
					param.put("carouttime", TimeTools.getTime_yyyyMMdd_HHmmss(curTime * 1000));
					param.put("indicate", "00");
					//HttpAsyncProxy.post(Constants.SENSOR_OUT_CAR, param, sensorHandler);
					HttpAsyncProxy.post(Constants.SENSOR_OUT_CAR, param, new NullHandler());
							
				}else if(dici.getStatus() == 1){
					Map<String, String> param = new HashMap<String, String>();
					param.put("sensornumber", sensorID);
					param.put("carintime", TimeTools.getTime_yyyyMMdd_HHmmss(curTime * 1000));
					param.put("indicate", "00");
					//HttpAsyncProxy.post(Constants.SENSOR_IN_CAR, param, sensorHandler);
					HttpAsyncProxy.post(Constants.SENSOR_IN_CAR, param, new NullHandler());
				}
			}
			for(HeartBeat hb :sensor.getHeartBeats()){
				int sensorId = hb.getDiciId();
				int status = hb.getStatus();
				
				Map<String, String> param1 = new HashMap<String, String>();
				param1.put("sensornumber", String.valueOf(sensorId));
				param1.put("battery", "0");
				param1.put("site_uuid", sensor.getNid());//车检器所绑定的基站唯一编号
				//HttpAsyncProxy.post(Constants.SENSOR_HEART_URL, param, sensorHandler);
				HttpAsyncProxy.post(Constants.SENSOR_HEART_URL, param1, new NullHandler());
				
				
				Map<String, String> param2 = new HashMap<String, String>();
				param2.put("transmitternumber", sensor.getNid());
				param2.put("voltagecaution", "0");
				HttpAsyncProxy.post(Constants.SITE_HEART_URL, param2, new NullHandler());
			}
		}else if(base.getCommand() == Constants.UPLOAD_ATTACH){
			XunLangAttaches attaches = (XunLangAttaches)msg;
			List<XunLangAttach> list = attaches.getAttaches();
			for(XunLangAttach xunLangAttach : list){
				List<? extends Attach> attachList = xunLangAttach.getAttaches();
				if(attachList != null){
					for(Attach attach : attachList){
						if(attach.getType() == Constants.ATTACH_TYPE_DICI){
							DiciAttach item = (DiciAttach)attach;
							Map<String, String> param = new HashMap<String, String>();
							param.put("sensornumber", attaches.getNid()+item.getDiciId());
							param.put("battery", ((double)item.getVoltageBattery()/100) + "");
							param.put("site_uuid", attaches.getNid());//车检器所绑定的基站唯一编号
							//HttpAsyncProxy.post(Constants.SENSOR_HEART_URL, param, sensorHandler);
							HttpAsyncProxy.post(Constants.SENSOR_HEART_URL, param, new NullHandler());
						}else if(attach.getType() == Constants.ATTACH_TYPE_TRANS){
							//---------------------发送基站心跳消息-------------------//
							TranAttach item = (TranAttach)attach;
							Map<String, String> param = new HashMap<String, String>();
							param.put("transmitternumber", attaches.getNid());
							param.put("voltagecaution", ((double)item.getVoltage()/100) + "");
							HttpAsyncProxy.post(Constants.SITE_HEART_URL, param, new NullHandler());
						}
					}
				}
			}
		}
    }
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		cause.printStackTrace();
    }
	
	@Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.error("连接断开，释放资源" + ctx.channel());
        ctx.close();
    }
}
