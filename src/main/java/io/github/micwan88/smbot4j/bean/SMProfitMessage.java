package io.github.micwan88.smbot4j.bean;

import java.util.ArrayList;

public class SMProfitMessage {
	private String errMsg = null;
	private int errCode = 0;
	private ArrayList<SMProfit> smProfitList = null;
	
	public SMProfitMessage() {
	}

	/**
	 * @param errMsg
	 * @param errCode
	 */
	public SMProfitMessage(String errMsg, int errCode) {
		this.errMsg = errMsg;
		this.errCode = errCode;
	}

	/**
	 * @return the errMsg
	 */
	public String getErrMsg() {
		return errMsg;
	}

	/**
	 * @param errMsg the errMsg to set
	 */
	public void setErrMsg(String errMsg) {
		this.errMsg = errMsg;
	}

	/**
	 * @return the errCode
	 */
	public int getErrCode() {
		return errCode;
	}

	/**
	 * @param errCode the errCode to set
	 */
	public void setErrCode(int errCode) {
		this.errCode = errCode;
	}

	/**
	 * @return the smProfitList
	 */
	public ArrayList<SMProfit> getSmProfitList() {
		return smProfitList;
	}

	/**
	 * @param smProfitList the smProfitList to set
	 */
	public void setSmProfitList(ArrayList<SMProfit> smProfitList) {
		this.smProfitList = smProfitList;
	}
}
