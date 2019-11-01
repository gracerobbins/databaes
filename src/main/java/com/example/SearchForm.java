//package com.hellokoding.springboot;
package com.example;

public class SearchForm {
    private String professorName;
    private String divisionName;
    private String departmentName;

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

    public void setDepartmentName(String deptartmentName) {
        this.departmentName = departmentName;
    }
}