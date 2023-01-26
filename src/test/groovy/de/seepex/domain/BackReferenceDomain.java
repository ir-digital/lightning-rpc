package de.seepex.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;

import java.util.List;

public class BackReferenceDomain {

    private String id;

    @JsonBackReference
    private BackReferenceDomain parent;

    private List<BackReferenceDomain> children;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BackReferenceDomain getParent() {
        return parent;
    }

    public void setParent(BackReferenceDomain parent) {
        this.parent = parent;
    }

    public List<BackReferenceDomain> getChildren() {
        return children;
    }

    public void setChildren(List<BackReferenceDomain> children) {
        this.children = children;
    }
}
