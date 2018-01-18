package io.github.micwan88.smbot4j.bean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SMProfit {
	private Date profitDate = null;
	private int profitValue = 0;
	
	/**
	 * @param profitDate
	 * @param profitValue
	 */
	public SMProfit(Date profitDate, int profitValue) {
		this.profitDate = profitDate;
		this.profitValue = profitValue;
	}
	
	public SMProfit(String profitDateString, int profitValue) throws ParseException {
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
		this.profitDate = dateFormat.parse(profitDateString);
		this.profitValue = profitValue;
	}
	
	/**
	 * @return the profitDate
	 */
	public Date getProfitDate() {
		return profitDate;
	}

	/**
	 * @param profitDate the profitDate to set
	 */
	public void setProfitDate(Date profitDate) {
		this.profitDate = profitDate;
	}

	/**
	 * @return the profitValue
	 */
	public int getProfitValue() {
		return profitValue;
	}

	/**
	 * @param profitValue the profitValue to set
	 */
	public void setProfitValue(int profitValue) {
		this.profitValue = profitValue;
	}
}
