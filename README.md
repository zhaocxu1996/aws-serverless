# serverless

Lambda function will be invoked by the SNS notification. Lambda function is responsible for sending email to the user.

As a user, I should be able to only have 1 request token active in database (DynamoDB) at a time.

As a user, I expect the request information to be stored in DynamoDB with TTL of 60 minutes.

As a user, I expect the request token to expire after 60 minutes if it is not used by then.

As a user, I expect the to receive links to all the bills that are due within X days in an email.

As a user, if I make multiple requests when there is a active token in the database, I should only receive 1 email.