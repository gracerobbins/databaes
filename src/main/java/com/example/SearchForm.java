//package com.hellokoding.springboot;
package com.example;

public class SearchForm {
    private String professorName;
    private String divisionName;
    private String departmentName;
    private String keywords;

    public String getProfessorName() {
        return professorName;
    }
    public void setProfessorName(String professorName) {
        this.professorName = professorName;
    }

    public String getDivisionName() {
        return divisionName;
    }
    public void setDivisionName(String divisionName) {
        this.divisionName = divisionName;
    }

    public String getDepartmentName() {
        return departmentName;
    }
    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public String getKeywords() {
        return keywords;
    }
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
}