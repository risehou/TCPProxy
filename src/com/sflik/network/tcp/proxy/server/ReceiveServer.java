package com.sflik.network.tcp.proxy.server; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.StringTokenizer;

import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;

import com.sflik.network.tcp.proxy.util.ProxyUtil;

public class ReceiveServer {
	private static Logger logger = Logger.getLogger(ReceiveServer.class
			.getName());

	private SSLSocketFactory sslSocketFactory;

	/**
     * 当前处理的请求数量
	 */
    private static int sCurrCount = 0;
    
    /**
     * 系统处理的请求总数
     */
    private static long sTotalCount = 0;

    /**
     * 信任的ACS IP
     */
    private static String iValidIP = "";
    
    /**
     * 监听客户端请求的端口
     */
    private int iLocalPort;
    
    
    private String iLocalIP;
    
    private  static int iServerPort;
    
    private static  String iServerIP;
    
    /**
     * 监听管理命令的端口
     */
    private int iAdminPort;
    
    /**
     * 接收到管理请求后建立的Socket
     */
    private Socket iAdminSocket = null;
    
    
    /**
     * 构造方法
     */
    public ReceiveServer(){
    
    }
    
    /**
     * 初始化系统的基本数据已经SSL连接
     * 
     * @throws Exception
     */
	private void init() throws Exception {
		try {
			iLocalPort = Integer.parseInt(ProxyUtil.getParameterByName("LocalPort"));
			iLocalIP = ProxyUtil.getParameterByName("LocalIP");
			
			iServerPort = Integer.parseInt(ProxyUtil.getParameterByName("ServerPort"));
			iServerIP = ProxyUtil.getParameterByName("ServerIP");
			
            iAdminPort = Integer.parseInt(ProxyUtil.getParameterByName("AdminPort"));
            iValidIP = ProxyUtil.getParameterByName("TrustClientIPAddress");
//            if (iValidIP == null || iValidIP.length() < 7){
//                throw new Exception("TrustClientIPAddress Set Error：[" + iValidIP + "]");
//            }
           //初始化SSL
           // initSSL();
            logger.info("Initialize System ... OK.");
		} catch (Exception e) {
            System.out.println("Initialize System ... FAIL");
            e.printStackTrace();
			throw e;
		}
	}
    

    
    /**
     * 启动通讯代理服务器
     * 
     * @throws Exception
     */
    private void startServer() throws Exception{
        try {
            ServerSocket tServerSocket = null;
            InetAddress myAD = Inet4Address.getByName(iLocalIP);
            //本地监听socket
            tServerSocket = new ServerSocket(iLocalPort, 1,myAD);
            // 接收客户机连接请求
            Proxy tProxy = new Proxy(tServerSocket, sslSocketFactory);
            //启动
            tProxy.start();
            
            logger.info("Start Server ... OK");
        } catch (Exception e) {
            System.out.println("Start Server ... FAIL");
            e.printStackTrace();
            throw e;
        }
    }

	/**
	 * @param aParamName
	 * @return
	 * @throws Exception
	 * @throws Exception
	 */
	public static void main(String args[]) throws Exception {

		if (args != null && args.length >= 1) {
			if (args[0].equalsIgnoreCase("stop")) {
                //初始化配置文件
                ProxyUtil.init();
                stop();
			} else if (args[0].equalsIgnoreCase("status")) {
                //初始化配置文件
                ProxyUtil.init();
				String tStatus = getStatus();
                if (tStatus != null && tStatus.length() >= 3){
                    StringTokenizer tST = new StringTokenizer(tStatus, "|");
                    String tTotalCount = tST.nextToken();
                    String tCurrCount = tST.nextToken();
                    System.out.println("CommProxy RUNNING");
                    System.out.println("Have Processed Requst Number: [" + tTotalCount + "]");
                    System.out.println("Now Processing Requst Number：[" + tCurrCount + "]");
                }
			} else if (args[0].equalsIgnoreCase("start")){
                ReceiveServer tReceiveServer = new ReceiveServer();
                //初始化配置文件
                ProxyUtil.init();

                //初始化系统
//                String sPW = null;
//                if (args.length >1) sPW = args[1];
                
                //初始化证书和端口
                tReceiveServer.init();

                //启动Server 进行监听5432
                tReceiveServer.startServer();

                //启动AdminServer 5433
                ServerSocket tAdminServerSocket = tReceiveServer.startAdminSocket();
                
                //日志输出
                logger.info("Start Manager&View Port ... OK");

                
                logger.info("Local Listen IP：[" + tReceiveServer.iLocalIP + "] Port：[" + tReceiveServer.iLocalPort + "]");
                logger.info("Server IP：[" + tReceiveServer.iServerIP + "] Port：[" + tReceiveServer.iServerPort + "]");
                
                logger.info("Manager&View Listen Port：[" + tReceiveServer.iAdminPort + "]\n");

                tReceiveServer.waitForAdmin(tAdminServerSocket);
			} else {
                showUsage();
			}
		} else {
            showUsage();
		}
	} 


