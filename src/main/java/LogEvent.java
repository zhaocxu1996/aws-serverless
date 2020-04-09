import com.amazonaws.AmazonServiceException;
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

        String[] message = request.getRecords().get(0).getSNS().getMessage().split("\\|");

        System.out.println(request.getRecords().get(0).getSNS().getMessage());
        String region = "us-east-1";
        String route53 = "dev.zhaocxu.me";
        String url = "https://" + route53 + "/v1/bill/";
        String email = message[0];

        long ttl = System.currentTimeMillis();

        AmazonDynamoDB DBclient = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_1).build();
        DynamoDB dynamoDB = new DynamoDB(DBclient);
        Table table = dynamoDB.getTable("csye6225");

        Item item = table.getItem("id", email);
        if (item == null || item.getNumber("TTl").longValue() < ttl - 1800000) {

            try {
                table.putItem(new Item().
                        withPrimaryKey("id", email).withString("Token", UUID.randomUUID().toString()).withNumber("TTl", ttl));
            } catch (AmazonServiceException e) {
                e.printStackTrace();
            }


            StringBuffer links = new StringBuffer();
            for (String s : Arrays.copyOfRange(message, 1, message.length))
                links.append("<a href='" + url + s + "'>" + s + "</a><br>");

            final String FROM = "assignment10@" + route53;
            final String SUBJECT = "recipes link";
            final String HTMLBODY = "your recipes: <br>" + links;
            try {
                AmazonSimpleEmailService SESclient = AmazonSimpleEmailServiceClientBuilder.standard().withRegion(region).build();
                SendEmailRequest sendEmailRequest = new SendEmailRequest()
                        .withDestination(new Destination().withToAddresses(email))
                        .withMessage(new Message()
                                .withBody(new Body()
                                        .withHtml(new Content().withCharset("UTF-8").withData(HTMLBODY)))
                                .withSubject(new Content().withCharset("UTF-8").withData(SUBJECT)))
                        .withSource(FROM);
                SESclient.sendEmail(sendEmailRequest);
                System.out.println("Email sent successfully!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return null;
    }
}
