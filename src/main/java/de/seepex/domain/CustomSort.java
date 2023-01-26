package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomSort {

    private List<CustomSortOrder> sortOrders = new ArrayList<>();

    public List<CustomSortOrder> getSortOrders() {
        return sortOrders;
    }

    public void setSortOrders(List<CustomSortOrder> sortOrders) {
        this.sortOrders = sortOrders;
    }
}