    /**
     * 停止系统服务
     */
    private void stopServices() {
        try {
            Proxy.shutDownThread();//设定停止服务标记，不再接收信息的请求。
            
            logger.info("Begin to stop ProxyScript ......");
            logger.info("  Waiting for all thread complete ......");  
            while(sCurrCount >0){
                logger.info("    Now Running Thread Number:[" + sCurrCount + "]");
                Thread.sleep(1000);
            }
            logger.info("Stop ProxyScript ... OK");  
            
            //响应系统管理请求
            logger.info("Response System manage request[stop system].\n");
            responseSysAdmin("STOPED\n");
            System.exit(0); 
        }
        catch (Exception e) {
            logger.error(e);
        }
    }
    
    /**
     * 响应系统管理请求
     */
    private void responseSysAdmin(String aResponseMsg) {
        OutputStreamWriter tOut = null;
        
        try {
            tOut = new OutputStreamWriter(iAdminSocket.getOutputStream(), "UTF-8");
            tOut.write(aResponseMsg);
            tOut.flush();
        }
        catch (Exception e) {
            logger.error(e);
        }
        finally {
            if (tOut != null) {
                try { tOut.close(); } catch (Exception e) { }
            }
        }
    }   
    
    /**
     * 启动系统管理监听端口
     */
    private ServerSocket startAdminSocket() throws Exception {
        int tBacklog = 1;
        InetAddress tLocalHostIP = null;;
        ServerSocket tServerSocket = null;
        try {
            tLocalHostIP = InetAddress.getByName("127.0.0.1");
            tServerSocket = new ServerSocket(iAdminPort, tBacklog, tLocalHostIP);
        }
        catch (UnknownHostException e) {
            throw new Exception("Cann't bind system manage IP:127.0.0.1");
        }
        catch (IOException e1) {
            throw new Exception("Cann't bind System manage listen port:" + iAdminPort);
        }

        return tServerSocket;
    }
    
    /**
     * 处理系统管理请求
     * 
     * @param aServerSocket
     */
    private void waitForAdmin(ServerSocket aServerSocket) {
        while (true) {
            BufferedReader tIn = null;
            
            try {
                //1、等待管理信息接入
                iAdminSocket = aServerSocket.accept();
                               
                //2、读取管理信息
                logger.info("Receive Manage Message");
                tIn = new BufferedReader(new InputStreamReader(iAdminSocket.getInputStream() ,"UTF-8"));
                String tReceiveMessage = tIn.readLine();
                logger.debug("Manage Message = [" + tReceiveMessage + "]");

                if (tReceiveMessage == null) {
                }                
                else if (tReceiveMessage.equalsIgnoreCase("STOP")) {
                    //3、停止服务
                    logger.info("Stop System Service ... ");
                    stopServices();
                    break;
                }
                else if (tReceiveMessage.equalsIgnoreCase("STATUS")) {
                    //4、停止服务
                    logger.info("System status query ... ");
                    responseSysAdmin(sTotalCount + "|" + sCurrCount + "\n");
                    logger.info("System status query ... DONE\n");
                }
                else {
                    logger.info("Can't recognise command\n");
                }
            }
            catch (IOException e) {
                logger.error(e);
            }
            finally {
                //4、关闭连线
                if (tIn != null) {
                    try { tIn.close(); } catch (Exception e) { }
                }
                
                if (iAdminSocket != null) {
                    try { iAdminSocket.close(); } catch (Exception e) { }
                }
            }
        }
    }
    
