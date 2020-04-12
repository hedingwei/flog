package com.yunxin.log.flog;

import java.io.File;
import java.io.FileFilter;
import java.sql.*;
import java.util.*;

public class LogDBHelper {

    private String logDir = null;

    private File currentDBFile = null;

    private long maxLogRecored = 1000;

    private CacheList<LogRecord> cache = null;

    private boolean isDisposed = false;

    private Thread cacheMonitorThread = null;

    private Connection connection = null;

    private PreparedStatement flogInsertPS = null;

    private int currentPersistentCount = 0;


    public LogDBHelper(String logDir){
        this(logDir,10*1000);
    }

    public LogDBHelper(String logDir, long maxLogRecored) {
        this.logDir = logDir;
        this.maxLogRecored = maxLogRecored;
        initDB(false);
        initCache();
        initCacheMonitor();
    }

    private void initCacheMonitor() {
        if(cacheMonitorThread==null){
            cacheMonitorThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!isDisposed){
                        try{
                            List<LogRecord> l = cache.getList();
                            cache.setList(new ArrayList<LogRecord>());
                            saveLogRecord(l);

                        }catch (Throwable t){
                            t.printStackTrace();
                        }
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            cacheMonitorThread.start();
        }

    }

    private synchronized void saveLogRecord(List<LogRecord> list){

        int tempIndex = -1;
        int newCount = 0;
        for(int i=0;i<list.size();i++){
            if(currentPersistentCount>=maxLogRecored){
                tempIndex = i;
                break;
            }else{
                saveLogRecord(list.get(i));

                try {
                    flogInsertPS.addBatch();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                currentPersistentCount++;
                newCount++;

            }
        }

        if(newCount>0){
            try {
                flogInsertPS.executeBatch();
                System.out.println("currentPersistentCount updated: "+currentPersistentCount+", total: "+getLogCount());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        if(tempIndex>=0){
            initDB(true);
            List<LogRecord> lrs = new ArrayList<>();
            for(int i=tempIndex;i<list.size();i++){
                lrs.add(list.get(i));
            }
            saveLogRecord(lrs);
        }


    }

    public int getLogCount(){
        return currentPersistentCount + cache.getList().size();
    }

    private void saveLogRecord(LogRecord record){
        try {

            flogInsertPS.setLong(1,record.getTime());
            flogInsertPS.setInt(2,record.getLevel());
            StringBuilder sb = new StringBuilder();
            for(StackTraceElement ste: record.getStackTraceElements()){
                if(ste.getClassName().equals("com.yunxin.log.flog.LogDBHelper")&&(ste.getMethodName().equals("log"))){
                    continue;
                }
                sb.append(ste.toString()).append("\n");
            }
            flogInsertPS.setString(3,sb.toString());
            flogInsertPS.setString(4,record.getMsg());
            flogInsertPS.setString(5,record.get_add());

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initDB(boolean newOne) {
        File dbDir = new File(logDir);
        if(!dbDir.exists()){
            dbDir.mkdirs();
        }

        File[] files = dbDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                String fileName = pathname.getName();
                if(fileName.matches(".*[0-9]*\\.log\\.db")){
                    return true;
                }
                return false;
            }
        });

        if(files.length>0){
            List<File> fileList = Arrays.asList(files);
            Collections.sort(fileList, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    int i1 = Integer.parseInt(o1.getAbsoluteFile().getName().replace(".log.db",""));
                    int i2 = Integer.parseInt(o2.getAbsoluteFile().getName().replace(".log.db",""));
                    return i1-i2;
                }
            });
            if(newOne){
                currentDBFile = new File(logDir,fileList.size()+".log.db");
            }else{
                currentDBFile = fileList.get(fileList.size()-1);
            }

        }else{
            currentDBFile = new File(logDir,"0.log.db");
        }

        try {
            if(connection!=null){
                try{
                    connection.close();
                    connection = null;
                }catch (Throwable t){
                    t.printStackTrace();
                }
            }
             connection = DriverManager.getConnection("jdbc:sqlite:"+currentDBFile.getAbsoluteFile().getAbsolutePath());//需要先创建文件夹dbs，数据库文件才会自动创建
            Statement state = connection.createStatement();
            initTables(state);
            flogInsertPS = connection.prepareStatement("insert into flog (_time, _level, _st, _msg, _add) values (?,?,?,?,?) ");
            PreparedStatement countLog = connection.prepareStatement("select count(*) from flog");
            ResultSet rs = countLog.executeQuery();
            while(rs.next()){
                int count = rs.getInt(1);
                currentPersistentCount = count;

            }

        }catch (Throwable t){
            t.printStackTrace();
        }



    }

    private void initCache() {
        cache = new CacheList<>(1231);
        cache.getCacheFullListenerList().add(new CacheList.ICacheFullListener<LogRecord>() {
            @Override
            public void onCacheFullEvent(List<LogRecord> list) {
              if(!list.isEmpty()){
                  saveLogRecord(list);
              }
            }
        });
    }


    private void initTables(Statement statement) throws SQLException {

        String ct_flog = "CREATE TABLE IF NOT EXISTS flog\n" +
                    "(\n" +
                    "    id INTEGER PRIMARY KEY AUTOINCREMENT DEFAULT 0 NOT NULL,\n" +
                    "    _time INTEGER NOT NULL,\n" +
                    "    _level INTEGER NOT NULL,\n" +
                    "    _st TEXT,\n" +
                    "    _msg TEXT,\n" +
                    "    _add TEXT\n" +
                    ");\n" +
                    "CREATE UNIQUE INDEX flog_id_uindex ON flog (id);";
        statement.execute(ct_flog);


        String ct_stelement = "CREATE TABLE IF NOT EXISTS stelement\n" +
                "(\n" +
                "    id INTEGER PRIMARY KEY AUTOINCREMENT,\n" +
                "    fileName TEXT NOT NULL,\n" +
                "    lineNumber int NOT NULL,\n" +
                "    className TEXT NOT NULL,\n" +
                "    methodName TEXT NOT NULL,\n" +
                "    isNative int NOT NULL\n" +
                ");";
        statement.execute(ct_stelement);
    }


    public void log(String log, int level, String add){

        StackTraceElement[] stack = new Exception().getStackTrace();
        LogRecord logRecord = new LogRecord();
        logRecord.setTime(System.currentTimeMillis());
        logRecord.setMsg(log);
        logRecord.setLevel(level);
        logRecord.setStackTraceElements(stack);
        logRecord.set_add(add);
        cache.add(logRecord);
    }

    public void log(String log, int level){
        this.log(log,level,"");
    }

    public void log(String log, com.yunxin.log.flog.Level level){
        this.log(log,level.getLevel());
    }




    public static void main(String[] args) {
        final LogDBHelper helper = new LogDBHelper("./flog",10000);


        for(int j=0;j<20;j++){
            final int finalJ = j;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for(int i=0;i<10000;i++){
                        helper.log("hello"+" t:"+ finalJ +" i:"+i, Level.INFO);
                        if(i%100==0){
                            System.out.println("p: "+finalJ+", i:"+i);
                        }
                    }
                }
            }).start();
        }

    }


}
