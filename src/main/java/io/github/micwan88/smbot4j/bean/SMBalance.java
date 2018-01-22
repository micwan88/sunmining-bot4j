package io.github.micwan88.smbot4j.bean;

import java.math.BigDecimal;

public class SMBalance {
	private String coinName = null;
	private BigDecimal balance = null;
	private BigDecimal usdEquiv = null;
	
	public SMBalance(String coinName, BigDecimal balance, BigDecimal usdEquiv) {
		this.coinName = coinName;
		this.balance = balance;
		this.usdEquiv = usdEquiv;
	}

	public String getCoinName() {
		return coinName;
	}

	public void setCoinName(String coinName) {
		this.coinName = coinName;
	}

	public BigDecimal getBalance() {
		return balance;
	}

	public void setBalance(BigDecimal balance) {
		this.balance = balance;
	}

	public BigDecimal getUsdEquiv() {
		return usdEquiv;
	}

	public void setUsdEquiv(BigDecimal usdEquiv) {
		this.usdEquiv = usdEquiv;
	}
}
