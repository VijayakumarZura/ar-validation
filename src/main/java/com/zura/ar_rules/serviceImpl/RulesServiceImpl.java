package com.zura.ar_rules.serviceImpl;

import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zura.ar_rules.encryption.Crypt;
import com.zura.ar_rules.model.Rules;
import com.zura.ar_rules.model.ValidationsResult;
import com.zura.ar_rules.repository.RulesRepository;
import com.zura.ar_rules.repository.ValidationsResultRepository;
import com.zura.ar_rules.service.RulesService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.BasicQuery;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.CriteriaDefinition;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service("RulesService")
public class RulesServiceImpl implements RulesService {

    @Autowired
    ValidationsResultRepository validationsResultRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    RulesRepository rulesRepository;

    ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.emailReplyUrl}")
    private String emailReplyUrl;

    @Value("${api.splitIntoDayWiseApi}")
    private String splitIntoDayWiseApi;


    @Override
    public ResponseEntity<?> createOrUpdateRule(List<Rules> rulesList) {
        ResponseEntity<?> responseEntity = null;
        try {
            for (Rules rule : rulesList) {
                Rules dbRule = null;
                if (rule.getId() != null) {
                    dbRule = rulesRepository.findById(rule.getId()).get();
                }
                if (dbRule != null) {
                    dbRule.setRule(rule.getRule());
                    dbRule.setEnabled(rule.isEnabled());
                    dbRule.setQuery(rule.getQuery());
                    dbRule.setErrorMessage(rule.getErrorMessage());
                    rule = rulesRepository.save(dbRule);
                    responseEntity = new ResponseEntity<>(
                            Collections.singletonMap("status", "Rule updated successfully"), HttpStatus.OK);
                } else {

                    rule = rulesRepository.save(rule);
                    responseEntity = new ResponseEntity<>(
                            Collections.singletonMap("status", "Rule created successfully"), HttpStatus.OK);

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            responseEntity = new ResponseEntity<>(Collections.singletonMap("status", e.getMessage()), HttpStatus.OK);

        }

        return responseEntity;
    }

    public ResponseEntity<?> getAllRules() {
        ResponseEntity<?> responseEntity = null;
        try {
            List<Rules> rulesList = rulesRepository.findByEnabled(true);
            responseEntity = new ResponseEntity<>(rulesList, HttpStatus.OK);
        } catch (Exception e) {
            responseEntity = new ResponseEntity<>(Collections.singletonMap("status", e.getMessage()), HttpStatus.OK);

        }
        return responseEntity;
    }

    @Override
    public ResponseEntity<?> getValidationsResultOfTimesheet(String jsonId) {
        ResponseEntity<?> responseEntity = null;

        try {
            ValidationsResult validationsResult = validationsResultRepository.findByCentralizedJsonId(jsonId);

            responseEntity = new ResponseEntity<>(validationsResult, HttpStatus.OK);
        } catch (Exception e) {
            responseEntity = new ResponseEntity<>(Collections.singletonMap("status", e.getMessage()), HttpStatus.OK);
        }

        return responseEntity;
    }

    public String currentDateTime() {
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
        String formattedDate = myDateObj.format(myFormatObj);
        return formattedDate;

    }

    public boolean validateConsultantName(String extractedConsultantName, String dbConsultantName) {
        boolean consultantFlag = false;
        extractedConsultantName = extractedConsultantName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
        dbConsultantName = dbConsultantName.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();


        char[] extractedCharArray = extractedConsultantName.toCharArray();
        Arrays.sort(extractedCharArray);
        extractedConsultantName = new String(extractedCharArray);

        char[] dbCharArray = dbConsultantName.toCharArray();
        Arrays.sort(dbCharArray);
        dbConsultantName = new String(dbCharArray);
        if (dbConsultantName.equalsIgnoreCase(extractedConsultantName)) {
            consultantFlag = true;
        }
        return consultantFlag;
    }


    public boolean dateValidation(String endDate, String startDate, String timesheetStart, String timesheetEnd,  Map consultantDetals ){
        boolean flag = true;

        DateTimeFormatter consultantDatesFormat = DateTimeFormatter.ofPattern("MM/dd/yyyy");
        DateTimeFormatter timesheetDatesFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        if(endDate == null|| endDate.isBlank()){
            String name = consultantDetals.get("consultantName").toString();
            String mail = consultantDetals.get("consultantEmail").toString();
            if(mail ==null || mail.isBlank() || name == null || name.isBlank()){
                flag = false;
            }
        }else{
            LocalDate timesheetStartDate =null;
            LocalDate timesheetEndDate = null;
            if(!timesheetStart.isBlank()){
                timesheetStartDate = LocalDate.parse(timesheetStart,timesheetDatesFormat);
            }else{
                flag = false;
            }
            if(!timesheetEnd.isBlank()) {
                timesheetEndDate = LocalDate.parse(timesheetEnd, timesheetDatesFormat);
            }else{
                flag = false;
            }
            LocalDate consultantStart = LocalDate.parse(startDate,consultantDatesFormat);
            LocalDate consultantEnd = LocalDate.parse(endDate,consultantDatesFormat);

            if(timesheetEndDate != null && timesheetStartDate != null){
                if(timesheetStartDate.isBefore(consultantStart) || timesheetStartDate.isAfter(consultantEnd) || timesheetEndDate.isAfter(consultantEnd)){
                    flag = false;
                }
            }

        }

        return flag;
    }

    @Override
    public ResponseEntity<?> runValidations(String id) {

        String currentState="";
        ResponseEntity<?> responseEntity = null;
        int validationFailedCount = 0;
        String validationFailedReason = "";
        String errorMessage="";
        String errorDetails="";

        try {
            List<Map> companyList = mongoTemplate.findAll(Map.class, "Company");

            Map workFlowMap = new LinkedHashMap();
            workFlowMap.put("id", Crypt.encrypt("4"));
            workFlowMap.put("state", Crypt.encrypt("Validation"));
            workFlowMap.put("startDate", Crypt.encrypt(currentDateTime()));

            Map centralizedJsonMap = mongoTemplate.findById(id, Map.class, "CentralizedJson");
            List<Map> consultantsList = (List) centralizedJsonMap.get("consultants");
            Map consultantDetals = (Map) ((Map) consultantsList.get(0)).get("consultantDetails");
            Map clientDetails = (Map) ((Map) consultantsList.get(0)).get("clientDetails");

            String jsonCreatedDate = centralizedJsonMap.get("createdDate").toString();

            // Format the parsed date to the desired output format
            String formattedCurrentDate = jsonCreatedDate.substring(0, 10);
            Map timesheetDataMap = (Map) consultantsList.get(0).get("timesheetData");
            String startDate = "";
            String endDate = "";
            if (timesheetDataMap.containsKey("timesheetStartDate")) {
                startDate = timesheetDataMap.get("timesheetStartDate").toString();
            }
            if (timesheetDataMap.containsKey("timesheetEndDate")) {
                endDate = timesheetDataMap.get("timesheetEndDate").toString();
            }
            boolean resultFlag = false;
            boolean notApplicableFlag = false;
            boolean applicableFlag = false;
            ValidationsResult validationsResult = new ValidationsResult();
            List<Map> resultList = new ArrayList<>();

            List<Rules> rulesList = rulesRepository.findByEnabled(true);
            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            String CURRENT_NEW_DATE = currentDate.format(formatter);

            for (Rules rule : rulesList) {
                boolean res = false;
                // 6690d05e5839853ffee3ebcd
                //668392e958d81279240010f0
                if (rule.getCustomQuery() != null && !rule.getCustomQuery().isBlank()) {
                    if (rule.getCustomQuery().equalsIgnoreCase("CONSULTANT_CHECK")) {

                        String extractedConsultantName = consultantDetals.get("extractedConsultantName").toString();

                        String dbConsultantName = consultantDetals.get("consultantName").toString();
                        boolean consultantValidationFlag = validateConsultantName(extractedConsultantName, dbConsultantName);


                        if (consultantValidationFlag) {
                            res = true;
                        }

                    } else if(rule.getCustomQuery().equalsIgnoreCase("STARTENDDATE_CHECK")) {
                        String consultantEndDate = Crypt.decrypt(consultantDetals.get("endDate").toString());
                        String consultantStartDate = Crypt.decrypt(consultantDetals.get("startDate").toString());

                        boolean resultflag = dateValidation(consultantEndDate,consultantStartDate,startDate,endDate,consultantDetals);

//
                        if(resultflag) {
                        	res = true;
                        }
                    }
                    
                    else if (rule.getCustomQuery().equalsIgnoreCase("DUPLICATE_CHECK")) {
                        if (timesheetDataMap.containsKey("timesheetEndDate") && timesheetDataMap.containsKey("timesheetStartDate") && clientDetails.containsKey("clientName") && consultantDetals.containsKey("consultantName")) {
                            List<Document> listOfDocuments = new ArrayList<>();

                            Criteria criteria = new Criteria().andOperator(
                                    Criteria.where("active").is(true),
                                    Criteria.where("approve").is(true),
                                    Criteria.where("consultants.consultantDetails.consultantName")
                                            .is(consultantDetals.get("consultantName").toString()),
                                    Criteria.where("consultants.clientDetails.clientName")

                                            .is(clientDetails.get("clientName").toString()),
                                    Criteria.where("consultants.timesheetData.timesheetStartDate")
                                            .is(timesheetDataMap.get("timesheetStartDate").toString()),
                                    Criteria.where("consultants.timesheetData.timesheetEndDate")
                                            .is(timesheetDataMap.get("timesheetEndDate").toString()));
                            listOfDocuments = mongoTemplate.find(Query.query(criteria), Document.class, "CentralizedJson");

                            if (listOfDocuments.size() == 0) {
                                res = true;

                            }
                        } else {
                            res = true;
                        }


                    }
                } else {
                    String queryString = rule.getQuery().replace("ID", id).replace("CURRENT_NEW_DATE", CURRENT_NEW_DATE);

                    BasicQuery basicQuery = new BasicQuery(queryString);
                    res = mongoTemplate.exists(basicQuery, "CentralizedJson");

                }
                Map resultMap = new LinkedHashMap();
                resultMap.put("ruleId", Crypt.encrypt(rule.getId()));
                resultMap.put("rule", Crypt.encrypt(rule.getRule()));
                //  resultMap.put("rule", rule.getRule());


                if (!res && !rule.isOptionalFlag()) {
                    validationFailedCount += 1;
                    resultFlag = true;
                    validationFailedReason += rule.getErrorMessage();

                    String ruleErrorMessage = "";
                    if (rule.getRule().equalsIgnoreCase("Name on timesheet should match name in ATS Records")) {
                        String extractedConsultantName = consultantDetals.get("extractedConsultantName").toString();
                        if (extractedConsultantName.equalsIgnoreCase("extractedName")) {
                            extractedConsultantName = "";
                        }

                        System.out.println(" extractedConsultant name :"+ extractedConsultantName);
                        ruleErrorMessage = rule.getErrorMessage().replaceAll("CONSULTANT_NAME", extractedConsultantName);

                        System.out.println(" error message :"+ ruleErrorMessage);
                    }else if (rule.getRule().equalsIgnoreCase("timesheet period should be within the contract timeline")) {
                        String consultantEndDate ="";
                        String consultantStartDate = "";
                        if(consultantDetals.get("endDate")!=null){
                            consultantEndDate = Crypt.decrypt(consultantDetals.get("endDate").toString());
                        }
                        if(consultantDetals.get("startDate") != null){
                            consultantStartDate =  Crypt.decrypt(consultantDetals.get("startDate").toString());
                        }

                        String error_msg = rule.getErrorMessage();

                        if (consultantEndDate == null || consultantEndDate.isBlank()) {
                            error_msg = "NA";
                            notApplicableFlag = true;
                            ruleErrorMessage = error_msg;
                        } else {
                            String consultantStartDateValue = (consultantStartDate != null && !consultantStartDate.isEmpty()) ? consultantStartDate : "NA";
                            String consultantEndDateValue = (consultantEndDate != null && !consultantEndDate.isEmpty()) ? consultantEndDate : "NA";
                            String timesheetStartDateValue = (startDate != null && !startDate.isEmpty()) ? startDate : "NA";
                            String timesheetEndDateValue = (endDate != null && !endDate.isEmpty()) ? endDate : "NA";

                            String message = String.format(error_msg,
                                    timesheetStartDateValue, timesheetEndDateValue, consultantStartDateValue, consultantEndDateValue
                            );
                            ruleErrorMessage = message;
                        }

                    }
                    else if (rule.getRule().equalsIgnoreCase("Timesheet Start Date < Current Date")) {

                        String error_msg = rule.getErrorMessage();
                        if(startDate == null || startDate.isBlank()){
                            error_msg = "NA";
                            applicableFlag = true;
                            ruleErrorMessage = error_msg;
                        }else{
                            ruleErrorMessage = rule.getErrorMessage().replaceAll("START_DATE", startDate);
                            ruleErrorMessage = ruleErrorMessage.replaceAll("CURRENT_DATE", formattedCurrentDate);
                        }

                    } else if (rule.getRule().equalsIgnoreCase("Timesheet Start Date < Timesheet End Date")) {

                        String error_msg = rule.getErrorMessage();
                        if(startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()){
                            error_msg = "NA";
                            applicableFlag = true;
                            ruleErrorMessage = error_msg;
                        }else {
                            ruleErrorMessage = rule.getErrorMessage().replaceAll("START_DATE", startDate);
                            ruleErrorMessage = ruleErrorMessage.replaceAll("END_DATE", endDate);
                        }
                    } else if (rule.getRule().equalsIgnoreCase("Timesheet End Date < Current Date")) {
                        String error_msg = rule.getErrorMessage();
                        if(endDate == null || endDate.isBlank()){
                            error_msg = "NA";
                            applicableFlag = true;
                            ruleErrorMessage = error_msg;
                        }else{
                            ruleErrorMessage = rule.getErrorMessage().replaceAll("END_DATE", endDate);
                            ruleErrorMessage = ruleErrorMessage.replaceAll("CURRENT_DATE", formattedCurrentDate);
                        }


                    } else if (rule.getRule().equalsIgnoreCase("Timesheet dates between start Date and End Date")) {
                        String error_msg = rule.getErrorMessage();
                        if(startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()){
                            error_msg = "NA";
                            applicableFlag = true;
                            ruleErrorMessage = error_msg;
                        }else{
                            ruleErrorMessage = rule.getErrorMessage();
                        }

                    }else {
                        ruleErrorMessage = rule.getErrorMessage();
                    }

                    if (errorDetails.isBlank()) {
                        errorDetails += ruleErrorMessage;
                    } else {
                        errorDetails += ", " + ruleErrorMessage;
                    }

                    if (errorMessage.isBlank()) {
                        if(!notApplicableFlag){
                            errorMessage += rule.getRuleAcronym();
                        }
                    } else {
                        if(!notApplicableFlag){

                        if (rule.getRuleAcronym().equalsIgnoreCase("Invalid Timesheet Start Date")) {
                            if (!errorMessage.contains("Invalid Timesheet Start Date")) {
                                errorMessage += ", " + rule.getRuleAcronym();

                            }
                        } else {
                            errorMessage += ", " + rule.getRuleAcronym();
                        }
                        }
                    }
                    resultMap.put("message", Crypt.encrypt(ruleErrorMessage));
                } else {
                    if (rule.isOptionalFlag()) {
                        if (res) {
                            resultMap.put("message", Crypt.encrypt(" "));
                        } else {
                            resultMap.put("message", Crypt.encrypt(rule.getErrorMessage() + " " + timesheetDataMap.get("senderEmail").toString()));

                        }

                    } else {
                        resultMap.put("message", Crypt.encrypt(" "));
                    }

                }
                //  resultMap.put("status",String.valueOf(res));
                resultMap.put("ruleAcronym", Crypt.encrypt(rule.getRuleAcronym()));
                resultMap.put("optionalFlag", rule.isOptionalFlag());
                resultMap.put("status", Crypt.encrypt(String.valueOf(res)));
                resultList.add(resultMap);
            }
            validationsResult.setResult(resultList);
            workFlowMap.put("endDate", Crypt.encrypt(currentDateTime()));


            if (!resultFlag) {
                workFlowMap.put("status", Crypt.encrypt("true"));


            } else {
                currentState = "Validation Error";
                centralizedJsonMap.put("currentState", "Validation Error");
                centralizedJsonMap.put("errorStatus", "Validation Error");
                centralizedJsonMap.put("errorMessage", errorMessage);
                centralizedJsonMap.put("errorDetails", errorDetails);
                centralizedJsonMap.put("approve", false);


                workFlowMap.put("status", Crypt.encrypt("false"));

            }
            ((List) timesheetDataMap.get("workflow")).add(workFlowMap);

            if (!resultFlag) {
                String monthStartDate = "";
                if (timesheetDataMap.containsKey("timesheetStartDate")) {
                    String[] startDateAray = timesheetDataMap.get("timesheetStartDate").toString().split("-");
                    monthStartDate = getMonthStartDate(
                            Integer.parseInt(startDateAray[0]), Integer.parseInt(startDateAray[1]));
                }
                String monthEndDate = "";
                if (timesheetDataMap.containsKey("timesheetEndDate")) {
                    String[] endDateAray = timesheetDataMap.get("timesheetEndDate").toString().split("-");
                    monthEndDate = getMonthEndDate(Integer.parseInt(endDateAray[0]),
                            Integer.parseInt(endDateAray[1]));
                }
                List<Document> lisOfDocuments = executeMonthlyInvoiceQuery(
                        monthStartDate, monthEndDate, consultantDetals.get("consultantEmail").toString(), clientDetails.get("clientId").toString());
                if (lisOfDocuments.size() > 0) {
                    lisOfDocuments.get(0).put("status", false);
                    mongoTemplate.save(lisOfDocuments.get(0), "MonthlyTimesheetSummary");
                }

                currentState = "Complete";
                centralizedJsonMap.put("currentState", "Complete");

                centralizedJsonMap.put("approve", true);
                workFlowMap = new LinkedHashMap();
                workFlowMap.put("id", Crypt.encrypt("5"));
                workFlowMap.put("state", Crypt.encrypt("Complete"));
                workFlowMap.put("startDate", Crypt.encrypt(currentDateTime()));
                workFlowMap.put("endDate", Crypt.encrypt(currentDateTime()));
                workFlowMap.put("status", Crypt.encrypt("true"));

                ((List) timesheetDataMap.get("workflow")).add(workFlowMap);

                centralizedJsonMap.put("approvedDate", currentDateTime());
            }


            centralizedJsonMap.put("validationsResult", validationsResult);


            consultantsList.get(0).put("timesheetData", timesheetDataMap);
            centralizedJsonMap.put("consultants", consultantsList);
            Map centralizedJson = mongoTemplate.save(centralizedJsonMap, "CentralizedJson");

            System.out.println("***********************************");
            System.out.println("centralized json Id: "+centralizedJson.get("timesheetId"));

            RestTemplate restTemplateDaywise = new RestTemplate();
            HttpHeaders headersdatWise = new HttpHeaders();
            String validationUrl = splitIntoDayWiseApi + centralizedJsonMap.get("_id");

            HttpEntity<String> entity = new HttpEntity<>(headersdatWise);

            restTemplateDaywise.exchange(validationUrl, HttpMethod.GET, entity, String.class);

            responseEntity = new ResponseEntity<>(validationsResult, HttpStatus.OK);
Map notificationsMap=new LinkedHashMap();
             if(((Map)companyList.get(0).get("configuration")).containsKey("notifications"))
             {
                 notificationsMap= (Map) ((Map)companyList.get(0).get("configuration")).get("notifications");
             }
             boolean faileFlag=Boolean.parseBoolean(notificationsMap.get("failedFlag").toString());
            boolean successFlag=Boolean.parseBoolean(notificationsMap.get("successFlag").toString());

            if ((faileFlag && !currentState.equalsIgnoreCase("Complete")) ||(successFlag&&currentState.equalsIgnoreCase("complete")))
            {
                if (timesheetDataMap.containsKey("messageId")) {
                    String emailBody = "";
                    if (currentState.equalsIgnoreCase("complete")) {
                        emailBody = notificationsMap.get("successMessage").toString();
                    } else {
                        emailBody =  notificationsMap.get("failedMessage").toString();
                    }

                    Map emailRequestMap = new LinkedHashMap();
                    emailRequestMap.put("messageId", Crypt.decrypt(timesheetDataMap.get("messageId").toString()));
                    emailRequestMap.put("emailBody", emailBody);
                    emailRequestMap.put("emailType", "reply");
                    emailRequestMap.put("jsonId", id);
                    emailRequestMap.put("senderEmail", Crypt.decrypt(timesheetDataMap.get("senderEmail").toString()));

                    emailRequestMap.put("emailSubject", Crypt.decrypt(timesheetDataMap.get("emailSubject").toString()));

                    emailRequestMap.put("conversationId", Crypt.decrypt(timesheetDataMap.get("conversationId").toString()));


                    RestTemplate restTemplate = new RestTemplate();
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    HttpEntity requestEntity = new HttpEntity<>(emailRequestMap, headers);
                    restTemplate.postForEntity(emailReplyUrl,requestEntity,String.class);
                    Map emailLog = new LinkedHashMap();

                    emailLog.put("consultantName", consultantDetals.get("consultantName"));
                    emailLog.put("consultantEmail", consultantDetals.get("consultantEmail"));
                    if (currentState.equalsIgnoreCase("Complete")) {
                        emailLog.put("emailType", "Success");
                    } else {
                        emailLog.put("emailType", "Failure");
                    }
                    emailLog.put("emailSentDate", currentDateTime());
                    emailLog.put("emailBody", emailBody);

                    mongoTemplate.save(emailLog, "EmailLogs");


                }
        }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return responseEntity;

    }


    public String getMonthStartDate(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(calendar.getTime());
    }

    public String getMonthEndDate(int year, int month) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.MONTH, month - 1);
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(calendar.getTime());
    }

    public List<Document> executeMonthlyInvoiceQuery(String startDate, String endDate, String consultantEmail, String clientId) throws Exception {
        try {
            Criteria criteria = new Criteria();
            criteria.andOperator(
                    Criteria.where("consultDetails.consultantEmail").is(consultantEmail),
                    Criteria.where("clientDetails.clientId").is(clientId),

                    new Criteria().orOperator(
                            Criteria.where("timesheets.timesheetStartDate").gte(startDate).lte(endDate),
                            Criteria.where("timesheets.timesheetEndDate").gte(startDate).lte(endDate),
                            new Criteria().andOperator(
                                    Criteria.where("timesheets.timesheetStartDate").lte(startDate),
                                    Criteria.where("timesheets.timesheetEndDate").gte(endDate)
                            )
                    )
            );

            Query query = new Query(criteria);
            return mongoTemplate.find(query, Document.class, "MonthlyTimesheetSummary");
        } catch (DataAccessException ex) {
            // Log the exception or handle it as per your application's requirements
            throw new Exception("Error executing timesheet query", ex);
        }
    }

}
