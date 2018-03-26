package com.dq.easy.cloud.pay.module.payment.config.dto;


import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.dq.easy.cloud.module.basic.utils.DqBaseUtils;
import com.dq.easy.cloud.module.common.sign.dto.DqCertDescriptorDTO;
import com.dq.easy.cloud.module.exception.bo.DqBaseBusinessException;
import com.dq.easy.cloud.pay.module.payment.constant.DqPayErrorCodeEnum;
import com.dq.easy.cloud.pay.module.paymessage.constant.DqMsgType;

/**
 * 
 * <p>
 * 支付基础配置存储
 * </p>
 *
 * @author daiqi
 * 创建时间    2018年2月23日 下午3:19:23
 */
public abstract class DqPayConfigStorageAbstract implements DqPayConfigStorageInf{

    private volatile Object attach;
    /**
     * 证书管理器
     */
    private volatile DqCertDescriptorDTO dqCertDescriptorDTO;

    /**
     *   应用私钥，rsa_private pkcs8格式 生成签名时使用
     */
    private volatile  String keyPrivate;
    /**
     *   应用私钥，rsa_private pkcs8格式 生成签名时使用
     */
    private volatile  String keyPrivateCertPwd;
    /**
     *  支付平台公钥(签名校验使用)
     */
    private volatile  String keyPublic;
    /**
     * 异步回调地址
     */
    private volatile  String notifyUrl;
    /**
     * 同步回调地址，支付完成后展示的页面
     */
    private volatile  String returnUrl;
    /**
     * 签名加密类型
     */
    private volatile  String signType;
    /**
     * 字符类型
     */
    private volatile  String inputCharset;


    /**
     * 支付类型 aliPay 支付宝， wxPay微信..等等，扩展支付模块定义唯一。
     */
    private volatile  String payType;

    /**
     * 消息来源类型
     */
    private volatile DqMsgType dqMsgType;


    /**
     *  访问令牌 每次请求其他方法都要传入的值
     */
    private volatile String accessToken;
    /**
     * access token 到期时间时间戳
     */
    private volatile long expiresTime;
    /**
     * 授权码锁
     */
    private Lock accessTokenLock = new ReentrantLock();
    /**
     * 是否为沙箱环境，默认为正式环境
     */
    private boolean isTest = false;

    /**
     * 是否为证书签名
     */
    private boolean isCertSign = false;

    @Override
    public Object getAttach() {
        return attach;
    }

    public void setAttach(Object attach) {
        this.attach = attach;
    }

    @Override
    public DqCertDescriptorDTO getCertDescriptor() {
        if (!isCertSign){
           throw DqBaseBusinessException.newInstance(DqPayErrorCodeEnum.PAY_CERTSIGN_IS_FLASE);
        }
        if (DqBaseUtils.isNull(dqCertDescriptorDTO)){
        	dqCertDescriptorDTO = DqCertDescriptorDTO.newInstance();
        }
        return dqCertDescriptorDTO;
    }

    @Override
    public String getKeyPrivate() {
        return keyPrivate;
    }

    public void setKeyPrivate(String keyPrivate) {
        this.keyPrivate = keyPrivate;
    }
    @Override
    public String getKeyPrivateCertPwd() {
        return keyPrivateCertPwd;
    }

    public void setKeyPrivateCertPwd(String keyPrivateCertPwd) {
        this.keyPrivateCertPwd = keyPrivateCertPwd;
    }

    @Override
    public String getKeyPublic() {
        return keyPublic;
    }

    public void setKeyPublic(String keyPublic) {
        this.keyPublic = keyPublic;
    }

    @Override
    public String getNotifyUrl() {
        return notifyUrl;
    }

    public void setNotifyUrl(String notifyUrl) {
        this.notifyUrl = notifyUrl;
    }


    @Override
    public String getReturnUrl() {
        return returnUrl;
    }

    public void setReturnUrl(String returnUrl) {
        this.returnUrl = returnUrl;
    }

    @Override
    public String getSignType() {
        return signType;
    }

    public void setSignType(String signType) {
        this.signType = signType;
    }

    @Override
    public String getInputCharset() {
        return inputCharset;
    }

    public void setInputCharset(String inputCharset) {
        this.inputCharset = inputCharset;
    }

    @Override
    public String getPayType() {
        return payType;
    }

    public void setPayType(String payType) {
        this.payType = payType;
    }

	public DqCertDescriptorDTO getDqCertDescriptorDTO() {
		return dqCertDescriptorDTO;
	}

	public void setDqCertDescriptorDTO(DqCertDescriptorDTO dqCertDescriptorDTO) {
		this.dqCertDescriptorDTO = dqCertDescriptorDTO;
	}

	public DqMsgType getDqMsgType() {
		return dqMsgType;
	}

	public void setDqMsgType(DqMsgType dqMsgType) {
		this.dqMsgType = dqMsgType;
	}

	@Override
    public String getAccessToken() {
        return this.accessToken;
    }

    @Override
    public Lock getAccessTokenLock() {
        return this.accessTokenLock;
    }

    @Override
    public long getExpiresTime() {
        return expiresTime;
    }

    @Override
    public boolean isAccessTokenExpired() {
        return System.currentTimeMillis() > this.expiresTime;
    }


    @Override
    public synchronized void updateAccessToken(String accessToken, int expiresInSeconds) {
        this.accessToken = accessToken;
        this.expiresTime = System.currentTimeMillis() + (expiresInSeconds - 600) * 1000L;
    }

    @Override
    public synchronized void updateAccessToken(String accessToken, long expiresTime) {
        this.accessToken = accessToken;
        this.expiresTime = expiresTime;
    }

    @Override
    public void expireAccessToken() {
        this.expiresTime = 0;
    }

    @Override
    public String getToken() {
        return null;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setExpiresTime(long expiresTime) {
        this.expiresTime = expiresTime;
    }

    public void setAccessTokenLock(Lock accessTokenLock) {
        this.accessTokenLock = accessTokenLock;
    }

    @Override
    public boolean isTest() {
        return isTest;
    }

    public void setTest(boolean test) {
        isTest = test;
    }

    public boolean isCertSign() {
        return isCertSign;
    }

    public void setCertSign(boolean certSign) {
        isCertSign = certSign;
        if (certSign){
            dqCertDescriptorDTO = DqCertDescriptorDTO.newInstance();
        }
    }
}