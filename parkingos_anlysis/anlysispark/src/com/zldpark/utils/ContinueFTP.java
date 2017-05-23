package com.zldpark.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;
import org.apache.log4j.Logger;

import com.zldpark.CustomDefind;

public class ContinueFTP {
	private static Logger logger = Logger.getLogger(ContinueFTP.class);
	static FTPClient ftpClient = null;
	public enum UploadStatus {  
		Create_Directory_Fail,      //Զ�̷�������ӦĿ¼����ʧ��  
		Create_Directory_Success,   //Զ�̷���������Ŀ¼�ɹ�  
		Upload_New_File_Success,    //�ϴ����ļ��ɹ�  
		Upload_New_File_Failed,     //�ϴ����ļ�ʧ��  
		File_Exits,                 //�ļ��Ѿ�����  
		Remote_Bigger_Local,        //Զ���ļ����ڱ����ļ�  
		Upload_From_Break_Success,  //�ϵ������ɹ�  
		Upload_From_Break_Failed,   //�ϵ�����ʧ��  
		Delete_Remote_Faild;        //ɾ��Զ���ļ�ʧ��  
	}  
	static{
		 ftpClient= new FTPClient();
		//����PassiveMode����
		ftpClient.enterLocalActiveMode();
//		ftpClient.enterLocalPassiveMode();
		ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
	}
	public ContinueFTP(){
		//���ý�������ʹ�õ����������������̨
//		this.ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
	}

	/**
	 * ���ӵ�FTP������
	 * @param hostname ������
	 * @param port �˿�
	 * @param username �û���
	 * @param password ����
	 * @return �Ƿ����ӳɹ�
	 * @throws IOException
	 */
	public boolean connect(String hostname,int port,String username,String password) throws IOException{
		ftpClient.connect(hostname, port);
		if(FTPReply.isPositiveCompletion(ftpClient.getReplyCode())){
			if(ftpClient.login(username, password)){
				ftpClient.enterLocalActiveMode();
//				ftpClient.enterLocalPassiveMode();
				return true;
			}
		}
		disconnect();
		return false;
	}

	/**
	 * ��FTP�������������ļ�
	 * @param remote Զ���ļ�·��
	 * @param local �����ļ�·��
	 * @return �Ƿ�ɹ�
	 * @throws IOException
	 */
	public boolean download(String remote,String local) throws IOException{
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		boolean result;
		File f = new File(local);
		FTPFile[] files = ftpClient.listFiles(remote);
		if(files.length != 1){
			System.out.println("Զ���ļ���Ψһ");
			return false;
		}
		long lRemoteSize = files[0].getSize();
		if(f.exists()){
			OutputStream out = new FileOutputStream(f,true);
			System.out.println("�����ļ���СΪ:"+f.length());
			if(f.length() >= lRemoteSize){
				System.out.println("�����ļ���С����Զ���ļ���С��������ֹ");
				return false;
			}
			ftpClient.setRestartOffset(f.length());
			result = ftpClient.retrieveFile(remote, out);
			out.close();
		}else {
			OutputStream out = new FileOutputStream(f);
			result = ftpClient.retrieveFile(remote, out);
			out.close();
		}
		return result;
	}

