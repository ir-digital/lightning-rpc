package de.seepex.domain;

public class Timings {

    private Long rabbitRTT;
    private Long totalOperationDuration;
    private Long methodExecutionTime;

    public Long getRabbitRTT() {
        return rabbitRTT;
    }

    public void setRabbitRTT(Long rabbitRTT) {
        this.rabbitRTT = rabbitRTT;
    }

    public Long getTotalOperationDuration() {
        return totalOperationDuration;
    }

    public void setTotalOperationDuration(Long totalOperationDuration) {
        this.totalOperationDuration = totalOperationDuration;
    }

    public Long getMethodExecutionTime() {
        return methodExecutionTime;
    }

    public void setMethodExecutionTime(Long methodExecutionTime) {
        this.methodExecutionTime = methodExecutionTime;
    }
}
