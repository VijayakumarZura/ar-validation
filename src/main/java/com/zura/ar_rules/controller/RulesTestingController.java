package com.zura.ar_rules.controller;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.print.Doc;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
public class RulesTestingController {
    @Autowired
    private MongoTemplate mongoTemplate;


    @PostMapping("/checkValidations/{id}")
    public List<Criteria> checkValidations(@PathVariable String id, @RequestBody Map extractedJson) {
List<Criteria> criteriaList= new ArrayList<>();


        List listofConsultantEmail= mongoTemplate.findDistinct(
                new Query(), "consultantEmail", "Consultant", String.class);

//        //2.If we receive empty email without having any attachements
//
//        Criteria criteria = new Criteria().andOperator(Criteria.where("_id").is(new ObjectId(id)),
//                Criteria.where("consultants.timesheetData.timesheetPath").exists(true));
//        boolean result = mongoTemplate.exists(Query.query(criteria), "CentralizedJson");
//        System.out.println(" attachments available in the email  " + result);
//        criteriaList.add(criteria);
//
//        //1.Consultant Email doesn/t exist
//
//
//        criteria = new Criteria().andOperator(Criteria.where("_id").is(new ObjectId(id)),
//                Criteria.where("consultants.consultantDetails.consultantEmail")
//                        .in(mongoTemplate.findDistinct(new Query(),"consultantEmail", "Consultant", String.class)));
//         result = mongoTemplate.exists(Query.query(criteria), "CentralizedJson");
//        System.out.println(" Consultant Email doesn't exist " + result);
//
//        System.out.println("query"+Query.query(criteria));
//        criteriaList.add(criteria);
//
//
//        //3.If we receive multiple attachements in the email
//        criteria = new Criteria().andOperator(Criteria.where("_id").is(new ObjectId(id)),
//                Criteria.where("consultants.timesheetData.timesheetPath").exists(true).size(1));
//
//        result = mongoTemplate.exists(Query.query(criteria), "CentralizedJson");
//        System.out.println(" multiple attachements in the email  " + result);
//
//       //4.If we receive Invalid attachement
//        criteria = new Criteria().andOperator(Criteria.where("_id").is(new ObjectId(id)), Criteria.where("consultants.timesheetData.timesheetPath").exists(true).size(1), Criteria.where("consultants.timesheetData.timesheetPath.0").regex(".*\\.(pdf|jpg|jpeg|png)$", "i"));
//        result = mongoTemplate.exists(Query.query(criteria), "CentralizedJson");
//        System.out.println(" we receive Invalid attachement  " + result);
//
//        criteriaList.add(criteria);
//
//        //5 If we couldnot able to find Timesheet data in the attachment
//        criteria =new Criteria().andOperator(Criteria.where("_id").is(new ObjectId(id)), Criteria.where("consultants.timesheetData.timesheet").exists(true));
//        result = mongoTemplate.exists(Query.query(criteria), "CentralizedJson");
//        System.out.println(" we couldnot able to find Timesheet data in the attachment  " + result);
//        criteriaList.add(criteria);
//
//6.If consultant sends again the duplicate timesheet

    Criteria    criteria =new Criteria().andOperator(
                Criteria.where("consultants.consultantDetails.consultantName")
                        .is(((Map) extractedJson.get("consultantDetails")).get("consultantName").toString()),
                Criteria.where("consultants.clientDetails.clientName")
                        .is(((Map) extractedJson.get("clientDetails")).get("clientName").toString()),
                Criteria.where("consultants.timesheetData.timesheetStartDate")
                        .is(((Map) extractedJson.get("timesheetData")).get("timesheetStartDate").toString()),
                Criteria.where("consultants.timesheetData.timesheetEndDate")
                        .is(((Map) extractedJson.get("timesheetData")).get("timesheetEndDate").toString()));
      List<Document>  result = mongoTemplate.find(Query.query(criteria), Document.class,"CentralizedJson");
        System.out.println(" consultant sends again the duplicate timesheet  " + result);
            System.out.println(" doc sixe"+ result.size());
//        criteriaList.add(criteria);
//
//        //7.Consultant name  mismatch
//        criteria =new Criteria().andOperator(Criteria.where("_id").is(new ObjectId(id)), Criteria.where("consultants.consultantDetails.extractedConsultantName").in(mongoTemplate.findDistinct(new Query(),"consultantName", "Consultant", String.class)));
//        result = mongoTemplate.exists(Query.query(criteria), "CentralizedJson");
//        System.out.println(" Consultant name  mismatch  " + result);
//        criteriaList.add(criteria);
//
//
//        //8Client Name mismatch
//
//        criteria = new Criteria().andOperator(Criteria.where("_id").is(new ObjectId(id)), Criteria.where("consultants.clientDetails.clientName").is(((Map)extractedJson.get("clientDetails")).get("clientName").toString()));
//        result = mongoTemplate.exists(Query.query(criteria), "CentralizedJson");
//        System.out.println(" Client Name mismatch  " + result);
//        criteriaList.add(criteria);
//
        //9.If the consultant entered more than 24 working hours for the day.
        Aggregation aggregation = Aggregation.newAggregation(
                // Match documents by _id and where timesheet array exists
                Aggregation.match(Criteria.where("_id").is(new ObjectId("668258b7d03564b849e9157d"))
                        .and("consultants.timesheetData.timesheet").exists(true)),

                // Unwind stages to deconstruct arrays
                Aggregation.unwind("consultants"),
                Aggregation.unwind("consultants.timesheetData.timesheet"),

                // Project stage to calculate total hours
                Aggregation.project()
                        .andExpression("consultants.timesheetData.timesheet.approvedHours + consultants.timesheetData.timesheet.unapprovedHours")
                        .as("totalHours"),

                // Match stage to filter documents where total hours is less than 24
                Aggregation.match(Criteria.where("totalHours").lt(24))

                // Limit to 1 document to check existence
        );

        // Execute the aggregation
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "yourCollectionName", Document.class);

