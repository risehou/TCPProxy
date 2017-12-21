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
     * 接收客户端请求的ServerSocket
     */
    private ServerSocket iServerSocket = null;
    
    /**
     * 已经建立的客户端连接Socket
     */
    private Socket iSocket = null;
    
	/**
     * 系统停止标记
	 */
    private static boolean sIsShutDown;

    /** 
     * 主要线程标记 
     */
    private boolean iIsMainThread = false;

    
    /**
     * 构造方法（用于启动主线程）
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
     * 构造方法（用于启动子线程）
     * @param aSocket
     * @param sslSocketFactory
     */
    public Proxy(Socket aSocket, SSLSocketFactory sslSocketFactory) {
        this.iIsMainThread = false;
        this.iSocket = aSocket;
        this.sslSocketFactory = sslSocketFactory;
    }

    /**
     * 设定系统停止标记
     */
	public synchronized static void shutDownThread() {
		sIsShutDown = true;
	}

    /**
     * 启动通讯代理处理线程
     * 两个构造方法构造出来的对象，一个是主线程，负责监控ACS请求，一个是处理ACS请求的线程。
     *
     * @version V1.0
     * @author yonggangguo
     */
	public void run() {
		
		//主线程
            if (iIsMainThread){
                try{
                    while (true) { // 等待用户请求
                        if (sIsShutDown){
                            break;
                        }
    
                         //把ServerSocket -->socket
                        Socket tClientSocket = iServerSocket.accept();
                        tClientSocket.setSoTimeout(0);
                        logger.debug("new request coming.");
//                        //如果Client IP不合法，终止处理当前请求
//                        if(!ReceiveServer.checkIP(tACSSocket.getInetAddress().getHostAddress())){
//                            continue;
//                        }
    
                        // 接收客户机连接请求
                        Proxy tProxy = new Proxy(tClientSocket, sslSocketFactory);
                        tProxy.start();
                        
                        ReceiveServer.increaseTotalCount();
                    } 
                
                    //等待所有客户端请求处理完毕
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
                
             //子线程
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
	
	
	   //关闭socket
    void closeSocket(Socket s) {
       try {
           s.close();
       } catch (Exception ef) {

       }
   }
}
