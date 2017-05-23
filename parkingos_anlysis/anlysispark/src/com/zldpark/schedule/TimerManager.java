package com.zldpark.schedule;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import com.zldpark.facade.StatsAccountFacade;
import com.zldpark.impl.CommonMethods;
import com.zldpark.service.DataBaseService;
import com.zldpark.service.PgOnlyReadService;
import com.zldpark.service.StatsCardService;
import com.zldpark.utils.MemcacheUtils;

/**
 * ÿʮ�����ͳ��lala����
 * @author Administrator
 *
 */

public class TimerManager {
	
	Logger logger = Logger.getLogger(TimerManager.class);
	
	private static final long period1 = 10 * 60;//10����
	
	private static final long period2 = 30 * 60;//30����
	
	private static final long period3 = 60 * 60;//60����
	
	private static final long period4 = 24 * 60 * 60;//һ��
	
	private static final long period5 = 1 * 60;//1����
	
	private static final long period6= 2 * 60;//2����
	
	
	

	public TimerManager(DataBaseService dataBaseService, PgOnlyReadService pgOnlyReadService,
			MemcacheUtils memcacheUtils, CommonMethods commonMethods, StatsAccountFacade accountFacade,
			StatsCardService cardService) {
		/*��������ִ�к�ʱ�Ƿ���ڼ��ʱ�䣬scheduleAtFixedRate��scheduleWithFixedDelay�����ᵼ��ͬһ�����񲢷��ر�ִ�С�
		Ψһ��ͬ����scheduleWithFixedDelay�ǵ�ǰһ�����������ʱ�̣���ʼ������ʱ�䣬��0�뿪ʼִ�е�һ�����������ʱ5�룬������ʱ��3�룬
		��ô�ڶ�������ִ�е�ʱ�����ڵ�8�뿪ʼ��*/
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(20);
		//----------------------------1��������һ��-----------------------------------//
		DiciEventSchedule dicitask = new DiciEventSchedule(dataBaseService, pgOnlyReadService,
				memcacheUtils);
		executor.scheduleAtFixedRate(dicitask, getDelayTime(period5), period5, TimeUnit.SECONDS);
		//----------------------------10��������һ��-----------------------------------//
		TaskPeriod10m task1 = new TaskPeriod10m(dataBaseService, pgOnlyReadService,
				memcacheUtils, commonMethods);
		executor.scheduleAtFixedRate(task1, getDelayTime(period1), period1, TimeUnit.SECONDS);
		//----------------------------30��������һ��-----------------------------------//
		TaskPeriod30m task2 = new TaskPeriod30m(dataBaseService, pgOnlyReadService,
				memcacheUtils, commonMethods);
		executor.scheduleAtFixedRate(task2, getDelayTime(period2), period2, TimeUnit.SECONDS);
		//----------------------------1Сʱ����һ��-----------------------------------//
		TaskPeriod1h task3 = new TaskPeriod1h(dataBaseService, pgOnlyReadService,
				memcacheUtils, commonMethods);
		executor.scheduleAtFixedRate(task3, getDelayTime(period3), period3, TimeUnit.SECONDS);
		//----------------------------1������һ��-----------------------------------//
		TaskPeriod1d task4 = new TaskPeriod1d(dataBaseService, pgOnlyReadService,
				memcacheUtils, commonMethods, accountFacade, cardService);
		executor.scheduleAtFixedRate(task4, getDailyDelayTime(), period4, TimeUnit.SECONDS);
		
		//--------------------------֮ǰ�Ķ�ʱ����-------------------------------//
		ParkSchedule task5 = new ParkSchedule(dataBaseService, pgOnlyReadService,
				memcacheUtils, commonMethods);
		executor.scheduleAtFixedRate(task5, getDailyDelayTime(), period4, TimeUnit.SECONDS);
		
		TaskPeriod5s taskPeriod5s = new TaskPeriod5s(pgOnlyReadService);
		executor.scheduleAtFixedRate(taskPeriod5s, getDelayTime(period6), period6, TimeUnit.SECONDS);
	}
	
	
	/**
	 * ��ȡ��һ��ִ�ж�ʱ�������ʼʱ��
	 * @param period ���ʱ��(��λ/��)
	 * @return
	 */
	private Long getDelayTime(long period){
		Long time = System.currentTimeMillis() / 1000;
		time = period - time % period;
		return time;
	}
	
	private Long getDailyDelayTime(){
		Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);//ÿ��
        //ÿ����賿һ��ִ��
        calendar.set(year, month, day, 01, 00, 00);
        Date date = calendar.getTime();
        if(date.before(new Date())){//����賿һ���ʱ��ȵ�ǰʱ���磬��ȡ�ڶ����賿һ�㿪ʼ
        	calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        long time = calendar.getTimeInMillis() / 1000;
        Long curTime = System.currentTimeMillis() / 1000;
        long delay = time - curTime;
        logger.error("�ӳ�"+delay+"��ִ��ÿ��һ�ε�����");
        return delay;
	}
}