    /**
     * 显示程序启动帮助信息
     */
    private static void showUsage(){
        System.out.println("Invalid parameter");
        System.out.println("Usage：java com.hitrust.acs.proxy.server.ReceiveServer <operType>");
        System.out.println("operType: start|stop|status");
    }
    
    /**
     * 验证客户端请求IP是否在允许访问的IP列表中
     * 
     * @param aClientIP
     * @return true:合法的IP；false:非法的IP
     */
    public static boolean checkIP(String aClientIP) {
		logger.debug("ValidIP = [" + iValidIP + "]");
        int tIndex = iValidIP.indexOf(aClientIP);
        if (tIndex == -1){
            logger.error("Invalid Client IP:[" + aClientIP + "]!");
            return false;
        }else {
            return true;
        }
    }
    
    /**
     * 停止通知服务器(主调程序)
     */
    public static void stop() throws Exception {
        Socket tSocket = null;
        OutputStreamWriter tOut = null;
        BufferedReader tIn = null;
        
        try {
            System.out.print("Stop ProxyScript......");
            int tAdminPort = Integer.parseInt(ProxyUtil.getParameterByName("AdminPort"));

            //1、连线信息通知服务器
            tSocket = new Socket("127.0.0.1", tAdminPort);

            //2、提交信息
            tOut = new OutputStreamWriter(tSocket.getOutputStream(), "UTF-8");
            tOut.write("STOP\n");
            tOut.flush();

            //3、等待信息通知服务器结果
            tIn = new BufferedReader(new InputStreamReader(tSocket.getInputStream() ,"UTF-8"));
            tIn.readLine();
            System.out.println("System have stoped!\n");
        }
        catch (Exception e) {
            System.out.println("Can not detect Service Status！");
            throw e;
        }
        finally {
            //4、关闭连线
            if (tIn != null) {
                try { tIn.close(); } catch (Exception e) { }
            }
            
            if (tOut != null) {
                try { tOut.close(); } catch (Exception e) { }
            }
            
            if (tSocket != null) {
                try { tSocket.close(); } catch (Exception e) { }
            }
        }
    }
    
    /**
     * 查询通知服务器运行状态（主调程序）
     * 
     * @return 通知服务器运行状态("currCount|totalCount")，如果成功返回，代表服务器运行正常
     */
    private static String getStatus() throws Exception {
        String tStatus = "";
        Socket tSocket = null;
        OutputStreamWriter tOut = null;
        BufferedReader tIn = null;
        
        try {
            int tAdminPort = Integer.parseInt(ProxyUtil.getParameterByName("AdminPort"));

            //1、连线信息通知服务器
            tSocket = new Socket("127.0.0.1", tAdminPort);
            tSocket.setSoTimeout(10000);

            //2、提交信息
            tOut = new OutputStreamWriter(tSocket.getOutputStream(), "UTF-8");
            tOut.write("STATUS\n");
            tOut.flush();

            //3、等待信息通知服务器结果
            tIn = new BufferedReader(new InputStreamReader(tSocket.getInputStream() ,"UTF-8"));
            tStatus = tIn.readLine();
            
            //4、关闭连线
            tIn.close();
            tOut.close();
            tSocket.close();
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            //5、关闭连线
            if (tIn != null) {
                try { tIn.close(); } catch (Exception e) { }
            }
            
            if (tOut != null) {
                try { tOut.close(); } catch (Exception e) { }
            }
            
            if (tSocket != null) {
                try { tSocket.close(); } catch (Exception e) { }
            }
        }
        return tStatus;
    }    
    
    /**
     * 增加线程数量
     */
    public static synchronized void increaseThreads(){
        sCurrCount++;
    }

    /**
     * 减少线程数量
     */
    public static synchronized void decreaseThreads(){
        sCurrCount--;
    }
    
    /**
     * 增加系统处理的交易总数
     */
    public static void increaseTotalCount(){
        sTotalCount++;
    }

    /**
     * 取得当前运行中的线程数量
     * 
     * @return 当前运行中的线程数量
     */
    public static int getSCurrCount() {
        return sCurrCount;
    }
    
    public static String getiServerIP() {
		return iServerIP;
	}
    public static int getiServerPort() {
		return iServerPort;
	}
}
