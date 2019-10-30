package com.example;

public class UpdateForm {
    private String professorNetId;
    private String professorName;
    private String professorEmail;
    private String professorDepartment;
    private String divisionName;

    //netid
    public String getProfessorNetId() {
        return professorNetId;
    }
    public void setProfessorNetId(String professorNetId) {
        this.professorNetId = professorNetId;
    }
    
    //name
    public String getProfessorName() {
        return professorName;
    }
    public void setProfessorName(String professorName) {
        this.professorName = professorName;
    }

    //email
    public String getProfessorEmail() {
        return professorEmail;
    }
    public void setProfessorEmail(String professorEmail) {
        this.professorEmail = professorEmail;
    }   
    
    //department
    public String getProfessorDepartment() {
        return professorDepartment;
    }
    public void setProfessorDepartment(String professorDepartment) {
        this.professorDepartment = professorDepartment;
    }

    //division
    public String getDivisionName() {
        return divisionName;
    }
    public void setDivisionName(String divisionName) {
        this.divisionName = divisionName;
    }
}