         if(results.getMappedResults().size() > 0){
             System.out.println("  doc found false"+results.getMappedResults().size());
         }
         else {
             System.out.println("  doc not found true"+results.getMappedResults().size());

         }
//        //10If there is UnApproved hours
//        criteria =new Criteria().andOperator(Criteria.where("_id").is(new ObjectId(id)),
//                Criteria.where("consultants.timesheetData.timesheet")
//                        .elemMatch(Criteria.where("unapprovedHours").exists(true).gte(0)));
//        result = mongoTemplate.exists(Query.query(criteria), "CentralizedJson");
//        System.out.println(" IIf there is UnApproved hours   " + result);
//        criteriaList.add(criteria);
//
//        //11.start date should not be a future date
//        LocalDate currentDate = LocalDate.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//        String CURRENT_NEW_DATE = currentDate.format(formatter);
//        criteriaList.add(criteria);
//
//        criteria = new Criteria().andOperator(
//                Criteria.where("_id").is(new ObjectId(id)),
//                Criteria.where("consultants.timesheetData.timesheetStartDate").lte(CURRENT_NEW_DATE));
//
//        result = mongoTemplate.exists(Query.query(criteria), "CentralizedJson");
//        System.out.println(" start date should not be a future date  " + result);
//        criteriaList.add(criteria);
//
//        //12.If the startDate of the timesheet greater than the endDate
//        criteria = new Criteria().andOperator(Criteria.where("_id").is(id),
//                Criteria.where("consultants.timesheetData.timesheetEndDate")
//                        .gte("$consultants.timesheetData.timesheetStartDate"));
//        result = mongoTemplate.exists(Query.query(criteria), "CentralizedJson");
//        System.out.println(" the startDate of the timesheet should not greater than the endDate  " + result);
//
//        criteriaList.add(criteria);
//
////13.end date should not be a future date
//
//        criteria = new Criteria().andOperator(
//                Criteria.where("_id").is(new ObjectId(id)),
//                Criteria.where("consultants.timesheetData.timesheetEndDate").lte(CURRENT_NEW_DATE));
//
//        result = mongoTemplate.exists(Query.query(criteria), "CentralizedJson");
//        System.out.println(" end date should not be a future date  " + result);
//        criteriaList.add(criteria);
//
//
////14. The dates of timesheet data are  greater than the  start date of the timesheet
//        new Criteria().andOperator(Criteria.where("_id").is(new ObjectId(id)),
//                Criteria.where("consultants.timesheetData.timesheet")
//                        .elemMatch(Criteria.where("date").gte("$consultants.timesheetData.timesheetStartDate")));
//        result = mongoTemplate.exists(Query.query(criteria), "CentralizedJson");
//        System.out.println(" The dates of timesheet data are  greater than the  start date of the timesheet  " + result);
//        criteriaList.add(criteria);
//
        //15.The dates of timesheet data are less than  the  end date of the timesheet
        criteria = new Criteria().andOperator(Criteria.where("_id").is(new ObjectId(id)),
                Criteria.where("consultants.timesheetData.timesheet")
                        .elemMatch(Criteria.where("date").lte("$consultants.timesheetData.timesheetEndDate")));

//        result = mongoTemplate.exists(Query.query(criteria), "CentralizedJson");
//        System.out.println(" The dates of timesheet data are less than  the  end date of the timesheet  " + result);

        criteriaList.add(criteria);

        return  criteriaList;

    }

}
