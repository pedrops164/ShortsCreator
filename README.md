

ports
8080 -> keycloak
8081 -> API Gateway
8083 -> Content Storage Service
8085 -> Content Generation Service
8086 -> Notification Service
8087 -> Payment Service
5672 and 15672 -> rabbitmq
27017 -> mongodb

# Run stripe cli with stripe listen --forward-to localhost:8081/api/v1/stripe/webhooks