package com.sflik.network.tcp.proxy.server; 

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLSocketFactory;

import org.apache.log4j.Logger;


public class Proxy extends Thread {
	private static Logger logger = Logger.getLogger(Proxy.class.getName());

	private SSLSocketFactory sslSocketFactory;
    
    /**
     * ���տͻ��������ServerSocket
     */
    private ServerSocket iServerSocket = null;
    
    /**
     * �Ѿ������Ŀͻ�������Socket
     */
    private Socket iSocket = null;
    
	/**
     * ϵͳֹͣ���
	 */
    private static boolean sIsShutDown;

    /** 
     * ��Ҫ�̱߳�� 
     */
    private boolean iIsMainThread = false;

    
    /**
     * ���췽���������������̣߳�
     * 
     * @param aServerSocket
     * @param sslSocketFactory
     */
	public Proxy(ServerSocket aServerSocket, SSLSocketFactory sslSocketFactory) {
        this.iIsMainThread = true;
		this.iServerSocket = aServerSocket;
        this.sslSocketFactory = sslSocketFactory;
        
        
	}

    /**
     * ���췽���������������̣߳�
     * @param aSocket
     * @param sslSocketFactory
     */
    public Proxy(Socket aSocket, SSLSocketFactory sslSocketFactory) {
        this.iIsMainThread = false;
        this.iSocket = aSocket;
        this.sslSocketFactory = sslSocketFactory;
    }

    /**
     * �趨ϵͳֹͣ���
     */
	public synchronized static void shutDownThread() {
		sIsShutDown = true;
	}

    /**
     * ����ͨѶ�������߳�
     * �������췽����������Ķ���һ�������̣߳�������ACS����һ���Ǵ���ACS������̡߳�
     *
     * @version V1.0
     * @author yonggangguo
     */
	public void run() {
		
		//���߳�
            if (iIsMainThread){
                try{
                    while (true) { // �ȴ��û�����
                        if (sIsShutDown){
                            break;
                        }
    
                         //��ServerSocket -->socket
                        Socket tClientSocket = iServerSocket.accept();
                        tClientSocket.setSoTimeout(0);
                        logger.debug("new request coming.");
//                        //���Client IP���Ϸ�����ֹ����ǰ����
//                        if(!ReceiveServer.checkIP(tACSSocket.getInetAddress().getHostAddress())){
//                            continue;
//                        }
    
                        // ���տͻ�����������
                        Proxy tProxy = new Proxy(tClientSocket, sslSocketFactory);
                        tProxy.start();
                        
                        ReceiveServer.increaseTotalCount();
                    } 
                
                    //�ȴ����пͻ������������
                    while (ReceiveServer.getSCurrCount() > 0) {
                        logger.debug("Client Number is being processed = " + ReceiveServer.getSCurrCount());
                        Thread.sleep(1000);
                    }
    
                    logger.debug("ProxyScript Stop ...done.");
                } catch (Exception e){
                    logger.info("Start ProxyScript ...FAIL.");
                    e.printStackTrace();
                    System.exit(1);
                }
                
             //���߳�
            }else { 
                ReceiveServer.increaseThreads();
                PrintWriter out = null;
                BufferedReader in = null;
                Socket outbound = null;
                try {
                    if (sIsShutDown){
                        return;
                    }
                    outbound = new Socket(ReceiveServer.getiServerIP(), ReceiveServer.getiServerPort());
                    iSocket.setSoTimeout(1000);
                    InputStream is = iSocket.getInputStream();
                    outbound.setSoTimeout(1000);
                    OutputStream os = outbound.getOutputStream();
                    pipe(is, outbound.getInputStream(), os, iSocket.getOutputStream());
                } catch (Exception e) {
                    logger.error("Exception: "+e.getMessage());
                    logger.error(e.getStackTrace());
                } finally {
                	closeSocket(outbound);
                	closeSocket(iSocket);
                    ReceiveServer.decreaseThreads();
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                        	 logger.error("Exception: "+e.getMessage());
                        }
                    }
                    if (out != null) {
                        out.close();
                    }
                }
            }
	}



	
	
	private void pipe(InputStream is0, InputStream is1, OutputStream os0,OutputStream os1) {
		logger.debug("begin send data to server....");
		try {
			int ir;
			byte bytes[] = new byte[1024];
			while (true) {
				try {
					if ((ir = is0.read(bytes)) > 0) {
						os0.write(bytes, 0, ir);
					} else if (ir < 0) {
						break;
					}
				} catch (InterruptedIOException e) {
					logger.error("Exception:"+e);
					e.getStackTrace();
				}
				try {
					if ((ir = is1.read(bytes)) > 0) {
						os1.write(bytes, 0, ir);
					} else if (ir < 0) {
						break;
					}
				} catch (InterruptedIOException e) {
					logger.error("Exception:"+e);
					e.getStackTrace();
				}
			}
		} catch (Exception e0) {
			logger.error("Exception:"+e0);
			e0.getStackTrace();
		}
	}
	
	
	   //�ر�socket
    void closeSocket(Socket s) {
       try {
           s.close();
       } catch (Exception ef) {

       }
   }
}
