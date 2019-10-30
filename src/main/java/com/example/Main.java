/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;

@Controller
@SpringBootApplication
public class Main {

  // @Value("${spring.datasource.url}")
  @Value("jdbc:postgresql://localhost/gracerobbins?user=gracerobbins&password=mypassword&ssl=false")
  private String dbUrl;

  @Autowired
  private DataSource dataSource;

  public static void main(String[] args) throws Exception {
    SpringApplication.run(Main.class, args);
  }

  @RequestMapping("/")
  String index() {
    return "index";
  }

  @RequestMapping("/db")
  String db(Map<String, Object> model) {
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
      stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
      ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

      ArrayList<String> output = new ArrayList<String>();
      while (rs.next()) {
        output.add("Read from DB: " + rs.getTimestamp("tick"));
      }

      model.put("records", output);
      return "db";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "error";
    }
  }

  @GetMapping("/search")
  public String formGet() {
    return "i am returning the form here";
  }
  
  @PostMapping("/search")
  public String formPost(SearchForm submission, Map<String, Object> model) {
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ResearchDivision(name VARCHAR(255) PRIMARY KEY, description VARCHAR(5000), relatedWords VARCHAR(5000))");
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Professor(netId VARCHAR(15) PRIMARY KEY, name VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL, department VARCHAR(255), researchDivision VARCHAR(255) REFERENCES ResearchDivision(name))");
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Student(netId VARCHAR(15) PRIMARY KEY, name VARCHAR(255) NOT NULL, yearGraduating INT, major VARCHAR(255) NOT NULL, professor VARCHAR(15) REFERENCES Professor(netId))");
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS PersonalInterests(interest_id INT GENERATED ALWAYS AS IDENTITY, professor VARCHAR(15) REFERENCES Professor(netId), student VARCHAR(15) REFERENCES Student(netId), departmentInterests VARCHAR(5000), nondepartmentInterests VARCHAR(5000))");
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Qualifications(qualificationId INT GENERATED ALWAYS AS IDENTITY, studentId VARCHAR(15) NOT NULL REFERENCES Student(netId), skill VARCHAR(255), organization VARCHAR(255), award VARCHAR(255))");
      ResultSet rs = stmt.executeQuery("SELECT * FROM ResearchDivision WHERE name LIKE '%" + submission.getDivisionName() + "%'");

      ArrayList<String> output = new ArrayList<String>();
      while (rs.next()) {
        //output.add("Professor: " + rs.getString("name") + ",  email address: " + rs.getString("email") + ",  department: " + rs.getString("department"));
        output.add("Department: " + rs.getString("name"));
      }

      model.put("records", output);
      return "searchresults";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "error";
    }
  }

  @GetMapping("/updateProfessor")
  public String updateGet() {
    return "updateForm";
  }

  @PostMapping("/updateProfessor")
  public String updatePost(UpdateForm submission, Map<String, Object> model) {
    model.put("updateResults", submission.getProfessorNetId());
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();

      ArrayList<String> output = new ArrayList<String>();//Sarita Adve

      ResultSet firstCheck = stmt.executeQuery("SELECT * FROM Professor WHERE netId = '" + submission.getProfessorNetId() + "'");
      int numRows = 0;
      while (firstCheck.next()) {
        numRows++;
      }
      if (numRows == 1) {//if there are rows
        ResultSet prof = stmt.executeQuery("SELECT * FROM Professor WHERE netId = '" + submission.getProfessorNetId() + "'");
        prof.next();

        String oldName = prof.getString("name");
        String oldEmail = prof.getString("email");
        String oldDepartment = prof.getString("department");
        String oldDivision = prof.getString("researchDivision");

        if (submission.getProfessorName() != null && submission.getProfessorName() != "") {
          output.add("Updating name for " + oldName  + "...");
          stmt.execute("UPDATE Professor SET name = '" + submission.getProfessorName() + "' WHERE netId = '" + submission.getProfessorNetId() + "'");
          output.add("New name: " + submission.getProfessorName());
        }
        if (submission.getProfessorEmail() != null && submission.getProfessorEmail() != "") {
          output.add("Updating email for " + oldEmail + "...");
          stmt.execute("UPDATE Professor SET email = '" + submission.getProfessorEmail() + "' WHERE netId = '" + submission.getProfessorNetId() + "'");
          output.add("New email address: " + submission.getProfessorEmail());
        }
        if (submission.getProfessorDepartment() != null && submission.getProfessorDepartment() != "") {
          output.add("Updating department from " + oldDepartment + " to ...");
          stmt.execute("UPDATE Professor SET department = '" + submission.getProfessorDepartment() + "' WHERE netId = '" + submission.getProfessorNetId() + "'");
          output.add("New department: " + submission.getProfessorDepartment());
        }
        if (submission.getDivisionName() != null && submission.getDivisionName() != "") {
          output.add("Updating research division for " + oldDivision + "...");
          stmt.execute("UPDATE Professor SET researchDivision = '" + submission.getDivisionName() + "' WHERE netId = '" + submission.getProfessorNetId() + "'");
          output.add("New research division: " + submission.getDivisionName());
        }
        prof.close();
      }
      else {
        output.add("No professor with netId " + submission.getProfessorNetId() + " found.");
      }

      model.put("updateResults", output);
      return "updateResults";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "error";
    }
    
  }

  @Bean
  public DataSource dataSource() throws SQLException {
    if (dbUrl == null || dbUrl.isEmpty()) {
      return new HikariDataSource();
    } else {
      HikariConfig config = new HikariConfig();
      config.setJdbcUrl(dbUrl);
      return new HikariDataSource(config);
    }
  }

}
