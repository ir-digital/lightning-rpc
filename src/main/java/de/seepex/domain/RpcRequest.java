package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonAlias;

import java.util.ArrayList;
import java.util.List;

public class RpcRequest {

    @JsonAlias({"service_id", "serviceId"})
    private String serviceId;
    private String method;
    private List<Param> params;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<Param> getParams() {
        if(this.params == null) {
            this.params = new ArrayList<>();
        }
        return params;
    }

    public void setParams(List<Param> params) {
        this.params = params;
    }

    public void addParam(Param param) {
        if(this.params == null) {
            this.params = new ArrayList<>();
        }

        this.params.add(param);
    }

    @Override
    public String toString() {
        return "RpcCommand{" +
                "serviceId='" + serviceId + '\'' +
                ", method='" + method + '\'' +
                ", params=" + params +
                '}';
    }
}
