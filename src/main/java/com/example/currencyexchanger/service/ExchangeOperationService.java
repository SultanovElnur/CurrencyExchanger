package com.example.currencyexchanger.service;

import com.example.currencyexchanger.dto.ExchangeDto;
import com.example.currencyexchanger.dto.ExchangeResultDto;
import com.example.currencyexchanger.entity.Operation;
import com.example.currencyexchanger.repository.ExchangeRepository;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;

@Service
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ExchangeOperationService {
    RestTemplate restTemplate;
    ExchangeRepository exchangeRepository;
    String url;
    String apiKey;

    static Logger logger = LoggerFactory.getLogger(ExchangeOperationService.class);

    public ExchangeOperationService(RestTemplate restTemplate, @Value("${currency.url}") String url, @Value("${currency.api-key}") String apiKey, ExchangeRepository exchangeRepository) {
        this.restTemplate = restTemplate;
        this.url = url;
        this.apiKey = apiKey;
        this.exchangeRepository = exchangeRepository;
    }

    public ExchangeResultDto exchange(ExchangeDto exchangeDto) {
        logger.info("exchange method INFO Message");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("apikey", apiKey);
        HttpEntity httpEntity = new HttpEntity(httpHeaders);
        var responseEntity = restTemplate.exchange(
                String.format("%s?from=%s&to=%s&amount=%s&date=%s", url, exchangeDto.getCurrencySell(),
                        exchangeDto.getCurrencyBuy(), exchangeDto.getAmount(), LocalDate.now()), HttpMethod.GET,
                httpEntity,
                ExchangeResultDto.class);
        var result = responseEntity.getBody();
        exchangeRepository.save(new Operation(
                exchangeDto.getClientName(),
                exchangeDto.getFin(),
                exchangeDto.getCurrencySell(),
                exchangeDto.getCurrencyBuy(),
                exchangeDto.getAmount(),
                result.getInfo().getQuote(),
                result.getResult(), result.getDate()));
        return result;
    }

    public Map<String, Object> getAllOperations(String name, String amount, String date, int page, int size) {
        logger.info("Getting all operations INFO Message");
        Pageable pageable = PageRequest.of(page, size);
        Page<Operation> operations;

        if (name != null) {
            operations = exchangeRepository.findByClientName(name, pageable);
        } else if (amount != null) {
            operations = exchangeRepository.findByAmount(Double.valueOf(amount), pageable);
        } else if (date != null) {
            operations = exchangeRepository.findByDate(LocalDate.parse(date), pageable);
        } else {
            operations = exchangeRepository.findAll(pageable);
        }

        return Map.of("operations", operations.getContent(),
                "currentPage", operations.getNumber(),
                "totalElements", operations.getTotalElements(),
                "totalPages", operations.getTotalPages());
    }
}
