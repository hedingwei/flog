package com.yunxin.log.flog;

public enum Level {
    INFO(700), DEBUG(800), WARN(900), ERROR(1000), FATAL(1100);

    Level(int i) {
        level = i;
    }

    private int level;

    public int getLevel() {
        return level;
    }
}
