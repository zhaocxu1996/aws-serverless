import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import java.util.Arrays;
import java.util.UUID;

public class LogEvent implements RequestHandler<SNSEvent, Object> {

    @Override
    public Object handleRequest(SNSEvent request, Context context) {

        LambdaLogger logger = context.getLogger();
        if (request.getRecords() == null) {
            logger.log("No record found.");
            return null;
        }
        logger.log(request.getRecords().get(0).getSNS().getMessage());
        String[] message = request.getRecords().get(0).getSNS().getMessage().split("\\|");
        String region = Regions.US_EAST_1.toString();
        String route53 = "dev.zhaocxu.me";
        String email = message[0];
        String url = "https://" + route53 + "/v1/bill/";

        long ttl = System.currentTimeMillis();

        AmazonDynamoDB dbClient = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        DynamoDB dynamoDB = new DynamoDB(dbClient);
        Table table = dynamoDB.getTable("csye6225");

        Item item = table.getItem("id", email);
        if (item == null || item.getNumber("TTl").longValue() < ttl - 1800000) {
            table.putItem(new Item().withPrimaryKey("id", email).withString("Token", UUID.randomUUID().toString()).withNumber("TTl", ttl));
            StringBuffer links = new StringBuffer();
            for (String s : Arrays.copyOfRange(message, 1, message.length)) {
                links.append("<a href='" + url + s + "'>" + s + "</a><br>");
//                links.append(s+"<br>");
            }
            final String from = "assignment10@" + route53;
            final String subject = "list of bill id";
            final String htmlBody = "your bill ids: <br>" + links;

            AmazonSimpleEmailService SESClient = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(region).build();
            SendEmailRequest sendEmailRequest = new SendEmailRequest()
                    .withDestination(new Destination().withToAddresses(email))
                    .withMessage(new Message()
                            .withSubject(new Content().withCharset("UTF-8").withData(subject))
                            .withBody(new Body()
                                    .withHtml(new Content().withCharset("UTF-8").withData(htmlBody))))
                    .withSource(from);
            SESClient.sendEmail(sendEmailRequest);
            logger.log("Email sent.");
        }
        return null;
    }
}
