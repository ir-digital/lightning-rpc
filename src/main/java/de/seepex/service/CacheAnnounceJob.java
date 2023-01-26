package de.seepex.service;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class CacheAnnounceJob implements Job {

    @Autowired
    private CacheContainer cacheContainer;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        cacheContainer.announceCaches();
    }
    
}
