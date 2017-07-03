package com.shimastripe.gpsmountainview;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Created by takagi_go on 2017/07/03.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "jobId",
        "ridge",
        "summit",
        "ridgeWidth",
        "ridgeHeight"
})
public class MountainRepository {

    @JsonProperty("jobId")
    private String jobId;
    @JsonProperty("ridge")
    private List<Integer> ridge = null;
    @JsonProperty("summit")
    private List<Mountain> summit = null;
    @JsonProperty("ridgeWidth")
    private Integer ridgeWidth;
    @JsonProperty("ridgeHeight")
    private Integer ridgeHeight;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("jobId")
    public String getJobId() {
        return jobId;
    }

    @JsonProperty("jobId")
    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    @JsonProperty("ridge")
    public List<Integer> getRidge() {
        return ridge;
    }

    @JsonProperty("ridge")
    public void setRidge(List<Integer> ridge) {
        this.ridge = ridge;
    }

    @JsonProperty("summit")
    public List<Mountain> getSummit() {
        return summit;
    }

    @JsonProperty("summit")
    public void setSummit(List<Mountain> summit) {
        this.summit = summit;
    }

    @JsonProperty("ridgeWidth")
    public Integer getRidgeWidth() {
        return ridgeWidth;
    }

    @JsonProperty("ridgeWidth")
    public void setRidgeWidth(Integer ridgeWidth) {
        this.ridgeWidth = ridgeWidth;
    }

    @JsonProperty("ridgeHeight")
    public Integer getRidgeHeight() {
        return ridgeHeight;
    }

    @JsonProperty("ridgeHeight")
    public void setRidgeHeight(Integer ridgeHeight) {
        this.ridgeHeight = ridgeHeight;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value);
    }

}
