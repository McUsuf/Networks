package com.vvboty;

import java.util.Date;

public class SendTry {
    private Date sent;
    private Integer msFromSending;
    private Integer tries;

    public Integer getTries() {
        return tries;
    }

    public void setTries(Integer tries) {
        this.tries = tries;
    }

    SendTry(){
        this.sent = new Date();
        this.msFromSending = 0;
        this.tries = 0;
    }

    public Date getSent() {
        return sent;
    }

    public void setSent(Date sent) {
        this.sent = sent;
    }

    public Integer getMsFromSending() {
        return msFromSending;
    }

    public void setMsFromSending(Integer msFromSending) {
        this.msFromSending = msFromSending;
    }
}