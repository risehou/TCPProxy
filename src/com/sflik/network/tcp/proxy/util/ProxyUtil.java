/*
 * @(#)MessageTagName.java
 *
 * Copyright (c) 2005 HiTRUST Incorporated. All rights reserved.
 *
 * Modify History:
 *  v1.00, 2005/09/12, Tim Cao
 *   1) First release
 */
package com.sflik.network.tcp.proxy.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

public class ProxyUtil {
    private static Properties tServerProperties = new Properties();
    private static Logger logger = Logger.getLogger(ProxyUtil.class
            .getName());


    
    /**
     * ���ݲ�����ȡ������ֵ
     * 
     * @param aParamName
     * @return ����ȡ�õĲ���ֵ��
     * 
     * @throws Exception �������δ���ļ������ã��׳��쳣��
     */ 
    public static String getParameterByName(String aParamName) throws Exception {
		String tValue = tServerProperties.getProperty(aParamName, "");
		if (tValue.equals("")){
            logger.debug("Read parameter from properties file Fail! ");
            throw new Exception(" - no set[" + aParamName + "] parameter value!");
		}

		return tValue;
	}
    
    /**
     * ��ʼ�������ļ�
     * 
     * @throws IOException
     */
    public static void init() throws IOException, FileNotFoundException{          
        FileInputStream tFIS = new FileInputStream("./ProxyConfig.properties");
        tServerProperties.load(tFIS);
        tFIS.close();
        logger.info("initialize properties file ... OK");
    }
}
