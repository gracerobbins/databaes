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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import org.neo4j.driver.v1.*;

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

  // void getGraphResults(Map<String, Object> model, SearchForm submission) {
  //   ArrayList<String> output = new ArrayList<String>();
  //   String gdbURL = "https://app149777534-AXikUZ:b.XhT8pu5BrMoS.CVOh6wa6LJMsyNem@hobby-flhcicbddikmgbkeiaajgddl.dbs.graphenedb.com:24780";
  //   String graphenedbURL = "bolt://hobby-flhcicbddikmgbkeiaajgddl.dbs.graphenedb.com:24787";
  //   String graphenedbUser = "app149777534-AXikUZ";
  //   String graphenedbPass = "b.XhT8pu5BrMoS.CVOh6wa6LJMsyNem";
    
  //   try (Driver driver = GraphDatabase.driver(graphenedbURL, AuthTokens.basic(graphenedbUser, graphenedbPass))) {
  //       Session session = driver.session();
  //       StatementResult result = session.run("MATCH (a:Professor{name:'" + submission.getProfessorName() + "'})-[WorkedWith]->(b:Professor) RETURN b.name AS name");
  //       while ( result.hasNext() )
  //       {
  //         Record record = result.next();
  //         output.add( record.get("name").asString() );
  //       }

  //       model.put("graphResults", output);
  //       session.close();
  //       driver.close();
  //   } catch (Exception e) {
  //       output.add("error");
  //       model.put("graphResults", output);
  //   }
  // }

