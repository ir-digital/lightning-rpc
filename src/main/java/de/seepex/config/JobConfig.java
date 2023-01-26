package de.seepex.config;

import de.seepex.service.CacheAnnounceJob;
import de.seepex.service.UpdateRedisMappingJob;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobConfig {

    @Bean
    public JobDetail redisUpdateJob() {
        return JobBuilder
                .newJob(UpdateRedisMappingJob.class).withIdentity("redisUpdateMappingJob")
                .storeDurably().build();
    }

    @Bean
    public JobDetail cacheAnnounceJob() {
        return JobBuilder
                .newJob(CacheAnnounceJob.class).withIdentity("cacheAnnounceJob")
                .storeDurably().build();
    }

    @Bean
    public Trigger redisUpdateTrigger(JobDetail redisUpdateJob) {
        return TriggerBuilder.newTrigger().forJob(redisUpdateJob)
                .withIdentity("redisUpdateMappingTrigger")
                .withSchedule(
                        // every two minutes
                        CronScheduleBuilder
                                .cronSchedule("0 0/2 * ? * * *")
                                .withMisfireHandlingInstructionDoNothing()
                        )
                .build();
    }

    @Bean
    public Trigger cacheAnnounceTrigger(JobDetail cacheAnnounceJob) {
        return TriggerBuilder.newTrigger().forJob(cacheAnnounceJob)
                .withIdentity("cacheAnnounceTrigger")
                .withSchedule(
                        // every minutes
                        CronScheduleBuilder
                                .cronSchedule("0 0/1 * ? * * *")
                                .withMisfireHandlingInstructionDoNothing()
                )
                .build();
    }
}
