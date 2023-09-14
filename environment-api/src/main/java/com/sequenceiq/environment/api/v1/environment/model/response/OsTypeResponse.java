package com.sequenceiq.environment.api.v1.environment.model.response;

public class OsTypeResponse {

    private String os;

    private String name;

    private String shortName;

    public String getOs() {
        return os;
    }

    public void setOs(String os) {
        this.os = os;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OsTypeResponse that = (OsTypeResponse) o;

        if (!os.equals(that.os)) {
            return false;
        }
        if (!name.equals(that.name)) {
            return false;
        }
        return shortName.equals(that.shortName);
    }

    @Override
    public int hashCode() {
        int result = os.hashCode();
        result = 31 * result + name.hashCode();
        result = 31 * result + shortName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "OsTypeResponse{" +
                "os='" + os + '\'' +
                ", name='" + name + '\'' +
                ", shortName='" + shortName + '\'' +
                '}';
    }
}
