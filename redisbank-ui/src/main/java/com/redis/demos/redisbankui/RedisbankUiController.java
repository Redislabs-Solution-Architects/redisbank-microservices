package com.redis.demos.redisbankui;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.redis.demos.redisbankui.Config.StompConfig;
import com.redis.lettucemod.search.SearchResults;

@RestController
@RequestMapping(path = "/api")
@CrossOrigin
public class RedisbankUiController {

    private Config config;
    private RestTemplate restTemplate;
    private String pfmUri;
    private String amUri;
    private String trUri;

    public RedisbankUiController(Config config, RestTemplate restTemplate, @Value("${pfm.uri}") String pfmUri,
            @Value("${am.uri}") String amUri, @Value("${tr.uri}") String trUri) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.pfmUri = pfmUri;
        this.amUri = amUri;
        this.trUri = trUri;
    }

    @GetMapping("/config/stomp")
    public StompConfig stompConfig() {
        return config.getStomp();
    }

    @GetMapping("/account")
    public Account getAccount() {
        URI accountUri = URI.create(amUri + "/account?iban=" + getAuthenticatedUserIban());
        return restTemplate.getForEntity(accountUri, Account.class).getBody();
    }

    @GetMapping("/balance")
    public Balance[] balance() {
        URI balanceUri = URI.create(pfmUri + "/balance?iban=" + getAuthenticatedUserIban());
        return restTemplate.getForEntity(balanceUri, Balance[].class).getBody();
    }

    @GetMapping("/biggestspenders")
    public BiggestSpenders biggestSpenders() {
        URI balanceUri = URI.create(pfmUri + "/biggestspenders?iban=" + getAuthenticatedUserIban());
        return restTemplate.getForEntity(balanceUri, BiggestSpenders.class).getBody();
    }

    @GetMapping("/transactions")
    public SearchResults<String, String> listTransactions() {
        URI transactionsUri = URI.create(trUri + "/transactions?iban=" + getAuthenticatedUserIban());
        return restTemplate
                .exchange(transactionsUri, HttpMethod.GET, null,
                        new ParameterizedTypeReference<SearchResults<String, String>>() {
                        })
                .getBody();
    }

    @GetMapping("/search")
    public SearchResults<String, String> searchTransactions(@RequestParam("term") String term) {
        URI transactionsUri = URI
                .create(trUri + "/search?iban=" + getAuthenticatedUserIban() + "&term=" + term);
        return restTemplate
                .exchange(transactionsUri, HttpMethod.GET, null,
                        new ParameterizedTypeReference<SearchResults<String, String>>() {
                        })
                .getBody();
    }

    private String getAuthenticatedUserIban() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof AnonymousAuthenticationToken)) {
            return Utilities.generateFakeIbanFrom(authentication.getName());
        } else {
            throw new RuntimeException("This functionality is not available to anonymous users");
        }
    }

}
