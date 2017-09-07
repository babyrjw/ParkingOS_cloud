package com.zld.schedule;

import java.util.TimerTask;

import org.apache.log4j.Logger;

import com.zld.CustomDefind;
import com.zld.sdk.tcp.SensorServer;
import com.zld.utils.Check;

public class TcpTimerTask extends TimerTask {
	//������־
	Logger logger = Logger.getLogger(TcpTimerTask.class);
	@Override
	public void run() {
		String port=CustomDefind.getValue("TCP_SERVER_PORT");
		Integer serverPort = 6789;//Ĭ��6789
		if(port!=null&&Check.isNumber(port))
			serverPort = Integer.valueOf(port);
		logger.error("����netty��ܵ�tcp�ƶ˷�����--------start,on port:"+serverPort);
		try {
			new SensorServer().bind(serverPort);
		} catch (Exception e) {
			logger.error("����netty��ܵ�tcp�ƶ˷����������쳣--------exception");
			e.printStackTrace();
		}
	}

}