ArrayList<String> getGraphResults(SearchForm submission) {
  ArrayList<String> output = new ArrayList<String>();
  String gdbURL = "https://app149777534-AXikUZ:b.XhT8pu5BrMoS.CVOh6wa6LJMsyNem@hobby-flhcicbddikmgbkeiaajgddl.dbs.graphenedb.com:24780";
  String graphenedbURL = "bolt://hobby-flhcicbddikmgbkeiaajgddl.dbs.graphenedb.com:24787";
  String graphenedbUser = "app149777534-AXikUZ";
  String graphenedbPass = "b.XhT8pu5BrMoS.CVOh6wa6LJMsyNem";
  
  try (Driver driver = GraphDatabase.driver(graphenedbURL, AuthTokens.basic(graphenedbUser, graphenedbPass))) {
      Session session = driver.session();
      StatementResult result = session.run("MATCH (a:Professor{name:'" + submission.getProfessorName() + "'})-[WorkedWith]->(b:Professor) RETURN b.name AS name");
      while ( result.hasNext() )
      {
        Record record = result.next();
        output.add( record.get("name").asString() );
      }

      session.close();
      driver.close();
      return output;
  } catch (Exception e) {
      output.add("error");
      return output;
  }
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

  int init(Statement stmt) {
    try {
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ResearchDivision(name VARCHAR(255) PRIMARY KEY, description VARCHAR(5000), relatedWords VARCHAR(5000))");
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Professor(netId VARCHAR(15) PRIMARY KEY, name VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL, department VARCHAR(255), researchDivision VARCHAR(255) REFERENCES ResearchDivision(name))");
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Student(netId VARCHAR(15) PRIMARY KEY, name VARCHAR(255) NOT NULL, yearGraduating INT, major VARCHAR(255) NOT NULL, professor VARCHAR(15) REFERENCES Professor(netId))");
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS PersonalInterests(interest_id INT GENERATED ALWAYS AS IDENTITY, professor VARCHAR(15) REFERENCES Professor(netId), student VARCHAR(15) REFERENCES Student(netId), departmentInterests VARCHAR(5000), nondepartmentInterests VARCHAR(5000))");
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS Qualifications(qualificationId INT GENERATED ALWAYS AS IDENTITY, studentId VARCHAR(15) NOT NULL REFERENCES Student(netId), skill VARCHAR(255), organization VARCHAR(255), award VARCHAR(255))");

      String createProcedure = "CREATE OR REPLACE FUNCTION match_interests(keywords VARCHAR) RETURNS TABLE(n VARCHAR, e VARCHAR, dpt VARCHAR, div VARCHAR, hits INTEGER) AS $$ " + 
      "DECLARE cur REFCURSOR; " +
      "key_arr TEXT[]; " +
      "num_hits INTEGER; " +
      "key TEXT; " +
      "rec RECORD; " +
      "BEGIN " +
      "  CREATE TEMP TABLE temptable(n VARCHAR(255), e VARCHAR(255), dpt VARCHAR(255), div VARCHAR(255), hits INTEGER) ON COMMIT DROP; " +
      "  SELECT regexp_split_to_array(keywords, ',') INTO key_arr; " +
      "  OPEN cur FOR SELECT * FROM Professor a INNER JOIN PersonalInterests b ON a.netId = b.professor; " +
      "  LOOP " +
      "   FETCH cur INTO rec; " +
      "   EXIT WHEN NOT FOUND; " +
      "   num_hits := 0; " +
      "     FOREACH key IN ARRAY key_arr " +
      "     LOOP " +
      "       IF rec.departmentInterests ILIKE ('%' || key || '%') THEN num_hits := num_hits + 1; " +
      "       ELSIF rec.nondepartmentInterests ILIKE ('%' || key || '%') THEN num_hits := num_hits + 1; " +
      "       END IF; " +
      "     END LOOP; " +
      "    IF num_hits > 0 THEN INSERT INTO temptable VALUES (rec.name, rec.email, rec.department, rec.researchdivision, num_hits); END IF; " +
      "  END LOOP; " +
      "  RETURN QUERY SELECT * FROM temptable; " +
      "END; " +
      "$$ LANGUAGE plpgsql;";
      stmt.execute(createProcedure);
      return 0;
    } catch (Exception e) {
      return 1;
    }
  }
  
  @PostMapping("/search")
  public String formPost(SearchForm submission, Map<String, Object> model) {
    ArrayList<String> graphProfessors = getGraphResults(submission);

    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      if (init(stmt) == 1) {//if init throws an error
        model.put("message", "Sorry, an error occurred. Please try again.");
        return "error";
      }

      ArrayList<String> fullGraphInfo = new ArrayList<String>();
      for (int i = 0; i < graphProfessors.size(); i++) {
        System.out.println("adding graph results for " + graphProfessors.get(i));
        System.out.println("SELECT * FROM Professor WHERE name ILIKE '%" + graphProfessors.get(i) + "%';");
        ResultSet rs = stmt.executeQuery("SELECT * FROM Professor WHERE name ILIKE '%" + graphProfessors.get(i) + "%';");
        Boolean professorFoundInSQLDatabase = false;
        while (rs.next()) {
          professorFoundInSQLDatabase = true;
          fullGraphInfo.add("Professor: " + rs.getString("name") + "; Email: " + rs.getString("email") + "; Department: " + rs.getString("department") + "; Research Division: " + rs.getString("researchdivision"));
        }
        if (!professorFoundInSQLDatabase) {
          fullGraphInfo.add("Professor: " + graphProfessors.get(i) + "; " + graphProfessors.get(i) + " also worked with " + submission.getProfessorName() + ", but no further information was found.");
        }
      }
      model.put("graphResults", fullGraphInfo);

      ArrayList<String> division = new ArrayList<String>();
      if(submission.getDivisionName() != null && !submission.getDivisionName().isEmpty()){
        // ResultSet rs = stmt.executeQuery("SELECT * FROM Professor WHERE researchdivision ILIKE '%" + submission.getDivisionName() + "%'");
        // while (rs.next()) {
        //   division.add("Professor: " + rs.getString("name") + "; Email: " + rs.getString("email") + "; Department: " + rs.getString("department") + "; Research Division: " + rs.getString("researchdivision"));
        // }
        ResultSet rs = stmt.executeQuery("SELECT p.name, p.email, p.researchDivision, p.department, rd.description FROM Professor p JOIN ResearchDivision rd ON p.researchDivision = rd.name WHERE rd.name ILIKE '%" + submission.getDivisionName() + "%' OR rd.description ILIKE '%" + submission.getDivisionName() + "%' GROUP BY p.researchDivision, p.name, p.email, p.department, rd.description;");
        while (rs.next()) {
          division.add("Professor: " + rs.getString("name") + "; Email: " + rs.getString("email") + "; Department: " + rs.getString("department") + "; Research Division: " + rs.getString("researchdivision"));
        }
      }
      model.put("divisionRecords", division);

      ArrayList<String> professor = new ArrayList<String>();
      if(submission.getProfessorName() != null && !submission.getProfessorName().isEmpty()){
          ResultSet rs = stmt.executeQuery("SELECT * FROM Professor WHERE name ILIKE '%" + submission.getProfessorName() + "%'");
          while (rs.next()) {
            professor.add("Professor: " + rs.getString("name") + "; Email: " + rs.getString("email") + "; Department: " + rs.getString("department") + "; Research Division: " + rs.getString("researchdivision"));
          }
      }
      model.put("professorRecords", professor);

      ArrayList<String> department = new ArrayList<String>();
      if(submission.getDepartmentName() != null && !submission.getDepartmentName().isEmpty()){
          ResultSet rs = stmt.executeQuery("SELECT * FROM Professor WHERE department ILIKE '%" + submission.getDepartmentName() + "%'");
          while (rs.next()) {
            department.add("Professor: " + rs.getString("name") + "; Email: " + rs.getString("email") + "; Department: " + rs.getString("department") + "; Research Division: " + rs.getString("researchdivision"));
          }
      }
      model.put("departmentRecords", department);

      ArrayList<String> keyword = new ArrayList<String>();
      if(submission.getKeywords() != null && !submission.getKeywords().isEmpty()){
        ResultSet rs = stmt.executeQuery("SELECT * FROM match_interests('" + submission.getKeywords() + "') ORDER BY hits DESC");
        while (rs.next()) {
          System.out.println("Adding professor " + rs.getString("n") + " to model.");
          keyword.add("You share " + rs.getInt("hits") + " interests with Professor " + rs.getString("n") + "; Email: " + rs.getString("e") + "; Department: " + rs.getString("dpt") + "; Research Division: " + rs.getString("div"));
        }
          // String[] words = submission.getKeywords().split(", ");
          // for (int i = 0; i < words.length; i++) {
          //   System.out.println(words[i]);
          //   ResultSet rs = stmt.executeQuery("SELECT * FROM Professor a INNER JOIN PersonalInterests b ON a.netId = b.professor WHERE b.departmentInterests ILIKE '%" + words[i] + "%'");
          //   while (rs.next()) {
          //     keyword.add("Professor: " + rs.getString("name") + "; Interests: " + rs.getString("departmentInterests"));
          //   }
          // }
      }
      model.put("keywordRecords", keyword);

      ArrayList<String> combo = new ArrayList<String>();
      int numCriteria = 0;
      // String query = "SELECT * FROM Professor WHERE ";
      // if(submission.getDepartmentName() != null && !submission.getDepartmentName().isEmpty()){
      //   query += "department ILIKE '%" + submission.getDepartmentName() + "%'";
      //   numCriteria++;
      // }
      // if(submission.getDivisionName() != null && !submission.getDivisionName().isEmpty()){
      //   if (numCriteria > 0) {
      //     query += " AND ";
      //   }
      //   query += "researchdivision ILIKE '%" + submission.getDivisionName() + "%'";
      //   numCriteria++;
      // }
      // if(submission.getProfessorName() != null && !submission.getProfessorName().isEmpty()){
      //   if (numCriteria > 0) {
      //     query += " AND ";
      //   }
      //   query += "name ILIKE '%" + submission.getProfessorName() + "%'";
      //   numCriteria++;
      // }

      String createView = "CREATE OR REPLACE VIEW professorInterests AS SELECT p.name, p.email, p.department, p.researchDivision, pi.nondepartmentInterests, pi.departmentInterests FROM Professor p JOIN PersonalInterests pi ON p.netId = pi.professor; ";
      stmt.execute(createView);
      String query = "";
      if (submission.getDepartmentName() != null && !submission.getDepartmentName().isEmpty()){
        query += "SELECT * FROM professorInterests WHERE department ILIKE '%" + submission.getDepartmentName() + "%' ";
        numCriteria++;
      }
      if (submission.getDivisionName() != null && !submission.getDivisionName().isEmpty()){
        if (numCriteria > 0) {
          query += " INTERSECT ";
        }
        query += "SELECT * FROM professorInterests WHERE researchDivision ILIKE '%" + submission.getDivisionName() + "%' ";
        numCriteria++;
      }
      if (submission.getProfessorName() != null && !submission.getProfessorName().isEmpty()){
        if (numCriteria > 0) {
          query += " INTERSECT ";
        }
        query += "SELECT * FROM professorInterests WHERE name ILIKE '%" + submission.getProfessorName() + "%' ";
        numCriteria++;
      }
      if (submission.getKeywords() != null && !submission.getKeywords().isEmpty()){
        if (numCriteria > 0) {
          query += " INTERSECT ";
        }
        String[] keys = submission.getKeywords().split(",");
        query += "SELECT * FROM professorInterests WHERE nondepartmentinterests ILIKE '%" + keys[0] + "%' OR departmentinterests ILIKE '%" + keys[0] + "%' ";
        for (int i = 1; i < keys.length; i++) {
          query += "OR nondepartmentinterests ILIKE '%" + keys[i] + "%' ";
          query += "OR departmentinterests ILIKE '%" + keys[i] + "%' ";
        }
        numCriteria++;
      }

      if (numCriteria > 0) {
        System.out.println("built query: " + query);
        ResultSet rs = stmt.executeQuery(query);
        while (rs.next()) {
          combo.add("Professor: " + rs.getString("name") + "; Email: " + rs.getString("email") + "; Department: " + rs.getString("department") + "; Research Division: " + rs.getString("researchDivision"));
        }
      }
      model.put("comboRecords", combo);
   
     // model.put("records", output);
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
  public String updatePost(HttpServletRequest request, HttpServletResponse response, UpdateForm submission, Map<String, Object> model) {
    if (request.getParameter("Update") != null) {
    //action for update here
        model.put("updateResults", submission.getProfessorNetId());
        try (Connection connection = dataSource.getConnection()) {
          Statement stmt = connection.createStatement();

          ArrayList<String> output = new ArrayList<String>();

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
    
    } else if (request.getParameter("Add") != null) {
        //action for add
        model.put("Creation Results", submission.getProfessorNetId());
        try (Connection connection = dataSource.getConnection()) {
          Statement stmt = connection.createStatement();
          ArrayList<String> output = new ArrayList<String>();

          ResultSet firstCheck = stmt.executeQuery("SELECT * FROM Professor WHERE netId = '" + submission.getProfessorNetId() + "'");
          int numRows = 0;
          while (firstCheck.next()) {
            numRows++;
          }
          ResultSet secondCheck = stmt.executeQuery("SELECT name FROM ResearchDivision WHERE name = '" + submission.getDivisionName() + "'");
          int numDivisions = 0;
          while (secondCheck.next()) {
            numDivisions++;
          }
          if (numRows == 0 && numDivisions > 0) {//if the professor does not exist
            if (submission.getProfessorName() != null && submission.getProfessorName() != "" && submission.getProfessorNetId() != null && submission.getProfessorNetId() != "") {
              stmt.execute("INSERT INTO Professor VALUES ('" + submission.getProfessorNetId() + "', '" + submission.getProfessorName() + "', '" + submission.getProfessorEmail() + "', '" + submission.getProfessorDepartment() + "', '" + submission.getDivisionName() + "')");
              output.add("Professor " + submission.getProfessorName() + " added to the database!");            
            }
            else {
              output.add("Please enter non-null name and netid");
            }
          }
          else {
            output.add("Professor with netId " + submission.getProfessorNetId() + " already exists or that research division does not exist. Please submit a valid form.");
          }

          model.put("updateResults", output);
          return "updateResults";
        } catch (Exception e) {
          model.put("message", e.getMessage());
          return "error";
        }
    } 
    else {
        //action for delete
        model.put("Deletion Results", submission.getProfessorNetId());
        try (Connection connection = dataSource.getConnection()) {
          Statement stmt = connection.createStatement();
          ArrayList<String> output = new ArrayList<String>();

          ResultSet firstCheck = stmt.executeQuery("SELECT * FROM Professor WHERE netId = '" + submission.getProfessorNetId() + "'");
          int numRows = 0;
          while (firstCheck.next()) {
            numRows++;
          }
          if (numRows != 0) {//if the professor does not exist
            if (submission.getProfessorNetId() != null && submission.getProfessorNetId() != "") {
              stmt.execute("UPDATE student SET professor = NULL WHERE professor = '" + submission.getProfessorNetId() + "'");
              stmt.execute("DELETE FROM personalInterests WHERE professor = '" + submission.getProfessorNetId() + "'");
              stmt.execute("DELETE FROM Professor WHERE netId = '" + submission.getProfessorNetId() + "'");
              output.add("Professor with netid " + submission.getProfessorNetId() + " has been removed from the database");            
            }
            else {
              output.add("Please enter non-null netid");
            }
          }
          else {
            output.add("Professor with netId " + submission.getProfessorNetId() + " does not exist in out database.");
          }

          model.put("updateResults", output);
          return "updateResults";
        } catch (Exception e) {
          model.put("message", e.getMessage());
          return "error";
        }
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
