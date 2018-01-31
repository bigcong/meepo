package com.cc.meepo.model;

import java.math.BigDecimal;

public class Order {
    public BigDecimal number;
    private BigDecimal current;

    public BigDecimal getNumber() {
        return number;
    }

    public void setNumber(BigDecimal number) {
        this.number = number;
    }

    public BigDecimal getCurrent() {
        return current;
    }

    public void setCurrent(BigDecimal current) {
        this.current = current;
    }
}
