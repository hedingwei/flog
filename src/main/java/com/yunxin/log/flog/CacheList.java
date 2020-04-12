package com.yunxin.log.flog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CacheList<T> {

    private List<T> list = null;
    private int maxSize = 10;
    private List<ICacheFullListener<T>> cacheFullListenerList = null;
    private ExecutorService service = Executors.newSingleThreadExecutor();

    public CacheList(int maxSize) {
        list = new ArrayList<>();
        this.maxSize = maxSize;
        cacheFullListenerList = new ArrayList<>();
    }

    public synchronized void add(T element){
        if(list.size()>maxSize){
            final  List<T> fullList = list;
            list = new ArrayList<>();
            service.execute(new Runnable() {
                @Override
                public void run() {
                    for(ICacheFullListener<T> listener: cacheFullListenerList){
                        try{
                            listener.onCacheFullEvent(fullList);
                        }catch (Throwable t){ }
                    }
                }
            });
        }

        list.add(element);
    }


    public List<ICacheFullListener<T>> getCacheFullListenerList() {
        return cacheFullListenerList;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public static interface ICacheFullListener<T>{
        public void onCacheFullEvent(List<T> list );
    }

    public static void main(String[] args) {
    }
}
