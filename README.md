# Spawtz Calendar

Produces iCalendar feeds for Brighton & Hove Tag rugby teams. 

## Build
```
./gradlew build
```

## Deployment
Currently deployed to AWS Lambda using AWS SAM. This is configured in [template.yaml](template.yaml).
```
sam build
sam deploy
```