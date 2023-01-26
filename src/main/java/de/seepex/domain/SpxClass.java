package de.seepex.domain;

public class SpxClass {

    private String id;
    private String name;
    private String description;

    public SpxClass(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "SpxClass{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
