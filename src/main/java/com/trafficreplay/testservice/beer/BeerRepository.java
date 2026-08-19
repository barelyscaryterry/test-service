package com.trafficreplay.testservice.beer;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanRequest;
import software.amazon.awssdk.services.dynamodb.model.ScanResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class BeerRepository {

    private static final String ATTR_ID = "id";
    private static final String ATTR_NAME = "name";
    private static final String ATTR_BREWERY = "brewery";
    private static final String ATTR_STYLE = "style";
    private static final String ATTR_ABV = "abv";
    private static final String ATTR_CREATED_AT = "createdAt";
    private static final String ATTR_UPDATED_AT = "updatedAt";

    private final DynamoDbClient dynamoDbClient;
    private final String tableName;

    public BeerRepository(DynamoDbClient dynamoDbClient,
                           @Value("${test-service.ddb.table}") String tableName) {
        this.dynamoDbClient = dynamoDbClient;
        this.tableName = tableName;
    }

    @RateLimiter(name = "beerApi")
    @Retry(name = "beerApi")
    @CircuitBreaker(name = "beerApi")
    public Beer put(Beer beer) {
        PutItemRequest request = PutItemRequest.builder()
                .tableName(tableName)
                .item(toItem(beer))
                .build();
        try {
            dynamoDbClient.putItem(request);
        } catch (DynamoDbException e) {
            throw new BeerAdapterException("Failed to put beer with id: " + beer.id(), e);
        }
        return beer;
    }

    @RateLimiter(name = "beerApi")
    @Retry(name = "beerApi")
    @CircuitBreaker(name = "beerApi")
    public Beer get(String id) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(tableName)
                .key(Map.of(ATTR_ID, AttributeValue.fromS(id)))
                .build();

        GetItemResponse response;
        try {
            response = dynamoDbClient.getItem(request);
        } catch (DynamoDbException e) {
            throw new BeerAdapterException("Failed to get beer with id: " + id, e);
        }

        if (!response.hasItem() || response.item().isEmpty()) {
            throw new BeerNotFoundException(id);
        }

        return fromItem(response.item());
    }

    @RateLimiter(name = "beerApi")
    @Retry(name = "beerApi")
    @CircuitBreaker(name = "beerApi")
    public List<Beer> findAll() {
        ScanRequest request = ScanRequest.builder()
                .tableName(tableName)
                .build();

        try {
            ScanResponse response = dynamoDbClient.scan(request);
            return response.items().stream().map(BeerRepository::fromItem).toList();
        } catch (DynamoDbException e) {
            throw new BeerAdapterException("Failed to scan beers", e);
        }
    }

    private static Map<String, AttributeValue> toItem(Beer beer) {
        return Map.of(
                ATTR_ID, AttributeValue.fromS(beer.id()),
                ATTR_NAME, AttributeValue.fromS(beer.name()),
                ATTR_BREWERY, AttributeValue.fromS(beer.brewery()),
                ATTR_STYLE, AttributeValue.fromS(beer.style()),
                ATTR_ABV, AttributeValue.fromN(beer.abv().toPlainString()),
                ATTR_CREATED_AT, AttributeValue.fromN(Long.toString(beer.createdAt().toEpochMilli())),
                ATTR_UPDATED_AT, AttributeValue.fromN(Long.toString(beer.updatedAt().toEpochMilli())));
    }

    private static Beer fromItem(Map<String, AttributeValue> item) {
        return new Beer(
                item.get(ATTR_ID).s(),
                item.get(ATTR_NAME).s(),
                item.get(ATTR_BREWERY).s(),
                item.get(ATTR_STYLE).s(),
                new BigDecimal(item.get(ATTR_ABV).n()),
                Instant.ofEpochMilli(Long.parseLong(item.get(ATTR_CREATED_AT).n())),
                Instant.ofEpochMilli(Long.parseLong(item.get(ATTR_UPDATED_AT).n())));
    }
}
