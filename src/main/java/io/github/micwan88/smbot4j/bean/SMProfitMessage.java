package io.github.micwan88.smbot4j.bean;

import java.util.ArrayList;

public class SMProfitMessage {
	private int errCode = 0;
	private ArrayList<SMProfit> smProfitList = null;

	/**
	 * @param smProfitList
	 */
	public SMProfitMessage(ArrayList<SMProfit> smProfitList) {
		this.smProfitList = smProfitList;
	}

	/**
	 * @param errCode
	 */
	public SMProfitMessage(int errCode) {
		this.errCode = errCode;
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
