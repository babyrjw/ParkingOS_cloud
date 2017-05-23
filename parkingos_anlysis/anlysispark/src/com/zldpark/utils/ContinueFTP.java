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
		Create_Directory_Fail,      //远程服务器相应目录创建失败  
		Create_Directory_Success,   //远程服务器闯将目录成功  
		Upload_New_File_Success,    //上传新文件成功  
		Upload_New_File_Failed,     //上传新文件失败  
		File_Exits,                 //文件已经存在  
		Remote_Bigger_Local,        //远程文件大于本地文件  
		Upload_From_Break_Success,  //断点续传成功  
		Upload_From_Break_Failed,   //断点续传失败  
		Delete_Remote_Faild;        //删除远程文件失败  
	}  
	static{
		 ftpClient= new FTPClient();
		//设置PassiveMode传输
		ftpClient.enterLocalActiveMode();
//		ftpClient.enterLocalPassiveMode();
		ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
	}
	public ContinueFTP(){
		//设置将过程中使用到的命令输出到控制台
//		this.ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
	}

	/**
	 * 连接到FTP服务器
	 * @param hostname 主机名
	 * @param port 端口
	 * @param username 用户名
	 * @param password 密码
	 * @return 是否连接成功
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
	 * 从FTP服务器上下载文件
	 * @param remote 远程文件路径
	 * @param local 本地文件路径
	 * @return 是否成功
	 * @throws IOException
	 */
	public boolean download(String remote,String local) throws IOException{
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		boolean result;
		File f = new File(local);
		FTPFile[] files = ftpClient.listFiles(remote);
		if(files.length != 1){
			System.out.println("远程文件不唯一");
			return false;
		}
		long lRemoteSize = files[0].getSize();
		if(f.exists()){
			OutputStream out = new FileOutputStream(f,true);
			System.out.println("本地文件大小为:"+f.length());
			if(f.length() >= lRemoteSize){
				System.out.println("本地文件大小大于远程文件大小，下载中止");
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
	 * 上传文件到FTP服务器，支持断点续传
	 * @param local 本地文件名称，绝对路径
	 * @param remote 远程文件路径，使用/home/directory1/subdirectory/file.ext 按照Linux上的路径指定方式，支持多级目录嵌套，支持递归创建不存在的目录结构
	 * @return 上传结果
	 * @throws IOException
	 */
	public UploadStatus upload(String local,String remote) throws IOException{
		//设置以二进制流的方式传输
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
		UploadStatus result;
		//对远程目录的处理
		String remoteFileName = remote;
		if(remote.contains("/")){
			remoteFileName = remote.substring(remote.lastIndexOf("/")+1);
			String directory = remote.substring(0,remote.lastIndexOf("/")+1);
			if(!directory.equalsIgnoreCase("/")&&!ftpClient.changeWorkingDirectory(directory)){
				//如果远程目录不存在，则递归创建远程服务器目录
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
							System.out.println("创建目录失败");
							return UploadStatus.Create_Directory_Fail;
						}
					}

					start = end + 1;
					end = directory.indexOf("/",start);

					//检查所有目录是否创建完毕
					if(end <= start){
						break;
					}
				}
			}
		}

		//检查远程是否存在文件
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

			//尝试移动文件内读取指针,实现断点续传
			InputStream is = new FileInputStream(f);
			if(is.skip(remoteSize)==remoteSize){
				ftpClient.setRestartOffset(remoteSize);
				if(ftpClient.storeFile(remote, is)){
					return UploadStatus.Upload_From_Break_Success;
				}
			}

			//如果断点续传没有成功，则删除服务器上文件，重新上传
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
				//如果远程目录不存在，则递归创建远程服务器目录
				ftpClient.makeDirectory(directory);
			}
		}
		return result;
	}
	/**
	 * 断开与远程服务器的连接
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
							logger.error("临时文件移动到正式文件夹失败！"+ftpfiles[i].getName());
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
			logger.error("连接FTP出错："+e.getMessage()); 
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
			       logger.error("与ntp服务器同步时间错误！", e);
			       return dateFormat.format(new Date());
			   } catch (IOException e) { 
			    logger.error("与ntp服务器同步时间错误！", e);
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
			   Runtime.getRuntime().exec("cmd /c date " + date); //修改应用服务器年月日
			   Runtime.getRuntime().exec("cmd /c time " + time);//修改应用服务器时分秒
			} catch (IOException e) {
			logger.error("与ntp服务器同步时间错误！", e);
			} 
			}
}