	/**
	 * �ϴ��ļ���FTP��������֧�ֶϵ�����
	 * @param local �����ļ����ƣ�����·��
	 * @param remote Զ���ļ�·����ʹ��/home/directory1/subdirectory/file.ext ����Linux�ϵ�·��ָ����ʽ��֧�ֶ༶Ŀ¼Ƕ�ף�֧�ֵݹ鴴�������ڵ�Ŀ¼�ṹ
	 * @return �ϴ����
	 * @throws IOException
	 */
	public UploadStatus upload(String local,String remote) throws IOException{
		//�����Զ��������ķ�ʽ����
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		UploadStatus result;
		//��Զ��Ŀ¼�Ĵ���
		String remoteFileName = remote;
		if(remote.contains("/")){
			remoteFileName = remote.substring(remote.lastIndexOf("/")+1);
			String directory = remote.substring(0,remote.lastIndexOf("/")+1);
			if(!directory.equalsIgnoreCase("/")&&!ftpClient.changeWorkingDirectory(directory)){
				//���Զ��Ŀ¼�����ڣ���ݹ鴴��Զ�̷�����Ŀ¼
				int start=0;
				int end = 0;
				if(directory.startsWith("/")){
					start = 1;
				}else{
					start = 0;
				}
				end = directory.indexOf("/",start);
				while(true){
					String subDirectory = remote.substring(start,end);
					if(!ftpClient.changeWorkingDirectory(subDirectory)){
						if(ftpClient.makeDirectory(subDirectory)){
							ftpClient.changeWorkingDirectory(subDirectory);
						}else {
							System.out.println("����Ŀ¼ʧ��");
							return UploadStatus.Create_Directory_Fail;
						}
					}

					start = end + 1;
					end = directory.indexOf("/",start);

					//�������Ŀ¼�Ƿ񴴽����
					if(end <= start){
						break;
					}
				}
			}
		}

		//���Զ���Ƿ�����ļ�
//		ftpClient.enterLocalPassiveMode();
		FTPFile[] files = ftpClient.listFiles(remoteFileName);
		if(files.length == 1 ){
			long remoteSize = files[0].getSize();
			File f = new File(local);
			long localSize = f.length();
			if(remoteSize==localSize){
				return UploadStatus.File_Exits;
			}else if(remoteSize > localSize){
				return UploadStatus.Remote_Bigger_Local;
			}

			//�����ƶ��ļ��ڶ�ȡָ��,ʵ�ֶϵ�����
			InputStream is = new FileInputStream(f);
			if(is.skip(remoteSize)==remoteSize){
				ftpClient.setRestartOffset(remoteSize);
				if(ftpClient.storeFile(remote, is)){
					return UploadStatus.Upload_From_Break_Success;
				}
			}

			//����ϵ�����û�гɹ�����ɾ�����������ļ��������ϴ�
			if(!ftpClient.deleteFile(remoteFileName)){
				return UploadStatus.Delete_Remote_Faild;
			}
			is = new FileInputStream(f);
			if(ftpClient.storeFile(remote, is)){	
				result = UploadStatus.Upload_New_File_Success;
			}else{
				result = UploadStatus.Upload_New_File_Failed;
			}
			is.close();
		}else {
			InputStream is = new FileInputStream(local);
			if(ftpClient.storeFile(remoteFileName, is)){
				result = UploadStatus.Upload_New_File_Success;
			}else{
				result = UploadStatus.Upload_New_File_Failed;
			}
			is.close();
			ftpClient.changeWorkingDirectory("/");
			String directory = remote.substring(0,remote.lastIndexOf("/")+1).split("_")[0];
			if(!directory.equalsIgnoreCase("/")&&!ftpClient.changeWorkingDirectory(directory)){
				//���Զ��Ŀ¼�����ڣ���ݹ鴴��Զ�̷�����Ŀ¼
				ftpClient.makeDirectory(directory);
			}
		}
		return result;
	}
	/**
	 * �Ͽ���Զ�̷�����������
	 * @throws IOException
	 */
	public void disconnect() throws IOException{
		if(ftpClient.isConnected()){
			ftpClient.disconnect();
		}
	}

