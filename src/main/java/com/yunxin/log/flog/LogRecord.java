package com.yunxin.log.flog;

public class LogRecord {
    private StackTraceElement[] stackTraceElements;
    private String msg;
    private int level;
    private long time;
    private String _add;

    public LogRecord() {
    }

    public StackTraceElement[] getStackTraceElements() {
        return stackTraceElements;
    }

    public void setStackTraceElements(StackTraceElement[] stackTraceElements) {
        this.stackTraceElements = stackTraceElements;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String get_add() {
        return _add;
    }

    public void set_add(String _add) {
        this._add = _add;
    }
}
