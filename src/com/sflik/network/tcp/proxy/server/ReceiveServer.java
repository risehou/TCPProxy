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
     * ��ǰ�������������
	 */
    private static int sCurrCount = 0;
    
    /**
     * ϵͳ�������������
     */
    private static long sTotalCount = 0;

    /**
     * ���ε�ACS IP
     */
    private static String iValidIP = "";
    
    /**
     * �����ͻ�������Ķ˿�
     */
    private int iLocalPort;
    
    
    private String iLocalIP;
    
    private  static int iServerPort;
    
    private static  String iServerIP;
    
    /**
     * ������������Ķ˿�
     */
    private int iAdminPort;
    
    /**
     * ���յ��������������Socket
     */
    private Socket iAdminSocket = null;
    
    
    /**
     * ���췽��
     */
    public ReceiveServer(){
    
    }
    
    /**
     * ��ʼ��ϵͳ�Ļ��������Ѿ�SSL����
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
//                throw new Exception("TrustClientIPAddress Set Error��[" + iValidIP + "]");
//            }
           //��ʼ��SSL
           // initSSL();
            logger.info("Initialize System ... OK.");
		} catch (Exception e) {
            System.out.println("Initialize System ... FAIL");
            e.printStackTrace();
			throw e;
		}
	}
    

    
    /**
     * ����ͨѶ���������
     * 
     * @throws Exception
     */
    private void startServer() throws Exception{
        try {
            ServerSocket tServerSocket = null;
            InetAddress myAD = Inet4Address.getByName(iLocalIP);
            //���ؼ���socket
            tServerSocket = new ServerSocket(iLocalPort, 1,myAD);
            // ���տͻ�����������
            Proxy tProxy = new Proxy(tServerSocket, sslSocketFactory);
            //����
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
                //��ʼ�������ļ�
                ProxyUtil.init();
                stop();
			} else if (args[0].equalsIgnoreCase("status")) {
                //��ʼ�������ļ�
                ProxyUtil.init();
				String tStatus = getStatus();
                if (tStatus != null && tStatus.length() >= 3){
                    StringTokenizer tST = new StringTokenizer(tStatus, "|");
                    String tTotalCount = tST.nextToken();
                    String tCurrCount = tST.nextToken();
                    System.out.println("CommProxy RUNNING");
                    System.out.println("Have Processed Requst Number: [" + tTotalCount + "]");
                    System.out.println("Now Processing Requst Number��[" + tCurrCount + "]");
                }
			} else if (args[0].equalsIgnoreCase("start")){
                ReceiveServer tReceiveServer = new ReceiveServer();
                //��ʼ�������ļ�
                ProxyUtil.init();

                //��ʼ��ϵͳ
//                String sPW = null;
//                if (args.length >1) sPW = args[1];
                
                //��ʼ��֤��Ͷ˿�
                tReceiveServer.init();

                //����Server ���м���5432
                tReceiveServer.startServer();

                //����AdminServer 5433
                ServerSocket tAdminServerSocket = tReceiveServer.startAdminSocket();
                
                //��־���
                logger.info("Start Manager&View Port ... OK");

                
                logger.info("Local Listen IP��[" + tReceiveServer.iLocalIP + "] Port��[" + tReceiveServer.iLocalPort + "]");
                logger.info("Server IP��[" + tReceiveServer.iServerIP + "] Port��[" + tReceiveServer.iServerPort + "]");
                
                logger.info("Manager&View Listen Port��[" + tReceiveServer.iAdminPort + "]\n");

                tReceiveServer.waitForAdmin(tAdminServerSocket);
			} else {
                showUsage();
			}
		} else {
            showUsage();
		}
	} 


    /**
     * ֹͣϵͳ����
     */
    private void stopServices() {
        try {
            Proxy.shutDownThread();//�趨ֹͣ�����ǣ����ٽ�����Ϣ������
            
            logger.info("Begin to stop ProxyScript ......");
            logger.info("  Waiting for all thread complete ......");  
            while(sCurrCount >0){
                logger.info("    Now Running Thread Number:[" + sCurrCount + "]");
                Thread.sleep(1000);
            }
            logger.info("Stop ProxyScript ... OK");  
            
            //��Ӧϵͳ��������
            logger.info("Response System manage request[stop system].\n");
            responseSysAdmin("STOPED\n");
            System.exit(0); 
        }
        catch (Exception e) {
            logger.error(e);
        }
    }
    
    /**
     * ��Ӧϵͳ��������
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
     * ����ϵͳ��������˿�
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
     * ����ϵͳ��������
     * 
     * @param aServerSocket
     */
    private void waitForAdmin(ServerSocket aServerSocket) {
        while (true) {
            BufferedReader tIn = null;
            
            try {
                //1���ȴ�������Ϣ����
                iAdminSocket = aServerSocket.accept();
                               
                //2����ȡ������Ϣ
                logger.info("Receive Manage Message");
                tIn = new BufferedReader(new InputStreamReader(iAdminSocket.getInputStream() ,"UTF-8"));
                String tReceiveMessage = tIn.readLine();
                logger.debug("Manage Message = [" + tReceiveMessage + "]");

                if (tReceiveMessage == null) {
                }                
                else if (tReceiveMessage.equalsIgnoreCase("STOP")) {
                    //3��ֹͣ����
                    logger.info("Stop System Service ... ");
                    stopServices();
                    break;
                }
                else if (tReceiveMessage.equalsIgnoreCase("STATUS")) {
                    //4��ֹͣ����
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
                //4���ر�����
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
     * ��ʾ��������������Ϣ
     */
    private static void showUsage(){
        System.out.println("Invalid parameter");
        System.out.println("Usage��java com.hitrust.acs.proxy.server.ReceiveServer <operType>");
        System.out.println("operType: start|stop|status");
    }
    
    /**
     * ��֤�ͻ�������IP�Ƿ���������ʵ�IP�б���
     * 
     * @param aClientIP
     * @return true:�Ϸ���IP��false:�Ƿ���IP
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
     * ֹ֪ͣͨ������(��������)
     */
    public static void stop() throws Exception {
        Socket tSocket = null;
        OutputStreamWriter tOut = null;
        BufferedReader tIn = null;
        
        try {
            System.out.print("Stop ProxyScript......");
            int tAdminPort = Integer.parseInt(ProxyUtil.getParameterByName("AdminPort"));

            //1��������Ϣ֪ͨ������
            tSocket = new Socket("127.0.0.1", tAdminPort);

            //2���ύ��Ϣ
            tOut = new OutputStreamWriter(tSocket.getOutputStream(), "UTF-8");
            tOut.write("STOP\n");
            tOut.flush();

            //3���ȴ���Ϣ֪ͨ���������
            tIn = new BufferedReader(new InputStreamReader(tSocket.getInputStream() ,"UTF-8"));
            tIn.readLine();
            System.out.println("System have stoped!\n");
        }
        catch (Exception e) {
            System.out.println("Can not detect Service Status��");
            throw e;
        }
        finally {
            //4���ر�����
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
     * ��ѯ֪ͨ����������״̬����������
     * 
     * @return ֪ͨ����������״̬("currCount|totalCount")������ɹ����أ������������������
     */
    private static String getStatus() throws Exception {
        String tStatus = "";
        Socket tSocket = null;
        OutputStreamWriter tOut = null;
        BufferedReader tIn = null;
        
        try {
            int tAdminPort = Integer.parseInt(ProxyUtil.getParameterByName("AdminPort"));

            //1��������Ϣ֪ͨ������
            tSocket = new Socket("127.0.0.1", tAdminPort);
            tSocket.setSoTimeout(10000);

            //2���ύ��Ϣ
            tOut = new OutputStreamWriter(tSocket.getOutputStream(), "UTF-8");
            tOut.write("STATUS\n");
            tOut.flush();

            //3���ȴ���Ϣ֪ͨ���������
            tIn = new BufferedReader(new InputStreamReader(tSocket.getInputStream() ,"UTF-8"));
            tStatus = tIn.readLine();
            
            //4���ر�����
            tIn.close();
            tOut.close();
            tSocket.close();
        }
        catch (Exception e) {
            throw e;
        }
        finally {
            //5���ر�����
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
     * �����߳�����
     */
    public static synchronized void increaseThreads(){
        sCurrCount++;
    }

    /**
     * �����߳�����
     */
    public static synchronized void decreaseThreads(){
        sCurrCount--;
    }
    
    /**
     * ����ϵͳ����Ľ�������
     */
    public static void increaseTotalCount(){
        sTotalCount++;
    }

    /**
     * ȡ�õ�ǰ�����е��߳�����
     * 
     * @return ��ǰ�����е��߳�����
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