	public static void ftpUtil(String filename2,String filename1) {
//	public static void main(String[] args) {
		ContinueFTP myFtp = new ContinueFTP();
		UploadStatus uploadStatus1 = null;
		UploadStatus uploadStatus2 = null;
		try {
//			myFtp.connect("118.192.91.210", 21, "tcbftp", "tqserver");
			if(ftpClient==null){
				logger.error("ftpclient is null,create.....");
				ftpClient = new FTPClient();
			}
			if(!ftpClient.isConnected())
				myFtp.connect(CustomDefind.SERVERIP, Integer.parseInt(CustomDefind.FTPPORT), CustomDefind.FTPUSER, CustomDefind.FTPPWD);
			logger.error(ftpClient.isConnected());
			uploadStatus1 = myFtp.upload(filename2, CustomDefind.DIR+CustomDefind.PARKID+"/UP/BERTH_TMP/"+filename1);
			if(uploadStatus1==UploadStatus.Upload_New_File_Success||UploadStatus.Upload_From_Break_Success==uploadStatus1||uploadStatus1==UploadStatus.File_Exits){
				ftpClient.enterLocalActiveMode();
//				myFtp.ftpClient.changeWorkingDirectory("/home");
//				ftpClient.enterLocalActiveMode();
				FTPFile[] ftpfiles = ftpClient.listFiles(CustomDefind.DIR+CustomDefind.PARKID+"/UP/BERTH_TMP/");
				for(int i = 0;i<ftpfiles.length;i++){
					if (ftpfiles[i].isFile()) {   
						boolean ismove = myFtp.ftpClient.rename( CustomDefind.DIR+CustomDefind.PARKID+"/UP/BERTH_TMP/"+ftpfiles[i].getName(),  CustomDefind.DIR+CustomDefind.PARKID+"/UP/BERTH/"+ftpfiles[i].getName());
						if(!ismove){
							logger.error("��ʱ�ļ��ƶ�����ʽ�ļ���ʧ�ܣ�"+ftpfiles[i].getName());
						}
					}
				
				}
				 File root = new File("C:\\test\\month\\");
				    File[] files = root.listFiles();
				    int i = 0;
				    for(File f:files){    
				     if(!f.isDirectory()){
				    	 if(i==1){
				    		 break;
				    	 }
				    	 System.out.println(f.getAbsolutePath());
				    	 System.out.println(f.getName());
				    	 myFtp.ftpClient.changeWorkingDirectory("/");
				    	 uploadStatus2 = myFtp.upload(f.getAbsolutePath(), CustomDefind.DIR.substring(1)+CustomDefind.PARKID+"/UP/MONTH_TMP/"+f.getName().split("\\.")[0]+"_DELAY.xml");
				    	 if(uploadStatus2==UploadStatus.Upload_New_File_Success||UploadStatus.Upload_From_Break_Success==uploadStatus2){
				    		 ftpClient.enterLocalActiveMode();
				    		 myFtp.ftpClient.changeWorkingDirectory("/");
				    		 boolean b = myFtp.ftpClient.rename( CustomDefind.DIR.substring(1)+CustomDefind.PARKID+"/UP/MONTH_TMP/"+f.getName().split("\\.")[0]+"_DELAY.xml",  CustomDefind.DIR.substring(1)+CustomDefind.PARKID+"/UP/MONTH/"+f.getName().split("\\.")[0]+"_DELAY.xml");
								if(b){
									File file = new File(f.getAbsolutePath());
									if(file.exists()){
										file.delete();
									}
								}
							}
				    	 i++;
				     }
				}
			}
		} catch (IOException e) {
			logger.error("����FTP����"+e.getMessage()); 
			e.printStackTrace();
		}finally{
			try {
				myFtp.disconnect();
			} catch (IOException e) {
				e.printStackTrace();
			}
			File file = new File(filename2);
			if(file.exists()){
				file.delete();
			}
		}
	}
//	}

//	public static String getFTPTime() {
	public static String getFTPTime() {
		ContinueFTP myFtp = new ContinueFTP();
		UploadStatus uploadStatus1 = null;
		UploadStatus uploadStatus2 = null;
		try {
//			myFtp.connect("118.192.91.210", 21, "tcbftp", "tqserver");
			if(ftpClient==null){
				System.out.println("ftpclient is null,create.....");
				ftpClient = new FTPClient();
			}
			if(!ftpClient.isConnected())
				myFtp.connect(CustomDefind.SERVERIP, Integer.parseInt(CustomDefind.FTPPORT), CustomDefind.FTPUSER, CustomDefind.FTPPWD);
			ftpClient.removeDirectory("time/times");
			ftpClient.mkd("time/times");
			
//			if(a>0){
				FTPFile[] list = ftpClient.listFiles("time");
				if (list.length>0) {
					GregorianCalendar gc = (GregorianCalendar) list[0].getTimestamp();
					Date date = new Date(gc.getTimeInMillis()+gc.getTimeZone().getOffset(gc.getTimeInMillis()));
					SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					System.out.println(sf.format(date));
				}
		}catch (Exception e) {
			e.printStackTrace();
		}finally{
			
		}
		return null;
	}
		public static String getDateTime(){
			DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			try { 
			       NTPUDPClient timeClient = new NTPUDPClient(); 
			       InetAddress timeServerAddress = InetAddress.getByName("118.192.91.210");
			       TimeInfo timeInfo = timeClient.getTime(timeServerAddress); 
			       TimeStamp timeStamp = timeInfo.getMessage().getTransmitTimeStamp(); 
			       return dateFormat.format(timeStamp.getDate()); 
			   } catch (UnknownHostException e) { 
			       e.printStackTrace(); 
			       logger.error("��ntp������ͬ��ʱ�����", e);
			       return dateFormat.format(new Date());
			   } catch (IOException e) { 
			    logger.error("��ntp������ͬ��ʱ�����", e);
			       return dateFormat.format(new Date());
			   }
//			}
//		return null;
	}
		public void dateTimeSynchronization(){
			try {
			String datetime = getDateTime();
			   String date = datetime.substring(0, 10);
			   String time = datetime.substring(11);
			   Runtime.getRuntime().exec("cmd /c date " + date); //�޸�Ӧ�÷�����������
			   Runtime.getRuntime().exec("cmd /c time " + time);//�޸�Ӧ�÷�����ʱ����
			} catch (IOException e) {
			logger.error("��ntp������ͬ��ʱ�����", e);
			} 
			}
}
