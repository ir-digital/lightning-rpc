package de.seepex.service;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

public class UpdateRedisMappingJob implements Job {

    @Autowired
    private ServiceCollector serviceCollector;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        serviceCollector.updateRoutingKeyMappings();
        serviceCollector.updateExclusiveRoutingKeyMappings();
    }
    
}
